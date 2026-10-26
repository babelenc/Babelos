package com.example.data

import android.util.Log
import com.example.model.*
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "AiAssistantService"

class AiAssistantService {

    private val generalModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    private val complexModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-argon"
        )
    }

    /**
     * Streams response chunks from Gemini for interactive assistant chat.
     */
    fun streamChatResponse(
        prompt: String,
        conversationHistory: List<ChatMessage>,
        contextSummary: String,
        enableDeepThinking: Boolean = false
    ): Flow<String> = flow {
        val model = if (enableDeepThinking) complexModel else generalModel

        val systemInstruction = """
            You are BabelOS Core AI, the personal life operating system assistant.
            Tagline: "Build Your Life. One Day at a Time."
            Your mission: Help the user organize daily life realistically, manage time, prioritize studies (including ITI Fitter, English, Mathematics), balance budgets, and track goals.
            
            Current User Live Context:
            $contextSummary
            
            Guidelines:
            - Practical, empathetic, realistic, and direct.
            - Provide clear actionable daily schedules, step-by-step task breakdowns, or budget insights.
            - Answer in English or Hinglish based on user input or preference.
            - Never invent fake records; reference the actual numbers provided in the context.
            - Format answers with clean bullets and concise highlights.
        """.trimIndent()

        val fullPrompt = "$systemInstruction\n\nUser Question:\n$prompt"

        try {
            withTimeout(60_000L) {
                model.generateContentStream(fullPrompt).collect { chunk ->
                    val text = chunk.text
                    if (!text.isNullOrEmpty()) {
                        emit(text)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI Assistant error", e)
            emit("\n[Error: ${e.localizedMessage ?: "Failed to generate AI response. Please check network connection."}]")
        }
    }

    /**
     * Generates a tailored daily schedule recommendation.
     */
    suspend fun generateDailyPlan(
        profile: UserProfile?,
        tasks: List<TaskItem>,
        subjects: List<StudySubject>,
        topics: List<StudyTopic>
    ): String {
        return try {
            val pendingTasks = tasks.filter { !it.isCompleted }
            val prompt = """
                Generate a realistic, optimized daily schedule for today based on the following:
                User wake time: ${profile?.wakeTime ?: "06:30"}, sleep time: ${profile?.sleepTime ?: "23:00"}
                Work schedule: ${profile?.workSchedule ?: "09:00 - 18:00"}
                Preferred study window: ${profile?.studyTime ?: "19:30 - 21:30"}
                Break times: ${profile?.breakTimes ?: "13:00 - 14:00"}
                Pending Tasks (${pendingTasks.size}): ${pendingTasks.take(5).joinToString { "${it.title} [${it.priority}]" }}
                Active Subjects: ${subjects.filter { it.isActive }.joinToString { it.name }}
                Active Topics: ${topics.filter { it.status != "Completed" }.take(4).joinToString { "${it.name} (${it.status})" }}
                
                Please output:
                1. Time-blocked schedule (Morning, Work block, Evening study, Rest)
                2. Top 3 priorities for today
                3. One actionable tip to maintain focus
                Keep it concise and realistic for someone working or studying.
            """.trimIndent()

            withTimeout(45_000L) {
                val response = generalModel.generateContent(prompt)
                response.text ?: "Could not generate daily plan at this moment."
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Plan generation failed", e)
            "Unable to generate plan: ${e.localizedMessage ?: "Network error"}"
        }
    }

    /**
     * Analyzes monthly financial records and provides evidence-based spending insights.
     */
    suspend fun analyzeFinances(
        totalIncome: Double,
        totalExpenses: Double,
        currency: String,
        categoryBreakdown: Map<String, Double>,
        budget: FinancialBudget?,
        savingsGoal: Double
    ): String {
        return try {
            val prompt = """
                Analyze the user's personal finances for the current month:
                Currency: $currency
                Total recorded income: $totalIncome
                Total recorded expenses: $totalExpenses
                Net cash flow: ${totalIncome - totalExpenses}
                Monthly Expense Budget limit: ${budget?.monthlyExpenseBudget ?: "Not set"}
                Monthly Savings Target: ${budget?.monthlySavingsTarget ?: savingsGoal}
                Category Spending breakdown:
                ${categoryBreakdown.entries.joinToString("\n") { "- ${it.key}: $currency ${it.value}" }}
                
                Provide:
                1. Objective spending summary (utilization % and net status)
                2. Biggest expense categories
                3. Actionable recommendation to hit the savings target without extreme deprivation
                Do not provide financial investment advice. Only budget and spending habits.
            """.trimIndent()

            withTimeout(45_000L) {
                val response = generalModel.generateContent(prompt)
                response.text ?: "Could not analyze spending."
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Finance analysis failed", e)
            "Unable to analyze finances: ${e.localizedMessage ?: "Network error"}"
        }
    }
}
