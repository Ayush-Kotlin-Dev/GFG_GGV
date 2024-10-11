package com.ayush.geeksforgeeks.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import com.ayush.geeksforgeeks.R
import com.ayush.geeksforgeeks.ui.theme.*

class LoginScreen : Screen {
    @Composable
    override fun Content() {

        val isDarkTheme = isSystemInDarkTheme()
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        val backgroundColor = if (isDarkTheme) Color.Black else MintGreen
        val cardColor = if (isDarkTheme) DarkGray else Color.White
        val textColor = if (isDarkTheme) Color.White else Color.Black

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // GeeksforGeeks Logo
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_background ),
                    contentDescription = "GeeksforGeeks Logo",
                    modifier = Modifier.size(80.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )

                Text(
                    text = "Please log in to continue and get\nthe best from our app",
                    fontSize = 16.sp,
                    color = textColor.copy(alpha = 0.7f),
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Login Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Login or Sign up",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email or Username") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GeeksForGeeksGreen,
                                unfocusedBorderColor = textColor.copy(alpha = 0.5f)
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GeeksForGeeksGreen,
                                unfocusedBorderColor = textColor.copy(alpha = 0.5f)
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Forgot Password?",
                            color = GeeksForGeeksGreen,
                            modifier = Modifier.align(Alignment.End)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { /* Handle login */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GeeksForGeeksGreen)
                        ) {
                            Text("Continue", color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Or, login with",
                            color = textColor.copy(alpha = 0.7f),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(16.dp))


                       // TODO() Add dont have account ? SignUp
                    }
                }
            }
        }
    }
}

@Composable
fun SocialLoginButton(iconRes: Int, contentDescription: String) {
    IconButton(
        onClick = { /* Handle social login */ },
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.LightGray.copy(alpha = 0.2f))
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp)
        )
    }
}

// Add this to your Color.kt file
val MintGreen = Color(0xFFE0F2F1)
val GeeksForGeeksGreen = Color(0xFF0F9D58)
val DarkGray = Color(0xFF121212)