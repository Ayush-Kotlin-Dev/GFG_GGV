package com.ayush.geeksforgeeks.profile
m

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumble.appyx.core.composable.Children
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.navigation.composable.Created
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8
import javax.inject.Inject

// Color Constants
val GFGPrimary = Color(0xFF4CAF50)
val GFGBackground = Color(0xFFF5F5F5)
val GFGTextPrimary = Color.Black

// User Settings Data Class
data class UserSettings(
    val name: String,
    val email: String
)

// ViewModel with Optimized State Management
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val queryRepository: QueryRepository
) : ViewModel() {
    // Sealed class for UI State
    sealed class ProfileUiState {
        object Loading : ProfileUiState()
        data class Success(val user: UserSettings) : ProfileUiState()
        data class Error(val message: String) : ProfileUiState()
    }

    // Sealed class for Query State
    sealed class QueryState {
        object Initial : QueryState()
        object Loading : QueryState()
        object Success : QueryState()
        data class Error(val message: String) : QueryState()
    }

    // UI State Flow
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // Query State Flow
    private val _queryState = MutableStateFlow<QueryState>(QueryState.Initial)
    val queryState: StateFlow<QueryState> = _queryState.asStateFlow()

    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val user = userRepository.getUserProfile()
                _uiState.value = ProfileUiState.Success(user)
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun logOut() {
        viewModelScope.launch {
            userRepository.logout()
        }
    }

    fun submitQuery(name: String, email: String, query: String) {
        viewModelScope.launch {
            _queryState.value = QueryState.Loading
            try {
                queryRepository.submitQuery(name, email, query)
                _queryState.value = QueryState.Success
            } catch (e: Exception) {
                _queryState.value = QueryState.Error(e.message ?: "Query submission failed")
            }
        }
    }
}

// Profile Screen Node
class ProfileScreen(
    buildContext: BuildContext
) : Node(buildContext) {
    @Composable
    override fun View(modifier: Modifier) {
        val viewModel: ProfileViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val navigator = LocalNavigator.current

        LaunchedEffect(uiState) {
            when (val state = uiState) {
                is ProfileViewModel.ProfileUiState.Success -> {
                    // Perform any initial loading or setup
                }
                is ProfileViewModel.ProfileUiState.Error -> {
                    // Optionally log error or show persistent error handling
                }
                else -> {}
            }
        }

        when (val state = uiState) {
            is ProfileViewModel.ProfileUiState.Loading -> LoadingIndicator()
            is ProfileViewModel.ProfileUiState.Success -> ProfileContent(
                state.user,
                viewModel,
                onLogout = {
                    viewModel.logOut()
                    navigator?.replaceAll(AuthScreen())
                }
            )
            is ProfileViewModel.ProfileUiState.Error -> ErrorMessage(state.message)
        }
    }
}

// MenuItem Data Class
data class MenuItem(
    val icon: ImageVector,
    val title: String,
    val onClick: (() -> Unit)?
)

// Dialogs and Sheets Handlers
data class DialogHandlers(
    val onLogout: () -> Unit,
    val onDismissLogout: () -> Unit,
    val onDismissContact: () -> Unit,
    val onDismissHelp: () -> Unit,
    val onDismissAboutUs: () -> Unit,
    val user: UserSettings,
    val viewModel: ProfileViewModel,
    val context: Context
)

// Main Profile Content Composable
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
    
    val context = LocalContext.current
    val appLink = remember { "https://github.com/Ayush-Kotlin-Dev/GFG_GGV/releases/" }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(GFGBackground)
    ) {
        ProfileHeader(user)

        Spacer(modifier = Modifier.height(16.dp))

        ProfileMenuSection(
            onContactClick = { showContactDialog = true },
            onShareApp = { shareApp(context, appLink) },
            onHelpClick = { showHelpDialog = true },
            onAboutUsClick = { showAboutUsBottomSheet = true }
        )

        Spacer(modifier = Modifier.weight(1f))

        LogoutButton(onClick = { showLogoutDialog = true })
    }

    val dialogHandlers = remember(viewModel, user) {
        DialogHandlers(
            onLogout = onLogout,
            onDismissLogout = { showLogoutDialog = false },
            onDismissContact = { showContactDialog = false },
            onDismissHelp = { showHelpDialog = false },
            onDismissAboutUs = { showAboutUsBottomSheet = false },
            user = user,
            viewModel = viewModel,
            context = context
        )
    }

    ProfileDialogsAndSheets(
        showLogoutDialog = showLogoutDialog,
        showContactDialog = showContactDialog,
        showHelpDialog = showHelpDialog,
        showAboutUsBottomSheet = showAboutUsBottomSheet,
        handlers = dialogHandlers
    )
}

// Utility Functions and Remaining Composables
private fun shareApp(context: Context, appLink: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "Check out this app of our coding club : $appLink")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}

@Composable
fun ProfileMenuSection(
    onContactClick: () -> Unit,
    onShareApp: () -> Unit,
    onHelpClick: () -> Unit,
    onAboutUsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            val menuItems = remember {
                listOf(
                    MenuItem(Icons.Default.Person, "Profile", null),
                    MenuItem(Icons.Default.Settings, "Setting", null),
                    MenuItem(Icons.Default.Email, "Contact", onContactClick),
                    MenuItem(Icons.Default.Share, "Share App", onShareApp),
                    MenuItem(Icons.Default.Info, "Help", onHelpClick),
                    MenuItem(Icons.Rounded.FavoriteBorder, "About Us", onAboutUsClick)
                )
            }

            menuItems.forEachIndexed { index, item ->
                ProfileMenuItem(
                    icon = item.icon,
                    title = item.title,
                    onClick = item.onClick ?: {}
                )
            }
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit = { }
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
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
                    tint = GFGPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
    Divider(color = Color.LightGray, thickness = 0.5.dp)
}

@Composable
fun ProfileHeader(user: UserSettings) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.pixelcut_export),
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .drawBehind {
                    drawCircle(
                        color = Color.Black,
                        radius = size.width / 2,
                        style = StrokeCap.Butt
                    )
                },
            contentScale = ContentScale.Crop
        )
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
}

@Composable
fun LogoutButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
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

@Composable
fun ProfileDialogsAndSheets(
    showLogoutDialog: Boolean,
    showContactDialog: Boolean,
    showHelpDialog: Boolean,
    showAboutUsBottomSheet: Boolean,
    handlers: DialogHandlers
) {
    // Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = handlers.onDismissLogout,
            title = { Text("Confirm Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(onClick = handlers.onLogout) {
                    Text("Yes, Logout", color = GFGPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = handlers.onDismissLogout) {
                    Text("Cancel", color = GFGPrimary)
                }
            }
        )
    }

    // Contact Dialog
    if (showContactDialog) {
        ContactDialog(
            onDismiss = handlers.onDismissContact,
            onCall = {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:+917408047420")
                }
                handlers.context.startActivity(intent)
                handlers.onDismissContact()
            },
            onEmail = {
                val subject = "Query from GFG App"
                val body = "Hello GFG Student Chapter,\n\nI have the following query:\n\n[Your query here]\n\nBest regards,\n${handlers.user.name}"
                val encodedSubject = URLEncoder.encode(subject, UTF_8.toString()).replace("+", "%20")
                val encodedBody = URLEncoder.encode(body, UTF_8.toString()).replace("+", "%20")
                val uri = Uri.parse("mailto:gfgstudentchapterggv@gmail.com?subject=$encodedSubject&body=$encodedBody")
                val intent = Intent(Intent.ACTION_SENDTO, uri)
                handlers.context.startActivity(intent)
                handlers.onDismissContact()
            },
            onWhatsApp = {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/+916264450423")
                }
                handlers.context.startActivity(intent)
                handlers.onDismissContact()
            }
        )
    }

    // Help Dialog
    if (showHelpDialog) {
        HelpDialog(
            user = handlers.user,
            viewModel = handlers.viewModel,

    if (showHelpDialog) {
        HelpDialog(user = user, viewModel = viewModel, onDismiss = { showHelpDialog = false })
    }

    if (showAboutUsBottomSheet) {
        AboutUsBottomSheet(onDismiss = { showAboutUsBottomSheet = false })
    }
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.pixelcut_export),
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .drawBehind {
                    drawCircle(
                        color = Color.Black,
                        radius = size.width / 2,
                        style = Stroke(width = 2.dp.toPx())
                    )
                },
            contentScale = ContentScale.Crop
        )
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
}
@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit = { }
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.White
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
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
    Divider(color = Color.LightGray, thickness = 0.5.dp)
}

@Composable
fun HelpDialog(user: UserSettings, viewModel: ProfileViewModel, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val queryState by viewModel.queryState.collectAsState()

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
                    modifier = Modifier.fillMaxWidth()
                )
                when (val state = queryState) {
                    is ProfileViewModel.QueryState.Loading -> CircularProgressIndicator()
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
fun AboutUsBottomSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GFGBackground,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        AboutUsContent(onClose = { scope.launch { sheetState.hide() } })
    }
}
