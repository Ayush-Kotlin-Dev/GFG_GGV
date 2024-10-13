
package com.ayush.geeksforgeeks.taskdetail

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import com.ayush.data.model.Task
import com.ayush.geeksforgeeks.task.ErrorMessage
import com.ayush.geeksforgeeks.task.LoadingIndicator

class TaskDetailScreen(private val taskId: String) : Screen {
    @Composable
    override fun Content() {
        val viewModel: TaskDetailViewModel = hiltViewModel()
        val taskState by viewModel.taskState.collectAsState()

        LaunchedEffect(taskId) {
            viewModel.loadTask(taskId)
        }

        when (val state = taskState) {
            is TaskDetailViewModel.TaskState.Loading -> LoadingIndicator()
            is TaskDetailViewModel.TaskState.Success -> TaskDetail(state.task)
            is TaskDetailViewModel.TaskState.Error -> ErrorMessage(state.message)
        }
    }
}

@Composable
fun TaskDetail(task: Task) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = task.title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Description: ${task.description}", style = MaterialTheme.typography.bodyLarge)
        Text(text = "Status: ${task.status}", style = MaterialTheme.typography.bodyLarge)
        Text(text = "Credits: ${task.credits}", style = MaterialTheme.typography.bodyLarge)
        Text(text = "Due Date: ${task.dueDate}", style = MaterialTheme.typography.bodyLarge)
    }
}