package com.ayush.geeksforgeeks

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.data.datastore.UserPreferences
import com.ayush.data.datastore.UserRole
import com.ayush.data.datastore.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferences.userData
                .map { userSettings ->
                    when {
                        !userSettings.isLoggedIn -> UiState.NotLoggedIn
                        userSettings.role == UserRole.TEAM_LEAD -> UiState.LoggedIn(UserRole.TEAM_LEAD)
                        userSettings.role == UserRole.ADMIN -> UiState.LoggedIn(UserRole.ADMIN)
                        else -> UiState.LoggedIn(UserRole.MEMBER)
                    }
                }
                .catch { _uiState.value = UiState.Error(it.message ?: "Unknown error") }
                .collect { _uiState.value = it }
        }
    }

    sealed class UiState {
        object Loading : UiState()
        object NotLoggedIn : UiState()
        data class LoggedIn(val userRole: UserRole) : UiState()
        data class Error(val message: String) : UiState()
    }
}