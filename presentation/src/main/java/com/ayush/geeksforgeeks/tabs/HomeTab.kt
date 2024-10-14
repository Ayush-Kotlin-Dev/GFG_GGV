package com.ayush.geeksforgeeks.tabs

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
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
import com.ayush.geeksforgeeks.home.HomeScreen

class HomeTab(
    private val onNavigator: (Boolean) -> Unit,
    private val userRole: UserRole
) : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = "Home"
            val icon = rememberVectorPainter(Icons.Default.Home)

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
        val initialScreen = when (userRole) {
            UserRole.TEAM_LEAD -> AdminScreen()
            else -> HomeScreen()
        }
        Log.d("HomeTab", "Content: $userRole")
        Navigator(initialScreen) { navigator ->
            LaunchedEffect(navigator) {
                onNavigator(true)
            }
            SlideTransition(navigator)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HomeTab) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}