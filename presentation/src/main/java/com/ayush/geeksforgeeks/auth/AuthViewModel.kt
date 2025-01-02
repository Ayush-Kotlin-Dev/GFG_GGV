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

    companion object {
        private const val VERIFICATION_TIMEOUT = 5 * 60 * 1000L
        private const val VERIFICATION_CHECK_INTERVAL = 5000L
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    private val _verificationState = MutableStateFlow<VerificationState>(VerificationState.Idle)
    val verificationState = _verificationState.asStateFlow()

    sealed class VerificationState {
        object Idle : VerificationState()
        object Loading : VerificationState()
        data class TimeoutWarning(val remainingSeconds: Int) : VerificationState()
        object Timeout : VerificationState()
        data class Error(val message: String) : VerificationState()
    }

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
            // Restore saved state
            savedStateHandle.get<String>("email")?.let { updateEmail(it) }
            savedStateHandle.get<String>("password")?.let { updatePassword(it) }
            
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
                if (!validateLoginInput()) return@launch

                _authState.value = AuthState.Loading
                Log.d("AuthViewModel", "Starting login for email: $email")

                val result = authRepository.login(email, password)
            
                result.fold(
                    onSuccess = { user ->
                        Log.d("AuthViewModel", "Login successful for user: ${user.uid}")
                        _authState.value = AuthState.Success(user)
                    },
                    onFailure = { e ->
                        when {
                            e is CancellationException -> throw e
                            e.message?.contains("Email not verified") == true -> {
                                Log.d("AuthViewModel", "Email not verified, starting verification check")
                                _authState.value = AuthState.EmailVerificationRequired
                                startVerificationCheck()  // Start verification check immediately
                            }
                            else -> {
                                Log.e("AuthViewModel", "Login failed: ${e.message}")
                                _authState.value = AuthState.Error(e.message ?: "Login failed", e)
                            }
                        }
                    }
                )
            } catch (e: CancellationException) {
                throw e
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
                val startTime = System.currentTimeMillis()
                
                while (System.currentTimeMillis() - startTime < VERIFICATION_TIMEOUT) {
                    _verificationState.value = VerificationState.Loading
                    
                    // Calculate and show remaining time
                    val remainingTime = VERIFICATION_TIMEOUT - (System.currentTimeMillis() - startTime)
                    if (remainingTime < 60000) {
                        _verificationState.value = VerificationState.TimeoutWarning(
                            (remainingTime / 1000).toInt()
                        )
                    }

                    // Check verification status
                    if (authRepository.isEmailVerified()) {
                        val user = authRepository.firebaseAuth.currentUser
                        if (user != null) {
                            Log.d("AuthViewModel", "Email verified, updating user data")
                            try {
                                authRepository.saveUserDataAfterVerification(user)
                                _authState.value = AuthState.Success(user)
                                _verificationState.value = VerificationState.Idle
                                return@launch
                            } catch (e: Exception) {
                                Log.e("AuthViewModel", "Error saving user data: ${e.message}")
                                _verificationState.value = VerificationState.Error(
                                    "Error updating user data. Please try logging in again."
                                )
                            }
                        }
                    }
                    
                    delay(VERIFICATION_CHECK_INTERVAL)
                }
                
                // Timeout handling
                _verificationState.value = VerificationState.Timeout
                _authState.value = AuthState.Error(
                    "Email verification timeout. Please try again.",
                    Exception("Verification timeout")
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Verification check error: ${e.message}")
                _verificationState.value = VerificationState.Error(
                    e.message ?: "Verification check failed"
                )
            }
        }
    }

    fun retryVerification() {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                _verificationState.value = VerificationState.Loading
                authRepository.sendEmailVerification()
                _authState.value = AuthState.EmailVerificationRequired
                startVerificationCheck()
            } catch (e: Exception) {
                _verificationState.value = VerificationState.Error(e.message ?: "Failed to retry verification")
                _authState.value = AuthState.Error("Failed to retry verification", e)
            }
        }
    }

    fun checkEmailVerification() {
        viewModelScope.launch {
            if (_authState.value is AuthState.Success) return@launch // Already verified

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
        viewModelScope.launch {
            try {
                verificationCheckJob?.cancel()
                currentAuthJob?.cancel()
                clearFormData()
                Log.d("AuthViewModel", "Successfully cleaned up resources")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error during cleanup: ${e.message}")
            }
        }
    }
}

sealed class ResetPasswordState {
    object Idle : ResetPasswordState()
    object Loading : ResetPasswordState()
    object Success : ResetPasswordState()
    data class Error(val message: String) : ResetPasswordState()
}