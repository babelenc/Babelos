package com.example.data

import android.content.Context
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class BabelRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing BabelOS data.")
    }

    val currentUserId: String?
        get() = auth.currentUser?.uid

    // ----------------------------------------------------
    // User Profile
    // ----------------------------------------------------
    fun observeUserProfile(): Flow<UserProfile?> = callbackFlow {
        val uid = currentUserId
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val docRef = db.collection("users").document(uid)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, docRef.path)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val profile = snapshot.toObject(UserProfile::class.java)
                trySend(profile)
            } else {
                trySend(null)
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            val data = mapOf(
                "displayName" to profile.displayName,
                "preferredLanguage" to profile.preferredLanguage,
                "preferredTheme" to profile.preferredTheme,
                "currency" to profile.currency,
                "timezone" to profile.timezone,
                "wakeTime" to profile.wakeTime,
                "sleepTime" to profile.sleepTime,
                "workSchedule" to profile.workSchedule,
                "studyTime" to profile.studyTime,
                "breakTimes" to profile.breakTimes,
                "weeklyRestDay" to profile.weeklyRestDay,
                "onboardingCompleted" to profile.onboardingCompleted,
                "createdAt" to (profile.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/${currentUserId}")
            Result.failure(e)
        }
    }

    // ----------------------------------------------------
    // Tasks & Daily Planner
    // ----------------------------------------------------
    fun observeTasks(): Flow<List<TaskItem>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val colRef = db.collection("users").document(uid).collection("tasks")
        val registration = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, colRef.path)
                return@addSnapshotListener
            }
            val tasks = snapshot?.documents?.mapNotNull { it.toObject(TaskItem::class.java) } ?: emptyList()
            trySend(tasks)
        }
        awaitClose { registration.remove() }
    }

    suspend fun addTask(task: TaskItem): Result<String> {
        return try {
            val uid = requireUserId()
            val colRef = db.collection("users").document(uid).collection("tasks")
            val data = mapOf(
                "title" to task.title,
                "description" to task.description,
                "category" to task.category,
                "priority" to task.priority,
                "dueDate" to task.dueDate,
                "startTime" to task.startTime,
                "endTime" to task.endTime,
                "estimatedDuration" to task.estimatedDuration,
                "isCompleted" to task.isCompleted,
                "recurrence" to task.recurrence,
                "relatedGoalId" to task.relatedGoalId,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val doc = colRef.add(data).await()
            Result.success(doc.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$currentUserId/tasks")
            Result.failure(e)
        }
    }

    suspend fun updateTask(task: TaskItem): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("tasks").document(task.id)
            val data = mapOf(
                "title" to task.title,
                "description" to task.description,
                "category" to task.category,
                "priority" to task.priority,
                "dueDate" to task.dueDate,
                "startTime" to task.startTime,
                "endTime" to task.endTime,
                "estimatedDuration" to task.estimatedDuration,
                "isCompleted" to task.isCompleted,
                "recurrence" to task.recurrence,
                "relatedGoalId" to task.relatedGoalId,
                "createdAt" to (task.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$currentUserId/tasks/${task.id}")
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$currentUserId/tasks/$taskId")
            Result.failure(e)
        }
    }

    // ----------------------------------------------------
    // Study Tracker (Subjects, Topics, Sessions)
    // ----------------------------------------------------
    fun observeSubjects(): Flow<List<StudySubject>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val colRef = db.collection("users").document(uid).collection("subjects")
        val registration = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, colRef.path)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(StudySubject::class.java) } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    suspend fun addSubject(subject: StudySubject): Result<String> {
        return try {
            val uid = requireUserId()
            val colRef = db.collection("users").document(uid).collection("subjects")
            val data = mapOf(
                "name" to subject.name,
                "description" to subject.description,
                "targetDate" to subject.targetDate,
                "weeklyStudyTargetMinutes" to subject.weeklyStudyTargetMinutes,
                "isActive" to subject.isActive,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val doc = colRef.add(data).await()
            Result.success(doc.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$currentUserId/subjects")
            Result.failure(e)
        }
    }

    fun observeTopics(): Flow<List<StudyTopic>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val colRef = db.collection("users").document(uid).collection("topics")
        val registration = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, colRef.path)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(StudyTopic::class.java) } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    suspend fun addTopic(topic: StudyTopic): Result<String> {
        return try {
            val uid = requireUserId()
            val colRef = db.collection("users").document(uid).collection("topics")
            val data = mapOf(
                "subjectId" to topic.subjectId,
                "name" to topic.name,
                "description" to topic.description,
                "status" to topic.status,
                "priority" to topic.priority,
                "estimatedMinutes" to topic.estimatedMinutes,
                "actualMinutes" to topic.actualMinutes,
                "notes" to topic.notes,
                "revisionDate" to topic.revisionDate,
                "quizScore" to topic.quizScore,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val doc = colRef.add(data).await()
            Result.success(doc.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$currentUserId/topics")
            Result.failure(e)
        }
    }

    suspend fun updateTopic(topic: StudyTopic): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("topics").document(topic.id)
            val data = mapOf(
                "subjectId" to topic.subjectId,
                "name" to topic.name,
                "description" to topic.description,
                "status" to topic.status,
                "priority" to topic.priority,
                "estimatedMinutes" to topic.estimatedMinutes,
                "actualMinutes" to topic.actualMinutes,
                "notes" to topic.notes,
                "revisionDate" to topic.revisionDate,
                "quizScore" to topic.quizScore,
                "createdAt" to (topic.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$currentUserId/topics/${topic.id}")
            Result.failure(e)
        }
    }

    fun observeStudySessions(): Flow<List<StudySession>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val colRef = db.collection("users").document(uid).collection("study_sessions")
        val registration = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, colRef.path)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(StudySession::class.java) } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    suspend fun recordStudySession(session: StudySession): Result<String> {
        return try {
            val uid = requireUserId()
            val colRef = db.collection("users").document(uid).collection("study_sessions")
            val data = mapOf(
                "subjectId" to session.subjectId,
                "topicId" to session.topicId,
                "subjectName" to session.subjectName,
                "topicName" to session.topicName,
                "date" to session.date,
                "durationMinutes" to session.durationMinutes,
                "studyMethod" to session.studyMethod,
                "notes" to session.notes,
                "score" to session.score,
                "createdAt" to FieldValue.serverTimestamp()
            )
            val doc = colRef.add(data).await()

            // If topicId is provided, increment topic actual study minutes
            if (session.topicId.isNotBlank()) {
                try {
                    val topicRef = db.collection("users").document(uid).collection("topics").document(session.topicId)
                    val topicSnap = topicRef.get().await()
                    if (topicSnap.exists()) {
                        val currentActual = topicSnap.getLong("actualMinutes") ?: 0L
                        topicRef.update(
                            mapOf(
                                "actualMinutes" to currentActual + session.durationMinutes,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                } catch (e: Exception) {
                    // Ignore secondary topic update error
                }
            }

            Result.success(doc.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$currentUserId/study_sessions")
            Result.failure(e)
        }
    }

    // ----------------------------------------------------
    // Personal Finance (Accounts, Transactions, Budgets)
    // ----------------------------------------------------
    fun observeAccounts(): Flow<List<FinancialAccount>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val colRef = db.collection("users").document(uid).collection("accounts")
        val registration = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, colRef.path)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(FinancialAccount::class.java) } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    suspend fun addAccount(account: FinancialAccount): Result<String> {
        return try {
            val uid = requireUserId()
            val colRef = db.collection("users").document(uid).collection("accounts")
            val data = mapOf(
                "name" to account.name,
                "accountType" to account.accountType,
                "openingBalance" to account.openingBalance,
                "currentBalance" to account.currentBalance,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val doc = colRef.add(data).await()
            Result.success(doc.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$currentUserId/accounts")
            Result.failure(e)
        }
    }

    fun observeTransactions(): Flow<List<FinancialTransaction>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val colRef = db.collection("users").document(uid).collection("transactions")
        val registration = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, colRef.path)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(FinancialTransaction::class.java) } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    suspend fun addTransaction(txn: FinancialTransaction): Result<String> {
        return try {
            val uid = requireUserId()
            val colRef = db.collection("users").document(uid).collection("transactions")
            val data = mapOf(
                "type" to txn.type,
                "amount" to txn.amount,
                "category" to txn.category,
                "date" to txn.date,
                "accountId" to txn.accountId,
                "toAccountId" to txn.toAccountId,
                "paymentMethod" to txn.paymentMethod,
                "description" to txn.description,
                "tags" to txn.tags,
                "createdAt" to FieldValue.serverTimestamp()
            )
            val doc = colRef.add(data).await()

            // Update account balance
            if (txn.accountId.isNotBlank()) {
                val accRef = db.collection("users").document(uid).collection("accounts").document(txn.accountId)
                val accSnap = accRef.get().await()
                if (accSnap.exists()) {
                    val currentBal = accSnap.getDouble("currentBalance") ?: 0.0
                    val newBal = when (txn.type) {
                        "Income" -> currentBal + txn.amount
                        "Expense" -> currentBal - txn.amount
                        "Transfer" -> currentBal - txn.amount
                        else -> currentBal
                    }
                    accRef.update(mapOf("currentBalance" to newBal, "updatedAt" to FieldValue.serverTimestamp())).await()
                }
            }
            if (txn.type == "Transfer" && txn.toAccountId.isNotBlank()) {
                val destAccRef = db.collection("users").document(uid).collection("accounts").document(txn.toAccountId)
                val destSnap = destAccRef.get().await()
                if (destSnap.exists()) {
                    val currentBal = destSnap.getDouble("currentBalance") ?: 0.0
                    destAccRef.update(mapOf("currentBalance" to currentBal + txn.amount, "updatedAt" to FieldValue.serverTimestamp())).await()
                }
            }

            Result.success(doc.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$currentUserId/transactions")
            Result.failure(e)
        }
    }

    fun observeBudget(monthYear: String): Flow<FinancialBudget?> = callbackFlow {
        val uid = currentUserId
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val colRef = db.collection("users").document(uid).collection("budgets")
        val registration = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, colRef.path)
                return@addSnapshotListener
            }
            val match = snapshot?.documents?.mapNotNull { it.toObject(FinancialBudget::class.java) }
                ?.firstOrNull { it.monthYear == monthYear }
            trySend(match)
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveBudget(budget: FinancialBudget): Result<Unit> {
        return try {
            val uid = requireUserId()
            val colRef = db.collection("users").document(uid).collection("budgets")
            val existingList = colRef.get().await().documents.mapNotNull { it.toObject(FinancialBudget::class.java) }
            val existing = existingList.firstOrNull { it.monthYear == budget.monthYear }
            val docRef = if (existing != null) colRef.document(existing.id) else colRef.document()

            val data = mapOf(
                "monthlyExpenseBudget" to budget.monthlyExpenseBudget,
                "monthlySavingsTarget" to budget.monthlySavingsTarget,
                "categoryBudgets" to budget.categoryBudgets,
                "monthYear" to budget.monthYear,
                "createdAt" to (existing?.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$currentUserId/budgets")
            Result.failure(e)
        }
    }

    // ----------------------------------------------------
    // Goals & Milestones
    // ----------------------------------------------------
    fun observeGoals(): Flow<List<GoalItem>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val colRef = db.collection("users").document(uid).collection("goals")
        val registration = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, colRef.path)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(GoalItem::class.java) } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    suspend fun addGoal(goal: GoalItem): Result<String> {
        return try {
            val uid = requireUserId()
            val colRef = db.collection("users").document(uid).collection("goals")
            val data = mapOf(
                "title" to goal.title,
                "description" to goal.description,
                "category" to goal.category,
                "targetAmount" to goal.targetAmount,
                "currentAmount" to goal.currentAmount,
                "targetDate" to goal.targetDate,
                "isCompleted" to goal.isCompleted,
                "progressPercent" to goal.progressPercent,
                "milestones" to goal.milestones,
                "notes" to goal.notes,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val doc = colRef.add(data).await()
            Result.success(doc.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$currentUserId/goals")
            Result.failure(e)
        }
    }

    suspend fun updateGoal(goal: GoalItem): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("goals").document(goal.id)
            val data = mapOf(
                "title" to goal.title,
                "description" to goal.description,
                "category" to goal.category,
                "targetAmount" to goal.targetAmount,
                "currentAmount" to goal.currentAmount,
                "targetDate" to goal.targetDate,
                "isCompleted" to goal.isCompleted,
                "progressPercent" to goal.progressPercent,
                "milestones" to goal.milestones,
                "notes" to goal.notes,
                "createdAt" to (goal.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$currentUserId/goals/${goal.id}")
            Result.failure(e)
        }
    }

    // ----------------------------------------------------
    // Chat Messages
    // ----------------------------------------------------
    fun observeChatMessages(): Flow<List<ChatMessage>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val colRef = db.collection("users").document(uid).collection("chat_messages")
        val registration = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, colRef.path)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(ChatMessage::class.java) }
                ?.sortedBy { it.timestamp } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    suspend fun addChatMessage(msg: ChatMessage): Result<String> {
        return try {
            val uid = requireUserId()
            val colRef = db.collection("users").document(uid).collection("chat_messages")
            val data = mapOf(
                "role" to msg.role,
                "content" to msg.content,
                "timestamp" to msg.timestamp,
                "createdAt" to FieldValue.serverTimestamp()
            )
            val doc = colRef.add(data).await()
            Result.success(doc.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$currentUserId/chat_messages")
            Result.failure(e)
        }
    }

    suspend fun clearChatMessages(): Result<Unit> {
        return try {
            val uid = requireUserId()
            val colRef = db.collection("users").document(uid).collection("chat_messages")
            val docs = colRef.get().await()
            for (doc in docs.documents) {
                doc.reference.delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$currentUserId/chat_messages")
            Result.failure(e)
        }
    }

    // ----------------------------------------------------
    // Starter Data Seeding (Upon Onboarding)
    // ----------------------------------------------------
    suspend fun seedStarterDataIfEmpty(
        selectedStarterSubjects: List<String>,
        openingBalance: Double,
        currency: String
    ) {
        val uid = requireUserId()

        // 1. Create Default Accounts if empty
        val accountsCol = db.collection("users").document(uid).collection("accounts")
        val existingAccounts = accountsCol.get().await()
        if (existingAccounts.isEmpty) {
            val cashAcc = mapOf(
                "name" to "Cash",
                "accountType" to "Cash",
                "openingBalance" to openingBalance,
                "currentBalance" to openingBalance,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val bankAcc = mapOf(
                "name" to "Bank Account",
                "accountType" to "Bank account",
                "openingBalance" to 0.0,
                "currentBalance" to 0.0,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            accountsCol.add(cashAcc).await()
            accountsCol.add(bankAcc).await()
        }

        // 2. Seed Subjects and Topics if empty
        val subjectsCol = db.collection("users").document(uid).collection("subjects")
        val topicsCol = db.collection("users").document(uid).collection("topics")
        val existingSubjects = subjectsCol.get().await()

        if (existingSubjects.isEmpty && selectedStarterSubjects.isNotEmpty()) {
            if ("ITI Fitter" in selectedStarterSubjects) {
                val fitterSub = subjectsCol.add(
                    mapOf(
                        "name" to "ITI Fitter",
                        "description" to "Workshop tools, trade theory, fitting operations & CBT practice",
                        "targetDate" to "2026-12-31",
                        "weeklyStudyTargetMinutes" to 360L,
                        "isActive" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                val fitterTopics = listOf(
                    "Workshop tools & safety guidelines",
                    "Fitting operations & bench work",
                    "Measuring instruments (Vernier, Micrometer)",
                    "Engineering drawing fundamentals",
                    "Workshop calculations & science",
                    "Trade theory & metallurgy",
                    "CBT examination practice questions"
                )
                for (t in fitterTopics) {
                    topicsCol.add(
                        mapOf(
                            "subjectId" to fitterSub.id,
                            "name" to t,
                            "description" to "Core module in ITI Fitter trade",
                            "status" to "Not started",
                            "priority" to "High",
                            "estimatedMinutes" to 60L,
                            "actualMinutes" to 0L,
                            "notes" to "",
                            "revisionDate" to "",
                            "quizScore" to 0L,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            if ("English" in selectedStarterSubjects) {
                val engSub = subjectsCol.add(
                    mapOf(
                        "name" to "English",
                        "description" to "Vocabulary, practical grammar, speaking & comprehension",
                        "targetDate" to "2026-12-31",
                        "weeklyStudyTargetMinutes" to 240L,
                        "isActive" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                val engTopics = listOf(
                    "Daily vocabulary (50 high-frequency words)",
                    "Grammar essentials (Tenses & prepositions)",
                    "Reading comprehension & news articles",
                    "Listening comprehension practice",
                    "Professional writing & email drafting",
                    "Daily speaking practice & fluency drills"
                )
                for (t in engTopics) {
                    topicsCol.add(
                        mapOf(
                            "subjectId" to engSub.id,
                            "name" to t,
                            "description" to "English communication module",
                            "status" to "Not started",
                            "priority" to "Medium",
                            "estimatedMinutes" to 45L,
                            "actualMinutes" to 0L,
                            "notes" to "",
                            "revisionDate" to "",
                            "quizScore" to 0L,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            if ("Mathematics" in selectedStarterSubjects) {
                val mathSub = subjectsCol.add(
                    mapOf(
                        "name" to "Mathematics",
                        "description" to "Arithmetic, percentages, speed-distance & workshop math",
                        "targetDate" to "2026-12-31",
                        "weeklyStudyTargetMinutes" to 300L,
                        "isActive" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                val mathTopics = listOf(
                    "Arithmetic & multiplication tables (1-30)",
                    "Fractions & decimals conversion",
                    "Percentages & profit-loss calculations",
                    "Ratio & proportion formulas",
                    "Basic algebra & equation solving",
                    "Time, speed and distance word problems"
                )
                for (t in mathTopics) {
                    topicsCol.add(
                        mapOf(
                            "subjectId" to mathSub.id,
                            "name" to t,
                            "description" to "Applied mathematical aptitude",
                            "status" to "Not started",
                            "priority" to "High",
                            "estimatedMinutes" to 60L,
                            "actualMinutes" to 0L,
                            "notes" to "",
                            "revisionDate" to "",
                            "quizScore" to 0L,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
        }

        // 3. Seed Starter Goals if empty
        val goalsCol = db.collection("users").document(uid).collection("goals")
        val existingGoals = goalsCol.get().await()
        if (existingGoals.isEmpty) {
            goalsCol.add(
                mapOf(
                    "title" to "Emergency Fund Savings ($currency 10,000)",
                    "description" to "Save monthly toward safety reserve",
                    "category" to "Financial",
                    "targetAmount" to 10000.0,
                    "currentAmount" to 0.0,
                    "targetDate" to "2026-12-31",
                    "isCompleted" to false,
                    "progressPercent" to 0L,
                    "milestones" to listOf("Save first $currency 2,500", "Reach $currency 5,000 half-way", "Hit $currency 10,000 target"),
                    "notes" to "Keep funds in bank savings account",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            goalsCol.add(
                mapOf(
                    "title" to "Complete ITI Trade Theory & Calculations",
                    "description" to "Finish all topic revisions before semester exam",
                    "category" to "Education",
                    "targetAmount" to 0.0,
                    "currentAmount" to 0.0,
                    "targetDate" to "2026-11-30",
                    "isCompleted" to false,
                    "progressPercent" to 15L,
                    "milestones" to listOf("Complete Workshop tools", "Finish Measuring instruments", "Attempt 3 CBT mocks"),
                    "notes" to "Study 45m daily after work shift",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        // 4. Seed initial Welcome Tasks if empty
        val tasksCol = db.collection("users").document(uid).collection("tasks")
        val existingTasks = tasksCol.get().await()
        if (existingTasks.isEmpty) {
            tasksCol.add(
                mapOf(
                    "title" to "Welcome to BabelOS: Review your schedule",
                    "description" to "Check daily routine windows and rest day",
                    "category" to "Personal",
                    "priority" to "High",
                    "dueDate" to "",
                    "startTime" to "07:30",
                    "endTime" to "08:00",
                    "estimatedDuration" to "30m",
                    "isCompleted" to false,
                    "recurrence" to "None",
                    "relatedGoalId" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            tasksCol.add(
                mapOf(
                    "title" to "Study session: Workshop Tools or English Vocabulary",
                    "description" to "Use the focus timer for 30 minutes",
                    "category" to "Study",
                    "priority" to "High",
                    "dueDate" to "",
                    "startTime" to "19:30",
                    "endTime" to "20:00",
                    "estimatedDuration" to "30m",
                    "isCompleted" to false,
                    "recurrence" to "Daily",
                    "relatedGoalId" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }
}
