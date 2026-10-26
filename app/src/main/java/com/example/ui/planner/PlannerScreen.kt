package com.example.ui.planner

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BabelRepository
import com.example.model.TaskItem
import com.example.ui.components.BabelBadge
import com.example.ui.components.BabelCard
import com.example.ui.components.BabelEmptyState
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    repository: BabelRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val tasks by repository.observeTasks().collectAsState(initial = emptyList())

    var selectedView by remember { mutableStateOf("Daily") } // Daily, Weekly, Calendar
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskItem?>(null) }
    var taskToDelete by remember { mutableStateOf<TaskItem?>(null) }

    val todayStr = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.format(Date())
    }

    // Filter tasks
    val filteredTasks = remember(tasks, selectedCategoryFilter, searchQuery) {
        tasks.filter { task ->
            val matchCategory = selectedCategoryFilter == "All" || task.category == selectedCategoryFilter
            val matchSearch = searchQuery.isBlank() || task.title.contains(searchQuery, ignoreCase = true) || task.description.contains(searchQuery, ignoreCase = true)
            matchCategory && matchSearch
        }.sortedWith(
            compareBy<TaskItem> { it.isCompleted }
                .thenByDescending { when (it.priority) { "High" -> 3; "Medium" -> 2; else -> 1 } }
                .thenBy { it.startTime.ifBlank { "99:99" } }
        )
    }

    val overdueTasks = remember(tasks, todayStr) {
        tasks.filter { !it.isCompleted && it.dueDate.isNotBlank() && it.dueDate < todayStr }
    }

    val categories = listOf("All", "Work", "Study", "Finance", "Personal", "Health", "Household", "Other")

    Scaffold(
        containerColor = BabelDarkBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daily Planner",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = BabelTextPrimary
                    )
                    IconButton(
                        onClick = { showAddTaskDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(BabelCyan)
                            .testTag("add_task_fab")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Task", tint = Color(0xFF001F29))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // View Selector (Daily, Weekly, Calendar)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BabelDarkSurfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Daily", "Weekly", "Calendar").forEach { viewName ->
                        val isSelected = selectedView == viewName
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) BabelCyan else Color.Transparent)
                                .clickable { selectedView = viewName }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = viewName,
                                color = if (isSelected) Color(0xFF001F29) else BabelTextSecondary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search tasks...", color = BabelTextMuted, style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BabelTextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BabelDarkSurface,
                        unfocusedContainerColor = BabelDarkSurface,
                        focusedBorderColor = BabelCyan,
                        unfocusedBorderColor = BabelDarkCardBorder
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category Filter Pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSelected = selectedCategoryFilter == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) BabelCyan.copy(alpha = 0.2f) else BabelDarkSurface)
                                .border(1.dp, if (isSelected) BabelCyan else BabelDarkCardBorder, RoundedCornerShape(8.dp))
                                .clickable { selectedCategoryFilter = cat }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) BabelCyan else BabelTextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Overdue Tasks Banner if any
            if (overdueTasks.isNotEmpty()) {
                item {
                    BabelCard(
                        borderColor = BabelWarning.copy(alpha = 0.5f),
                        backgroundColor = Color(0xFF1E1711),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = BabelWarning, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${overdueTasks.size} tasks overdue",
                                style = MaterialTheme.typography.titleSmall,
                                color = BabelWarning,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Review and reschedule or mark them complete to keep your daily timeline accurate.",
                            style = MaterialTheme.typography.bodySmall,
                            color = BabelTextSecondary
                        )
                    }
                }
            }

            when (selectedView) {
                "Daily" -> {
                    // Chronological Timeline View
                    if (filteredTasks.isEmpty()) {
                        item {
                            BabelEmptyState(
                                icon = Icons.Default.EventNote,
                                title = "No Tasks Found",
                                description = "You have no tasks matching this filter. Tap '+' to create your first scheduled task.",
                                actionLabel = "Add Task",
                                onAction = { showAddTaskDialog = true }
                            )
                        }
                    } else {
                        items(filteredTasks, key = { it.id }) { task ->
                            PlannerTaskItemCard(
                                task = task,
                                onToggleComplete = {
                                    coroutineScope.launch {
                                        repository.updateTask(task.copy(isCompleted = !task.isCompleted))
                                    }
                                },
                                onEdit = { taskToEdit = task },
                                onDelete = { taskToDelete = task }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
                "Weekly" -> {
                    // 7-Day Overview
                    item {
                        WeeklyOverviewCard(tasks = tasks, todayStr = todayStr)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    items(filteredTasks, key = { it.id }) { task ->
                        PlannerTaskItemCard(
                            task = task,
                            onToggleComplete = {
                                coroutineScope.launch {
                                    repository.updateTask(task.copy(isCompleted = !task.isCompleted))
                                }
                            },
                            onEdit = { taskToEdit = task },
                            onDelete = { taskToDelete = task }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
                "Calendar" -> {
                    // Date Navigation & Scheduled view
                    item {
                        CalendarViewHeader(todayStr = todayStr)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    items(filteredTasks, key = { it.id }) { task ->
                        PlannerTaskItemCard(
                            task = task,
                            onToggleComplete = {
                                coroutineScope.launch {
                                    repository.updateTask(task.copy(isCompleted = !task.isCompleted))
                                }
                            },
                            onEdit = { taskToEdit = task },
                            onDelete = { taskToDelete = task }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Add or Edit Task Dialog
    if (showAddTaskDialog || taskToEdit != null) {
        val editing = taskToEdit
        TaskEditDialog(
            initialTask = editing,
            onDismiss = {
                showAddTaskDialog = false
                taskToEdit = null
            },
            onSave = { updatedTask ->
                coroutineScope.launch {
                    if (editing != null) {
                        repository.updateTask(updatedTask)
                    } else {
                        repository.addTask(updatedTask)
                    }
                    showAddTaskDialog = false
                    taskToEdit = null
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (taskToDelete != null) {
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete Task?", color = BabelTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${taskToDelete?.title}'? This action cannot be undone.", color = BabelTextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = taskToDelete?.id
                        if (!id.isNullOrBlank()) {
                            coroutineScope.launch { repository.deleteTask(id) }
                        }
                        taskToDelete = null
                    }
                ) {
                    Text("Delete", color = BabelError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancel", color = BabelTextSecondary)
                }
            },
            containerColor = BabelDarkSurface
        )
    }
}

@Composable
fun PlannerTaskItemCard(
    task: TaskItem,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    BabelCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (task.isCompleted) BabelDarkCardBorder else BabelDarkCardBorder.copy(alpha = 0.8f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() },
                colors = CheckboxDefaults.colors(checkedColor = BabelCyan),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (task.isCompleted) BabelTextMuted else BabelTextPrimary,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = BabelTextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (task.startTime.isNotBlank()) {
                        Text(
                            text = "${task.startTime} (${task.estimatedDuration})",
                            style = MaterialTheme.typography.labelSmall,
                            color = BabelCyan
                        )
                        Text(text = " • ", color = BabelTextMuted)
                    }
                    Text(
                        text = task.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = BabelTextSecondary
                    )
                    if (task.dueDate.isNotBlank()) {
                        Text(text = " • Due ${task.dueDate}", style = MaterialTheme.typography.labelSmall, color = BabelTextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            BabelBadge(
                text = task.priority,
                color = when (task.priority) {
                    "High" -> BabelError
                    "Medium" -> BabelWarning
                    else -> BabelCyan
                }
            )

            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = BabelTextSecondary, modifier = Modifier.size(16.dp))
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = BabelTextMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun WeeklyOverviewCard(tasks: List<TaskItem>, todayStr: String) {
    BabelCard {
        Text("7-DAY SCHEDULE OVERVIEW", style = MaterialTheme.typography.labelSmall, color = BabelCyan, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            days.forEachIndexed { idx, day ->
                val count = tasks.count { !it.isCompleted } // sample grouping
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(day, style = MaterialTheme.typography.labelSmall, color = BabelTextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (idx == 0) BabelCyan.copy(alpha = 0.2f) else BabelDarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$count",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (idx == 0) BabelCyan else BabelTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarViewHeader(todayStr: String) {
    BabelCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = BabelCyan, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Today: $todayStr", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BabelTextPrimary)
            }
            BabelBadge(text = "Active Day", color = BabelCyan)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditDialog(
    initialTask: TaskItem?,
    onDismiss: () -> Unit,
    onSave: (TaskItem) -> Unit
) {
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var description by remember { mutableStateOf(initialTask?.description ?: "") }
    var category by remember { mutableStateOf(initialTask?.category ?: "Work") }
    var priority by remember { mutableStateOf(initialTask?.priority ?: "Medium") }
    var dueDate by remember { mutableStateOf(initialTask?.dueDate ?: "") }
    var startTime by remember { mutableStateOf(initialTask?.startTime ?: "") }
    var endTime by remember { mutableStateOf(initialTask?.endTime ?: "") }
    var estimatedDuration by remember { mutableStateOf(initialTask?.estimatedDuration ?: "30m") }
    var recurrence by remember { mutableStateOf(initialTask?.recurrence ?: "None") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialTask != null) "Edit Task" else "Create Task",
                fontWeight = FontWeight.Bold,
                color = BabelTextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    modifier = Modifier.fillMaxWidth().testTag("task_title_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                // Category & Priority
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = priority,
                        onValueChange = { priority = it },
                        label = { Text("Priority") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Timing
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start (e.g. 10:00)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = estimatedDuration,
                        onValueChange = { estimatedDuration = it },
                        label = { Text("Duration (e.g. 45m)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val task = (initialTask ?: TaskItem()).copy(
                            title = title,
                            description = description,
                            category = category,
                            priority = priority,
                            dueDate = dueDate,
                            startTime = startTime,
                            endTime = endTime,
                            estimatedDuration = estimatedDuration,
                            recurrence = recurrence
                        )
                        onSave(task)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BabelCyan),
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("save_task_button")
            ) {
                Text("Save Task", color = Color(0xFF001F29), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = BabelTextSecondary)
            }
        },
        containerColor = BabelDarkSurface
    )
}
