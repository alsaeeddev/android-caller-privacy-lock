package com.alsaeeddev.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alsaeeddev.security.PrivacyLogger
import com.alsaeeddev.telecom.CallManager

class ScreenOffReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_SCREEN_OFF) {
            PrivacyLogger.i("Screen OFF broadcast received.")
            CallManager.getInstance(context).onScreenOff()
        }
    }
}
