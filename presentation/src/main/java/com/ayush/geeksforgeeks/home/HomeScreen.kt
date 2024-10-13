package com.ayush.geeksforgeeks.home

import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

        val HviewModel: MainActivityViewModel = hiltViewModel()
        val HuiState by viewModel.uiState.collectAsState()

        Text(text = HuiState.toString())
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
    Column(modifier = Modifier.padding(16.dp)) {
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
    // Implement task item UI
}

@Composable
fun ErrorMessage(message: String) {
    Log.d("HomeScreen", "Error: $message")
    Text(text = "Error: $message", color = MaterialTheme.colorScheme.error)
}