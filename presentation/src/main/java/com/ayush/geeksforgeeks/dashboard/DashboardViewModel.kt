package com.ayush.geeksforgeeks.dashboard


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.data.datastore.UserSettings
import com.ayush.data.model.CreditLog
import com.ayush.data.repository.UserRepository
import com.ayush.data.repository.TaskRepository
import com.ayush.data.repository.CreditRepository
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
                val completedTasks = taskRepository.getCompletedTasksCount(user.userId)
                val creditHistory = creditRepository.getUserCreditHistory(user.userId)
                val topContributors = userRepository.getTopContributors(5)

                _uiState.value = DashboardUiState.Success(
                    user = user,
                    completedTasks = completedTasks,
                    creditHistory = creditHistory,
                    topContributors = topContributors
                )
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error(e.message ?: "An error occurred")
            }
        }
    }

    sealed class DashboardUiState {
        object Loading : DashboardUiState()
        data class Success(
            val user: UserSettings,
            val completedTasks: Int,
            val creditHistory: List<CreditLog>,
            val topContributors: List<UserSettings>
        ) : DashboardUiState()
        data class Error(val message: String) : DashboardUiState()
    }
}