package com.ayush.geeksforgeeks.mentorship

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
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
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() &&
            (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= messages.size - 2) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GFGBackground)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = team.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = threadDetails?.title ?: "Loading...",
                        style = MaterialTheme.typography.bodySmall,
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

        Box(
            modifier = Modifier
                .weight(1f)
                .background(Color(0xFFF8F9FA))
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    threadDetails?.let { details ->
                        DiscussionPost(
                            details,
                            isTeamLead,
                            onThreadEnableClick = { viewModel.enableThread() }
                        )
                    }
                }

                items(
                    items = messages,
                    key = { it.id }
                ) { message ->
                    MessageItem(message)
                }

                item { Spacer(modifier = Modifier.height(60.dp)) }
            }

            // Calculate if we should show the scroll-to-bottom FAB
            val showScrollToBottom by remember {
                derivedStateOf {
                    val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    val totalItems = messages.size + 1 // +1 for the DiscussionPost item
                    messages.isNotEmpty() && lastVisibleItem < totalItems - 2 // -2 to account for spacer and better threshold
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showScrollToBottom,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    },
                    containerColor = GFGStatusPendingText,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = "Scroll to bottom",
                        tint = Color.White
                    )
                }
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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = GFGStatusPending.copy(alpha = 0.1f),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = GFGStatusPendingText,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Waiting for mentor to start the discussion",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GFGStatusPendingText
                    )
                }
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
            Text(
                text = threadDetails.title,
                style = MaterialTheme.typography.titleLarge,
                color = GFGBlack
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = threadDetails.message,
                style = MaterialTheme.typography.bodyLarge,
                color = GFGBlack.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))

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

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
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
                modifier = Modifier.padding(12.dp)
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
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type your message...") },
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GFGStatusPendingText,
                    focusedLabelColor = GFGStatusPendingText,
                    cursorColor = GFGStatusPendingText
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            FilledIconButton(
                onClick = onSend,
                enabled = value.isNotBlank() && !isLoading,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = GFGStatusPendingText,
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        modifier = Modifier.size(20.dp)
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