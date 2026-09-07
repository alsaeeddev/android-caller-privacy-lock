package com.alsaeeddev.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alsaeeddev.security.PrivacyLogger
import com.alsaeeddev.telecom.CallManager

/**
 * BroadcastReceiver to handle notification action buttons (Accept / Decline)
 * and notification dismissal / clear events.
 */
class CallNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        PrivacyLogger.i("CallNotificationReceiver received action: $action")

        val callManager = CallManager.getInstance(context.applicationContext)
        val notificationManager = CallNotificationManager(context.applicationContext)

        when (action) {
            CallNotificationManager.ACTION_ACCEPT -> {
                callManager.acceptCall()
                notificationManager.dismissCallNotification()
            }
            CallNotificationManager.ACTION_DECLINE -> {
                callManager.declineCall()
                notificationManager.dismissCallNotification()
            }
            CallNotificationManager.ACTION_DISMISS -> {
                notificationManager.dismissCallNotification()
                callManager.declineCall()
            }
        }
    }
}
