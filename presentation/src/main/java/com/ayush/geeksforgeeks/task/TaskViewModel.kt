package com.ayush.geeksforgeeks.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.data.model.Task
import com.ayush.data.model.TaskStatus
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
                val taskCounts = TaskCounts(
                    pending = tasks.count { it.status == TaskStatus.PENDING },
                    inProgress = tasks.count { it.status == TaskStatus.IN_PROGRESS },
                    completed = tasks.count { it.status == TaskStatus.COMPLETED }
                )
                _uiState.value = TasksUiState.Success(tasks, taskCounts)
            } catch (e: Exception) {
                _uiState.value = TasksUiState.Error(e.message ?: "An error occurred")
            }
        }
    }

    data class TaskCounts(
        val pending: Int = 0,
        val inProgress: Int = 0,
        val completed: Int = 0
    )

    sealed class TasksUiState {
        object Loading : TasksUiState()
        data class Success(
            val tasks: List<Task>,
            val taskCounts: TaskCounts
        ) : TasksUiState()
        data class Error(val message: String) : TasksUiState()
    }
}