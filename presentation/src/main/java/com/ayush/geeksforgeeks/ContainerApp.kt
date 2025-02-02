package com.ayush.geeksforgeeks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
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
import com.ayush.data.datastore.UserRole
import com.ayush.geeksforgeeks.tabs.*
import com.ayush.geeksforgeeks.ui.theme.GFGStatusPendingText

data class ContainerApp(private val userRole: UserRole) : Screen {
    @Composable
    override fun Content() {
        var showBottomBar by remember { mutableStateOf(true) }
        val initialTab = remember { HomeTab(
            isAdmin = userRole == UserRole.TEAM_LEAD || userRole == UserRole.ADMIN
        ) }

        // Add animated offset for content
        val bottomPadding by animateDpAsState(
            targetValue = if (showBottomBar) 73.dp else 0.dp,
            animationSpec = tween(300), 
            label = "bottom padding animation"
        )

        TabNavigator(initialTab) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    BottomNavigationBar(showBottomBar, userRole) { show ->
                        showBottomBar = show
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                            top = innerPadding.calculateTopPadding(),
                            end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                            bottom = bottomPadding
                        )
                ) {
                    CurrentTab()
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    showBottomBar: Boolean,
    userRole: UserRole,
    onNavigatorChange: (Boolean) -> Unit
) {
    AnimatedVisibility(
        visible = showBottomBar,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300))
    ) {
        NavigationBar(
            modifier = Modifier.height(73.dp),
            containerColor = Color.White,
            contentColor = Color.Black
        ) {
            TabNavigationItem(HomeTab(isAdmin = userRole == UserRole.TEAM_LEAD || userRole == UserRole.ADMIN))
            TabNavigationItem(DashboardTab(onNavigator = onNavigatorChange))

            when (userRole) {
                UserRole.GUEST -> {
                    TabNavigationItem(MentorshipTab(
                        onNavigator = onNavigatorChange,
                        userRole = userRole
                    ))
                }
                UserRole.MEMBER -> {
                    TabNavigationItem(TaskTab(onNavigator = onNavigatorChange, userRole = userRole))
                }
                UserRole.TEAM_LEAD, UserRole.ADMIN -> {
                    TabNavigationItem(TaskTab(onNavigator = onNavigatorChange, userRole = userRole))
                    TabNavigationItem(MentorshipTab(
                        onNavigator = onNavigatorChange,
                        userRole = userRole
                    ))
                }
                else -> {} 
            }

            TabNavigationItem(ProfileTab(onNavigator = onNavigatorChange))
        }
    }
}

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

@Composable
private fun TabIcon(tab: Tab, selected: Boolean) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (selected) Color.Green.copy(alpha = 0.1f) else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        tab.options.icon?.let { painter ->
            Icon(
                painter = painter,
                contentDescription = tab.options.title,
                tint = if (selected) GFGStatusPendingText else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun TabLabel(tab: Tab, selected: Boolean) {
    Text(
        text = tab.options.title,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        fontSize = 10.sp,
        color = if (selected) GFGStatusPendingText else Color.Gray
    )
}