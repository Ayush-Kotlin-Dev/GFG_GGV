package com.ayush.geeksforgeeks.auth.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayush.geeksforgeeks.auth.AuthViewModel
import com.ayush.geeksforgeeks.auth.AuthViewModel.VerificationState
import com.ayush.geeksforgeeks.ui.theme.GFGBackground
import com.ayush.geeksforgeeks.ui.theme.GFGBlack
import com.ayush.geeksforgeeks.ui.theme.GFGStatusPendingText
import kotlinx.coroutines.delay

@Composable
fun EmailVerificationContent(
    onResendEmail: () -> Unit,
    viewModel: AuthViewModel
) {
    val verificationState by viewModel.verificationState.collectAsState()
    var isResending by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GFGBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Email Verification Required",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = GFGBlack
            )
            Spacer(modifier = Modifier.height(16.dp))

            when (verificationState) {
                is VerificationState.Loading -> {
                    CircularProgressIndicator(color = GFGStatusPendingText)
                    Text(
                        "Checking verification status...",
                        color = GFGBlack,
                        textAlign = TextAlign.Center
                    )
                }

                is VerificationState.TimeoutWarning -> {
                    val remainingSeconds =
                        (verificationState as VerificationState.TimeoutWarning).remainingSeconds
                    Text(
                        "Verification expires in ${remainingSeconds}s",
                        color = Color.Red,
                        textAlign = TextAlign.Center
                    )
                }

                is VerificationState.Timeout -> {
                    Text(
                        "Verification timed out. Please try again.",
                        color = Color.Red,
                        textAlign = TextAlign.Center
                    )
                }

                is VerificationState.Error -> {
                    Text(
                        (verificationState as VerificationState.Error).message,
                        color = Color.Red,
                        textAlign = TextAlign.Center
                    )
                }

                else -> {
                    Text(
                        "Please check your email and click the verification link to continue.",
                        color = GFGBlack,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        isResending = true
                        onResendEmail()
                    },
                    enabled = !isResending && verificationState !is VerificationState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = GFGStatusPendingText)
                ) {
                    if (isResending) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text("Resend Email", color = Color.White)
                    }
                }

                if (verificationState is VerificationState.Timeout ||
                    verificationState is VerificationState.Error
                ) {
                    Button(
                        onClick = { viewModel.retryVerification() },
                        colors = ButtonDefaults.buttonColors(containerColor = GFGStatusPendingText)
                    ) {
                        Text("Retry Verification", color = Color.White)
                    }
                }
            }
        }
    }

    LaunchedEffect(isResending) {
        if (isResending) {
            delay(3000)
            isResending = false
        }
    }
}