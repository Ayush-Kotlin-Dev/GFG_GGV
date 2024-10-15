package com.ayush.geeksforgeeks.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import com.ayush.data.datastore.User
import com.ayush.data.model.Task
import com.ayush.data.model.TaskStatus

class AdminScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel: AdminViewModel = hiltViewModel()
        val teamMembers by viewModel.teamMembers.collectAsState()
        val tasks by viewModel.tasks.collectAsState()

        var showAddTaskDialog by remember { mutableStateOf(false) }
        var selectedTab by remember { mutableStateOf(0) }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("Admin Panel", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tasks", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { showAddTaskDialog = true }) {
                    Text("Add New Task")
                }
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) { Text("All") }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) { Text("Un Assigned") }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) { Text("In Progress") }
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }) { Text("Completed") }
            }

            TaskList(
                tasks = when (selectedTab) {
                    0 -> tasks
                    1 -> tasks.filter { it.assignedTo.isEmpty() }
                    2 -> tasks.filter { it.status == TaskStatus.IN_PROGRESS }
                    3 -> tasks.filter { it.status == TaskStatus.COMPLETED }
                    else -> emptyList()
                },
                onAssign = { viewModel.showAssignTaskDialog(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Team Members", style = MaterialTheme.typography.titleMedium)
            LazyColumn {
                items(teamMembers) { member ->
                    TeamMemberItem(member)
                }
            }
        }

        if (showAddTaskDialog) {
            AddTaskDialog(
                onDismiss = { showAddTaskDialog = false },
                onTaskAdded = { task ->
                    viewModel.addTask(task)
                    showAddTaskDialog = false
                }
            )
        }

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
fun TaskList(tasks: List<Task>, onAssign: (Task) -> Unit) {
    LazyColumn {
        items(tasks) { task ->
            TaskItem(task, onAssign)
        }
    }
}
@Composable
fun TeamMemberItem(member: User) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(8.dp)) {
            Text(text = member.name, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.weight(1f))
            Text(text = member.role.toString(), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun TaskItem(task: Task, onAssign: (Task) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = task.title, style = MaterialTheme.typography.bodyMedium)
                Text(text = "Status: ${task.status}", style = MaterialTheme.typography.bodySmall)
                Text(text = "Credits: ${task.credits}", style = MaterialTheme.typography.bodySmall)
                if (task.assignedTo.isNotEmpty()) {
                    Text(text = "Assigned to: ${task.assignedTo}", style = MaterialTheme.typography.bodySmall)
                }
                Text(text = "Domain: ${task.domainId}", style = MaterialTheme.typography.bodySmall)
            }
            if (task.assignedTo.isEmpty()) {
                Button(onClick = { onAssign(task) }) {
                    Text("Assign")
                }
            }
        }
    }
}

@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onTaskAdded: (Task) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var credits by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Task") },
        text = {
            Column {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = credits,
                    onValueChange = { credits = it },
                    label = { Text("Credits") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onTaskAdded(Task(
                    title = title,
                    description = description,
                    credits = credits.toIntOrNull() ?: 0,
                    // The domainId will be set in the ViewModel
                ))
            }) {
                Text("Add Task")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
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
    var selectedMemberId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Task: ${task.title}") },
        text = {
            Column {
                teamMembers.forEach { member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedMemberId == member.userId,
                            onClick = { selectedMemberId = member.userId }
                        )
                        Text(member.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedMemberId?.let { onAssign(it) } },
                enabled = selectedMemberId != null
            ) {
                Text("Assign")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}