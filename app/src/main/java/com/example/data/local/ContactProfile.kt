package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contact_profiles",
    indices = [Index(value = ["contactName"], unique = true)]
)
data class ContactProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val contactName: String,
    val phoneOrHandle: String = "",
    val customTone: String = "Warm & Friendly",
    val memoryNotes: String = "",
    val isAutoReplyEnabled: Boolean = true,
    val isVip: Boolean = false,
    val isBlacklisted: Boolean = false,
    val isEncrypted: Boolean = true,
    val lastRepliedTimestamp: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)
