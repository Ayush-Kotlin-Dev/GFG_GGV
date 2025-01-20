package com.ayush.geeksforgeeks.tabs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.SlideTransition
import com.ayush.data.datastore.UserRole
import com.ayush.geeksforgeeks.mentorship.GuestMentorshipScreen
import com.ayush.geeksforgeeks.mentorship.lead.MentorThreadsScreen

data class MentorshipTab(
    @Transient
    private val onNavigator: (Boolean) -> Unit = {},
    private val userRole: UserRole
) : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = when (userRole) {
                UserRole.TEAM_LEAD, UserRole.ADMIN -> "Mentorship Hub"
                else -> "Ask Mentor"
            }

            val icon = rememberVectorPainter(Icons.Default.SupervisorAccount)

            return remember {
                TabOptions(
                    index = 4u,
                    title = title,
                    icon = icon
                )
            }
        }

    private val initialScreen = when (userRole) {
        UserRole.TEAM_LEAD, UserRole.ADMIN -> MentorThreadsScreen() // Using default values
        else -> GuestMentorshipScreen()
    }

    @Composable
    override fun Content() {
        Navigator(initialScreen) { navigator ->
            LaunchedEffect(navigator.lastItem) {
                onNavigator(when (userRole) {
                    UserRole.TEAM_LEAD, UserRole.ADMIN ->
                        navigator.lastItem is MentorThreadsScreen
                    else -> navigator.lastItem is GuestMentorshipScreen
                })
            }
            SlideTransition(navigator)
        }
    }
}