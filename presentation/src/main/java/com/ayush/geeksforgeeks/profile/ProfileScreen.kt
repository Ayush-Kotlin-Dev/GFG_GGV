package com.ayush.geeksforgeeks.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ContactSupport
import androidx.compose.material.icons.automirrored.filled.HelpCenter
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import coil.compose.AsyncImage
import com.ayush.data.datastore.UserSettings
import com.ayush.data.model.ContributorData
import com.ayush.geeksforgeeks.profile.components.AboutUsContent
import com.ayush.geeksforgeeks.profile.components.ContributorsContent
import com.ayush.geeksforgeeks.profile.profile_detail.ProfileDetailScreen
import com.ayush.geeksforgeeks.profile.settings.SettingsScreen
import com.ayush.geeksforgeeks.ui.theme.GFGBackground
import com.ayush.geeksforgeeks.ui.theme.GFGPrimary
import com.ayush.geeksforgeeks.ui.theme.GFGTextPrimary
import com.ayush.geeksforgeeks.utils.ErrorScreen
import com.ayush.geeksforgeeks.utils.LoadingIndicator
import com.ayush.geeksforgeeks.utils.PulseAnimation
import com.ayush.geeksforgeeks.utils.SimpleLoadingIndicator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLEncoder
import kotlin.text.Charsets.UTF_8

class ProfileScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel: ProfileViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()
        val navigator = LocalNavigator.current
        val logoutState by viewModel.logoutState.collectAsState()
        val context = LocalContext.current
        val activity = (context as? Activity)

        when (val state = uiState) {
            is ProfileViewModel.ProfileUiState.Loading -> LoadingIndicator()
            is ProfileViewModel.ProfileUiState.Success -> ProfileContent(
                state.user,
                viewModel,
                onLogout = {
                    viewModel.logOut()
                }
            )
            is ProfileViewModel.ProfileUiState.Error -> ErrorScreen(state.message)
        }

        LaunchedEffect(logoutState) {
            when (logoutState) {
                is ProfileViewModel.LogoutState.Success -> {
                    Toast.makeText(context, "Goodbye! See you again soon!", Toast.LENGTH_SHORT).show()
                    delay(1500)
                    activity?.finishAffinity()
                }
                is ProfileViewModel.LogoutState.Error -> {
                    Toast.makeText(
                        context,
                        (logoutState as ProfileViewModel.LogoutState.Error).message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {}
            }
        }
    }
}

@Composable
fun ProfileContent(
    user: UserSettings,
    viewModel: ProfileViewModel,
    onLogout: () -> Unit
) {
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    var showHelpDialog by rememberSaveable { mutableStateOf(false) }
    var showAboutUsBottomSheet by rememberSaveable { mutableStateOf(false) }
    var showContactDialog by rememberSaveable { mutableStateOf(false) }
    var showContributorsBottomSheet by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    var shouldShareOnLoad by remember { mutableStateOf(false) }
    val releaseState by viewModel.releaseState.collectAsState()
    val contributorsState by viewModel.contributorsState.collectAsState()
    var isLoadingContributors by remember { mutableStateOf(false) }

    LaunchedEffect(releaseState) {
        if (shouldShareOnLoad && !releaseState.isLoading && releaseState.release != null) {
            // Reset the flag
            shouldShareOnLoad = false

            // Share the release
            val releaseUrl = releaseState.release?.htmlUrl ?: "https://github.com/Ayush-Kotlin-Dev/GFG_GGV/releases/"
            val version = releaseState.release?.tagName?.let { " (Version $it)" } ?: ""
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Check out this app of our coding club: $releaseUrl$version"
                )
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            context.startActivity(shareIntent)
        }
    }

    LaunchedEffect(contributorsState) {
        if (isLoadingContributors && !contributorsState.isLoading) {
            isLoadingContributors = false  // Reset the loading flag

            if (contributorsState.contributors != null) {
                showContributorsBottomSheet = true
            } else if (contributorsState.error != null) {
                Toast.makeText(
                    context,
                    contributorsState.error,
                    Toast.LENGTH_SHORT
                ).show()
            } else if (contributorsState.isOffline) {
                Toast.makeText(
                    context,
                    "You're offline. Please check your internet connection.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFFF5F5F5))
    ) {
        ProfileHeader(user)

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                ProfileMenuItem(Icons.Default.ManageAccounts, "Profile") {
                    navigator?.push(ProfileDetailScreen())
                }
                ProfileMenuItem(Icons.Default.AdminPanelSettings, "Setting") {
                    navigator?.push(SettingsScreen())
                }
                ProfileMenuItem(
                    Icons.AutoMirrored.Filled.ContactSupport,
                    "Contact"
                ) { showContactDialog = true }
                ProfileMenuItem(
                    icon = Icons.Default.Share,
                    title = "Share App",
                    enabled = !releaseState.isLoading,
                    isLoading = releaseState.isLoading,
                ) {
                    if (releaseState.release == null) {
                        // Set flag to share when data is loaded
                        shouldShareOnLoad = true
                        viewModel.fetchLatestRelease()
                    } else {
                        // If we already have the data, share immediately
                        val releaseUrl = releaseState.release?.htmlUrl ?: "https://github.com/Ayush-Kotlin-Dev/GFG_GGV/releases/"
                        val version = releaseState.release?.tagName?.let { " (Version $it)" } ?: ""
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Check out this app of our coding club : $releaseUrl $version"
                            )
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    }
                }
                ProfileMenuItem(Icons.AutoMirrored.Filled.HelpCenter, "Help") { showHelpDialog = true }
                ProfileMenuItem(
                    Icons.Default.Engineering,
                    "Contributors",
                    enabled = !contributorsState.isLoading,
                    isLoading = contributorsState.isLoading
                ) {
                    if (contributorsState.contributors == null) {
                        isLoadingContributors = true
                        viewModel.loadContributors()
                    } else {
                        showContributorsBottomSheet = true
                    }
                }
                ProfileMenuItem(Icons.Rounded.Groups, "About Us") { showAboutUsBottomSheet = true }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(
            onClick = { showLogoutDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Sign Out",
                color = Color.Red,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Confirm Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Yes, Logout", color = GFGPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = GFGPrimary)
                }
            }
        )
    }
    if (showContactDialog) {
        ContactDialog(
            onDismiss = { showContactDialog = false },
            onCall = {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:+917408047420")
                }
                context.startActivity(intent)
                showContactDialog = false
            },
            onEmail = {
                val subject = "Query from GFG App"
                val body = "Hello GFG Student Chapter,\n\nI have the following query:\n\n[Your query here]\n\nBest regards,\n${user.name}"
                val encodedSubject = URLEncoder.encode(subject, UTF_8.toString()).replace("+", "%20")
                val encodedBody = URLEncoder.encode(body, UTF_8.toString()).replace("+", "%20")
                val uri = Uri.parse("mailto:gfgstudentchapterggv@gmail.com?subject=$encodedSubject&body=$encodedBody")
                val intent = Intent(Intent.ACTION_SENDTO, uri)
                context.startActivity(intent)
                showContactDialog = false
            },
            onWhatsApp = {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/+916264450423")
                }
                context.startActivity(intent)
                showContactDialog = false
            }
        )
    }

    if (showHelpDialog) {
        HelpDialog(user = user, viewModel = viewModel, onDismiss = { showHelpDialog = false })
    }

    if (showAboutUsBottomSheet) {
        AboutUsBottomSheet(onDismiss = { showAboutUsBottomSheet = false })
    }
    if (showContributorsBottomSheet) {
        ContributorsBottomSheet(
            contributors = contributorsState.contributors ?: emptyList(),
            onDismiss = {
                showContributorsBottomSheet = false
            }
        )
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    onClick: () -> Unit = { }
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled && !isLoading,
                onClick = onClick
            ),
        color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFF0F0F0), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) Color.Black else Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f)
            )
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color(0xFF4CAF50),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = if (enabled) Color.Gray else Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
    HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
}

@Composable
fun ContactDialog(
    onDismiss: () -> Unit,
    onCall: () -> Unit,
    onEmail: () -> Unit,
    onWhatsApp: () -> Unit
) {
    AlertDialog(
        icon = {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = "Call Icon",
                tint = GFGPrimary
            )
        },
        onDismissRequest = onDismiss,
        title = { Text("Contact Us") },
        text = { Text("How would you like to contact us?") },
        confirmButton = {
            TextButton(onClick = onCall) {
                Text("Call", color = GFGPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onEmail) {
                Text("Email", color = GFGPrimary)
            }
        },
        containerColor = GFGBackground,
    )
}

@Composable
fun ProfileHeader(user: UserSettings) {
    val context = LocalContext.current
    var showEnlargedImage by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(100.dp)
                .clickable { showEnlargedImage = true }
        ) {
            var isLoading by remember { mutableStateOf(true) }

            AsyncImage(
                model = user.profilePicUrl,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .drawBehind {
                        drawCircle(
                            color = Color.Black,
                            radius = size.width / 2,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    },
                contentScale = ContentScale.Crop,
                onLoading = { isLoading = true },
                onSuccess = { isLoading = false },
                onError = { isLoading = false }
            )

            if (isLoading) {
                PulseAnimation(
                    modifier = Modifier.fillMaxSize(),
                    color = GFGPrimary
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = user.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = GFGTextPrimary
        )
        Text(
            text = user.email,
            style = MaterialTheme.typography.bodyMedium,
            color = GFGTextPrimary.copy(alpha = 0.7f)
        )
    }

    if (showEnlargedImage) {
        Dialog(
            onDismissRequest = { showEnlargedImage = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                var isDialogImageLoading by remember { mutableStateOf(true) }

                AsyncImage(
                    model = user.profilePicUrl,
                    contentDescription = "Enlarged Profile Picture",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                    onLoading = { isDialogImageLoading = true },
                    onSuccess = { isDialogImageLoading = false },
                    onError = { isDialogImageLoading = false }
                )

                if (isDialogImageLoading) {
                    SimpleLoadingIndicator()
                }

                IconButton(
                    onClick = { showEnlargedImage = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun HelpDialog(user: UserSettings, viewModel: ProfileViewModel, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val queryState by viewModel.queryState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    // Request focus when dialog opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Help Icon",
                tint = GFGPrimary
            )
        },
        onDismissRequest = onDismiss,
        title = { Text("Help") },
        text = {
            Column {
                Text("Please enter your query:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
                when (val state = queryState) {
                    is ProfileViewModel.QueryState.Loading -> LoadingIndicator()
                    is ProfileViewModel.QueryState.Error -> Text(state.message, color = Color.Red)
                    is ProfileViewModel.QueryState.Success -> {
                        LaunchedEffect(Unit) {
                            onDismiss()
                        }
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.submitQuery(user.name, user.email, query)
                },
                enabled = queryState !is ProfileViewModel.QueryState.Loading
            ) {
                Text("Submit", color = GFGPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = GFGPrimary)
            }
        },
        containerColor = GFGBackground,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributorsBottomSheet(
    contributors: List<ContributorData>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 8.dp,
        scrimColor = Color.Black.copy(alpha = 0.32f),
    ) {
        ContributorsContent(
            contributors = contributors,
            onClose = {
                scope.launch {
                    sheetState.hide()
                    onDismiss()
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutUsBottomSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 8.dp,
        scrimColor = Color.Black.copy(alpha = 0.32f),
    ) {
        AboutUsContent(
            onClose = {
                scope.launch {
                    sheetState.hide()
                    onDismiss()
                }
            }
        )
    }
}