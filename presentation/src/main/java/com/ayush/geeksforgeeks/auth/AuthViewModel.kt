package com.ayush.geeksforgeeks.auth

import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.data.datastore.UserRole
import com.ayush.data.repository.AuthRepository
import com.ayush.data.repository.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    private var currentAuthJob: Job? = null
    private var verificationCheckJob: Job? = null

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
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Password reset error: ${e.message}")
                _resetPasswordState.value = ResetPasswordState.Error(
                    e.message ?: "Failed to send reset email"
                )
            }
        }
    }

    private fun loadTeams() {
        viewModelScope.launch {
            try {
                _teams.value = authRepository.getTeams()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error loading teams: ${e.message}")
                // Add proper error handling
                _authState.value = AuthState.Error("Failed to load teams", e)
            }
        }
    }

    private fun loadTeamMembers(teamId: String) {
        viewModelScope.launch {
            try {
                val members = authRepository.getTeamMembers(teamId)
                _teamMembers.value = members.sortedBy { it.role != UserRole.TEAM_LEAD }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error loading team members: ${e.message}")
                _authState.value = AuthState.Error("Failed to load team members", e)
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
            try {
                if (!validateSignUpInput()) {
                    return@launch
                }

                if (selectedTeam == null || selectedMember == null) {
                    Log.e("AuthViewModel", "Team or member not selected")
                    _authState.value = AuthState.Error("Please select team and name", Exception())
                    return@launch
                }

                _authState.value = AuthState.Loading
                Log.d("AuthViewModel", "Starting signup for ${selectedMember?.name}")

                val result = authRepository.signUp(
                    teamId = selectedTeam?.id ?: "",
                    email = email,
                    password = password
                )

                result.fold(
                    onSuccess = {
                        Log.d("AuthViewModel", "Signup successful, verification email sent")
                        _authState.value = AuthState.EmailVerificationRequired
                    },
                    onFailure = { e ->
                        Log.e("AuthViewModel", "Signup failed: ${e.message}")
                        _authState.value = AuthState.Error(e.message ?: "Sign up failed", e)
                    }
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Signup error: ${e.message}")
                _authState.value = AuthState.Error(e.message ?: "Sign up failed", e)
            }
        }
    }

    private fun validateLoginInput(): Boolean {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password are required", Exception())
            return false
        }
        return true
    }

    private fun validateSignUpInput(): Boolean {
        if (selectedTeam == null || selectedMember == null) {
            _authState.value = AuthState.Error("Please select team and name", Exception())
            return false
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters", Exception())
            return false
        }
        return true
    }

    fun login() {
        currentAuthJob?.cancel()
        verificationCheckJob?.cancel()
        currentAuthJob = viewModelScope.launch {
            try {
                if (!validateLoginInput()) {
                    return@launch
                }

                Log.d("AuthViewModel", "Starting login process...")
                _authState.value = AuthState.Loading

                Log.d("AuthViewModel", "Calling repository login...")
                val result = authRepository.login(email, password)

                result.fold(
                    onSuccess = { user ->
                        Log.d("AuthViewModel", "Login successful for user: ${user.uid}")
                        _authState.value = AuthState.Success(user)
                    },
                    onFailure = { e ->
                        when {
                            e is CancellationException -> throw e  // Rethrow cancellation
                            e.message == "Email not verified" -> {
                                Log.w("AuthViewModel", "Login failed: Email not verified")
                                _authState.value = AuthState.EmailVerificationRequired
                            }
                            else -> {
                                Log.e("AuthViewModel", "Login failed with error: ${e.message}", e)
                                _authState.value = AuthState.Error(e.message ?: "Login failed", e)
                            }
                        }
                    }
                )
            } catch (e: CancellationException) {
                Log.d("AuthViewModel", "Login cancelled")
                throw e  // Rethrow cancellation
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Login error: ${e.message}")
                _authState.value = AuthState.Error(e.message ?: "Login failed", e)
            }
        }
    }

    private fun startVerificationCheck() {
        verificationCheckJob?.cancel()
        verificationCheckJob = viewModelScope.launch {
            try {
                while (true) {
                    delay(5000)
                    Log.d("AuthViewModel", "Checking email verification status")
                    if (authRepository.isEmailVerified()) {
                        val user = authRepository.firebaseAuth.currentUser
                        if (user != null) {
                            Log.d("AuthViewModel", "Email verified, saving user data")
                            authRepository.saveUserDataAfterVerification(user)
                            _authState.value = AuthState.Success(user)
                            break  // Exit the loop once verified
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e  // Rethrow cancellation
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Verification check error: ${e.message}")
                _authState.value = AuthState.Error("Verification check failed", e)
            }
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
                startVerificationCheck()
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
        _authState.value = AuthState.Idle
        _resetPasswordState.value = ResetPasswordState.Idle
        verificationCheckJob?.cancel()
    }

    fun resetState() {
        _authState.value = AuthState.Idle
        clearFormData()
    }

    suspend fun getUserRoleOnLogin(): UserRole =
        authRepository.getUserRole(email)

    override fun onCleared() {
        super.onCleared()
        verificationCheckJob?.cancel()
    }
}

sealed class ResetPasswordState {
    object Idle : ResetPasswordState()
    object Loading : ResetPasswordState()
    object Success : ResetPasswordState()
    data class Error(val message: String) : ResetPasswordState()
}