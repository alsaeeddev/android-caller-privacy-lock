package com.alsaeeddev.presentation.diagnostics

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alsaeeddev.data.model.CheckStatus
import com.alsaeeddev.data.model.DiagnosticItem
import com.alsaeeddev.data.model.OemCompatibilityInfo
import com.alsaeeddev.security.DiagnosticsManager
import com.alsaeeddev.telecom.RoleHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DiagnosticsUiState(
    val isRunning: Boolean = false,
    val items: List<DiagnosticItem> = emptyList(),
    val oemInfo: OemCompatibilityInfo? = null,
    val overallStatus: CheckStatus = CheckStatus.CHECKING,
    val passedCount: Int = 0,
    val totalCount: Int = 0
)

class DiagnosticsViewModel(application: Application) : AndroidViewModel(application) {

    private val diagnosticsManager = DiagnosticsManager(application)
    private val roleHelper = RoleHelper(application)

    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    init {
        runFullDiagnostics()
    }

    fun runFullDiagnostics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRunning = true)
            delay(400L) // Visual feedback for diagnostic scan

            val items = diagnosticsManager.runDiagnostics()
            val oem = diagnosticsManager.getOemCompatibility()

            val passed = items.count { it.status == CheckStatus.PASSED }
            val hasFailed = items.any { it.status == CheckStatus.FAILED }
            val hasWarning = items.any { it.status == CheckStatus.WARNING }

            val overall = when {
                hasFailed -> CheckStatus.FAILED
                hasWarning -> CheckStatus.WARNING
                else -> CheckStatus.PASSED
            }

            _uiState.value = DiagnosticsUiState(
                isRunning = false,
                items = items,
                oemInfo = oem,
                overallStatus = overall,
                passedCount = passed,
                totalCount = items.size
            )
        }
    }

    fun getRequestDialerIntent(): Intent? {
        return roleHelper.createRequestDialerRoleIntent()
    }
}
