package com.example.service

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.ai.GeminiAiEngine
import com.example.crypto.EncryptionManager
import com.example.data.local.MessageHistoryItem
import com.example.data.local.ReplyPilotDatabase
import com.example.data.preferences.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class WhatsAppAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var appPreferences: AppPreferences
    private lateinit var database: ReplyPilotDatabase

    // Cooldown and anti-loop tracking: contact -> lastProcessedMessage to avoid repeated auto-types
    private val lastProcessedMessages = ConcurrentHashMap<String, String>()
    private val lastRepliedTimestamps = ConcurrentHashMap<String, Long>()

    override fun onCreate() {
        super.onCreate()
        appPreferences = AppPreferences(applicationContext)
        database = ReplyPilotDatabase.getInstance(applicationContext)
        Log.i(TAG, "ReplyPilot Accessibility Service started")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName != "com.whatsapp" && packageName != "com.whatsapp.w4b") {
            return
        }

        val settings = appPreferences.settingsFlow.value
        if (!settings.isMasterEnabled || !settings.isAccessibilityEngineEnabled) {
            return
        }

        // Only process content or window changes
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
            return
        }

        val rootNode = rootInActiveWindow ?: return

        serviceScope.launch {
            try {
                processWhatsAppWindow(rootNode)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing WhatsApp window", e)
            }
        }
    }

    private suspend fun processWhatsAppWindow(rootNode: AccessibilityNodeInfo) {
        val contactName = extractConversationContactName(rootNode) ?: return
        val latestMessage = extractLatestIncomingMessage(rootNode) ?: return

        val settings = appPreferences.settingsFlow.value
        val cooldownMs = settings.cooldownMinutes * 60 * 1000L
        val now = System.currentTimeMillis()

        // Anti-loop check 1: Has this message text already been processed for this contact?
        val lastMsg = lastProcessedMessages[contactName]
        if (lastMsg == latestMessage) {
            return
        }

        // Anti-loop check 2: Cooldown interval
        val lastTime = lastRepliedTimestamps[contactName] ?: 0L
        if (now - lastTime < cooldownMs && cooldownMs > 0) {
            Log.d(TAG, "Skipping auto-reply for $contactName due to cooldown")
            return
        }

        val dao = database.replyPilotDao()
        val contactProfile = dao.getContactByName(contactName)

        // Check if contact is blacklisted or has auto-reply turned off
        if (contactProfile != null) {
            if (contactProfile.isBlacklisted || !contactProfile.isAutoReplyEnabled) {
                return
            }
        }

        // Decrypt contact memory if stored encrypted
        val rawMemory = contactProfile?.memoryNotes ?: ""
        val decryptedMemory = if (EncryptionManager.isEncrypted(rawMemory)) {
            EncryptionManager.decrypt(rawMemory)
        } else {
            rawMemory
        }

        val tone = contactProfile?.customTone?.takeIf { it.isNotBlank() } ?: settings.defaultTone

        // Mark as being processed immediately to prevent duplicate runs
        lastProcessedMessages[contactName] = latestMessage

        // Retrieve recent chat context from DB for conversational continuity
        val recentHistory = dao.getRecentMessagesForContact(contactName, 3)
            .map { item ->
                val inc = if (EncryptionManager.isEncrypted(item.incomingMessage)) {
                    EncryptionManager.decrypt(item.incomingMessage)
                } else item.incomingMessage
                val rep = if (EncryptionManager.isEncrypted(item.replyMessage)) {
                    EncryptionManager.decrypt(item.replyMessage)
                } else item.replyMessage
                Pair(item.contactName, inc) to Pair("Me", rep)
            }
            .flatMap { listOf(it.first, it.second) }

        val reply = GeminiAiEngine.generateWhatsAppReply(
            apiKey = settings.geminiApiKey,
            model = settings.selectedModel,
            contactName = contactName,
            incomingMessage = latestMessage,
            customTone = tone,
            memoryNotes = decryptedMemory,
            userPersona = settings.userPersonaBio,
            recentContext = recentHistory,
            appendSignature = settings.appendSignature,
            signatureText = settings.signatureText
        )

        if (reply.isBlank()) return

        // Delay typing if configured to look human
        if (settings.typingDelaySeconds > 0) {
            delay(settings.typingDelaySeconds * 1000L)
        }

        // Re-check root window to type and click send
        val currentRoot = rootInActiveWindow ?: rootNode
        val success = typeAndSendMessage(currentRoot, reply)

        if (success) {
            lastRepliedTimestamps[contactName] = System.currentTimeMillis()

            // Save encrypted or plain based on setting
            val shouldEncrypt = settings.isLocalEncryptionEnabled
            val storedIncoming = if (shouldEncrypt) EncryptionManager.encrypt(latestMessage) else latestMessage
            val storedReply = if (shouldEncrypt) EncryptionManager.encrypt(reply) else reply

            dao.insertMessage(
                MessageHistoryItem(
                    contactName = contactName,
                    incomingMessage = storedIncoming,
                    replyMessage = storedReply,
                    toneUsed = tone,
                    memoryUsed = decryptedMemory.take(80),
                    timestamp = System.currentTimeMillis(),
                    channel = "ACCESSIBILITY",
                    isEncrypted = shouldEncrypt,
                    status = "SENT"
                )
            )

            // Update contact lastReplied timestamp
            if (contactProfile != null) {
                dao.updateContact(contactProfile.copy(lastRepliedTimestamp = System.currentTimeMillis()))
            }
            Log.i(TAG, "Successfully auto-replied to $contactName via Accessibility")
        }
    }

    private fun extractConversationContactName(root: AccessibilityNodeInfo): String? {
        // Try known WhatsApp title view IDs
        val titleNodes = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/conversation_contact_name")
        if (titleNodes.isNotEmpty()) {
            val title = titleNodes[0].text?.toString()?.trim()
            if (!title.isNullOrEmpty()) return title
        }

        // Try action bar title or toolbar
        val toolbarNodes = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/toolbar")
        if (toolbarNodes.isNotEmpty()) {
            val textNodes = mutableListOf<AccessibilityNodeInfo>()
            findNodesByClassName(toolbarNodes[0], "android.widget.TextView", textNodes)
            for (node in textNodes) {
                val txt = node.text?.toString()?.trim()
                if (!txt.isNullOrEmpty() && txt != "WhatsApp" && txt != "Search") {
                    return txt
                }
            }
        }
        return null
    }

    private fun extractLatestIncomingMessage(root: AccessibilityNodeInfo): String? {
        val messageNodes = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/message_text")
        if (messageNodes.isNotEmpty()) {
            // Pick latest visible message text
            for (i in messageNodes.indices.reversed()) {
                val text = messageNodes[i].text?.toString()?.trim()
                if (!text.isNullOrEmpty() && text.length < 500) {
                    return text
                }
            }
        }

        // Generic fallback scan for chat TextViews
        val allTextViews = mutableListOf<AccessibilityNodeInfo>()
        findNodesByClassName(root, "android.widget.TextView", allTextViews)
        for (i in allTextViews.indices.reversed()) {
            val text = allTextViews[i].text?.toString()?.trim() ?: continue
            if (text.length in 2..300 && !text.matches(Regex("^[0-9]{1,2}:[0-9]{2}(\\s?[AP]M)?$"))) {
                return text
            }
        }
        return null
    }

    private fun typeAndSendMessage(root: AccessibilityNodeInfo, text: String): Boolean {
        // 1. Locate EditText input field
        var inputNode: AccessibilityNodeInfo? = null
        val entryNodes = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/entry")
        if (entryNodes.isNotEmpty()) {
            inputNode = entryNodes[0]
        } else {
            val editableNodes = mutableListOf<AccessibilityNodeInfo>()
            findEditableNodes(root, editableNodes)
            if (editableNodes.isNotEmpty()) {
                inputNode = editableNodes[0]
            }
        }

        if (inputNode == null) {
            Log.w(TAG, "Could not find WhatsApp input field")
            return false
        }

        // 2. Set text via ACTION_SET_TEXT
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        val setTextResult = inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        if (!setTextResult) {
            Log.w(TAG, "Failed to perform ACTION_SET_TEXT")
            return false
        }

        // 3. Locate Send Button
        var sendButton: AccessibilityNodeInfo? = null
        val sendNodes = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send")
        if (sendNodes.isNotEmpty()) {
            sendButton = sendNodes[0]
        } else {
            // Find clickable image button or view with 'Send' description
            val buttons = mutableListOf<AccessibilityNodeInfo>()
            findClickableButtons(root, buttons)
            sendButton = buttons.firstOrNull {
                val desc = it.contentDescription?.toString()?.lowercase() ?: ""
                desc.contains("send") || desc.contains("kirim") || desc.contains("enviar")
            } ?: buttons.lastOrNull()
        }

        return if (sendButton != null) {
            sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            Log.w(TAG, "Could not locate Send button in WhatsApp")
            false
        }
    }

    private fun findNodesByClassName(
        root: AccessibilityNodeInfo,
        className: String,
        outList: MutableList<AccessibilityNodeInfo>
    ) {
        if (root.className?.toString() == className) {
            outList.add(root)
        }
        for (i in 0 until root.childCount) {
            root.getChild(i)?.let { findNodesByClassName(it, className, outList) }
        }
    }

    private fun findEditableNodes(root: AccessibilityNodeInfo, outList: MutableList<AccessibilityNodeInfo>) {
        if (root.isEditable) {
            outList.add(root)
        }
        for (i in 0 until root.childCount) {
            root.getChild(i)?.let { findEditableNodes(it, outList) }
        }
    }

    private fun findClickableButtons(root: AccessibilityNodeInfo, outList: MutableList<AccessibilityNodeInfo>) {
        if (root.isClickable) {
            outList.add(root)
        }
        for (i in 0 until root.childCount) {
            root.getChild(i)?.let { findClickableButtons(it, outList) }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    companion object {
        private const val TAG = "WhatsAppA11yService"
    }
}
