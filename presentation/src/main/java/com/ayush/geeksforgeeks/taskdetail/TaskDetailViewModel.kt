package com.ayush.geeksforgeeks.taskdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.data.model.Task
import com.ayush.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _taskState = MutableStateFlow<TaskState>(TaskState.Loading)
    val taskState: StateFlow<TaskState> = _taskState

    fun loadTask(taskId: String) {
        viewModelScope.launch {
            try {
                val task = taskRepository.getTaskById(taskId)
                if (task != null) {
                    _taskState.value = TaskState.Success(task)
                } else {
                    _taskState.value = TaskState.Error("Task not found")
                }
            } catch (e: Exception) {
                _taskState.value = TaskState.Error(e.message ?: "An error occurred")
            }
        }
    }

    sealed class TaskState {
        object Loading : TaskState()
        data class Success(val task: Task) : TaskState()
        data class Error(val message: String) : TaskState()
    }
}