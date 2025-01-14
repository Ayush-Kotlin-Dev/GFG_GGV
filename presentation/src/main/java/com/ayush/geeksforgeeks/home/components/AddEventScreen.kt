package com.ayush.geeksforgeeks.home.components

import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.font.FontWeight
import com.ayush.geeksforgeeks.ui.theme.GFGBackground
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(
    onEventAdded: suspend (Event, Uri) -> Boolean,
    onDismiss: () -> Unit
) {
    var eventData by remember { mutableStateOf(EventData()) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isAddingEvent by remember { mutableStateOf(false) }
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
                        imageError = "Image size (${String.format(Locale.ROOT, "%.1f", sizeMB)}MB) exceeds limit of 15MB"
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
        Header(onDismiss)

        EventDetailsSection(eventData, onEventDataChange = { eventData = it })
        DateTimeSection(
            eventData = eventData,
            onEventDataChange = { eventData = it },
            showDatePicker = { showDatePicker = true },
            showTimePicker = { showTimePicker = true },
            showDeadlinePicker = { showDeadlinePicker = true }
        )
        FormLinkSection(
            formLink = eventData.formLink,
            onFormLinkChange = {
                eventData = eventData.copy(formLink = it)
                urlError = if (it.isNotBlank() && !validateUrl(it)) {
                    "URL must start with http:// or https://"
                } else null
            },
            error = urlError
        )
        ImageSection(
            imageUri = imageUri,
            onImagePick = { imagePicker.launch("image/*") },
            error = imageError
        )

        ActionButtons(
            isValid = eventData.isValid() && imageUri != null && !isAddingEvent &&
                    urlError == null && imageError == null,
            isLoading = isAddingEvent,
            onCancel = onDismiss,
            onAdd = {
                coroutineScope.launch {
                    isAddingEvent = true
                    imageUri?.let { uri ->
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
                        val success = onEventAdded(newEvent, uri)
                        if (success) {
                            Toast.makeText(context, "Woohoo! Event added successfully!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Oopsie! Something went wrong. give it another shot!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    isAddingEvent = false
                }
            }
        )
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

@Composable
private fun Header(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Add New Event",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun EventDetailsSection(
    eventData: EventData,
    onEventDataChange: (EventData) -> Unit
) {
    SectionTitle("Event Details")
    EventInputField(
        value = eventData.title,
        onValueChange = { onEventDataChange(eventData.copy(title = it)) },
        label = "Title",
        leadingIcon = { Icon(Icons.Default.Event, null) }
    )
    EventInputField(
        value = eventData.description,
        onValueChange = { onEventDataChange(eventData.copy(description = it)) },
        label = "Description",
        singleLine = false,
        minLines = 3,
        leadingIcon = { Icon(Icons.Default.Description, null) }
    )
}

@Composable
private fun DateTimeSection(
    eventData: EventData,
    onEventDataChange: (EventData) -> Unit,
    showDatePicker: () -> Unit,
    showTimePicker: () -> Unit,
    showDeadlinePicker: () -> Unit
) {
    SectionTitle("Date & Time")

    // Event Date and Time in separate rows
    EventInputField(
        value = eventData.date,
        onValueChange = {},
        label = "Event Date",
        readOnly = true,
        leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
        trailingIcon = {
            IconButton(onClick = showDatePicker) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = "Select Date",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    )

    EventInputField(
        value = eventData.time,
        onValueChange = {},
        label = "Event Time",
        readOnly = true,
        leadingIcon = { Icon(Icons.Default.Schedule, null) },
        trailingIcon = {
            IconButton(onClick = showTimePicker) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = "Select Time",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    )

    // Registration Deadline
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Registration Deadline",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            EventInputField(
                value = eventData.registrationDeadline,
                onValueChange = {},
                label = "Select Deadline",
                readOnly = true,
                leadingIcon = { Icon(Icons.Default.Timer, null) },
                trailingIcon = {
                    IconButton(onClick = showDeadlinePicker) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "Select Registration Deadline",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FormLinkSection(
    formLink: String,
    onFormLinkChange: (String) -> Unit,
    error: String?
) {
    SectionTitle("Form Link")
    EventInputField(
        value = formLink,
        onValueChange = onFormLinkChange,
        label = "Form Link",
        leadingIcon = { Icon(Icons.Default.Link, null) },
        isError = error != null,
        errorMessage = error,
        singleLine = true
    )
}

@Composable
private fun ImageSection(
    imageUri: Uri?,
    onImagePick: () -> Unit,
    error: String?
) {
    SectionTitle("Event Image")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(onClick = onImagePick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (imageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(imageUri),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.AddPhotoAlternate,
                        null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Click to add image",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
    if (error != null) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun ActionButtons(
    isValid: Boolean,
    isLoading: Boolean,
    onCancel: () -> Unit,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f),
            enabled = !isLoading
        ) {
            Text("Cancel")
        }
        Button(
            onClick = onAdd,
            modifier = Modifier.weight(1f),
            enabled = isValid && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Add Event")
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
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
    leadingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    minLines: Int = 1
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            readOnly = readOnly,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            isError = isError,
            minLines = minLines,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(8.dp)
        )
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
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