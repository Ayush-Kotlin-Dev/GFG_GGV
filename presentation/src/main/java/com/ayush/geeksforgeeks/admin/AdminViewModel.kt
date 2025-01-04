package com.ayush.geeksforgeeks.admin

import android.app.Application
import android.content.Intent
import android.util.Log
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
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
            loadData()
        }
    }

    private suspend fun loadData() {
        try {
            val currentUser = userRepository.getCurrentUser()
            currentUserDomainId = currentUser.domainId

            val teamMembers = userRepository.getTeamMembers()
            _teamMembers.value = teamMembers

            val tasks = taskRepository.getTasks(currentUserDomainId)
            _tasks.value = tasks

            updateTaskStats()
        } catch (e: Exception) {
            Log.e("AdminViewModel", "Error in loadData(): ${e.message}", e)
        }
    }

    private fun updateTaskStats() {
        val allTasks = _tasks.value
        val allMembers = _teamMembers.value
        val stats = mutableMapOf<String, Int>()


        // Overall statistics
        stats["total"] = allTasks.size
        stats["completed"] = allTasks.count { it.status == TaskStatus.COMPLETED }
        stats["inProgress"] = allTasks.count { it.status == TaskStatus.IN_PROGRESS }
        stats["pending"] = allTasks.count { it.status == TaskStatus.PENDING }
        stats["unassigned"] = allTasks.count { it.assignedTo.isEmpty() }

        // Per-user statistics
        allMembers.forEach { member ->
            val completedTasks = allTasks.count { task ->
                task.assignedTo == member.userId && task.status == TaskStatus.COMPLETED
            }
            Log.d(
                "AdminViewModel",
                "Stats for ${member.name} (${member.userId}): $completedTasks completed tasks"
            )
            stats[member.userId] = completedTasks
        }

        _taskStats.value = stats
        Log.d("AdminViewModel", "Final stats: $stats")
    }

    fun addTask(task: Task) {
        viewModelScope.launch {
            try {
                val newTask = task.copy(
                    domainId = currentUserDomainId,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now(),
                    status = TaskStatus.PENDING,
                    dueDate = Timestamp(Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000))
                )
                taskRepository.addTask(newTask)
                loadData()
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error adding task: ${e.message}", e)
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
                loadData()
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error assigning task: ${e.message}", e)
            }
        }
    }

    fun updateTaskStatus(taskId: String, newStatus: TaskStatus) {
        viewModelScope.launch {
            try {
                taskRepository.updateTaskStatus(taskId, newStatus)
                loadData()
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error updating task status: ${e.message}", e)
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                taskRepository.deleteTask(task.id)
                loadData()
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error deleting task: ${e.message}", e)
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
                val currentTeamMembers = teamMembers.value
                val currentTasks = tasks.value
                val oneWeekAgo = Instant.now().minus(7, ChronoUnit.DAYS)

                val fileName = "WeeklyReport_${LocalDate.now().format(DateTimeFormatter.ISO_DATE)}.xlsx"
                val file = File(application.getExternalFilesDir(null), fileName)

                val workbook = XSSFWorkbook()

                val currentTimestamp = Timestamp.now()

                currentTeamMembers.forEach { member ->
                    val sheet = workbook.createSheet(member.name)
                    var rowNum = 0

                    // Add member info
                    var row = sheet.createRow(rowNum++)
                    row.createCell(0).setCellValue("Name:")
                    row.createCell(1).setCellValue(member.name)

                    row = sheet.createRow(rowNum++)
                    row.createCell(0).setCellValue("Role:")
                    row.createCell(1).setCellValue(member.role.toString())

                    // Add current date and time
                    row = sheet.createRow(rowNum++)
                    row.createCell(0).setCellValue("Report Generated:")
                    row.createCell(1).setCellValue(formatTimestamp(currentTimestamp))

                    rowNum++ // Empty row for spacing

                    // Create header row for tasks
                    row = sheet.createRow(rowNum++)
                    val headers =
                        listOf("Task Title", "Status", "Credits", "Due Date", "Updated At")
                    headers.forEachIndexed { index, header ->
                        row.createCell(index).setCellValue(header)
                    }

                    // Add task data
                    val memberTasks = currentTasks.filter { task ->
                        task.assignedTo == member.userId &&
                                isWithinLastWeek(task.updatedAt, oneWeekAgo)
                    }

                    memberTasks.forEach { task ->
                        row = sheet.createRow(rowNum++)
                        row.createCell(0).setCellValue(task.title)
                        row.createCell(1).setCellValue(task.status.toString())
                        row.createCell(2).setCellValue(task.credits.toDouble())
                        row.createCell(3).setCellValue(formatTimestamp(task.dueDate))
                        row.createCell(4).setCellValue(formatTimestamp(task.updatedAt))
                    }

                    rowNum++ // Empty row for spacing

                    // Add summary
                    row = sheet.createRow(rowNum++)
                    row.createCell(0).setCellValue("Total Tasks:")
                    row.createCell(1).setCellValue(memberTasks.size.toDouble())

                    row = sheet.createRow(rowNum++)
                    row.createCell(0).setCellValue("Completed Tasks:")
                    row.createCell(1).setCellValue(memberTasks.count { it.status == TaskStatus.COMPLETED }.toDouble())

                    row = sheet.createRow(rowNum)
                    row.createCell(0).setCellValue("Total Credits:")
                    row.createCell(1).setCellValue(memberTasks.sumOf { it.credits }.toDouble())

                    // Instead of auto-sizing, set a fixed column width
                    for (i in 0..4) {
                        sheet.setColumnWidth(i, 15 * 256) // 15 characters wide
                    }
                }

                FileOutputStream(file).use { fileOut ->
                    workbook.write(fileOut)
                }
                workbook.close()

                // Share the file
                shareFile(file)

                Log.d("AdminViewModel", "Weekly report generated successfully")
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error generating weekly report: ${e.message}", e)
            }
        }
    }

    private fun isWithinLastWeek(timestamp: Timestamp, oneWeekAgo: Instant): Boolean {
        val instant = Instant.ofEpochSecond(timestamp.seconds, timestamp.nanoseconds.toLong())
        return instant.isAfter(oneWeekAgo)
    }

    private fun formatTimestamp(timestamp: Timestamp): String {
        val instant = Instant.ofEpochSecond(timestamp.seconds, timestamp.nanoseconds.toLong())
        val zoneId = ZoneId.systemDefault()
        val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm:ss a z")
        return instant.atZone(zoneId).format(formatter)
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