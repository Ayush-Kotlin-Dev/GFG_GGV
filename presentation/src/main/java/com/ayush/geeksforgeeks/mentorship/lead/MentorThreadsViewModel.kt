package com.ayush.geeksforgeeks.mentorship.lead

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.data.model.ThreadDetails
import com.ayush.data.repository.MentorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MentorThreadsViewModel @Inject constructor(
    private val mentorRepository: MentorRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MentorThreadsUiState>(MentorThreadsUiState.Loading)
    val uiState: StateFlow<MentorThreadsUiState> = _uiState.asStateFlow()

    init {
        loadThreads()
    }

    private fun loadThreads() {
        viewModelScope.launch {
            try {
                mentorRepository.getTeamThreads()
                    .collect { threads ->
                        _uiState.value = MentorThreadsUiState.Success(
                            threads.groupBy { it.isEnabled }
                                .let { grouped ->
                                    MentorThreadsList(
                                        pendingThreads = grouped[false] ?: emptyList(),
                                        activeThreads = grouped[true] ?: emptyList()
                                    )
                                }
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = MentorThreadsUiState.Error(e.message ?: "Failed to load threads")
            }
        }
    }

    fun enableThread(threadId: String) {
        viewModelScope.launch {
            try {
                mentorRepository.updateThreadStatus(threadId, true)
                // No need to reload threads as we're using Flow and will get updates automatically
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }
}

sealed interface MentorThreadsUiState {
    data object Loading : MentorThreadsUiState
    data class Success(val threads: MentorThreadsList) : MentorThreadsUiState
    data class Error(val message: String) : MentorThreadsUiState
}

data class MentorThreadsList(
    val pendingThreads: List<ThreadDetails>,
    val activeThreads: List<ThreadDetails>
)