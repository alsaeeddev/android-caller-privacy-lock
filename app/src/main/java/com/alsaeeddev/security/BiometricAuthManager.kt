package com.alsaeeddev.security

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

sealed interface BiometricSupportStatus {
    data object Available : BiometricSupportStatus
    data class DeviceCredentialOnly(val message: String) : BiometricSupportStatus
    data class Unavailable(val reason: String) : BiometricSupportStatus
}

class BiometricAuthManager(private val context: Context) {

    private val biometricManager = BiometricManager.from(context)
    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

    fun hasBiometricsEnrolled(): Boolean {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun isDeviceSecure(): Boolean {
        return keyguardManager?.isDeviceSecure == true
    }

    fun isDeviceLocked(): Boolean {
        return keyguardManager?.isDeviceLocked == true || keyguardManager?.isKeyguardLocked == true
    }

    fun checkBiometricSupport(): BiometricSupportStatus {
        if (hasBiometricsEnrolled()) {
            return BiometricSupportStatus.Available
        }
        if (isDeviceSecure()) {
            return BiometricSupportStatus.DeviceCredentialOnly("Biometrics not enrolled. Device PIN/Pattern/Password will be used.")
        }
        return BiometricSupportStatus.Unavailable("No screen lock or biometric credentials enrolled on device.")
    }

    fun createConfirmDeviceCredentialIntent(
        title: String = "Reveal Private Caller",
        description: String = "Enter your device PIN, Pattern, or Password to reveal caller identity"
    ): Intent? {
        return keyguardManager?.createConfirmDeviceCredentialIntent(title, description)
    }

    /**
     * Prompts for authentication with priority:
     * 1st priority: Lockscreen native authentication (Fingerprint / Face / PIN / Pattern via Keyguard) when locked.
     * 2nd priority: BiometricPrompt (Fingerprint / Face) when unlocked.
     * 3rd priority: Device Credential Intent (PIN / Pattern / Password fallback).
     *
     * If neither is supported or enrolled, onNoSecurityConfigured is called.
     * Identity is ONLY revealed if user passes verification.
     */
    fun promptAuthentication(
        activity: FragmentActivity,
        title: String = "Reveal Private Caller",
        subtitle: String = "Place your finger on sensor or authenticate to reveal",
        requireBiometrics: Boolean = true,
        allowDeviceCredentialFallback: Boolean = true,
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit,
        onFailed: () -> Unit,
        onFallbackToDeviceCredential: (Intent) -> Unit,
        onNoSecurityConfigured: () -> Unit
    ): BiometricPrompt? {
        val hasBio = hasBiometricsEnrolled()
        val isSecure = isDeviceSecure()
        val isLocked = isDeviceLocked()

        PrivacyLogger.i("promptAuthentication called: hasBio=$hasBio, isSecure=$isSecure, isLocked=$isLocked")

        // If device has neither biometric nor device lock set
        if (!hasBio && !isSecure) {
            PrivacyLogger.w("No biometric or device screen lock configured on this device. Auto-revealing.")
            onNoSecurityConfigured()
            return null
        }

        // On Lock Screen (Keyguard Active): Use native KeyguardManager dismiss callback which natively
        // captures fingerprint sensor and device PIN/Pattern from the lock screen.
        if (isLocked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && keyguardManager != null) {
            try {
                PrivacyLogger.i("Device is locked. Requesting Keyguard dismissal with Fingerprint/PIN...")
                keyguardManager.requestDismissKeyguard(
                    activity,
                    object : KeyguardManager.KeyguardDismissCallback() {
                        override fun onDismissSucceeded() {
                            super.onDismissSucceeded()
                            PrivacyLogger.i("Keyguard dismiss succeeded via Lockscreen Fingerprint/PIN. Revealing caller!")
                            activity.runOnUiThread {
                                onSuccess()
                            }
                        }

                        override fun onDismissCancelled() {
                            super.onDismissCancelled()
                            PrivacyLogger.w("Keyguard dismiss cancelled by user.")
                            activity.runOnUiThread {
                                onError(BiometricPrompt.ERROR_USER_CANCELED, "Authentication cancelled")
                            }
                        }

                        override fun onDismissError() {
                            super.onDismissError()
                            PrivacyLogger.w("Keyguard dismiss error. Falling back to BiometricPrompt/Credential...")
                            activity.runOnUiThread {
                                launchBiometricPrompt(
                                    activity = activity,
                                    title = title,
                                    subtitle = subtitle,
                                    hasBio = hasBio,
                                    isSecure = isSecure,
                                    allowDeviceCredentialFallback = allowDeviceCredentialFallback,
                                    onSuccess = onSuccess,
                                    onError = onError,
                                    onFailed = onFailed,
                                    onFallbackToDeviceCredential = onFallbackToDeviceCredential
                                )
                            }
                        }
                    }
                )
                return null
            } catch (e: Exception) {
                PrivacyLogger.e("Exception during requestDismissKeyguard: ${e.message}", e)
            }
        }

        // When not locked or if Keyguard dismiss is not used, launch BiometricPrompt
        return launchBiometricPrompt(
            activity = activity,
            title = title,
            subtitle = subtitle,
            hasBio = hasBio,
            isSecure = isSecure,
            allowDeviceCredentialFallback = allowDeviceCredentialFallback,
            onSuccess = onSuccess,
            onError = onError,
            onFailed = onFailed,
            onFallbackToDeviceCredential = onFallbackToDeviceCredential
        )
    }

    private fun launchBiometricPrompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        hasBio: Boolean,
        isSecure: Boolean,
        allowDeviceCredentialFallback: Boolean,
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit,
        onFailed: () -> Unit,
        onFallbackToDeviceCredential: (Intent) -> Unit
    ): BiometricPrompt? {
        // If biometrics are not enrolled or not supported, directly launch device credential
        if (!hasBio && allowDeviceCredentialFallback && isSecure) {
            val credentialIntent = createConfirmDeviceCredentialIntent(title, subtitle)
            if (credentialIntent != null) {
                PrivacyLogger.i("Biometrics unavailable. Launching device PIN/Pattern credential...")
                onFallbackToDeviceCredential(credentialIntent)
                return null
            }
        }

        try {
            val executor = ContextCompat.getMainExecutor(activity)

            val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setConfirmationRequired(false)

            if (hasBio) {
                promptInfoBuilder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
                if (allowDeviceCredentialFallback && isSecure) {
                    promptInfoBuilder.setNegativeButtonText("Use PIN / Password")
                } else {
                    promptInfoBuilder.setNegativeButtonText("Cancel")
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && allowDeviceCredentialFallback) {
                promptInfoBuilder.setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            } else {
                promptInfoBuilder.setNegativeButtonText("Cancel")
            }

            val biometricPrompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        PrivacyLogger.i("Biometric authentication succeeded.")
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        PrivacyLogger.w("Biometric authentication error [$errorCode]: $errString")

                        // If user clicked negative button ("Use PIN / Password")
                        if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON && allowDeviceCredentialFallback && isSecure) {
                            val credentialIntent = createConfirmDeviceCredentialIntent(title, subtitle)
                            if (credentialIntent != null) {
                                PrivacyLogger.i("User selected PIN/Pattern fallback from biometric prompt.")
                                onFallbackToDeviceCredential(credentialIntent)
                                return
                            }
                        }

                        // If hardware unavailable / not enrolled and fallback allowed
                        if (allowDeviceCredentialFallback &&
                            (errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS ||
                                    errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT ||
                                    errorCode == BiometricPrompt.ERROR_HW_UNAVAILABLE ||
                                    errorCode == BiometricPrompt.ERROR_UNABLE_TO_PROCESS) &&
                            isSecure
                        ) {
                            val credentialIntent = createConfirmDeviceCredentialIntent(title, subtitle)
                            if (credentialIntent != null) {
                                PrivacyLogger.i("Launching fallback device credential intent (PIN/Pattern)...")
                                onFallbackToDeviceCredential(credentialIntent)
                                return
                            }
                        }

                        onError(errorCode, errString)
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        PrivacyLogger.w("Authentication failed (unrecognized fingerprint).")
                        onFailed()
                    }
                }
            )

            biometricPrompt.authenticate(promptInfoBuilder.build())
            return biometricPrompt
        } catch (e: Exception) {
            PrivacyLogger.e("Exception during biometric prompt: ${e.message}", e)
            if (allowDeviceCredentialFallback && isSecure) {
                val credentialIntent = createConfirmDeviceCredentialIntent(title, subtitle)
                if (credentialIntent != null) {
                    onFallbackToDeviceCredential(credentialIntent)
                    return null
                }
            }
            onError(BiometricPrompt.ERROR_UNABLE_TO_PROCESS, e.message ?: "Authentication error")
            return null
        }
    }
}
