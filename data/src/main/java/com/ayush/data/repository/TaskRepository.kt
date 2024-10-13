package com.ayush.data.repository

import com.ayush.data.model.Task
import com.ayush.data.model.TaskStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class TaskRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun getRecentTasksForUser(userId: String, limit: Int): List<Task> {
        return firestore.collection("tasks")
            .whereEqualTo("assignedTo", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
            .toObjects(Task::class.java)
    }

    suspend fun getTasksForUser(userId: String): List<Task> {
        return firestore.collection("tasks")
            .whereEqualTo("assignedTo", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()
            .toObjects(Task::class.java)
    }

    suspend fun updateTaskStatus(taskId: String, newStatus: TaskStatus) {
        firestore.collection("tasks")
            .document(taskId)
            .update("status", newStatus.name)
            .await()
    }

    suspend fun getTaskById(taskId: String): Task? {
        return firestore.collection("tasks")
            .document(taskId)
            .get()
            .await()
            .toObject(Task::class.java)
    }

    suspend fun getCompletedTasksCount(userId: String): Int {
        return try {
            val querySnapshot = firestore.collection("tasks")
                .whereEqualTo("assignedTo", userId)
                .whereEqualTo("status", "COMPLETED")
                .get()
                .await()

            querySnapshot.size()
        } catch (e: Exception) {
            // Log the error or handle it as needed
            println("Error getting completed tasks count: ${e.message}")
            0 // Return 0 if there's an error
        }
    }

    suspend fun getTasks(): List<Task> {
        return try {
            firestore.collection("tasks")
                .get()
                .await()
                .toObjects(Task::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addTask(task: Task) {
        try {
            firestore.collection("tasks")
                .add(task)
                .await()
        } catch (e: Exception) {
            // Handle or log error
        }
    }

    suspend fun assignTask(taskId: String, userId: String) {
        try {
            firestore.collection("tasks")
                .document(taskId)
                .update("assignedTo", userId)
                .await()
        } catch (e: Exception) {
            // Handle or log error
        }
    }
}