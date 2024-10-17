package com.ayush.geeksforgeeks.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import coil.compose.rememberAsyncImagePainter
import com.ayush.data.datastore.UserSettings
import com.ayush.geeksforgeeks.auth.AuthScreen
import com.ayush.geeksforgeeks.profile.ProfileViewModel.ProfileUiState

class ProfileScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel: ProfileViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()
        val navigator = LocalNavigator.current
        //
        when (val state = uiState) {
            is ProfileUiState.Loading -> LoadingIndicator()
            is ProfileUiState.Success -> ProfileContent(
                state.user, viewModel::updateProfile
            ) {
                viewModel.logOut(); navigator?.push(AuthScreen())
            }

            is ProfileUiState.Error -> ErrorMessage(state.message)
        }
    }
}

@Composable
fun LoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun ProfileContent(
    user: UserSettings,
    onUpdateProfile: (String, String?) -> Unit,
    onLogout: () -> Unit
) {
    var name by remember { mutableStateOf(user.name) }
    var isEditing by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Profile", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (isEditing) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") }
            )
        } else {
            Text(text = "Name: ${user.name}", style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Email: ${user.email}", style = MaterialTheme.typography.bodyLarge)
        Text(text = "Role: ${user.role}", style = MaterialTheme.typography.bodyLarge)
        Text(text = "Domain: ${user.domainId}", style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "Total Credits: ${user.totalCredits}",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (isEditing) {
                    onUpdateProfile(name, user.profilePicUrl)
                }
                isEditing = !isEditing
            }
        ) {
            Text(if (isEditing) "Save" else "Edit Profile")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (user.profilePicUrl != null) {
            Image(
                painter = rememberAsyncImagePainter(user.profilePicUrl),
                contentDescription = "Profile Picture",
                modifier = Modifier.size(100.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onLogout() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }

    }

}

@Composable
fun ErrorMessage(message: String) {
    Text(text = "Error: $message", color = MaterialTheme.colorScheme.error)
}