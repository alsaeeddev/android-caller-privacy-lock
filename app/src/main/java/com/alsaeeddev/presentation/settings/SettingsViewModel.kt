package com.alsaeeddev.presentation.settings

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alsaeeddev.data.model.ProtectionMode
import com.alsaeeddev.data.model.ProtectionSettings
import com.alsaeeddev.data.repository.SettingsRepository
import com.alsaeeddev.telecom.RoleHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isDefaultDialer: Boolean = false,
    val isCallScreeningRoleHeld: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val roleHelper = RoleHelper(application)

    val settings: StateFlow<ProtectionSettings> = settingsRepository.settingsFlow

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refreshRoles()
    }

    fun refreshRoles() {
        _uiState.value = SettingsUiState(
            isDefaultDialer = roleHelper.isDefaultDialer(),
            isCallScreeningRoleHeld = roleHelper.isCallScreeningRoleHeld()
        )
    }

    fun setProtectionEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateProtectionEnabled(enabled) }
    }

    fun setProtectionMode(mode: ProtectionMode) {
        viewModelScope.launch { settingsRepository.updateProtectionMode(mode) }
    }

    fun setRevealTimeout(seconds: Int) {
        viewModelScope.launch { settingsRepository.updateRevealTimeout(seconds) }
    }

    fun togglePrivacy(
        hideName: Boolean? = null,
        hideNumber: Boolean? = null,
        hidePhoto: Boolean? = null,
        hideOnLockScreen: Boolean? = null,
        hideNotificationContent: Boolean? = null
    ) {
        viewModelScope.launch {
            settingsRepository.updatePrivacyToggles(
                hideName = hideName,
                hideNumber = hideNumber,
                hidePhoto = hidePhoto,
                hideOnLockScreen = hideOnLockScreen,
                hideNotificationContent = hideNotificationContent
            )
        }
    }

    fun toggleSecurityBehavior(
        autoHideScreenOff: Boolean? = null,
        autoHideBackground: Boolean? = null,
        autoHideCallEnds: Boolean? = null,
        requireBiometrics: Boolean? = null,
        allowDeviceCredential: Boolean? = null
    ) {
        viewModelScope.launch {
            settingsRepository.updateSecurityBehavior(
                autoHideScreenOff = autoHideScreenOff,
                autoHideBackground = autoHideBackground,
                autoHideCallEnds = autoHideCallEnds,
                requireBiometrics = requireBiometrics,
                allowDeviceCredential = allowDeviceCredential
            )
        }
    }

    fun getRequestDialerIntent(): Intent? {
        return roleHelper.createRequestDialerRoleIntent()
    }
}
