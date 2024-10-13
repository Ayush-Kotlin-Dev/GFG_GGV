package com.ayush.geeksforgeeks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.data.datastore.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            try {
                val userData = userPreferences.userData.first()
                _uiState.value = if (userData.isLoggedIn) {
                    UiState.LoggedIn
                } else {
                    UiState.NotLoggedIn
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "An error occurred")
            }
        }
    }

    sealed class UiState {
        object Loading : UiState()
        object LoggedIn : UiState()
        object NotLoggedIn : UiState()
        data class Error(val message: String) : UiState()
    }
}