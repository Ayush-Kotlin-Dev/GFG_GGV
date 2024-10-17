package com.ayush.geeksforgeeks.task

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ayush.data.model.Task
import com.ayush.data.model.TaskStatus
import com.ayush.geeksforgeeks.taskdetail.TaskDetailScreen

class TasksScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel: TasksViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        when (val state = uiState) {
            is TasksViewModel.TasksUiState.Loading -> LoadingIndicator()
            is TasksViewModel.TasksUiState.Success -> TaskList(state.tasks, viewModel::updateTaskStatus) {
                navigator.push(TaskDetailScreen(it.id))
            }
            is TasksViewModel.TasksUiState.Error -> ErrorMessage(state.message)
        }
    }
}

@Composable
fun LoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun TaskList(
    tasks: List<Task>,
    onStatusChange: (String, TaskStatus) -> Unit,
    onTaskClick: (Task) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(tasks) { task ->
            TaskItem(task, onStatusChange, onTaskClick)
        }
    }
}

@Composable
fun TaskItem(
    task: Task,
    onStatusChange: (String, TaskStatus) -> Unit,
    onTaskClick: (Task) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onTaskClick(task) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = task.title, style = MaterialTheme.typography.titleMedium)
            Text(text = "Status: ${task.status}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Credits: ${task.credits}", style = MaterialTheme.typography.bodyMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TaskStatusButtons(task, onStatusChange)
            }
        }
    }
}

@Composable
fun TaskStatusButtons(task: Task, onStatusChange: (String, TaskStatus) -> Unit) {
    TaskStatus.entries.forEach { status ->
        if (status != task.status) {
            Button(
                onClick = { onStatusChange(task.id, status) },
                modifier = Modifier.padding(4.dp)
            ) {
                Text(text = status.name)
            }
        }
    }
}

@Composable
fun ErrorMessage(message: String) {
    Text(text = "Error: $message", color = MaterialTheme.colorScheme.error)
}