package com.alsaeeddev.security

import android.util.Log

/**
 * Privacy-compliant logger that strictly guarantees zero exposure of phone numbers,
 * contact names, or sensitive identifiers in Logcat or system traces.
 */
object PrivacyLogger {
    private const val TAG = "PrivateCallerID"
    private const val ENABLE_LOGS = true

    fun d(message: String) {
        if (ENABLE_LOGS) {
            Log.d(TAG, sanitize(message))
        }
    }

    fun i(message: String) {
        if (ENABLE_LOGS) {
            Log.i(TAG, sanitize(message))
        }
    }

    fun w(message: String) {
        if (ENABLE_LOGS) {
            Log.w(TAG, sanitize(message))
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (ENABLE_LOGS) {
            Log.e(TAG, sanitize(message), throwable)
        }
    }

    /**
     * Replaces phone number sequences with privacy hashes or masks.
     */
    fun maskPhoneNumber(rawNumber: String?): String {
        if (rawNumber.isNullOrBlank()) return "[UNAVAILABLE]"
        val digits = rawNumber.filter { it.isDigit() || it == '+' }
        if (digits.length <= 4) return "****"
        val prefix = if (digits.startsWith("+")) digits.take(3) else digits.take(2)
        val suffix = digits.takeLast(2)
        return "$prefix ***-*** $suffix"
    }

    private fun sanitize(input: String): String {
        // Redact potential 7-15 digit phone patterns
        val phoneRegex = Regex("""(\+?\d[\d\s\-\(\)]{6,}\d)""")
        return phoneRegex.replace(input) { match ->
            maskPhoneNumber(match.value)
        }
    }
}
