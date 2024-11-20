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
    ) = withContext(ioDispatcher) {
        val userSettings = UserSettings(
            name = username.takeIf { it.isNotBlank() } ?: user.displayName ?: "GFG User",
            userId = user.uid,
            email = user.email.orEmpty(),
            profilePicUrl = user.photoUrl?.toString(),
            isLoggedIn = false, // Set this to false initially
            domainId = teamId.toIntOrNull() ?: 0,
            role = role
        )

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

    // Modify the login function to save user data to preferences only if email is verified
    suspend fun login(email: String, password: String): Result<FirebaseUser> = withContext(ioDispatcher) {
        try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            result.user?.let { user ->
                // Reload user to get latest verification status
                user.reload().await()

                if (user.isEmailVerified) {
                    saveUserDataAfterVerification(user)
                    Result.success(user)
                } else {
                    // Only send verification email if enough time has passed
                    if (shouldSendVerificationEmail()) {
                        sendEmailVerification()
                    }
                    Result.failure(Exception("Email not verified"))
                }
            } ?: Result.failure(IllegalStateException("Login failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Add a new function to save user data to preferences after email verification
    suspend fun saveUserDataAfterVerification(user: FirebaseUser) {
        val userDoc = firestore.collection("users").document(user.uid).get().await()
        val userSettings = userDoc.toObject(UserSettings::class.java)
        userSettings?.let {
            val updatedUserSettings = it.copy(isLoggedIn = true)
            userPreferences.setUserData(updatedUserSettings)
            // Also update Firestore
            firestore.collection("users").document(user.uid).set(updatedUserSettings).await()
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