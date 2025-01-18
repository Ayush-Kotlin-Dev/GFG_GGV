package com.ayush.geeksforgeeks.mentorship.components

fun getTeamDescription(teamName: String): String {
    return when {
        teamName.contains("App", ignoreCase = true) ->
            "Mobile app development with Kotlin & Android"
        teamName.contains("iOS", ignoreCase = true) ->
            "iOS development with Swift & SwiftUI"
        teamName.contains("Web", ignoreCase = true) ->
            "Web development with modern frameworks"
        teamName.contains("Backend", ignoreCase = true) ->
            "Backend development and system design"
        teamName.contains("ML", ignoreCase = true) ->
            "Machine Learning and AI development"
        teamName.contains("Cloud", ignoreCase = true) ->
            "Cloud infrastructure and DevOps"
        teamName.contains("UI", ignoreCase = true) ->
            "UI/UX design and implementation"
        teamName.contains("Game", ignoreCase = true) ->
            "Game development and graphics programming"
        teamName.contains("Data", ignoreCase = true) ->
            "Data Science and Analytics"
        teamName.contains("Security", ignoreCase = true) ->
            "Cybersecurity and secure coding"
        teamName.contains("QA", ignoreCase = true) ->
            "Quality Assurance and Testing"
        teamName.contains("DevOps", ignoreCase = true) ->
            "DevOps practices and tools"
        else -> "Ask questions about ${teamName.lowercase()}"
    }
}