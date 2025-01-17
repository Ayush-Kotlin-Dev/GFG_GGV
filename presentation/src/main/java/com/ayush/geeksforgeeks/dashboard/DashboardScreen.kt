package com.ayush.geeksforgeeks.dashboard
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerInputChange
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
import com.ayush.geeksforgeeks.utils.ErrorScreen
import com.ayush.geeksforgeeks.utils.LoadingIndicator
import com.patrykandpatrick.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.startAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.scale

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
            is DashboardUiState.Error -> ErrorScreen(uiState.message)
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
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = GFGPrimary.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.medium
            ),
        colors = CardDefaults.cardColors(containerColor = GFGCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,  
                    contentDescription = null,
                    tint = GFGPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Club Overview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GFGPrimary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    "Total Members",
                    clubStats.totalMembers.toString(),
                    Icons.Default.Groups  
                )
                StatItem(
                    "Active Projects",
                    clubStats.activeProjects.toString(),
                    Icons.Default.Terminal  
                )
                StatItem(
                    "Total Credits",
                    clubStats.totalCredits.toString(),
                    Icons.Default.WorkspacePremium  
                )
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, icon: ImageVector) {
    var isHovered by remember { mutableStateOf(false) }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .hoverable(
                interactionSource = remember { MutableInteractionSource() },
                enabled = true
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GFGPrimary,
            modifier = Modifier
                .size(24.dp)
                .scale(animateFloatAsState(if (isHovered) 1.1f else 1f).value)
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
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = GFGPrimary.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.medium
            ),
        colors = CardDefaults.cardColors(containerColor = GFGCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Leaderboard,  
                    contentDescription = null,
                    tint = GFGPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Top Contributors",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GFGPrimary
                )
            }
            topContributors.forEachIndexed { index, contributor ->
                ContributorItem(contributor, index + 1)
                if (index < topContributors.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
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
        // Medal icons for top 3
        when (rank) {
            1 -> Icon(Icons.Default.EmojiEvents, "Gold", tint = androidx.compose.ui.graphics.Color(0xFFFFD700))
            2 -> Icon(Icons.Default.EmojiEvents, "Silver", tint = androidx.compose.ui.graphics.Color(0xFFC0C0C0))
            3 -> Icon(Icons.Default.EmojiEvents, "Bronze", tint = androidx.compose.ui.graphics.Color(0xFFCD7F32))
            else -> Text(
                text = "#$rank",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GFGPrimary,
                modifier = Modifier.width(40.dp)
            )
        }
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
    }
}

data class ClubStats(
    val totalMembers: Int,
    val activeProjects: Int,
    val totalCredits: Int
)