package com.ayush.data.repository

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
            val memberDoc = firestore.collection("teams").document(teamId)
                .collection("members").document(email).get().await()

            require(memberDoc.exists()) { "Member not found in the selected team" }

            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { user ->
                // Send verification email with proper error handling
                val verificationResult = sendEmailVerification()
                if (verificationResult is EmailVerificationResult.Error) {
                    return@withContext Result.failure(Exception(verificationResult.message))
                }

                val name = memberDoc.getString("name") ?: "Unknown"
                val role = UserRole.valueOf(memberDoc.getString("role") ?: UserRole.MEMBER.name)

                // Save user data to Firestore, but not to preferences
                saveUserDataToFirestore(name, user, teamId, role)

                Result.success(user)
            } ?: Result.failure(IllegalStateException("User creation failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<FirebaseUser> = withContext(ioDispatcher) {
        Log.d("AuthRepository", "Starting login process for email: ${email.take(3)}***")
        try {
            Log.d("AuthRepository", "Attempting Firebase sign in...")
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()

            result.user?.let { user ->
                Log.d("AuthRepository", "Firebase sign in successful, uid: ${user.uid}")
                Log.d("AuthRepository", "Reloading user data...")
                user.reload().await()

                if (user.isEmailVerified) {
                    Log.d("AuthRepository", "Email is verified, fetching user data from Firestore...")

                    // Get user data from Firestore first
                    val userDoc = firestore.collection("users")
                        .document(user.uid)
                        .get()
                        .await()

                    val userSettings = userDoc.toObject(UserSettings::class.java)
                    if (userSettings == null) {
                        Log.e("AuthRepository", "User settings not found in Firestore for uid: ${user.uid}")
                        throw IllegalStateException("User data not found")
                    }
                    Log.d("AuthRepository", "User data retrieved from Firestore successfully")

                    // Update the isLoggedIn status and save to both Firestore and DataStore
                    val updatedUserSettings = userSettings.copy(isLoggedIn = true)
                    Log.d("AuthRepository", "Updating user login status...")

                    try {
                        // Save to Firestore first
                        Log.d("AuthRepository", "Saving updated user data to Firestore...")
                        firestore.collection("users")
                            .document(user.uid)
                            .set(updatedUserSettings)
                            .await()
                        Log.d("AuthRepository", "Firestore update successful")

                        // Then save to DataStore
                        Log.d("AuthRepository", "Saving user data to DataStore...")
                        userPreferences.setUserData(updatedUserSettings)
                        Log.d("AuthRepository", "DataStore update successful")

                        Log.d("AuthRepository", "Login process completed successfully")
                        Result.success(user)
                    } catch (e: Exception) {
                        Log.e("AuthRepository", "Error saving user data: ${e.message}")
                        // If saving fails, try to revert Firestore changes
                        try {
                            firestore.collection("users")
                                .document(user.uid)
                                .set(userSettings)
                                .await()
                        } catch (revertError: Exception) {
                            Log.e("AuthRepository", "Failed to revert Firestore changes: ${revertError.message}")
                        }
                        throw e
                    }
                } else {
                    Log.w("AuthRepository", "Email not verified, checking if verification email should be sent")
                    if (shouldSendVerificationEmail()) {
                        Log.d("AuthRepository", "Sending verification email...")
                        sendEmailVerification()
                    }
                    Result.failure(Exception("Email not verified"))
                }
            } ?: run {
                Log.e("AuthRepository", "Login failed: User is null after sign in")
                Result.failure(IllegalStateException("Login failed"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Login error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun saveUserDataAfterVerification(user: FirebaseUser) = withContext(ioDispatcher) {
        try {
            Log.d("AuthRepository", "Saving verified user data for uid: ${user.uid}")
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

            // Sequential operations for better error handling
            firestore.collection("users")
                .document(user.uid)
                .set(updatedUserSettings)
                .await()
            Log.d("AuthRepository", "Updated Firestore with verified user data")

            userPreferences.setUserData(updatedUserSettings)
            Log.d("AuthRepository", "Updated DataStore with verified user data")

            Log.d("AuthRepository", "Verification process completed successfully")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error saving verified user data: ${e.message}")
            throw e
        }
    }

    suspend fun sendEmailVerification(): EmailVerificationResult = withContext(ioDispatcher) {
        try {
            val user = firebaseAuth.currentUser ?: return@withContext EmailVerificationResult.Error("No user signed in")

            // Check if already verified
            user.reload().await()
            if (user.isEmailVerified) {
                return@withContext EmailVerificationResult.AlreadyVerified
            }

            // Check time interval
            if (!shouldSendVerificationEmail()) {
                return@withContext EmailVerificationResult.Error("Please wait before requesting another verification email")
            }

            user.sendEmailVerification().await()
            lastVerificationEmailSent = System.currentTimeMillis()
            EmailVerificationResult.Success
        } catch (e: Exception) {
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
                user.reload().await()
                user.isEmailVerified
            } ?: false
        } catch (e: Exception) {
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
        require(teamId.isNotBlank()) { "Team ID must not be blank" }
        try {
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
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error getting team members: ${e.message}")
            emptyList()
        }
    }

    suspend fun sendPasswordResetEmail(email: String) {
        firebaseAuth.sendPasswordResetEmail(email).await()
    }

    suspend fun logout() {
        withContext(ioDispatcher) {
            firebaseAuth.signOut()
            userPreferences.clearUserData()
        }
    }

    suspend fun isEmailVerified(): Boolean = withContext(ioDispatcher) {
        firebaseAuth.currentUser?.reload()?.await()
        firebaseAuth.currentUser?.isEmailVerified ?: false
    }

    suspend fun reloadUser() = withContext(ioDispatcher) {
        firebaseAuth.currentUser?.reload()?.await()
    }

    data class Team(val id: String, val name: String)
    data class TeamMember(val name: String, val email: String, val role: UserRole)
}