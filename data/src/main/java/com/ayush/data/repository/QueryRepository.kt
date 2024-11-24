package com.ayush.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class QueryRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun submitQuery(name: String, email: String, query: String): Boolean {
        val problem = hashMapOf(
            "name" to name,
            "email" to email,
            "query" to query,
            "timestamp" to System.currentTimeMillis()
        )
        return try {
            firestore.collection("problems").add(problem).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}