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
                isLoggedIn = preferences[IS_USER_LOGGED_IN] ?: false,
                role = UserRole.valueOf(preferences[USER_ROLE] ?: UserRole.MEMBER.toString()),
                domainId = preferences[USER_DOMAIN_ID] ?: 0
            )
        }

    suspend fun setUserData(userSettings: UserSettings) {
        dataStore.edit { preferences ->
            preferences[USER_NAME] = userSettings.name
            preferences[USER_ID] = userSettings.userId
            preferences[USER_EMAIL] = userSettings.email
            userSettings.profilePicUrl?.let { preferences[USER_PROFILE_PIC] = it }
            preferences[IS_USER_LOGGED_IN] = userSettings.isLoggedIn
            preferences[USER_ROLE] = userSettings.role.toString()
            preferences[USER_DOMAIN_ID] = userSettings.domainId
        }
    }

    suspend fun clearUserData() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}