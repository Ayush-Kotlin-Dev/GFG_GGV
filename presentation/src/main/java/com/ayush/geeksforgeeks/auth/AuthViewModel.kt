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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <T> SavedStateHandle.delegate(key: String, defaultValue: T): ReadWriteProperty<Any, T> =
    object : ReadWriteProperty<Any, T> {
        override fun getValue(thisRef: Any, property: KProperty<*>): T {
            return get(key) ?: defaultValue
        }
        override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
            set(key, value)
        }
    }
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
                _teamMembers.value = authRepository.getTeamMembers(teamId)
            } catch (e: Exception) {
                // Handle error, maybe set an error state
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

            _authState.value = result.fold(
                onSuccess = { AuthState.Success(it) },
                onFailure = { AuthState.Error(it.message ?: "Sign up failed", it) }
            )
        }
    }

    fun login() {
        currentAuthJob?.cancel()
        currentAuthJob = viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.login(email, password)
            _authState.value = result.fold(
                onSuccess = { AuthState.Success(it) },
                onFailure = { AuthState.Error(it.message ?: "Login failed", it) }
            )
        }
    }

    private fun clearFormData() {
        email = ""
        password = ""
        selectedTeam = null
        selectedMember = null
    }

    suspend fun getUserRoleOnLogin(): UserRole =
        authRepository.getUserRole(email)

    override fun onCleared() {
        super.onCleared()
        currentAuthJob?.cancel()
    }
}

sealed class ResetPasswordState {
    object Idle : ResetPasswordState()
    object Loading : ResetPasswordState()
    object Success : ResetPasswordState()
    data class Error(val message: String) : ResetPasswordState()
}