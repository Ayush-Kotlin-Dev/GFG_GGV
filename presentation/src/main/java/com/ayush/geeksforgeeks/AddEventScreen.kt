package com.ayush.geeksforgeeks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ayush.geeksforgeeks.home.Event

@Composable
fun AddEventScreen(
    onEventAdded: (Event) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var registrationDeadline by remember { mutableStateOf("") }
    var formLink by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Add New Event", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Date (DD-MM-YYYY)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = time,
            onValueChange = { time = it },
            label = { Text("Time (HH:MM-HH:MM)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = registrationDeadline,
            onValueChange = { registrationDeadline = it },
            label = { Text("Registration Deadline (DD-MM-YYYY)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = formLink,
            onValueChange = { formLink = it },
            label = { Text("Form Link") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    val newEvent = Event(
                        id = System.currentTimeMillis().toInt(),
                        title = title,
                        description = description,
                        date = date,
                        time = time,
                        registrationDeadline = registrationDeadline,
                        formLink = formLink,
                        imageRes = R.drawable.twitter_color_svgrepo_com
                    )
                    onEventAdded(newEvent)
                },
                enabled = title.isNotBlank() && description.isNotBlank() && date.isNotBlank() &&
                        time.isNotBlank() && registrationDeadline.isNotBlank() && formLink.isNotBlank()
            ) {
                Text("Add Event")
            }
        }
    }
}