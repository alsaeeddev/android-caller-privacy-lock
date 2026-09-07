package com.alsaeeddev.presentation.call

import android.app.Application
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import com.alsaeeddev.data.model.ActiveCallSession
import com.alsaeeddev.data.model.CallerRevealState
import com.alsaeeddev.data.model.ProtectionSettings
import com.alsaeeddev.data.repository.SettingsRepository
import com.alsaeeddev.security.BiometricAuthManager
import com.alsaeeddev.security.PrivacyLogger
import com.alsaeeddev.telecom.CallManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class IncomingCallViewModel(application: Application) : AndroidViewModel(application) {

    private val callManager = CallManager.getInstance(application)
    private val biometricAuthManager = BiometricAuthManager(application)
    private val settingsRepository = SettingsRepository(application)

    val activeCall: StateFlow<ActiveCallSession?> = callManager.activeCallSession

    val settings: StateFlow<ProtectionSettings> = settingsRepository.settingsFlow

    private val _authFeedbackMessage = MutableStateFlow<String?>(null)
    val authFeedbackMessage: StateFlow<String?> = _authFeedbackMessage.asStateFlow()

    private val _showNoSecurityDialog = MutableStateFlow(false)
    val showNoSecurityDialog: StateFlow<Boolean> = _showNoSecurityDialog.asStateFlow()

    fun dismissNoSecurityDialog() {
        _showNoSecurityDialog.value = false
    }

    fun clearFeedbackMessage() {
        _authFeedbackMessage.value = null
    }

    private var activeBiometricPrompt: androidx.biometric.BiometricPrompt? = null

    /**
     * Authenticates the user before decrypting the caller identity:
     * 1. Biometric (Fingerprint/Face) is 1st Priority.
     * 2. Device Credential (PIN/Pattern/Password) is 2nd Priority.
     * Caller is only revealed upon successful authentication.
     */
    fun requestRevealAuthentication(
        activity: FragmentActivity?,
        onLaunchDeviceCredentialIntent: (Intent) -> Unit
    ) {
        val current = activeCall.value ?: return
        if (current.revealState is CallerRevealState.Revealed) return

        if (activity == null) {
            PrivacyLogger.w("FragmentActivity is null during reveal authentication request.")
            _authFeedbackMessage.value = "Authentication window not ready. Please try again."
            return
        }

        val currentSettings = settings.value
        PrivacyLogger.i("Prompting authentication with requireBiometrics=${currentSettings.requireBiometrics}, allowFallback=${currentSettings.allowDeviceCredentialFallback}...")

        try {
            activeBiometricPrompt?.cancelAuthentication()
        } catch (e: Exception) {
            // ignore
        }

        activeBiometricPrompt = biometricAuthManager.promptAuthentication(
            activity = activity,
            title = "Reveal Private Caller",
            subtitle = if (currentSettings.requireBiometrics) "Touch fingerprint sensor or use Device PIN" else "Confirm Device Screen Lock (PIN/Pattern)",
            requireBiometrics = currentSettings.requireBiometrics,
            allowDeviceCredentialFallback = currentSettings.allowDeviceCredentialFallback,
            onSuccess = {
                PrivacyLogger.i("Authentication verified successfully. Revealing caller.")
                _authFeedbackMessage.value = null
                callManager.onAuthenticationSuccessful()
            },
            onError = { errorCode, errString ->
                PrivacyLogger.w("Authentication event [$errorCode]: $errString")
                // Do NOT display "Authentication cancelled by user" error banner when prompt is cancelled or dismissed
                if (errorCode != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != androidx.biometric.BiometricPrompt.ERROR_CANCELED &&
                    errorCode != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON
                ) {
                    _authFeedbackMessage.value = errString.toString()
                } else {
                    _authFeedbackMessage.value = null
                }
            },
            onFailed = {
                PrivacyLogger.w("Authentication failed (fingerprint mismatch).")
                _authFeedbackMessage.value = "Fingerprint not recognized. Try again."
            },
            onFallbackToDeviceCredential = { intent ->
                PrivacyLogger.i("Launching device screen lock (PIN/Pattern)...")
                onLaunchDeviceCredentialIntent(intent)
            },
            onNoSecurityConfigured = {
                PrivacyLogger.w("No lock screen or biometrics set on device.")
                _showNoSecurityDialog.value = true
            }
        )
    }

    /**
     * Automatically activates fingerprint sensor listening in the background
     * so user can simply place their finger on the sensor to reveal caller identity without pressing button.
     */
    fun startPassiveBiometricListening(
        activity: FragmentActivity?,
        onLaunchDeviceCredentialIntent: (Intent) -> Unit
    ) {
        val current = activeCall.value ?: return
        if (current.revealState is CallerRevealState.Revealed) return
        if (!biometricAuthManager.hasBiometricsEnrolled()) return

        requestRevealAuthentication(
            activity = activity,
            onLaunchDeviceCredentialIntent = onLaunchDeviceCredentialIntent
        )
    }

    fun stopBiometricListening() {
        try {
            activeBiometricPrompt?.cancelAuthentication()
        } catch (e: Exception) {
            // ignore
        }
        activeBiometricPrompt = null
    }

    fun onDeviceCredentialResult(resultCode: Int) {
        if (resultCode == android.app.Activity.RESULT_OK) {
            PrivacyLogger.i("Device PIN/Pattern/Password confirmed successfully. Revealing caller.")
            _authFeedbackMessage.value = null
            callManager.onAuthenticationSuccessful()
        } else {
            PrivacyLogger.w("Device credential verification was cancelled or failed.")
            _authFeedbackMessage.value = null
        }
    }

    fun hideCallerNow() {
        callManager.hideCallerNow()
    }

    fun acceptCall() {
        callManager.acceptCall()
    }

    fun declineCall() {
        callManager.declineCall()
    }

    fun silenceCall() {
        callManager.silenceRinger()
    }

    fun toggleSpeaker() {
        callManager.toggleSpeaker()
    }

    fun toggleMute() {
        callManager.toggleMute()
    }
}
