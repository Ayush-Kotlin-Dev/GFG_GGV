package com.ayush.geeksforgeeks.admin

import android.app.Application
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.data.datastore.User
import com.ayush.data.model.Task
import com.ayush.data.model.TaskStatus
import com.ayush.data.repository.TaskRepository
import com.ayush.data.repository.UserRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
    private val application: Application
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

    fun generateWeeklyReport() {
        viewModelScope.launch {
            try {
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Weekly Report")

                // Create header row
                val headerRow = sheet.createRow(0)
                headerRow.createCell(0).setCellValue("Name")
                headerRow.createCell(1).setCellValue("Role")
                headerRow.createCell(2).setCellValue("Completed Tasks")
                headerRow.createCell(3).setCellValue("Task Titles")
                headerRow.createCell(4).setCellValue("Completed Credits")

                // Populate data
                var rowNum = 1
                // Get the current values from StateFlow instead of using .value directly
                val currentTeamMembers = teamMembers.value
                val currentTasks = tasks.value

                currentTeamMembers.forEach { member ->
                    val row = sheet.createRow(rowNum++)
                    row.createCell(0).setCellValue(member.name)
                    row.createCell(1).setCellValue(member.role.toString())

                    // Filter tasks correctly using member.userId instead of member.name
                    val completedTasks = currentTasks.filter { task ->
                        task.assignedTo == member.userId &&
                                task.status == TaskStatus.COMPLETED
                    }

                    // Set completed tasks count
                    row.createCell(2).setCellValue(completedTasks.size.toDouble())

                    // Get task titles and join them
                    val taskTitles = completedTasks.joinToString(", ") { it.title }
                    row.createCell(3).setCellValue(taskTitles)

                    // Calculate total credits
                    val totalCredits = completedTasks.sumOf { it.credits }
                    row.createCell(4).setCellValue(totalCredits.toDouble())
                }

                // Save the workbook
                val fileName = "WeeklyReport_${LocalDate.now().format(DateTimeFormatter.ISO_DATE)}.xlsx"
                val file = File(application.getExternalFilesDir(null), fileName)
                FileOutputStream(file).use { outputStream ->
                    workbook.write(outputStream)
                }
                workbook.close()

                // Share the file
                shareFile(file)
            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
            }
        }
    }

    private fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(
            application,
            "${application.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share Weekly Report")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(chooser)
    }
}