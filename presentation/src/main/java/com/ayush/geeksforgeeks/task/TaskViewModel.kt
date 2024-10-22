package com.ayush.geeksforgeeks.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.data.model.Task
import com.ayush.data.repository.TaskRepository
import com.ayush.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TasksUiState>(TasksUiState.Loading)
    val uiState: StateFlow<TasksUiState> = _uiState

    private var currentUserId: String = ""

    init {
        loadTasks()
    }

    private fun loadTasks() {
        viewModelScope.launch {
            try {
                val user = userRepository.getCurrentUser()
                currentUserId = user.userId
                val tasks = taskRepository.getTasksForUser(currentUserId)
                _uiState.value = TasksUiState.Success(tasks)
            } catch (e: Exception) {
                _uiState.value = TasksUiState.Error(e.message ?: "An error occurred")
            }
        }
    }

    sealed class TasksUiState {
        object Loading : TasksUiState()
        data class Success(val tasks: List<Task>) : TasksUiState()
        data class Error(val message: String) : TasksUiState()
    }
}