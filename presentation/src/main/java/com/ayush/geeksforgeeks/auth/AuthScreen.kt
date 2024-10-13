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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.ayush.geeksforgeeks.ContainerApp
import com.ayush.geeksforgeeks.R
import com.ayush.geeksforgeeks.home.HomeScreen
import com.ayush.geeksforgeeks.ui.theme.DarkGray
import com.ayush.geeksforgeeks.ui.theme.GeeksForGeeksGreen
import com.ayush.geeksforgeeks.ui.theme.MintGreen

class AuthScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel : AuthViewModel = hiltViewModel()
        val navigator = LocalNavigator.currentOrThrow

        LoginContent(viewModel,navigator)
    }
}

@Composable
private fun LoginContent(
    viewModel: AuthViewModel,
    navigator: Navigator
) {
    val isDarkTheme = isSystemInDarkTheme()
    var isLoginMode by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val colors = getThemeColors(isDarkTheme)
    val focusManager = LocalFocusManager.current
    val (usernameFocus, emailFocus, passwordFocus) = remember { FocusRequester.createRefs() }

    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                navigator.replaceAll(ContainerApp())
                Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
            }
            is AuthState.Error -> {
                Toast.makeText(context, "Login failed", Toast.LENGTH_SHORT).show()
            }
            else -> {

            } // Handle other states if needed
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Logo()
            WelcomeText(colors.text)
            LoginCard(
                isLoginMode = isLoginMode,
                username = username,
                email = email,
                password = password,
                onUsernameChange = { username = it },
                onEmailChange = { email = it },
                onPasswordChange = { password = it },
                onModeChange = { isLoginMode = it },
                colors = colors,
                focusManager = focusManager,
                usernameFocus = usernameFocus,
                emailFocus = emailFocus,
                passwordFocus = passwordFocus,
                onSubmitButtonClick = {
                    if (isLoginMode) {
                        viewModel.login(email, password)
                    } else {
                        viewModel.signUp(username , email, password)
                    }
                }
            )
        }
    }
}
@Composable
private fun Logo() {
    Image(
        painter = painterResource(id = R.drawable.geeksforgeeks_logo  ),
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
private fun LoginCard(
    isLoginMode: Boolean,
    username: String,
    email: String,
    password: String,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onModeChange: (Boolean) -> Unit,
    colors: ThemeColors,
    focusManager: FocusManager,
    usernameFocus: FocusRequester,
    emailFocus: FocusRequester,
    passwordFocus: FocusRequester,
    onSubmitButtonClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card)
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
                color = colors.text
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (!isLoginMode) {
                InputField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = "Username",
                    colors = colors,
                    focusRequester = usernameFocus,
                    nextFocusRequester = emailFocus,
                    focusManager = focusManager
                )
            }

            InputField(
                value = email,
                onValueChange = onEmailChange,
                label = "Email",
                colors = colors,
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
                colors = colors,
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

            ToggleModeText(isLoginMode, onModeChange, colors.text)
        }
    }
}


@Composable
private fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false,
    colors: ThemeColors,
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
            focusedBorderColor = GeeksForGeeksGreen,
            unfocusedBorderColor = colors.text.copy(alpha = 0.5f),
            focusedLabelColor = GeeksForGeeksGreen,
            unfocusedLabelColor = colors.text,
            focusedTextColor = colors.text,
            unfocusedTextColor = colors.text,
            cursorColor = GeeksForGeeksGreen
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
            color = GeeksForGeeksGreen,
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
        colors = ButtonDefaults.buttonColors(containerColor = GeeksForGeeksGreen)
    ) {
        Text(if (isLoginMode) "Login" else "Sign Up", color = Color.White)
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun ToggleModeText(isLoginMode: Boolean, onModeChange: (Boolean) -> Unit, textColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isLoginMode) "Don't have an account? " else "Already have an account? ",
            color = textColor.copy(alpha = 0.7f)
        )
        Text(
            text = if (isLoginMode) "Sign Up" else "Login",
            color = GeeksForGeeksGreen,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onModeChange(!isLoginMode) }
        )
    }
}

private data class ThemeColors(
    val background: Color,
    val card: Color,
    val text: Color,
    val focusedLabel: Color = GeeksForGeeksGreen,
    val unfocusedLabel: Color = text
)

@Composable
private fun getThemeColors(isDarkTheme: Boolean): ThemeColors {
    return if (isDarkTheme) {
        ThemeColors(
            background = Color.Black,
            card = DarkGray,
            text = Color.White
        )
    } else {
        ThemeColors(
            background = MintGreen,
            card = Color.White,
            text = Color.Black
        )
    }
}