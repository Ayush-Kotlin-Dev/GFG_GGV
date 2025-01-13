package com.ayush.geeksforgeeks.home

import android.telephony.PhoneNumberUtils.formatNumber
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import coil.compose.AsyncImage
import com.ayush.data.model.Event
import com.ayush.geeksforgeeks.R
import com.ayush.geeksforgeeks.home.components.EventDetailsDialog
import com.ayush.geeksforgeeks.home.components.SectionHeader
import com.ayush.geeksforgeeks.ui.theme.GFGLightGray
import com.ayush.geeksforgeeks.ui.theme.GFGPrimary
import com.ayush.geeksforgeeks.home.components.AddEventScreen
import com.ayush.geeksforgeeks.utils.TripleOrbitLoadingAnimation
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.TextStyle
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer

data class HomeScreenEvent(
    val isAdmin: Boolean = false
) : Screen {
    @Composable
    override fun Content() {
        val viewModel: HomeScreenViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()
        val context = LocalContext.current

        HomeScreen(
            viewModel = viewModel,
            onNotificationClick = {
                android.widget.Toast.makeText(context, "Coming Soon! This feature is under development.", android.widget.Toast.LENGTH_SHORT).show()
            },
            onEventClick = { event ->
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(event.formLink)
                )
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            },
            isAdmin = isAdmin,
            context = context
        )
    }
}

@Composable
fun HomeScreen(
    viewModel: HomeScreenViewModel = hiltViewModel(),
    onNotificationClick: () -> Unit = {},
    onEventClick: (Event) -> Unit = {},
    isAdmin: Boolean,
    context: android.content.Context
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddEventDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            HomeTopBar(
                onNotificationClick = onNotificationClick,
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HeaderSection(clubStats = uiState.clubStats)
            }

            item {
                MetricsCardRow(quickStats = uiState.quickStats)
            }

            item {
                FeaturedEventCard(
                    events = uiState.events,
                    onEventClick = onEventClick,
                    isAdmin = isAdmin,
                    onAddEventClick = { showAddEventDialog = true },
                    context = context
                )
            }

            item {
                ActivitySection(activities = uiState.recentActivities, context = context)
            }

            item {
                AchievementsSection(context = context)
            }
        }
    }
    if (showAddEventDialog) {
        Dialog(
            onDismissRequest = { showAddEventDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                AddEventScreen(
                    onEventAdded = { event, imageUri ->
                        viewModel.addEventWithImage(event, imageUri)
                    },
                    onDismiss = { showAddEventDialog = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    onNotificationClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.geeksforgeeks_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                )
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Student Chapter",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        actions = {
            NotificationIcon(
                badgeCount = 3,
                onClick = onNotificationClick
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}
@Composable
private fun HeaderSection(clubStats: ClubStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF2E8B57),
                            Color(0xFF1A5D3A),
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .padding(24.dp)
        ) {
            Text(
                text = "Welcome to GFG",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            StatsRow(clubStats = clubStats)
        }
    }
}
@Composable
private fun StatsRow(clubStats: ClubStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatCounter(
            count = clubStats.yearsActive,
            label = "Years\nActive",
            icon = Icons.Filled.Timeline
        )
        StatCounter(
            count = clubStats.studentsBenefited,
            label = "Students\nBenefited",
            icon = Icons.Filled.School
        )
        StatCounter(
            count = clubStats.activeMembers,
            label = "Active\nMembers",
            icon = Icons.Filled.Groups
        )
    }
}

@Composable
private fun StatCounter(
    count: Int,
    label: String,
    icon: ImageVector,
    duration: Int = 1000
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .size(24.dp)
                .scale(animateFloatAsState(
                    if (count > 0) 1f else 0.8f
                ).value)
        )

        RollingCounter(
            targetValue = count,
            duration = duration,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RollingCounter(
    targetValue: Int,
    duration: Int,
    style: TextStyle,
    color: Color,
    fontWeight: FontWeight
) {
    val animationProgress = remember { Animatable(0f) }
    val formattedTarget = remember(targetValue) { formatNumber(targetValue) }

    // Calculate starting value (3 digits before target)
    val startingValue = remember(targetValue) {
        val offset = minOf(targetValue - 3, (targetValue * 0.95).toInt())
        maxOf(0, offset)
    }

    LaunchedEffect(targetValue) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = duration,
                easing = LinearEasing
            )
        )
    }

    val currentValue = lerp(
        start = startingValue,
        stop = targetValue,
        fraction = animationProgress.value
    )

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        formatNumber(currentValue).forEach { char ->
            FlippingDigit(
                digit = char,
                style = style,
                color = color,
                fontWeight = fontWeight
            )
            Spacer(modifier = Modifier.width(2.dp))
        }
    }
}

@Composable
private fun FlippingDigit(
    digit: Char,
    style: TextStyle,
    color: Color,
    fontWeight: FontWeight
) {
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(digit) {
        rotation.animateTo(
            targetValue = 360f,
            animationSpec = tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing
            )
        )
    }

    Box(
        modifier = Modifier
            .graphicsLayer {
                rotationX = rotation.value
                cameraDistance = 12f * density
            }
    ) {
        Text(
            text = digit.toString(),
            style = style,
            color = color,
            fontWeight = fontWeight,
            modifier = Modifier.graphicsLayer {
                if (rotation.value >= 180f) {
                    rotationX = 0f
                    alpha = if (rotation.value <= 270f) 0f else 1f
                }
            }
        )
    }
}
private fun formatNumber(number: Int): String {
    return when {
        number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000.0)
        number >= 1_000 -> String.format("%.1fK", number / 1_000.0)
        else -> number.toString()
    }
}
// Helper function for linear interpolation
private fun lerp(start: Int, stop: Int, fraction: Float): Int {
    return (start + (stop - start) * fraction).toInt()
}

@Composable
private fun MetricsCardRow(quickStats: QuickStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricCard(
            modifier = Modifier.weight(1f),
            title = "Projects",
            value = quickStats.ongoingProjects.toString(),
            icon = Icons.Filled.Terminal,
            color = MaterialTheme.colorScheme.tertiary,
            borderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
        )
        MetricCard(
            modifier = Modifier.weight(1f),
            title = "Members",
            value = quickStats.activeMembers.toString(),
            icon = Icons.Filled.PeopleAlt,
            color = MaterialTheme.colorScheme.secondary,
            borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
        )
        MetricCard(
            modifier = Modifier.weight(1f),
            title = "Events",
            value = quickStats.recentAchievements.toString(),
            icon = Icons.Filled.Event,
            color = MaterialTheme.colorScheme.primary,
            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    borderColor: Color
) {
    Card(
        modifier = modifier
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = GFGLightGray
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FeaturedEventCard(
    events: List<Event>,
    onEventClick: (Event) -> Unit,
    isAdmin: Boolean,
    onAddEventClick: () -> Unit,
    context: android.content.Context
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        SectionHeader(
            title = "Featured Events",
            action = "View All",
            isAdmin = isAdmin,
            onAddClick = onAddEventClick,
            onViewAllClick = {
                android.widget.Toast.makeText(context, "That's all we've got! Want to see more events here? Get involved and propose your innovative ideas for organizing exciting events. The club needs your creativity!", android.widget.Toast.LENGTH_LONG).show()
            }
        )

        if (events.isEmpty()) {
            // Loading state
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GFGLightGray),
                    contentAlignment = Alignment.Center
                ) {
                    TripleOrbitLoadingAnimation(
                        modifier = Modifier.size(140.dp),
                        color = GFGPrimary
                    )
                }
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { events.size })

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.height(200.dp)
            ) { page ->
                EventCard(event = events[page], onClick = { onEventClick(events[page]) })
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(events.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) GFGPrimary else Color.LightGray
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(8.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EventCard(event: Event, onClick: () -> Unit) {
    var showDetailsDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showDetailsDialog = true }
            ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            var isLoading by remember { mutableStateOf(true) }

            AsyncImage(
                model = event.imageRes,
                contentDescription = "Event Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onLoading = { isLoading = true },
                onSuccess = { isLoading = false },
                onError = { isLoading = false }
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    TripleOrbitLoadingAnimation(
                        modifier = Modifier.size(140.dp),
                        color = GFGPrimary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = event.date,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
    
    // Show dialog when long pressed
    if (showDetailsDialog) {
        EventDetailsDialog(
            event = event,
            onDismiss = { showDetailsDialog = false }
        )
    }
}

@Composable
private fun AchievementsSection(context: android.content.Context) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        SectionHeader(
            title = "Recent Achievements",
            action = "View All",
            onViewAllClick = {
                android.widget.Toast.makeText(context, "That's all we have for now! Work hard to create more achievements worth showing.", android.widget.Toast.LENGTH_LONG).show()
            }
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = GFGLightGray
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Divider()

                AchievementItem(
                    icon = Icons.Filled.Star,
                    title = "CodeJam Champions",
                    description = "1st place in regional coding competition",
                    date = "February 2024",
                    iconTint = MaterialTheme.colorScheme.tertiary
                )

                Divider()

                AchievementItem(
                    icon = Icons.Filled.Person,
                    title = "Community Growth",
                    description = "Reached 1000+ active members",
                    date = "January 2024",
                    iconTint = MaterialTheme.colorScheme.secondary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Show More Achievements")
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementItem(
    icon: ImageVector,
    title: String,
    description: String,
    date: String,
    iconTint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = iconTint.copy(alpha = 0.1f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = "View achievement details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActivitySection(activities: List<RecentActivity> , context: android.content.Context) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        SectionHeader(
            title = "Recent Activities",
            action = "See All",
            onViewAllClick = {
                android.widget.Toast.makeText(context, "That's all we have for now! Work hard to create more achievements worth showing.", android.widget.Toast.LENGTH_LONG).show()
            }
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = GFGLightGray
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                activities.forEach { activity ->
                    ActivityItem(activity = activity)
                    if (activity != activities.last()) {
                        Divider(
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityItem(activity: RecentActivity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = activity.userAvatar),
            contentDescription = stringResource(R.string.user_avatar_desc, activity.userName),
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.userName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = activity.action,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = activity.timestamp,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NotificationIcon(
    badgeCount: Int,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        BadgedBox(
            badge = {
                Badge {
                    Text(badgeCount.toString())
                }
            }
        ) {
            Icon(
                imageVector = Icons.Filled.NotificationsActive,
                contentDescription = "Notifications"
            )
        }
    }
}

