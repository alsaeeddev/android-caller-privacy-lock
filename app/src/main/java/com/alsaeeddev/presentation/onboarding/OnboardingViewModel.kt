package com.alsaeeddev.presentation.onboarding

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alsaeeddev.data.repository.SettingsRepository
import com.alsaeeddev.security.BiometricAuthManager
import com.alsaeeddev.telecom.CallManager
import com.alsaeeddev.telecom.RoleHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val currentStep: Int = 0,
    val isDefaultDialer: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val isCompleted: Boolean = false
)

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val roleHelper = RoleHelper(application)
    private val biometricAuthManager = BiometricAuthManager(application)
    private val callManager = CallManager.getInstance(application)

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        checkStatus()
    }

    fun checkStatus() {
        _uiState.value = _uiState.value.copy(
            isDefaultDialer = roleHelper.isDefaultDialer(),
            isBiometricAvailable = biometricAuthManager.isDeviceSecure()
        )
    }

    fun nextStep() {
        val next = _uiState.value.currentStep + 1
        if (next >= 5) {
            completeOnboarding()
        } else {
            _uiState.value = _uiState.value.copy(currentStep = next)
        }
    }

    fun prevStep() {
        val prev = (_uiState.value.currentStep - 1).coerceAtLeast(0)
        _uiState.value = _uiState.value.copy(currentStep = prev)
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(true)
            _uiState.value = _uiState.value.copy(isCompleted = true)
        }
    }

    fun getRequestDialerIntent(): Intent? {
        return roleHelper.createRequestDialerRoleIntent()
    }

    fun launchTestCall() {
        callManager.startSimulatedCall(
            callerName = "Dr. Robert Vance",
            callerNumber = "+1 (800) 555-0144",
            location = "New York, NY"
        )
    }
}
