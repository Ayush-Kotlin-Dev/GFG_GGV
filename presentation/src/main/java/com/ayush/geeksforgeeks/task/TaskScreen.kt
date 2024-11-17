// TaskScreen.kt
package com.ayush.geeksforgeeks.task

import TaskDetailScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.ayush.geeksforgeeks.admin.TaskStatusChip
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
            TaskScreenHeader()

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                is TasksViewModel.TasksUiState.Loading -> LoadingIndicator()
                is TasksViewModel.TasksUiState.Success -> {
                    TaskScreenContent(
                        tasks = state.tasks,
                        onTaskClick = { navigator.push(TaskDetailScreen(it.id, navigator::pop)) }
                    )
                }
                is TasksViewModel.TasksUiState.Error -> ErrorMessage(state.message)
            }
        }
    }
}

@Composable
fun TaskScreenHeader() {
    Text(
        "Task Board",
        style = MaterialTheme.typography.headlineMedium,
        color = GFGPrimary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 16.dp)
    )
}

@Composable
fun TaskScreenContent(
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column {
        TaskManagementSection(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            tasks = when (selectedTab) {
                0 -> tasks
                1 -> tasks.filter { it.status == TaskStatus.PENDING }
                2 -> tasks.filter { it.status == TaskStatus.IN_PROGRESS }
                3 -> tasks.filter { it.status == TaskStatus.COMPLETED }
                else -> emptyList()
            },
            onTaskClick = onTaskClick
        )
    }
}

@Composable
fun TaskManagementSection(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit
) {
    Column {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = GFGPrimary,
            edgePadding = 0.dp
        ) {
            listOf("All", "Pending", "In Progress", "Completed").forEachIndexed { index, title ->
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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(tasks) { task ->
                EnhancedTaskItem(
                    task = task,
                    onTaskClick = onTaskClick
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EnhancedTaskItem(
    task: Task,
    onTaskClick: (Task) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize()
            .combinedClickable(
                onClick = { expanded = !expanded },
                onLongClick = { onTaskClick(task) },
                onLongClickLabel = "View task details"
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Due: ${formatDate(task.dueDate)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedButton(
                            onClick = { onTaskClick(task) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GFGPrimary)
                        ) {
                            Text("View Details")
                        }
                    }
                }
            }
        }
    }
}

