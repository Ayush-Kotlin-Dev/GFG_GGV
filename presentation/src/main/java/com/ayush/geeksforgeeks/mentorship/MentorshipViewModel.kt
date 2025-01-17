package com.ayush.geeksforgeeks.mentorship

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.data.model.Team
import com.ayush.data.repository.MentorshipThread
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.ayush.data.repository.MentorshipRepository

@HiltViewModel
class MentorshipViewModel @Inject constructor(
    private val mentorshipRepository: MentorshipRepository
) : ViewModel() {

    private val _teams = MutableStateFlow<List<Team>>(emptyList())
    val teams: StateFlow<List<Team>> = _teams.asStateFlow()

    private val _selectedTeam = MutableStateFlow<Team?>(null)
    val selectedTeam: StateFlow<Team?> = _selectedTeam.asStateFlow()

    private val _threads = MutableStateFlow<List<MentorshipThread>>(emptyList())
    val threads: StateFlow<List<MentorshipThread>> = _threads.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadTeams()
    }

    private fun loadTeams() {
        viewModelScope.launch {
            try {
                _teams.value = mentorshipRepository.getTeams()
            } catch (e: Exception) {
                Log.e("MentorshipViewModel", "Error loading teams: ${e.message}")
            }
        }
    }

    fun selectTeam(team: Team) {
        _selectedTeam.value = team
        loadThreads(team.id)
    }

    private fun loadThreads(teamId: String) {
        viewModelScope.launch {
            try {
                _threads.value = mentorshipRepository.getThreads(teamId)
            } catch (e: Exception) {
                Log.e("MentorshipViewModel", "Error loading threads: ${e.message}")
            }
        }
    }

    fun createThread(title: String, message: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val teamId = _selectedTeam.value?.id ?: return@launch
                mentorshipRepository.createThread(teamId, title, message).onSuccess {
                    loadThreads(teamId)
                }.onFailure { error ->
                    _error.value = error.message ?: "Failed to create thread"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
}