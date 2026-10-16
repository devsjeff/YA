package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.crypto.EncryptionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ContactProfile::class, MessageHistoryItem::class],
    version = 1,
    exportSchema = false
)
abstract class ReplyPilotDatabase : RoomDatabase() {

    abstract fun replyPilotDao(): ReplyPilotDao

    companion object {
        @Volatile
        private var INSTANCE: ReplyPilotDatabase? = null

        fun getInstance(context: Context): ReplyPilotDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReplyPilotDatabase::class.java,
                    "replypilot_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Prepopulate initial contacts with sample tones and memories
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val dao = database.replyPilotDao()
                        val momMemory = "Mom: Ask about her garden. Remind her I am coming over this Sunday for dinner. Very caring."
                        val alexMemory = "Alex (Project Lead): Current sprint deadline is Thursday. Keep messages crisp and technical. Updates on Q3 deliverables."
                        val sarahMemory = "Sarah (Best Friend): Likes sarcasm and emojis. Planning a beach trip next weekend. Inside joke about burnt pizza."

                        dao.insertContact(
                            ContactProfile(
                                contactName = "Mom",
                                phoneOrHandle = "+1 555-0101",
                                customTone = "Warm & Affectionate",
                                memoryNotes = EncryptionManager.encrypt(momMemory),
                                isVip = true,
                                isAutoReplyEnabled = true
                            )
                        )
                        dao.insertContact(
                            ContactProfile(
                                contactName = "Alex (Project Lead)",
                                phoneOrHandle = "+1 555-0102",
                                customTone = "Crisp & Professional",
                                memoryNotes = EncryptionManager.encrypt(alexMemory),
                                isVip = false,
                                isAutoReplyEnabled = true
                            )
                        )
                        dao.insertContact(
                            ContactProfile(
                                contactName = "Sarah",
                                phoneOrHandle = "+1 555-0103",
                                customTone = "Casual & Playful",
                                memoryNotes = EncryptionManager.encrypt(sarahMemory),
                                isVip = true,
                                isAutoReplyEnabled = true
                            )
                        )

                        // Sample initial history item
                        val sampleIncoming = "Hey! Are you still joining us for dinner this Sunday?"
                        val sampleReply = "Yes Mom! Looking forward to it, see you Sunday at 6pm! ❤️"
                        dao.insertMessage(
                            MessageHistoryItem(
                                contactName = "Mom",
                                incomingMessage = EncryptionManager.encrypt(sampleIncoming),
                                replyMessage = EncryptionManager.encrypt(sampleReply),
                                toneUsed = "Warm & Affectionate",
                                memoryUsed = "Dinner this Sunday",
                                timestamp = System.currentTimeMillis() - 1000 * 60 * 35,
                                channel = "ACCESSIBILITY",
                                isEncrypted = true,
                                status = "SENT"
                            )
                        )
                    }
                }
            }
        }
    }
}
