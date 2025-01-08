package com.ayush.geeksforgeeks.utils

object DomainUtils {
    fun getDomainName(domainId: Int): String {
        return when (domainId) {
            0 -> "Core Leadership"
            1 -> "Web Development"
            2 -> "CP/DSA"
            3 -> "App Development"
            4 -> "AI/ML"
            5 -> "Game Development"
            6 -> "IoT"
            7 -> "Cyber Security"
            8 -> "Design & Branding"
            9 -> "Content"
            10 -> "Social Media"
            11 -> "Event Management"
            12 -> "Marketing & PR"
            else -> "Unknown Domain"
        }
    }
}