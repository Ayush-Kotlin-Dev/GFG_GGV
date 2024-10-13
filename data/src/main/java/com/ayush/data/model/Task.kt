package com.ayush.data.model

import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.random.Random
import com.google.firebase.Timestamp



@Serializable
data class Task(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val domainId: String = "",
    val assignedTo: String = "",
    val status: TaskStatus = TaskStatus.PENDING,
    @Serializable(with = TimestampSerializer::class)
    val dueDate: Timestamp = Timestamp.now(),
    val credits: Int = 0,
    @Serializable(with = TimestampSerializer::class)
    val createdAt: Timestamp = Timestamp.now(),
    @Serializable(with = TimestampSerializer::class)
    val updatedAt: Timestamp = Timestamp.now()
) {
    companion object {
        fun generateDummyTasks(count: Int = 10): List<Task> {
            val domains = listOf("APP", "WEB", "AIML", "CLOUD", "CYBERSECURITY")
            val members = listOf("6PSQ9xAADkV2W6adNzK2erDsLE13")

            return List(count) { index ->
                Task(
                    id = UUID.randomUUID().toString(),
                    title = "Task ${index + 1}",
                    description = "This is a description for Task ${index + 1}",
                    domainId = domains.random(),
                    assignedTo = members.random(),
                    status = TaskStatus.values().random(),
                    dueDate = Timestamp(java.util.Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000)),
                    credits = Random.nextInt(1, 11), // Random credits between 1 and 10
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )
            }
        }
    }
}

@Serializable
enum class TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED
}

