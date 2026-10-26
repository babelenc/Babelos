package com.example.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.example.model.FinancialBudget
import com.example.model.UserProfile
import com.example.ui.components.BabelCard
import com.example.ui.components.BabelProgressBar
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    repository: BabelRepository,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var currentStep by remember { mutableStateOf(1) } // 1 to 5
    var isSaving by remember { mutableStateOf(false) }

    // Step 1: Basic Profile
    var displayName by remember { mutableStateOf("User") }
    var preferredLanguage by remember { mutableStateOf("English") }
    var preferredTheme by remember { mutableStateOf("Dark") }
    var currency by remember { mutableStateOf("INR") }
    var timezone by remember { mutableStateOf("Asia/Kolkata") }

    // Step 2: Daily Routine
    var wakeTime by remember { mutableStateOf("06:30") }
    var sleepTime by remember { mutableStateOf("23:00") }
    var workSchedule by remember { mutableStateOf("09:00 - 18:00") }
    var studyTime by remember { mutableStateOf("19:30 - 21:30") }
    var breakTimes by remember { mutableStateOf("13:00 - 14:00") }
    var weeklyRestDay by remember { mutableStateOf("Sunday") }

    // Step 3: Financial Setup
    var expectedIncome by remember { mutableStateOf("25000") }
    var savingsTarget by remember { mutableStateOf("5000") }
    var startingCash by remember { mutableStateOf("2000") }

    // Step 4: Study Subjects
    var includeItiFitter by remember { mutableStateOf(true) }
    var includeEnglish by remember { mutableStateOf(true) }
    var includeMath by remember { mutableStateOf(true) }

    // Step 5: Goals
    var goalSaveMoney by remember { mutableStateOf(true) }
    var goalStudyConsistency by remember { mutableStateOf(true) }
    var goalEnglishHabit by remember { mutableStateOf(true) }

    val finishOnboarding = {
        isSaving = true
        coroutineScope.launch {
            val profile = UserProfile(
                displayName = displayName.ifBlank { "User" },
                preferredLanguage = preferredLanguage,
                preferredTheme = preferredTheme,
                currency = currency,
                timezone = timezone,
                wakeTime = wakeTime,
                sleepTime = sleepTime,
                workSchedule = workSchedule,
                studyTime = studyTime,
                breakTimes = breakTimes,
                weeklyRestDay = weeklyRestDay,
                onboardingCompleted = true
            )
            repository.saveUserProfile(profile)

            val starterSubjects = mutableListOf<String>()
            if (includeItiFitter) starterSubjects.add("ITI Fitter")
            if (includeEnglish) starterSubjects.add("English")
            if (includeMath) starterSubjects.add("Mathematics")

            val cashAmount = startingCash.toDoubleOrNull() ?: 2000.0
            repository.seedStarterDataIfEmpty(starterSubjects, cashAmount, currency)

            // Seed budget
            val budgetTarget = savingsTarget.toDoubleOrNull() ?: 5000.0
            val monthlyExpenseEstimate = expectedIncome.toDoubleOrNull()?.minus(budgetTarget)?.coerceAtLeast(5000.0) ?: 15000.0
            repository.saveBudget(
                FinancialBudget(
                    monthlyExpenseBudget = monthlyExpenseEstimate,
                    monthlySavingsTarget = budgetTarget,
                    monthYear = "2026-10"
                )
            )

            isSaving = false
            onCompleted()
        }
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
                        text = "System Setup • Step $currentStep of 5",
                        style = MaterialTheme.typography.labelMedium,
                        color = BabelCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(
                        onClick = { finishOnboarding() },
                        enabled = !isSaving
                    ) {
                        Text("Skip All", color = BabelTextSecondary, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                BabelProgressBar(progress = currentStep / 5f)
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentStep > 1) {
                    OutlinedButton(
                        onClick = { currentStep -= 1 },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back", color = BabelTextPrimary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Button(
                    onClick = {
                        if (currentStep < 5) {
                            currentStep += 1
                        } else {
                            finishOnboarding()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BabelCyan),
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("onboarding_next_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF001F29))
                    } else {
                        Text(
                            text = if (currentStep == 5) "Launch BabelOS" else "Continue",
                            color = Color(0xFF001F29),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            when (currentStep) {
                1 -> {
                    // Step 1: Basic Profile
                    Text("Welcome to BabelOS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = BabelTextPrimary)
                    Text("Configure your personal identity and system preferences.", style = MaterialTheme.typography.bodyMedium, color = BabelTextSecondary)
                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth().testTag("onboarding_name_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Preferred Language", style = MaterialTheme.typography.labelMedium, color = BabelTextSecondary)
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LanguageChip("English", preferredLanguage == "English") { preferredLanguage = "English" }
                        LanguageChip("Hinglish", preferredLanguage == "Hinglish") { preferredLanguage = "Hinglish" }
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it },
                        label = { Text("Currency (e.g. INR, USD, EUR)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = timezone,
                        onValueChange = { timezone = it },
                        label = { Text("Time Zone") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                2 -> {
                    // Step 2: Daily Routine
                    Text("Your Daily Routine", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = BabelTextPrimary)
                    Text("BabelOS uses these windows to plan realistic days around your commitments.", style = MaterialTheme.typography.bodyMedium, color = BabelTextSecondary)
                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = wakeTime,
                        onValueChange = { wakeTime = it },
                        label = { Text("Normal Wake-up Time (e.g. 06:30)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = sleepTime,
                        onValueChange = { sleepTime = it },
                        label = { Text("Normal Sleep Time (e.g. 23:00)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = workSchedule,
                        onValueChange = { workSchedule = it },
                        label = { Text("Work / College Schedule (e.g. 09:00 - 18:00)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = studyTime,
                        onValueChange = { studyTime = it },
                        label = { Text("Preferred Study Window (e.g. 19:30 - 21:30)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = weeklyRestDay,
                        onValueChange = { weeklyRestDay = it },
                        label = { Text("Weekly Rest Day (e.g. Sunday)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                3 -> {
                    // Step 3: Financial Setup
                    Text("Financial Planning Setup", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = BabelTextPrimary)
                    Text("Manual tracking without bank credentials. Expected income is for planning estimates only.", style = MaterialTheme.typography.bodyMedium, color = BabelTextSecondary)
                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = expectedIncome,
                        onValueChange = { expectedIncome = it },
                        label = { Text("Expected Monthly Income ($currency)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = savingsTarget,
                        onValueChange = { savingsTarget = it },
                        label = { Text("Monthly Savings Target ($currency)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = startingCash,
                        onValueChange = { startingCash = it },
                        label = { Text("Opening Cash / Bank Balance ($currency)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                4 -> {
                    // Step 4: Study Setup
                    Text("Study Modules & Subjects", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = BabelTextPrimary)
                    Text("Select starter subjects to bootstrap your curriculum (fully customizable later).", style = MaterialTheme.typography.bodyMedium, color = BabelTextSecondary)
                    Spacer(modifier = Modifier.height(20.dp))

                    SubjectCheckboxCard(
                        title = "ITI Fitter",
                        desc = "Workshop tools, measuring instruments, engineering drawing, calculations, trade theory & CBT practice",
                        checked = includeItiFitter,
                        onCheckedChange = { includeItiFitter = it }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SubjectCheckboxCard(
                        title = "English Communication",
                        desc = "Vocabulary, essential grammar, reading comprehension, writing & speaking practice",
                        checked = includeEnglish,
                        onCheckedChange = { includeEnglish = it }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SubjectCheckboxCard(
                        title = "Applied Mathematics",
                        desc = "Arithmetic, tables, fractions, percentages, ratio & proportion, algebra, speed-distance",
                        checked = includeMath,
                        onCheckedChange = { includeMath = it }
                    )
                }
                5 -> {
                    // Step 5: Review & Goals
                    Text("Review & Initial Goals", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = BabelTextPrimary)
                    Text("Review your setup summary before launching your personal OS.", style = MaterialTheme.typography.bodyMedium, color = BabelTextSecondary)
                    Spacer(modifier = Modifier.height(20.dp))

                    BabelCard {
                        Text("PROFILE SUMMARY", style = MaterialTheme.typography.labelSmall, color = BabelCyan, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        SummaryRow("Commander", displayName)
                        SummaryRow("Language & Currency", "$preferredLanguage • $currency")
                        SummaryRow("Daily Schedule", "$wakeTime wake • $sleepTime sleep")
                        SummaryRow("Study Window", studyTime)
                        SummaryRow("Starting Balance", "$currency $startingCash")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Active Initial Goals:", style = MaterialTheme.typography.titleSmall, color = BabelTextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    GoalCheckRow("Save emergency funds ($currency $savingsTarget / month)", goalSaveMoney) { goalSaveMoney = it }
                    GoalCheckRow("Complete trade theory study topics consistently", goalStudyConsistency) { goalStudyConsistency = it }
                    GoalCheckRow("Build 15-minute daily English fluency habit", goalEnglishHabit) { goalEnglishHabit = it }
                }
            }
        }
    }
}

@Composable
private fun LanguageChip(name: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) BabelCyan else BabelDarkSurfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = name,
            color = if (selected) Color(0xFF001F29) else BabelTextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SubjectCheckboxCard(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    BabelCard(
        onClick = { onCheckedChange(!checked) },
        borderColor = if (checked) BabelCyan.copy(alpha = 0.5f) else BabelDarkCardBorder
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = BabelCyan)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BabelTextPrimary)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = BabelTextSecondary)
            }
        }
    }
}

@Composable
private fun GoalCheckRow(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = BabelCyan)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = BabelTextPrimary)
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = BabelTextSecondary)
        Text(value, style = MaterialTheme.typography.bodySmall, color = BabelTextPrimary, fontWeight = FontWeight.Medium)
    }
}
