package com.ayush.geeksforgeeks.presentation.mentorship.mentee.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ayush.geeksforgeeks.data.model.ThreadDetails
import com.ayush.geeksforgeeks.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MenteeThreadCard(
    thread: ThreadDetails,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (thread.isPinned) 
                GFGStatusPendingText.copy(alpha = 0.05f) 
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Thread header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status indicator
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                thread.isResolved -> Color.Green.copy(alpha = 0.1f)
                                thread.isPinned -> GFGStatusPendingText.copy(alpha = 0.1f)
                                else -> GFGPrimary.copy(alpha = 0.1f)
                            }
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
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = thread.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (thread.category.isNotEmpty()) {
                            SuggestionChip(
                                onClick = { },
                                label = { Text(thread.category) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = GFGPrimary.copy(alpha = 0.1f),
                                    labelColor = GFGPrimary
                                ),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        
                        // Thread tags (up to 2)
                        thread.tags.take(2).forEach { tag ->
                            AssistChip(
                                onClick = { },
                                label = { 
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelSmall
                                    ) 
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = GFGStatusPending.copy(alpha = 0.2f),
                                    labelColor = GFGBlack.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        
                        // Show indicator if there are more tags
                        if (thread.tags.size > 2) {
                            Text(
                                text = "+${thread.tags.size - 2}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Thread message preview
            Text(
                text = thread.message,
                style = MaterialTheme.typography.bodyMedium,
                color = GFGBlack.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Thread footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status indicators
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!thread.isEnabled) {
                        Surface(
                            color = GFGStatusPending.copy(alpha = 0.2f),
                            contentColor = GFGStatusPendingText,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "Awaiting Response",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    
                    if (thread.isResolved) {
                        Surface(
                            color = Color.Green.copy(alpha = 0.1f),
                            contentColor = Color.Green,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "Resolved",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                
                // Thread metadata
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reply count
                    if (thread.repliesCount > 0) {
                        Icon(
                            imageVector = Icons.Default.Message,
                            contentDescription = "Replies",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Text(
                            text = "${thread.repliesCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp, end = 12.dp)
                        )
                    }
                    
                    // Last activity time
                    val timeFormatted = formatTime(thread.lastMessageAt)
                    Text(
                        text = "Last reply $timeFormatted",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "just now"
        diff < 3_600_000 -> "${diff / 60_000} min ago"
        diff < 86_400_000 -> "${diff / 3_600_000} hr ago"
        diff < 604_800_000 -> "${diff / 86_400_000} day ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}