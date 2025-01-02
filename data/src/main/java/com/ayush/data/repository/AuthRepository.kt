package com.ayush.data.repository

import android.content.ContentValues.TAG
import android.util.Log
import com.ayush.data.datastore.UserPreferences
import com.ayush.data.datastore.UserRole
import com.ayush.data.datastore.UserSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val userUpdateLock = Mutex()

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

        // First fetch existing data
        val existingData = retryIO {
            firestore.collection("users")
                .document(user.uid)
                .get()
                .await()
                .data ?: emptyMap()
        }

        val userSettings = existingData + mapOf(
            Pair("name", username.ifBlank { user.displayName ?: "GFG User" }),
            Pair("userId", user.uid),
            Pair("email", user.email ?: ""),
            Pair("profilePicUrl", user.photoUrl?.toString()),
            Pair("isLoggedIn", false),
            Pair("domainId", teamId.toIntOrNull() ?: 0),
            Pair("role", role.name),
            Pair("totalCredits", (existingData["totalCredits"] as? Number)?.toInt() ?: 0)
        )

        retryIO {
            firestore.collection("users")
                .document(user.uid)
                .set(userSettings, SetOptions.merge())  // Use merge option
                .await()
        }
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

            val memberDoc = retryIO {
                firestore.collection("teams").document(teamId)
                    .collection("members").document(email).get().await()
            }

            require(memberDoc.exists()) { "Member not found in the selected team" }

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
        userUpdateLock.withLock {
            try {
                require(email.isNotBlank()) { "Email cannot be empty" }
                require(password.isNotBlank()) { "Password cannot be empty" }

                Log.d(TAG, "Starting login for email: ${email.take(3)}***")

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

                val userDoc = retryIO {
                    firestore.collection("users")
                        .document(user.uid)
                        .get()
                        .await()
                }

                val userSettings = userDoc.toObject(UserSettings::class.java)
                    ?: throw IllegalStateException("User data not found")

                if (!userSettings.isLoggedIn) {
                    retryIO {
                        firestore.collection("users")
                            .document(user.uid)
                            .update("isLoggedIn", true)
                            .await()
                    }
                }

                userPreferences.setUserData(userSettings.copy(isLoggedIn = true))
                Result.success(user)
            } catch (e: Exception) {
                Log.e(TAG, "Login error: ${e.message}")
                Result.failure(e)
            }
        }
    }

    suspend fun saveUserDataAfterVerification(user: FirebaseUser) = withContext(ioDispatcher) {
        userUpdateLock.withLock {
            try {
                // First check if update is needed
                val currentDoc = retryIO {
                    firestore.collection("users")
                        .document(user.uid)
                        .get()
                        .await()
                }

                val currentSettings = currentDoc.toObject(UserSettings::class.java)
                if (currentSettings?.isLoggedIn == true) {
                    // User is already logged in, no need to update
                    userPreferences.setUserData(currentSettings)
                    return@withLock
                }

                // Update only if necessary
                retryIO {
                    firestore.collection("users")
                        .document(user.uid)
                        .update(
                            mapOf(
                                "isLoggedIn" to true,
                                "userId" to user.uid
                            )
                        )
                        .await()
                }

                // Fetch final state for preferences
                val updatedDoc = retryIO {
                    firestore.collection("users")
                        .document(user.uid)
                        .get()
                        .await()
                }

                val userSettings = updatedDoc.toObject(UserSettings::class.java)
                    ?: throw IllegalStateException("User data not found in Firestore")

                userPreferences.setUserData(userSettings)
            } catch (e: Exception) {
                Log.e("AuthRepository", "Failed to save user data: ${e.message}")
                throw e
            }
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
            val userId = firebaseAuth.currentUser?.uid

            firebaseAuth.signOut()

            userPreferences.clearUserData()

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
        return block()
    }

    data class Team(val id: String, val name: String)
    data class TeamMember(val name: String, val email: String, val role: UserRole)
}