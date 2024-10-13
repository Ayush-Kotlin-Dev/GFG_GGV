package com.ayush.geeksforgeeks.tabs



import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.SlideTransition
import com.ayush.geeksforgeeks.home.HomeScreen
import com.ayush.geeksforgeeks.task.TasksScreen


data class TaskTab(
    val onNavigator: (Boolean) -> Unit
) : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = "Task"
            val icon = rememberVectorPainter(Icons.Default.MailOutline )

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
        Navigator(TasksScreen()){ navigator ->
            LaunchedEffect(navigator.lastItem){
                onNavigator(navigator.lastItem is TasksScreen)
            }
            SlideTransition(navigator)
        }
    }
}