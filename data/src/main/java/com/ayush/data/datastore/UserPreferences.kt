package com.ayush.data.datastore

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
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
    }

    val userData: Flow<UserSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserSettings(
                name = preferences[USER_NAME] ?: "",
                userId = preferences[USER_ID] ?: "",
                email = preferences[USER_EMAIL] ?: "",
                profilePicUrl = preferences[USER_PROFILE_PIC],
                isLoggedIn = preferences[IS_USER_LOGGED_IN] ?: false
            )
        }

    suspend fun setUserData(userSettings: UserSettings) {
        Log.d("UserPreferences", "setUserData: $userSettings")
        dataStore.edit { preferences ->
            preferences[USER_NAME] = userSettings.name
            preferences[USER_ID] = userSettings.userId
            preferences[USER_EMAIL] = userSettings.email
            userSettings.profilePicUrl?.let { preferences[USER_PROFILE_PIC] = it }
            preferences[IS_USER_LOGGED_IN] = userSettings.isLoggedIn
        }
    }

    suspend fun clearUserData() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}