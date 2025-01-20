package com.ayush.data.repository

import com.ayush.data.datastore.UserPreferences
import com.ayush.data.model.ThreadDetails
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class MentorRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val userPreferences: UserPreferences,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        private const val MENTORSHIP_COLLECTION = "mentorship"
        private const val TEAMS_COLLECTION = "teams"
        private const val THREADS_COLLECTION = "threads"
    }

    // Get all threads for a specific team
    suspend fun getTeamThreads(): Flow<List<ThreadDetails>> {
        val teamId = userPreferences.userData.first().domainId.toString()

        return firestore.collection(MENTORSHIP_COLLECTION)
            .document(teamId)
            .collection(THREADS_COLLECTION)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ThreadDetails::class.java)?.copy(id = doc.id)
                }
            }
    }

    // Update thread status
    suspend fun updateThreadStatus(threadId: String, isEnabled: Boolean) {
        val teamId = userPreferences.userData.first().domainId.toString()
        
        retryIO {
            firestore.collection(MENTORSHIP_COLLECTION)
                .document(teamId)
                .collection(THREADS_COLLECTION)
                .document(threadId)
                .update("isEnabled", isEnabled)
                .await()
        }
    }

    suspend fun deleteThread(threadId: String) {
        val teamId = userPreferences.userData.first().domainId.toString()
        retryIO {
            firestore.collection(MENTORSHIP_COLLECTION)
                .document(teamId)
                .collection(THREADS_COLLECTION)
                .document(threadId)
                .delete()
                .await()
        }
    }

    private suspend fun <T> retryIO(
        times: Int = 3,
        initialDelay: Long = 100,
        maxDelay: Long = 1000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(times - 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        return block()
    }
}