package com.ayush.data.datastore

import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val name: String = "",
    val userId: String = "",
    val email: String = "",
    val profilePicUrl: String? = null,
    val isLoggedIn: Boolean = false
)