package com.ayush.data.model

import androidx.annotation.Keep
import com.ayush.data.datastore.UserRole
@Keep
data class Team(val id: String, val name: String)
@Keep
data class TeamMember(val name: String, val email: String, val role: UserRole)