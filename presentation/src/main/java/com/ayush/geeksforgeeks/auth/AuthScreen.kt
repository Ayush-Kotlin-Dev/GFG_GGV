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
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.ayush.data.repository.AuthRepository
import com.ayush.data.repository.AuthState
import com.ayush.geeksforgeeks.ContainerApp
import com.ayush.geeksforgeeks.R
import com.ayush.geeksforgeeks.auth.components.EmailVerificationContent
import com.ayush.geeksforgeeks.auth.components.ForgotPasswordDialog
import com.ayush.geeksforgeeks.auth.components.InputField
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
                        teams = teams,
                        teamMembers = teamMembers,
                        selectedTeam = viewModel.selectedTeam,
                        selectedMember = viewModel.selectedMember,
                        onEmailChange = viewModel::updateEmail,
                        onPasswordChange = viewModel::updatePassword,
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
                                viewModel.signUp()
                            }
                        },
                        onForgotPasswordClick = { showForgotPasswordDialog = true }
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
    teams: List<AuthRepository.Team>,
    teamMembers: List<AuthRepository.TeamMember>,
    selectedTeam: AuthRepository.Team?,
    selectedMember: AuthRepository.TeamMember?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTeamSelect: (AuthRepository.Team) -> Unit,
    onMemberSelect: (AuthRepository.TeamMember) -> Unit,
    onModeChange: (Boolean) -> Unit,
    focusManager: FocusManager,
    emailFocus: FocusRequester,
    passwordFocus: FocusRequester,
    onSubmitButtonClick: () -> Unit,
    onForgotPasswordClick: () -> Unit = { }
) {
    var showContactAdminDialog by remember { mutableStateOf(false) }
    var newEmail by remember { mutableStateOf("") }
    val context = LocalContext.current

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
            Text(
                text = if (isLoginMode) "Login" else "Sign Up",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = GFGBlack
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (!isLoginMode) {
                var expandedTeam by remember { mutableStateOf(false) }
                var expandedMember by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expandedTeam,
                    onExpandedChange = { expandedTeam = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedTeam?.name ?: "Select Team",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Team") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTeam) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GFGStatusPendingText,
                            unfocusedBorderColor = GFGBlack.copy(alpha = 0.5f),
                            focusedLabelColor = GFGStatusPendingText,
                            unfocusedLabelColor = GFGBlack,
                            focusedTextColor = GFGBlack,
                            unfocusedTextColor = GFGBlack
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTeam,
                        onDismissRequest = { expandedTeam = false },
                        modifier = Modifier
                            .exposedDropdownSize()
                            .background(GFGStatusPending)
                            .alpha(0.8f)
                    ) {
                        // Leadership Team
                        teams.filter { it.id.toInt() == 0 }.forEach { team ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        team.name,
                                        color = GFGBlack,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                onClick = {
                                    onTeamSelect(team)
                                    expandedTeam = false
                                },
                            )
                        }
                        Divider(color = GFGStatusPendingText, thickness = 1.dp)

                        // Tech Teams
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Tech Teams",
                                    fontWeight = FontWeight.Bold,
                                    color = GFGStatusPendingText
                                )
                            },
                            onClick = { },
                            enabled = false
                        )
                        teams.filter { it.id.toInt() in 1..7 }.forEach { team ->
                            DropdownMenuItem(
                                text = { Text(team.name, color = GFGBlack) },
                                onClick = {
                                    onTeamSelect(team)
                                    expandedTeam = false
                                },
                            )
                        }
                        Divider(color = GFGStatusPendingText, thickness = 1.dp)

                        // Non-Tech Teams
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Non-Tech Teams",
                                    fontWeight = FontWeight.Bold,
                                    color = GFGStatusPendingText
                                )
                            },
                            onClick = { },
                            enabled = false
                        )
                        teams.filter { it.id.toInt() > 7 }.forEach { team ->
                            DropdownMenuItem(
                                text = { Text(team.name, color = GFGBlack) },
                                onClick = {
                                    onTeamSelect(team)
                                    expandedTeam = false
                                },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (selectedTeam != null) {
                    ExposedDropdownMenuBox(
                        expanded = expandedMember,
                        onExpandedChange = { expandedMember = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedMember?.name ?: "Select Your Name",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Your Name") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMember) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GFGStatusPendingText,
                                unfocusedBorderColor = GFGBlack.copy(alpha = 0.5f),
                                focusedLabelColor = GFGStatusPendingText,
                                unfocusedLabelColor = GFGBlack,
                                focusedTextColor = GFGBlack,
                                unfocusedTextColor = GFGBlack
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expandedMember,
                            onDismissRequest = { expandedMember = false },
                            modifier = Modifier
                                .exposedDropdownSize()
                                .background(GFGStatusPending)
                                .alpha(0.8f)
                        ) {
                            teamMembers.forEach { member ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "${member.name} (${member.role})",
                                            color = GFGBlack
                                        )
                                    },
                                    onClick = {
                                        onMemberSelect(member)
                                        expandedMember = false
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = GFGBlack,
                                        leadingIconColor = GFGBlack,
                                        trailingIconColor = GFGBlack,
                                        disabledTextColor = GFGBlack.copy(alpha = 0.5f),
                                        disabledLeadingIconColor = GFGBlack.copy(alpha = 0.5f),
                                        disabledTrailingIconColor = GFGBlack.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            InputField(
                value = email,
                onValueChange = onEmailChange,
                label = "Email",
                focusRequester = emailFocus,
                nextFocusRequester = passwordFocus,
                focusManager = focusManager,
                keyboardType = KeyboardType.Email,
                readOnly = !isLoginMode
            )
            if (!isLoginMode && selectedTeam != null && selectedMember != null) {
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
                focusRequester = passwordFocus,
                focusManager = focusManager,
                imeAction = ImeAction.Done
            )

            if (isLoginMode) {
                ForgotPasswordText(onClick = onForgotPasswordClick)
            }

            LoginButton(
                isLoginMode,
                focusManager,
                onSubmitButtonClick,
            )

            ToggleModeText(isLoginMode, onModeChange)
        }
    }
    if (showContactAdminDialog) {
        AlertDialog(
            onDismissRequest = { showContactAdminDialog = false },
            title = { Text("Contact Admin") },
            text = {
                Column {
                    Text("Please enter your correct email:")
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("New Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val message = "Hello, this is regarding the GFG Student Chapter GGV app. " +
                                "I need to change my email for my account. " +
                                "My current email is $email. " +
                                "I want to change it to $newEmail. " +
                                "Team: ${selectedTeam?.name}, " +
                                "Member: ${selectedMember?.name}. " +
                                "Can you please help?"
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data =
                                Uri.parse("https://wa.me/+916264450423?text=${Uri.encode(message)}")
                        }
                        context.startActivity(intent)
                        showContactAdminDialog = false
                    }
                ) {
                    Text("Contact Admin on WhatsApp")
                }
            },
            dismissButton = {
                TextButton(onClick = { showContactAdminDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = GFGBackground,
        )
    }
}

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
private fun LoginButton(
    isLoginMode: Boolean,
    focusManager: FocusManager,
    onSubmitButtonClick: () -> Unit
) {
    Button(
        onClick = {
            focusManager.clearFocus()
            onSubmitButtonClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GFGStatusPendingText)
    ) {
        Text(if (isLoginMode) "Login" else "Sign Up", color = Color.White)
    }
    Spacer(modifier = Modifier.height(16.dp))
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

