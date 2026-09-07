package com.alsaeeddev.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.alsaeeddev.data.model.ProtectionMode
import com.alsaeeddev.data.model.ProtectionSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "private_caller_settings")

class SettingsRepository(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("private_caller_fast_settings", Context.MODE_PRIVATE)

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private object PrefKeys {
        const val IS_PROTECTION_ENABLED = "is_protection_enabled"
        const val PROTECTION_MODE = "protection_mode"
        const val HIDE_CALLER_NAME = "hide_caller_name"
        const val HIDE_CALLER_NUMBER = "hide_caller_number"
        const val HIDE_CONTACT_PHOTO = "hide_contact_photo"
        const val REVEAL_TIMEOUT_SECONDS = "reveal_timeout_seconds"
        const val REQUIRE_BIOMETRICS = "require_biometrics"
        const val ALLOW_DEVICE_CREDENTIAL = "allow_device_credential"
        const val HIDE_ON_LOCK_SCREEN = "hide_on_lock_screen"
        const val HIDE_NOTIFICATION_CONTENT = "hide_notification_content"
        const val AUTO_HIDE_ON_SCREEN_OFF = "auto_hide_on_screen_off"
        const val AUTO_HIDE_ON_BACKGROUND = "auto_hide_on_background"
        const val AUTO_HIDE_AFTER_CALL_ENDS = "auto_hide_after_call_ends"
        const val IS_ONBOARDING_COMPLETED = "is_onboarding_completed"
    }

    private object DataStoreKeys {
        val IS_PROTECTION_ENABLED = booleanPreferencesKey("is_protection_enabled")
        val PROTECTION_MODE = stringPreferencesKey("protection_mode")
        val HIDE_CALLER_NAME = booleanPreferencesKey("hide_caller_name")
        val HIDE_CALLER_NUMBER = booleanPreferencesKey("hide_caller_number")
        val HIDE_CONTACT_PHOTO = booleanPreferencesKey("hide_contact_photo")
        val REVEAL_TIMEOUT_SECONDS = intPreferencesKey("reveal_timeout_seconds")
        val REQUIRE_BIOMETRICS = booleanPreferencesKey("require_biometrics")
        val ALLOW_DEVICE_CREDENTIAL = booleanPreferencesKey("allow_device_credential")
        val HIDE_ON_LOCK_SCREEN = booleanPreferencesKey("hide_on_lock_screen")
        val HIDE_NOTIFICATION_CONTENT = booleanPreferencesKey("hide_notification_content")
        val AUTO_HIDE_ON_SCREEN_OFF = booleanPreferencesKey("auto_hide_on_screen_off")
        val AUTO_HIDE_ON_BACKGROUND = booleanPreferencesKey("auto_hide_on_background")
        val AUTO_HIDE_AFTER_CALL_ENDS = booleanPreferencesKey("auto_hide_after_call_ends")
        val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
    }

    private val _settingsFlow: MutableStateFlow<ProtectionSettings> =
        MutableStateFlow(loadSettingsSynchronously())

    val settingsFlow: StateFlow<ProtectionSettings> = _settingsFlow.asStateFlow()

    init {
        // Fast asynchronous check to migrate any existing DataStore values if SharedPreferences not populated
        repositoryScope.launch {
            try {
                if (!sharedPreferences.contains(PrefKeys.IS_PROTECTION_ENABLED)) {
                    val dsData = context.dataStore.data.firstOrNull()
                    if (dsData != null && dsData.asMap().isNotEmpty()) {
                        val modeStr = dsData[DataStoreKeys.PROTECTION_MODE] ?: ProtectionMode.MAX_PRIVACY.name
                        val mode = try { ProtectionMode.valueOf(modeStr) } catch (e: Exception) { ProtectionMode.MAX_PRIVACY }
                        val migrated = ProtectionSettings(
                            isProtectionEnabled = dsData[DataStoreKeys.IS_PROTECTION_ENABLED] ?: true,
                            mode = mode,
                            hideCallerName = dsData[DataStoreKeys.HIDE_CALLER_NAME] ?: true,
                            hideCallerNumber = dsData[DataStoreKeys.HIDE_CALLER_NUMBER] ?: true,
                            hideContactPhoto = dsData[DataStoreKeys.HIDE_CONTACT_PHOTO] ?: true,
                            revealTimeoutSeconds = dsData[DataStoreKeys.REVEAL_TIMEOUT_SECONDS] ?: 10,
                            requireBiometrics = dsData[DataStoreKeys.REQUIRE_BIOMETRICS] ?: true,
                            allowDeviceCredentialFallback = dsData[DataStoreKeys.ALLOW_DEVICE_CREDENTIAL] ?: true,
                            hideOnLockScreen = dsData[DataStoreKeys.HIDE_ON_LOCK_SCREEN] ?: true,
                            hideNotificationContent = dsData[DataStoreKeys.HIDE_NOTIFICATION_CONTENT] ?: true,
                            autoHideOnScreenOff = dsData[DataStoreKeys.AUTO_HIDE_ON_SCREEN_OFF] ?: true,
                            autoHideOnBackground = dsData[DataStoreKeys.AUTO_HIDE_ON_BACKGROUND] ?: true,
                            autoHideAfterCallEnds = dsData[DataStoreKeys.AUTO_HIDE_AFTER_CALL_ENDS] ?: true,
                            isOnboardingCompleted = dsData[DataStoreKeys.IS_ONBOARDING_COMPLETED] ?: false
                        )
                        saveSettingsToPrefs(migrated)
                        _settingsFlow.value = migrated
                    }
                }
            } catch (e: Exception) {
                // Keep fast synchronous defaults
            }
        }
    }

    fun getCurrentSettings(): ProtectionSettings = _settingsFlow.value

    private fun loadSettingsSynchronously(): ProtectionSettings {
        val modeStr = sharedPreferences.getString(PrefKeys.PROTECTION_MODE, ProtectionMode.MAX_PRIVACY.name)
            ?: ProtectionMode.MAX_PRIVACY.name
        val mode = try {
            ProtectionMode.valueOf(modeStr)
        } catch (e: Exception) {
            ProtectionMode.MAX_PRIVACY
        }

        return ProtectionSettings(
            isProtectionEnabled = sharedPreferences.getBoolean(PrefKeys.IS_PROTECTION_ENABLED, true),
            mode = mode,
            hideCallerName = sharedPreferences.getBoolean(PrefKeys.HIDE_CALLER_NAME, true),
            hideCallerNumber = sharedPreferences.getBoolean(PrefKeys.HIDE_CALLER_NUMBER, true),
            hideContactPhoto = sharedPreferences.getBoolean(PrefKeys.HIDE_CONTACT_PHOTO, true),
            revealTimeoutSeconds = sharedPreferences.getInt(PrefKeys.REVEAL_TIMEOUT_SECONDS, 10),
            requireBiometrics = sharedPreferences.getBoolean(PrefKeys.REQUIRE_BIOMETRICS, true),
            allowDeviceCredentialFallback = sharedPreferences.getBoolean(PrefKeys.ALLOW_DEVICE_CREDENTIAL, true),
            hideOnLockScreen = sharedPreferences.getBoolean(PrefKeys.HIDE_ON_LOCK_SCREEN, true),
            hideNotificationContent = sharedPreferences.getBoolean(PrefKeys.HIDE_NOTIFICATION_CONTENT, true),
            autoHideOnScreenOff = sharedPreferences.getBoolean(PrefKeys.AUTO_HIDE_ON_SCREEN_OFF, true),
            autoHideOnBackground = sharedPreferences.getBoolean(PrefKeys.AUTO_HIDE_ON_BACKGROUND, true),
            autoHideAfterCallEnds = sharedPreferences.getBoolean(PrefKeys.AUTO_HIDE_AFTER_CALL_ENDS, true),
            isOnboardingCompleted = sharedPreferences.getBoolean(PrefKeys.IS_ONBOARDING_COMPLETED, false)
        )
    }

    private fun saveSettingsToPrefs(settings: ProtectionSettings) {
        sharedPreferences.edit()
            .putBoolean(PrefKeys.IS_PROTECTION_ENABLED, settings.isProtectionEnabled)
            .putString(PrefKeys.PROTECTION_MODE, settings.mode.name)
            .putBoolean(PrefKeys.HIDE_CALLER_NAME, settings.hideCallerName)
            .putBoolean(PrefKeys.HIDE_CALLER_NUMBER, settings.hideCallerNumber)
            .putBoolean(PrefKeys.HIDE_CONTACT_PHOTO, settings.hideContactPhoto)
            .putInt(PrefKeys.REVEAL_TIMEOUT_SECONDS, settings.revealTimeoutSeconds)
            .putBoolean(PrefKeys.REQUIRE_BIOMETRICS, settings.requireBiometrics)
            .putBoolean(PrefKeys.ALLOW_DEVICE_CREDENTIAL, settings.allowDeviceCredentialFallback)
            .putBoolean(PrefKeys.HIDE_ON_LOCK_SCREEN, settings.hideOnLockScreen)
            .putBoolean(PrefKeys.HIDE_NOTIFICATION_CONTENT, settings.hideNotificationContent)
            .putBoolean(PrefKeys.AUTO_HIDE_ON_SCREEN_OFF, settings.autoHideOnScreenOff)
            .putBoolean(PrefKeys.AUTO_HIDE_ON_BACKGROUND, settings.autoHideOnBackground)
            .putBoolean(PrefKeys.AUTO_HIDE_AFTER_CALL_ENDS, settings.autoHideAfterCallEnds)
            .putBoolean(PrefKeys.IS_ONBOARDING_COMPLETED, settings.isOnboardingCompleted)
            .apply()
    }

    private fun syncToDataStore(settings: ProtectionSettings) {
        repositoryScope.launch {
            try {
                context.dataStore.edit { preferences ->
                    preferences[DataStoreKeys.IS_PROTECTION_ENABLED] = settings.isProtectionEnabled
                    preferences[DataStoreKeys.PROTECTION_MODE] = settings.mode.name
                    preferences[DataStoreKeys.HIDE_CALLER_NAME] = settings.hideCallerName
                    preferences[DataStoreKeys.HIDE_CALLER_NUMBER] = settings.hideCallerNumber
                    preferences[DataStoreKeys.HIDE_CONTACT_PHOTO] = settings.hideContactPhoto
                    preferences[DataStoreKeys.REVEAL_TIMEOUT_SECONDS] = settings.revealTimeoutSeconds
                    preferences[DataStoreKeys.REQUIRE_BIOMETRICS] = settings.requireBiometrics
                    preferences[DataStoreKeys.ALLOW_DEVICE_CREDENTIAL] = settings.allowDeviceCredentialFallback
                    preferences[DataStoreKeys.HIDE_ON_LOCK_SCREEN] = settings.hideOnLockScreen
                    preferences[DataStoreKeys.HIDE_NOTIFICATION_CONTENT] = settings.hideNotificationContent
                    preferences[DataStoreKeys.AUTO_HIDE_ON_SCREEN_OFF] = settings.autoHideOnScreenOff
                    preferences[DataStoreKeys.AUTO_HIDE_ON_BACKGROUND] = settings.autoHideOnBackground
                    preferences[DataStoreKeys.AUTO_HIDE_AFTER_CALL_ENDS] = settings.autoHideAfterCallEnds
                    preferences[DataStoreKeys.IS_ONBOARDING_COMPLETED] = settings.isOnboardingCompleted
                }
            } catch (e: Exception) {
                // Non-critical background sync
            }
        }
    }

    suspend fun updateProtectionEnabled(enabled: Boolean) {
        val updated = _settingsFlow.value.copy(isProtectionEnabled = enabled)
        _settingsFlow.value = updated
        sharedPreferences.edit().putBoolean(PrefKeys.IS_PROTECTION_ENABLED, enabled).apply()
        syncToDataStore(updated)
    }

    suspend fun updateProtectionMode(mode: ProtectionMode) {
        val updated = _settingsFlow.value.copy(mode = mode)
        _settingsFlow.value = updated
        sharedPreferences.edit().putString(PrefKeys.PROTECTION_MODE, mode.name).apply()
        syncToDataStore(updated)
    }

    suspend fun updateRevealTimeout(seconds: Int) {
        val updated = _settingsFlow.value.copy(revealTimeoutSeconds = seconds)
        _settingsFlow.value = updated
        sharedPreferences.edit().putInt(PrefKeys.REVEAL_TIMEOUT_SECONDS, seconds).apply()
        syncToDataStore(updated)
    }

    suspend fun updatePrivacyToggles(
        hideName: Boolean? = null,
        hideNumber: Boolean? = null,
        hidePhoto: Boolean? = null,
        hideOnLockScreen: Boolean? = null,
        hideNotificationContent: Boolean? = null
    ) {
        val current = _settingsFlow.value
        val updated = current.copy(
            hideCallerName = hideName ?: current.hideCallerName,
            hideCallerNumber = hideNumber ?: current.hideCallerNumber,
            hideContactPhoto = hidePhoto ?: current.hideContactPhoto,
            hideOnLockScreen = hideOnLockScreen ?: current.hideOnLockScreen,
            hideNotificationContent = hideNotificationContent ?: current.hideNotificationContent
        )
        _settingsFlow.value = updated
        val editor = sharedPreferences.edit()
        hideName?.let { editor.putBoolean(PrefKeys.HIDE_CALLER_NAME, it) }
        hideNumber?.let { editor.putBoolean(PrefKeys.HIDE_CALLER_NUMBER, it) }
        hidePhoto?.let { editor.putBoolean(PrefKeys.HIDE_CONTACT_PHOTO, it) }
        hideOnLockScreen?.let { editor.putBoolean(PrefKeys.HIDE_ON_LOCK_SCREEN, it) }
        hideNotificationContent?.let { editor.putBoolean(PrefKeys.HIDE_NOTIFICATION_CONTENT, it) }
        editor.apply()
        syncToDataStore(updated)
    }

    suspend fun updateSecurityBehavior(
        autoHideScreenOff: Boolean? = null,
        autoHideBackground: Boolean? = null,
        autoHideCallEnds: Boolean? = null,
        requireBiometrics: Boolean? = null,
        allowDeviceCredential: Boolean? = null
    ) {
        val current = _settingsFlow.value
        val updated = current.copy(
            autoHideOnScreenOff = autoHideScreenOff ?: current.autoHideOnScreenOff,
            autoHideOnBackground = autoHideBackground ?: current.autoHideOnBackground,
            autoHideAfterCallEnds = autoHideCallEnds ?: current.autoHideAfterCallEnds,
            requireBiometrics = requireBiometrics ?: current.requireBiometrics,
            allowDeviceCredentialFallback = allowDeviceCredential ?: current.allowDeviceCredentialFallback
        )
        _settingsFlow.value = updated
        val editor = sharedPreferences.edit()
        autoHideScreenOff?.let { editor.putBoolean(PrefKeys.AUTO_HIDE_ON_SCREEN_OFF, it) }
        autoHideBackground?.let { editor.putBoolean(PrefKeys.AUTO_HIDE_ON_BACKGROUND, it) }
        autoHideCallEnds?.let { editor.putBoolean(PrefKeys.AUTO_HIDE_AFTER_CALL_ENDS, it) }
        requireBiometrics?.let { editor.putBoolean(PrefKeys.REQUIRE_BIOMETRICS, it) }
        allowDeviceCredential?.let { editor.putBoolean(PrefKeys.ALLOW_DEVICE_CREDENTIAL, it) }
        editor.apply()
        syncToDataStore(updated)
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        val updated = _settingsFlow.value.copy(isOnboardingCompleted = completed)
        _settingsFlow.value = updated
        sharedPreferences.edit().putBoolean(PrefKeys.IS_ONBOARDING_COMPLETED, completed).apply()
        syncToDataStore(updated)
    }
}

