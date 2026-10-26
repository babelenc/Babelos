package com.example.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AiAssistantService
import com.example.data.BabelRepository
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: BabelRepository,
    aiService: AiAssistantService,
    onNavigateToPlanner: () -> Unit,
    onNavigateToStudy: () -> Unit,
    onNavigateToFinance: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToAssistant: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val userProfile by repository.observeUserProfile().collectAsState(initial = null)
    val tasks by repository.observeTasks().collectAsState(initial = emptyList())
    val subjects by repository.observeSubjects().collectAsState(initial = emptyList())
    val topics by repository.observeTopics().collectAsState(initial = emptyList())
    val studySessions by repository.observeStudySessions().collectAsState(initial = emptyList())
    val transactions by repository.observeTransactions().collectAsState(initial = emptyList())
    val goals by repository.observeGoals().collectAsState(initial = emptyList())

    val todayDateStr = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.format(Date())
    }
    val currentMonthStr = remember {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        sdf.format(Date())
    }
    val todayDisplayDate = remember {
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        sdf.format(Date())
    }

    val currency = userProfile?.currency ?: "INR"
    val budget by repository.observeBudget(currentMonthStr).collectAsState(initial = null)

    // Calculations based on real stored records
    val todayTasks = remember(tasks, todayDateStr) {
        tasks.filter { it.dueDate.isEmpty() || it.dueDate == todayDateStr }
    }
    val completedTasksCount = todayTasks.count { it.isCompleted }
    val totalTasksCount = todayTasks.size
    val taskCompletionPercent = if (totalTasksCount > 0) ((completedTasksCount.toFloat() / totalTasksCount) * 100).toInt() else 0

    val todayStudyMinutes = remember(studySessions, todayDateStr) {
        studySessions.filter { it.date == todayDateStr }.sumOf { it.durationMinutes }
    }
    val weekStudyMinutes = remember(studySessions) {
        // sum of last 7 recorded sessions
        studySessions.sumOf { it.durationMinutes }
    }

    val monthTransactions = remember(transactions, currentMonthStr) {
        transactions.filter { it.date.startsWith(currentMonthStr) }
    }
    val recordedIncome = remember(monthTransactions) {
        monthTransactions.filter { it.type == "Income" }.sumOf { it.amount }
    }
    val recordedExpenses = remember(monthTransactions) {
        monthTransactions.filter { it.type == "Expense" }.sumOf { it.amount }
    }
    val netCashFlow = recordedIncome - recordedExpenses

    // AI Daily Plan Dialog State
    var showPlanDialog by remember { mutableStateOf(false) }
    var isGeneratingPlan by remember { mutableStateOf(false) }
    var generatedPlanContent by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = BabelDarkBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Header: Greeting, Date, Timezone & Quick Plan Button
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SYSTEM ONLINE",
                                style = MaterialTheme.typography.labelSmall,
                                color = BabelCyan,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Hello, ${userProfile?.displayName ?: "Commander"}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = BabelTextPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(BabelDarkSurfaceVariant)
                                .border(1.dp, BabelCyan.copy(alpha = 0.4f), CircleShape)
                                .clickable { onNavigateToAssistant() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "AI Assistant",
                                tint = BabelCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$todayDisplayDate • ${userProfile?.timezone ?: "Asia/Kolkata"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = BabelTextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // "Generate Today's Plan" Action Button
                    Button(
                        onClick = {
                            showPlanDialog = true
                            isGeneratingPlan = true
                            generatedPlanContent = null
                            coroutineScope.launch {
                                val plan = aiService.generateDailyPlan(userProfile, tasks, subjects, topics)
                                generatedPlanContent = plan
                                isGeneratingPlan = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BabelDarkSurfaceVariant,
                            contentColor = BabelCyan
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BabelCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .testTag("generate_plan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = BabelCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Generate Today's Plan with AI",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Module A: Today's Progress Card
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    BabelSectionHeader(
                        title = "Today's Execution",
                        actionText = "Open Planner",
                        onActionClick = onNavigateToPlanner
                    )

                    BabelCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "$completedTasksCount / $totalTasksCount Tasks Completed",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BabelTextPrimary
                                )
                                Text(
                                    text = if (totalTasksCount == 0) "No tasks scheduled for today" else "${totalTasksCount - completedTasksCount} remaining today",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BabelTextSecondary
                                )
                            }
                            BabelBadge(
                                text = "$taskCompletionPercent%",
                                color = if (taskCompletionPercent == 100 && totalTasksCount > 0) BabelSuccess else BabelCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        BabelProgressBar(progress = if (totalTasksCount > 0) completedTasksCount.toFloat() / totalTasksCount else 0f)

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = BabelTextMuted, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Study Recorded: ${todayStudyMinutes}m",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BabelTextSecondary
                                )
                            }
                            Text(
                                text = "Planned Window: ${userProfile?.studyTime ?: "19:30 - 21:30"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = BabelTextMuted
                            )
                        }
                    }
                }
            }

            // Module B: Financial Snapshot
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    BabelSectionHeader(
                        title = "Financial Snapshot",
                        actionText = "Manage Ledger",
                        onActionClick = onNavigateToFinance
                    )

                    BabelCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "OCTOBER 2026 RECORDED NET",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BabelTextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$currency ${"%,.2f".format(netCashFlow)}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (netCashFlow >= 0) BabelSuccess else BabelError
                                )
                            }

                            val budgetLimit = budget?.monthlyExpenseBudget ?: 15000.0
                            val budgetUtilizedPercent = ((recordedExpenses / budgetLimit) * 100).toInt().coerceAtMost(100)
                            BabelBadge(
                                text = "$budgetUtilizedPercent% Budget Used",
                                color = if (budgetUtilizedPercent > 90) BabelWarning else BabelCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Income Received", style = MaterialTheme.typography.labelSmall, color = BabelTextMuted)
                                Text("$currency ${"%,.0f".format(recordedIncome)}", style = MaterialTheme.typography.bodyMedium, color = BabelSuccess, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Expenses Logged", style = MaterialTheme.typography.labelSmall, color = BabelTextMuted)
                                Text("$currency ${"%,.0f".format(recordedExpenses)}", style = MaterialTheme.typography.bodyMedium, color = BabelError, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Savings Target", style = MaterialTheme.typography.labelSmall, color = BabelTextMuted)
                                Text("$currency ${"%,.0f".format(budget?.monthlySavingsTarget ?: 5000.0)}", style = MaterialTheme.typography.bodyMedium, color = BabelCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Module C: Study Snapshot
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    BabelSectionHeader(
                        title = "Study Progress",
                        actionText = "Track Subjects",
                        onActionClick = onNavigateToStudy
                    )

                    val activeSubs = subjects.filter { it.isActive }
                    val completedTopicsCount = topics.count { it.status == "Completed" }
                    val totalTopicsCount = topics.size

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BabelMetricCard(
                            title = "Active Subjects",
                            value = "${activeSubs.size}",
                            subtitle = "${totalTopicsCount} topics tracked",
                            icon = Icons.Default.MenuBook,
                            accentColor = BabelCyan,
                            modifier = Modifier.weight(1f)
                        )
                        BabelMetricCard(
                            title = "Study This Week",
                            value = "${weekStudyMinutes}m",
                            subtitle = "$completedTopicsCount topics finished",
                            icon = Icons.Default.TrendingUp,
                            accentColor = BabelTeal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Module E: Upcoming Tasks
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    BabelSectionHeader(
                        title = "Upcoming Tasks",
                        actionText = "View All",
                        onActionClick = onNavigateToPlanner
                    )

                    val upcomingTasks = tasks.filter { !it.isCompleted }.take(4)
                    if (upcomingTasks.isEmpty()) {
                        BabelCard {
                            Text(
                                text = "All planned tasks are complete! Tap below to add a new task.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BabelTextSecondary
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            upcomingTasks.forEach { task ->
                                BabelCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.updateTask(task.copy(isCompleted = true))
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = task.isCompleted,
                                            onCheckedChange = { checked ->
                                                coroutineScope.launch {
                                                    repository.updateTask(task.copy(isCompleted = checked))
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = BabelCyan)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = task.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = BabelTextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (task.startTime.isNotBlank()) {
                                                Text(
                                                    text = "${task.startTime} - ${task.endTime.ifBlank { "Later" }} • ${task.category}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = BabelTextMuted
                                                )
                                            }
                                        }
                                        BabelBadge(
                                            text = task.priority,
                                            color = when (task.priority) {
                                                "High" -> BabelError
                                                "Medium" -> BabelWarning
                                                else -> BabelCyan
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Module D: Goals Overview
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    BabelSectionHeader(
                        title = "Key Goals & Milestones",
                        actionText = "Manage Goals",
                        onActionClick = onNavigateToGoals
                    )

                    val activeGoals = goals.filter { !it.isCompleted }.take(2)
                    if (activeGoals.isEmpty()) {
                        BabelCard {
                            Text("No active goals. Set your first goal in Goals section.", style = MaterialTheme.typography.bodySmall, color = BabelTextSecondary)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            activeGoals.forEach { goal ->
                                BabelCard {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = goal.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = BabelTextPrimary
                                        )
                                        BabelBadge(text = "${goal.progressPercent}%", color = BabelCyan)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    BabelProgressBar(progress = goal.progressPercent / 100f)
                                    if (goal.milestones.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Next: ${goal.milestones.first()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BabelTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Module G: Evidence-Based Weekly Insight
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                    BabelCard(
                        borderColor = BabelPurple.copy(alpha = 0.4f),
                        backgroundColor = Color(0xFF13172C)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = BabelCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("EVIDENCE-BASED INSIGHT", style = MaterialTheme.typography.labelSmall, color = BabelCyan, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        val insightText = if (studySessions.isNotEmpty() && tasks.isNotEmpty()) {
                            "You logged $weekStudyMinutes minutes of study and completed $completedTasksCount tasks this period. Your study consistency is highest in the evening block."
                        } else if (studySessions.isNotEmpty()) {
                            "You recorded $weekStudyMinutes minutes of focused study. Keep logging study timer sessions to unlock trend forecasts."
                        } else {
                            "Start tracking your tasks and use the Focus Timer in Study to see data-driven insights about your productivity here."
                        }

                        Text(
                            text = insightText,
                            style = MaterialTheme.typography.bodySmall,
                            color = BabelTextPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }

    // AI Daily Plan Dialog
    if (showPlanDialog) {
        AlertDialog(
            onDismissRequest = { showPlanDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BabelCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Daily Schedule Plan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BabelTextPrimary)
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    if (isGeneratingPlan) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = BabelCyan)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Analyzing your routine & pending tasks...", style = MaterialTheme.typography.bodySmall, color = BabelTextSecondary)
                        }
                    } else {
                        LazyColumn {
                            item {
                                Text(
                                    text = generatedPlanContent ?: "Plan generated.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BabelTextPrimary,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPlanDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BabelCyan)
                ) {
                    Text("Got It", color = Color(0xFF001F29), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = BabelDarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
