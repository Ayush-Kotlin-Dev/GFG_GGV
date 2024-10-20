package com.ayush.geeksforgeeks.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.ayush.data.datastore.UserSettings
import com.ayush.geeksforgeeks.R
import com.ayush.geeksforgeeks.auth.AuthScreen
import com.ayush.geeksforgeeks.dashboard.ErrorMessage
import com.ayush.geeksforgeeks.dashboard.LoadingIndicator
import com.ayush.geeksforgeeks.ui.theme.GFGBackground
import com.ayush.geeksforgeeks.ui.theme.GFGPrimary
import com.ayush.geeksforgeeks.ui.theme.GFGTextPrimary

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
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GFGBackground)
    ) {
        // Profile Header
        ProfileHeader(user)

        // Menu Items
        ProfileMenuItem(Icons.Default.Person, "Profile")
        ProfileMenuItem(Icons.Default.Settings, "Setting")
        ProfileMenuItem(Icons.Default.Email, "Contact")
        ProfileMenuItem(Icons.Default.Share, "Share App")
        ProfileMenuItem(Icons.Default.Info, "Help")
        ProfileMenuItem(Icons.Rounded.FavoriteBorder, "About Us ")

        Spacer(modifier = Modifier.weight(1f))

        // Sign Out Button
        TextButton(
            onClick = { showLogoutDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Sign Out",
                color = GFGPrimary,
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
                .clip(CircleShape),
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
fun ProfileMenuItem(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GFGPrimary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = GFGTextPrimary
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = GFGTextPrimary.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

