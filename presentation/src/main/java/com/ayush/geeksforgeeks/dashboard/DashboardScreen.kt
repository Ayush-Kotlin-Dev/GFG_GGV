package com.ayush.geeksforgeeks.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.text.style.TextOverflow
import com.ayush.geeksforgeeks.ui.theme.GFGPrimary

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
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun DashboardContent(state: DashboardUiState.Success) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { UserStatsCard(state.user, state.completedTasks) }
        item { CreditHistoryChart(state.creditHistory) }
        item { TopContributorsSection(state.topContributors) }
    }
}

@Composable
fun UserStatsCard(user: UserSettings, completedTasks: Int) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Your Stats",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Total Credits", user.totalCredits.toString(), Icons.Default.Star)
                StatItem("Completed Tasks", completedTasks.toString(), Icons.Default.Person)
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(text = value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun CreditHistoryChart(creditHistory: List<CreditLog>) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Credit History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = GFGPrimary

            )
            Spacer(modifier = Modifier.height(16.dp))

            val chartEntryModel = remember(creditHistory) {
                entryModelOf(creditHistory.mapIndexed { index, log ->
                    FloatEntry(x = index.toFloat(), y = log.credits.toFloat())
                })
            }

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
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Top Contributors",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = GFGPrimary

            )
            Spacer(modifier = Modifier.height(16.dp))
            topContributors.forEachIndexed { index, contributor ->
                ContributorItem(contributor, index + 1)
                if (index < topContributors.lastIndex) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
fun ContributorItem(user: UserSettings, rank: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#$rank",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(40.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${user.totalCredits} credits",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ErrorMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Error: $message",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}