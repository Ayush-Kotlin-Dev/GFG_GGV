package com.ayush.geeksforgeeks.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.data.model.Event
import com.ayush.data.repository.HomeRepository
import com.ayush.geeksforgeeks.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject



@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val homeRepository: HomeRepository

) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeScreenState())
    val uiState: StateFlow<HomeScreenState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }


    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val events = homeRepository.fetchEvents()
                _uiState.value = _uiState.value.copy(
                    clubStats = ClubStats(
                        yearsActive = 3,
                        studentsBenefited = 1000,
                        activeMembers = 150
                    ),
                    events = events,
                    quickStats = QuickStats(
                        activeMembers = 150,
                        ongoingProjects = 5,
                        recentAchievements = 10
                    ),
                    recentActivities = SampleData.recentActivities,
                    isLoading = false
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load events: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    fun addEvent(event: Event) {
        viewModelScope.launch {
            val success = homeRepository.createEvent(event)
            if (success) {
                val currentEvents = _uiState.value.events.toMutableList()
                currentEvents.add(event)
                _uiState.value = _uiState.value.copy(events = currentEvents)
            } else {
                // Handle error (e.g., show an error message)
            }
        }
    }
}

object SampleData {

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


data class RecentAchievement(
    val icon : Int,
    val id: Int,
    val title: String,
    val description: String,
    val date : String,
    val imageRes: Int
)


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
