package com.alsaeeddev.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import com.alsaeeddev.data.model.ActiveCallSession
import com.alsaeeddev.data.model.CallAudioRoute
import com.alsaeeddev.data.model.CallState
import com.alsaeeddev.data.model.CallerRevealState
import com.alsaeeddev.data.model.ProtectionMode
import com.alsaeeddev.data.model.ProtectionSettings
import com.alsaeeddev.data.repository.SettingsRepository
import com.alsaeeddev.notification.CallNotificationManager
import com.alsaeeddev.security.BiometricAuthManager
import com.alsaeeddev.security.KeystoreCryptoManager
import com.alsaeeddev.security.PrivacyLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class CallManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: CallManager? = null

        fun getInstance(context: Context): CallManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CallManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val settingsRepository = SettingsRepository(context)
    private val cryptoManager = KeystoreCryptoManager(context)
    private val biometricAuthManager = BiometricAuthManager(context)
    private val ringtoneManager = CallRingtoneManager(context)
    private val notificationManager = CallNotificationManager(context)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _activeCallSession = MutableStateFlow<ActiveCallSession?>(null)
    val activeCallSession: StateFlow<ActiveCallSession?> = _activeCallSession.asStateFlow()

    private var currentTelecomCall: Call? = null
    private var inCallServiceInstance: android.telecom.InCallService? = null
    private var autoHideJob: Job? = null
    private var durationTimerJob: Job? = null

    fun registerInCallService(service: android.telecom.InCallService) {
        inCallServiceInstance = service
        PrivacyLogger.i("InCallService registered with CallManager.")
    }

    fun unregisterInCallService(service: android.telecom.InCallService) {
        if (inCallServiceInstance == service) {
            inCallServiceInstance = null
            PrivacyLogger.i("InCallService unregistered from CallManager.")
        }
    }

    fun updateAudioStateFromTelecom(isMuted: Boolean, route: CallAudioRoute) {
        val current = _activeCallSession.value ?: return
        _activeCallSession.value = current.copy(
            isMuted = isMuted,
            audioRoute = route
        )
    }

    private var cachedSettings: ProtectionSettings = ProtectionSettings()

    private var isScreenOffReceiverRegistered = false
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                onScreenOff()
            }
        }
    }

    init {
        cachedSettings = settingsRepository.getCurrentSettings()
        scope.launch {
            settingsRepository.settingsFlow.collect { updated ->
                cachedSettings = updated
                PrivacyLogger.i("CallManager synced settings: protection=${updated.isProtectionEnabled}, hideName=${updated.hideCallerName}, hideNumber=${updated.hideCallerNumber}, hidePhoto=${updated.hideContactPhoto}, timeout=${updated.revealTimeoutSeconds}s")
            }
        }
    }

    fun updateSettings(settings: ProtectionSettings) {
        cachedSettings = settings
    }

    private fun registerScreenOffReceiver() {
        if (!isScreenOffReceiverRegistered) {
            try {
                val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
                context.registerReceiver(screenOffReceiver, filter)
                isScreenOffReceiverRegistered = true
            } catch (e: Exception) {
                PrivacyLogger.e("Error registering screen off receiver: ${e.message}", e)
            }
        }
    }

    private fun unregisterScreenOffReceiver() {
        if (isScreenOffReceiverRegistered) {
            try {
                context.unregisterReceiver(screenOffReceiver)
                isScreenOffReceiverRegistered = false
            } catch (e: Exception) {
                PrivacyLogger.e("Error unregistering screen off receiver: ${e.message}", e)
            }
        }
    }

    /**
     * Called when a real incoming call is detected via Telecom InCallService.
     */
    fun onTelecomCallAdded(call: Call) {
        currentTelecomCall = call
        val isLocked = biometricAuthManager.isDeviceLocked()

        val handleUri: Uri? = call.details?.handle
        val rawNumber = handleUri?.schemeSpecificPart ?: ""
        val callerDisplayName = call.details?.callerDisplayName ?: ""

        // Query device contacts to resolve saved name for this number
        val contact = ContactLookupHelper.resolveContact(context, rawNumber)
        val displayName = when {
            callerDisplayName.isNotBlank() -> callerDisplayName
            !contact.name.isNullOrBlank() -> contact.name
            rawNumber.isNotBlank() -> rawNumber
            else -> "Incoming Caller"
        }
        val displayNumber = contact.formattedNumber?.ifBlank { rawNumber } ?: rawNumber.ifBlank { "Restricted" }
        val location = "Cellular Call"

        PrivacyLogger.i("Incoming Telecom call registered. Resolved name='$displayName', number='$displayNumber'. Encrypting payload...")
        val encryptedPayload = cryptoManager.encryptCallerData(
            name = displayName,
            number = displayNumber,
            location = location
        )

        val callState = when (call.state) {
            Call.STATE_RINGING -> CallState.INCOMING_RINGING
            Call.STATE_ACTIVE -> CallState.ACTIVE
            Call.STATE_HOLDING -> CallState.ON_HOLD
            Call.STATE_CONNECTING, Call.STATE_DIALING -> CallState.CONNECTING
            else -> CallState.INCOMING_RINGING
        }

        // Determine initial reveal state based on protection settings
        val initialRevealState = determineInitialRevealState(isLocked, encryptedPayload, displayName, displayNumber, location)

        _activeCallSession.value = ActiveCallSession(
            callId = UUID.randomUUID().toString(),
            callState = callState,
            isSimulated = false,
            encryptedPayload = encryptedPayload,
            revealState = initialRevealState,
            audioRoute = CallAudioRoute.EARPIECE,
            isDeviceLockedAtArrival = isLocked,
            rawCallerName = displayName,
            rawCallerNumber = displayNumber,
            rawLocation = location
        )

        registerScreenOffReceiver()

        if (callState == CallState.INCOMING_RINGING) {
            ringtoneManager.startRinging()
        }

        startDurationTimer()
    }

    fun onTelecomCallStateChanged(call: Call, state: Int) {
        val current = _activeCallSession.value ?: return
        val newState = when (state) {
            Call.STATE_RINGING -> CallState.INCOMING_RINGING
            Call.STATE_ACTIVE -> CallState.ACTIVE
            Call.STATE_HOLDING -> CallState.ON_HOLD
            Call.STATE_DISCONNECTED -> CallState.DISCONNECTED
            else -> current.callState
        }

        if (newState != CallState.INCOMING_RINGING) {
            ringtoneManager.stopRinging()
        }

        _activeCallSession.value = current.copy(callState = newState)

        if (newState == CallState.DISCONNECTED) {
            handleCallEnded()
        }
    }

    fun onTelecomCallRemoved(call: Call) {
        ringtoneManager.stopRinging()
        if (currentTelecomCall == call) {
            currentTelecomCall = null
            handleCallEnded()
        }
    }

    /**
     * Launches a realistic simulated incoming call for live privacy testing and demonstrations.
     */
    fun startSimulatedCall(
        callerName: String = "Johnathan Doe",
        callerNumber: String = "+1 (555) 839-2041",
        location: String = "San Francisco, CA"
    ) {
        val isLocked = biometricAuthManager.isDeviceLocked()

        PrivacyLogger.i("Starting simulated incoming call test...")
        val encryptedPayload = cryptoManager.encryptCallerData(
            name = callerName,
            number = callerNumber,
            location = location
        )

        val initialRevealState = determineInitialRevealState(isLocked, encryptedPayload, callerName, callerNumber, location)

        _activeCallSession.value = ActiveCallSession(
            callId = "SIM_${UUID.randomUUID()}",
            callState = CallState.INCOMING_RINGING,
            isSimulated = true,
            encryptedPayload = encryptedPayload,
            revealState = initialRevealState,
            audioRoute = CallAudioRoute.EARPIECE,
            isDeviceLockedAtArrival = isLocked,
            rawCallerName = callerName,
            rawCallerNumber = callerNumber,
            rawLocation = location
        )

        registerScreenOffReceiver()
        ringtoneManager.startRinging()
        startDurationTimer()
    }

    /**
     * Shows the background notification when user leaves the foreground call UI (e.g. presses Home or Back),
     * strictly provided that an incoming call is still currently ringing or active.
     */
    fun showBackgroundCallNotification() {
        val session = _activeCallSession.value ?: run {
            notificationManager.dismissCallNotification()
            return
        }
        if (session.callState == CallState.INCOMING_RINGING || session.callState == CallState.ACTIVE) {
            notificationManager.showPrivateIncomingCallNotification(
                callerName = session.rawCallerName,
                callerNumber = session.rawCallerNumber,
                settings = cachedSettings
            )
        } else {
            notificationManager.dismissCallNotification()
        }
    }

    /**
     * Dismisses the background notification when user returns to foreground call UI.
     */
    fun dismissBackgroundCallNotification() {
        notificationManager.dismissCallNotification()
    }

    private fun determineInitialRevealState(
        isLocked: Boolean,
        encryptedPayload: com.alsaeeddev.data.model.EncryptedCallerPayload,
        callerName: String,
        callerNumber: String,
        location: String
    ): CallerRevealState {
        if (!cachedSettings.isProtectionEnabled) {
            // Protection toggled OFF -> reveal immediately
            return CallerRevealState.Revealed(
                remainingSeconds = cachedSettings.revealTimeoutSeconds,
                totalTimeoutSeconds = cachedSettings.revealTimeoutSeconds,
                callerName = callerName,
                formattedNumber = callerNumber,
                locationOrCarrier = location
            )
        }

        return when (cachedSettings.mode) {
            ProtectionMode.MAX_PRIVACY -> CallerRevealState.Hidden
            ProtectionMode.ALWAYS_PRIVATE -> CallerRevealState.Hidden
            ProtectionMode.LOCK_SCREEN_PRIVACY -> {
                if (isLocked) {
                    CallerRevealState.Hidden
                } else {
                    // Device is already unlocked: show standard identity
                    CallerRevealState.Revealed(
                        remainingSeconds = 0,
                        totalTimeoutSeconds = 0,
                        callerName = callerName,
                        formattedNumber = callerNumber,
                        locationOrCarrier = location
                    )
                }
            }
        }
    }

    /**
     * Triggers decrypted reveal following authenticated confirmation.
     */
    fun onAuthenticationSuccessful() {
        val current = _activeCallSession.value ?: return
        val payload = current.encryptedPayload ?: return

        val decrypted = cryptoManager.decryptCallerData(payload)
        if (decrypted != null) {
            var finalName = decrypted.name
            var finalNumber = decrypted.number

            // If name is not set or generic, resolve fresh from contacts
            if (finalName.isBlank() || finalName == "Incoming Caller" || finalName == "Unknown Caller" || finalName == finalNumber) {
                val contact = ContactLookupHelper.resolveContact(context, finalNumber)
                if (!contact.name.isNullOrBlank()) {
                    finalName = contact.name
                    if (!contact.formattedNumber.isNullOrBlank()) {
                        finalNumber = contact.formattedNumber
                    }
                }
            }

            val timeoutSecs = cachedSettings.revealTimeoutSeconds
            _activeCallSession.value = current.copy(
                rawCallerName = finalName,
                rawCallerNumber = finalNumber,
                revealState = CallerRevealState.Revealed(
                    remainingSeconds = timeoutSecs,
                    totalTimeoutSeconds = timeoutSecs,
                    callerName = finalName,
                    formattedNumber = finalNumber,
                    locationOrCarrier = decrypted.location
                )
            )

            startAutoHideCountdown(timeoutSecs)
        }
    }

    private fun startAutoHideCountdown(seconds: Int) {
        autoHideJob?.cancel()
        if (seconds <= 0) {
            // "Until call ends" mode - no automatic timer countdown
            return
        }

        autoHideJob = scope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000L)
                remaining--
                val current = _activeCallSession.value ?: break
                val reveal = current.revealState
                if (reveal is CallerRevealState.Revealed) {
                    if (remaining == 0) {
                        PrivacyLogger.i("Auto-hide privacy timer expired. Re-masking caller identity.")
                        _activeCallSession.value = current.copy(revealState = CallerRevealState.Expired)
                        delay(600L)
                        _activeCallSession.value = _activeCallSession.value?.copy(revealState = CallerRevealState.Hidden)
                    } else {
                        _activeCallSession.value = current.copy(
                            revealState = reveal.copy(remainingSeconds = remaining)
                        )
                    }
                } else {
                    break
                }
            }
        }
    }

    fun hideCallerNow() {
        autoHideJob?.cancel()
        val current = _activeCallSession.value ?: return
        _activeCallSession.value = current.copy(revealState = CallerRevealState.Hidden)
        PrivacyLogger.i("Caller identity manually re-hidden.")
    }

    fun onScreenOff() {
        if (cachedSettings.autoHideOnScreenOff) {
            PrivacyLogger.i("Screen OFF broadcast received. Auto-locking revealed caller identity.")
            hideCallerNow()
        }
    }

    fun onAppBackgrounded() {
        if (cachedSettings.autoHideOnBackground) {
            PrivacyLogger.i("App backgrounded. Auto-locking revealed caller identity.")
            hideCallerNow()
        }
    }

    fun acceptCall() {
        ringtoneManager.stopRinging()
        notificationManager.dismissCallNotification()
        val current = _activeCallSession.value ?: return
        try {
            currentTelecomCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
        } catch (e: Exception) {
            PrivacyLogger.e("Exception answering telecom call: ${e.message}", e)
        }
        _activeCallSession.value = current.copy(callState = CallState.ACTIVE)
        PrivacyLogger.i("Call accepted by user.")
    }

    fun declineCall() {
        PrivacyLogger.i("declineCall() invoked by user.")
        ringtoneManager.stopRinging()
        notificationManager.dismissCallNotification()
        val current = _activeCallSession.value ?: return

        try {
            currentTelecomCall?.let { telecomCall ->
                if (telecomCall.state == Call.STATE_RINGING) {
                    try {
                        telecomCall.reject(false, null)
                    } catch (e: Exception) {
                        PrivacyLogger.w("telecomCall.reject failed, trying disconnect: ${e.message}")
                        telecomCall.disconnect()
                    }
                } else {
                    telecomCall.disconnect()
                }
            }
        } catch (e: Exception) {
            PrivacyLogger.e("Exception declining/disconnecting telecom call: ${e.message}", e)
        }

        _activeCallSession.value = current.copy(callState = CallState.DISCONNECTED)
        handleCallEnded()
    }

    fun silenceRinger() {
        val current = _activeCallSession.value ?: return
        val newSilenced = !current.isSilenced
        if (newSilenced) {
            ringtoneManager.stopRinging()
            try {
                audioManager?.let { am ->
                    if (am.ringerMode != AudioManager.RINGER_MODE_SILENT) {
                        am.ringerMode = AudioManager.RINGER_MODE_SILENT
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        _activeCallSession.value = current.copy(isSilenced = newSilenced)
        PrivacyLogger.i("Ringer silenced state toggled to $newSilenced.")
    }

    fun toggleSpeaker() {
        val current = _activeCallSession.value ?: return
        val newRoute = if (current.audioRoute == CallAudioRoute.SPEAKER) {
            CallAudioRoute.EARPIECE
        } else {
            CallAudioRoute.SPEAKER
        }

        // 1. InCallService audio routing (for active Telecom calls)
        try {
            inCallServiceInstance?.let { service ->
                val telecomRoute = if (newRoute == CallAudioRoute.SPEAKER) {
                    CallAudioState.ROUTE_SPEAKER
                } else {
                    CallAudioState.ROUTE_EARPIECE
                }
                service.setAudioRoute(telecomRoute)
                PrivacyLogger.i("InCallService audio route set to $telecomRoute")
            }
        } catch (e: Exception) {
            PrivacyLogger.e("Error setting InCallService audio route: ${e.message}", e)
        }

        // 2. Direct Android AudioManager control (works for both simulated calls and fallback)
        try {
            audioManager?.let { am ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (newRoute == CallAudioRoute.SPEAKER) {
                        am.mode = AudioManager.MODE_IN_COMMUNICATION
                        val speakerDevice = am.availableCommunicationDevices.firstOrNull {
                            it.type == android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                        }
                        if (speakerDevice != null) {
                            am.setCommunicationDevice(speakerDevice)
                        }
                    } else {
                        am.clearCommunicationDevice()
                    }
                }
                @Suppress("DEPRECATION")
                am.isSpeakerphoneOn = (newRoute == CallAudioRoute.SPEAKER)
            }
        } catch (e: Exception) {
            PrivacyLogger.e("Error setting AudioManager speakerphone: ${e.message}", e)
        }

        _activeCallSession.value = current.copy(audioRoute = newRoute)
        PrivacyLogger.i("Speaker toggled to $newRoute")
    }

    fun toggleMute() {
        val current = _activeCallSession.value ?: return
        val newMuted = !current.isMuted

        // 1. InCallService mute control
        try {
            inCallServiceInstance?.let { service ->
                service.setMuted(newMuted)
                PrivacyLogger.i("InCallService mute set to $newMuted")
            }
        } catch (e: Exception) {
            PrivacyLogger.e("Error setting InCallService mute: ${e.message}", e)
        }

        // 2. Direct AudioManager microphone mute control
        try {
            audioManager?.let { am ->
                am.isMicrophoneMute = newMuted
            }
        } catch (e: Exception) {
            PrivacyLogger.e("Error setting AudioManager microphone mute: ${e.message}", e)
        }

        _activeCallSession.value = current.copy(isMuted = newMuted)
        PrivacyLogger.i("Mute toggled to $newMuted")
    }

    fun endCall() {
        declineCall()
    }

    private fun handleCallEnded() {
        notificationManager.dismissCallNotification()
        unregisterScreenOffReceiver()
        autoHideJob?.cancel()
        durationTimerJob?.cancel()

        // Reset audio hardware state
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager?.clearCommunicationDevice()
            }
            @Suppress("DEPRECATION")
            audioManager?.isSpeakerphoneOn = false
            audioManager?.isMicrophoneMute = false
            audioManager?.mode = AudioManager.MODE_NORMAL
        } catch (e: Exception) {
            // ignore
        }

        scope.launch {
            delay(1200L) // Brief UI animation transition
            _activeCallSession.value = null
        }
    }

    private fun startDurationTimer() {
        durationTimerJob?.cancel()
        durationTimerJob = scope.launch {
            while (true) {
                delay(1000L)
                val current = _activeCallSession.value ?: break
                if (current.callState == CallState.ACTIVE) {
                    _activeCallSession.value = current.copy(
                        callDurationSeconds = current.callDurationSeconds + 1
                    )
                }
            }
        }
    }
}
