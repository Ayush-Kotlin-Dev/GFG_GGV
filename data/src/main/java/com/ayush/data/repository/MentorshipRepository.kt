package com.ayush.data.repository

import android.content.ContentValues.TAG
import android.util.Log
import com.ayush.data.repository.MentorshipThread
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
import com.ayush.data.datastore.UserPreferences
import com.ayush.data.model.Team
import kotlinx.coroutines.delay

class MentorshipRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val userPreferences: UserPreferences,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        private const val MENTORSHIP_COLLECTION = "mentorship"
        private const val TEAMS_COLLECTION = "teams"
        private const val THREADS_COLLECTION = "threads"
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

    suspend fun getThreads(teamId: String): List<MentorshipThread> = withContext(ioDispatcher) {
        try {
            retryIO {
                val snapshot = firestore.collection(MENTORSHIP_COLLECTION)
                    .document(teamId)
                    .collection(THREADS_COLLECTION)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(MentorshipThread::class.java)?.copy(id = doc.id)
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
    ): Result<MentorshipThread> = withContext(ioDispatcher) {
        try {
            val userData = userPreferences.userData.first()
            val thread = MentorshipThread(
                title = title,
                message = message,
                authorId = userData.userId,
                authorName = userData.name,
                teamId = teamId,
                createdAt = System.currentTimeMillis()
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
data class MentorshipThread(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val teamId: String = "",
    val createdAt: Long = 0,
    val repliesCount: Int = 0
)