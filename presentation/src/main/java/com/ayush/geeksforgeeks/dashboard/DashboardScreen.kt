package com.ayush.geeksforgeeks.dashboard

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import com.ayush.data.datastore.UserSettings
import com.ayush.data.model.CreditLog
import com.ayush.geeksforgeeks.dashboard.DashboardViewModel.DashboardUiState
import com.patrykandpatrick.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.startAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.FloatEntry

class DashboardScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel: DashboardViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()

        when (val state = uiState) {
            is DashboardUiState.Loading -> LoadingIndicator()
            is DashboardUiState.Success -> DashboardContent(state)
            is DashboardUiState.Error -> ErrorMessage(state.message)
        }
    }
}

@Composable
fun LoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator(modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
    }
}

@Composable
fun DashboardContent(state: DashboardUiState.Success) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Dashboard", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        UserStatsCard(state.user, state.completedTasks)
        Spacer(modifier = Modifier.height(16.dp))

        CreditHistoryChart(state.creditHistory)
        Spacer(modifier = Modifier.height(16.dp))

        TopContributorsSection(state.topContributors)
    }
}

@Composable
fun UserStatsCard(user: UserSettings, completedTasks: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Your Stats", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Total Credits: ${user.totalCredits}")
            Text(text = "Completed Tasks: $completedTasks")
        }
    }
}


@Composable
fun CreditHistoryChart(creditHistory: List<CreditLog>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Credit History", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            val chartEntryModel = entryModelOf(creditHistory.mapIndexed { index, log ->
                FloatEntry(x = index.toFloat(), y = log.credits.toFloat())
            })

            Chart(
                chart = lineChart(),
                model = chartEntryModel,
                startAxis = startAxis(),
                bottomAxis = bottomAxis(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}

@Composable
fun TopContributorsSection(topContributors: List<UserSettings>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Top Contributors", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            topContributors.forEachIndexed { index, user ->
                Text(text = "${index + 1}. ${user.name} - ${user.totalCredits} credits")
            }
        }
    }
}

@Composable
fun ErrorMessage(message: String) {
    Text(text = "Error: $message", color = MaterialTheme.colorScheme.error)
}