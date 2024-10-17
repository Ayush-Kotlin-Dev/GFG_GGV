package com.ayush.geeksforgeeks.admin

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import com.ayush.data.datastore.User
import com.ayush.data.model.Task
import com.ayush.data.model.TaskStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

// Define GFG theme colors
object GFGTheme {
    val Primary = Color(0xFF2F8D46)
    val Secondary = Color(0xFF4CAF50)
    val Background = Color(0xFFF5F5F5)
    val CardBackground = Color.White
    val TextPrimary = Color(0xFF333333)
}

class AdminScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel: AdminViewModel = hiltViewModel()
        val teamMembers by viewModel.teamMembers.collectAsState()
        val tasks by viewModel.tasks.collectAsState()
        val stats by viewModel.taskStats.collectAsState()

        var showAddTaskDialog by remember { mutableStateOf(false) }
        var selectedTab by remember { mutableIntStateOf(0) }
        var showStatsDialog by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GFGTheme.Background)
                .padding(16.dp)
        ) {
            // Header Section
            AdminHeader(
                onAddTask = { showAddTaskDialog = true },
                onShowStats = { showStatsDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Task Management Section
            TaskManagementSection(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                tasks = when (selectedTab) {
                    0 -> tasks
                    1 -> tasks.filter { it.assignedTo.isEmpty() }
                    2 -> tasks.filter { it.status == TaskStatus.IN_PROGRESS }
                    3 -> tasks.filter { it.status == TaskStatus.COMPLETED }
                    else -> emptyList()
                },
                onAssign = { viewModel.showAssignTaskDialog(it) },
                onDelete = { viewModel.deleteTask(it) },
                onUpdateStatus = { task, status -> viewModel.updateTaskStatus(task.id, status) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Team Section
            TeamSection(teamMembers, stats)
        }

        // Dialogs
        if (showAddTaskDialog) {
            AddTaskDialog(
                onDismiss = { showAddTaskDialog = false },
                onTaskAdded = { task ->
                    viewModel.addTask(task)
                    showAddTaskDialog = false
                }
            )
        }

        if (showStatsDialog) {
            TaskStatsDialog(
                stats = stats,
                onDismiss = { showStatsDialog = false }
            )
        }

        // Assign Task Dialog
        val assignTaskDialogState by viewModel.assignTaskDialogState.collectAsState()
        assignTaskDialogState?.let { (task, members) ->
            AssignTaskDialog(
                task = task,
                teamMembers = members,
                onDismiss = { viewModel.dismissAssignTaskDialog() },
                onAssign = { memberId ->
                    viewModel.assignTask(task.id, memberId)
                    viewModel.dismissAssignTaskDialog()
                }
            )
        }
    }
}

@Composable
fun AdminHeader(onAddTask: () -> Unit, onShowStats: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "GeeksForGeeks Admin Panel",
            style = MaterialTheme.typography.headlineMedium,
            color = GFGTheme.Primary,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onShowStats,
                colors = ButtonDefaults.buttonColors(containerColor = GFGTheme.Secondary)
            ) {
                Icon(Icons.Default.Person, "Stats")
                Spacer(Modifier.width(4.dp))
                Text("Stats")
            }
            Button(
                onClick = onAddTask,
                colors = ButtonDefaults.buttonColors(containerColor = GFGTheme.Primary)
            ) {
                Icon(Icons.Default.Add, "Add Task")
                Spacer(Modifier.width(4.dp))
                Text("New Task")
            }
        }
    }
}

@Composable
fun TaskManagementSection(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    tasks: List<Task>,
    onAssign: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onUpdateStatus: (Task, TaskStatus) -> Unit
) {
    Column {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = GFGTheme.Primary
        ) {
            listOf("All", "Unassigned", "In Progress", "Completed").forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    text = { Text(title) },
                    selectedContentColor = Color.White,
                    unselectedContentColor = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        AnimatedVisibility(
            visible = tasks.isEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No tasks found", style = MaterialTheme.typography.bodyLarge)
            }
        }

        LazyColumn {
            items(tasks) { task ->
                EnhancedTaskItem(
                    task = task,
                    onAssign = onAssign,
                    onDelete = onDelete,
                    onUpdateStatus = onUpdateStatus
                )
            }
        }
    }
}

@Composable
fun EnhancedTaskItem(
    task: Task,
    onAssign: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onUpdateStatus: (Task, TaskStatus) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = GFGTheme.CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = GFGTheme.TextPrimary
                    )
                    Text(
                        text = "Domain: ${task.domainId}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand"
                        )
                    }
                    if (task.status != TaskStatus.COMPLETED) {
                        IconButton(onClick = { onDelete(task) }) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                        }
                    }
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(task.description)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TaskStatusChip(task.status)
                    Text("Credits: ${task.credits}", style = MaterialTheme.typography.bodyMedium)
                }

                if (task.assignedTo.isNotEmpty()) {
                    Text(
                        "Assigned to: ${task.assignedTo}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Button(
                        onClick = { onAssign(task) },
                        colors = ButtonDefaults.buttonColors(containerColor = GFGTheme.Primary)
                    ) {
                        Text("Assign Task")
                    }
                }

                // Status Update Buttons
                if (task.assignedTo.isNotEmpty() && task.status != TaskStatus.COMPLETED) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = {
                                onUpdateStatus(
                                    task,
                                    when (task.status) {
                                        TaskStatus.NEW -> TaskStatus.IN_PROGRESS
                                        TaskStatus.IN_PROGRESS -> TaskStatus.COMPLETED
                                        else -> task.status
                                    }
                                )
                            }
                        ) {
                            Text(
                                when (task.status) {
                                    TaskStatus.NEW -> "Start Task"
                                    TaskStatus.IN_PROGRESS -> "Mark Complete"
                                    else -> "Update Status"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskStatusChip(status: TaskStatus) {
    val (backgroundColor, textColor) = when (status) {
        TaskStatus.NEW -> Color(0xFFE3F2FD) to Color(0xFF1976D2)
        TaskStatus.IN_PROGRESS -> Color(0xFFFFF3E0) to Color(0xFFF57C00)
        TaskStatus.COMPLETED -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
    }

    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = status.toString(),
            color = textColor,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun TeamSection(teamMembers: List<User>, stats: Map<String, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GFGTheme.CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Team Performance",
                style = MaterialTheme.typography.titleLarge,
                color = GFGTheme.Primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                items(teamMembers) { member ->
                    EnhancedTeamMemberItem(
                        member = member,
                        completedTasks = stats[member.userId] ?: 0
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedTeamMemberItem(member: User, completedTasks: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = member.role.toString(),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$completedTasks tasks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GFGTheme.Primary
                )
            }
        }
    }
}

@Composable
fun TaskStatsDialog(stats: Map<String, Int>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Task Statistics") },
        text = {
            Column {
                Text("Total Tasks: ${stats["total"] ?: 0}")
                Text("Completed Tasks: ${stats["completed"] ?: 0}")
                Text("In Progress: ${stats["inProgress"] ?: 0}")
                Text("Unassigned: ${stats["unassigned"] ?: 0}")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onTaskAdded: (Task) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var credits by remember { mutableStateOf("") }
    var domainId by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Add New Task",
                style = MaterialTheme.typography.headlineSmall,
                color = GFGTheme.Primary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = showError && title.isBlank()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    isError = showError && description.isBlank()
                )

                OutlinedTextField(
                    value = credits,
                    onValueChange = { credits = it.filter { char -> char.isDigit() } },
                    label = { Text("Credits") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = showError && credits.isBlank()
                )

                OutlinedTextField(
                    value = domainId,
                    onValueChange = { domainId = it },
                    label = { Text("Domain ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = showError && domainId.isBlank()
                )

                if (showError) {
                    Text(
                        "Please fill in all fields",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank() || description.isBlank() ||
                        credits.isBlank() || domainId.isBlank()) {
                        showError = true
                        return@Button
                    }
                    onTaskAdded(
                        Task(
                            id = UUID.randomUUID().toString(),
                            title = title,
                            description = description,
                            credits = credits.toInt(),
                            domainId = domainId,
                            status = TaskStatus.NEW,
                            assignedTo = "",
                            createdAt = LocalDateTime.now().format(
                                DateTimeFormatter.ISO_LOCAL_DATE_TIME
                            )
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = GFGTheme.Primary)
            ) {
                Text("Add Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AssignTaskDialog(
    task: Task,
    teamMembers: List<User>,
    onDismiss: () -> Unit,
    onAssign: (String) -> Unit
) {
    var selectedMember by remember { mutableStateOf<String?>(null) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    "Assign Task",
                    style = MaterialTheme.typography.headlineSmall,
                    color = GFGTheme.Primary
                )
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GFGTheme.TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Select team member:",
                    style = MaterialTheme.typography.bodyMedium
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(teamMembers) { member ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = if (selectedMember == member.userId)
                                        GFGTheme.Primary
                                    else
                                        Color.LightGray,
                                    shape = MaterialTheme.shapes.small
                                )
                                .clip(MaterialTheme.shapes.small)
                                .clickable { selectedMember = member.userId }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    member.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    member.role.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            if (selectedMember == member.userId) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = GFGTheme.Primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (showError) {
                    Text(
                        "Please select a team member",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedMember == null) {
                        showError = true
                        return@Button
                    }
                    selectedMember?.let { onAssign(it) }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GFGTheme.Primary)
            ) {
                Text("Assign")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}