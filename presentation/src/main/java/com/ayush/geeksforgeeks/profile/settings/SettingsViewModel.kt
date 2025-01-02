package com.ayush.geeksforgeeks.profile.settings

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.data.repository.AuthRepository
import com.ayush.geeksforgeeks.auth.ResetPasswordState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val isDarkMode: Boolean = false,
    val isNotificationsEnabled: Boolean = true,
    val isEventRemindersEnabled: Boolean = true,
    val appVersion: String = "1.0.0",
    val isLoading: Boolean = false,
)

sealed class SettingsEvent {
    data class ToggleDarkMode(val enabled: Boolean) : SettingsEvent()
    data class ToggleNotifications(val enabled: Boolean) : SettingsEvent()
    data class ToggleEventReminders(val enabled: Boolean) : SettingsEvent()
    object ChangePassword : SettingsEvent()
    object OpenPrivacySettings : SettingsEvent()
    object OpenDownloadSettings : SettingsEvent()
    object OpenLanguageSettings : SettingsEvent()
    object OpenTermsOfService : SettingsEvent()
    object OpenPrivacyPolicy : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val _showResetDialog = MutableStateFlow(false)
    val showResetDialog = _showResetDialog.asStateFlow()

    private val _resetPasswordState = MutableStateFlow<ResetPasswordState>(ResetPasswordState.Idle)
    val resetPasswordState = _resetPasswordState.asStateFlow()

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ToggleDarkMode -> updateDarkMode(event.enabled)
            is SettingsEvent.ToggleNotifications -> updateNotifications(event.enabled)
            is SettingsEvent.ToggleEventReminders -> updateEventReminders(event.enabled)
            SettingsEvent.ChangePassword -> handleChangePassword()
            SettingsEvent.OpenPrivacySettings -> handlePrivacySettings()
            SettingsEvent.OpenDownloadSettings -> handleDownloadSettings()
            SettingsEvent.OpenLanguageSettings -> handleLanguageSettings()
            SettingsEvent.OpenTermsOfService -> handleTermsOfService()
            SettingsEvent.OpenPrivacyPolicy -> handlePrivacyPolicy()
        }
    }

    private fun updateDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            Toast.makeText(
                context,
                "Dark mode? In this economy? 🌚 (Coming soon!)",
                Toast.LENGTH_SHORT
            ).show()
            _state.update { it.copy(isDarkMode = enabled) }
        }
    }

    private fun updateNotifications(enabled: Boolean) {
        viewModelScope.launch {
            Toast.makeText(
                context,
                if (enabled) "Sure, I'll ping you... when this feature is ready 🔔"
                else "Fine, I'll be quiet... not that I was making any noise anyway 🤫",
                Toast.LENGTH_SHORT
            ).show()
            _state.update { it.copy(isNotificationsEnabled = enabled) }
        }
    }

    private fun updateEventReminders(enabled: Boolean) {
        // Update event reminders preference in DataStore
        viewModelScope.launch {
            Toast.makeText(
                context,
                if (enabled) "Event reminders enabled 🎉" else "Event reminders disabled 😢",
                Toast.LENGTH_SHORT
            ).show()
            _state.update { it.copy(isEventRemindersEnabled = enabled) }
        }

    }

    fun handleChangePassword() {
        _showResetDialog.value = true
    }

    fun hideResetDialog() {
        _showResetDialog.value = false
        _resetPasswordState.value = ResetPasswordState.Idle
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _resetPasswordState.value = ResetPasswordState.Error("Email is required")
            return
        }

        viewModelScope.launch {
            _resetPasswordState.value = ResetPasswordState.Loading
            try {
                authRepository.sendPasswordResetEmail(email)
                _resetPasswordState.value = ResetPasswordState.Success
                delay(3000) // Show success for 3 seconds
                _resetPasswordState.value = ResetPasswordState.Idle
                _showResetDialog.value = false
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Password reset error: ${e.message}")
                _resetPasswordState.value = ResetPasswordState.Error(
                    e.message ?: "Failed to send reset email"
                )
            }
        }
    }

    private fun handlePrivacySettings() {
        // Navigate to privacy settings screen
    }

    private fun handleDownloadSettings() {
        viewModelScope.launch {
            Toast.makeText(
                context,
                "Downloads? Coming soon to a screen near you! 📥",
                Toast.LENGTH_SHORT
            ).show()

        }
    }

    private fun handleLanguageSettings() {
        // Show language selector
    }

    private fun handleTermsOfService() {
        // Open terms of service
    }

    private fun handlePrivacyPolicy() {
        // Open privacy policy
    }

}