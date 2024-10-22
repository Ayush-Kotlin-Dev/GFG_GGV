import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import com.ayush.data.model.Task
import com.ayush.data.model.TaskStatus
import com.ayush.geeksforgeeks.taskdetail.TaskDetailViewModel
import com.ayush.geeksforgeeks.ui.theme.GFGPrimary
import java.text.SimpleDateFormat
import java.util.Locale

data class TaskDetailScreen(
    val taskId: String,
    val onNavigateBack: () -> Unit,
)  : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel: TaskDetailViewModel = hiltViewModel()
        val taskState by viewModel.taskState.collectAsState()

        LaunchedEffect(taskId) {
            viewModel.loadTask(taskId)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Task Details") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { innerPadding ->
            when (val state = taskState) {
                is TaskDetailViewModel.TaskState.Loading -> LoadingScreen()
                is TaskDetailViewModel.TaskState.Error -> ErrorScreen(state.message)
                is TaskDetailViewModel.TaskState.Success -> TaskDetailContent(
                    task = state.task,
                    onStatusUpdate = { newStatus ->
                        viewModel.updateTaskStatus(taskId, newStatus)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = GFGPrimary)
    }
}

@Composable
fun ErrorScreen(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Error: $message", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
fun TaskDetailContent(
    task: Task,
    onStatusUpdate: (TaskStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TaskHeader(task)
        Spacer(modifier = Modifier.height(16.dp))
        TaskDescription(task.description)
        Spacer(modifier = Modifier.height(16.dp))
        TaskMetadata(task)
        Spacer(modifier = Modifier.height(24.dp))
        TaskStatusUpdate(task.status, onStatusUpdate)
    }
}

@Composable
fun TaskHeader(task: Task) {
    Text(
        text = task.title,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Assigned To",
            tint = GFGPrimary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Assigned to: ${task.assignedTo}",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun TaskDescription(description: String) {
    Text(
        text = "Description",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = description, style = MaterialTheme.typography.bodyLarge)
}

@Composable
fun TaskMetadata(task: Task) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Column {
        MetadataItem(Icons.Default.MailOutline, "Domain", "Domain ${task.domainId}")
        MetadataItem(Icons.Default.DateRange, "Due Date", dateFormat.format(task.dueDate.toDate()))
        MetadataItem(Icons.Default.Star, "Credits", "${task.credits} credits")
        MetadataItem(Icons.Default.Refresh, "Last Updated", dateFormat.format(task.updatedAt.toDate()))
    }
}

@Composable
fun MetadataItem(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = GFGPrimary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskStatusUpdate(currentStatus: TaskStatus, onStatusUpdate: (TaskStatus) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val statusOptions = TaskStatus.values()

    Column {
        Text(
            text = "Current Status",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = currentStatus.name,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                statusOptions.forEach { status ->
                    DropdownMenuItem(
                        text = { Text(status.name) },
                        onClick = {
                            onStatusUpdate(status)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}