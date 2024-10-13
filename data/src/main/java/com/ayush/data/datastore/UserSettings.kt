package com.ayush.data.datastore

import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val profilePicUrl: String? = null,
    val isLoggedIn: Boolean = false,
    val role: UserRole = UserRole.MEMBER,
    val domainId: String = "",
    val totalCredits: Int = 0
)

enum class UserRole {
    MEMBER,
    DOMAIN_LEAD,
    ADMIN
}