package com.ayush.geeksforgeeks.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.ayush.data.datastore.UserSettings
import com.ayush.geeksforgeeks.R
import com.ayush.geeksforgeeks.auth.AuthScreen
import com.ayush.geeksforgeeks.utils.AboutUsContent
import com.ayush.geeksforgeeks.dashboard.ErrorMessage
import com.ayush.geeksforgeeks.dashboard.LoadingIndicator
import com.ayush.geeksforgeeks.ui.theme.GFGBackground
import com.ayush.geeksforgeeks.ui.theme.GFGPrimary
import com.ayush.geeksforgeeks.ui.theme.GFGTextPrimary
import kotlinx.coroutines.launch
import java.net.URLEncoder
import kotlin.text.Charsets.UTF_8

class ProfileScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel: ProfileViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()
        val navigator = LocalNavigator.current

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

@Composable
fun ProfileContent(
    user: UserSettings,
    viewModel: ProfileViewModel,
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showAboutUsBottomSheet by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val appLink = "https://github.com/Ayush-Kotlin-Dev/GFG_GGV/releases/"


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
                ProfileMenuItem(Icons.Default.Person, "Profile")
                ProfileMenuItem(Icons.Default.Settings, "Setting")
                ProfileMenuItem(Icons.Default.Email, "Contact") {
                    showContactDialog = true
                }
                ProfileMenuItem(Icons.Default.Share, "Share App") {
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Check out this app of our coding club : $appLink")
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                }
                ProfileMenuItem(Icons.Default.Info, "Help") { showHelpDialog = true }
                ProfileMenuItem(Icons.Rounded.FavoriteBorder, "About Us") { showAboutUsBottomSheet = true }
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
                TextButton(onClick = onLogout) {
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
