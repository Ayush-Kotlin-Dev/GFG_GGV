package com.ayush.data.datastore

import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName
import kotlinx.serialization.Serializable
import java.util.Locale

@Keep
@Serializable
data class UserSettings(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val profilePicUrl: String? = null,
    @field:JvmField
    val isLoggedIn: Boolean = false,
    val role: UserRole = UserRole.MEMBER,
    val domainId: Int = 0,
    val totalCredits: Int = 0,
    val fcmToken: String? = null
)

@Keep
@Serializable
data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val profilePicUrl: String? = null,
    val role: UserRole = UserRole.MEMBER,
    val domainId: Int = 0,
    val totalCredits: Int = 0,
    val isLoggedIn: Boolean = false
)

@Serializable
enum class UserRole {
    MEMBER,
    TEAM_LEAD,
    ADMIN,
    GUEST;

    companion object {
        fun fromString(role: String): UserRole {
            return when (role.uppercase(Locale.ROOT)) {
                "MEMBER" -> MEMBER
                "TEAM_LEAD" -> TEAM_LEAD
                "ADMIN" -> ADMIN
                "GUEST" -> GUEST  // Updated
                else -> MEMBER
            }
        }
    }

    override fun toString(): String {
        return name
    }
}