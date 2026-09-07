package com.alsaeeddev.data.model

enum class CallState {
    IDLE,
    INCOMING_RINGING,
    CONNECTING,
    ACTIVE,
    ON_HOLD,
    DISCONNECTED
}

enum class CallAudioRoute {
    EARPIECE,
    SPEAKER,
    BLUETOOTH,
    WIRED_HEADSET
}

sealed interface CallerRevealState {
    data object Hidden : CallerRevealState
    data object Authenticating : CallerRevealState
    data class Revealed(
        val remainingSeconds: Int,
        val totalTimeoutSeconds: Int,
        val callerName: String,
        val formattedNumber: String,
        val locationOrCarrier: String? = null
    ) : CallerRevealState
    data object Expired : CallerRevealState
}

data class EncryptedCallerPayload(
    val iv: ByteArray,
    val cipherText: ByteArray,
    val authTagLength: Int = 128
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EncryptedCallerPayload
        if (!iv.contentEquals(other.iv)) return false
        if (!cipherText.contentEquals(other.cipherText)) return false
        return authTagLength == other.authTagLength
    }

    override fun hashCode(): Int {
        var result = iv.contentHashCode()
        result = 31 * result + cipherText.contentHashCode()
        result = 31 * result + authTagLength
        return result
    }
}

data class ActiveCallSession(
    val callId: String,
    val callState: CallState = CallState.IDLE,
    val isSimulated: Boolean = false,
    val encryptedPayload: EncryptedCallerPayload? = null,
    val revealState: CallerRevealState = CallerRevealState.Hidden,
    val audioRoute: CallAudioRoute = CallAudioRoute.EARPIECE,
    val isMuted: Boolean = false,
    val isSilenced: Boolean = false,
    val isOnHold: Boolean = false,
    val callDurationSeconds: Long = 0L,
    val startTimeMillis: Long = System.currentTimeMillis(),
    val isDeviceLockedAtArrival: Boolean = false,
    val rawCallerName: String = "",
    val rawCallerNumber: String = "",
    val rawLocation: String = ""
)
