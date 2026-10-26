package com.example.ui.goals

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BabelRepository
import com.example.model.GoalItem
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    repository: BabelRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val goals by repository.observeGoals().collectAsState(initial = emptyList())
    val profile by repository.observeUserProfile().collectAsState(initial = null)
    val currency = profile?.currency ?: "INR"

    val todayStr = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.format(Date())
    }

    var showAddGoalDialog by remember { mutableStateOf(false) }
    var goalToEdit by remember { mutableStateOf<GoalItem?>(null) }

    Scaffold(
        containerColor = BabelDarkBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Goals & Milestones",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = BabelTextPrimary
                    )
                    Text(
                        text = "Track your long-term objectives and actionable steps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BabelTextSecondary
                    )
                }

                IconButton(
                    onClick = { showAddGoalDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BabelCyan)
                        .testTag("add_goal_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Goal", tint = Color(0xFF001F29))
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
            if (goals.isEmpty()) {
                item {
                    BabelEmptyState(
                        icon = Icons.Default.TrackChanges,
                        title = "No Goals Active",
                        description = "Define milestones for your career, studies, or savings to stay disciplined and measure progress.",
                        actionLabel = "Create First Goal",
                        onAction = { showAddGoalDialog = true }
                    )
                }
            } else {
                items(goals, key = { it.id }) { goal ->
                    val isOverdue = goal.targetDate.isNotBlank() && goal.targetDate < todayStr && !goal.isCompleted

                    BabelCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        borderColor = if (goal.isCompleted) BabelSuccess.copy(alpha = 0.5f) else if (isOverdue) BabelWarning.copy(alpha = 0.5f) else BabelDarkCardBorder
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = goal.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BabelTextPrimary
                                )
                                if (goal.description.isNotBlank()) {
                                    Text(
                                        text = goal.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BabelTextSecondary
                                    )
                                }
                            }
                            BabelBadge(
                                text = goal.category,
                                color = when (goal.category) {
                                    "Financial" -> BabelSuccess
                                    "Education" -> BabelCyan
                                    else -> BabelPurple
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress Metric
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (goal.targetAmount > 0) "$currency ${"%,.0f".format(goal.currentAmount)} / $currency ${"%,.0f".format(goal.targetAmount)}"
                                else "Progress: ${goal.progressPercent}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = BabelTextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${goal.progressPercent}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (goal.isCompleted) BabelSuccess else BabelCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        BabelProgressBar(
                            progress = goal.progressPercent / 100f,
                            color = if (goal.isCompleted) BabelSuccess else BabelCyan
                        )

                        if (goal.milestones.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Milestones:", style = MaterialTheme.typography.labelSmall, color = BabelCyan, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            goal.milestones.forEach { ms ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = BabelCyan, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = ms, style = MaterialTheme.typography.bodySmall, color = BabelTextSecondary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Footer with Dates & Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (goal.targetDate.isNotBlank()) {
                                Text(
                                    text = if (isOverdue) "Overdue (was due ${goal.targetDate})" else "Target: ${goal.targetDate}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isOverdue) BabelWarning else BabelTextMuted
                                )
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            Row {
                                TextButton(
                                    onClick = {
                                        val newPercent = if (goal.progressPercent >= 100L) 0L else (goal.progressPercent + 25L).coerceAtMost(100L)
                                        coroutineScope.launch {
                                            repository.updateGoal(goal.copy(progressPercent = newPercent, isCompleted = newPercent >= 100L))
                                        }
                                    }
                                ) {
                                    Text(if (goal.isCompleted) "Reopen" else "+25% Progress", color = BabelCyan, style = MaterialTheme.typography.labelSmall)
                                }

                                IconButton(onClick = { goalToEdit = goal }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = BabelTextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Add or Edit Goal Dialog
    if (showAddGoalDialog || goalToEdit != null) {
        val editing = goalToEdit
        var title by remember { mutableStateOf(editing?.title ?: "") }
        var description by remember { mutableStateOf(editing?.description ?: "") }
        var category by remember { mutableStateOf(editing?.category ?: "Personal development") }
        var targetAmountStr by remember { mutableStateOf(if ((editing?.targetAmount ?: 0.0) > 0) editing!!.targetAmount.toString() else "") }
        var targetDate by remember { mutableStateOf(editing?.targetDate ?: "") }
        var milestone1 by remember { mutableStateOf(editing?.milestones?.getOrNull(0) ?: "") }
        var milestone2 by remember { mutableStateOf(editing?.milestones?.getOrNull(1) ?: "") }

        AlertDialog(
            onDismissRequest = {
                showAddGoalDialog = false
                goalToEdit = null
            },
            title = { Text(if (editing != null) "Edit Goal" else "Create Goal", fontWeight = FontWeight.Bold, color = BabelTextPrimary) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Goal Title *") }, modifier = Modifier.fillMaxWidth().testTag("goal_title_input"), singleLine = true)
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (Education, Financial, Career, etc.)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = targetAmountStr, onValueChange = { targetAmountStr = it }, label = { Text("Target Amount ($currency) - optional") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = targetDate, onValueChange = { targetDate = it }, label = { Text("Target Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = milestone1, onValueChange = { milestone1 = it }, label = { Text("Milestone 1") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = milestone2, onValueChange = { milestone2 = it }, label = { Text("Milestone 2") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val milestonesList = listOf(milestone1, milestone2).filter { it.isNotBlank() }
                            val targetAmt = targetAmountStr.toDoubleOrNull() ?: 0.0
                            val goal = (editing ?: GoalItem()).copy(
                                title = title,
                                description = description,
                                category = category,
                                targetAmount = targetAmt,
                                targetDate = targetDate,
                                milestones = milestonesList
                            )
                            coroutineScope.launch {
                                if (editing != null) {
                                    repository.updateGoal(goal)
                                } else {
                                    repository.addGoal(goal)
                                }
                                showAddGoalDialog = false
                                goalToEdit = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BabelCyan),
                    enabled = title.isNotBlank()
                ) {
                    Text("Save Goal", color = Color(0xFF001F29), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddGoalDialog = false
                    goalToEdit = null
                }) {
                    Text("Cancel", color = BabelTextSecondary)
                }
            },
            containerColor = BabelDarkSurface
        )
    }
}
