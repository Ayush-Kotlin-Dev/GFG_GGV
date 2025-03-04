package com.ayush.geeksforgeeks.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.data.datastore.UserSettings
import com.ayush.data.model.CreditLog
import com.ayush.data.repository.CreditRepository
import com.ayush.data.repository.TaskRepository
import com.ayush.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val taskRepository: TaskRepository,
    private val creditRepository: CreditRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            try {
                val user = userRepository.getCurrentUser()
                val creditHistory = creditRepository.getUserCreditHistory(user.userId)
                val topContributors = userRepository.getTopContributors(5)
                val clubStats = getClubStats()

                _uiState.value = DashboardUiState.Success(
                    user = user,
                    creditHistory = creditHistory,
                    topContributors = topContributors,
                    clubStats = clubStats
                )
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error(e.message ?: "An error occurred")
            }
        }
    }

    private suspend fun getClubStats(): ClubStats {
        val totalMembers = userRepository.getTotalMembersCount()
        val activeProjects = taskRepository.getActiveProjectsCount()
        val totalCredits = creditRepository.getTotalClubCredits()

        return ClubStats(
            totalMembers = totalMembers,
            activeProjects = activeProjects,
            totalCredits = totalCredits
        )
    }

    sealed class DashboardUiState {
        object Loading : DashboardUiState()
        data class Success(
            val user: UserSettings,
            val creditHistory: List<CreditLog>,
            val topContributors: List<UserSettings>,
            val clubStats: ClubStats
        ) : DashboardUiState()
        data class Error(val message: String) : DashboardUiState()
    }
}