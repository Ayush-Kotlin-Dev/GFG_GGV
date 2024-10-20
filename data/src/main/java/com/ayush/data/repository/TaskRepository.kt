package com.ayush.data.repository

import android.util.Log
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
        val task = getTaskById(taskId)
        task?.let { t ->
            if (newStatus == TaskStatus.COMPLETED) {
                val assignedUserId = t.assignedTo
                if (assignedUserId != null) {
                    val userRef = firestore.collection("users").document(assignedUserId)
                    firestore.runTransaction { transaction ->
                        val userSnapshot = transaction.get(userRef)
                        val currentCompletedTasks = userSnapshot.getLong("completedTasks") ?: 0
                        transaction.update(userRef, "completedTasks", currentCompletedTasks + 1)
                    }.await()
                }
            }
        }

        
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

    suspend fun getTasks(domainId: Int): List<Task> {
        return try {
            firestore.collection("tasks")
                .whereEqualTo("domainId", domainId)
                .get()
                .await()
                .toObjects(Task::class.java)
        } catch (e: Exception) {
            Log.e("TaskRepository", "Error getting tasks: ${e.message}")
            emptyList()
        }
    }

    suspend fun addTask(task: Task): String {
        return try {
            val docRef = firestore.collection("tasks").document()
            val taskWithId = task.copy(id = docRef.id)
            docRef.set(taskWithId).await()
            docRef.id
        } catch (e: Exception) {
            Log.e("TaskRepository", "Error adding task: ${e.message}")
            throw e
        }
    }

    suspend fun assignTask(taskId: String, userId: String) {
        try {
            firestore.collection("tasks")
                .document(taskId)
                .update("assignedTo", userId)
                .await()
        } catch (e: Exception) {
            Log.e("TaskRepository", "Error assigning task: ${e.message}")
        }
    }
    fun seedDummyTasks() {
//        val dummyTasks = Task.generateDummyTasks(10) // Generate 20 dummy tasks

//        val batch = firestore.batch()
//        dummyTasks.forEach { task ->
//            val docRef = firestore.collection("tasks").document(task.id)
//            batch.set(docRef, task)
//        }

//        batch.commit().addOnSuccessListener {
//            println("Successfully added dummy tasks to Firestore")
//        }.addOnFailureListener { e ->
//            println("Error adding dummy tasks to Firestore: ${e.message}")
//        }
    }

    suspend fun deleteTask(taskId: String) {
        try {
            firestore.collection("tasks")
                .document(taskId)
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e("TaskRepository", "Error deleting task: ${e.message}")
            throw e
        }
    }

    suspend fun updateTask(task: Task) {
        try {
            firestore.collection("tasks")
                .document(task.id)
                .set(task)
                .await()
        } catch (e: Exception) {
            Log.e("TaskRepository", "Error updating task: ${e.message}")
            throw e
        }
    }

    suspend fun getActiveProjectsCount(): Int {
        return try {
            val querySnapshot = firestore.collection("tasks")
                .whereEqualTo("status", TaskStatus.IN_PROGRESS.name)
                .get()
                .await()

            querySnapshot.size()
        } catch (e: Exception) {
            // Log the error or handle it as needed
            println("Error getting active projects count: ${e.message}")
            0 // Return 0 if there's an error
        }
    }
}