package com.ayush.data.datastore

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class UserPreferences @Inject constructor(private val dataStore: DataStore<Preferences>) {

    companion object {
        const val PREFERENCES_FILE_NAME = "app_user_settings"
        private val USER_NAME = stringPreferencesKey("user_name")
        private val USER_ID = stringPreferencesKey("user_id")
        private val USER_EMAIL = stringPreferencesKey("user_email")
        private val USER_PROFILE_PIC = stringPreferencesKey("user_profile_pic")
        private val IS_USER_LOGGED_IN = booleanPreferencesKey("is_user_logged_in")
        private val USER_ROLE = stringPreferencesKey("user_role")
        private val USER_DOMAIN_ID = intPreferencesKey("user_domain_id")
        private val FCM_TOKEN = stringPreferencesKey("fcm_token")  
        private val TOTAL_CREDITS = intPreferencesKey("total_credits")

    }

    suspend fun setUserData(userSettings: UserSettings) {
        try {
            require(userSettings.userId.isNotBlank()) { "User ID cannot be blank" }
            require(userSettings.email.isNotBlank()) { "Email cannot be blank" }
            require(userSettings.name.isNotBlank()) { "Name cannot be blank" }

            dataStore.edit { preferences ->
                preferences[USER_NAME] = userSettings.name
                preferences[USER_ID] = userSettings.userId
                preferences[USER_EMAIL] = userSettings.email
                userSettings.profilePicUrl?.let { preferences[USER_PROFILE_PIC] = it }
                preferences[IS_USER_LOGGED_IN] = userSettings.isLoggedIn
                preferences[USER_ROLE] = userSettings.role.toString()
                preferences[USER_DOMAIN_ID] = userSettings.domainId
                preferences[TOTAL_CREDITS] = userSettings.totalCredits
                userSettings.fcmToken?.let { preferences[FCM_TOKEN] = it }  
            }
        } catch (e: Exception) {
            Log.e("UserPreferences", "Error saving user data: ${e.message}")
            throw e
        }
    }

    suspend fun clearUserData() {
        try {
            Log.d("UserPreferences", "Clearing user data from DataStore")
            dataStore.edit { preferences ->
                preferences.clear()
            }
            Log.d("UserPreferences", "Successfully cleared user data")
        } catch (e: Exception) {
            Log.e("UserPreferences", "Error clearing user data: ${e.message}")
            throw e
        }
    }

    val userData: Flow<UserSettings> = dataStore.data
        .catch { exception ->
            Log.e("UserPreferences", "Error reading user data: ${exception.message}")
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            try {
                UserSettings(
                    name = preferences[USER_NAME] ?: "",
                    userId = preferences[USER_ID] ?: "",
                    email = preferences[USER_EMAIL] ?: "",
                    profilePicUrl = preferences[USER_PROFILE_PIC],
                    isLoggedIn = preferences[IS_USER_LOGGED_IN] ?: false,
                    role = UserRole.valueOf(preferences[USER_ROLE] ?: UserRole.MEMBER.toString()),
                    domainId = preferences[USER_DOMAIN_ID] ?: 0,
                    totalCredits = preferences[TOTAL_CREDITS] ?: 0,
                    fcmToken = preferences[FCM_TOKEN]  
                )
            } catch (e: Exception) {
                Log.e("UserPreferences", "Error mapping preferences to UserSettings: ${e.message}")
                throw e
            }
        }
}