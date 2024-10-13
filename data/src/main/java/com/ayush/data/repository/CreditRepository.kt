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
}