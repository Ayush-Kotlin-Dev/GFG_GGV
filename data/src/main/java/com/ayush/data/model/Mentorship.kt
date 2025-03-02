package com.ayush.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class ThreadMessage(
    val id: String = "",
    val threadId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val createdAt: Long = 0,
    @get:PropertyName("isTeamLead") @set:PropertyName("isTeamLead")
    var isTeamLead: Boolean = false
)
@Keep
@Serializable
data class ThreadDetails(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val teamId: String = "",
    val createdAt: Long = 0,
    @get:PropertyName("isEnabled") @set:PropertyName("isEnabled")
    var isEnabled: Boolean = false,
    val lastMessageAt: Long = 0,
    val repliesCount: Int = 0,
    val category: String = "General",
    val tags: List<String> = emptyList(),
    val isPinned: Boolean = false,
    val isResolved: Boolean = false
)
