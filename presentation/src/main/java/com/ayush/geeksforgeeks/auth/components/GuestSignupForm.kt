package com.ayush.geeksforgeeks.auth.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun GuestSignupForm(
    name: String,
    email: String,
    password: String,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    focusManager: FocusManager
) {
    val (nameFocus, emailFocus, passwordFocus) = remember { FocusRequester.createRefs() }

    InputField(
        value = name,
        onValueChange = onNameChange,
        label = "Full Name",
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Next,
        focusRequester = nameFocus,
        nextFocusRequester = emailFocus,
        focusManager = focusManager
    )
    InputField(
        value = email,
        onValueChange = onEmailChange,
        label = "College Email (@ggv.ac.in)",
        keyboardType = KeyboardType.Email,
        imeAction = ImeAction.Next,
        focusRequester = emailFocus,
        nextFocusRequester = passwordFocus,
        focusManager = focusManager
    )
    InputField(
        value = password,
        onValueChange = onPasswordChange,
        label = "Password",
        isPassword = true,
        keyboardType = KeyboardType.Password,
        imeAction = ImeAction.Done,
        focusRequester = passwordFocus,
        focusManager = focusManager,
        onImeAction = {
            focusManager.clearFocus()
            onSubmit()
        }
    )
}