package com.ayush.geeksforgeeks

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import com.ayush.geeksforgeeks.auth.AuthScreen
import com.ayush.geeksforgeeks.profile.settings.SettingsScreen
import com.ayush.geeksforgeeks.ui.theme.GFGGGVTheme
import com.ayush.geeksforgeeks.utils.CrashlyticsTestActivity
import com.ayush.geeksforgeeks.utils.ErrorScreen
import com.ayush.geeksforgeeks.utils.LoadingIndicator
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
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach { entry ->
            Log.d("FCM", "${entry.key} permission granted: ${entry.value}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val splashScreen = splashScreenProvider.provideSplashScreen(this)
        
        requestRequiredPermissions()
        
        val specialRoute = handleSpecialIntents()

        setContent {
            GFGGGVTheme {
                val viewModel: MainViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsState()

                // Remove splash screen when we have content to show
                LaunchedEffect(uiState) {
                    if (uiState !is MainViewModel.UiState.Loading) {
                        splashScreen.setKeepOnScreenCondition { false }
                    }
                }

                // Use special route if available, otherwise follow normal flow
                when {
                    specialRoute != null -> {
                        Navigator(screen = specialRoute)
                    }
                    uiState is MainViewModel.UiState.Loading -> {
                        // The splash screen will be shown
                    }
                    uiState is MainViewModel.UiState.NotLoggedIn -> {
                        Navigator(screen = AuthScreen())
                    }
                    uiState is MainViewModel.UiState.LoggedIn -> {
                        val userRole = (uiState as MainViewModel.UiState.LoggedIn).userRole
                        Box {
                            Navigator(screen = ContainerApp(userRole = userRole))
                            
                            // Debug FAB for testing Crashlytics in debug builds
                            if (BuildConfig.DEBUG) {
                                DebugCrashlyticsFAB()
                            }
                        }
                    }
                    uiState is MainViewModel.UiState.Error -> {
                        val errorMessage = (uiState as MainViewModel.UiState.Error).message
                        ErrorScreen(
                            errorMessage = errorMessage,
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
    
    @Composable
    private fun DebugCrashlyticsFAB() {
        val context = LocalContext.current
        
        Box(
            modifier = androidx.compose.ui.Modifier.padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = {
                    Toast.makeText(context, "Opening Crashlytics Test Screen", Toast.LENGTH_SHORT).show()
                    val intent = Intent(context, CrashlyticsTestActivity::class.java)
                    context.startActivity(intent)
                }
            ) {
                Icon(
                    painter = painterResource(id = androidx.core.R.drawable.ic_call_answer), 
                    contentDescription = "Test Crashlytics"
                )
            }
        }
    }
    
    /**
     * Handle special intents like app updates and return the appropriate screen if needed
     */
    private fun handleSpecialIntents(): Screen? {
        if (intent?.action == "UPDATE_APP") {
            val downloadUrl = intent.getStringExtra("downloadUrl")
            if (!downloadUrl.isNullOrEmpty()) {
                return SettingsScreen()
            }
        }
        return null
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.REQUEST_INSTALL_PACKAGES,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val permissionsToRequest = permissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}

/**
 * Provider class for splash screen instance
 */
class SplashScreenProvider @Inject constructor() {
    fun provideSplashScreen(activity: Activity): SplashScreen {
        return activity.installSplashScreen().apply {
            setKeepOnScreenCondition { true }
        }
    }
}