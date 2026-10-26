package com.example.ui.study

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
import com.example.data.BabelRepository
import com.example.model.StudySession
import com.example.model.StudySubject
import com.example.model.StudyTopic
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    repository: BabelRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val subjects by repository.observeSubjects().collectAsState(initial = emptyList())
    val topics by repository.observeTopics().collectAsState(initial = emptyList())
    val sessions by repository.observeStudySessions().collectAsState(initial = emptyList())

    var selectedTab by remember { mutableStateOf("Timer") } // Timer, Curriculum, Revision, Analytics
    var selectedSubjectId by remember { mutableStateOf<String?>(null) }

    // Focus Timer State
    var isTimerRunning by remember { mutableStateOf(false) }
    var timerSecondsElapsed by remember { mutableLongStateOf(0L) }
    var timerTopicId by remember { mutableStateOf("") }
    var timerSubjectId by remember { mutableStateOf("") }

    // Coroutine effect for running timer
    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning) {
            delay(1000L)
            timerSecondsElapsed += 1
        }
    }

    // Dialog States
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var showAddTopicDialog by remember { mutableStateOf(false) }
    var showManualSessionDialog by remember { mutableStateOf(false) }
    var showSaveTimerDialog by remember { mutableStateOf(false) }
    var selectedTopicForDetail by remember { mutableStateOf<StudyTopic?>(null) }

    val todayDateStr = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.format(Date())
    }

    val totalWeekMinutes = remember(sessions) {
        sessions.sumOf { it.durationMinutes }
    }

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
                        text = "Study Tracker",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = BabelTextPrimary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { showManualSessionDialog = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(BabelDarkSurfaceVariant)
                        ) {
                            Icon(Icons.Default.AddComment, contentDescription = "Log Session", tint = BabelCyan, modifier = Modifier.size(18.dp))
                        }

                        IconButton(
                            onClick = { showAddSubjectDialog = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(BabelCyan)
                                .testTag("add_subject_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Subject", tint = Color(0xFF001F29), modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs: Focus Timer, Curriculum, Revision, Analytics
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BabelDarkSurfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Timer", "Curriculum", "Revision", "Analytics").forEach { tab ->
                        val isSelected = selectedTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) BabelCyan else Color.Transparent)
                                .clickable { selectedTab = tab }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab,
                                color = if (isSelected) Color(0xFF001F29) else BabelTextSecondary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
            when (selectedTab) {
                "Timer" -> {
                    // Focus Timer View
                    item {
                        BabelCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = if (isTimerRunning) BabelCyan else BabelDarkCardBorder
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isTimerRunning) "FOCUS SESSION ACTIVE" else "FOCUS TIMER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isTimerRunning) BabelCyan else BabelTextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                val minutes = timerSecondsElapsed / 60
                                val seconds = timerSecondsElapsed % 60
                                val formattedTime = "%02d:%02d".format(minutes, seconds)

                                Text(
                                    text = formattedTime,
                                    style = MaterialTheme.typography.displayLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BabelTextPrimary
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Select Subject & Topic for Timer
                                if (subjects.isNotEmpty()) {
                                    val currentSubName = subjects.firstOrNull { it.id == timerSubjectId }?.name ?: "Select Subject"
                                    Text(
                                        text = "Target Subject: $currentSubName",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BabelCyan
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(subjects) { sub ->
                                            val isSel = timerSubjectId == sub.id
                                            BabelBadge(
                                                text = sub.name,
                                                color = if (isSel) BabelCyan else BabelTextMuted,
                                                modifier = Modifier.clickable {
                                                    timerSubjectId = sub.id
                                                    val firstTopic = topics.firstOrNull { it.subjectId == sub.id }
                                                    if (firstTopic != null) timerTopicId = firstTopic.id
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Timer Control Buttons
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    if (!isTimerRunning) {
                                        Button(
                                            onClick = {
                                                if (timerSubjectId.isBlank() && subjects.isNotEmpty()) {
                                                    timerSubjectId = subjects.first().id
                                                }
                                                isTimerRunning = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = BabelCyan),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.testTag("start_timer_button")
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF001F29))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (timerSecondsElapsed > 0) "Resume" else "Start Timer", color = Color(0xFF001F29), fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Button(
                                            onClick = { isTimerRunning = false },
                                            colors = ButtonDefaults.buttonColors(containerColor = BabelWarning),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Pause, contentDescription = null, tint = Color(0xFF1E1711))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Pause", color = Color(0xFF1E1711), fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (timerSecondsElapsed > 0) {
                                        Button(
                                            onClick = {
                                                isTimerRunning = false
                                                showSaveTimerDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = BabelDarkSurfaceVariant),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Save, contentDescription = null, tint = BabelCyan)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Save", color = BabelTextPrimary)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                isTimerRunning = false
                                                timerSecondsElapsed = 0L
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Discard", color = BabelError)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }

                    // Recent Sessions List
                    item {
                        BabelSectionHeader(title = "Recent Recorded Sessions")
                        if (sessions.isEmpty()) {
                            BabelCard {
                                Text("No study sessions logged yet. Complete a timer session or log one manually.", style = MaterialTheme.typography.bodySmall, color = BabelTextSecondary)
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                sessions.take(6).forEach { session ->
                                    BabelCard {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = session.subjectName.ifBlank { "Study Session" },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BabelTextPrimary
                                                )
                                                if (session.topicName.isNotBlank()) {
                                                    Text(text = session.topicName, style = MaterialTheme.typography.bodySmall, color = BabelTextSecondary)
                                                }
                                                Text(text = "${session.date} • Method: ${session.studyMethod}", style = MaterialTheme.typography.labelSmall, color = BabelTextMuted)
                                            }
                                            BabelBadge(text = "${session.durationMinutes}m", color = BabelCyan)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "Curriculum" -> {
                    // Subjects & Topics Tree
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("All Subjects", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BabelTextPrimary)
                            TextButton(onClick = { showAddTopicDialog = true }) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = BabelCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Topic", color = BabelCyan, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (subjects.isEmpty()) {
                        item {
                            BabelEmptyState(
                                icon = Icons.Default.MenuBook,
                                title = "No Subjects Yet",
                                description = "Add your core subjects (e.g. ITI Fitter, English, Mathematics) to start tracking topics.",
                                actionLabel = "Add Subject",
                                onAction = { showAddSubjectDialog = true }
                            )
                        }
                    } else {
                        items(subjects, key = { it.id }) { subject ->
                            val subjectTopics = topics.filter { it.subjectId == subject.id }
                            val completedCount = subjectTopics.count { it.status == "Completed" }

                            BabelCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(subject.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BabelTextPrimary)
                                        if (subject.description.isNotBlank()) {
                                            Text(subject.description, style = MaterialTheme.typography.bodySmall, color = BabelTextSecondary)
                                        }
                                    }
                                    BabelBadge(text = "$completedCount / ${subjectTopics.size} Topics", color = BabelCyan)
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                BabelProgressBar(progress = if (subjectTopics.isNotEmpty()) completedCount.toFloat() / subjectTopics.size else 0f)

                                Spacer(modifier = Modifier.height(12.dp))

                                // Topics under this subject
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    subjectTopics.forEach { topic ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(BabelDarkSurfaceVariant)
                                                .clickable { selectedTopicForDetail = topic }
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(
                                                    imageVector = if (topic.status == "Completed") Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                    contentDescription = null,
                                                    tint = if (topic.status == "Completed") BabelSuccess else BabelTextMuted,
                                                    modifier = Modifier.size(16.dp).clickable {
                                                        val nextStatus = if (topic.status == "Completed") "In progress" else "Completed"
                                                        coroutineScope.launch { repository.updateTopic(topic.copy(status = nextStatus)) }
                                                    }
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = topic.name,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = BabelTextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${topic.actualMinutes}m",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = BabelCyan
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                BabelBadge(
                                                    text = topic.priority,
                                                    color = when (topic.priority) { "High" -> BabelError; "Medium" -> BabelWarning; else -> BabelCyan }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "Revision" -> {
                    // Scheduled Revisions
                    item {
                        Text("Spaced Revision Queue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BabelTextPrimary)
                        Text("Topics scheduled for periodic review to lock in knowledge.", style = MaterialTheme.typography.bodySmall, color = BabelTextSecondary)
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    val revisionTopics = topics.filter { it.revisionDate.isNotBlank() }
                    if (revisionTopics.isEmpty()) {
                        item {
                            BabelCard {
                                Text("No topics currently scheduled for revision. Set a revision date on any completed or in-progress topic.", style = MaterialTheme.typography.bodySmall, color = BabelTextSecondary)
                            }
                        }
                    } else {
                        items(revisionTopics, key = { it.id }) { topic ->
                            BabelCard(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(topic.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = BabelTextPrimary)
                                        Text("Scheduled Revision: ${topic.revisionDate}", style = MaterialTheme.typography.labelSmall, color = BabelCyan)
                                    }
                                    Button(
                                        onClick = {
                                            // Advance revision by 7 days
                                            coroutineScope.launch {
                                                repository.updateTopic(topic.copy(revisionDate = "Done"))
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BabelDarkSurfaceVariant),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Mark Reviewed", style = MaterialTheme.typography.labelSmall, color = BabelSuccess)
                                    }
                                }
                            }
                        }
                    }
                }
                "Analytics" -> {
                    // Study Analytics
                    item {
                        Text("Study Metrics & Consistency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BabelTextPrimary)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            BabelMetricCard(
                                title = "Total Studied",
                                value = "${sessions.sumOf { it.durationMinutes }}m",
                                subtitle = "${sessions.size} logged sessions",
                                icon = Icons.Default.HistoryEdu,
                                accentColor = BabelCyan,
                                modifier = Modifier.weight(1f)
                            )
                            BabelMetricCard(
                                title = "Weekly Target",
                                value = "${totalWeekMinutes}m",
                                subtitle = "Target: 900m",
                                icon = Icons.Default.TrackChanges,
                                accentColor = BabelTeal,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        BabelCard {
                            Text("HOURS STUDIED BY SUBJECT", style = MaterialTheme.typography.labelSmall, color = BabelCyan, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(10.dp))

                            subjects.forEach { sub ->
                                val subMinutes = sessions.filter { it.subjectId == sub.id }.sumOf { it.durationMinutes }
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(sub.name, style = MaterialTheme.typography.bodySmall, color = BabelTextPrimary)
                                        Text("${subMinutes}m", style = MaterialTheme.typography.bodySmall, color = BabelCyan, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    BabelProgressBar(progress = (subMinutes / 300f).coerceIn(0f, 1f))
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Add Subject Dialog
    if (showAddSubjectDialog) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var targetMinutes by remember { mutableStateOf("300") }

        AlertDialog(
            onDismissRequest = { showAddSubjectDialog = false },
            title = { Text("New Study Subject", fontWeight = FontWeight.Bold, color = BabelTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Subject Name *") }, modifier = Modifier.fillMaxWidth().testTag("subject_name_input"), singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
                    OutlinedTextField(value = targetMinutes, onValueChange = { targetMinutes = it }, label = { Text("Weekly Target Minutes") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            coroutineScope.launch {
                                repository.addSubject(
                                    StudySubject(
                                        name = name,
                                        description = desc,
                                        weeklyStudyTargetMinutes = targetMinutes.toLongOrNull() ?: 300L,
                                        isActive = true
                                    )
                                )
                                showAddSubjectDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BabelCyan),
                    enabled = name.isNotBlank()
                ) {
                    Text("Add Subject", color = Color(0xFF001F29), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showAddSubjectDialog = false }) { Text("Cancel", color = BabelTextSecondary) } },
            containerColor = BabelDarkSurface
        )
    }

    // Add Topic Dialog
    if (showAddTopicDialog) {
        var subId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: "") }
        var name by remember { mutableStateOf("") }
        var priority by remember { mutableStateOf("Medium") }
        var estMinutes by remember { mutableStateOf("60") }

        AlertDialog(
            onDismissRequest = { showAddTopicDialog = false },
            title = { Text("New Study Topic", fontWeight = FontWeight.Bold, color = BabelTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Topic Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = priority, onValueChange = { priority = it }, label = { Text("Priority (High, Medium, Low)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = estMinutes, onValueChange = { estMinutes = it }, label = { Text("Estimated Minutes") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && subId.isNotBlank()) {
                            coroutineScope.launch {
                                repository.addTopic(
                                    StudyTopic(
                                        subjectId = subId,
                                        name = name,
                                        priority = priority,
                                        estimatedMinutes = estMinutes.toLongOrNull() ?: 60L,
                                        status = "Not started"
                                    )
                                )
                                showAddTopicDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BabelCyan),
                    enabled = name.isNotBlank() && subId.isNotBlank()
                ) {
                    Text("Add Topic", color = Color(0xFF001F29), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showAddTopicDialog = false }) { Text("Cancel", color = BabelTextSecondary) } },
            containerColor = BabelDarkSurface
        )
    }

    // Save Timer Session Dialog
    if (showSaveTimerDialog) {
        val durationMins = (timerSecondsElapsed / 60).coerceAtLeast(1L)
        val selectedSub = subjects.firstOrNull { it.id == timerSubjectId }
        val selectedTop = topics.firstOrNull { it.id == timerTopicId }

        AlertDialog(
            onDismissRequest = { showSaveTimerDialog = false },
            title = { Text("Save Focus Session", fontWeight = FontWeight.Bold, color = BabelTextPrimary) },
            text = {
                Column {
                    Text("Duration: $durationMins minutes", style = MaterialTheme.typography.titleMedium, color = BabelCyan, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Subject: ${selectedSub?.name ?: "General Study"}", style = MaterialTheme.typography.bodySmall, color = BabelTextSecondary)
                    if (selectedTop != null) {
                        Text("Topic: ${selectedTop.name}", style = MaterialTheme.typography.bodySmall, color = BabelTextSecondary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("This session will be recorded and added to your total study metrics.", style = MaterialTheme.typography.bodySmall, color = BabelTextMuted)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.recordStudySession(
                                StudySession(
                                    subjectId = timerSubjectId.ifBlank { subjects.firstOrNull()?.id ?: "general" },
                                    topicId = timerTopicId,
                                    subjectName = selectedSub?.name ?: "Study",
                                    topicName = selectedTop?.name ?: "",
                                    date = todayDateStr,
                                    durationMinutes = durationMins,
                                    studyMethod = "Focus Timer"
                                )
                            )
                            timerSecondsElapsed = 0L
                            showSaveTimerDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BabelCyan)
                ) {
                    Text("Confirm & Save", color = Color(0xFF001F29), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveTimerDialog = false }) { Text("Cancel", color = BabelTextSecondary) }
            },
            containerColor = BabelDarkSurface
        )
    }

    // Manual Log Session Dialog
    if (showManualSessionDialog) {
        var subId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: "") }
        var durationStr by remember { mutableStateOf("30") }
        var method by remember { mutableStateOf("Reading") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showManualSessionDialog = false },
            title = { Text("Log Past Study Session", fontWeight = FontWeight.Bold, color = BabelTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = durationStr, onValueChange = { durationStr = it }, label = { Text("Duration (minutes) *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = method, onValueChange = { method = it }, label = { Text("Study Method (e.g. CBT Practice, Notes)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes / Observations") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mins = durationStr.toLongOrNull() ?: 30L
                        val subName = subjects.firstOrNull { it.id == subId }?.name ?: "Study"
                        coroutineScope.launch {
                            repository.recordStudySession(
                                StudySession(
                                    subjectId = subId.ifBlank { "general" },
                                    subjectName = subName,
                                    date = todayDateStr,
                                    durationMinutes = mins,
                                    studyMethod = method,
                                    notes = notes
                                )
                            )
                            showManualSessionDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BabelCyan)
                ) {
                    Text("Save Session", color = Color(0xFF001F29), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showManualSessionDialog = false }) { Text("Cancel", color = BabelTextSecondary) } },
            containerColor = BabelDarkSurface
        )
    }
}
