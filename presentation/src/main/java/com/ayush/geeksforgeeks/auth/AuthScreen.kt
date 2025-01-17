package com.ayush.geeksforgeeks.auth

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ayush.data.datastore.UserRole
import com.ayush.data.model.Team
import com.ayush.data.model.TeamMember
import com.ayush.data.repository.AuthRepository
import com.ayush.data.repository.AuthState
import com.ayush.geeksforgeeks.ContainerApp
import com.ayush.geeksforgeeks.R
import com.ayush.geeksforgeeks.auth.components.ContactAdminDialog
import com.ayush.geeksforgeeks.auth.components.EmailVerificationContent
import com.ayush.geeksforgeeks.auth.components.ForgotPasswordDialog
import com.ayush.geeksforgeeks.auth.components.GuestSignupForm
import com.ayush.geeksforgeeks.auth.components.InputField
import com.ayush.geeksforgeeks.auth.components.MemberDropdown
import com.ayush.geeksforgeeks.auth.components.TeamDropdown
import com.ayush.geeksforgeeks.ui.theme.GFGBackground
import com.ayush.geeksforgeeks.ui.theme.GFGBlack
import com.ayush.geeksforgeeks.ui.theme.GFGStatusPending
import com.ayush.geeksforgeeks.ui.theme.GFGStatusPendingText
import com.ayush.geeksforgeeks.utils.LoadingIndicator
import kotlinx.coroutines.launch

class AuthScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel: AuthViewModel = hiltViewModel()
        val navigator = LocalNavigator.currentOrThrow
        LoginContent(viewModel, navigator)
    }
}

@Composable
private fun LoginContent(
    viewModel: AuthViewModel,
    navigator: Navigator
) {
    var isLoginMode by rememberSaveable { mutableStateOf(true) }

    val focusManager = LocalFocusManager.current
    val (emailFocus, passwordFocus) = remember { FocusRequester.createRefs() }

    val authState by viewModel.authState.collectAsState()
    val teams by viewModel.teams.collectAsState()
    val teamMembers by viewModel.teamMembers.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var userRole by rememberSaveable { mutableStateOf<UserRole?>(null) }
    var showForgotPasswordDialog by rememberSaveable { mutableStateOf(false) }
    val resetPasswordState by viewModel.resetPasswordState.collectAsState()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                coroutineScope.launch {
                    userRole = viewModel.getUserRoleOnLogin()
                    userRole?.let {
                        navigator.replaceAll(ContainerApp(it))
                        Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            is AuthState.Error -> {
                Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_SHORT)
                    .show()
            }

            is AuthState.EmailVerificationRequired, is AuthState.EmailVerificationSent -> {
                Toast.makeText(
                    context,
                    "Please verify your email to continue. Check your inbox for the verification link.",
                    Toast.LENGTH_LONG
                ).show()
            }

            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GFGStatusPending)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Logo()
            WelcomeText(textColor = GFGBlack)

            when (authState) {
                is AuthState.EmailVerificationRequired,
                is AuthState.EmailVerificationSent -> {
                    EmailVerificationContent(
                        onResendEmail = { viewModel.resendVerificationEmail() },
                        viewModel = viewModel
                    )
                }

                else -> {
                    LoginCard(
                        isLoginMode = isLoginMode,
                        email = viewModel.email,
                        password = viewModel.password,
                        regularStudentName = viewModel.guestName,
                        teams = teams,
                        teamMembers = teamMembers,
                        selectedTeam = viewModel.selectedTeam,
                        selectedMember = viewModel.selectedMember,
                        onEmailChange = viewModel::updateEmail,
                        onPasswordChange = viewModel::updatePassword,
                        onRegularStudentNameChange = viewModel::updateGuestName,
                        onTeamSelect = viewModel::selectTeam,
                        onMemberSelect = viewModel::selectMember,
                        onModeChange = { isLoginMode = it },
                        focusManager = focusManager,
                        emailFocus = emailFocus,
                        passwordFocus = passwordFocus,
                        onSubmitButtonClick = {
                            if (isLoginMode) {
                                viewModel.login()
                            } else {
                                if (viewModel.isGuestSignup) {
                                    viewModel.signUpGuest()
                                } else {
                                    viewModel.signUp()
                                }
                            }
                        },
                        onForgotPasswordClick = { showForgotPasswordDialog = true },
                        isRegularSignup = viewModel.isGuestSignup,
                        onRegularSignupChange = viewModel::updateSignupMode
                    )
                }
            }
        }
        if (authState is AuthState.Loading) {
            LoadingIndicator()
        }
        if (showForgotPasswordDialog) {
            ForgotPasswordDialog(
                onDismiss = { showForgotPasswordDialog = false },
                onSubmit = { email ->
                    viewModel.sendPasswordResetEmail(email)
                },
                resetPasswordState = resetPasswordState
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginCard(
    isLoginMode: Boolean,
    email: String,
    password: String,
    regularStudentName: String,
    teams: List<Team>,
    teamMembers: List<TeamMember>,
    selectedTeam: Team?,
    selectedMember: TeamMember?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRegularStudentNameChange: (String) -> Unit,
    onTeamSelect: (Team) -> Unit,
    onMemberSelect: (TeamMember) -> Unit,
    onModeChange: (Boolean) -> Unit,
    focusManager: FocusManager,
    emailFocus: FocusRequester,
    passwordFocus: FocusRequester,
    onSubmitButtonClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    isRegularSignup: Boolean,
    onRegularSignupChange: (Boolean) -> Unit
) {
    var expandedTeam by remember { mutableStateOf(false) }
    var expandedMember by remember { mutableStateOf(false) }
    var showContactAdminDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GFGBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            val submitButtonText by remember(isLoginMode, isRegularSignup) {
                derivedStateOf {
                    when {
                        isLoginMode -> "Login"
                        isRegularSignup -> "Sign Up as Guest"
                        else -> "Sign Up as Club Member"
                    }
                }
            }

            val cardTitle by remember(isLoginMode, isRegularSignup) {
                derivedStateOf {
                    when {
                        isLoginMode -> "Login"
                        isRegularSignup -> "Guest / Visitor Sign Up"
                        else -> "Club Member Sign Up"
                    }
                }
            }

            Text(
                text = cardTitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = GFGBlack
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (!isLoginMode) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = !isRegularSignup,
                        onClick = { onRegularSignupChange(false) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = GFGStatusPendingText,
                            activeContentColor = Color.White,
                            inactiveContainerColor = GFGStatusPending,
                            inactiveContentColor = GFGBlack
                        )
                    ) {
                        Text(
                            text = "Club Member",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    SegmentedButton(
                        selected = isRegularSignup,
                        onClick = { onRegularSignupChange(true) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = GFGStatusPendingText,
                            activeContentColor = Color.White,
                            inactiveContainerColor = GFGStatusPending,
                            inactiveContentColor = GFGBlack
                        )
                    ) {
                        Text(
                            text = "Guest User",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (isRegularSignup) {
                    GuestSignupForm(
                        name = regularStudentName,
                        email = email,
                        password = password,
                        onNameChange = onRegularStudentNameChange,
                        onEmailChange = onEmailChange,
                        onPasswordChange = onPasswordChange,
                        onSubmit = onSubmitButtonClick,
                        focusManager = focusManager,
                    )
                } else {
                    val teamSections = listOf(
                        TeamSection("Core Teams", 0..0),
                        TeamSection("Tech Teams", 1..7),
                        TeamSection("Non-Tech Teams", 8..Int.MAX_VALUE)
                    )

                    TeamDropdown(
                        teams = teams,
                        selectedTeam = selectedTeam,
                        onTeamSelect = onTeamSelect,
                        teamSections = teamSections
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (selectedTeam != null) {
                        MemberDropdown(
                            teamMembers = teamMembers,
                            selectedMember = selectedMember,
                            onMemberSelect = onMemberSelect
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    InputField(
                        value = email,
                        onValueChange = onEmailChange,
                        label = "Email",
                        focusRequester = emailFocus,
                        nextFocusRequester = passwordFocus,
                        focusManager = focusManager,
                        keyboardType = KeyboardType.Email,
                        readOnly = true
                    )
                    if (selectedTeam != null && selectedMember != null) {
                        Text(
                            text = "Incorrect email?",
                            color = GFGStatusPendingText,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showContactAdminDialog = true }
                                .padding(top = 4.dp, bottom = 8.dp),
                            textAlign = TextAlign.End
                        )
                    }
                    InputField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = "Password",
                        isPassword = true,
                        keyboardType = KeyboardType.Password,

                        focusRequester = passwordFocus,
                        focusManager = focusManager,
                        imeAction = ImeAction.Done
                    )
                }
            } else {
                InputField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = "Email",
                    focusRequester = emailFocus,
                    nextFocusRequester = passwordFocus,
                    focusManager = focusManager,
                    keyboardType = KeyboardType.Email
                )
                InputField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = "Password",
                    isPassword = true,
                    focusRequester = passwordFocus,
                    keyboardType = KeyboardType.Password,
                    focusManager = focusManager,
                    imeAction = ImeAction.Done
                )
                ForgotPasswordText(onClick = onForgotPasswordClick)
            }

            Button(
                onClick = onSubmitButtonClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GFGStatusPendingText)
            ) {
                Text(
                    text = submitButtonText,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            ToggleModeText(isLoginMode, onModeChange)
        }
    }
    if (showContactAdminDialog) {
        ContactAdminDialog(
            currentEmail = email,
            selectedTeam = selectedTeam,
            selectedMember = selectedMember,
            onDismiss = { showContactAdminDialog = false }
        )
    }
}

data class TeamSection(
    val title: String,
    val idRange: IntRange
)

@Composable
private fun Logo() {
    Image(
        painter = painterResource(id = R.drawable.geeksforgeeks_logo),
        contentDescription = "GeeksforGeeks Logo",
        modifier = Modifier.size(100.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun WelcomeText(textColor: Color) {
    Text(
        text = "Welcome",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = textColor
    )
    Text(
        text = "Please log in to continue",
        fontSize = 16.sp,
        color = textColor.copy(alpha = 0.7f),
        lineHeight = 24.sp
    )
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun ForgotPasswordText(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = "Forgot Password?",
            color = GFGStatusPendingText,
            modifier = Modifier
                .padding(end = 8.dp)
                .clickable(onClick = onClick)
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun ToggleModeText(isLoginMode: Boolean, onModeChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isLoginMode) "Don't have an account? " else "Already have an account? ",
            color = GFGBlack.copy(alpha = 0.7f)
        )
        Text(
            text = if (isLoginMode) "Sign Up" else "Login",
            color = GFGStatusPendingText,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onModeChange(!isLoginMode) }
        )
    }
}