package com.ayush.geeksforgeeks.mentorship

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ayush.data.datastore.UserPreferences
import com.ayush.data.datastore.UserRole
import com.ayush.data.model.Team
import com.ayush.data.repository.ThreadDetails
import com.ayush.data.repository.ThreadMessage
import com.ayush.geeksforgeeks.ui.theme.*

data class ThreadDiscussionScreen(
    private val teamId : String,
    private val teamName : String,
    private val threadId: String
) : Screen {
    @Composable
    override fun Content() {
        val viewModel: ThreadViewModel = hiltViewModel()
        val currentUser by viewModel.currentUser.collectAsState()

        LaunchedEffect(currentUser) {
            currentUser?.let {
                viewModel.loadThread(teamId, threadId)
            }
        }

        currentUser?.let { settings ->
            CompositionLocalProvider(
                LocalUserProvider provides CurrentUser(
                    userId = settings.userId,
                    name = settings.name,
                    role = settings.role
                )
            ) {
                ThreadDiscussionContent(
                    viewModel = viewModel,
                    team = Team(teamId,teamName),
                    threadId = threadId,
                    isTeamLead = settings.role == UserRole.TEAM_LEAD
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThreadDiscussionContent(
    viewModel: ThreadViewModel,
    team: Team,
    threadId: String,
    isTeamLead: Boolean
) {
    val messages by viewModel.messages.collectAsState()
    val threadDetails by viewModel.threadDetails.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val navigator = LocalNavigator.currentOrThrow
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll to bottom when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GFGBackground)
    ) {
        // Header
        TopAppBar(
            title = { Text(team.name) },
            navigationIcon = {
                IconButton(onClick = { navigator.pop() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        // Messages
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // The original thread/discussion post
            item {
                threadDetails?.let { details ->
                    DiscussionPost(details, isTeamLead, onThreadEnableClick = { viewModel.enableThread() })
                }
            }

            // Messages
            items(messages) { message ->
                MessageItem(message)
            }
        }

        if (threadDetails?.isEnabled == true) {
            ChatInput(
                value = messageText,
                onValueChange = { messageText = it },
                onSend = {
                    viewModel.sendMessage(messageText)
                    messageText = ""
                },
                isLoading = isLoading
            )
        } else {
            // Show disabled state message
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = GFGStatusPending.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "Waiting for mentor to start the discussion",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = GFGStatusPendingText
                )
            }
        }
    }
}

@Composable
private fun DiscussionPost(
    threadDetails: ThreadDetails,
    isTeamLead: Boolean,
    onThreadEnableClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = GFGStatusPendingText.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title
            Text(
                text = threadDetails.title,
                style = MaterialTheme.typography.titleLarge,
                color = GFGBlack
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Main message/question
            Text(
                text = threadDetails.message,
                style = MaterialTheme.typography.bodyLarge,
                color = GFGBlack.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Author info and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Posted by ${threadDetails.authorName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GFGBlack.copy(alpha = 0.5f)
                )

                if (!threadDetails.isEnabled) {
                    Surface(
                        color = if (isTeamLead) GFGStatusPendingText else GFGStatusPending,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = if (isTeamLead) "Tap to start discussion" else "Waiting for mentor",
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .then(
                                    if (isTeamLead || !isTeamLead) { //TODO its for testing i will change it later
                                        Modifier.clickable { onThreadEnableClick.invoke() }
                                    } else {
                                        Modifier
                                    }
                                ),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isTeamLead) Color.White else GFGStatusPendingText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageItem(message: ThreadMessage) {
    val isCurrentUser = message.senderId == LocalUserProvider.current.userId

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isCurrentUser)
                    GFGStatusPendingText.copy(alpha = 0.1f)
                else
                    Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (message.isTeamLead)
                        GFGStatusPendingText
                    else
                        GFGBlack.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message") },
                maxLines = 4
            )

            IconButton(
                onClick = onSend,
                enabled = value.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = GFGStatusPendingText
                    )
                } else {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = GFGStatusPendingText
                    )
                }
            }
        }
    }
}

data class CurrentUser(
    val userId: String,
    val name: String,
    val role: UserRole
)

val LocalUserProvider = staticCompositionLocalOf<CurrentUser> {
    error("No user provided")
}