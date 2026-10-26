package com.example.ui.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.data.BabelRepository
import com.example.model.UserProfile
import com.example.ui.components.BabelBadge
import com.example.ui.components.BabelCard
import com.example.ui.theme.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: BabelRepository,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val profile by repository.observeUserProfile().collectAsState(initial = null)

    var displayName by remember { mutableStateOf(profile?.displayName ?: "User") }
    var preferredLanguage by remember { mutableStateOf(profile?.preferredLanguage ?: "English") }
    var currency by remember { mutableStateOf(profile?.currency ?: "INR") }
    var wakeTime by remember { mutableStateOf(profile?.wakeTime ?: "06:30") }
    var sleepTime by remember { mutableStateOf(profile?.sleepTime ?: "23:00") }
    var workSchedule by remember { mutableStateOf(profile?.workSchedule ?: "09:00 - 18:00") }
    var studyTime by remember { mutableStateOf(profile?.studyTime ?: "19:30 - 21:30") }
    var weeklyRestDay by remember { mutableStateOf(profile?.weeklyRestDay ?: "Sunday") }

    LaunchedEffect(profile) {
        if (profile != null) {
            displayName = profile!!.displayName
            preferredLanguage = profile!!.preferredLanguage
            currency = profile!!.currency
            wakeTime = profile!!.wakeTime
            sleepTime = profile!!.sleepTime
            workSchedule = profile!!.workSchedule
            studyTime = profile!!.studyTime
            weeklyRestDay = profile!!.weeklyRestDay
        }
    }

    var isSaving by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = BabelDarkBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "System Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = BabelTextPrimary
                )
                Text(
                    text = "Personalize your operating system profile and schedule.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BabelTextSecondary
                )
            }
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cloud Sync Status Card
            BabelCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BabelSuccess.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = BabelSuccess, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Cloud Firestore Sync", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BabelTextPrimary)
                            Text("Google Account: ${Firebase.auth.currentUser?.email ?: "Connected"}", style = MaterialTheme.typography.labelSmall, color = BabelTextSecondary)
                        }
                    }
                    BabelBadge(text = "LIVE", color = BabelSuccess)
                }
            }

            // Identity & Localization Card
            BabelCard {
                Text("IDENTITY & LOCALIZATION", style = MaterialTheme.typography.labelSmall, color = BabelCyan, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = preferredLanguage,
                        onValueChange = { preferredLanguage = it },
                        label = { Text("Language (English/Hinglish)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it },
                        label = { Text("Currency (e.g. INR)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            // Daily Routine Windows Card
            BabelCard {
                Text("ROUTINE WINDOWS", style = MaterialTheme.typography.labelSmall, color = BabelCyan, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = wakeTime, onValueChange = { wakeTime = it }, label = { Text("Wake Time") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = sleepTime, onValueChange = { sleepTime = it }, label = { Text("Sleep Time") }, modifier = Modifier.weight(1f), singleLine = true)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = workSchedule,
                    onValueChange = { workSchedule = it },
                    label = { Text("Work / College Schedule") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = studyTime,
                    onValueChange = { studyTime = it },
                    label = { Text("Preferred Study Time") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = weeklyRestDay,
                    onValueChange = { weeklyRestDay = it },
                    label = { Text("Weekly Rest Day") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            if (saveMessage != null) {
                Text(saveMessage!!, color = BabelSuccess, style = MaterialTheme.typography.bodySmall)
            }

            // Save Settings Button
            Button(
                onClick = {
                    isSaving = true
                    coroutineScope.launch {
                        val current = profile ?: UserProfile()
                        val updated = current.copy(
                            displayName = displayName,
                            preferredLanguage = preferredLanguage,
                            currency = currency,
                            wakeTime = wakeTime,
                            sleepTime = sleepTime,
                            workSchedule = workSchedule,
                            studyTime = studyTime,
                            weeklyRestDay = weeklyRestDay
                        )
                        repository.saveUserProfile(updated)
                        isSaving = false
                        saveMessage = "Settings successfully updated."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BabelCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("save_settings_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF001F29))
                } else {
                    Text("Save Changes", color = Color(0xFF001F29), fontWeight = FontWeight.Bold)
                }
            }

            // Sign Out
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        try {
                            val credMan = CredentialManager.create(context)
                            credMan.clearCredentialState(ClearCredentialStateRequest())
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            // ignore clear credential failure
                        }
                        Firebase.auth.signOut()
                        onSignOut()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = BabelError)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out of BabelOS", color = BabelError, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
