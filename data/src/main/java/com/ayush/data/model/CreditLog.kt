package com.ayush.data.model


import com.google.firebase.Timestamp
import kotlinx.serialization.Serializable

@Serializable
data class CreditLog(
    val id: String = "",
    val userId: String = "",
    val taskId: String = "",
    val credits: Int = 0,
    val reason: String = "",
    @Serializable(with = TimestampSerializer::class)
    val timestamp: Timestamp = Timestamp.now()
)