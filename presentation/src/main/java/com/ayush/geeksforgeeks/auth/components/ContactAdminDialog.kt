package com.ayush.geeksforgeeks.auth.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import com.ayush.data.model.Team
import com.ayush.data.model.TeamMember
import com.ayush.data.repository.AuthRepository
import com.ayush.geeksforgeeks.ui.theme.GFGBackground

@Composable
fun ContactAdminDialog(
    currentEmail: String,
    selectedTeam: Team?,
    selectedMember: TeamMember?,
    onDismiss: () -> Unit
) {
    var newEmail by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
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
                    val message = buildWhatsAppMessage(
                        currentEmail = currentEmail,
                        newEmail = newEmail,
                        teamName = selectedTeam?.name,
                        memberName = selectedMember?.name
                    )
                    launchWhatsApp(context, message)
                    onDismiss()
                }
            ) {
                Text("Contact Admin on WhatsApp")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = GFGBackground
    )
}

private fun buildWhatsAppMessage(
    currentEmail: String,
    newEmail: String,
    teamName: String?,
    memberName: String?
): String {
    return """
        Hello, this is regarding the GFG Student Chapter GGV app.
        I need to change my email for my account.
        My current email is $currentEmail.
        I want to change it to $newEmail.
        Team: $teamName
        Member: $memberName
        Can you please help?
    """.trimIndent()
}

private fun launchWhatsApp(context: android.content.Context, message: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("https://wa.me/${Constants.ADMIN_WHATSAPP}?text=${Uri.encode(message)}")
    }
    context.startActivity(intent)
}

private object Constants {
    const val ADMIN_WHATSAPP = "+916264450423"
}