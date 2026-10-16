package com.example.service

import android.app.Notification
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.example.ai.GeminiAiEngine
import com.example.crypto.EncryptionManager
import com.example.data.local.MessageHistoryItem
import com.example.data.local.ReplyPilotDatabase
import com.example.data.preferences.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class WhatsAppNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var appPreferences: AppPreferences
    private lateinit var database: ReplyPilotDatabase

    private val lastRepliedTimestamps = ConcurrentHashMap<String, Long>()
    private val lastRepliedTexts = ConcurrentHashMap<String, String>()

    override fun onCreate() {
        super.onCreate()
        appPreferences = AppPreferences(applicationContext)
        database = ReplyPilotDatabase.getInstance(applicationContext)
        Log.i(TAG, "ReplyPilot Notification Listener started")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val packageName = sbn.packageName
        if (packageName != "com.whatsapp" && packageName != "com.whatsapp.w4b") {
            return
        }

        val settings = appPreferences.settingsFlow.value
        if (!settings.isMasterEnabled || !settings.isNotificationEngineEnabled) {
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: return
        val message = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()?.trim() ?: return

        // Skip group summaries or status messages like "3 new messages" or "WhatsApp Web"
        if (title.isBlank() || message.isBlank() || title.equals("WhatsApp", ignoreCase = true)) {
            return
        }

        serviceScope.launch {
            try {
                handleNotificationReply(sbn, title, message)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling notification reply", e)
            }
        }
    }

    private suspend fun handleNotificationReply(
        sbn: StatusBarNotification,
        contactName: String,
        incomingMessage: String
    ) {
        val settings = appPreferences.settingsFlow.value
        val cooldownMs = settings.cooldownMinutes * 60 * 1000L
        val now = System.currentTimeMillis()

        // Anti-loop check: message deduplication
        val lastMsg = lastRepliedTexts[contactName]
        if (lastMsg == incomingMessage) return

        // Anti-loop check: cooldown
        val lastTime = lastRepliedTimestamps[contactName] ?: 0L
        if (now - lastTime < cooldownMs && cooldownMs > 0) return

        val dao = database.replyPilotDao()
        val contactProfile = dao.getContactByName(contactName)

        if (contactProfile != null) {
            if (contactProfile.isBlacklisted || !contactProfile.isAutoReplyEnabled) {
                return
            }
        }

        val actions = NotificationCompat.getActionCount(sbn.notification)
        if (actions == 0) return

        var replyAction: NotificationCompat.Action? = null
        var replyRemoteInput: RemoteInput? = null

        // Look for wearable or inline reply action with RemoteInput
        for (i in 0 until actions) {
            val action = NotificationCompat.getAction(sbn.notification, i)
            val remoteInputs = action?.remoteInputs
            if (!remoteInputs.isNullOrEmpty()) {
                replyAction = action
                replyRemoteInput = remoteInputs[0]
                break
            }
        }

        if (replyAction == null || replyRemoteInput == null) {
            return
        }

        // Decrypt contact memory if stored encrypted
        val rawMemory = contactProfile?.memoryNotes ?: ""
        val decryptedMemory = if (EncryptionManager.isEncrypted(rawMemory)) {
            EncryptionManager.decrypt(rawMemory)
        } else {
            rawMemory
        }

        val tone = contactProfile?.customTone?.takeIf { it.isNotBlank() } ?: settings.defaultTone

        lastRepliedTexts[contactName] = incomingMessage

        val reply = GeminiAiEngine.generateWhatsAppReply(
            apiKey = settings.geminiApiKey,
            model = settings.selectedModel,
            contactName = contactName,
            incomingMessage = incomingMessage,
            customTone = tone,
            memoryNotes = decryptedMemory,
            userPersona = settings.userPersonaBio,
            appendSignature = settings.appendSignature,
            signatureText = settings.signatureText
        )

        if (reply.isBlank()) return

        // Send direct reply intent
        val replyIntent = Intent()
        val bundle = Bundle().apply {
            putCharSequence(replyRemoteInput.resultKey, reply)
        }
        RemoteInput.addResultsToIntent(arrayOf(replyRemoteInput), replyIntent, bundle)

        replyAction.actionIntent?.send(applicationContext, 0, replyIntent)
        lastRepliedTimestamps[contactName] = System.currentTimeMillis()

        // Save encrypted or plain based on setting
        val shouldEncrypt = settings.isLocalEncryptionEnabled
        val storedIncoming = if (shouldEncrypt) EncryptionManager.encrypt(incomingMessage) else incomingMessage
        val storedReply = if (shouldEncrypt) EncryptionManager.encrypt(reply) else reply

        dao.insertMessage(
            MessageHistoryItem(
                contactName = contactName,
                incomingMessage = storedIncoming,
                replyMessage = storedReply,
                toneUsed = tone,
                memoryUsed = decryptedMemory.take(80),
                timestamp = System.currentTimeMillis(),
                channel = "NOTIFICATION_DIRECT_REPLY",
                isEncrypted = shouldEncrypt,
                status = "SENT"
            )
        )

        if (contactProfile != null) {
            dao.updateContact(contactProfile.copy(lastRepliedTimestamp = System.currentTimeMillis()))
        }
        Log.i(TAG, "Sent instant notification reply to $contactName")
    }

    companion object {
        private const val TAG = "WhatsAppNotifListener"
    }
}
