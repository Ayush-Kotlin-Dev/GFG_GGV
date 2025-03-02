// GuestMentorshipScreen.kt
package com.ayush.geeksforgeeks.mentorship

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ayush.data.model.Team
import com.ayush.data.model.ThreadDetails
import com.ayush.geeksforgeeks.mentorship.components.CreateThreadDialog
import com.ayush.geeksforgeeks.mentorship.components.ShimmerLoading
import com.ayush.geeksforgeeks.mentorship.components.getTeamDescription
import com.ayush.geeksforgeeks.ui.theme.GFGBackground
import com.ayush.geeksforgeeks.ui.theme.GFGBlack
import com.ayush.geeksforgeeks.ui.theme.GFGStatusPending
import com.ayush.geeksforgeeks.ui.theme.GFGStatusPendingText

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = GFGBlack.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = GFGStatusPendingText
            )
        ) {
            Text("Retry")
        }
    }
}

class GuestMentorshipScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel: MentorshipViewModel = hiltViewModel()
        val teamsUiState by viewModel.teamsUiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GFGBackground)
        ) {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text(
                            "Ask Mentors",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Get guidance from experienced developers and mentors",
                            style = MaterialTheme.typography.bodySmall,
                            color = GFGBlack.copy(alpha = 0.6f)
                        )
                    }
                },
                modifier = Modifier.padding(top = 8.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GFGBackground,
                    titleContentColor = GFGBlack
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                when (teamsUiState) {
                    TeamsUiState.Loading -> ShimmerLoading()
                    is TeamsUiState.Error -> ErrorState(
                        message = (teamsUiState as TeamsUiState.Error).message,
                        onRetry = {
                            viewModel.clearError()
                            viewModel.loadTeams()
                        }
                    )
                    is TeamsUiState.Success -> {
                        val teams = (teamsUiState as TeamsUiState.Success).teams
                        if (teams.isEmpty()) {
                            EmptyState("No teams available yet")
                        } else {
                            TeamsContent(
                                teams = teams,
                                onTeamClick = { team ->
                                    navigator.push(TeamThreadsScreen(team.id, team.name))
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TeamsContent(
        teams: List<Team>,
        onTeamClick: (Team) -> Unit
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp), // Add only vertical padding
            verticalArrangement = Arrangement.spacedBy(12.dp) // Increase spacing between items
        ) {
            items(
                items = teams,
                key = { it.id }
            ) { team ->
                TeamCard(
                    team = team,
                    onClick = { onTeamClick(team) }
                )
            }
        }
    }

    @Composable
    fun TeamCard(
        team: Team,
        onClick: () -> Unit
    ) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Enhanced Team Icon
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = GFGStatusPendingText.copy(alpha = 0.1f),
                    border = BorderStroke(
                        width = 2.dp,
                        color = GFGStatusPendingText
                    )
                ) {
                    Text(
                        text = team.name.take(1),
                        modifier = Modifier.wrapContentSize(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = GFGStatusPendingText
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = team.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = GFGBlack
                    )
                    Text(
                        text = getTeamDescription(team.name),
                        style = MaterialTheme.typography.bodyMedium,
                        color = GFGBlack.copy(alpha = 0.6f)
                    )
                }

                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = GFGStatusPendingText
                )
            }
        }

    }
}

data class TeamThreadsScreen(
    private val teamId: String,
    private val teamName: String
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() { //This is made previously for Juniour/Guest side screen
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: MentorshipViewModel = hiltViewModel()
        val threadsUiState by viewModel.threadsUiState.collectAsState()
        val createThreadUiState by viewModel.createThreadUiState.collectAsState()
        var showCreateDialog by remember { mutableStateOf(false) }

        LaunchedEffect(teamId) {
            viewModel.selectTeam(Team(id = teamId, name = teamName))
        }

        LaunchedEffect(createThreadUiState.isSuccess) {
            if (createThreadUiState.isSuccess) {
                showCreateDialog = false
                viewModel.resetCreateThreadState()
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(teamName)
                            Text(
                                "Discussion Forum",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GFGBlack.copy(alpha = 0.6f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = GFGBackground,
                        titleContentColor = GFGBlack
                    )
                )
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = threadsUiState !is ThreadsUiState.Loading,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    FloatingActionButton( //TODO add ripple effect fab explosion effect
                        onClick = { showCreateDialog = true },
                        containerColor = GFGStatusPendingText,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Add, "Ask Question")
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (threadsUiState) {
                    ThreadsUiState.Loading -> ShimmerLoading()
                    is ThreadsUiState.Error -> ErrorState(
                        message = (threadsUiState as ThreadsUiState.Error).message,
                        onRetry = {
                            viewModel.clearError()
                            viewModel.selectTeam(Team(id = teamId, name = teamName))
                        }
                    )
                    is ThreadsUiState.Success -> {
                        val threads = (threadsUiState as ThreadsUiState.Success).threads
                        if (threads.isEmpty()) {
                            EmptyState("Start the first discussion\nin this team!")
                        } else {
                            ThreadsContent(
                                threads = threads,
                                onThreadClick = { threadId ->
                                    navigator.push(ThreadDiscussionScreen(teamId, teamName, threadId))
                                }
                            )
                        }
                    }
                }
            }
        }

        // Inside TeamThreadsScreen Content
        if (showCreateDialog) {
            CreateThreadDialog(
                onDismiss = {
                    showCreateDialog = false
                    viewModel.resetCreateThreadState()
                },
                onSubmit = { title, message, category, tags ->
                    viewModel.createThread(title, message, category, tags)
                },
                isLoading = createThreadUiState.isLoading,
                error = createThreadUiState.error
            )
        }

    }
}

@Composable
private fun ThreadsContent(
    threads: List<ThreadDetails>,
    onThreadClick: (String) -> Unit
) {
    if (threads.isEmpty()) {
        EmptyState("Start the first discussion\nin this team!")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = threads,
                key = { it.id }
            ) { thread ->
                ThreadItem(
                    thread = thread,
                    onClick = { onThreadClick(thread.id) }
                )
            }
        }
    }
}

@Composable
private fun ThreadItem(
    thread: ThreadDetails,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = thread.title,
                style = MaterialTheme.typography.titleMedium,
                color = GFGBlack
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = thread.message,
                style = MaterialTheme.typography.bodyMedium,
                color = GFGBlack.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "By ${thread.authorName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GFGBlack.copy(alpha = 0.5f)
                )
                if (!thread.isEnabled) {
                    Surface(
                        color = GFGStatusPending,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Pending",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = GFGStatusPendingText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = GFGBlack.copy(alpha = 0.5f)
        )
    }
}