package com.ayush.geeksforgeeks.home

import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ayush.data.model.Task
import com.ayush.geeksforgeeks.MainActivityViewModel
import com.ayush.geeksforgeeks.home.HomeViewModel.HomeUiState

class HomeScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel: HomeViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()

        when (val state = uiState) {
            is HomeUiState.Loading -> LoadingIndicator()
            is HomeUiState.Success -> HomeContent(state)
            is HomeUiState.Error -> ErrorMessage(state.message)
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
fun HomeContent(state: HomeUiState.Success) {
    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(text = "Welcome, ${state.user.name}", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Domain: ${state.user.domainId}", style = MaterialTheme.typography.bodyLarge)
        Text(text = "Role: ${state.user.role}", style = MaterialTheme.typography.bodyLarge)
        Text(text = "Total Credits: ${state.user.totalCredits}", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Recent Tasks", style = MaterialTheme.typography.titleLarge)
        state.recentTasks.forEach { task ->
            TaskItem(task)
        }
    }
}

@Composable
fun TaskItem(task: Task) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = task.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Credits: ${task.credits}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Due: ${task.dueDate}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

}

@Composable
fun ErrorMessage(message: String) {
    Log.d("HomeScreen", "Error: $message")
    Text(text = "Error: $message", color = MaterialTheme.colorScheme.error)
}