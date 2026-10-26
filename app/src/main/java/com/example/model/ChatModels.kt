package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val role: String = "user", // "user" or "model"
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    @ServerTimestamp val createdAt: Timestamp? = null
)
