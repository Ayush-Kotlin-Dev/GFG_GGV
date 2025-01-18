package com.ayush.geeksforgeeks.mentorship.components


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
        title = { Text("Ask a Question") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showError && error != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Question Title") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true,
                    isError = title.isBlank() && title.length > 0,
                    supportingText = {
                        if (title.isBlank() && title.length > 0) {
                            Text("Title cannot be empty")
                        }
                    }
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    minLines = 3,
                    maxLines = 5,
                    isError = message.isBlank() && message.length > 0,
                    supportingText = {
                        if (message.isBlank() && message.length > 0) {
                            Text("Description cannot be empty")
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
                    containerColor = GFGStatusPendingText
                ),
                modifier = Modifier.widthIn(min = 88.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Submit")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}