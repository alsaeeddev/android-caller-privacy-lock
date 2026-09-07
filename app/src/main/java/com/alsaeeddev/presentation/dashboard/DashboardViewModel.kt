package com.alsaeeddev.presentation.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alsaeeddev.data.model.ActiveCallSession
import com.alsaeeddev.data.model.DiagnosticItem
import com.alsaeeddev.data.model.ProtectionMode
import com.alsaeeddev.data.model.ProtectionSettings
import com.alsaeeddev.data.repository.SettingsRepository
import com.alsaeeddev.security.BiometricAuthManager
import com.alsaeeddev.security.DiagnosticsManager
import com.alsaeeddev.telecom.CallManager
import com.alsaeeddev.telecom.RoleHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isDefaultDialer: Boolean = false,
    val isDeviceSecure: Boolean = false,
    val diagnosticsSummary: List<DiagnosticItem> = emptyList(),
    val quickStatusText: String = "Initializing..."
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val callManager = CallManager.getInstance(application)
    private val roleHelper = RoleHelper(application)
    private val biometricAuthManager = BiometricAuthManager(application)
    private val diagnosticsManager = DiagnosticsManager(application)

    val settings: StateFlow<ProtectionSettings> = settingsRepository.settingsFlow

    val activeCall: StateFlow<ActiveCallSession?> = callManager.activeCallSession

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refreshSystemStatus()
        viewModelScope.launch {
            settings.collect { currentSettings ->
                callManager.updateSettings(currentSettings)
            }
        }
    }

    fun refreshSystemStatus() {
        val isDialer = roleHelper.isDefaultDialer()
        val isSecure = biometricAuthManager.isDeviceSecure()
        val diags = diagnosticsManager.runDiagnostics()

        _uiState.value = DashboardUiState(
            isDefaultDialer = isDialer,
            isDeviceSecure = isSecure,
            diagnosticsSummary = diags,
            quickStatusText = if (isDialer) "Full Telecom Protection Active" else "Limited Protection (Dialer Role Recommended)"
        )
    }

    fun toggleProtection(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateProtectionEnabled(enabled)
        }
    }

    fun setProtectionMode(mode: ProtectionMode) {
        viewModelScope.launch {
            settingsRepository.updateProtectionMode(mode)
        }
    }

    fun triggerQuickSimulation() {
        callManager.startSimulatedCall(
            callerName = "Jane Foster",
            callerNumber = "+1 (415) 555-0199",
            location = "San Francisco, CA"
        )
    }
}
