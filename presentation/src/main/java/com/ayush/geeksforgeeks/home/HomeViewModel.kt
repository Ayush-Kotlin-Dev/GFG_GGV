package com.ayush.geeksforgeeks.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.data.datastore.UserSettings
import com.ayush.data.model.Task
import com.ayush.data.repository.UserRepository
import com.ayush.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadHomeData()
        taskRepository.seedDummyTasks()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            try {
                val user = userRepository.getCurrentUser()
                val recentTasks = taskRepository.getRecentTasksForUser(user.userId, limit = 5)
                _uiState.value = HomeUiState.Success(
                    user = user,
                    recentTasks = recentTasks
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "An error occurred")
            }
        }
    }

    sealed class HomeUiState {
        object Loading : HomeUiState()
        data class Success(
            val user: UserSettings,
            val recentTasks: List<Task>
        ) : HomeUiState()
        data class Error(val message: String) : HomeUiState()
    }
}