package com.ayush.geeksforgeeks.auth

import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.data.datastore.UserRole
import com.ayush.data.repository.AuthRepository
import com.ayush.data.repository.AuthState
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    private var currentAuthJob: Job? = null

    var email by mutableStateOf(savedStateHandle.get<String>("email") ?: "")
        private set
    var password by mutableStateOf(savedStateHandle.get<String>("password") ?: "")
        private set

    private val _teams = MutableStateFlow<List<AuthRepository.Team>>(emptyList())
    val teams = _teams.asStateFlow()

    private val _teamMembers = MutableStateFlow<List<AuthRepository.TeamMember>>(emptyList())
    val teamMembers = _teamMembers.asStateFlow()

    var selectedTeam by mutableStateOf<AuthRepository.Team?>(null)
        private set
    var selectedMember by mutableStateOf<AuthRepository.TeamMember?>(null)
        private set

    init {
        viewModelScope.launch {
            savedStateHandle.setSavedStateProvider("email") { Bundle().apply { putString("email", email) } }
            savedStateHandle.setSavedStateProvider("password") { Bundle().apply { putString("password", password) } }
            loadTeams()
        }
    }

    fun updateEmail(newEmail: String) {
        email = newEmail
        savedStateHandle["email"] = newEmail
    }

    fun updatePassword(newPassword: String) {
        password = newPassword
        savedStateHandle["password"] = newPassword
    }

    fun selectTeam(team: AuthRepository.Team) {
        selectedTeam = team
        selectedMember = null
        updateEmail("")
        viewModelScope.launch {
            loadTeamMembers(team.id)
        }
    }

    fun selectMember(member: AuthRepository.TeamMember) {
        selectedMember = member
        updateEmail(member.email)
    }

    private val _resetPasswordState = MutableStateFlow<ResetPasswordState>(ResetPasswordState.Idle)
    val resetPasswordState: StateFlow<ResetPasswordState> = _resetPasswordState

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _resetPasswordState.value = ResetPasswordState.Loading
            try {
                authRepository.sendPasswordResetEmail(email)
                _resetPasswordState.value = ResetPasswordState.Success
            } catch (e: Exception) {
                _resetPasswordState.value = ResetPasswordState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    private fun loadTeams() {
        viewModelScope.launch {
            try {
                _teams.value = authRepository.getTeams()
            } catch (e: Exception) {
                // Handle error, maybe set an error state
            }
        }
    }

    private fun loadTeamMembers(teamId: String) {
        viewModelScope.launch {
            try {
                val members = authRepository.getTeamMembers(teamId)
                _teamMembers.value = members.sortedBy { it.role != UserRole.TEAM_LEAD }
            } catch (e: Exception) {
                // Handle error, maybe set an error state
            }
        }
    }


    private fun sendEmailVerification() {
        viewModelScope.launch {
            try {
                authRepository.sendEmailVerification()
                _authState.value = AuthState.EmailVerificationSent
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Failed to send verification email: ${e.message}", e)
            }
        }
    }


    fun signUp() {
        currentAuthJob?.cancel()
        currentAuthJob = viewModelScope.launch {
            _authState.value = AuthState.Loading

            val result = authRepository.signUp(
                teamId = selectedTeam?.id ?: "",
                email = email,
                password = password
            )

            result.fold(
                onSuccess = {
                    _authState.value = AuthState.EmailVerificationRequired
                },
                onFailure = {
                    _authState.value = AuthState.Error(it.message ?: "Sign up failed", it)
                }
            )
        }
    }

    fun login() {
        currentAuthJob?.cancel()
        currentAuthJob = viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.login(email, password)
            result.fold(
                onSuccess = { user ->
                    _authState.value = AuthState.Success(user)
                },
                onFailure = { e ->
                    if (e.message == "Email not verified") {
                        _authState.value = AuthState.EmailVerificationRequired
                    } else {
                        _authState.value = AuthState.Error(e.message ?: "Login failed", e)
                    }
                }
            )
        }
    }

    fun checkEmailVerification() {
        viewModelScope.launch {
            if (authRepository.isEmailVerified()) {
                val user = authRepository.firebaseAuth.currentUser
                user?.let {
                    authRepository.saveUserDataAfterVerification(it)
                    _authState.value = AuthState.Success(it)
                }
            } else {
                _authState.value = AuthState.EmailVerificationRequired
            }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            try {
                authRepository.sendEmailVerification()
                _authState.value = AuthState.EmailVerificationSent
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Failed to send verification email: ${e.message}", e)
            }
        }
    }

    private fun clearFormData() {
        email = ""
        password = ""
        selectedTeam = null
        selectedMember = null
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    suspend fun getUserRoleOnLogin(): UserRole =
        authRepository.getUserRole(email)

    private var verificationCheckJob: Job? = null


    private fun stopVerificationCheck() {
        verificationCheckJob?.cancel()
    }


    override fun onCleared() {
        super.onCleared()
        stopVerificationCheck()
    }
}

sealed class ResetPasswordState {
    object Idle : ResetPasswordState()
    object Loading : ResetPasswordState()
    object Success : ResetPasswordState()
    data class Error(val message: String) : ResetPasswordState()
}
