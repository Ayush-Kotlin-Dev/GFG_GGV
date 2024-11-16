package com.ayush.data.model

import com.ayush.data.datastore.UserRole

data class Team(val id: Int, val name: String)
data class TeamMember(val name: String, val role: UserRole)