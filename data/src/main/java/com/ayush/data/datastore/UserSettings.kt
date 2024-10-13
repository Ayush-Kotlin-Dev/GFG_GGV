package com.ayush.data.datastore

import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class UserSettings(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val profilePicUrl: String? = null,
    @field:JvmField
    val isLoggedIn:Boolean = false,
    val role: UserRole = UserRole.MEMBER,
    val domainId: String = "",
    val totalCredits: Int = 0
)


@Serializable
data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val profilePicUrl: String? = null,
    val role: UserRole = UserRole.MEMBER,
    val domainId: String = "",
    val totalCredits: Int = 0,
    val isLoggedIn: Boolean = false
)

@Serializable
enum class UserRole {
    MEMBER,
    TEAM_LEAD,
    ADMIN;

    companion object {
        fun fromString(role: String): UserRole {
            return when (role.uppercase(Locale.ROOT)) {
                "MEMBER" -> MEMBER
                "TEAM_LEAD" -> TEAM_LEAD
                "ADMIN" -> ADMIN
                else -> MEMBER // Default to MEMBER if unknown role
            }
        }
    }

    override fun toString(): String {
        return name
    }
}