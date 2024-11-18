package com.ayush.geeksforgeeks

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.navigator.Navigator
import com.ayush.data.datastore.UserRole
import com.ayush.geeksforgeeks.auth.AuthScreen
import com.ayush.geeksforgeeks.auth.FirebaseDataPopulator
import com.ayush.geeksforgeeks.ui.theme.GFGGGVTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var firebaseDataPopulator: FirebaseDataPopulator

    @SuppressLint("CoroutineCreationDuringComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Handle the splash screen transition
        splashScreen.setKeepOnScreenCondition { true }


        setContent {
            GFGGGVTheme {
                val viewModel: MainActivityViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsState()
//                lifecycleScope.launch {
//                    firebaseDataPopulator.populateTeamsAndMembers()
//                }
                // This will be called when the content is ready
                LaunchedEffect(Unit) {
                    splashScreen.setKeepOnScreenCondition { false }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (val state = uiState) {
                        MainActivityViewModel.UiState.Loading -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                        MainActivityViewModel.UiState.NotLoggedIn -> {
                            Navigator(screen = AuthScreen())
                        }
                        is MainActivityViewModel.UiState.LoggedInAsAdmin -> {
                            Navigator(screen = ContainerApp(userRole = UserRole.TEAM_LEAD))
                        }
                        is MainActivityViewModel.UiState.LoggedInAsMember -> {
                            Navigator(screen = ContainerApp(userRole = UserRole.MEMBER))
                        }
                    }
                }
            }
        }
    }
}