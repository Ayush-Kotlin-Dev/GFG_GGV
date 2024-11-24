package com.ayush.geeksforgeeks

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var splashScreenProvider: SplashScreenProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val splashScreen = splashScreenProvider.provideSplashScreen(this)

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
}

class SplashScreenProvider @Inject constructor() {
    fun provideSplashScreen(activity: Activity): SplashScreen {
        return activity.installSplashScreen().apply {
            setKeepOnScreenCondition { true }
        }
    }
}

