package com.ayush.geeksforgeeks.profile.settings

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.data.repository.AuthRepository
import com.ayush.geeksforgeeks.BuildConfig
import com.ayush.geeksforgeeks.auth.ResetPasswordState
import com.ayush.geeksforgeeks.utils.GithubRelease
import com.ayush.geeksforgeeks.utils.UpdateManager
import com.github.theapache64.fig.Fig
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.URL
import javax.inject.Inject

data class SettingsState(
    val isDarkMode: Boolean = false,
    val isNotificationsEnabled: Boolean = true,
    val isEventRemindersEnabled: Boolean = true,
    val appVersion: String = "${BuildConfig.VERSION_NAME} (${BuildConfig.BUILD_VARIANT})",
    val isLoading: Boolean = false,
    val buildVariant: String = BuildConfig.BUILD_VARIANT,
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

    private val _showUpdateDialog = MutableStateFlow<Pair<String, GithubRelease>?>(null)
    val showUpdateDialog = _showUpdateDialog.asStateFlow()

    private var lastUpdateCheck = 0L
    private val UPDATE_CHECK_COOLDOWN = 5000L

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

    fun checkForUpdates(context: Context) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateCheck < UPDATE_CHECK_COOLDOWN) {
            val remainingSeconds = ((UPDATE_CHECK_COOLDOWN - (currentTime - lastUpdateCheck)) / 1000) + 1
            Toast.makeText(
                context,
                "Please wait ${remainingSeconds}s before checking again",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        lastUpdateCheck = currentTime

        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                Log.d("UpdateCheck", "Starting update check...")

                // 1. Get latest version from spreadsheet
                val fig = Fig()
                fig.init("https://docs.google.com/spreadsheets/d/1a9VU0CCwSjX7x6mPCARsvtQM8mw4zgXJiUGEhEM8ESs/edit?usp=sharing")
                val latestVersion = fig.getValue("latest_version", null)

                // 2. Compare with current version
                val currentVersion = BuildConfig.VERSION_NAME

                if (latestVersion != currentVersion) {

                    val githubUrl = "https://api.github.com/repos/Ayush-Kotlin-Dev/GFG_GGV/releases/latest"

                    val response = withContext(Dispatchers.IO) {
                        try {
                            URL(githubUrl)
                                .openConnection()
                                .apply {
                                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                                }
                                .getInputStream()
                                .bufferedReader()
                                .use { it.readText() }
                        } catch (e: Exception) {
                            Log.e("UpdateCheck", "Failed to fetch from GitHub", e)
                            throw e
                        }
                    }

                    val json = Json {
                        ignoreUnknownKeys = true
                        prettyPrint = true
                    }

                    val release = json.decodeFromString<GithubRelease>(response)

                    val apkAsset = release.assets.find { it.name.endsWith(".apk") }

                    if (apkAsset != null) {
                        withContext(Dispatchers.Main) {
                            _showUpdateDialog.value = Pair(apkAsset.browser_download_url, release)
                        }
                    } else {
                    }
                } else {
                    // Show "App is up to date" message
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "App is up to date", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Show error message to user
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to check for updates", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun dismissUpdateDialog() {
        _showUpdateDialog.value = null
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