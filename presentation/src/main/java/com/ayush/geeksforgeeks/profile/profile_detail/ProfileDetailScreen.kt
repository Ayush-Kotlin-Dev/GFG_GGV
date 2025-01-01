package com.ayush.geeksforgeeks.profile.profile_detail

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import coil.compose.AsyncImage
import com.ayush.data.datastore.UserSettings
import com.ayush.geeksforgeeks.R
import com.ayush.geeksforgeeks.profile.ProfileViewModel
import com.ayush.geeksforgeeks.profile.ProfileViewModel.ProfileUiState
import com.ayush.geeksforgeeks.ui.theme.GFGBackground
import com.ayush.geeksforgeeks.ui.theme.GFGCardBackground
import com.ayush.geeksforgeeks.ui.theme.GFGPrimary
import com.ayush.geeksforgeeks.ui.theme.GFGTextPrimary
import com.ayush.geeksforgeeks.utils.DomainUtils
import com.ayush.geeksforgeeks.utils.ErrorScreen
import com.ayush.geeksforgeeks.utils.LoadingIndicator

class ProfileDetailScreen : Screen {
    @Composable
    override fun Content() {
        ProfileDetail()
    }
}

@Composable
fun ProfileDetail(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsState()

    when (val state = uiState.value) {
        is ProfileUiState.Success -> EditableProfileContent(
            user = state.user,
            viewModel = viewModel
        )

        is ProfileUiState.Loading -> LoadingIndicator()
        is ProfileUiState.Error -> ErrorScreen(errorMessage = state.message)
    }
}

@Composable
fun EditableProfileContent(
    user: UserSettings,
    viewModel: ProfileViewModel
) {
    var showImagePicker by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            showConfirmDialog = true
        }
    }
    val uiState by viewModel.uiState.collectAsState()
    val navigation = LocalNavigator.current

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is ProfileUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                isUploading = false
            }

            is ProfileUiState.Success -> {
                if (isUploading) {
                    Toast.makeText(
                        context,
                        "Profile picture updated successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    isUploading = false
                }
            }

            else -> {}
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        IconButton(
            onClick = { navigation?.pop() },
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GFGBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
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
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                    ) {
                        AsyncImage(
                            model = user.profilePicUrl ?: R.drawable.geeksforgeeks_logo,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    IconButton(
                        onClick = { showImagePicker = true },
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                            .background(Color.White, CircleShape)
                            .border(1.dp, Color(0xFFE0E0E0), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile Picture",
                            tint = GFGPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Profile Details Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            ProfileSection(
                title = "Personal Information",
                content = {
                    ProfileField(
                        icon = Icons.Default.Person,
                        label = "Name",
                        value = user.name
                    )
                    ProfileField(
                        icon = Icons.Default.Email,
                        label = "Email",
                        value = user.email
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProfileSection(
                title = "Role & Credits",
                content = {
                    ProfileField(
                        icon = Icons.Default.Badge,
                        label = "Role",
                        value = user.role.name
                    )
                    ProfileField(
                        icon = Icons.Default.Domain,
                        label = "Team",
                        value = DomainUtils.getDomainName(user.domainId)
                    )
                    ProfileField(
                        icon = Icons.Default.Star,
                        label = "Total Credits",
                        value = "${user.totalCredits} points"
                    )
                }
            )

            // Add achievements section if credits > 0
            if (user.totalCredits > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                ProfileSection(
                    title = "Achievements",
                    content = {
                        AchievementItem(
                            icon = Icons.Default.EmojiEvents,
                            title = "Active Contributor",
                            description = "Earned ${user.totalCredits} credits"
                        )
                    }
                )
            }
        }
    }

    // Image Picker and Dialog code remains the same
    if (showImagePicker) {
        launcher.launch("image/*")
        showImagePicker = false
    }

    if (showConfirmDialog && selectedImageUri != null) {
        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                selectedImageUri = null
            },
            title = { Text("Confirm Changes") },
            text = { Text("Do you want to update your profile picture?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedImageUri?.let { uri ->
                            isUploading = true
                            viewModel.uploadProfilePicture(uri)
                        }
                        showConfirmDialog = false
                        selectedImageUri = null
                    },
                    enabled = !isUploading
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = GFGPrimary
                        )
                    } else {
                        Text("Confirm", color = GFGPrimary)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        selectedImageUri = null
                    },
                    enabled = !isUploading
                ) {
                    Text("Cancel", color = GFGPrimary)
                }
            }
        )
    }
}

@Composable
private fun ProfileSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = GFGCardBackground,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = GFGPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ProfileField(
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
            tint = GFGPrimary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = GFGTextPrimary
            )
        }
    }
}

@Composable
private fun AchievementItem(
    icon: ImageVector,
    title: String,
    description: String
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
            tint = Color(0xFFFFD700),
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}