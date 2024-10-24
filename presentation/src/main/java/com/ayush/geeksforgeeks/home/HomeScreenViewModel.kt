package com.ayush.geeksforgeeks.home

import androidx.lifecycle.ViewModel
import com.ayush.geeksforgeeks.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject


data class HomeScreenState(
    val clubStats: ClubStats = ClubStats(),
    val events: List<Event> = emptyList(),
    val quickStats: QuickStats = QuickStats(),
    val recentActivities: List<RecentActivity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ClubStats(
    val yearsActive: Int = 3,
    val studentsBenefited: Int = 1000,
    val activeMembers: Int = 150
)

data class QuickStats(
    val activeMembers: Int = 150,
    val ongoingProjects: Int = 5,
    val recentAchievements: Int = 10
)

data class Event(
    val id: Int,
    val title: String,
    val date: String,
    val time: String,
    val registrationDeadline: String,
    val formLink: String,
    val imageRes: Int,
    val description: String
)

data class RecentAchievement(
    val icon : Int,
    val id: Int,
    val title: String,
    val description: String,
    val date : String,
    val imageRes: Int
)

@HiltViewModel
class HomeScreenViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(HomeScreenState())
    val uiState: StateFlow<HomeScreenState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        // In a real app, this would come from a repository
        _uiState.value = HomeScreenState(
            clubStats = ClubStats(
                yearsActive = 3,
                studentsBenefited = 1000,
                activeMembers = 150
            ),
            events = SampleData.events,
            quickStats = QuickStats(
                activeMembers = 150,
                ongoingProjects = 5,
                recentAchievements = 10
            ),
            recentActivities = SampleData.recentActivities
        )
    }
}

object SampleData {
    val events = listOf(
        Event(
            id = 1,
            title = "GFG Info Session 2024 ",
            date = "21-10-2024",
            time = "18:00-20:00",
            registrationDeadline = "20-10-2024",
            formLink = "https://forms.gle/uTZqozJdKZjif9wMA",
            imageRes = R.drawable.gfg_info,
            description = "Join us for an informative session on GFG and its activities."
        ),
        Event(
            id = 2,
            title = "Coding Workshop",
            date = "16-12-2024",
            time = "14:00-16:00",
            registrationDeadline = "10-12-2024",
            formLink = "https://www.instagram.com/gfgsc_ggv/",
            imageRes = R.drawable.coding,
            description = "Join us for an intensive coding workshop focused on algorithms and data structures."
        ),

    )

    val recentActivities = listOf(
        RecentActivity(
            id = 1,
            userName = "Ayush Rai & Team",
            action = "Won Sih 2024 Hackathon",
            timestamp = "2 hours ago",
            userAvatar = R.drawable.sih
        ),
        RecentActivity(
            id = 2,
            userName = "Cp/Dsa Team",
            action = "won the coding competition at GFG HackFest",
            timestamp = "1 day ago",
            userAvatar = R.drawable.pixelcut_export
        ),
    )

    val infoItems = listOf(
        InfoItem(
            id = 1,
            title = "New Course Available",
            description = "Introduction to Machine Learning course is now open for registration.",
            type = InfoType.ANNOUNCEMENT
        ),
        InfoItem(
            id = 2,
            title = "Hackathon 2023",
            description = "Annual Hackathon event scheduled for August 15-17. Register now!",
            type = InfoType.EVENT
        )
    )

    val recentAchievements = listOf(
        RecentAchievement(
            icon = R.drawable.coding,
            id = 1,
            title = "Won Sih 2024 Hackathon",
            description = "Our team won the Smart India Hackathon 2024.",
            date = "2 hours ago",
            imageRes = R.drawable.sih
        ),
    )
}

data class InfoItem(
    val id: Int,
    val title: String,
    val description: String,
    val type: InfoType
)

enum class InfoType {
    ANNOUNCEMENT,
    EVENT
}

data class RecentActivity(
    val id: Int,
    val userName: String,
    val action: String,
    val timestamp: String,
    val userAvatar: Int
)
