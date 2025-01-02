package com.ayush.geeksforgeeks

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.navigator.Navigator
import com.ayush.geeksforgeeks.auth.AuthScreen
import com.ayush.geeksforgeeks.ui.theme.GFGGGVTheme
import com.ayush.geeksforgeeks.utils.ErrorScreen
import dagger.hilt.android.AndroidEntryPoint
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

        setContent {
            GFGGGVTheme {
                val viewModel: MainViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(uiState) {
                    if (uiState !is MainViewModel.UiState.Loading) {
                        splashScreen.setKeepOnScreenCondition { false }
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
                        Navigator(screen = ContainerApp(userRole = state.userRole))
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