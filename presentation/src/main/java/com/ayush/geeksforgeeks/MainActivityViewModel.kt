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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferences.userData.collect { userSettings ->
                Log.d("MainActivityViewModel", "UserSettings: ${userSettings}")
                _uiState.value = when {
                    !userSettings.isLoggedIn -> UiState.NotLoggedIn
                    userSettings.role == UserRole.TEAM_LEAD -> UiState.LoggedInAsAdmin(userSettings)
                    else -> UiState.LoggedInAsMember(userSettings)
                }
            }
        }
    }

    sealed class UiState {
        object Loading : UiState()
        object NotLoggedIn : UiState()
        data class LoggedInAsAdmin(val userSettings: UserSettings) : UiState()
        data class LoggedInAsMember(val userSettings: UserSettings) : UiState()
    }
}