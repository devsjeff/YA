package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReplyPilotDao {

    // Contact Profiles
    @Query("SELECT * FROM contact_profiles ORDER BY isVip DESC, updatedAt DESC")
    fun getAllContactsFlow(): Flow<List<ContactProfile>>

    @Query("SELECT * FROM contact_profiles WHERE contactName = :name LIMIT 1")
    suspend fun getContactByName(name: String): ContactProfile?

    @Query("SELECT * FROM contact_profiles WHERE id = :id LIMIT 1")
    suspend fun getContactById(id: Long): ContactProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactProfile): Long

    @Update
    suspend fun updateContact(contact: ContactProfile)

    @Delete
    suspend fun deleteContact(contact: ContactProfile)

    @Query("SELECT COUNT(*) FROM contact_profiles")
    fun getContactCountFlow(): Flow<Int>

    // Message History
    @Query("SELECT * FROM message_history ORDER BY timestamp DESC")
    fun getAllMessagesFlow(): Flow<List<MessageHistoryItem>>

    @Query("SELECT * FROM message_history WHERE contactName = :contactName ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessagesForContact(contactName: String, limit: Int = 10): List<MessageHistoryItem>

    @Query("SELECT * FROM message_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMessagesFlow(limit: Int = 15): Flow<List<MessageHistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageHistoryItem): Long

    @Delete
    suspend fun deleteMessage(message: MessageHistoryItem)

    @Query("DELETE FROM message_history")
    suspend fun clearAllMessages()

    @Query("SELECT COUNT(*) FROM message_history")
    fun getTotalRepliedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM message_history WHERE timestamp >= :sinceTimestamp")
    fun getTodayRepliedCountFlow(sinceTimestamp: Long): Flow<Int>
}
