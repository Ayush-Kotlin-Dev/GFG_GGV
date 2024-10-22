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

    private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@(.+)\$".toRegex()


    suspend fun signUp(
        username: String,
        email: String,
        password: String,
        domain: Int,
        role: UserRole
    ): Result<FirebaseUser> = withContext(ioDispatcher) {
        try {
            require(email.isNotBlank() && EMAIL_REGEX.matches(email)) { "Invalid email" }
            require(password.isNotBlank()) { "Password cannot be empty" }
            require(username.isNotBlank()) { "Username cannot be empty" }

            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { user ->
                saveUserData(username, user, isNewUser = true, domain, role)
                Result.success(user)
            } ?: Result.failure(IllegalStateException("User creation failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(
        email: String,
        password: String
    ): Result<FirebaseUser> = withContext(ioDispatcher) {
        
        try {
            require(email.isNotBlank()  && EMAIL_REGEX.matches(email) ) { "Invalid email" }
            require(password.isNotBlank()) { "Password cannot be empty" }

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
        domain: Int? = null,
        role: UserRole = UserRole.TEAM_LEAD
    ) = withContext(ioDispatcher) {
        val userSettings = UserSettings(
            name = username.takeIf { it.isNotBlank() } ?: user.displayName ?: "GFG User",
            userId = user.uid,
            email = user.email.orEmpty(),
            profilePicUrl = user.photoUrl?.toString(),
            isLoggedIn = true,
            domainId = domain ?: 0,
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
                when (doc.getString("role")) {
                    "TEAM_LEAD" -> UserRole.TEAM_LEAD
                    "TEAM_MEMBER" -> UserRole.MEMBER
                    else -> UserRole.TEAM_LEAD
                }
            } ?: UserRole.TEAM_LEAD
        } catch (e: Exception) {
            UserRole.TEAM_LEAD
        }
    }

}