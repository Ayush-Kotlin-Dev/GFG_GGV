package com.ayush.data.repository

import android.content.ContentValues.TAG
import android.util.Log
import com.ayush.data.datastore.UserPreferences
import com.ayush.data.datastore.UserRole
import com.ayush.data.datastore.UserSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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
    object EmailVerificationRequired : AuthState()
    object EmailVerificationSent : AuthState()
    object RegistrationSuccess : AuthState()

}

sealed class EmailVerificationResult {
    object Success : EmailVerificationResult()
    object AlreadyVerified : EmailVerificationResult()
    data class Error(val message: String) : EmailVerificationResult()
}

class AuthRepository @Inject constructor(
    val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userPreferences: UserPreferences,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private var lastVerificationEmailSent: Long = 0
    private val minEmailInterval = 60_000L

    val userData: Flow<UserSettings> = userPreferences.userData
        .flowOn(ioDispatcher)
        .distinctUntilChanged()

    private suspend fun saveUserDataToFirestore(
        username: String,
        user: FirebaseUser,
        teamId: String,
        role: UserRole
    ) {
        require(teamId.isNotBlank()) { "Team ID must be provided" }
        // Construct user settings
        val userSettings = UserSettings(
            name = username.ifBlank { user.displayName ?: "GFG User" },
            userId = user.uid,
            email = user.email.orEmpty(),
            profilePicUrl = user.photoUrl?.toString(),
            isLoggedIn = false,
            domainId = teamId.toIntOrNull() ?: 0,
            role = role
        )

        // Save user settings
        firestore.collection("users").document(user.uid)
            .set(userSettings)
            .await()
    }

    suspend fun signUp(
        teamId: String,
        email: String,
        password: String
    ): Result<FirebaseUser> = withContext(ioDispatcher) {
        try {
            require(teamId.isNotBlank()) { "Team ID is required" }
            require(email.isNotBlank()) { "Email is required" }
            require(password.isNotBlank()) { "Password is required" }

            // Add retryIO here
            val memberDoc = retryIO {
                firestore.collection("teams").document(teamId)
                    .collection("members").document(email).get().await()
            }

            require(memberDoc.exists()) { "Member not found in the selected team" }

            // Add retryIO here
            val result = retryIO {
                firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            }

            result.user?.let { user ->
                val verificationResult = sendEmailVerification()
                if (verificationResult is EmailVerificationResult.Error) {
                    return@withContext Result.failure(Exception(verificationResult.message))
                }

                val name = memberDoc.getString("name") ?: "Unknown"
                val role = UserRole.valueOf(memberDoc.getString("role") ?: UserRole.MEMBER.name)

                // Add retryIO here
                retryIO {
                    saveUserDataToFirestore(name, user, teamId, role)
                }

                Result.success(user)
            } ?: Result.failure(IllegalStateException("User creation failed"))
        } catch (e: Exception) {
            Log.e(TAG, "Signup failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<FirebaseUser> = withContext(ioDispatcher) {
        try {
            require(email.isNotBlank()) { "Email cannot be empty" }
            require(password.isNotBlank()) { "Password cannot be empty" }

            Log.d(TAG, "Starting login for email: ${email.take(3)}***")

            // Try sign in
            val result = retryIO {
                firebaseAuth.signInWithEmailAndPassword(email, password).await()
            }

            val user = result.user ?: return@withContext Result.failure(
                IllegalStateException("Login failed: No user returned")
            )

            try {
                retryIO {
                    user.reload().await()
                }
                if (!user.isEmailVerified) {
                    if (shouldSendVerificationEmail()) {
                        sendEmailVerification()
                    }
                    return@withContext Result.failure(Exception("Email not verified"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking email verification: ${e.message}")
                return@withContext Result.failure(e)
            }

            // Get and update user data
            try {
                val userDoc = firestore.collection("users")
                    .document(user.uid)
                    .get()
                    .await()

                val userSettings = userDoc.toObject(UserSettings::class.java)
                    ?: throw IllegalStateException("User data not found")

                val updatedUserSettings = userSettings.copy(isLoggedIn = true)

                // Transaction to ensure data consistency
                firestore.runTransaction { transaction ->
                    transaction.set(
                        firestore.collection("users").document(user.uid),
                        updatedUserSettings
                    )
                }.await()

                userPreferences.setUserData(updatedUserSettings)

                Result.success(user)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating user data: ${e.message}")
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun saveUserDataAfterVerification(user: FirebaseUser) = withContext(ioDispatcher) {
        try {
            retryIO {
                val userDoc = firestore.collection("users")
                    .document(user.uid)
                    .get()
                    .await()

                val userSettings = userDoc.toObject(UserSettings::class.java)
                    ?: throw IllegalStateException("User data not found in Firestore")

                val updatedUserSettings = userSettings.copy(
                    isLoggedIn = true,
                    userId = user.uid
                )

                // Use transaction for atomic operation with retry
                firestore.runTransaction { transaction ->
                    transaction.set(
                        firestore.collection("users").document(user.uid),
                        updatedUserSettings
                    )
                }.await()

                userPreferences.setUserData(updatedUserSettings)
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to save user data after retries: ${e.message}")
            throw e
        }
    }

    suspend fun sendEmailVerification(): EmailVerificationResult = withContext(ioDispatcher) {
        try {
            val user = firebaseAuth.currentUser ?: return@withContext EmailVerificationResult.Error("No user signed in")

            retryIO {
                user.reload().await()
            }

            if (user.isEmailVerified) {
                return@withContext EmailVerificationResult.AlreadyVerified
            }

            if (!shouldSendVerificationEmail()) {
                return@withContext EmailVerificationResult.Error("Please wait before requesting another verification email")
            }

            retryIO {
                user.sendEmailVerification().await()
            }
            lastVerificationEmailSent = System.currentTimeMillis()
            EmailVerificationResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send verification email: ${e.message}")
            EmailVerificationResult.Error(e.message ?: "Failed to send verification email")
        }
    }

    private fun shouldSendVerificationEmail(): Boolean {
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastVerificationEmailSent) >= minEmailInterval
    }

    suspend fun checkEmailVerificationStatus(): Boolean = withContext(ioDispatcher) {
        try {
            firebaseAuth.currentUser?.let { user ->
                retryIO {
                    user.reload().await()
                    user.isEmailVerified
                }
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check email verification status: ${e.message}")
            false
        }
    }

    private suspend fun saveUserData(
        username: String = "",
        user: FirebaseUser,
        isNewUser: Boolean,
        teamId: String? = null,
        role: UserRole = UserRole.MEMBER
    ) = withContext(ioDispatcher) {
        try {
            val userSettings = UserSettings(
                name = username.takeIf { it.isNotBlank() } ?: user.displayName ?: "GFG User",
                userId = user.uid,
                email = user.email.orEmpty(),
                profilePicUrl = user.photoUrl?.toString(),
                isLoggedIn = true,
                domainId = teamId?.toIntOrNull() ?: 0,
                role = role
            )

            retryIO {
                firestore.runTransaction { transaction ->
                    transaction.set(
                        firestore.collection("users").document(user.uid),
                        userSettings
                    )
                }.await()
                userPreferences.setUserData(userSettings)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save user data: ${e.message}")
            throw e
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
        try {
            retryIO {
                val snapshot = firestore.collection("teams").get().await()
                snapshot.documents.map { doc ->
                    Team(doc.id, doc.getString("name") ?: "")
                }
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to get teams after retries: ${e.message}")
            emptyList()
        }
    }

    suspend fun getTeamMembers(teamId: String): List<TeamMember> = withContext(ioDispatcher) {
        require(teamId.isNotBlank()) { "Team ID must not be blank" }
        try {
            retryIO {
                val snapshot = firestore.collection("teams")
                    .document(teamId)
                    .collection("members")
                    .get()
                    .await()

                snapshot.documents.mapNotNull { doc ->
                    try {
                        TeamMember(
                            name = doc.getString("name") ?: throw IllegalStateException("Name not found"),
                            email = doc.id,
                            role = UserRole.valueOf(doc.getString("role") ?: UserRole.MEMBER.name)
                        )
                    } catch (e: Exception) {
                        Log.e("AuthRepository", "Error mapping team member: ${e.message}")
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error getting team members after retries: ${e.message}")
            emptyList()
        }
    }

    suspend fun sendPasswordResetEmail(email: String) = withContext(ioDispatcher) {
        try {
            require(email.isNotBlank()) { "Email cannot be empty" }
            retryIO {
                firebaseAuth.sendPasswordResetEmail(email).await()
            }
            Log.d(TAG, "Password reset email sent successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send password reset email: ${e.message}")
            throw e
        }
    }

    suspend fun logout() = withContext(ioDispatcher) {
        try {
            // Get user ID before signing out
            val userId = firebaseAuth.currentUser?.uid

            // Sign out from Firebase
            firebaseAuth.signOut()

            // Clear local data
            userPreferences.clearUserData()

            // Update Firestore if we have the user ID
            userId?.let {
                try {
                    firestore.collection("users")
                        .document(it)
                        .update("isLoggedIn", false)
                        .await()
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating Firestore login status: ${e.message}")
                }
            }

            Log.d(TAG, "Logout completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during logout: ${e.message}")
            throw e
        }
    }

    suspend fun isEmailVerified(): Boolean = withContext(ioDispatcher) {
        try {
            retryIO {
                firebaseAuth.currentUser?.let { user ->
                    user.reload().await()
                    user.isEmailVerified
                } ?: false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check email verification: ${e.message}")
            false
        }
    }

    suspend fun reloadUser() = withContext(ioDispatcher) {
        firebaseAuth.currentUser?.reload()?.await()
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
                Log.w(TAG, "Attempt ${attempt + 1} failed: ${e.message}")
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        return block() // last attempt
    }
    data class Team(val id: String, val name: String)
    data class TeamMember(val name: String, val email: String, val role: UserRole)
}