package com.ayush.geeksforgeeks.auth

import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayush.data.datastore.UserRole
import com.ayush.data.repository.AuthRepository
import com.ayush.data.repository.AuthState
import com.ayush.data.repository.EmailVerificationResult
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    private var currentAuthJob: Job? = null

    var email by mutableStateOf(savedStateHandle.get<String>("email") ?: "")
        private set
    var password by mutableStateOf(savedStateHandle.get<String>("password") ?: "")
        private set

    private val _teams = MutableStateFlow<List<AuthRepository.Team>>(emptyList())
    val teams = _teams.asStateFlow()

    private val _teamMembers = MutableStateFlow<List<AuthRepository.TeamMember>>(emptyList())
    val teamMembers = _teamMembers.asStateFlow()

    var selectedTeam by mutableStateOf<AuthRepository.Team?>(null)
        private set
    var selectedMember by mutableStateOf<AuthRepository.TeamMember?>(null)
        private set

    init {
        viewModelScope.launch {
            savedStateHandle.setSavedStateProvider("email") { Bundle().apply { putString("email", email) } }
            savedStateHandle.setSavedStateProvider("password") { Bundle().apply { putString("password", password) } }
            loadTeams()
        }
    }

    fun updateEmail(newEmail: String) {
        email = newEmail
        savedStateHandle["email"] = newEmail
    }

    fun updatePassword(newPassword: String) {
        password = newPassword
        savedStateHandle["password"] = newPassword
    }

    fun selectTeam(team: AuthRepository.Team) {
        selectedTeam = team
        selectedMember = null
        updateEmail("")
        viewModelScope.launch {
            loadTeamMembers(team.id)
        }
    }

    fun selectMember(member: AuthRepository.TeamMember) {
        selectedMember = member
        updateEmail(member.email)
    }

    private val _resetPasswordState = MutableStateFlow<ResetPasswordState>(ResetPasswordState.Idle)
    val resetPasswordState: StateFlow<ResetPasswordState> = _resetPasswordState

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _resetPasswordState.value = ResetPasswordState.Loading
            try {
                authRepository.sendPasswordResetEmail(email)
                _resetPasswordState.value = ResetPasswordState.Success
            } catch (e: Exception) {
                _resetPasswordState.value = ResetPasswordState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    private fun loadTeams() {
        viewModelScope.launch {
            try {
                _teams.value = authRepository.getTeams()
            } catch (e: Exception) {
                // Handle error, maybe set an error state
            }
        }
    }

    private fun loadTeamMembers(teamId: String) {
        viewModelScope.launch {
            try {
                val members = authRepository.getTeamMembers(teamId)
                _teamMembers.value = members.sortedBy { it.role != UserRole.TEAM_LEAD }
            } catch (e: Exception) {
                // Handle error, maybe set an error state
            }
        }
    }

    fun signUp() {
        currentAuthJob?.cancel()
        currentAuthJob = viewModelScope.launch {
            _authState.value = AuthState.Loading

            val result = authRepository.signUp(
                teamId = selectedTeam?.id ?: "",
                email = email,
                password = password
            )

            result.fold(
                onSuccess = {
                    sendEmailVerification()
                    _authState.value = AuthState.RegistrationSuccess
                },
                onFailure = {
                    _authState.value = AuthState.Error(it.message ?: "Sign up failed", it)
                }
            )
        }
    }


    fun login() {
        currentAuthJob?.cancel()
        currentAuthJob = viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.login(email, password)
            result.fold(
                onSuccess = { user ->
                    if (user.isEmailVerified) {
                        _authState.value = AuthState.Success(user)
                    } else {
                        _authState.value = AuthState.EmailVerificationRequired
                    }
                },
                onFailure = { e ->
                    _authState.value = AuthState.Error(e.message ?: "Login failed", e)
                }
            )
        }
    }

    private fun sendEmailVerification() {
        viewModelScope.launch {
            try {
                authRepository.sendEmailVerification()
                _authState.value = AuthState.EmailVerificationSent
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Failed to send verification email: ${e.message}", e)
            }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            try {
                authRepository.sendEmailVerification()
                _authState.value = AuthState.EmailVerificationSent
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Failed to send verification email: ${e.message}", e)
            }
        }
    }

    private fun clearFormData() {
        email = ""
        password = ""
        selectedTeam = null
        selectedMember = null
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    suspend fun getUserRoleOnLogin(): UserRole =
        authRepository.getUserRole(email)

    private var verificationCheckJob: Job? = null


    fun stopVerificationCheck() {
        verificationCheckJob?.cancel()
    }


    override fun onCleared() {
        super.onCleared()
        stopVerificationCheck()
    }
}

sealed class ResetPasswordState {
    object Idle : ResetPasswordState()
    object Loading : ResetPasswordState()
    object Success : ResetPasswordState()
    data class Error(val message: String) : ResetPasswordState()
}


class FirebaseDataPopulator @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun populateTeamsAndMembers() = withContext(Dispatchers.IO) {
        val teamsWithMembers = mapOf(
            Team(1, "Web Development") to listOf(
                TeamMember("ABHINAV SINGH", "sabhinav797@gmail.com", UserRole.TEAM_LEAD),
                TeamMember("DEEPAK KUMAR", "thedeepak.ism37881@gmail.com", UserRole.MEMBER),
                TeamMember("DHEERAJ PATNAIK", "dheerajofficial35@gmail.com", UserRole.MEMBER),
                TeamMember("HARSH AGARWAL", "harsh.agarwal@gmail.com", UserRole.MEMBER),
                TeamMember("PRIYANSHU GUPTA", "priyanshugupta.ggv@gmail.com", UserRole.MEMBER),
                TeamMember("NIKITA SINGH", "nk2371174@gmail.com", UserRole.MEMBER)
            ),
            Team(2, "CP/DSA") to listOf(
                TeamMember("ANKIT SHARMA", "asharma65382@gmail.com", UserRole.TEAM_LEAD),
                TeamMember("ARJUN VERMA", "vermarjun26@gmail.com", UserRole.MEMBER),
                TeamMember("DEEPAK RAY", "deepakray262003@gmail.com", UserRole.MEMBER),
                TeamMember("HALDHAR BEHERA", "haldharbehera42@gmail.com", UserRole.MEMBER),
                TeamMember("ABHIRAJ", "avirajsingh176@gmail.com", UserRole.MEMBER),
                TeamMember("RAVI TIWARI", "raviashoktiwari9559@gmail.com", UserRole.MEMBER),
                TeamMember("NIKHIL CHANDRAKAR", "nikhilchandrakar00@gmail.com", UserRole.MEMBER)
            ),
            Team(3, "App Development") to listOf(
                TeamMember("AYUSH RAI", "shweetarai@gmail.com", UserRole.TEAM_LEAD),
                TeamMember("AYUSH RAI", "shweetarai@gmail.com", UserRole.TEAM_LEAD),
                TeamMember("HIMANSHU RANJAN", "himanshumehta824237@gmail.com", UserRole.MEMBER),
                TeamMember("KANDERI ALEKHYA", "alekhyak383@gmail.com", UserRole.MEMBER),
                TeamMember("OSIM KUMAR LAHA", "osimkumarlaha@gmail.com", UserRole.MEMBER),
                TeamMember("SAIMANDS ROY", "saimandsroy@gmail.com", UserRole.MEMBER),
                TeamMember("SHIVAM KUMAR", "shivamkumaar2004@gmail.com", UserRole.MEMBER)
            ),
            Team(4, "AI/ML") to listOf(
                TeamMember("ABHISHEK SINGH", "abhishek.singh@gfgggv.com", UserRole.TEAM_LEAD),
                TeamMember("DIVYANSHU MISHRA", "divyanshu.mishra0208@gmail.com", UserRole.MEMBER),
                TeamMember("SOURAV KUMAR", "sourav.kumar@gfgggv.com", UserRole.MEMBER),
                TeamMember("HIMANSHU AGRAHARI", "himanshuggv@gmail.com", UserRole.MEMBER),
                TeamMember("VINEET KUMAR", "vineet.kumar@gfgggv.com", UserRole.MEMBER),
                TeamMember("ADITYA KUMAR PANDEY", "pandey.aditya2048@gmail.com", UserRole.MEMBER),
                TeamMember("GAVIDI DEEPASHIKHA", "gavidi.deepashikha@gfgggv.com", UserRole.MEMBER)
            ),
            Team(5, "Game Development") to listOf(
                TeamMember("KANISHK RANJAN", "kaniskaranjanbarman@gmail.com", UserRole.TEAM_LEAD),
                TeamMember("TANISHQ JANGIR", "tanishqjangir01@gmail.com", UserRole.MEMBER),
                TeamMember("NIRAJ DHORE", "dhoreniraj83@gmail.com", UserRole.MEMBER),
                TeamMember("AKASH DAS MAHANT", "akashmahant14@gmail.com", UserRole.MEMBER),
                TeamMember("BALAKRITI KUMARI", "balakriti2004@gmail.com", UserRole.MEMBER),
                TeamMember("CHHAYANSH SAHU", "chhayanshsahu16@gmail.com", UserRole.MEMBER)
            ),
            Team(6, "IOT Team") to listOf(
                TeamMember("RAUSHAN YADAV", "raushanyadav1001@gmail.com", UserRole.TEAM_LEAD),
                TeamMember("KELLA Y. SAI KAUSHAL", "saikoushalkella@gmail.com", UserRole.MEMBER),
                TeamMember("UJJWAL KUMAR", "ujjwalkumar0226@gmail.com", UserRole.MEMBER),
                TeamMember("SANIYA SHRISTY GUPTA", "saniyashristy1612@gmail.com", UserRole.MEMBER),
                TeamMember("PARASMANI KHUNTE", "parasmanikhunt@gmail.com", UserRole.MEMBER)
            ),
            Team(7, "Cyber Security") to listOf(
                TeamMember("AYUSH SRIVASTAVA", "ayushsrivastavtaktakpur@gmail.com", UserRole.TEAM_LEAD),
                TeamMember("MEGHANA JILAKARA", "meghana.jilakara@gfgggv.com", UserRole.MEMBER),
                TeamMember("RAUSHAN KUMAR", "raushan80_443@gmail.com", UserRole.MEMBER),
                TeamMember("BABLU KUMAR", "krbablu7050@gmail.com", UserRole.MEMBER),
                TeamMember("SAMI BANJARE", "samibanjare04@gmail.com", UserRole.MEMBER)
            )
        )

        teamsWithMembers.forEach { (team, members) ->
            val teamRef = firestore.collection("teams").document(team.id.toString())
            teamRef.set(mapOf("name" to team.name)).await()

            members.forEach { member ->
                teamRef.collection("members").document(member.email)
                    .set(member).await()
            }
        }
    }

    data class Team(val id: Int, val name: String)
    data class TeamMember(val name: String, val email: String, val role: UserRole)
}


