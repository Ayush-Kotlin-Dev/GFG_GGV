package com.ayush.geeksforgeeks.presentation.mentorship.mentee

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ayush.geeksforgeeks.data.model.Team
import com.ayush.geeksforgeeks.data.model.ThreadDetails
import com.ayush.geeksforgeeks.presentation.mentorship.ThreadDiscussionScreen
import com.ayush.geeksforgeeks.presentation.mentorship.components.CreateThreadDialog
import com.ayush.geeksforgeeks.presentation.mentorship.components.ShimmerThreadItem
import com.ayush.geeksforgeeks.presentation.mentorship.mentee.components.MenteeThreadCard
import com.ayush.geeksforgeeks.ui.theme.*
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MenteeThreadsScreen(private val team: Team) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel: MenteeThreadsViewModel = hiltViewModel()
        val navigator = LocalNavigator.currentOrThrow
        val threads by viewModel.threads.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val isRefreshing by viewModel.isRefreshing.collectAsState()
        val errorMessage by viewModel.errorMessage.collectAsState()
        val searchQuery by viewModel.searchQuery.collectAsState()
        val filteredThreads by viewModel.filteredThreads.collectAsState()
        
        val scope = rememberCoroutineScope()
        var showCreateThreadDialog by remember { mutableStateOf(false) }
        var isSearchActive by remember { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }
        val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

        // Load threads when screen is shown
        LaunchedEffect(team.id) {
            viewModel.loadThreads(team.id)
        }
        
        // Show error messages
        LaunchedEffect(errorMessage) {
            errorMessage?.let {
                val result = snackbarHostState.showSnackbar(
                    message = it,
                    actionLabel = "Retry",
                    duration = SnackbarDuration.Long
                )
                
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.loadThreads(team.id)
                }
                
                viewModel.clearError()
            }
        }

        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = { Text(team.name) },
                        navigationIcon = {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { isSearchActive = !isSearchActive }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search threads"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = GFGBackground,
                            titleContentColor = GFGBlack
                        )
                    )
                    
                    AnimatedVisibility(
                        visible = isSearchActive,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut()
                    ) {
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = { viewModel.setSearchQuery(it) },
                            onSearch = { isSearchActive = false },
                            active = isSearchActive,
                            onActiveChange = { isSearchActive = it },
                            placeholder = { Text("Search for threads...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear search"
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            // Search results appear here
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredThreads) { thread ->
                                    ThreadSearchResult(
                                        thread = thread,
                                        onClick = {
                                            isSearchActive = false
                                            viewModel.setSearchQuery("")
                                            navigator.push(
                                                ThreadDiscussionScreen(
                                                    team.id,
                                                    team.name,
                                                    thread.id
                                                )
                                            )
                                        }
                                    )
                                }
                                
                                if (filteredThreads.isEmpty() && searchQuery.isNotEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "No matching threads found",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showCreateThreadDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New Thread") },
                    containerColor = GFGPrimary,
                    contentColor = Color.White
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(GFGBackground)
            ) {
                SwipeRefresh(
                    state = swipeRefreshState,
                    onRefresh = { viewModel.refreshThreads() }
                ) {
                    if (isLoading && threads.isEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(5) {
                                ShimmerThreadItem()
                            }
                        }
                    } else if (threads.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = GFGPrimary.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No threads yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Start a discussion by creating a new thread",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { showCreateThreadDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GFGPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create Thread")
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Display pinned threads first
                            val pinnedThreads = threads.filter { it.isPinned }
                            val unpinnedThreads = threads.filter { !it.isPinned }
                            
                            if (pinnedThreads.isNotEmpty()) {
                                item {
                                    Text(
                                        "PINNED",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GFGBlack.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                                    )
                                }
                                
                                items(pinnedThreads) { thread ->
                                    MenteeThreadCard(
                                        thread = thread,
                                        onClick = {
                                            navigator.push(
                                                ThreadDiscussionScreen(
                                                    team.id,
                                                    team.name,
                                                    thread.id
                                                )
                                            )
                                        }
                                    )
                                }
                                
                                item {
                                    Divider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = GFGBlack.copy(alpha = 0.1f)
                                    )
                                }
                            }
                            
                            if (unpinnedThreads.isNotEmpty()) {
                                item {
                                    Text(
                                        "DISCUSSIONS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GFGBlack.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                                    )
                                }
                                
                                items(unpinnedThreads) { thread ->
                                    MenteeThreadCard(
                                        thread = thread,
                                        onClick = {
                                            navigator.push(
                                                ThreadDiscussionScreen(
                                                    team.id,
                                                    team.name,
                                                    thread.id
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Create Thread Dialog
            if (showCreateThreadDialog) {
                CreateThreadDialog(
                    onDismiss = { showCreateThreadDialog = false },
                    onSubmit = { title, message, category, tags ->
                        scope.launch {
                            viewModel.createThread(team.id, title, message, category, tags)
                                .collectLatest { result ->
                                    result.onSuccess {
                                        showCreateThreadDialog = false
                                        snackbarHostState.showSnackbar("Thread created successfully")
                                    }.onFailure { error ->
                                        snackbarHostState.showSnackbar("Failed to create thread: ${error.message}")
                                    }
                                }
                        }
                    }
                )
            }
        }
    }

    @Composable
    private fun ThreadSearchResult(
        thread: ThreadDetails,
        onClick: () -> Unit
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (thread.isResolved) Color.Green.copy(alpha = 0.1f)
                            else if (thread.isPinned) GFGStatusPendingText.copy(alpha = 0.1f)
                            else GFGPrimary.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            thread.isResolved -> Icons.Default.CheckCircle
                            thread.isPinned -> Icons.Default.PushPin
                            else -> Icons.Default.Forum
                        },
                        contentDescription = null,
                        tint = when {
                            thread.isResolved -> Color.Green
                            thread.isPinned -> GFGStatusPendingText
                            else -> GFGPrimary
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = thread.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = thread.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SuggestionChip(
                            onClick = { },
                            label = { Text(thread.category, style = MaterialTheme.typography.labelSmall) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = GFGPrimary.copy(alpha = 0.1f),
                                labelColor = GFGPrimary
                            )
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = formatTimestamp(thread.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val now = Date()
        val diff = now.time - date.time
        
        return when {
            diff < 60 * 60 * 1000 -> {
                val minutes = (diff / (60 * 1000)).toInt()
                "$minutes min${if (minutes > 1) "s" else ""} ago"
            }
            diff < 24 * 60 * 60 * 1000 -> {
                val hours = (diff / (60 * 60 * 1000)).toInt()
                "$hours hour${if (hours > 1) "s" else ""} ago"
            }
            else -> {
                SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
            }
        }
    }
}