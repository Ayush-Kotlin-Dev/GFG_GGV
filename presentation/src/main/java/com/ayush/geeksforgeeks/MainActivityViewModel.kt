package com.ayush.geeksforgeeks

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.core.screen.Screen
import com.ayush.data.datastore.UserPreferences
import com.ayush.data.repository.AuthRepository
import com.ayush.data.repository.UserRepository
import com.ayush.geeksforgeeks.auth.AuthScreen
import com.ayush.geeksforgeeks.home.HomeScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val authRepository: AuthRepository,
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
                val isLoggedIn = authRepository.isUserLoggedIn()
                _uiState.value = if (isLoggedIn == true) {
                    UiState.LoggedIn
                } else {
                    UiState.NotLoggedIn
                }
                Log.d("MainActivityViewModel", "checkLoginStatus: isLoggedIn=$isLoggedIn")
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