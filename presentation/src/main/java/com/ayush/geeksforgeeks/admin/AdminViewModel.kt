package com.ayush.geeksforgeeks.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.data.datastore.User
import com.ayush.data.model.Task
import com.ayush.data.repository.TaskRepository
import com.ayush.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _teamMembers = MutableStateFlow<List<User>>(emptyList())
    val teamMembers: StateFlow<List<User>> = _teamMembers

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks

    private val _assignTaskDialogState = MutableStateFlow<Pair<Task, List<User>>?>(null)
    val assignTaskDialogState: StateFlow<Pair<Task, List<User>>?> = _assignTaskDialogState

    init {
        loadTeamMembers()
        loadTasks()
    }



    private fun loadTeamMembers() {
        viewModelScope.launch {
            _teamMembers.value = userRepository.getTeamMembers()
        }
    }

    private fun loadTasks() {
        viewModelScope.launch {
            _tasks.value = taskRepository.getTasks()
        }
    }

    fun addTask(task: Task) {
        viewModelScope.launch {
            try {
                taskRepository.addTask(task)
                loadTasks()
            } catch (e: Exception) {
                // Handle the error, maybe update a UI state to show an error message
            }
        }
    }

    fun showAssignTaskDialog(task: Task) {

        _assignTaskDialogState.value = Pair(task, _teamMembers.value)
    }

    fun dismissAssignTaskDialog() {
        _assignTaskDialogState.value = null
    }

    fun assignTask(taskId: String, userId: String) {
        viewModelScope.launch {
            Log.d("AdminViewModel", "Assigning task $taskId to user $userId")
            taskRepository.assignTask(taskId, userId)
            loadTasks() // Reload tasks after assigning
        }
    }
}