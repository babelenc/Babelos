package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class StudySubject(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val targetDate: String = "",
    val weeklyStudyTargetMinutes: Long = 300L,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StudyTopic(
    @DocumentId val id: String = "",
    val subjectId: String = "",
    val name: String = "",
    val description: String = "",
    val status: String = "Not started", // Not started, In progress, Completed
    val priority: String = "Medium", // High, Medium, Low
    val estimatedMinutes: Long = 60L,
    val actualMinutes: Long = 0L,
    val notes: String = "",
    val revisionDate: String = "",
    val quizScore: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StudySession(
    @DocumentId val id: String = "",
    val subjectId: String = "",
    val topicId: String = "",
    val subjectName: String = "",
    val topicName: String = "",
    val date: String = "",
    val durationMinutes: Long = 0L,
    val studyMethod: String = "Reading", // Reading, Practice, CBT Practice, Notes, Revision
    val notes: String = "",
    val score: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)
