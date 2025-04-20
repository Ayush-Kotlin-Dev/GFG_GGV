package com.ayush.geeksforgeeks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.tab.*
import com.ayush.geeksforgeeks.data.datastore.UserRole
import com.ayush.geeksforgeeks.presentation.tabs.DashboardTab
import com.ayush.geeksforgeeks.presentation.tabs.HomeTab
import com.ayush.geeksforgeeks.presentation.tabs.MentorshipTab
import com.ayush.geeksforgeeks.presentation.tabs.ProfileTab
import com.ayush.geeksforgeeks.presentation.tabs.TaskTab
import com.ayush.geeksforgeeks.ui.theme.GFGStatusPendingText

data class ContainerApp(private val userRole: UserRole) : Screen {
    @Composable
    override fun Content() {
        var showBottomBar by remember { mutableStateOf(true) }
        
        // Create initial tab only once using remember
        val initialTab = remember {
            HomeTab(isAdmin = userRole.isAdmin())
        }

        // Setup tab navigator with initial tab
        TabNavigator(initialTab) {
            val currentTab = LocalTabNavigator.current.current
            
            // Track if current tab needs status bar padding
            // HomeTab doesn't need additional top padding as it has its own
            val applyTopPadding = remember(currentTab) {
                currentTab !is HomeTab
            }
            
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    BottomNavigationBar(
                        showBottomBar = showBottomBar,
                        userRole = userRole,
                        onNavigatorChange = { show -> showBottomBar = show }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                            top = if (applyTopPadding) innerPadding.calculateTopPadding() else 0.dp,
                            end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                            bottom = if (showBottomBar) 73.dp else 0.dp
                        )
                ) {
                    CurrentTab()
                }
            }
        }
    }
}

/**
 * Extension function to check if a user role is admin or team lead
 */
private fun UserRole.isAdmin(): Boolean = 
    this == UserRole.TEAM_LEAD || this == UserRole.ADMIN

/**
 * Animated bottom navigation bar
 */
@Composable
private fun BottomNavigationBar(
    showBottomBar: Boolean,
    userRole: UserRole,
    onNavigatorChange: (Boolean) -> Unit
) {
    // Pre-calculate which tabs to show based on user role
    val tabItems = createTabItems(userRole, onNavigatorChange)
    
    AnimatedVisibility(
        visible = showBottomBar,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300))
    ) {
        NavigationBar(
            containerColor = Color.White,
            contentColor = Color.Black
        ) {
            // Render pre-calculated tab items
            tabItems.forEach { tab ->
                TabNavigationItem(tab)
            }
        }
    }
}

/**
 * Create tab items list based on user role
 */
@Composable
private fun createTabItems(userRole: UserRole, onNavigatorChange: (Boolean) -> Unit): List<Tab> {
    val isAdmin = userRole.isAdmin()
    
    // Create base tabs that all users have
    val homeTab = HomeTab(isAdmin = isAdmin)
    val dashboardTab = DashboardTab(onNavigator = onNavigatorChange)
    val profileTab = ProfileTab(onNavigator = onNavigatorChange)
    
    // Create role-specific tabs
    val roleSpecificTabs = when (userRole) {
        UserRole.GUEST -> listOf(
            MentorshipTab(onNavigator = onNavigatorChange, userRole = userRole)
        )
        UserRole.MEMBER -> listOf(
            TaskTab(onNavigator = onNavigatorChange, userRole = userRole)
        )
        UserRole.TEAM_LEAD, UserRole.ADMIN -> listOf(
            TaskTab(onNavigator = onNavigatorChange, userRole = userRole),
            MentorshipTab(onNavigator = onNavigatorChange, userRole = userRole)
        )
    }
    
    // Combine all tabs in the correct order
    return listOf(homeTab, dashboardTab) + roleSpecificTabs + listOf(profileTab)
}

/**
 * Individual tab navigation item
 */
@Composable
private fun RowScope.TabNavigationItem(tab: Tab) {
    val tabNavigator = LocalTabNavigator.current
    val selected = tabNavigator.current == tab

    NavigationBarItem(
        selected = selected,
        onClick = { tabNavigator.current = tab },
        icon = {
            TabIcon(tab, selected)
        },
        label = {
            TabLabel(tab, selected)
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.Red,
            unselectedIconColor = Color.Gray,
            indicatorColor = Color.Transparent
        ),
        alwaysShowLabel = false
    )
}

/**
 * Tab icon with selection indicator
 */
@Composable
private fun TabIcon(tab: Tab, selected: Boolean) {
    val backgroundColor = if (selected) Color.Green.copy(alpha = 0.1f) else Color.Transparent
    val iconTint = if (selected) GFGStatusPendingText else Color.Gray
    
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        tab.options.icon?.let { painter ->
            Icon(
                painter = painter,
                contentDescription = tab.options.title,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Tab label with selection indicator
 */
@Composable
private fun TabLabel(tab: Tab, selected: Boolean) {
    val fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
    val textColor = if (selected) GFGStatusPendingText else Color.Gray
    
    Text(
        text = tab.options.title,
        fontWeight = fontWeight,
        fontSize = 10.sp,
        color = textColor
    )
}