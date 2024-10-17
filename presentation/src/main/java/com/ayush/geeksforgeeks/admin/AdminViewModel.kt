package com.ayush.geeksforgeeks.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.data.datastore.User
import com.ayush.data.model.Task
import com.ayush.data.model.TaskStatus
import com.ayush.data.repository.TaskRepository
import com.ayush.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import com.google.firebase.Timestamp

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

    private val _taskStats = MutableStateFlow<Map<String, Int>>(emptyMap())
    val taskStats: StateFlow<Map<String, Int>> = _taskStats

    private var currentUserDomainId: Int = 0

    init {
        viewModelScope.launch {
            currentUserDomainId = userRepository.getCurrentUser().domainId
            loadTeamMembers()
            loadTasks()
            updateTaskStats()
        }
    }

    private fun loadTeamMembers() {
        viewModelScope.launch {
            try {
                _teamMembers.value = userRepository.getTeamMembers()
            } catch (e: Exception) {
                // Handle error - could emit to a UI state
                e.printStackTrace()
            }
        }
    }

    private fun loadTasks() {
        viewModelScope.launch {
            try {
                _tasks.value = taskRepository.getTasks(currentUserDomainId)
                updateTaskStats()
            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
            }
        }
    }

    private fun updateTaskStats() {
        viewModelScope.launch {
            val allTasks = _tasks.value
            val stats = mutableMapOf<String, Int>()

            // Overall statistics
            stats["total"] = allTasks.size
            stats["completed"] = allTasks.count { it.status == TaskStatus.COMPLETED }
            stats["inProgress"] = allTasks.count { it.status == TaskStatus.IN_PROGRESS }
            stats["pending"] = allTasks.count { it.status == TaskStatus.PENDING }
            stats["unassigned"] = allTasks.count { it.assignedTo.isEmpty() }

            // Per-user statistics
            _teamMembers.value.forEach { member ->
                val completedTasks = allTasks.count {
                    it.assignedTo == member.userId && it.status == TaskStatus.COMPLETED
                }
                stats[member.userId] = completedTasks
            }

            _taskStats.value = stats
        }
    }

    fun addTask(task: Task) {
        viewModelScope.launch {
            try {
                val newTask = task.copy(
                    domainId = currentUserDomainId,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now(),
                    status = TaskStatus.PENDING,
                    dueDate = Timestamp(Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000)) // 1 week from now
                )
                taskRepository.addTask(newTask)
                loadTasks()
            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
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
            try {
                taskRepository.assignTask(taskId, userId)
                loadTasks()
            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
            }
        }
    }

    fun updateTaskStatus(taskId: String, newStatus: TaskStatus) {
        viewModelScope.launch {
            try {
                taskRepository.updateTaskStatus(taskId, newStatus)
                loadTasks()
            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
            }
        }
    }

    fun deleteTask(task: Task) {
        // Add this method to your TaskRepository
        viewModelScope.launch {
            try {
                taskRepository.deleteTask(task.id)
                loadTasks()
            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
            }
        }
    }

    fun getTaskById(taskId: String) {
        viewModelScope.launch {
            try {
                taskRepository.getTaskById(taskId)
            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
            }
        }
    }
}