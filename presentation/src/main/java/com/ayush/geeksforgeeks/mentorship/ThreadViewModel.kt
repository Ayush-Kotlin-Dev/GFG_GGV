// ThreadViewModel.kt
package com.ayush.geeksforgeeks.mentorship

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.data.datastore.UserPreferences
import com.ayush.data.datastore.UserRole
import com.ayush.data.datastore.UserSettings
import com.ayush.data.repository.MentorshipRepository
import com.ayush.data.repository.ThreadDetails
import com.ayush.data.repository.ThreadMessage
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

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _currentUser = MutableStateFlow<UserSettings?>(null)
    val currentUser: StateFlow<UserSettings?> = _currentUser.asStateFlow()

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

    private var messagesJob: Job? = null

    fun loadThread(teamId: String, threadId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Load thread details using Flow
                launch {
                    mentorshipRepository.getThreadDetailsFlow(teamId, threadId)
                        .catch { e ->
                            Log.e("ThreadViewModel", "Error loading thread details: ${e.message}")
                            _error.value = e.message
                        }
                        .collect { details ->
                            _threadDetails.value = details
                            Log.d("ThreadViewModel", "Thread details loaded: $details")
                        }
                }

                // Cancel existing messages subscription if any
                messagesJob?.cancel()

                // Start listening to messages
                messagesJob = launch {
                    mentorshipRepository.getMessages(teamId, threadId)
                        .catch { e ->
                            Log.e("ThreadViewModel", "Error loading messages: ${e.message}")
                            _error.value = e.message
                        }
                        .collect { messagesList ->
                            _messages.value = messagesList
                        }
                }
            } catch (e: Exception) {
                Log.e("ThreadViewModel", "Error loading thread: ${e.message}")
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun isTeamLead(): Boolean = 
        userPreferences.userData.first().role == UserRole.TEAM_LEAD

    fun sendMessage(message: String) {
        if (message.isBlank()) return

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val teamId = _threadDetails.value?.teamId ?: return@launch
                val threadId = _threadDetails.value?.id ?: return@launch

                mentorshipRepository.sendMessage(
                    teamId = teamId,
                    threadId = threadId,
                    message = message.trim()
                ).onFailure { error ->
                    _error.value = error.message ?: "Failed to send message"
                }
            } catch (e: Exception) {
                Log.e("ThreadViewModel", "Error sending message: ${e.message}")
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun enableThread() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val teamId = _threadDetails.value?.teamId ?: return@launch
                val threadId = _threadDetails.value?.id ?: return@launch
                // Simulate thread enable success
                delay(1000) // Simulate network delay
                _threadDetails.value = _threadDetails.value?.copy(isEnabled = true)
//                mentorshipRepository.updateThreadStatus(
//                    teamId = teamId,
//                    threadId = threadId,
//                    isEnabled = true
//                ).onSuccess {
//                    _threadDetails.value = _threadDetails.value?.copy(isEnabled = true)
//                }.onFailure { error ->
//                    _error.value = error.message
//                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        messagesJob?.cancel()
    }
}