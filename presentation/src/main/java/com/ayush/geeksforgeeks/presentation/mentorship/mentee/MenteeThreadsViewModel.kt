package com.ayush.geeksforgeeks.presentation.mentorship.mentee

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.geeksforgeeks.data.datastore.UserPreferences
import com.ayush.geeksforgeeks.data.model.ThreadDetails
import com.ayush.geeksforgeeks.data.repository.MentorshipRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class MenteeThreadsViewModel @Inject constructor(
    private val mentorshipRepository: MentorshipRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {
    
    private val _threads = MutableStateFlow<List<ThreadDetails>>(emptyList())
    val threads: StateFlow<List<ThreadDetails>> = _threads.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // Search results
    val filteredThreads = combine(_threads, _searchQuery) { threads, query ->
        if (query.isBlank()) {
            threads
        } else {
            threads.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.message.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true) ||
                it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    private var currentTeamId: String? = null
    private var loadThreadsJob: Job? = null
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun loadThreads(teamId: String) {
        currentTeamId = teamId
        loadThreadsJob?.cancel()
        
        loadThreadsJob = viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                val threads = mentorshipRepository.getThreads(teamId)
                _threads.value = threads
                
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                
                Log.e("MenteeThreadsViewModel", "Failed to load threads: ${e.message}")
                _errorMessage.value = e.message ?: "Failed to load discussions"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun refreshThreads() {
        val teamId = currentTeamId ?: return
        loadThreadsJob?.cancel()
        
        loadThreadsJob = viewModelScope.launch {
            try {
                _isRefreshing.value = true
                _errorMessage.value = null
                
                val threads = mentorshipRepository.getThreads(teamId)
                _threads.value = threads
                
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                
                Log.e("MenteeThreadsViewModel", "Failed to refresh threads: ${e.message}")
                _errorMessage.value = e.message ?: "Failed to refresh discussions"
            } finally {
                _isRefreshing.value = false
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
                // Refresh the threads list to include the new thread
                refreshThreads()
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to create thread"
            }
            
            emit(result)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            
            val errorMsg = e.message ?: "Unknown error occurred"
            _errorMessage.value = errorMsg
            emit(Result.failure(e))
        } finally {
            _isLoading.value = false
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        loadThreadsJob?.cancel()
    }
}