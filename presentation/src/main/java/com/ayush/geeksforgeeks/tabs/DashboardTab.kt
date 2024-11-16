package com.ayush.geeksforgeeks.tabs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.SlideTransition
import com.ayush.geeksforgeeks.dashboard.DashboardScreen

class DashboardTab(
     onNavigator: (Boolean) -> Unit
) : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val title = "Dashboard"
            val icon = rememberVectorPainter(Icons.Default.DateRange)

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