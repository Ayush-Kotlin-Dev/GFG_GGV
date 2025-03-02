package com.ayush.geeksforgeeks.task

import TaskDetailScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.RunCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ayush.data.model.Task
import com.ayush.data.model.TaskStatus
import com.ayush.geeksforgeeks.admin.TaskStatusChip
import com.ayush.geeksforgeeks.utils.ErrorScreen
import com.ayush.geeksforgeeks.utils.LoadingIndicator
import com.ayush.geeksforgeeks.utils.formatDate
import com.ayush.geeksforgeeks.ui.theme.GFGBackground
import com.ayush.geeksforgeeks.ui.theme.GFGPrimary

class TasksScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel: TasksViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GFGBackground)
                .padding(16.dp)
        ) {
            TaskScreenHeader(uiState)

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                is TasksViewModel.TasksUiState.Loading -> LoadingIndicator()
                is TasksViewModel.TasksUiState.Success -> {
                    TaskScreenContent(
                        tasks = state.tasks,
                        onTaskClick = { navigator.push(TaskDetailScreen(it.id, navigator::pop)) }
                    )
                }
                is TasksViewModel.TasksUiState.Error -> ErrorScreen(state.message)
            }
        }
    }
}

data class TaskCounts(
    val pending: Int = 0,
    val inProgress: Int = 0,
    val completed: Int = 0
)

sealed class TasksUiState {
    object Loading : TasksUiState()
    data class Success(
        val tasks: List<Task>,
        val taskCounts: TaskCounts
    ) : TasksUiState()
    data class Error(val message: String) : TasksUiState()
}

@Composable
fun TaskScreenHeader(uiState: TasksViewModel.TasksUiState) {
    Column {
        Text(
            "Task Board",
            style = MaterialTheme.typography.headlineMedium,
            color = GFGPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Your assigned tasks and progress",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        when (uiState) {
            is TasksViewModel.TasksUiState.Success -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatusCard(
                        count = uiState.taskCounts.pending,
                        label = "Pending",
                        color = Color(0xFFF57C00)
                    )
                    StatusCard(
                        count = uiState.taskCounts.inProgress,
                        label = "In Progress",
                        color = GFGPrimary
                    )
                    StatusCard(
                        count = uiState.taskCounts.completed,
                        label = "Completed",
                        color = Color(0xFF2E7D32)
                    )
                }
            }
            else -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatusCard(
                        count = 0,
                        label = "Pending",
                        color = Color(0xFFF57C00)
                    )
                    StatusCard(
                        count = 0,
                        label = "In Progress",
                        color = GFGPrimary
                    )
                    StatusCard(
                        count = 0,
                        label = "Completed",
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(count: Int, label: String, color: Color) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = color
            )
        }
    }
}

@Composable
fun TaskScreenContent(
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    Column {
        // Search field
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search tasks") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Task completion progress
            if (tasks.isNotEmpty()) {
                val completedPercentage = tasks.count { it.status == TaskStatus.COMPLETED } / tasks.size.toFloat()
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Task Completion", style = MaterialTheme.typography.bodyMedium)
                        Text("${(completedPercentage * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = completedPercentage, 
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Filter tasks based on search query
        val filteredTasks = remember(tasks, searchQuery, selectedTab) {
            tasks.filter { it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }
        }

        TaskManagementSection(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            tasks = when (selectedTab) {
                0 -> filteredTasks
                1 -> filteredTasks.filter { it.status == TaskStatus.PENDING }
                2 -> filteredTasks.filter { it.status == TaskStatus.IN_PROGRESS }
                3 -> filteredTasks.filter { it.status == TaskStatus.COMPLETED }
                else -> emptyList()
            },
            onTaskClick = onTaskClick,
            searchQuery = searchQuery
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskManagementSection(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit,
    searchQuery: String = ""
) {
    Column {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = GFGPrimary,
                        height = 3.dp
                    )
                }
            ) {
                listOf(
                    Triple("All", Icons.Default.List, tasks.size),
                    Triple("Pending", Icons.Default.Timer, tasks.count { it.status == TaskStatus.PENDING }),
                    Triple("In Progress", Icons.Default.RunCircle, tasks.count { it.status == TaskStatus.IN_PROGRESS }),
                    Triple("Completed", Icons.Default.CheckCircle, tasks.count { it.status == TaskStatus.COMPLETED })
                ).forEachIndexed { index, (title, icon, count) ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { onTabSelected(index) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("$title ($count)")
                            }
                        },
                        selectedContentColor = GFGPrimary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = tasks.isEmpty() || (searchQuery.isNotBlank() && tasks.none { it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            EmptyStateMessage(selectedTab, searchQuery)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val visibleTasks = tasks.filter { task ->
                searchQuery.isEmpty() || task.title.contains(searchQuery, ignoreCase = true) || task.description.contains(searchQuery, ignoreCase = true)
            }
            items(
                items = visibleTasks,
                key = { it.id }
            ) { task ->
                EnhancedTaskItem(
                    task = task,
                    onTaskClick = onTaskClick,
                    modifier = Modifier.animateItemPlacement()
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EnhancedTaskItem(
    task: Task,
    onTaskClick: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val dueDate = formatDate(task.dueDate)
    val isOverdue = task.status != TaskStatus.COMPLETED && task.dueDate.seconds < System.currentTimeMillis() / 1000

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .combinedClickable(
                onClick = { expanded = !expanded },
                onLongClick = { onTaskClick(task) }
            ),
        colors = CardDefaults.cardColors(
            containerColor = when (task.status) {
                TaskStatus.PENDING -> Color(0xFFF57C00).copy(alpha = 0.05f)
                TaskStatus.IN_PROGRESS -> GFGPrimary.copy(alpha = 0.05f)
                TaskStatus.COMPLETED -> Color(0xFF2E7D32).copy(alpha = 0.05f)
                else -> MaterialTheme.colorScheme.surface
            }.copy(alpha = 0.05f)
        ),
        border = BorderStroke(
            1.dp,
            when (task.status) {
                TaskStatus.PENDING -> Color(0xFFF57C00)
                TaskStatus.IN_PROGRESS -> GFGPrimary
                TaskStatus.COMPLETED -> Color(0xFF2E7D32)
                else -> MaterialTheme.colorScheme.outline
            }.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TaskStatusChip(task.status)
                        Text(
                            "${task.credits} Credits",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Due: $dueDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOverdue) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { onTaskClick(task) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = GFGPrimary
                            )
                        ) {
                            Text("View Details")
                        }
                    }
                    
                    // Show a warning if task is overdue
                    if (isOverdue) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Warning",
                                tint = Color.Red,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "This task is overdue",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Red
                            )
                        }
                    }

                    // Credits visualization
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Credit value: ", 
                            style = MaterialTheme.typography.bodySmall
                        )
                        LinearProgressIndicator(
                            progress = task.credits / 10f, 
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateMessage(
    selectedTab: Int,
    searchQuery: String = ""
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = when (selectedTab) {
                1 -> Icons.Default.Timer
                2 -> Icons.Default.RunCircle
                3 -> Icons.Default.CheckCircle
                else -> Icons.Default.List
            },
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (searchQuery.isNotBlank()) {
                "No matching tasks found for \"$searchQuery\""
            } else {
                when (selectedTab) {
                    1 -> "No pending tasks"
                    2 -> "No tasks in progress"
                    3 -> "No completed tasks"
                    else -> "No tasks available"
                }
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}