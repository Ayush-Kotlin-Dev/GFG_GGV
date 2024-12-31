package com.ayush.data.repository

import android.net.Uri
import android.util.Log
import com.ayush.data.datastore.User
import com.ayush.data.datastore.UserPreferences
import com.ayush.data.datastore.UserSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val userPreferences: UserPreferences
) {
    suspend fun getCurrentUser(): UserSettings {
        try {
            // First check if there's a Firebase user
            val firebaseUser = firebaseAuth.currentUser
                ?: throw IllegalStateException("No user logged in")

            Log.d("UserRepository", "Firebase user ID: ${firebaseUser.uid}")

            // Try to get user from DataStore
            val localUser = userPreferences.userData.first()
            Log.d("UserRepository", "Local user from DataStore: $localUser")

            // If local user is valid and logged in, use it
            if (localUser.isLoggedIn && localUser.userId.isNotBlank()) {
                Log.d("UserRepository", "Using local user data")
                return localUser
            }

            // If local user is invalid or not logged in, fetch from Firestore
            Log.d("UserRepository", "Fetching user data from Firestore")
            val firestoreUser = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()
                .toObject(UserSettings::class.java)
                ?: throw IllegalStateException("User data not found in Firestore")

            // Update DataStore with Firestore data
            Log.d("UserRepository", "Updating DataStore with Firestore data")
            userPreferences.setUserData(firestoreUser)

            return firestoreUser
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting current user: ${e.message}", e)
            throw e
        }
    }

    suspend fun getTopContributors(limit: Int = 5): List<UserSettings> {
        return try {
            val querySnapshot = firestore.collection("users")
                .orderBy("totalCredits", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            querySnapshot.documents.mapNotNull { document ->
                document.toObject(UserSettings::class.java)
            }
        } catch (e: Exception) {
            // Log the error or handle it as needed
            println("Error getting top contributors: ${e.message}")
            emptyList() // Return an empty list if there's an error
        }
    }

    suspend fun updateUser(userSettings: UserSettings) {
        require(userSettings.userId.isNotBlank()) { "User ID must not be blank" }
        firestore.collection("users")
            .document(userSettings.userId)
            .set(userSettings)
            .await()
        userPreferences.setUserData(userSettings)
    }

    suspend fun getTeamMembers(): List<User> {
        return try {

            val currentUser = getCurrentUser()
            val leadDomainId = currentUser.domainId
            Log.d("UserRepository", "getTeamMembers: leadDomainId=$leadDomainId")
            firestore.collection("users")
                .whereEqualTo("role", "MEMBER")
                .whereEqualTo("domainId", leadDomainId)
                .get()
                .await()
                .toObjects(User::class.java)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting team members: ${e.message}")
            emptyList()
        }
    }

    suspend fun logout() {
        firebaseAuth.signOut()
        userPreferences.clearUserData()
    }

    suspend fun uploadProfileImage(uri: Uri): String {
        val userId = firebaseAuth.currentUser?.uid
            ?: throw IllegalStateException("No user logged in")
        try {
            // Upload to Firebase Storage
            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("profile_pics/$userId")
            val uploadTask = imageRef.putFile(uri)
            uploadTask.await()
            val imageUrl = imageRef.downloadUrl.await().toString()

            // Get current user data
            val currentUser = userPreferences.userData.first()
            val updatedUser = currentUser.copy(profilePicUrl = imageUrl)

            // Update both Firestore and local preferences in one go
            updateUser(updatedUser) // This function handles both Firestore and local updates

            return imageUrl
        } catch (e: Exception) {
            throw Exception("Failed to update profile picture: ${e.message}")
        }
    }


    suspend fun getTotalMembersCount(): Int {
        return try {
            val querySnapshot = firestore.collection("users")
                .get()
                .await()

            querySnapshot.size()
        } catch (e: Exception) {
            // Log the error or handle it as needed
            println("Error getting total members count: ${e.message}")
            0 // Return 0 if there's an error
        }
    }
}