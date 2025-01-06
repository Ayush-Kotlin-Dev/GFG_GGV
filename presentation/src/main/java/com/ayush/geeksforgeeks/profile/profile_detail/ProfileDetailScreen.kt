package com.ayush.geeksforgeeks.profile.profile_detail

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.util.Log
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.ayush.geeksforgeeks.ui.theme.GFGPrimary
import com.ayush.geeksforgeeks.utils.ErrorScreen
import com.ayush.geeksforgeeks.utils.LoadingIndicator
import com.ayush.geeksforgeeks.utils.PaletteGenerator
import com.ayush.geeksforgeeks.utils.PulseAnimation
import com.ayush.geeksforgeeks.utils.parserColor
import androidx.compose.ui.graphics.Color as ColorUi

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
    var profileImageColors by remember {
        mutableStateOf(
            mapOf(
                "vibrant" to "#2E8B57",
                "darkVibrant" to "#1A5D3A",
                "onDarkVibrant" to "#FFFFFF"
            )
        )
    }
    var isDynamicColorEnabled by remember { mutableStateOf(true) }

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

    rememberDominantColorState(
        context = LocalContext.current,
        imageUrl = user.profilePicUrl,
        defaultColor = ColorUi(0xFF2E8B57),
        isDynamicColorEnabled = isDynamicColorEnabled
    ) { colors ->
        profileImageColors = colors
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())

            .background(GFGBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            ColorUi(parserColor(profileImageColors["vibrant"] ?: "#2E8B57")),
                            ColorUi(parserColor(profileImageColors["darkVibrant"] ?: "#1A5D3A")),
                            ColorUi(parserColor(profileImageColors["darkMuted"] ?: "#2C3E50"))
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp , start = 16.dp),
            ) {
                IconButton(
                    onClick = { navigation?.pop() },
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ColorUi.White
                    )
                }
            }
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
                            .border(2.dp, ColorUi.White, CircleShape)
                    ) {
                        var isLoading by remember { mutableStateOf(true) }
                        AsyncImage(
                            model = user.profilePicUrl ?: R.drawable.geeksforgeeks_logo,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .drawBehind {
                                    drawCircle(
                                        color = ColorUi.Black,
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

                    IconButton(
                        onClick = { showImagePicker = true },
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                            .background(ColorUi.White, CircleShape)
                            .border(1.dp, ColorUi(0xFFE0E0E0), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
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
                    color = ColorUi.White,
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
                profileImageColors = profileImageColors,
                content = {
                    ProfileField(
                        icon = Icons.Filled.Person,
                        label = "Name",
                        value = user.name,
                        profileImageColors = profileImageColors
                    )
                    ProfileField(
                        icon = Icons.Filled.Email,
                        label = "Email",
                        value = user.email,
                        profileImageColors = profileImageColors
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProfileSection(
                title = "Role & Credits",
                profileImageColors = profileImageColors,
                content = {
                    ProfileField(
                        icon = Icons.Filled.Badge,
                        label = "Role",
                        value = user.role.name,
                        profileImageColors = profileImageColors
                    )
                    ProfileField(
                        icon = Icons.Filled.Domain,
                        label = "Team",
                        value =  user.domainId.toString(),
                        profileImageColors = profileImageColors
                    )
                    ProfileField(
                        icon = Icons.Filled.Star,
                        label = "Total Credits",
                        value = "${user.totalCredits} points",
                        profileImageColors = profileImageColors
                    )
                }
            )

            // Add achievements section if credits > 0
            if (user.totalCredits > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                ProfileSection(
                    title = "Achievements",
                    profileImageColors = profileImageColors,
                    content = {
                        AchievementItem(
                            icon = Icons.Filled.EmojiEvents,
                            title = "Active Contributor",
                            description = "Earned ${user.totalCredits} credits",
                            profileImageColors = profileImageColors
                        )
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dynamic Colors",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enable profile-based color theming",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isDynamicColorEnabled,
                    onCheckedChange = { isEnabled ->
                        isDynamicColorEnabled = isEnabled
                        if (!isEnabled) {
                            // Reset to default colors when disabled
                            profileImageColors = mapOf(
                                "vibrant" to "#2E8B57",
                                "darkVibrant" to "#1A5D3A",
                                "onDarkVibrant" to "#FFFFFF"
                            )
                        }
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

private fun calculateTextColor(backgroundColor: String): String {
    val color = Color.parseColor(backgroundColor)
    // Extract RGB values
    val red = Color.red(color)
    val green = Color.green(color)
    val blue = Color.blue(color)

    // Calculate relative luminance using the formula from WCAG 2.0
    val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255

    // Use black text for light backgrounds and white text for dark backgrounds
    return if (luminance > 0.5) "#000000" else "#FFFFFF"
}

@Composable
private fun ProfileSection(
    title: String,
    profileImageColors: Map<String, String>,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = ColorUi(parserColor(profileImageColors["vibrant"] ?: "#2E8B57")),
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
    value: String,
    profileImageColors: Map<String, String>
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
            tint = ColorUi(parserColor(profileImageColors["vibrant"] ?: "#2E8B57")),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AchievementItem(
    icon: ImageVector,
    title: String,
    description: String,
    profileImageColors: Map<String, String>
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
            tint = ColorUi(0xFFFFD700),
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun rememberDominantColorState(
    context: Context,
    imageUrl: String?,
    defaultColor: ColorUi,
    isDynamicColorEnabled: Boolean,
    onColorCalculated: (Map<String, String>) -> Unit = {}
) {
    LaunchedEffect(key1 = imageUrl, key2 = isDynamicColorEnabled) {
        if (imageUrl != null && isDynamicColorEnabled) {
            try {
                val bitmap = PaletteGenerator.convertImageUrlToBitmap(imageUrl, context)
                bitmap?.let {
                    val colors = PaletteGenerator.extractColorsFromBitmap(it)
                    onColorCalculated(colors)
                }
            } catch (e: Exception) {
                Log.e("ProfileDetail", "Error extracting colors: ${e.message}")
                onColorCalculated(
                    mapOf(
                        "vibrant" to "#2E8B57",
                        "darkVibrant" to "#1A5D3A",
                        "onDarkVibrant" to "#FFFFFF"
                    )
                )
            }
        } else {
            onColorCalculated(
                mapOf(
                    "vibrant" to "#2E8B57",
                    "darkVibrant" to "#1A5D3A",
                    "onDarkVibrant" to "#FFFFFF"
                )
            )
        }
    }
}