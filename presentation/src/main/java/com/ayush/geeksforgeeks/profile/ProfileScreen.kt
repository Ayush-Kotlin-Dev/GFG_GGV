package com.ayush.geeksforgeeks.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import coil.compose.rememberAsyncImagePainter
import com.ayush.data.datastore.UserRole
import com.ayush.data.datastore.UserSettings
import com.ayush.geeksforgeeks.R
import com.ayush.geeksforgeeks.auth.AuthScreen
import com.ayush.geeksforgeeks.dashboard.ErrorMessage
import com.ayush.geeksforgeeks.dashboard.LoadingIndicator

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
                viewModel::updateProfile,
                viewModel::uploadProfilePicture,
                onLogout = {
                    viewModel.logOut()
                    navigator?.push(AuthScreen())
                }
            )
            is ProfileViewModel.ProfileUiState.Error -> ErrorMessage(state.message)
        }
    }
}

@Composable
fun ProfileContent(
    user: UserSettings,
    onUpdateProfile: (String, String?) -> Unit,
    onUpdateProfilePicture: (Uri) -> Unit,
    onLogout: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(user.name) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showRoleInfo by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onUpdateProfilePicture(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Profile Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Profile Picture Section
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 24.dp)
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = user.profilePicUrl ?: R.drawable.ic_launcher_foreground,
                ),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { imagePicker.launch("image/*") },
                contentScale = ContentScale.Crop
            )

            IconButton(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Change Picture",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // User Info Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                if (isEditing) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    ProfileInfoRow(
                        icon = Icons.Default.Person,
                        label = "Name",
                        value = user.name
                    )
                }

                ProfileInfoRow(
                    icon = Icons.Default.Email,
                    label = "Email",
                    value = user.email
                )

                Row(
                    modifier = Modifier.clickable { showRoleInfo = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileInfoRow(
                        icon = Icons.Default.Person,
                        label = "Role",
                        value = user.role.toString()
                    )
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Role Info",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                ProfileInfoRow(
                    icon = Icons.AutoMirrored.Filled.List,
                    label = "Domain ID",
                    value = user.domainId.toString()
                )

                ProfileInfoRow(
                    icon = Icons.AutoMirrored.Filled.Send,
                    label = "Total Credits",
                    value = user.totalCredits.toString()
                )
            }
        }

        // Action Buttons
        Button(
            onClick = {
                if (isEditing) {
                    onUpdateProfile(name, user.profilePicUrl)
                }
                isEditing = !isEditing
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = if (isEditing) Icons.Default.Add else Icons.Default.Edit,
                contentDescription = if (isEditing) "Save" else "Edit",
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(if (isEditing) "Save Changes" else "Edit Profile")
        }

        OutlinedButton(
            onClick = { showLogoutDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Logout",
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Logout")
        }
    }

    // Dialogs
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Confirm Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(onClick = onLogout) {
                    Text("Yes, Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRoleInfo) {
        AlertDialog(
            onDismissRequest = { showRoleInfo = false },
            title = { Text("Role Information") },
            text = {
                Column {
                    Text("Your current role: ${user.role}")
                    Text(
                        when (user.role) {
                            UserRole.ADMIN -> "As an admin, you have full access to all features."
                            UserRole.TEAM_LEAD -> "As a team lead, you can manage your team members."
                            UserRole.MEMBER -> "As a member, you can participate in team activities."
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoleInfo = false }) {
                    Text("Got it")
                }
            }
        )
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(end = 12.dp)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}