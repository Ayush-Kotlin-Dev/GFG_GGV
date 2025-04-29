// ThreadViewModel.kt
package com.ayush.geeksforgeeks.presentation.mentorship

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.geeksforgeeks.data.datastore.UserPreferences
import com.ayush.geeksforgeeks.data.datastore.UserRole
import com.ayush.geeksforgeeks.data.datastore.UserSettings
import com.ayush.geeksforgeeks.data.repository.MentorshipRepository
import com.ayush.geeksforgeeks.data.model.ThreadDetails
import com.ayush.geeksforgeeks.data.model.ThreadMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class ThreadViewModel @Inject constructor(
    private val mentorshipRepository: MentorshipRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ThreadMessage>>(emptyList())
    val messages: StateFlow<List<ThreadMessage>> = _messages.asStateFlow()

    private val _threadDetails = MutableStateFlow<ThreadDetails?>(null)
    val threadDetails: StateFlow<ThreadDetails?> = _threadDetails.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _currentUser = MutableStateFlow<UserSettings?>(null)
    val currentUser: StateFlow<UserSettings?> = _currentUser.asStateFlow()

    private var messagesJob: Job? = null
    private var threadDetailsJob: Job? = null

    private var currentTeamId: String? = null
    private var currentThreadId: String? = null

    init {
        viewModelScope.launch {
            userPreferences.userData
                .catch { e -> 
                    Log.e("ThreadViewModel", "Error loading user data: ${e.message}")
                }
                .collect { settings ->
                    _currentUser.value = settings
                }
        }
    }

    fun loadThread(teamId: String, threadId: String) {
        currentTeamId = teamId
        currentThreadId = threadId

        // Cancel any existing jobs
        threadDetailsJob?.cancel()
        messagesJob?.cancel()

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Load thread details using Flow
                threadDetailsJob = launch {
                    mentorshipRepository.getThreadDetailsFlow(teamId, threadId)
                        .catch { e ->
                            if (e is CancellationException) throw e

                            Log.e("ThreadViewModel", "Error loading thread details: ${e.message}")
                            _error.value = "Failed to load thread details: ${e.message}"
                        }
                        .collect { details ->
                            // Don't update with null if we already have details (helps with offline support)
                            if (details != null || _threadDetails.value == null) {
                                _threadDetails.value = details
                            }
                            Log.d("ThreadViewModel", "Thread details loaded: $details")
                        }
                }

                // Start listening to messages
                messagesJob = launch {
                    mentorshipRepository.getMessages(teamId, threadId)
                        .catch { e ->
                            if (e is CancellationException) throw e

                            Log.e("ThreadViewModel", "Error loading messages: ${e.message}")
                            _error.value = "Failed to load messages: ${e.message}"
                        }
                        .collect { messagesList ->
                            _messages.value = messagesList
                        }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                Log.e("ThreadViewModel", "Error loading thread: ${e.message}")
                _error.value = "Error loading thread: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshThread() {
        val teamId = currentTeamId ?: return
        val threadId = currentThreadId ?: return

        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                _error.value = null

                // Reload thread details directly
                try {
                    val threadDetails = mentorshipRepository.getThreadDetails(teamId, threadId)
                    if (threadDetails != null) {
                        _threadDetails.value = threadDetails
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e("ThreadViewModel", "Error refreshing thread details: ${e.message}")
                    // Don't set error here, as we might still get messages
                }

                // We don't need to explicitly refresh messages, as they're already coming from a Flow
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                Log.e("ThreadViewModel", "Error refreshing thread: ${e.message}")
                _error.value = "Error refreshing thread: ${e.message}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    suspend fun isTeamLead(): Boolean = 
        userPreferences.userData.first().role == UserRole.TEAM_LEAD

    fun sendMessage(message: String): Flow<Result<ThreadMessage>> = flow {
        if (message.isBlank()) {
            emit(Result.failure(IllegalArgumentException("Message cannot be empty")))
            return@flow
        }

        _isLoading.value = true
        _error.value = null

        try {
            val teamId = currentTeamId ?: throw IllegalStateException("No team selected")
            val threadId = currentThreadId ?: throw IllegalStateException("No thread selected")

            val result = mentorshipRepository.sendMessage(
                teamId = teamId,
                threadId = threadId,
                message = message.trim()
            )

            result.onFailure { error ->
                _error.value = error.message ?: "Failed to send message"
            }

            emit(result)
        } catch (e: Exception) {
            if (e is CancellationException) throw e

            Log.e("ThreadViewModel", "Error sending message: ${e.message}")
            _error.value = e.message
            emit(Result.failure(e))
        } finally {
            _isLoading.value = false
        }
    }

    fun enableThread(): Flow<Result<Unit>> = flow {
        _isLoading.value = true
        _error.value = null

        try {
            val teamId = currentTeamId ?: throw IllegalStateException("No team selected")
            val threadId = currentThreadId ?: throw IllegalStateException("No thread selected")

            val result = mentorshipRepository.updateThreadStatus(
                teamId = teamId,
                threadId = threadId,
                isEnabled = true
            )

            result.onSuccess {
                _threadDetails.value = _threadDetails.value?.copy(isEnabled = true)
            }.onFailure { error ->
                _error.value = error.message ?: "Failed to enable thread"
            }

            emit(result)
        } catch (e: Exception) {
            if (e is CancellationException) throw e

            _error.value = e.message
            emit(Result.failure(e))
        } finally {
            _isLoading.value = false
        }
    }

    fun resolveThread(resolved: Boolean): Flow<Result<Unit>> = flow {
        _isLoading.value = true
        _error.value = null

        try {
            val teamId = currentTeamId ?: throw IllegalStateException("No team selected")
            val threadId = currentThreadId ?: throw IllegalStateException("No thread selected")

            val result = mentorshipRepository.updateThreadStatus(
                teamId = teamId,
                threadId = threadId,
                isResolved = resolved
            )

            result.onSuccess {
                _threadDetails.value = _threadDetails.value?.copy(isResolved = resolved)
            }.onFailure { error ->
                _error.value = error.message ?: "Failed to update thread status"
            }

            emit(result)
        } catch (e: Exception) {
            if (e is CancellationException) throw e

            _error.value = e.message ?: "Error updating thread"
            emit(Result.failure(e))
        } finally {
            _isLoading.value = false
        }
    }

    fun pinThread(pinned: Boolean): Flow<Result<Unit>> = flow {
        _isLoading.value = true
        _error.value = null

        try {
            val teamId = currentTeamId ?: throw IllegalStateException("No team selected")
            val threadId = currentThreadId ?: throw IllegalStateException("No thread selected")

            val result = mentorshipRepository.updateThreadStatus(
                teamId = teamId,
                threadId = threadId,
                isPinned = pinned
            )

            result.onSuccess {
                _threadDetails.value = _threadDetails.value?.copy(isPinned = pinned)
            }.onFailure { error ->
                _error.value = error.message ?: "Failed to update pinned status"
            }

            emit(result)
        } catch (e: Exception) {
            if (e is CancellationException) throw e

            _error.value = e.message ?: "Error updating thread"
            emit(Result.failure(e))
        } finally {
            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        messagesJob?.cancel()
        threadDetailsJob?.cancel()
    }
}