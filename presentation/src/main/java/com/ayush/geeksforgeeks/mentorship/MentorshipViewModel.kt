package com.ayush.geeksforgeeks.mentorship

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.data.model.Team
import com.ayush.data.model.ThreadDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.ayush.data.repository.MentorshipRepository

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

    init {
        loadTeams()
    }

    fun loadTeams() {
        viewModelScope.launch {
            _teamsUiState.value = TeamsUiState.Loading
            try {
                val teams = mentorshipRepository.getTeams()
                _teamsUiState.value = TeamsUiState.Success(teams)
            } catch (e: Exception) {
                Log.e("MentorshipViewModel", "Error loading teams: ${e.message}")
                _teamsUiState.value = TeamsUiState.Error(
                    e.message ?: "Failed to load teams"
                )
            }
        }
    }

    fun selectTeam(team: Team) {
        loadThreads(team)
    }

    private fun loadThreads(team: Team) {
        viewModelScope.launch {
            _threadsUiState.value = ThreadsUiState.Loading
            try {
                val threads = mentorshipRepository.getThreads(team.id)
                _threadsUiState.value = ThreadsUiState.Success(
                    threads = threads,
                    selectedTeam = team
                )
            } catch (e: Exception) {
                Log.e("MentorshipViewModel", "Error loading threads: ${e.message}")
                _threadsUiState.value = ThreadsUiState.Error(
                    e.message ?: "Failed to load discussions"
                )
            }
        }
    }

    fun createThread(title: String, message: String, category: String = "General", tags: List<String> = emptyList()) {
        viewModelScope.launch {
            _createThreadUiState.update { it.copy(isLoading = true, error = null) }

            try {
                val currentState = _threadsUiState.value
                if (currentState !is ThreadsUiState.Success) {
                    _createThreadUiState.update {
                        it.copy(
                            isLoading = false,
                            error = "No team selected"
                        )
                    }
                    return@launch
                }

                mentorshipRepository.createThread(
                    currentState.selectedTeam.id,
                    title,
                    message,
                    category,
                    tags
                ).onSuccess {
                    _createThreadUiState.update {
                        it.copy(isLoading = false, isSuccess = true)
                    }
                    // Reload threads after successful creation
                    loadThreads(currentState.selectedTeam)
                }.onFailure { error ->
                    _createThreadUiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to create thread"
                        )
                    }
                }
            } catch (e: Exception) {
                _createThreadUiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }

    fun resetCreateThreadState() {
        _createThreadUiState.value = CreateThreadUiState()
    }

    fun clearError() {
        when (val currentTeamsState = _teamsUiState.value) {
            is TeamsUiState.Error -> loadTeams()
            else -> Unit
        }

        when (val currentThreadsState = _threadsUiState.value) {
            is ThreadsUiState.Error -> {
                val currentState = _threadsUiState.value
                if (currentState is ThreadsUiState.Success) {
                    loadThreads(currentState.selectedTeam)
                }
            }
            else -> Unit
        }

        _createThreadUiState.update { it.copy(error = null) }
    }
}