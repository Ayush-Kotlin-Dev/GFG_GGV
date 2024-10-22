package com.ayush.geeksforgeeks.dashboard
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import com.ayush.data.datastore.UserSettings
import com.ayush.data.model.CreditLog
import com.ayush.geeksforgeeks.dashboard.DashboardViewModel.DashboardUiState
import com.ayush.geeksforgeeks.ui.theme.GFGBackground
import com.ayush.geeksforgeeks.ui.theme.GFGCardBackground
import com.ayush.geeksforgeeks.ui.theme.GFGPrimary
import com.ayush.geeksforgeeks.ui.theme.GFGTextPrimary
import com.patrykandpatrick.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.startAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf

class DashboardScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel: DashboardViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()

        DashboardContent(uiState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(uiState: DashboardUiState) {
        when (uiState) {
            is DashboardUiState.Loading -> LoadingIndicator()
            is DashboardUiState.Success -> DashboardSuccessContent(uiState)
            is DashboardUiState.Error -> ErrorMessage(uiState.message)
        }
}

@Composable
fun DashboardSuccessContent(state: DashboardUiState.Success) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(GFGBackground),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item { WelcomeCard(state.user) }
        item { ClubStatsCard(state.clubStats) }
        item { CreditHistoryChart(state.creditHistory) }
        item { TopContributorsCard(state.topContributors) }
    }
}

@Composable
fun WelcomeCard(user: UserSettings) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GFGCardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = GFGPrimary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Welcome, ${user.name}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = GFGTextPrimary
                )
                Text(
                    text = "Total Credits: ${user.totalCredits}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = GFGTextPrimary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun ClubStatsCard(clubStats: ClubStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GFGCardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Club Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = GFGPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Total Members", clubStats.totalMembers.toString(), Icons.Default.Person)
                StatItem("Active Projects", clubStats.activeProjects.toString(), Icons.Default.Build)
                StatItem("Total Credits", clubStats.totalCredits.toString(), Icons.Default.Star)
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
            tint = GFGPrimary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
            color = GFGTextPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = GFGTextPrimary.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun CreditHistoryChart(creditHistory: List<CreditLog>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GFGCardBackground)
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
                    .height(200.dp),
            )
        }
    }
}

@Composable
fun TopContributorsCard(topContributors: List<UserSettings>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GFGCardBackground)
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
            color = GFGPrimary,
            modifier = Modifier.width(40.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = GFGTextPrimary
            )
            Text(
                text = "${user.totalCredits} credits",
                style = MaterialTheme.typography.bodyMedium,
                color = GFGTextPrimary.copy(alpha = 0.7f)
            )
        }
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = GFGPrimary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GFGBackground)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.Center),
            color = GFGPrimary
        )
    }
}

@Composable
fun ErrorMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GFGBackground),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Error: $message",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

data class ClubStats(
    val totalMembers: Int,
    val activeProjects: Int,
    val totalCredits: Int
)