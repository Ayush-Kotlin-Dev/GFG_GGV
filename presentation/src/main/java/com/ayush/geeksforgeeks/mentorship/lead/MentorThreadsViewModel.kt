package com.ayush.geeksforgeeks.mentorship.lead

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.data.datastore.UserPreferences
import com.ayush.data.datastore.UserSettings
import com.ayush.data.model.ThreadDetails
import com.ayush.data.repository.MentorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MentorThreadsViewModel @Inject constructor(
    private val mentorRepository: MentorRepository,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    private val _userSettings = MutableStateFlow(UserSettings())
    val userSettings = _userSettings.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _currentSort = MutableStateFlow(ThreadSort.NEWEST_FIRST)
    val currentSort = _currentSort.asStateFlow()

    private val _threads = MutableStateFlow<List<ThreadDetails>>(emptyList())

    // Combine all streams into a single UI state
    val uiState = combine(
        _threads,
        searchQuery,
        currentSort,
    ) { threads, query, sort ->
        try {
            val filteredThreads = threads.filter { thread ->
                if (query.isEmpty()) true else {
                    thread.title.contains(query, ignoreCase = true) ||
                            thread.message.contains(query, ignoreCase = true) ||
                            thread.authorName.contains(query, ignoreCase = true)
                }
            }

            val sortedThreads = when (sort) {
                ThreadSort.NEWEST_FIRST -> filteredThreads.sortedByDescending { it.lastMessageAt }
                ThreadSort.OLDEST_FIRST -> filteredThreads.sortedBy { it.lastMessageAt }
                ThreadSort.MOST_ACTIVE -> filteredThreads.sortedByDescending { it.repliesCount }
            }

            val groupedThreads = sortedThreads.groupBy { it.isEnabled }
            MentorThreadsUiState.Success(
                MentorThreadsList(
                    pendingThreads = groupedThreads[false] ?: emptyList(),
                    activeThreads = groupedThreads[true] ?: emptyList()
                )
            )
        } catch (e: Exception) {
            MentorThreadsUiState.Error(e.message ?: "Failed to load threads")
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MentorThreadsUiState.Loading
    )

    init {
        loadUserSettings()
        loadThreads()
    }

    private fun loadThreads() {
        viewModelScope.launch {
            try {
                mentorRepository.getTeamThreads().collect { threadsList ->
                    _threads.value = threadsList
                }
            } catch (e: Exception) {
                // Handle error if needed
                val errorMessage = e.message ?: "Failed to load threads"
                _threads.value = emptyList()
            }
        }
    }

    private fun loadUserSettings() {
        viewModelScope.launch {
            userPreferences.userData.collect { settings ->
                _userSettings.value = settings
            }
        }
    }

    fun enableThread(threadId: String) {
        viewModelScope.launch {
            try {
                mentorRepository.updateThreadStatus(threadId, true)
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }

    fun deleteThread(threadId: String) {
        viewModelScope.launch {
            try {
                mentorRepository.deleteThread(threadId)
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSort(sort: ThreadSort) {
        _currentSort.value = sort
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
) {
    val totalThreads: Int get() = pendingThreads.size + activeThreads.size
}

enum class ThreadSort(val displayName: String) {
    NEWEST_FIRST("Newest First"),
    OLDEST_FIRST("Oldest First"),
    MOST_ACTIVE("Most Active")
}