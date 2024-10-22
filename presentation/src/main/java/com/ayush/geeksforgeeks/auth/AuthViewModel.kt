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
    var username by mutableStateOf(savedStateHandle.get<String>("username") ?: "")
        private set

    init {
        viewModelScope.launch {
            savedStateHandle.setSavedStateProvider("email") { Bundle().apply { putString("email", email) } }
            savedStateHandle.setSavedStateProvider("password") { Bundle().apply { putString("password", password) } }
            savedStateHandle.setSavedStateProvider("username") { Bundle().apply { putString("username", username) } }
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

    fun updateUsername(newUsername: String) {
        username = newUsername
        savedStateHandle["username"] = newUsername
    }

    fun signUp(domain: Int, role: UserRole) {
        currentAuthJob?.cancel()
        currentAuthJob = viewModelScope.launch {
            _authState.value = AuthState.Loading

            val result = authRepository.signUp(
                username = username,
                email = email,
                password = password,
                domain = domain,
                role = role
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
        username = ""
    }

    suspend fun getUserRoleOnLogin(): UserRole =
        authRepository.getUserRole(email)

    override fun onCleared() {
        super.onCleared()
        currentAuthJob?.cancel()
    }
}

