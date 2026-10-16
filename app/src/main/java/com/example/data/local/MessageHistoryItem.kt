package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "message_history",
    indices = [
        Index(value = ["contactName"]),
        Index(value = ["timestamp"])
    ]
)
data class MessageHistoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val contactName: String,
    val incomingMessage: String,
    val replyMessage: String,
    val toneUsed: String = "Warm & Friendly",
    val memoryUsed: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val channel: String = "ACCESSIBILITY", // ACCESSIBILITY, NOTIFICATION, SIMULATOR
    val isEncrypted: Boolean = true,
    val status: String = "SENT" // SENT, FAILED, TEST
)
