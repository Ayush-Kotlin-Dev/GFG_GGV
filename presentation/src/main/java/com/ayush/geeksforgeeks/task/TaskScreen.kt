// TaskScreen.kt
package com.ayush.geeksforgeeks.task

import TaskDetailScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ayush.data.model.Task
import com.ayush.data.model.TaskStatus
import com.ayush.geeksforgeeks.common.formatDate
import com.ayush.geeksforgeeks.dashboard.ErrorMessage
import com.ayush.geeksforgeeks.dashboard.LoadingIndicator
import com.ayush.geeksforgeeks.ui.theme.GFGBackground
import com.ayush.geeksforgeeks.ui.theme.GFGCardBackground
import com.ayush.geeksforgeeks.ui.theme.GFGPrimary
import com.ayush.geeksforgeeks.ui.theme.GFGStatusCompleted
import com.ayush.geeksforgeeks.ui.theme.GFGStatusCompletedText
import com.ayush.geeksforgeeks.ui.theme.GFGStatusInProgress
import com.ayush.geeksforgeeks.ui.theme.GFGStatusInProgressText
import com.ayush.geeksforgeeks.ui.theme.GFGStatusPending
import com.ayush.geeksforgeeks.ui.theme.GFGStatusPendingText
import com.ayush.geeksforgeeks.ui.theme.GFGTextPrimary
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.firebase.Timestamp

class TasksScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel: TasksViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            topBar = { TaskScreenTopBar() }
        ) { paddingValues ->
            when (val state = uiState) {
                is TasksViewModel.TasksUiState.Loading -> LoadingIndicator()
                is TasksViewModel.TasksUiState.Success -> {
                    TaskScreenContent(
                        modifier = Modifier.padding(paddingValues),
                        tasks = state.tasks,
                        onTaskClick = { navigator.push(TaskDetailScreen(it.id , navigator::pop)) }
                    )
                }

                is TasksViewModel.TasksUiState.Error -> ErrorMessage(state.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreenTopBar() {
    TopAppBar(
        title = {
            Text(
                text = "Task Board",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = GFGTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = GFGCardBackground
        )
    )
}

@Composable
fun TaskScreenContent(
    modifier: Modifier = Modifier,
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit
) {
    val (recentTasks, otherTasks) = tasks.partition {
        it.status == TaskStatus.PENDING || it.status == TaskStatus.IN_PROGRESS
    }

    val pendingTasks = recentTasks.filter { it.status == TaskStatus.PENDING }
    val inProgressTasks = recentTasks.filter { it.status == TaskStatus.IN_PROGRESS }
    val completedTasks = otherTasks.filter { it.status == TaskStatus.COMPLETED }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(GFGBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TaskStatusOverview(tasks)
        }

        if (pendingTasks.isNotEmpty()) {
            item {
                TaskSection(
                    title = "Pending Tasks",
                    icon = Icons.Default.Settings,
                    tasks = pendingTasks,
                    onTaskClick = onTaskClick
                )
            }
        }

        if (inProgressTasks.isNotEmpty()) {
            item {
                TaskSection(
                    title = "In Progress",
                    icon = Icons.Default.Refresh,
                    tasks = inProgressTasks,
                    onTaskClick = onTaskClick
                )
            }
        }

        if (completedTasks.isNotEmpty()) {
            item {
                TaskSection(
                    title = "Completed",
                    icon = Icons.Default.CheckCircle,
                    tasks = completedTasks,
                    onTaskClick = onTaskClick
                )
            }
        }
    }
}

@Composable
fun TaskStatusOverview(tasks: List<Task>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GFGCardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GFGTextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusCount(
                    "Pending",
                    tasks.count { it.status == TaskStatus.PENDING },
                    GFGStatusPending
                )
                StatusCount(
                    "In Progress",
                    tasks.count { it.status == TaskStatus.IN_PROGRESS },
                    GFGStatusInProgress
                )
                StatusCount(
                    "Completed",
                    tasks.count { it.status == TaskStatus.COMPLETED },
                    GFGStatusCompleted
                )
            }
        }
    }
}

@Composable
fun StatusCount(label: String, count: Int, backgroundColor: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = GFGTextPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = GFGTextPrimary
        )
    }
}

@Composable
fun TaskSection(
    title: String,
    icon: ImageVector,
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GFGCardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = GFGPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GFGTextPrimary
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = GFGPrimary
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    tasks.forEach { task ->
                        TaskCard(
                            task = task,
                            onTaskClick = onTaskClick
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    onTaskClick: (Task) -> Unit
) {
    val statusColor = when (task.status) {
        TaskStatus.PENDING -> GFGStatusPending to GFGStatusPendingText
        TaskStatus.IN_PROGRESS -> GFGStatusInProgress to GFGStatusInProgressText
        TaskStatus.COMPLETED -> GFGStatusCompleted to GFGStatusCompletedText
        TaskStatus.NEW -> TODO()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTaskClick(task) },
        colors = CardDefaults.cardColors(containerColor = statusColor.first.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GFGTextPrimary
                )
                FilterChip(
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = statusColor.first,
                        labelColor = statusColor.second
                    ),
                    label = {
                        Text(
                            text = task.status.name,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    onClick = {  },
                    selected = task.status != TaskStatus.COMPLETED
                )

            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Credits",
                        tint = GFGPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${task.credits} credits",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GFGTextPrimary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Due date",
                        tint = GFGPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatDate(task.dueDate),
                        style = MaterialTheme.typography.bodyMedium,
                        color = GFGTextPrimary
                    )

                }
            }
        }
    }
}

