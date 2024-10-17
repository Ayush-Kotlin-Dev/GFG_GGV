package com.ayush.geeksforgeeks.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.data.datastore.UserSettings
import com.ayush.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        loadProfileData()
    }

    private fun loadProfileData() {
        viewModelScope.launch {
            try {
                val user = userRepository.getCurrentUser()
                _uiState.value = ProfileUiState.Success(user)
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun updateProfile(name: String, profilePicUrl: String?) {
        viewModelScope.launch {
            try {
                val currentUser = (uiState.value as? ProfileUiState.Success)?.user ?: return@launch
                val updatedUser = currentUser.copy(name = name, profilePicUrl = profilePicUrl , isLoggedIn = true)
                userRepository.updateUser(updatedUser)
                _uiState.value = ProfileUiState.Success(updatedUser)
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Failed to update profile")
            }
        }
    }

    fun logOut() {
        viewModelScope.launch {
            try {
                userRepository.logout()
                _uiState.value = ProfileUiState.Error("User logged out")
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Failed to log out")
            }
        }
    }

    sealed class ProfileUiState {
        object Loading : ProfileUiState()
        data class Success(val user: UserSettings) : ProfileUiState()
        data class Error(val message: String) : ProfileUiState()
    }
}