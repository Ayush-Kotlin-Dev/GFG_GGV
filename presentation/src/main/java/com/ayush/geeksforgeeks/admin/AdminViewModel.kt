package com.ayush.geeksforgeeks.admin

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

//    init {
//        loadTeamMembers()
//        loadTasks()
//    }

    fun loadInitialData() {
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
            taskRepository.addTask(task)
            loadTasks() // Reload tasks after adding a new one
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
            taskRepository.assignTask(taskId, userId)
            loadTasks() // Reload tasks after assigning
        }
    }
}