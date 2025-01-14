// SettingsScreen.kt
package com.ayush.geeksforgeeks.profile.settings

import android.app.DownloadManager
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.ayush.geeksforgeeks.auth.components.ForgotPasswordDialog
import com.ayush.geeksforgeeks.ui.theme.GFGPrimary
import com.ayush.geeksforgeeks.utils.UpdateManager

class SettingsScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel: SettingsViewModel = hiltViewModel()
        val state by viewModel.state.collectAsState()

        SettingsContent(
            state = state,
            onEvent = viewModel::onEvent,
            viewModel = viewModel
        )
    }
}


@Composable
fun SettingsContent(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    viewModel: SettingsViewModel
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val showResetDialog by viewModel.showResetDialog.collectAsState()
    val resetPasswordState by viewModel.resetPasswordState.collectAsState()
    val updateDialogState by viewModel.showUpdateDialog.collectAsState()
    val appInfoDialog = remember { mutableStateOf(false) }
    val navigation = LocalNavigator.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .animateContentSize()
    ) {
        // Header with animation
        Header(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .graphicsLayer {
                    alpha = 1f - (scrollState.value / 600f).coerceIn(0f, 1f)
                    translationY = -scrollState.value * 0.5f
                },
            onBackPressed = { navigation?.pop() }
        )

        // App Preferences Section
        SettingsSection(title = "App Preferences") {
            SettingItem(
                title = "Dark Mode",
                subtitle = "Toggle dark/light theme",
                icon = Icons.Default.DarkMode,
                isToggleable = true,
                checked = state.isDarkMode,
                onToggle = { onEvent(SettingsEvent.ToggleDarkMode(it)) }
            )
            SettingItem(
                title = "Notifications",
                subtitle = "Manage push notifications",
                icon = Icons.Default.Notifications,
                isToggleable = true,
                checked = state.isNotificationsEnabled,
                onToggle = { onEvent(SettingsEvent.ToggleNotifications(it)) }
            )
            SettingItem(
                title = "Language",
                subtitle = "Choose app language",
                icon = Icons.Default.Language,
                onClick = { onEvent(SettingsEvent.OpenLanguageSettings) }
            )
        }

        // Account Settings Section
        SettingsSection(title = "Account Settings") {
            SettingItem(
                title = "Change Password",
                subtitle = "Update your account password",
                icon = Icons.Default.Lock,
                onClick = { viewModel.handleChangePassword() }
            )
            SettingItem(
                title = "Privacy Settings",
                subtitle = "Manage your data and privacy",
                icon = Icons.Default.Security,
                onClick = { onEvent(SettingsEvent.OpenPrivacySettings) }
            )
        }

        // Content Preferences Section
        SettingsSection(title = "Content Preferences") {
            SettingItem(
                title = "Event Reminders",
                subtitle = "Get notifications before events",
                icon = Icons.Default.Event,
                isToggleable = true,
                checked = state.isEventRemindersEnabled,
                onToggle = { onEvent(SettingsEvent.ToggleEventReminders(it)) }
            )
            SettingItem(
                title = "Download Settings",
                subtitle = "Manage offline content",
                icon = Icons.Default.Download,
                onClick = { onEvent(SettingsEvent.OpenDownloadSettings) }
            )
        }

        // App Information Section
        SettingsSection(title = "App Information") {
            SettingItem(
                title = "App Version",
                subtitle = "Version ${state.appVersion}",
                icon = Icons.Default.Info,
                onClick = null
            )
            SettingItem(
                title = "Check for Updates",
                subtitle = if (state.isLoading) "Checking for updates..."
                else "Current version: ${state.appVersion}",
                icon = Icons.Default.Download,
                onClick = if (!state.isLoading) {
                    { viewModel.checkForUpdates(context) }
                } else null
            )
            SettingItem(
                title = "Installation Guide",
                subtitle = "Having trouble updating? Tap here for instructions",
                icon = Icons.Default.Info,
                onClick = {
                    // Show installation guide dialog
                    appInfoDialog.value = true
                }
            )
            SettingItem(
                title = "Terms of Service",
                subtitle = "Read our terms and conditions",
                icon = Icons.Default.Description,
                onClick = { onEvent(SettingsEvent.OpenTermsOfService) }
            )
            SettingItem(
                title = "Privacy Policy",
                subtitle = "Read our privacy policy",
                icon = Icons.Default.PrivacyTip,
                onClick = { onEvent(SettingsEvent.OpenPrivacyPolicy) }
            )
        }
    }

    if (showResetDialog) {
        ForgotPasswordDialog(
            onDismiss = viewModel::hideResetDialog,
            onSubmit = viewModel::resetPassword,
            resetPasswordState = resetPasswordState
        )
    }

    updateDialogState?.let { (downloadUrl, release) ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdateDialog() },
            title = {
                Text(
                    text = "🚀 Update Available",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column {
                    Text(
                        buildString {
                            append("📱 A new version (${release.tag_name}) is available")
                            append("\n\n")

                            if (!release.name.isNullOrBlank()) {
                                append("🎉 ${release.name}")
                                append("\n\n")
                            }

                            if (!release.body.isNullOrBlank()) {
                                append("✨ What's New:\n")
                                append("━━━━━━━━━━━━━━━━\n")
                                append(release.body.trim())
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        UpdateManager(context).downloadAndInstallUpdate(downloadUrl)
                        viewModel.dismissUpdateDialog()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("📥 Update Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                    Text("⏳ Later")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shape = MaterialTheme.shapes.extraLarge
        )
    }

    if (appInfoDialog.value) {
        AlertDialog(
            onDismissRequest = {
                appInfoDialog.value = false
            },
            title = {
                Text(
                    text = "📱 How to Install Updates",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    buildString {
                        append("First check for updates:\n\n")
                        append("1. Click on 'Check for Updates' option\n")
                        append("2. If update available, click on 'Update Now'\n")
                        append("3. After clicking 'Update Now', the APK will start downloading\n")
                        append("4. Once downloaded, you should get a prompt to install the update\n\n")
                        append("If automatic installation doesn't work after downloading:\n\n")
                        append("1. Open your device's Downloads folder\n")
                        append("2. Look for 'GFG-app-update.apk'\n")
                        append("3. Tap on the file\n")
                        append("4. If prompted, allow installation from this source\n")
                        append("5. Tap 'Install' or 'Update'\n\n")
                        append("Note: You might need to grant permission to install apps from unknown sources.")
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    // Open downloads folder
                    val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                }) {
                    Text("📂 Open Downloads")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    appInfoDialog.value = false
                }) {
                    Text("Got it")
                }
            },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        )
    }

}

@Composable
private fun Header(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit

) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = true,
            enter = slideInHorizontally() + fadeIn(),
            exit = slideOutHorizontally() + fadeOut()
        ) {
            IconButton(
                onClick = { onBackPressed.invoke() },
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
        }

        // Section Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            content()
        }
    }
}


@Composable
private fun SettingItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isToggleable: Boolean = false,
    checked: Boolean = false,
    onToggle: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = onClick != null,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick ?: {}
            ),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with background
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isToggleable) {
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = checked,
                    onCheckedChange = { onToggle?.invoke(it) },
                    colors = SwitchDefaults.colors(
                        // Thumb (the moving circle)
                        checkedThumbColor = Color.White,  // White circle when ON
                        uncheckedThumbColor = Color.White,  // White circle when OFF

                        // Track (the background)
                        checkedTrackColor = GFGPrimary,  // Green background when ON
                        uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f),  // Light gray when OFF

                        // Border
                        uncheckedBorderColor = Color.Gray.copy(alpha = 0.5f),  // Gray border when OFF
                        checkedBorderColor = GFGPrimary  // Green border when ON
                    )
                )
            }
        }
    }

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    )
}