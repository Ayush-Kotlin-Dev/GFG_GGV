package com.ayush.geeksforgeeks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.navigator.Navigator
import com.ayush.data.datastore.UserRole
import com.ayush.geeksforgeeks.auth.AuthScreen
import com.ayush.geeksforgeeks.ui.theme.GFGGGVTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GFGGGVTheme {
                val viewModel: MainActivityViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsState()

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