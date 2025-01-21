package com.ayush.geeksforgeeks.mentorship.lead

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
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
import com.ayush.geeksforgeeks.ui.theme.GFGPrimary
import com.ayush.geeksforgeeks.ui.theme.GFGSecondary
import com.ayush.geeksforgeeks.ui.theme.GFGStatusPending
import com.ayush.geeksforgeeks.ui.theme.GFGStatusPendingText
import com.ayush.geeksforgeeks.utils.DomainUtils

class MentorThreadsScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel: MentorThreadsViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val userSettings by viewModel.userSettings.collectAsState()

        val teamId = userSettings.domainId.toString()
        val teamName = DomainUtils.getDomainName(userSettings.domainId)

        val searchQuery by viewModel.searchQuery.collectAsState()
        val currentSort by viewModel.currentSort.collectAsState()

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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = GFGBackground,
                        titleContentColor = GFGBlack
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::updateSearchQuery
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sort by:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    SortingDropdown(
                        currentSort = currentSort,
                        onSortChange = viewModel::updateSort
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (val state = uiState) {
                        is MentorThreadsUiState.Success -> {
                            if (searchQuery.isNotEmpty() && state.threads.totalThreads == 0) {
                                EmptySearchState(searchQuery)
                            } else if (state.threads.totalThreads == 0) {
                                EmptyThreadsState()
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
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
                                                },
                                                onDeleteThread = {
                                                    viewModel.deleteThread(thread.id)
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
                                                },
                                                onDeleteThread = {
                                                    viewModel.deleteThread(thread.id)
                                                }
                                            )
                                        }
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

    @Composable
    private fun EmptySearchState(query: String) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No results found for \"$query\"",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Try different keywords",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    @Composable
    private fun EmptyThreadsState() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Chat,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No discussions yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Team discussions will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SearchBar(
        query: String,
        onQueryChange: (String) -> Unit,
        modifier: Modifier = Modifier
    ) {
        androidx.compose.material3.SearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = {},
            active = false,
            onActiveChange = {},
            placeholder = {
                Text(
                    "Search discussions",
                    color = GFGBlack.copy(alpha = 0.6f)
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = GFGBlack.copy(alpha = 0.6f)
                )
            },
            colors = SearchBarDefaults.colors(
                containerColor = GFGStatusPending.copy(alpha = 0.1f),
                dividerColor = Color.Transparent,
                inputFieldColors = TextFieldDefaults.colors(
                    focusedTextColor = GFGBlack,
                    unfocusedTextColor = GFGBlack,
                    cursorColor = GFGPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                )
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(
                    width = 1.dp,
                    color = GFGStatusPendingText,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {}
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SortingDropdown(
        currentSort: ThreadSort,
        onSortChange: (ThreadSort) -> Unit
    ) {
        var expanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            TextField(
                value = currentSort.displayName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded,
                    )
                },
                colors = ExposedDropdownMenuDefaults.textFieldColors(
                    focusedContainerColor = GFGSecondary.copy(alpha = 0.1f),
                    unfocusedContainerColor = GFGSecondary.copy(alpha = 0.1f),
                    focusedTextColor = GFGBlack,
                    unfocusedTextColor = GFGBlack,
                    focusedTrailingIconColor = GFGBlack,
                    unfocusedTrailingIconColor = GFGBlack,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .menuAnchor()
                    .width(160.dp)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .exposedDropdownSize()
                    .background(GFGStatusPending)
                    .alpha(0.8f)
            ) {
                ThreadSort.entries.forEach { sort ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                sort.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = GFGBlack
                            )
                        },
                        onClick = {
                            onSortChange(sort)
                            expanded = false
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = GFGBlack,
                            leadingIconColor = GFGBlack,
                            trailingIconColor = GFGBlack,
                            disabledTextColor = GFGBlack.copy(alpha = 0.5f),
                            disabledLeadingIconColor = GFGBlack.copy(alpha = 0.5f),
                            disabledTrailingIconColor = GFGBlack.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    }
}