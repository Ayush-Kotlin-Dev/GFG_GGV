package com.ayush.geeksforgeeks.presentation.mentorship

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.geeksforgeeks.data.model.Team
import com.ayush.geeksforgeeks.data.model.ThreadDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.ayush.geeksforgeeks.data.repository.MentorshipRepository

// Define UI States
sealed interface TeamsUiState {
    data object Loading : TeamsUiState
    data class Error(val message: String) : TeamsUiState
    data class Success(val teams: List<Team>) : TeamsUiState
}

sealed interface ThreadsUiState {
    data object Loading : ThreadsUiState
    data class Error(val message: String) : ThreadsUiState
    data class Success(
        val threads: List<ThreadDetails>,
        val selectedTeam: Team
    ) : ThreadsUiState
}

data class CreateThreadUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class MentorshipViewModel @Inject constructor(
    private val mentorshipRepository: MentorshipRepository
) : ViewModel() {

    private val _teamsUiState = MutableStateFlow<TeamsUiState>(TeamsUiState.Loading)
    val teamsUiState: StateFlow<TeamsUiState> = _teamsUiState.asStateFlow()

    private val _threadsUiState = MutableStateFlow<ThreadsUiState>(ThreadsUiState.Loading)
    val threadsUiState: StateFlow<ThreadsUiState> = _threadsUiState.asStateFlow()

    private val _createThreadUiState = MutableStateFlow(CreateThreadUiState())
    val createThreadUiState: StateFlow<CreateThreadUiState> = _createThreadUiState.asStateFlow()

    // Expose simple states for convenience in UI
    private val _threads = MutableStateFlow<List<ThreadDetails>>(emptyList())
    val threads: StateFlow<List<ThreadDetails>> = _threads.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var loadThreadsJob: Job? = null
    private var selectedTeamId: String? = null

    init {
        loadTeams()
    }

    fun loadTeams(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _teamsUiState.value = TeamsUiState.Loading
            _isLoading.value = true

            try {
                val teams = mentorshipRepository.getTeams()
                _teamsUiState.value = TeamsUiState.Success(teams)
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                Log.e("MentorshipViewModel", "Error loading teams: ${e.message}")
                _teamsUiState.value = TeamsUiState.Error(
                    e.message ?: "Failed to load teams"
                )
                _errorMessage.value = e.message ?: "Failed to load teams"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectTeam(team: Team) {
        loadThreads(team)
    }

    private fun loadThreads(team: Team) {
        loadThreadsJob?.cancel()
        selectedTeamId = team.id

        loadThreadsJob = viewModelScope.launch {
            _threadsUiState.value = ThreadsUiState.Loading
            _isLoading.value = true

            try {
                val threads = mentorshipRepository.getThreads(team.id)
                _threadsUiState.value = ThreadsUiState.Success(
                    threads = threads,
                    selectedTeam = team
                )
                _threads.value = threads
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                Log.e("MentorshipViewModel", "Error loading threads: ${e.message}")
                _threadsUiState.value = ThreadsUiState.Error(
                    e.message ?: "Failed to load discussions"
                )
                _errorMessage.value = e.message ?: "Failed to load discussions"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadThreads(teamId: String) {
        loadThreadsJob?.cancel()
        selectedTeamId = teamId

        loadThreadsJob = viewModelScope.launch {
            _isLoading.value = true

            try {
                val threads = mentorshipRepository.getThreads(teamId)
                _threads.value = threads
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                Log.e("MentorshipViewModel", "Error loading threads: ${e.message}")
                _errorMessage.value = e.message ?: "Failed to load discussions"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshThreads() {
        selectedTeamId?.let { teamId ->
            loadThreadsJob?.cancel()

            loadThreadsJob = viewModelScope.launch {
                _isRefreshing.value = true

                try {
                    val threads = mentorshipRepository.getThreads(teamId)
                    _threads.value = threads

                    // Also update the threadsUiState if it's in Success state
                    val currentState = _threadsUiState.value
                    if (currentState is ThreadsUiState.Success) {
                        _threadsUiState.value = currentState.copy(threads = threads)
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e

                    Log.e("MentorshipViewModel", "Error refreshing threads: ${e.message}")
                    _errorMessage.value = e.message ?: "Failed to refresh discussions"
                } finally {
                    _isRefreshing.value = false
                }
            }
        }
    }

    fun createThread(
        teamId: String,
        title: String,
        message: String,
        category: String = "General",
        tags: List<String> = emptyList()
    ): Flow<Result<ThreadDetails>> = flow {
        _createThreadUiState.update { it.copy(isLoading = true, error = null) }
        _isLoading.value = true

        try {
            val result = mentorshipRepository.createThread(
                teamId,
                title,
                message,
                category,
                tags
            )

            result.onSuccess {
                _createThreadUiState.update {
                    it.copy(isLoading = false, isSuccess = true)
                }
                // Refresh threads list after creating a new thread
                refreshThreads()
            }.onFailure { error ->
                _createThreadUiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to create thread"
                    )
                }
                _errorMessage.value = error.message ?: "Failed to create thread"
            }

            emit(result)
        } catch (e: Exception) {
            if (e is CancellationException) throw e

            val errorMsg = e.message ?: "Unknown error occurred"
            _createThreadUiState.update {
                it.copy(
                    isLoading = false,
                    error = errorMsg
                )
            }
            _errorMessage.value = errorMsg
            emit(Result.failure(e))
        } finally {
            _isLoading.value = false
        }
    }

    fun updateThreadStatus(
        teamId: String,
        threadId: String,
        isEnabled: Boolean? = null,
        isPinned: Boolean? = null,
        isResolved: Boolean? = null
    ): Flow<Result<Unit>> = flow {
        _isLoading.value = true

        try {
            val result = mentorshipRepository.updateThreadStatus(
                teamId,
                threadId,
                isEnabled,
                isPinned,
                isResolved
            )

            result.onSuccess {
                // Update the local threads list to reflect changes immediately
                val updatedThreads = _threads.value.map { thread ->
                    if (thread.id == threadId) {
                        thread.copy(
                            isEnabled = isEnabled ?: thread.isEnabled,
                            isPinned = isPinned ?: thread.isPinned,
                            isResolved = isResolved ?: thread.isResolved
                        )
                    } else {
                        thread
                    }
                }
                _threads.value = updatedThreads

                // Also update ThreadsUiState if needed
                val currentState = _threadsUiState.value
                if (currentState is ThreadsUiState.Success) {
                    _threadsUiState.value = currentState.copy(threads = updatedThreads)
                }
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to update thread status"
            }

            emit(result)
        } catch (e: Exception) {
            if (e is CancellationException) throw e

            val errorMsg = e.message ?: "Unknown error occurred while updating thread"
            _errorMessage.value = errorMsg
            emit(Result.failure(e))
        } finally {
            _isLoading.value = false
        }
    }

    fun resetCreateThreadState() {
        _createThreadUiState.value = CreateThreadUiState()
    }

    fun clearError() {
        _errorMessage.value = null

        when (val currentTeamsState = _teamsUiState.value) {
            is TeamsUiState.Error -> loadTeams()
            else -> Unit
        }

        when (val currentThreadsState = _threadsUiState.value) {
            is ThreadsUiState.Error -> {
                val state = _threadsUiState.value
                if (state is ThreadsUiState.Success) {
                    loadThreads(state.selectedTeam)
                } else if (selectedTeamId != null) {
                    loadThreads(selectedTeamId!!)
                }
            }
            else -> Unit
        }

        _createThreadUiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        loadThreadsJob?.cancel()
    }
}