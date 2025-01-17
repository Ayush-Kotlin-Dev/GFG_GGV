// GuestMentorshipScreen.kt
package com.ayush.geeksforgeeks.mentorship

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.ayush.data.repository.ThreadDetails
import com.ayush.geeksforgeeks.mentorship.components.CreateThreadDialog
import com.ayush.geeksforgeeks.ui.theme.*
import com.ayush.geeksforgeeks.utils.DomainUtils

class GuestMentorshipScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel: MentorshipViewModel = hiltViewModel()
        val teams by viewModel.teams.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GFGBackground)
        ) {
            // Header
            TopAppBar(
                title = { Text("Ask Mentors") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GFGBackground,
                    titleContentColor = GFGBlack
                )
            )

            // Teams List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(teams) { team ->
                    TeamListItem(
                        team = team,
                        onClick = {
                            navigator.push(TeamThreadsScreen(team))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamListItem(
    team: Team,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Team Icon
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = GFGStatusPending
            ) {
                Text(
                    text = team.name.take(1),
                    modifier = Modifier.wrapContentSize(),
                    style = MaterialTheme.typography.titleLarge,
                    color = GFGStatusPendingText
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Team Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = team.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = GFGBlack
                )
                Text(
                    text = "Ask questions about ${team.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GFGBlack.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// TeamThreadsScreen.kt
class TeamThreadsScreen(private val team: Team) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel: MentorshipViewModel = hiltViewModel()
        val threads by viewModel.threads.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        var showCreateDialog by remember { mutableStateOf(false) }

        LaunchedEffect(team) {
            viewModel.selectTeam(team)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GFGBackground)
        ) {
            TopAppBar(
                title = { Text(team.name) },
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

            Box(modifier = Modifier.fillMaxSize()) {
                if (threads.isEmpty()) {
                    EmptyState(message = "No discussions yet\nBe the first to ask a question!")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(threads) { thread ->
                            ThreadItem(
                                thread = thread,
                                team = team,
                                onClick = {
                                    navigator.push(ThreadDiscussionScreen(team, thread.id))
                                }
                            )
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = GFGStatusPendingText,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Ask Question")
                }
            }
        }

        if (showCreateDialog) {
            CreateThreadDialog(
                onDismiss = { showCreateDialog = false },
                onSubmit = { title, message ->
                    viewModel.createThread(title, message)
                    showCreateDialog = false
                },
                isLoading = isLoading
            )
        }
    }
}

@Composable
private fun ThreadItem(
    thread: ThreadDetails,  // Note: Using ThreadDetails instead of MentorshipThread
    team: Team,
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                Text(
                    text = "${thread.repliesCount} replies",
                    style = MaterialTheme.typography.bodySmall,
                    color = GFGStatusPendingText
                )
            }
        }
    }
}

// EmptyState.kt
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