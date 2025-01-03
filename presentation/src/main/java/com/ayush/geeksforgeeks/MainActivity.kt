package com.ayush.geeksforgeeks

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.navigator.Navigator
import com.ayush.geeksforgeeks.auth.AuthScreen
import com.ayush.geeksforgeeks.profile.settings.SettingsScreen
import com.ayush.geeksforgeeks.ui.theme.GFGGGVTheme
import com.ayush.geeksforgeeks.utils.ErrorScreen
import com.ayush.geeksforgeeks.utils.UpdateManager
import com.github.theapache64.fig.Fig
import com.google.android.datatransport.BuildConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL
import javax.inject.Inject

/**
 * Note to other developers:
 * 
 * This project is structured into three modules:
 * 
 * 1. buildSrc - Manages project dependencies using Kotlin DSL
 *    - Centralizes dependency versions
 *    - Makes dependency management type-safe
 * 
 * 2. data - Handles the data layer
 *    - Contains Firebase API calls
 *    - Manages data models and repositories
 *    - Handles data source interactions
 * 
 * 3. presentation - Contains the UI layer
 *    - Implements Jetpack Compose UI
 *    - Contains ViewModels and UI states
 *    - Handles user interactions
 * 
 * Note: Domain layer was intentionally omitted since this is a small, 
 * fast-build application. The business logic is minimal and handled 
 * directly between data and presentation layers.
 */

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var splashScreenProvider: SplashScreenProvider

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("FCM", "Notification permission granted: $isGranted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val splashScreen = splashScreenProvider.provideSplashScreen(this)

        // Add permission check
        checkNotificationPermission()

        // Add permissions
        val permission1 = android.Manifest.permission.INTERNET
        val permission2 = android.Manifest.permission.REQUEST_INSTALL_PACKAGES
        val permission3 = android.Manifest.permission.WRITE_EXTERNAL_STORAGE

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(permission3)
            requestPermissionLauncher.launch(permission1)
            requestPermissionLauncher.launch(permission2)
        } else {
            requestPermissionLauncher.launch(permission3)
            requestPermissionLauncher.launch(permission1)
            requestPermissionLauncher.launch(permission2)
        }

        setContent {
            GFGGGVTheme {
                val viewModel: MainViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(uiState) {
                    if (uiState !is MainViewModel.UiState.Loading) {
                        splashScreen.setKeepOnScreenCondition { false }
                    }
                }
                if (intent?.action == "UPDATE_APP") {
                    val downloadUrl = intent.getStringExtra("downloadUrl")
                    val version = intent.getStringExtra("version")
                    val releaseNotes = intent.getStringExtra("releaseNotes")

                    if (!downloadUrl.isNullOrEmpty()) {
                        // Show your existing update dialog
                        Navigator(screen = SettingsScreen())
                    }
                }
                when (val state = uiState) {
                    MainViewModel.UiState.Loading -> {
                        // The splash screen will be shown
                    }
                    MainViewModel.UiState.NotLoggedIn -> {
                        Navigator(screen = AuthScreen())
                    }
                    is MainViewModel.UiState.LoggedIn -> {
                        Navigator(screen = SettingsScreen())

//                        Navigator(screen = ContainerApp(userRole = state.userRole))
                    }
                    is MainViewModel.UiState.Error -> {
                        ErrorScreen(
                            errorMessage = state.message,
                            contactDetails = listOf(
                                "gfgstudentchapterggv@gmail.com",
                                "7408047420",
                                "8102471811"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            when {
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                    Log.d("FCM", "Notification permission granted")
                }
                else -> {
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

}


class SplashScreenProvider @Inject constructor() {
    fun provideSplashScreen(activity: Activity): SplashScreen {
        return activity.installSplashScreen().apply {
            setKeepOnScreenCondition { true }
        }
    }
}