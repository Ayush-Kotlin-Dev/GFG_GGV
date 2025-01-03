package com.ayush.geeksforgeeks.utils

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.ayush.data.model.Event
import kotlinx.coroutines.launch
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import com.ayush.geeksforgeeks.ui.theme.GFGBackground
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(
    onEventAdded: (Event, Uri) -> Unit,
    onDismiss: () -> Unit
) {
    var eventData by remember { mutableStateOf(EventData()) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeadlinePicker by remember { mutableStateOf(false) }
    var urlError by remember { mutableStateOf<String?>(null) }
    var imageError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // Enhanced URL validation function
    fun validateUrl(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://")
    }

    // Enhanced image picker with size validation
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                    val fileSize = descriptor.statSize
                    val maxSize = 15 * 1024 * 1024 // 15MB in bytes
                    val sizeMB = fileSize / (1024.0 * 1024.0)

                    if (fileSize > maxSize) {
                        imageError = "Image size (${String.format("%.1f", sizeMB)}MB) exceeds limit of 15MB"
                        imageUri = null
                    } else {
                        imageError = null
                        imageUri = uri
                    }
                }
            } catch (e: Exception) {
                imageError = "Failed to process image"
                imageUri = null
            }
        }
    }

    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()
    val deadlinePickerState = rememberDatePickerState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Add New Event", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        EventInputField(
            value = eventData.title,
            onValueChange = { eventData = eventData.copy(title = it) },
            label = "Title"
        )

        EventInputField(
            value = eventData.description,
            onValueChange = { eventData = eventData.copy(description = it) },
            label = "Description",
            singleLine = false
        )

        EventInputField(
            value = eventData.date,
            onValueChange = {},
            label = "Event Date",
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                }
            }
        )

        EventInputField(
            value = eventData.time,
            onValueChange = {},
            label = "Event Time",
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showTimePicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Select Time")
                }
            }
        )

        EventInputField(
            value = eventData.registrationDeadline,
            onValueChange = {},
            label = "Registration Deadline",
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDeadlinePicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Select Registration Deadline")
                }
            }
        )

        EventInputField(
            value = eventData.formLink,
            onValueChange = { 
                eventData = eventData.copy(formLink = it)
                urlError = if (it.isNotBlank() && !validateUrl(it)) {
                    "URL must start with http:// or https://"
                } else null
            },
            label = "Form Link",
            isError = urlError != null,
            errorMessage = urlError
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { imagePicker.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select Image")
        }

        Spacer(modifier = Modifier.height(8.dp))

        imageUri?.let { uri ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { imagePicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = "Selected Image",
                    modifier = Modifier
                        .fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Add error message for image if exists
        if (imageError != null) {
            Text(
                text = imageError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

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
                    coroutineScope.launch {
                        isLoading = true
                        val newEvent = Event(
                            id = System.currentTimeMillis().toString(),
                            title = eventData.title,
                            description = eventData.description,
                            date = eventData.date,
                            time = eventData.time,
                            registrationDeadline = eventData.registrationDeadline,
                            formLink = eventData.formLink,
                            imageRes = ""
                        )
                        imageUri?.let { uri ->
                            onEventAdded(newEvent, uri)
                        }
                        isLoading = false
                        onDismiss()
                    }
                },
                enabled = eventData.isValid() && imageUri != null && !isLoading && urlError == null && imageError == null
            ) {
                if (isLoading) {
                    SimpleLoadingIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Add Event")
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val localDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        eventData = eventData.copy(date = localDate.format(dateFormatter))
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
            colors = DatePickerDefaults.colors(containerColor = GFGBackground)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(containerColor = GFGBackground)
            )
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val localTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    eventData = eventData.copy(time = localTime.format(timeFormatter))
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(containerColor = GFGBackground)
                )
            },
            containerColor = GFGBackground
        )
    }

    if (showDeadlinePicker) {
        DatePickerDialog(
            onDismissRequest = { showDeadlinePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    deadlinePickerState.selectedDateMillis?.let { millis ->
                        val localDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        eventData = eventData.copy(registrationDeadline = localDate.format(dateFormatter))
                    }
                    showDeadlinePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeadlinePicker = false }) {
                    Text("Cancel")
                }
            },
            colors = DatePickerDefaults.colors(containerColor = GFGBackground)
        ) {
            DatePicker(
                state = deadlinePickerState,
                colors = DatePickerDefaults.colors(containerColor = GFGBackground)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = modifier.fillMaxWidth(),
            singleLine = singleLine,
            readOnly = readOnly,
            trailingIcon = trailingIcon,
            isError = isError
        )
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

data class EventData(
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val time: String = "",
    val registrationDeadline: String = "",
    val formLink: String = ""
) {
    fun isValid(): Boolean {
        return title.isNotBlank() && description.isNotBlank() && date.isNotBlank() &&
                time.isNotBlank() && registrationDeadline.isNotBlank() && formLink.isNotBlank()
    }
}