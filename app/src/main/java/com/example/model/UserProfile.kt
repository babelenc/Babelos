package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val displayName: String = "Commander",
    val preferredLanguage: String = "English",
    val preferredTheme: String = "Dark",
    val currency: String = "INR",
    val timezone: String = "Asia/Kolkata",
    val wakeTime: String = "06:30",
    val sleepTime: String = "23:00",
    val workSchedule: String = "09:00 - 18:00",
    val studyTime: String = "19:30 - 21:30",
    val breakTimes: String = "13:00 - 14:00",
    val weeklyRestDay: String = "Sunday",
    val onboardingCompleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
