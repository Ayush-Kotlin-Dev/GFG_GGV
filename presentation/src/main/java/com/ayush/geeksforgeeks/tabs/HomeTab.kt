package com.ayush.geeksforgeeks.tabs

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
import com.ayush.geeksforgeeks.home.HomeScreen

class HomeTab(private val onNavigator: (Boolean) -> Unit) : Tab {
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
        Navigator(HomeScreen()){ navigator ->
            LaunchedEffect(navigator.lastItem){
                onNavigator(navigator.lastItem is HomeScreen)
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