package com.ayush.data.repository

import com.ayush.data.datastore.UserPreferences
import com.ayush.data.datastore.UserRole
import com.ayush.data.datastore.UserSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: FirebaseUser) : AuthState()
    data class Error(val message: String, val exception: Throwable) : AuthState()
}

class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userPreferences: UserPreferences,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    val userData: Flow<UserSettings> = userPreferences.userData
        .flowOn(ioDispatcher)
        .distinctUntilChanged()

    suspend fun signUp(
        teamId: String,
        email: String,
        password: String
    ): Result<FirebaseUser> = withContext(ioDispatcher) {
        try {
            val memberDoc = firestore.collection("teams").document(teamId)
                .collection("members").document(email).get().await()

            require(memberDoc.exists()) { "Member not found in the selected team" }

            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { user ->
                val name = memberDoc.getString("name") ?: "Unknown"
                val role = UserRole.valueOf(memberDoc.getString("role") ?: UserRole.MEMBER.name)
                saveUserData(name, user, isNewUser = true, teamId = teamId, role = role)
                Result.success(user)
            } ?: Result.failure(IllegalStateException("User creation failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<FirebaseUser> = withContext(ioDispatcher) {
        try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            result.user?.let { user ->
                saveUserData(user = user, isNewUser = false)
                Result.success(user)
            } ?: Result.failure(IllegalStateException("Login failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun saveUserData(
        username: String = "",
        user: FirebaseUser,
        isNewUser: Boolean,
        teamId: String? = null,
        role: UserRole = UserRole.MEMBER
    ) = withContext(ioDispatcher) {
        val userSettings = UserSettings(
            name = username.takeIf { it.isNotBlank() } ?: user.displayName ?: "GFG User",
            userId = user.uid,
            email = user.email.orEmpty(),
            profilePicUrl = user.photoUrl?.toString(),
            isLoggedIn = true,
            domainId = teamId?.toIntOrNull() ?: 0,
            role = role
        )

        coroutineScope {
            launch { userPreferences.setUserData(userSettings) }
            if (isNewUser) {
                launch {
                    firestore.collection("users").document(user.uid)
                        .set(userSettings)
                        .await()
                }
            }
        }
    }

    suspend fun getUserRole(email: String): UserRole = withContext(ioDispatcher) {
        try {
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .await()

            querySnapshot.documents.firstOrNull()?.let { doc ->
                UserRole.valueOf(doc.getString("role") ?: UserRole.MEMBER.name)
            } ?: UserRole.MEMBER
        } catch (e: Exception) {
            UserRole.MEMBER
        }
    }

    suspend fun getTeams(): List<Team> = withContext(ioDispatcher) {
        val snapshot = firestore.collection("teams").get().await()
        snapshot.documents.map { doc ->
            Team(doc.id, doc.getString("name") ?: "")
        }
    }

    suspend fun getTeamMembers(teamId: String): List<TeamMember> = withContext(ioDispatcher) {
        val snapshot = firestore.collection("teams").document(teamId)
            .collection("members").get().await()
        snapshot.documents.map { doc ->
            TeamMember(
                name = doc.getString("name") ?: "",
                email = doc.id,
                role = UserRole.valueOf(doc.getString("role") ?: UserRole.MEMBER.name)
            )
        }
    }


    suspend fun sendPasswordResetEmail(email: String) {
        firebaseAuth.sendPasswordResetEmail(email).await()
    }

    data class Team(val id: String, val name: String)
    data class TeamMember(val name: String, val email: String, val role: UserRole)
}