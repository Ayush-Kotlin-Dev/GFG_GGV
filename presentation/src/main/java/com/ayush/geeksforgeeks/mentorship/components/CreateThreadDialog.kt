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

@Composable
fun CreateThreadDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit,
    isLoading: Boolean = false,
    error: String? = null
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var showError by remember(error) { mutableStateOf(error != null) }

    if (showError && error != null) {
        LaunchedEffect(error) {
            delay(3000)
            showError = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
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

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Question Title") },
                    placeholder = { Text("Enter a clear, specific title") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true,
                    isError = title.isBlank() && title.length > 0,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GFGStatusPendingText,
                        focusedLabelColor = GFGStatusPendingText,
                        cursorColor = GFGStatusPendingText
                    ),
                    supportingText = {
                        if (title.isBlank() && title.length > 0) {
                            Text(
                                "Title cannot be empty",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Description") },
                    placeholder = { Text("Explain your question in detail") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    minLines = 3,
                    maxLines = 5,
                    isError = message.isBlank() && message.isNotEmpty(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GFGStatusPendingText,
                        focusedLabelColor = GFGStatusPendingText,
                        cursorColor = GFGStatusPendingText
                    ),
                    supportingText = {
                        if (message.isBlank() && message.isNotEmpty()) {
                            Text(
                                "Description cannot be empty",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(title, message) },
                enabled = title.isNotBlank() && message.isNotBlank() && !isLoading,
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