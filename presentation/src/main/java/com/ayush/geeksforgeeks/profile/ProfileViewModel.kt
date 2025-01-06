package com.ayush.geeksforgeeks.profile

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.data.datastore.UserSettings
import com.ayush.data.model.ContributorData
import com.ayush.data.repository.AuthRepository
import com.ayush.data.repository.QueryRepository
import com.ayush.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.inject.Inject

data class GitHubRelease(
    val tagName: String,
    val htmlUrl: String,
    val name: String
)

data class ReleaseState(
    val isLoading: Boolean = false,
    val release: GitHubRelease? = null,
    val error: String? = null,
    val isOffline: Boolean = false
)

private var lastFetchTime: Long = 0
private val CACHE_DURATION = 1000 * 60 * 60

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val queryRepository: QueryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _releaseState = MutableStateFlow(ReleaseState())
    val releaseState: StateFlow<ReleaseState> = _releaseState

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState

    private val _logoutState = MutableStateFlow<LogoutState>(LogoutState.Idle)
    val logoutState: StateFlow<LogoutState> = _logoutState

    val _contributorsState = MutableStateFlow<ContributorsState>(ContributorsState.Initial)
    val contributorsState: StateFlow<ContributorsState> = _contributorsState

    init {
        loadProfileData()
    }

    private fun loadProfileData() {
        viewModelScope.launch {
            try {
                val user = userRepository.getCurrentUser()
                _uiState.value = ProfileUiState.Success(user)
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun updateProfile(name: String, profilePicUrl: String?) {
        viewModelScope.launch {
            try {
                val currentUser = (uiState.value as? ProfileUiState.Success)?.user ?: return@launch
                val updatedUser = currentUser.copy(
                    name = name,
                    profilePicUrl = profilePicUrl,
                    isLoggedIn = true
                )
                userRepository.updateUser(updatedUser)
                _uiState.value = ProfileUiState.Success(updatedUser)
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Failed to update profile")
            }
        }
    }

    fun uploadProfilePicture(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value = ProfileUiState.Loading

                val imageUrl = userRepository.uploadProfileImage(uri)

                // Update UI state with the current user data
                _uiState.value = ProfileUiState.Success(userRepository.getCurrentUser())
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Failed to upload profile picture")
            }
        }
    }

    private val _queryState = MutableStateFlow<QueryState>(QueryState.Idle)
    val queryState: StateFlow<QueryState> = _queryState

    fun submitQuery(name: String, email: String, query: String) {
        viewModelScope.launch {
            _queryState.value = QueryState.Loading
            val result = queryRepository.submitQuery(name, email, query)
            _queryState.value = if (result) QueryState.Success else QueryState.Error("Failed to submit query")
        }
    }

    fun logOut() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                authRepository.logout()
                _logoutState.value = LogoutState.Success
            } catch (e: Exception) {
                _logoutState.value = LogoutState.Error(e.message ?: "Logout failed")
            }
        }
    }

    fun fetchLatestRelease() {
        val currentTime = System.currentTimeMillis()
        if (releaseState.value.release != null &&
            currentTime - lastFetchTime < CACHE_DURATION) {
            return
        }

        viewModelScope.launch {
            _releaseState.value = ReleaseState(isLoading = true)
            try {
                // First check for internet connectivity
                if (!isNetworkAvailable()) {
                    _releaseState.value = ReleaseState(
                        isOffline = true,
                        release = GitHubRelease(
                            tagName = "latest",
                            htmlUrl = "https://github.com/Ayush-Kotlin-Dev/GFG_GGV/releases/",
                            name = "latest"
                        )
                    )
                    return@launch
                }

                Log.d("ProfileViewModel", "Fetching latest release...")
                val response = withContext(Dispatchers.IO) {
                    val connection = URL("https://api.github.com/repos/Ayush-Kotlin-Dev/GFG_GGV/releases/latest")
                        .openConnection()
                        .apply {
                            setRequestProperty("Accept", "application/vnd.github.v3+json")
                            setRequestProperty("User-Agent", "GFG-App")
                            connectTimeout = 5000
                            readTimeout = 5000
                        }

                    try {
                        connection.getInputStream().bufferedReader().use { it.readText() }
                    } catch (e: Exception) {
                        Log.e("ProfileViewModel", "Network error: ${e.message}")
                        null
                    }
                }

                if (response != null) {
                    try {
                        val jsonObject = JSONObject(response)
                        Log.d("ProfileViewModel", "Response: $response")

                        val release = GitHubRelease(
                            tagName = jsonObject.getString("tag_name"),
                            htmlUrl = jsonObject.getString("html_url"),
                            name = jsonObject.getString("name")
                        )
                        Log.d("ProfileViewModel", "Parsed release: $release")
                        _releaseState.value = ReleaseState(release = release)
                    } catch (e: Exception) {
                        Log.e("ProfileViewModel", "JSON parsing error: ${e.message}")
                        _releaseState.value = ReleaseState(error = e.message)
                    }
                } else {
                    _releaseState.value = ReleaseState(error = "Failed to fetch release")
                }
                lastFetchTime = currentTime

            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error fetching release: ${e.message}", e)
                // Set offline state if it's a network error
                if (e is java.net.UnknownHostException || e is java.net.ConnectException) {
                    _releaseState.value = ReleaseState(
                        isOffline = true,
                        release = GitHubRelease(
                            tagName = "latest",
                            htmlUrl = "https://github.com/Ayush-Kotlin-Dev/GFG_GGV/releases/",
                            name = "latest"
                        )
                    )
                } else {
                    _releaseState.value = ReleaseState(error = e.message)
                }
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && (
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            )
    }

    fun loadContributors() {
        viewModelScope.launch {
            _contributorsState.value = ContributorsState.Loading
            try {
                val contributors = userRepository.getContributors()
                _contributorsState.value = ContributorsState.Success(contributors)
            } catch (e: Exception) {
                _contributorsState.value = ContributorsState.Error(e.message ?: "Failed to load contributors")
            }
        }
    }

    sealed class ContributorsState {
        object Initial : ContributorsState()
        object Loading : ContributorsState()
        data class Success(val contributors: List<ContributorData>) : ContributorsState()
        data class Error(val message: String) : ContributorsState()
    }

    sealed class ProfileUiState {
        object Loading : ProfileUiState()
        data class Success(val user: UserSettings) : ProfileUiState()
        data class Error(val message: String) : ProfileUiState()
    }

    sealed class QueryState {
        object Idle : QueryState()
        object Loading : QueryState()
        object Success : QueryState()
        data class Error(val message: String) : QueryState()
    }

    sealed class LogoutState {
        object Idle : LogoutState()
        object Loading : LogoutState()
        object Success : LogoutState()
        data class Error(val message: String) : LogoutState()
    }
}