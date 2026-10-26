package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class TaskItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "Work", // Work, Study, Finance, Personal, Health, Household, Other
    val priority: String = "Medium", // High, Medium, Low
    val dueDate: String = "", // YYYY-MM-DD
    val startTime: String = "", // HH:mm
    val endTime: String = "", // HH:mm
    val estimatedDuration: String = "30m",
    val isCompleted: Boolean = false,
    val recurrence: String = "None", // None, Daily, Weekly
    val relatedGoalId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
