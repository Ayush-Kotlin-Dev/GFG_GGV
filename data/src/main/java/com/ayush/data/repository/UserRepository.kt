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
        val localUser = userPreferences.userData.first()
        return if (localUser.isLoggedIn) {
            val firebaseUser = firestore.collection("users")
                .document(localUser.userId)
                .get()
                .await()
                .toObject(UserSettings::class.java)

            Log.d("UserRepository", "getCurrentUser: firebaseUser=$firebaseUser")
            firebaseUser?.let { userPreferences.setUserData(it) }
            firebaseUser ?: localUser
        } else {
            throw IllegalStateException("No user logged in")
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
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("profile_pics/${firebaseAuth.currentUser?.uid}")
        val uploadTask = imageRef.putFile(uri)
        uploadTask.await()
        return imageRef.downloadUrl.await().toString()

    }


}