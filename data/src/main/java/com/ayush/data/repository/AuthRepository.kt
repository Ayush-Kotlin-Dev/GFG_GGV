package com.ayush.data.repository

import android.provider.ContactsContract.Data
import android.util.Log
import androidx.datastore.core.DataStore
import com.ayush.data.datastore.UserPreferences
import com.ayush.data.datastore.UserSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userPreferences: UserPreferences
) {

    val userData: Flow<UserSettings> = userPreferences.userData

    suspend fun signUp(username : String , email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user!!
            saveUserData(username , user, isNewUser = true)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user!!
            saveUserData(user = user, isNewUser = false)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun saveUserData(username: String? = "", user: FirebaseUser, isNewUser: Boolean) {
        val userSettings = UserSettings(
            name = username ?: user.displayName ?: "GFG User",
            userId = user.uid,
            email = user.email ?: "",
            profilePicUrl = user.photoUrl?.toString(),
            isLoggedIn = true
        )
        Log.d("AuthRepository", "saveUserData: $userSettings")
        userPreferences.setUserData(userSettings)

        if (isNewUser) {
            firestore.collection("users").document(user.uid).set(userSettings).await()
        }
    }

    fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser

    suspend fun logout() {
        firebaseAuth.signOut()
        userPreferences.clearUserData()
    }

    suspend fun updateUserProfile(name: String, profilePicUrl: String?) {
        val user = getCurrentUser() ?: throw IllegalStateException("No user logged in")
        val updates = mutableMapOf<String, Any>()

        if (name.isNotBlank()) updates["name"] = name
        profilePicUrl?.let { updates["profilePicUrl"] = it }

        if (updates.isNotEmpty()) {
            firestore.collection("users").document(user.uid).update(updates).await()

            val currentData = userPreferences.userData.first()
            userPreferences.setUserData(currentData.copy(name = name, profilePicUrl = profilePicUrl))

            val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                displayName = name.takeIf { it.isNotBlank() }
                photoUri = profilePicUrl?.let { android.net.Uri.parse(it) }
            }
            user.updateProfile(profileUpdates).await()
        }
    }

    suspend fun isUserLoggedIn(): Boolean {
        return userPreferences.userData.map { it.isLoggedIn }.first()
    }
}


