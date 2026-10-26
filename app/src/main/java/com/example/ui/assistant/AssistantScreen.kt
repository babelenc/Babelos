package com.example.ui.assistant

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AiAssistantService
import com.example.data.BabelRepository
import com.example.model.ChatMessage
import com.example.ui.components.BabelBadge
import com.example.ui.components.BabelCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    repository: BabelRepository,
    aiService: AiAssistantService,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val chatMessages by repository.observeChatMessages().collectAsState(initial = emptyList())
    val profile by repository.observeUserProfile().collectAsState(initial = null)
    val tasks by repository.observeTasks().collectAsState(initial = emptyList())
    val subjects by repository.observeSubjects().collectAsState(initial = emptyList())
    val topics by repository.observeTopics().collectAsState(initial = emptyList())
    val transactions by repository.observeTransactions().collectAsState(initial = emptyList())

    var inputText by remember { mutableStateOf("") }
    var isThinkingMode by remember { mutableStateOf(false) } // High thinking with gemini-4-argon
    var isStreamingResponse by remember { mutableStateOf(false) }
    var currentStreamingChunk by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new messages
    LaunchedEffect(chatMessages.size, currentStreamingChunk) {
        if (chatMessages.isNotEmpty() || currentStreamingChunk.isNotEmpty()) {
            listState.animateScrollToItem((chatMessages.size + if (currentStreamingChunk.isNotEmpty()) 1 else 0).coerceAtLeast(0))
        }
    }

    val suggestedPrompts = listOf(
        "Organize my day realistically",
        "Review my spending this month",
        "Suggest priority for ITI Fitter & English",
        "Check for schedule conflicts in routine",
        "Break my goals into actionable steps"
    )

    fun sendMessage(messageText: String) {
        if (messageText.isBlank() || isStreamingResponse) return

        val userMsg = ChatMessage(
            role = "user",
            content = messageText,
            timestamp = System.currentTimeMillis()
        )

        val contextSummary = buildString {
            appendLine("User: ${profile?.displayName ?: "User"}")
            appendLine("Language: ${profile?.preferredLanguage ?: "English"}")
            appendLine("Currency: ${profile?.currency ?: "INR"}")
            appendLine("Routine: Wake ${profile?.wakeTime ?: "06:30"}, Sleep ${profile?.sleepTime ?: "23:00"}, Work ${profile?.workSchedule ?: "09:00 - 18:00"}, Study ${profile?.studyTime ?: "19:30 - 21:30"}")
            appendLine("Tasks (${tasks.size}): ${tasks.take(6).joinToString { "${it.title} [${it.priority}] completed=${it.isCompleted}" }}")
            appendLine("Active Subjects: ${subjects.filter { it.isActive }.joinToString { it.name }}")
            appendLine("Topics in Progress: ${topics.filter { it.status == "In progress" }.joinToString { it.name }}")
            val recordedExp = transactions.filter { it.type == "Expense" }.sumOf { it.amount }
            val recordedInc = transactions.filter { it.type == "Income" }.sumOf { it.amount }
            appendLine("October Cash Flow: Income ${profile?.currency} $recordedInc, Expenses ${profile?.currency} $recordedExp, Net ${recordedInc - recordedExp}")
        }

        inputText = ""
        isStreamingResponse = true
        currentStreamingChunk = ""

        coroutineScope.launch {
            repository.addChatMessage(userMsg)

            val fullResponseBuilder = StringBuilder()
            aiService.streamChatResponse(
                prompt = messageText,
                conversationHistory = chatMessages,
                contextSummary = contextSummary,
                enableDeepThinking = isThinkingMode
            ).collect { chunk ->
                fullResponseBuilder.append(chunk)
                currentStreamingChunk = fullResponseBuilder.toString()
            }

            val finalContent = fullResponseBuilder.toString().ifBlank { "I was unable to complete the request. Please try again." }
            repository.addChatMessage(
                ChatMessage(
                    role = "model",
                    content = finalContent,
                    timestamp = System.currentTimeMillis()
                )
            )
            currentStreamingChunk = ""
            isStreamingResponse = false
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
                    Column {
                        Text(
                            text = "BabelOS Core AI",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = BabelTextPrimary
                        )
                        Text(
                            text = if (isThinkingMode) "Gemini 4 Argon • High Thinking Enabled" else "Gemini 3.8 Flash • General Assistant",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isThinkingMode) BabelPurple else BabelCyan
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // High Thinking Toggle
                        FilterChip(
                            selected = isThinkingMode,
                            onClick = { isThinkingMode = !isThinkingMode },
                            label = { Text("High Thinking", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BabelPurple.copy(alpha = 0.3f),
                                selectedLabelColor = BabelPurple
                            ),
                            modifier = Modifier.testTag("thinking_mode_chip")
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = { coroutineScope.launch { repository.clearChatMessages() } },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Chat", tint = BabelTextMuted, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BabelDarkSurface)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Quick prompt suggestions
                if (chatMessages.isEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        items(suggestedPrompts) { prompt ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BabelDarkSurfaceVariant)
                                    .border(1.dp, BabelCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .clickable { sendMessage(prompt) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(prompt, color = BabelCyan, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // Input bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask BabelOS in English or Hinglish...", color = BabelTextMuted, style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = BabelDarkSurfaceVariant,
                            unfocusedContainerColor = BabelDarkSurfaceVariant,
                            focusedBorderColor = BabelCyan,
                            unfocusedBorderColor = BabelDarkCardBorder
                        ),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { sendMessage(inputText) },
                        enabled = inputText.isNotBlank() && !isStreamingResponse,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank()) BabelCyan else BabelDarkSurfaceVariant)
                            .testTag("chat_send_button")
                    ) {
                        if (isStreamingResponse) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF001F29), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = if (inputText.isNotBlank()) Color(0xFF001F29) else BabelTextMuted)
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (chatMessages.isEmpty() && currentStreamingChunk.isEmpty()) {
                item {
                    BabelCard(modifier = Modifier.padding(vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(BabelCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BabelCyan, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("BabelOS Assistant Ready", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BabelTextPrimary)
                                Text("Ask for schedule optimization, study roadmaps, budget feedback, or task prioritization.", style = MaterialTheme.typography.bodySmall, color = BabelTextSecondary)
                            }
                        }
                    }
                }
            }

            items(chatMessages, key = { it.id }) { msg ->
                ChatBubble(msg.role, msg.content)
            }

            if (currentStreamingChunk.isNotEmpty()) {
                item {
                    ChatBubble("model", currentStreamingChunk, isStreaming = true)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    role: String,
    content: String,
    isStreaming: Boolean = false
) {
    val isUser = role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(BabelCyan.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Psychology, contentDescription = null, tint = BabelCyan, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(if (isUser) BabelCyan else BabelDarkSurfaceVariant)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) Color(0xFF001F29) else BabelTextPrimary,
                lineHeight = 20.sp
            )
        }
    }
}
