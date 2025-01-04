package com.ayush.geeksforgeeks.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import com.ayush.data.datastore.User
import com.ayush.data.model.Task
import com.ayush.data.model.TaskStatus
import com.ayush.geeksforgeeks.R
import com.ayush.geeksforgeeks.ui.theme.GFGBackground
import com.ayush.geeksforgeeks.ui.theme.GFGCardBackground
import com.ayush.geeksforgeeks.ui.theme.GFGPrimary
import com.ayush.geeksforgeeks.ui.theme.GFGTextPrimary
import java.util.UUID
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.unit.DpOffset

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
                .background(GFGBackground)
                .padding(16.dp)
        ) {
            LazyColumn {
                item {
                    AdminHeader(
                        stats = stats,
                        onAddTask = { showAddTaskDialog = true },
                        onShowStats = { showStatsDialog = true },
                        onGenerateReport = { viewModel.generateWeeklyReport() }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
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
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    TeamSection(teamMembers, stats)
                }
            }
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
fun AdminHeader(
    stats: Map<String, Int>,
    onAddTask: () -> Unit,
    onShowStats: () -> Unit,
    onGenerateReport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Text(
            "Team Lead Panel",
            style = MaterialTheme.typography.headlineMedium,
            color = GFGPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Manage your team and tasks",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatsCard(
                icon = Icons.Default.AssignmentInd,
                label = "Total Tasks",
                value = "${stats["total"] ?: 0}",
                backgroundColor = MaterialTheme.colorScheme.primary
            )
            StatsCard(
                icon = Icons.Default.PendingActions,
                label = "Unassigned",
                value = "${stats["unassigned"] ?: 0}",
                backgroundColor = MaterialTheme.colorScheme.error
            )
            StatsCard(
                icon = Icons.Default.CheckCircle,
                label = "Completed",
                value = "${stats["completed"] ?: 0}",
                backgroundColor = MaterialTheme.colorScheme.tertiary
            )
        }

        ActionButtons(onAddTask, onShowStats, onGenerateReport)
    }
}

@Composable
fun StatsCard(
    icon: ImageVector,
    label: String,
    value: String,
    backgroundColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, backgroundColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = backgroundColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = backgroundColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = backgroundColor
            )
        }
    }
}

@Composable
fun ActionButtons(onAddTask: () -> Unit, onShowStats: () -> Unit, onGenerateReport: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onShowStats,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = GFGPrimary,
                containerColor = Color.Transparent
            ),
            border = BorderStroke(1.dp, GFGPrimary),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Icon(Icons.Default.Person, "Stats", modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(4.dp))
            Text("Stats", maxLines = 1)
        }
        OutlinedButton(
            onClick = onAddTask,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = GFGPrimary,
                containerColor = Color.Transparent
            ),
            border = BorderStroke(1.dp, GFGPrimary),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Icon(Icons.Default.Add, "Add Task", modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(4.dp))
            Text("New Task", maxLines = 1)
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedButton(
        onClick = onGenerateReport,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = GFGPrimary,
            containerColor = Color.Transparent
        ),
        border = BorderStroke(1.dp, GFGPrimary),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.word_image),
            contentDescription = "Generate Report"
        )
        Spacer(Modifier.width(4.dp))
        Text("Generate Report")
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
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = GFGPrimary,
            edgePadding = 0.dp
        ) {
            listOf("All", "Unassigned", "In Progress", "Completed").forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    text = {
                        Text(
                            text = title,
                            maxLines = 1,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    selectedContentColor = Color.White,
                    unselectedContentColor = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp)
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

        Column {
            tasks.forEach { task ->
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EnhancedTaskItem(
    task: Task,
    onAssign: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onUpdateStatus: (Task, TaskStatus) -> Unit
) {
    val viewModel: AdminViewModel = hiltViewModel()
    val teamMembers by viewModel.teamMembers.collectAsState()

    var expanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }

    val assignedMemberName = remember(task.assignedTo, teamMembers) {
        teamMembers.find { it.userId == task.assignedTo }?.name ?: "Unknown User"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize()
            .combinedClickable(
                onClick = { expanded = !expanded },
                onLongClick = {
                    if (task.status != TaskStatus.COMPLETED) {
                        showMenu = true
                    }
                },
                onLongClickLabel = "More options"
            ),
        colors = CardDefaults.cardColors(containerColor = GFGCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = GFGTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TaskStatusChip(task.status)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${task.credits} Credits", style = MaterialTheme.typography.bodySmall)
                    }
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand"
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = GFGTextPrimary.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (task.assignedTo.isNotEmpty()) {
                        Text(
                            "Assigned to: $assignedMemberName",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (task.assignedTo.isEmpty()) {
                            OutlinedButton(
                                onClick = { onAssign(task) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GFGPrimary)
                            ) {
                                Text("Assign Task")
                            }
                        }
                        if (task.status != TaskStatus.COMPLETED && task.assignedTo.isNotEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    val newStatus = when (task.status) {
                                        TaskStatus.NEW -> TaskStatus.IN_PROGRESS
                                        TaskStatus.IN_PROGRESS -> TaskStatus.COMPLETED
                                        TaskStatus.PENDING -> TaskStatus.IN_PROGRESS
                                        else -> TaskStatus.COMPLETED
                                    }
                                    onUpdateStatus(task, newStatus)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GFGPrimary)
                            ) {
                                Text(
                                    when (task.status) {
                                        TaskStatus.NEW -> "Start Task"
                                        TaskStatus.IN_PROGRESS -> "Mark Complete"
                                        TaskStatus.PENDING -> "Start Task"
                                        else -> "Mark Complete"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
        offset = menuOffset,
        containerColor = GFGBackground,
    ) {
        DropdownMenuItem(
            text = { Text("Edit") },
            onClick = {
                // TODO: Implement edit functionality
                showMenu = false
            }
        )
        DropdownMenuItem(
            text = { Text("Delete") },
            onClick = {
                onDelete(task)
                showMenu = false
            }
        )
    }
}

@Composable
fun TaskStatusChip(status: TaskStatus) {
    val (backgroundColor, textColor) = when (status) {
        TaskStatus.NEW -> Color(0xFFE3F2FD) to Color(0xFF1976D2)
        TaskStatus.IN_PROGRESS -> Color(0xFFFFF3E0) to Color(0xFFF57C00)
        TaskStatus.COMPLETED -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        TaskStatus.PENDING -> Color(0xFFE0F2F1) to Color(0xFF0F9D58)
    }

    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = status.toString(),
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun TeamSection(teamMembers: List<User>, stats: Map<String, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GFGCardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Team Performance",
                    style = MaterialTheme.typography.titleLarge,
                    color = GFGPrimary
                )
                Text(
                    "${teamMembers.size} Members",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column {
                teamMembers.forEach { member ->
                    EnhancedTeamMemberItem(
                        member = member,
                        completedTasks = stats[member.userId] ?: 0,
                        totalTasks = (stats["total"] ?: 0)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun EnhancedTeamMemberItem(
    member: User,
    completedTasks: Int,
    totalTasks: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Text(
                    text = "$completedTasks/$totalTasks tasks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GFGPrimary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(MaterialTheme.shapes.small),
                color = GFGPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
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
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Add New Task",
                style = MaterialTheme.typography.headlineSmall,
                color = GFGPrimary
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
        containerColor = GFGBackground,
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank() || description.isBlank() ||
                        credits.isBlank()
                    ) {
                        showError = true
                        return@Button
                    }
                    onTaskAdded(
                        Task(
                            id = UUID.randomUUID().toString(),
                            title = title,
                            description = description,
                            credits = credits.toInt(),
                            status = TaskStatus.NEW,
                            assignedTo = "",
                            createdAt = com.google.firebase.Timestamp.now(),
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = GFGPrimary)
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
                    color = GFGPrimary
                )
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GFGTextPrimary
                )
            }
        },
        containerColor = GFGBackground,
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
                                        GFGPrimary
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
                                    tint = GFGPrimary
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
                colors = ButtonDefaults.buttonColors(containerColor = GFGPrimary)
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