package com.ayush.geeksforgeeks.profile

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import com.ayush.data.datastore.UserSettings
import com.ayush.geeksforgeeks.profile.ProfileViewModel.ProfileUiState

class ProfileScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel: ProfileViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()

        when (val state = uiState) {
            is ProfileUiState.Loading -> LoadingIndicator()
            is ProfileUiState.Success -> ProfileContent(state.user, viewModel::updateProfile)
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
fun ProfileContent(user: UserSettings, onUpdateProfile: (String, String?) -> Unit) {
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
        Text(text = "Total Credits: ${user.totalCredits}", style = MaterialTheme.typography.bodyLarge)

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
    }
}

@Composable
fun ErrorMessage(message: String) {
    Text(text = "Error: $message", color = MaterialTheme.colorScheme.error)
}