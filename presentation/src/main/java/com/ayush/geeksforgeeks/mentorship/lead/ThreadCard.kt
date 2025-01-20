package com.ayush.geeksforgeeks.mentorship.lead

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ayush.data.model.ThreadDetails
import com.ayush.geeksforgeeks.ui.theme.GFGBlack
import com.ayush.geeksforgeeks.ui.theme.GFGStatusPending
import com.ayush.geeksforgeeks.ui.theme.GFGStatusPendingText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ThreadCard(
    thread: ThreadDetails,
    onClick: () -> Unit,
    onEnableThread: () -> Unit
) {
    val cardColor by animateColorAsState(
        targetValue = if (!thread.isEnabled) GFGStatusPending.copy(alpha = 0.05f) else Color.White,
        label = "cardColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = thread.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = GFGBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (!thread.isEnabled) {
                    Button(
                        onClick = onEnableThread,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GFGStatusPendingText,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.padding(start = 8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Enable",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                } else {
                    Surface(
                        color = GFGStatusPending,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Active",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = GFGStatusPendingText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = thread.message,
                style = MaterialTheme.typography.bodyMedium,
                color = GFGBlack.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "By ${thread.authorName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GFGBlack.copy(alpha = 0.5f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (thread.repliesCount > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "${thread.repliesCount} ${if (thread.repliesCount == 1) "reply" else "replies"}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Text(
                        text = formatDate(thread.lastMessageAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = GFGBlack.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now" // less than 1 minute
        diff < 3600_000 -> "${diff / 60_000}m ago" // less than 1 hour
        diff < 86400_000 -> "${diff / 3600_000}h ago" // less than 1 day
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}