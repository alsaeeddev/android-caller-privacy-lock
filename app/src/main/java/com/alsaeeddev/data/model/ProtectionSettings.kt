package com.alsaeeddev.data.model

enum class ProtectionMode(
    val title: String,
    val description: String,
    val securityLevel: String
) {
    MAX_PRIVACY(
        title = "Maximum Privacy",
        description = "Always hide phone number, caller name, and contact photo on both lock screen and when unlocked. Identity is only revealed after explicit biometric or device authentication.",
        securityLevel = "STRICT"
    ),
    LOCK_SCREEN_PRIVACY(
        title = "Lock Screen Privacy",
        description = "Hide caller identity while the device is locked. When device is already unlocked by the owner, standard caller info can be presented.",
        securityLevel = "BALANCED"
    ),
    ALWAYS_PRIVATE(
        title = "Always Private",
        description = "Never expose caller identity automatically under any circumstances. Explicit authentication prompt is mandatory on every call.",
        securityLevel = "HIGH"
    )
}

enum class RevealTimeoutOption(val seconds: Int, val label: String) {
    FIVE_SECONDS(5, "5 seconds"),
    TEN_SECONDS(10, "10 seconds (Recommended)"),
    FIFTEEN_SECONDS(15, "15 seconds"),
    THIRTY_SECONDS(30, "30 seconds"),
    UNTIL_CALL_ENDS(0, "Until call ends")
}

data class ProtectionSettings(
    val isProtectionEnabled: Boolean = true,
    val mode: ProtectionMode = ProtectionMode.MAX_PRIVACY,
    val hideCallerName: Boolean = true,
    val hideCallerNumber: Boolean = true,
    val hideContactPhoto: Boolean = true,
    val revealTimeoutSeconds: Int = 10,
    val requireBiometrics: Boolean = true,
    val allowDeviceCredentialFallback: Boolean = true,
    val hideOnLockScreen: Boolean = true,
    val hideNotificationContent: Boolean = true,
    val autoHideOnScreenOff: Boolean = true,
    val autoHideOnBackground: Boolean = true,
    val autoHideAfterCallEnds: Boolean = true,
    val isOnboardingCompleted: Boolean = false
)
