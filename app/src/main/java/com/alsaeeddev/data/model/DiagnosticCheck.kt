package com.alsaeeddev.data.model

enum class CheckStatus {
    PASSED,
    WARNING,
    FAILED,
    CHECKING
}

data class DiagnosticItem(
    val id: String,
    val title: String,
    val category: String,
    val status: CheckStatus,
    val summary: String,
    val technicalDetails: String,
    val remediationActionLabel: String? = null,
    val remediationActionType: String? = null
)

data class OemCompatibilityInfo(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val androidVersion: Int,
    val androidRelease: String,
    val isDefaultDialerAllowed: Boolean,
    val isCustomRomKnown: Boolean,
    val oemNotes: String,
    val protectionLevel: String // "Full Protection" or "Limited Protection"
)
