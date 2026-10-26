package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class GoalItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "Personal development", // Education, Financial, Career, Personal development, Health and routine, Custom
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val targetDate: String = "", // YYYY-MM-DD
    val isCompleted: Boolean = false,
    val progressPercent: Long = 0L,
    val milestones: List<String> = emptyList(),
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
