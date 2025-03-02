package com.ayush.geeksforgeeks.mentorship.components


import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ayush.geeksforgeeks.ui.theme.GFGBlack
import com.ayush.geeksforgeeks.ui.theme.GFGStatusPending
import com.ayush.geeksforgeeks.ui.theme.GFGStatusPendingText
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateThreadDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, List<String>) -> Unit,
    isLoading: Boolean = false,
    error: String? = null,
    categories: List<String> = listOf("General", "Debugging", "Career", "Programming", "Resources")
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var showError by remember(error) { mutableStateOf(error != null) }
    var tagsInput by remember { mutableStateOf("") }

    // Define character limits
    val titleMaxLength = 40
    val titleMinLength = 10
    val messageMaxLength = 200
    val messageMinLength = 30

    // Validation states
    val isTitleValid = title.length in titleMinLength..titleMaxLength
    val isMessageValid = message.length in messageMinLength..messageMaxLength
    val canSubmit = isTitleValid && isMessageValid && !isLoading

    // Process tags
    val tags = tagsInput
        .split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .take(5)

    if (showError && error != null) {
        LaunchedEffect(error) {
            delay(3000)
            showError = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Ask a Question",
                    style = MaterialTheme.typography.headlineSmall,
                    color = GFGBlack
                )
                Text(
                    "Share your doubts with mentors",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GFGBlack.copy(alpha = 0.6f)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (showError && error != null) {
                    Surface(
                        color = GFGStatusPending.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, GFGStatusPendingText.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = GFGStatusPendingText,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { if (it.length <= titleMaxLength) title = it },
                        label = { Text("Question Title") },
                        placeholder = { Text("Brief, specific title ($titleMinLength-$titleMaxLength chars)") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        singleLine = true,
                        isError = title.isNotEmpty() && !isTitleValid,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GFGStatusPendingText,
                            focusedLabelColor = GFGStatusPendingText,
                            cursorColor = GFGStatusPendingText
                        ),
                        supportingText = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (title.isNotEmpty() && !isTitleValid) {
                                    Text(
                                        when {
                                            title.length < titleMinLength -> "Title too short"
                                            else -> "Title too long"
                                        },
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Text(
                                    "${title.length}/$titleMaxLength",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (title.length > titleMaxLength)
                                        MaterialTheme.colorScheme.error
                                    else
                                        GFGBlack.copy(alpha = 0.5f)
                                )
                            }
                        }
                    )
                }

                // Category selection
                ExposedDropdownMenuBox(
                    expanded = false,
                    onExpandedChange = { },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Category:", 
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Category chips
                        categories.forEach { category ->
                            val isSelected = selectedCategory == category
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = category },
                                label = { Text(category) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GFGStatusPendingText,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }

                // Tags input
                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = { Text("Tags (comma-separated)") },
                    placeholder = { Text("e.g. java, algorithms, data structures") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GFGStatusPendingText,
                        focusedLabelColor = GFGStatusPendingText,
                        cursorColor = GFGStatusPendingText
                    ),
                    supportingText = { 
                        Text("Up to 5 tags, separated by commas", style = MaterialTheme.typography.bodySmall)
                    }
                )

                // Show tag chips if any are entered
                if (tags.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        tags.forEach { tag ->
                            SuggestionChip(
                                onClick = { },
                                label = { Text(tag) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                }

                Column {
                    OutlinedTextField(
                        value = message,
                        onValueChange = { if (it.length <= messageMaxLength) message = it },
                        label = { Text("Description") },
                        placeholder = { Text("Detailed explanation ($messageMinLength-$messageMaxLength chars)") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        minLines = 3,
                        maxLines = 5,
                        isError = message.isNotEmpty() && !isMessageValid,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GFGStatusPendingText,
                            focusedLabelColor = GFGStatusPendingText,
                            cursorColor = GFGStatusPendingText
                        ),
                        supportingText = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (message.isNotEmpty() && !isMessageValid) {
                                    Text(
                                        when {
                                            message.length < messageMinLength -> "Description too short"
                                            else -> "Description too long"
                                        },
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Text(
                                    "${message.length}/$messageMaxLength",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (message.length > messageMaxLength)
                                        MaterialTheme.colorScheme.error
                                    else
                                        GFGBlack.copy(alpha = 0.5f)
                                )
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(title, message, selectedCategory, tags) },
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GFGStatusPendingText,
                    contentColor = Color.White,
                    disabledContainerColor = GFGStatusPendingText.copy(alpha = 0.3f)
                ),
                modifier = Modifier.widthIn(min = 88.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                AnimatedContent(
                    targetState = isLoading,
                    label = "loading_button"
                ) { loading ->
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Submit Question")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = GFGStatusPendingText
                )
            ) {
                Text("Cancel")
            }
        }
    )
}