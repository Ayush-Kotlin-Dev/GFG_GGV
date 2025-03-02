package com.ayush.geeksforgeeks.tabs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.SlideTransition
import com.ayush.geeksforgeeks.dashboard.DashboardScreen

/**
 * Dashboard tab implementation for analytics and statistics
 * @param onNavigator Callback for bottom navigation visibility
 */
class DashboardTab(
    private val onNavigator: (Boolean) -> Unit
) : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val title = "Dashboard"
            val icon = rememberVectorPainter(Icons.Default.Leaderboard)

            return remember {
                TabOptions(
                    index = 1u,
                    title = title,
                    icon = icon
                )
            }
        }


    @Composable
    override fun Content() {
        Navigator(screen = DashboardScreen()){ Navigator ->
            SlideTransition(navigator = Navigator)
        }
    }
}