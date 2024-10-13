package com.ayush.data.repository


import com.ayush.data.model.Task
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
    fun seedDummyTasks() {
        val dummyTasks = Task.generateDummyTasks(10) // Generate 20 dummy tasks

        val batch = firestore.batch()
        dummyTasks.forEach { task ->
            val docRef = firestore.collection("tasks").document(task.id)
            batch.set(docRef, task)
        }

        batch.commit().addOnSuccessListener {
            println("Successfully added dummy tasks to Firestore")
        }.addOnFailureListener { e ->
            println("Error adding dummy tasks to Firestore: ${e.message}")
        }
    }
}