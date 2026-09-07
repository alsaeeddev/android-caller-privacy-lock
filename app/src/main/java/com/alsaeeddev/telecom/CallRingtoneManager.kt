package com.alsaeeddev.telecom

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import com.alsaeeddev.security.PrivacyLogger

/**
 * Manages standard incoming call ringing and vibration according to the device's
 * ringer mode and system ringtone settings.
 */
class CallRingtoneManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var mediaPlayer: MediaPlayer? = null
    private var fallbackRingtone: Ringtone? = null
    private var isRinging = false

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    // Standard phone ringing vibration pattern: delay 0ms, vibrate 1000ms, pause 1000ms
    private val ringVibrationPattern = longArrayOf(0, 1000, 1000)

    /**
     * Starts ringing and/or vibration based on the current system ringer mode:
     * - NORMAL: Plays default phone ringtone in a continuous loop + vibrates
     * - VIBRATE: Vibrates in standard phone ringing cadence without sound
     * - SILENT: Suppresses both ringtone sound and vibration
     */
    @Synchronized
    fun startRinging() {
        if (isRinging) return
        isRinging = true

        val ringerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
        PrivacyLogger.i("CallRingtoneManager: startRinging triggered (Ringer mode: $ringerMode)")

        when (ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> {
                playSystemRingtone()
                startRingingVibration()
            }
            AudioManager.RINGER_MODE_VIBRATE -> {
                startRingingVibration()
            }
            AudioManager.RINGER_MODE_SILENT -> {
                PrivacyLogger.i("CallRingtoneManager: Phone is SILENT. Suppressing ringtone sound & vibration.")
            }
        }
    }

    private fun playSystemRingtone() {
        try {
            val ringtoneUri: Uri = RingtoneManager.getActualDefaultRingtoneUri(
                context,
                RingtoneManager.TYPE_RINGTONE
            ) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: Settings.System.DEFAULT_RINGTONE_URI

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, ringtoneUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setLegacyStreamType(AudioManager.STREAM_RING)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
            PrivacyLogger.i("CallRingtoneManager: MediaPlayer started playing system ringtone in loop.")
        } catch (e: Exception) {
            PrivacyLogger.w("CallRingtoneManager: MediaPlayer failed (${e.message}), trying Ringtone fallback.")
            try {
                val ringtoneUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: Settings.System.DEFAULT_RINGTONE_URI
                fallbackRingtone = RingtoneManager.getRingtone(context, ringtoneUri)?.apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        isLooping = true
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        audioAttributes = AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setLegacyStreamType(AudioManager.STREAM_RING)
                            .build()
                    }
                    play()
                }
            } catch (ex: Exception) {
                PrivacyLogger.e("CallRingtoneManager: Failed to play fallback ringtone: ${ex.message}", ex)
            }
        }
    }

    private fun startRingingVibration() {
        try {
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(ringVibrationPattern, 0)
                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .build()
                    vibrator.vibrate(effect, audioAttributes)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(ringVibrationPattern, 0)
                }
                PrivacyLogger.i("CallRingtoneManager: Incoming call vibration started.")
            }
        } catch (e: Exception) {
            PrivacyLogger.w("CallRingtoneManager: Vibration failed: ${e.message}")
        }
    }

    /**
     * Immediately stops ringtone sound and cancels vibration.
     */
    @Synchronized
    fun stopRinging() {
        if (!isRinging) return
        isRinging = false
        PrivacyLogger.i("CallRingtoneManager: Stopping ringtone playback & vibration.")

        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.reset()
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            PrivacyLogger.w("CallRingtoneManager: Error stopping MediaPlayer: ${e.message}")
        }

        try {
            fallbackRingtone?.let {
                if (it.isPlaying) {
                    it.stop()
                }
            }
            fallbackRingtone = null
        } catch (e: Exception) {
            PrivacyLogger.w("CallRingtoneManager: Error stopping fallback Ringtone: ${e.message}")
        }

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            PrivacyLogger.w("CallRingtoneManager: Error stopping vibrator: ${e.message}")
        }
    }
}
