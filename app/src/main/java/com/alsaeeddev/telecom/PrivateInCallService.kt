package com.alsaeeddev.telecom

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import com.alsaeeddev.data.model.CallAudioRoute
import com.alsaeeddev.notification.CallNotificationManager
import com.alsaeeddev.presentation.call.IncomingCallActivity
import com.alsaeeddev.security.PrivacyLogger

class PrivateInCallService : InCallService() {

    private lateinit var callManager: CallManager
    private lateinit var notificationManager: CallNotificationManager

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            PrivacyLogger.i("Telecom Call state changed to: $state")
            callManager.onTelecomCallStateChanged(call, state)
            if (state == Call.STATE_DISCONNECTED) {
                notificationManager.dismissCallNotification()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        callManager = CallManager.getInstance(applicationContext)
        notificationManager = CallNotificationManager(applicationContext)
        callManager.registerInCallService(this)
        PrivacyLogger.i("PrivateInCallService created and registered with CallManager.")
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        audioState?.let {
            val route = when (it.route) {
                CallAudioState.ROUTE_SPEAKER -> CallAudioRoute.SPEAKER
                CallAudioState.ROUTE_BLUETOOTH -> CallAudioRoute.BLUETOOTH
                CallAudioState.ROUTE_WIRED_HEADSET -> CallAudioRoute.WIRED_HEADSET
                else -> CallAudioRoute.EARPIECE
            }
            PrivacyLogger.i("Telecom audio state changed: isMuted=${it.isMuted}, route=$route")
            callManager.updateAudioStateFromTelecom(it.isMuted, route)
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        PrivacyLogger.i("Telecom onCallAdded received.")
        callManager.registerInCallService(this)
        call.registerCallback(callCallback)
        callManager.onTelecomCallAdded(call)

        // Launch full-screen incoming call UI
        val intent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        PrivacyLogger.i("Telecom onCallRemoved received.")
        call.unregisterCallback(callCallback)
        callManager.onTelecomCallRemoved(call)
        notificationManager.dismissCallNotification()
    }

    override fun onDestroy() {
        super.onDestroy()
        callManager.unregisterInCallService(this)
        notificationManager.dismissCallNotification()
    }
}
