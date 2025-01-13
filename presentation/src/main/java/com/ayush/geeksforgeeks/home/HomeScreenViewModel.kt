package com.ayush.geeksforgeeks.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.data.model.Event
import com.ayush.data.repository.HomeRepository
import com.ayush.geeksforgeeks.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.random.Random


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
    suspend fun addEventWithImage(event: Event, imageUri: Uri): Boolean {
        return try {
            val success = homeRepository.createEventWithImage(event, imageUri)
            if (success) {
                val currentEvents = _uiState.value.events.toMutableList()
                currentEvents.add(event)
                _uiState.value = _uiState.value.copy(events = currentEvents)
            }
            success
        } catch (e: Exception) {
            false
        }
    }
}

object SampleData {

    val recentActivities = listOf(
        RecentActivity(
            id = 1,
            userName = "GFG Team",
            action = "conducted an introductory session",
            timestamp = "21 oct 2024",
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
