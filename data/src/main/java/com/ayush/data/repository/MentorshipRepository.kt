package com.ayush.data.repository

import android.content.ContentValues.TAG
import android.util.Log
import com.ayush.data.datastore.UserPreferences
import com.ayush.data.model.Team
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import androidx.annotation.Keep
import com.ayush.data.datastore.UserRole
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MentorshipRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val userPreferences: UserPreferences,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        private const val MENTORSHIP_COLLECTION = "mentorship"
        private const val TEAMS_COLLECTION = "teams"
        private const val THREADS_COLLECTION = "threads"
        private const val MESSAGES_COLLECTION = "messages"
    }

    suspend fun getTeams(): List<Team> = withContext(ioDispatcher) {
        try {
            retryIO {
                // First get teams from teams collection
                val snapshot = firestore.collection(TEAMS_COLLECTION).get().await()
                snapshot.documents.map { doc ->
                    Team(doc.id, doc.getString("name") ?: "")
                }
            }
        } catch (e: Exception) {
            Log.e("MentorshipRepository", "Failed to get teams: ${e.message}")
            emptyList()
        }
    }

    suspend fun getThreads(teamId: String): List<ThreadDetails> = withContext(ioDispatcher) {
        try {
            retryIO {
                val snapshot = firestore.collection(MENTORSHIP_COLLECTION)
                    .document(teamId)
                    .collection(THREADS_COLLECTION)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ThreadDetails::class.java)?.copy(id = doc.id)
                }
            }
        } catch (e: Exception) {
            Log.e("MentorshipRepository", "Failed to get threads: ${e.message}")
            emptyList()
        }
    }

    suspend fun createThread(
        teamId: String,
        title: String,
        message: String
    ): Result<ThreadDetails> = withContext(ioDispatcher) {
        try {
            val userData = userPreferences.userData.first()
            val thread = ThreadDetails(
                title = title,
                message = message,
                authorId = userData.userId,
                authorName = userData.name,
                teamId = teamId,
                createdAt = System.currentTimeMillis(),
                isEnabled = false,
                lastMessageAt = System.currentTimeMillis(),
                repliesCount = 0
            )

            retryIO {
                val docRef = firestore.collection(MENTORSHIP_COLLECTION)
                    .document(teamId)
                    .collection(THREADS_COLLECTION)
                    .add(thread)
                    .await()

                Result.success(thread.copy(id = docRef.id))
            }
        } catch (e: Exception) {
            Log.e("MentorshipRepository", "Failed to create thread: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getThreadDetails(teamId: String, threadId: String): ThreadDetails? = 
        withContext(ioDispatcher) {
            try {
                retryIO {
                    firestore.collection(MENTORSHIP_COLLECTION)
                        .document(teamId)
                        .collection(THREADS_COLLECTION)
                        .document(threadId)
                        .get()
                        .await()
                        .toObject(ThreadDetails::class.java)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting thread details: ${e.message}")
                null
            }
        }

    fun getThreadDetailsFlow(teamId: String, threadId: String): Flow<ThreadDetails?> =
        firestore.collection(MENTORSHIP_COLLECTION)
            .document(teamId)
            .collection(THREADS_COLLECTION)
            .document(threadId)
            .snapshots()
            .map { snapshot -> 
                snapshot.toObject(ThreadDetails::class.java)?.copy(id = snapshot.id)
            }

    suspend fun getMessages(teamId: String, threadId: String): Flow<List<ThreadMessage>> =
        firestore.collection(MENTORSHIP_COLLECTION)
            .document(teamId)
            .collection(THREADS_COLLECTION)
            .document(threadId)
            .collection(MESSAGES_COLLECTION)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ThreadMessage::class.java)?.copy(id = doc.id)
                }
            }

    suspend fun sendMessage(
        teamId: String, 
        threadId: String, 
        message: String
    ): Result<ThreadMessage> = withContext(ioDispatcher) {
        try {
            val userData = userPreferences.userData.first()
            val isTeamLead = userData.role == UserRole.TEAM_LEAD

            val threadMessage = ThreadMessage(
                threadId = threadId,
                senderId = userData.userId,
                senderName = userData.name,
                message = message,
                createdAt = System.currentTimeMillis(),
                isTeamLead = isTeamLead
            )

            retryIO {
                val messageRef = firestore.collection(MENTORSHIP_COLLECTION)
                    .document(teamId)
                    .collection(THREADS_COLLECTION)
                    .document(threadId)
                    .collection(MESSAGES_COLLECTION)
                    .add(threadMessage)
                    .await()

                val batch = firestore.batch()


                val threadRef = firestore.collection(MENTORSHIP_COLLECTION)
                    .document(teamId)
                    .collection(THREADS_COLLECTION)
                    .document(threadId)

                val updates = mutableMapOf(
                    "id" to threadId,
                    "repliesCount" to FieldValue.increment(1),
                    "lastMessageAt" to threadMessage.createdAt
                )

                if (isTeamLead) {
                    updates["isEnabled"] = true
                }

                batch.update(threadRef, updates)

                // Execute batch
                batch.commit().await()

                Result.success(threadMessage.copy(id = messageRef.id))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun <T> retryIO(
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
                Log.w(TAG, "Attempt ${attempt + 1} failed: ${e.message}")
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        return block()
    }
}

@Keep
@Serializable
data class ThreadMessage(
    val id: String = "",
    val threadId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val createdAt: Long = 0,
    val isTeamLead: Boolean = false
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
    val repliesCount: Int = 0
)
