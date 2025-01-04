package com.ayush.geeksforgeeks.tabs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Code
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.SlideTransition
import com.ayush.data.datastore.UserRole
import com.ayush.geeksforgeeks.admin.AdminScreen
import com.ayush.geeksforgeeks.task.TasksScreen

data class TaskTab(
    @Transient
    val onNavigator: (Boolean) -> Unit,
    val userRole: UserRole
) : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = when (userRole) {
                UserRole.ADMIN -> "Task Hub"
                UserRole.TEAM_LEAD -> "Task Hub"
                else -> "My Tasks"
            }

            val icon = rememberVectorPainter(Icons.Default.Code)

            return remember {
                TabOptions(
                    index = 0u,
                    title = title,
                    icon = icon
                )
            }
        }
    val initialScreen = when (userRole) {
        UserRole.TEAM_LEAD -> AdminScreen()
        else -> TasksScreen()
    }
    @Composable
    override fun Content() {
        Navigator(initialScreen) { navigator ->
            LaunchedEffect(navigator.lastItem) {
                // Fix: Check if the last item is either TasksScreen or AdminScreen
                onNavigator(when (userRole) {
                    UserRole.TEAM_LEAD -> navigator.lastItem is AdminScreen
                    else -> navigator.lastItem is TasksScreen
                })
            }
            SlideTransition(navigator)
        }
    }
}