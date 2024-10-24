package com.ayush.geeksforgeeks.auth

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ayush.data.datastore.UserRole
import com.ayush.data.repository.AuthState
import com.ayush.geeksforgeeks.ContainerApp
import com.ayush.geeksforgeeks.R
import com.ayush.geeksforgeeks.ui.theme.GFGBackground
import com.ayush.geeksforgeeks.ui.theme.GFGBlack
import com.ayush.geeksforgeeks.ui.theme.GFGStatusPending
import com.ayush.geeksforgeeks.ui.theme.GFGStatusPendingText
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
    var isLoginMode by remember { mutableStateOf(true) }

    var selectedDomain by remember { mutableIntStateOf(0) }
    var selectedRole by remember { mutableStateOf("") }
    var expandedDomain by remember { mutableStateOf(false) }
    var expandedRole by remember { mutableStateOf(false) }
    val domains = listOf(
        1 to "App dev",
        2 to "Web dev ",
        3 to "IoT",
        4 to "Cyber Security",
        5 to "Cp/Dsa ",
        6 to "Ai/Ml",
        7 to "Game dev",
        8 to "Design & Branding",
        9 to "Content",
        10 to "Event",
        11 to "Marketing & Pr",
    )
    val roles = listOf("MEMBER", "TEAM_LEAD")

    val focusManager = LocalFocusManager.current
    val (usernameFocus, emailFocus, passwordFocus) = remember { FocusRequester.createRefs() }

    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var userRole by remember { mutableStateOf<UserRole?>(null) }

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
                Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_SHORT).show()
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

            LoginCard(
                isLoginMode = isLoginMode,
                username = viewModel.username,
                email = viewModel.email,
                password = viewModel.password,
                selectedDomain = selectedDomain,
                selectedRole = selectedRole,
                expandedDomain = expandedDomain,
                expandedRole = expandedRole,
                domains = domains,
                roles = roles,
                onUsernameChange = viewModel::updateUsername,
                onEmailChange = viewModel::updateEmail,
                onPasswordChange = viewModel::updatePassword,
                onDomainChange = { selectedDomain = it },
                onRoleChange = { selectedRole = it },
                onExpandedDomainChange = { expandedDomain = it },
                onExpandedRoleChange = { expandedRole = it },
                onModeChange = { isLoginMode = it },
                focusManager = focusManager,
                usernameFocus = usernameFocus,
                emailFocus = emailFocus,
                passwordFocus = passwordFocus,
                onSubmitButtonClick = {
                    if (isLoginMode) {
                        viewModel.login()
                    } else {
                        viewModel.signUp(domains[selectedDomain].first, UserRole.valueOf(selectedRole))
                    }
                }
            )
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginCard(
    isLoginMode: Boolean,
    username: String,
    email: String,
    password: String,
    selectedDomain: Int,
    selectedRole: String,
    expandedDomain: Boolean,
    expandedRole: Boolean,
    domains: List<Pair<Int, String>>,
    roles: List<String>,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onDomainChange: (Int) -> Unit,
    onRoleChange: (String) -> Unit,
    onExpandedDomainChange: (Boolean) -> Unit,
    onExpandedRoleChange: (Boolean) -> Unit,
    onModeChange: (Boolean) -> Unit,
    focusManager: FocusManager,
    usernameFocus: FocusRequester,
    emailFocus: FocusRequester,
    passwordFocus: FocusRequester,
    onSubmitButtonClick: () -> Unit
) {
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
                InputField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = "Username",
                    focusRequester = usernameFocus,
                    nextFocusRequester = emailFocus,
                    focusManager = focusManager
                )

                ExposedDropdownMenuBox(
                    expanded = expandedDomain,
                    onExpandedChange = onExpandedDomainChange,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = domains.find { it.first == selectedDomain }?.second ?: "Select Domain",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Domain") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDomain) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
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
                        expanded = expandedDomain,
                        onDismissRequest = { onExpandedDomainChange(false) }
                    ) {
                        domains.forEach { (id, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    onDomainChange(id)
                                    onExpandedDomainChange(false)
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedRole,
                    onExpandedChange = onExpandedRoleChange,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedRole,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Role") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRole) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
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
                        expanded = expandedRole,
                        onDismissRequest = { onExpandedRoleChange(false) }
                    ) {
                        roles.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role) },
                                onClick = {
                                    onRoleChange(role)
                                    onExpandedRoleChange(false)
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

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
                focusManager = focusManager,
                imeAction = ImeAction.Done
            )

            if (isLoginMode) {
                ForgotPasswordText()
            }

            LoginButton(
                isLoginMode,
                focusManager,
                onSubmitButtonClick
            )

            ToggleModeText(isLoginMode, onModeChange)
        }
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
private fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false,
    focusRequester: FocusRequester,
    nextFocusRequester: FocusRequester? = null,
    focusManager: FocusManager,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GFGStatusPendingText,
            unfocusedBorderColor = GFGBlack.copy(alpha = 0.5f),
            focusedLabelColor = GFGStatusPendingText,
            unfocusedLabelColor = GFGBlack,
            focusedTextColor = GFGBlack,
            unfocusedTextColor = GFGBlack,
            cursorColor = GFGStatusPendingText
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = { nextFocusRequester?.requestFocus() },
            onDone = { focusManager.clearFocus() }
        ),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun ForgotPasswordText() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = "Forgot Password?",
            color = GFGStatusPendingText,
            modifier = Modifier
                .padding(end = 8.dp)
                .clickable { /* Add forgot password action here */ }
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

