package com.alsaeeddev.presentation.call

import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.alsaeeddev.data.model.CallState
import com.alsaeeddev.security.PrivacyLogger
import com.alsaeeddev.telecom.CallManager
import com.alsaeeddev.ui.theme.DeepObsidian
import com.alsaeeddev.ui.theme.MyApplicationTheme

class IncomingCallActivity : FragmentActivity() {

    private val viewModel: IncomingCallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupLockScreenFlags()
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        // Handle Back button press: minimize call screen to background and show notification
        onBackPressedDispatcher.addCallback(this) {
            val currentCall = CallManager.getInstance(applicationContext).activeCallSession.value
            if (currentCall != null && (currentCall.callState == CallState.INCOMING_RINGING || currentCall.callState == CallState.ACTIVE)) {
                PrivacyLogger.i("Back pressed on call UI: moving to background and showing notification.")
                moveTaskToBack(true)
                CallManager.getInstance(applicationContext).showBackgroundCallNotification()
            } else {
                finish()
            }
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DeepObsidian
                ) {
                    val activeCall by viewModel.activeCall.collectAsState()

                    LaunchedEffect(activeCall) {
                        val session = activeCall
                        if (session == null || session.callState == CallState.DISCONNECTED || session.callState == CallState.IDLE) {
                            PrivacyLogger.i("Active call session ended: finishing IncomingCallActivity.")
                            CallManager.getInstance(applicationContext).dismissBackgroundCallNotification()
                            finish()
                        }
                    }

                    activeCall?.let { session ->
                        IncomingCallScreen(
                            callSession = session,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // When full Call UI is visible on screen, dismiss the notification bar icon to prevent duplicate clutter
        PrivacyLogger.i("IncomingCallActivity onResume: Call UI is visible, dismissing notification.")
        CallManager.getInstance(applicationContext).dismissBackgroundCallNotification()
    }

    override fun onStop() {
        super.onStop()
        // When user leaves the Call UI (presses Home, Back, or switches apps), show notification if call is still alive
        val currentCall = CallManager.getInstance(applicationContext).activeCallSession.value
        if (currentCall != null && (currentCall.callState == CallState.INCOMING_RINGING || currentCall.callState == CallState.ACTIVE)) {
            PrivacyLogger.i("IncomingCallActivity onStop: User backgrounded active/ringing call, showing notification.")
            CallManager.getInstance(applicationContext).showBackgroundCallNotification()
        } else {
            CallManager.getInstance(applicationContext).dismissBackgroundCallNotification()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val currentCall = CallManager.getInstance(applicationContext).activeCallSession.value
        if (currentCall == null || currentCall.callState == CallState.DISCONNECTED || currentCall.callState == CallState.IDLE) {
            CallManager.getInstance(applicationContext).dismissBackgroundCallNotification()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            viewModel.silenceCall()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
