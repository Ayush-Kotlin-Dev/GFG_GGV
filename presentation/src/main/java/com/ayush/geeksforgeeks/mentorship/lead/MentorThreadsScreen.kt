package com.ayush.geeksforgeeks.mentorship.lead

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.ayush.geeksforgeeks.mentorship.ErrorState
import com.ayush.geeksforgeeks.mentorship.ThreadDiscussionScreen
import com.ayush.geeksforgeeks.mentorship.components.ShimmerLoading
import com.ayush.geeksforgeeks.ui.theme.GFGBackground
import com.ayush.geeksforgeeks.ui.theme.GFGBlack
import com.ayush.geeksforgeeks.ui.theme.GFGStatusPending
import com.ayush.geeksforgeeks.ui.theme.GFGStatusPendingText

@OptIn(ExperimentalMaterial3Api::class)
class MentorThreadsScreen(
    private val teamId: String = "3",
    private val teamName: String = "hacker"
) : Screen {
    @Composable
    override fun Content() {
        val viewModel: MentorThreadsViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = teamName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Team Discussions",
                                style = MaterialTheme.typography.bodySmall,
                                color = GFGBlack.copy(alpha = 0.6f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = GFGBackground,
                        titleContentColor = GFGBlack
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (val state = uiState) {
                    is MentorThreadsUiState.Success -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Inside the LazyColumn content when handling Success state
                            if (state.threads.pendingThreads.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Pending Questions",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = GFGStatusPendingText
                                        )

                                        Surface(
                                            color = GFGStatusPending.copy(alpha = 0.1f),
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Text(
                                                text = "${state.threads.pendingThreads.size} pending",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = GFGStatusPendingText
                                            )
                                        }
                                    }
                                }

                                items(
                                    items = state.threads.pendingThreads,
                                    key = { it.id }
                                ) { thread ->
                                    ThreadCard(
                                        thread = thread,
                                        onClick = {
                                            navigator.push(
                                                ThreadDiscussionScreen(
                                                    teamId = teamId,
                                                    teamName = teamName,
                                                    threadId = thread.id
                                                )
                                            )
                                        },
                                        onEnableThread = {
                                            viewModel.enableThread(thread.id)
                                        }
                                    )
                                }
                            }

                            if (state.threads.pendingThreads.isNotEmpty() &&
                                state.threads.activeThreads.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                            }

                            if (state.threads.activeThreads.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Active Discussions",
                                            style = MaterialTheme.typography.titleMedium
                                        )

                                        Text(
                                            text = "${state.threads.activeThreads.size} active",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = GFGBlack.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                items(
                                    items = state.threads.activeThreads,
                                    key = { it.id }
                                ) { thread ->
                                    ThreadCard(
                                        thread = thread,
                                        onClick = {
                                            navigator.push(
                                                ThreadDiscussionScreen(
                                                    teamId = teamId,
                                                    teamName = teamName,
                                                    threadId = thread.id
                                                )
                                            )
                                        },
                                        onEnableThread = {
                                            viewModel.enableThread(thread.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    is MentorThreadsUiState.Loading -> {
                        ShimmerLoading()
                    }
                    is MentorThreadsUiState.Error -> {
                        ErrorState(
                            message = state.message,
                            onRetry = { /* Implement retry */ }
                        )
                    }
                }
            }
        }
    }
}