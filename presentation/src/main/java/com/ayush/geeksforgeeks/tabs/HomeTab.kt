package com.ayush.geeksforgeeks.tabs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.SlideTransition
import com.ayush.geeksforgeeks.home.HomeScreenEvent

/**
 * Home tab implementation for the main container
 * @param isAdmin Whether the user has admin privileges
 */
data class HomeTab(
    @Transient val isAdmin: Boolean
): Tab {
    // Cache tab options to avoid recomposition
    override val options: TabOptions
        @Composable
        get() {
            val title = "Home"
            val icon = rememberVectorPainter(Icons.Default.Dashboard)

            return remember {
                TabOptions(
                    index = 0u,
                    title = title,
                    icon = icon
                )
            }
        }

    @Composable
    override fun Content() {
        // Use efficient Navigator and transition for the home screen
        Navigator(HomeScreenEvent(isAdmin = isAdmin)) { navigator ->
            SlideTransition(navigator)
        }
    }
}