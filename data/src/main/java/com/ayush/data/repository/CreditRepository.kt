package com.ayush.data.repository

import com.ayush.data.model.CreditLog
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CreditRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun getUserCreditHistory(userId: String): List<CreditLog> {
        return firestore.collection("creditLogs")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp")
            .get()
            .await()
            .toObjects(CreditLog::class.java)
    }

    suspend fun addCreditLog(creditLog: CreditLog) {
        firestore.collection("creditLogs")
            .add(creditLog)
            .await()
    }

    suspend fun getTotalClubCredits(): Int {
        return try {
            val querySnapshot = firestore.collection("creditLogs")
                .get()
                .await()

            querySnapshot.documents.sumBy { it.toObject(CreditLog::class.java)?.credits ?: 0 }
        } catch (e: Exception) {
            // Log the error or handle it as needed
            println("Error getting total club credits: ${e.message}")
            0 // Return 0 if there's an error
        }
    }
}