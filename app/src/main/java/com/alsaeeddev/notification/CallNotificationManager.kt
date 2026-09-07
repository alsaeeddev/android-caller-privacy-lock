package com.alsaeeddev.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.alsaeeddev.R
import com.alsaeeddev.presentation.call.IncomingCallActivity
import com.alsaeeddev.security.PrivacyLogger

class CallNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "private_call_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_ACCEPT = "com.alsaeeddev.ACTION_ACCEPT_CALL"
        const val ACTION_DECLINE = "com.alsaeeddev.ACTION_DECLINE_CALL"
        const val ACTION_DISMISS = "com.alsaeeddev.ACTION_DISMISS_NOTIFICATION"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                setShowBadge(false)
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showPrivateIncomingCallNotification(
        callerName: String = "",
        callerNumber: String = "",
        settings: com.alsaeeddev.data.model.ProtectionSettings? = null
    ) {
        PrivacyLogger.i("Posting incoming call notification with privacy settings: hideNotificationContent=${settings?.hideNotificationContent}, hideName=${settings?.hideCallerName}, hideNumber=${settings?.hideCallerNumber}")

        val fullScreenIntent = Intent(context, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val declineIntent = Intent(context, CallNotificationReceiver::class.java).apply {
            action = ACTION_DECLINE
        }
        val declinePendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val deleteIntent = Intent(context, CallNotificationReceiver::class.java).apply {
            action = ACTION_DISMISS
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isProtectionEnabled = settings?.isProtectionEnabled ?: true
        val hideNotificationContent = settings?.hideNotificationContent ?: true
        val hideCallerName = settings?.hideCallerName ?: true
        val hideCallerNumber = settings?.hideCallerNumber ?: true

        val title = if (!isProtectionEnabled || !hideNotificationContent && !hideCallerName && callerName.isNotBlank()) {
            "Incoming Call: $callerName"
        } else {
            context.getString(R.string.notification_private_call_title)
        }

        val text = if (!isProtectionEnabled || !hideNotificationContent && !hideCallerNumber && callerNumber.isNotBlank()) {
            callerNumber
        } else {
            context.getString(R.string.notification_private_call_text)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(if (hideNotificationContent) NotificationCompat.VISIBILITY_PRIVATE else NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(false)
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Decline",
                declinePendingIntent
            )

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun dismissCallNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
