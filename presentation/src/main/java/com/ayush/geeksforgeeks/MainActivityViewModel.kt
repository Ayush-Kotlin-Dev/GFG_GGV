package com.ayush.geeksforgeeks

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.data.datastore.UserPreferences
import com.ayush.data.datastore.UserRole
import com.ayush.data.datastore.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    val uiState: StateFlow<UiState> = userPreferences.userData
        .map { userSettings ->
            when {
                !userSettings.isLoggedIn -> UiState.NotLoggedIn
                userSettings.role == UserRole.TEAM_LEAD -> UiState.LoggedIn(UserRole.TEAM_LEAD)
                userSettings.role == UserRole.ADMIN -> UiState.LoggedIn(UserRole.ADMIN)
                userSettings.role == UserRole.GUEST -> UiState.LoggedIn(UserRole.GUEST)
                else -> UiState.LoggedIn(UserRole.MEMBER)
            }
        }
        .catch { error ->
            emit(UiState.Error(error.message ?: "Unknown error"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiState.Loading
        )

    sealed class UiState {
        object Loading : UiState()
        object NotLoggedIn : UiState()
        data class LoggedIn(val userRole: UserRole) : UiState()
        data class Error(val message: String) : UiState()
    }
}