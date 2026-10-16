package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiAiEngine
import com.example.crypto.EncryptionManager
import com.example.data.local.ContactProfile
import com.example.data.local.MessageHistoryItem
import com.example.data.local.ReplyPilotDatabase
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.AppSettingsState
import com.example.service.ServiceStateMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class SimChatMessage(
    val id: String,
    val senderName: String,
    val text: String,
    val isIncoming: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val statusBadge: String = ""
)

class ReplyPilotViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = ReplyPilotDatabase.getInstance(context)
    private val dao = database.replyPilotDao()
    private val appPreferences = AppPreferences(context)

    val settings: StateFlow<AppSettingsState> = appPreferences.settingsFlow

    val contacts: StateFlow<List<ContactProfile>> = dao.getAllContactsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentMessages: StateFlow<List<MessageHistoryItem>> = dao.getRecentMessagesFlow(15)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMessages: StateFlow<List<MessageHistoryItem>> = dao.getAllMessagesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalRepliesCount: StateFlow<Int> = dao.getTotalRepliedCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val todayMidnight: Long
        get() {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }

    val todayRepliesCount: StateFlow<Int> = dao.getTodayRepliedCountFlow(todayMidnight)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Service permissions state
    private val _isAccessibilityActive = MutableStateFlow(false)
    val isAccessibilityActive: StateFlow<Boolean> = _isAccessibilityActive.asStateFlow()

    private val _isNotificationListenerActive = MutableStateFlow(false)
    val isNotificationListenerActive: StateFlow<Boolean> = _isNotificationListenerActive.asStateFlow()

    private val _isBatteryOptimizationIgnored = MutableStateFlow(false)
    val isBatteryOptimizationIgnored: StateFlow<Boolean> = _isBatteryOptimizationIgnored.asStateFlow()

    // Encrypted display mode toggle (user can toggle to see raw AES-256 ciphertext or decrypted text)
    private val _showRawCiphertext = MutableStateFlow(false)
    val showRawCiphertext: StateFlow<Boolean> = _showRawCiphertext.asStateFlow()

    // Simulator State
    private val _simMessages = MutableStateFlow<List<SimChatMessage>>(emptyList())
    val simMessages: StateFlow<List<SimChatMessage>> = _simMessages.asStateFlow()

    private val _isSimulating = MutableStateFlow(false)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    private val _simStepLog = MutableStateFlow("")
    val simStepLog: StateFlow<String> = _simStepLog.asStateFlow()

    init {
        refreshServiceStatuses()
        setupInitialSimMessages()
    }

    fun refreshServiceStatuses() {
        _isAccessibilityActive.value = ServiceStateMonitor.isAccessibilityServiceEnabled(context)
        _isNotificationListenerActive.value = ServiceStateMonitor.isNotificationListenerEnabled(context)
        _isBatteryOptimizationIgnored.value = ServiceStateMonitor.isIgnoringBatteryOptimizations(context)
    }

    fun toggleMasterSwitch() {
        val current = settings.value.isMasterEnabled
        appPreferences.updateMasterEnabled(!current)
    }

    fun toggleRawCiphertextView() {
        _showRawCiphertext.value = !_showRawCiphertext.value
    }

    fun updateApiKey(newKey: String) {
        appPreferences.updateApiKey(newKey)
    }

    fun updateModel(model: String) {
        appPreferences.updateSelectedModel(model)
    }

    fun updateDefaultTone(tone: String) {
        appPreferences.updateDefaultTone(tone)
    }

    fun updateCustomTonePrompt(prompt: String) {
        appPreferences.updateCustomTonePrompt(prompt)
    }

    fun updateUserPersonaBio(bio: String) {
        appPreferences.updateUserPersonaBio(bio)
    }

    fun updateTypingDelay(seconds: Int) {
        appPreferences.updateTypingDelay(seconds)
    }

    fun updateCooldown(minutes: Int) {
        appPreferences.updateCooldown(minutes)
    }

    fun updateLocalEncryption(enabled: Boolean) {
        appPreferences.updateLocalEncryption(enabled)
    }

    fun updateAntiLoop(enabled: Boolean) {
        appPreferences.updateAntiLoop(enabled)
    }

    fun updateSignature(enabled: Boolean, text: String) {
        appPreferences.updateSignatureSettings(enabled, text)
    }

    fun saveContact(
        contactName: String,
        phoneOrHandle: String,
        customTone: String,
        memoryNotes: String,
        isAutoReplyEnabled: Boolean,
        isVip: Boolean,
        isBlacklisted: Boolean,
        existingId: Long = 0L
    ) {
        viewModelScope.launch {
            val shouldEncrypt = settings.value.isLocalEncryptionEnabled
            val storedMemory = if (shouldEncrypt && memoryNotes.isNotBlank()) {
                EncryptionManager.encrypt(memoryNotes)
            } else {
                memoryNotes
            }

            val contact = ContactProfile(
                id = existingId,
                contactName = contactName.trim(),
                phoneOrHandle = phoneOrHandle.trim(),
                customTone = customTone,
                memoryNotes = storedMemory,
                isAutoReplyEnabled = isAutoReplyEnabled,
                isVip = isVip,
                isBlacklisted = isBlacklisted,
                isEncrypted = shouldEncrypt,
                updatedAt = System.currentTimeMillis()
            )

            if (existingId != 0L) {
                dao.updateContact(contact)
            } else {
                dao.insertContact(contact)
            }
        }
    }

    fun deleteContact(contact: ContactProfile) {
        viewModelScope.launch {
            dao.deleteContact(contact)
        }
    }

    fun clearMessageHistory() {
        viewModelScope.launch {
            dao.clearAllMessages()
        }
    }

    private fun setupInitialSimMessages() {
        _simMessages.value = listOf(
            SimChatMessage(
                id = "1",
                senderName = "Alex (Project Lead)",
                text = "Hey, did you finish the client presentation slides?",
                isIncoming = true,
                statusBadge = "Received"
            ),
            SimChatMessage(
                id = "2",
                senderName = "ReplyPilot (AI)",
                text = "Hi Alex, almost done with the final touch-ups. I'll have them over to you before 2 PM.",
                isIncoming = false,
                statusBadge = "AES-256 Encrypted"
            )
        )
    }

    fun runSimulatorMessage(contactName: String, incomingText: String) {
        if (incomingText.isBlank()) return

        viewModelScope.launch {
            _isSimulating.value = true
            _simStepLog.value = "📥 Incoming message received from $contactName..."

            // Add incoming message to simulator list
            val incomingMsgId = System.currentTimeMillis().toString()
            val newIncoming = SimChatMessage(
                id = incomingMsgId,
                senderName = contactName,
                text = incomingText,
                isIncoming = true,
                statusBadge = "Received"
            )
            _simMessages.value = _simMessages.value + newIncoming

            delay(500)
            val contact = dao.getContactByName(contactName)
            val rawMemory = contact?.memoryNotes ?: ""
            val decryptedMemory = if (EncryptionManager.isEncrypted(rawMemory)) {
                EncryptionManager.decrypt(rawMemory)
            } else rawMemory

            val tone = contact?.customTone?.takeIf { it.isNotBlank() } ?: settings.value.defaultTone

            _simStepLog.value = "🧠 Accessing contact memory & tone: \"$tone\"..."
            delay(600)

            _simStepLog.value = "⚡ Calling Gemini (${settings.value.selectedModel})..."
            val replyText = GeminiAiEngine.generateWhatsAppReply(
                apiKey = settings.value.geminiApiKey,
                model = settings.value.selectedModel,
                contactName = contactName,
                incomingMessage = incomingText,
                customTone = tone,
                memoryNotes = decryptedMemory,
                userPersona = settings.value.userPersonaBio,
                appendSignature = settings.value.appendSignature,
                signatureText = settings.value.signatureText
            )

            _simStepLog.value = "⌨️ Simulating auto-type in WhatsApp (delay ${settings.value.typingDelaySeconds}s)..."
            delay(1000)

            _simStepLog.value = "🔒 Encrypting chat history entry using AES-256-GCM..."
            val shouldEncrypt = settings.value.isLocalEncryptionEnabled
            val storedIncoming = if (shouldEncrypt) EncryptionManager.encrypt(incomingText) else incomingText
            val storedReply = if (shouldEncrypt) EncryptionManager.encrypt(replyText) else replyText

            // Insert into local Room database
            dao.insertMessage(
                MessageHistoryItem(
                    contactName = contactName,
                    incomingMessage = storedIncoming,
                    replyMessage = storedReply,
                    toneUsed = tone,
                    memoryUsed = decryptedMemory.take(80),
                    timestamp = System.currentTimeMillis(),
                    channel = "SIMULATOR",
                    isEncrypted = shouldEncrypt,
                    status = "SENT"
                )
            )

            // Add reply to simulator list
            val replyMsg = SimChatMessage(
                id = (System.currentTimeMillis() + 1).toString(),
                senderName = "ReplyPilot (Auto)",
                text = replyText,
                isIncoming = false,
                statusBadge = if (shouldEncrypt) "AES-256-GCM" else "Auto-Sent"
            )
            _simMessages.value = _simMessages.value + replyMsg

            _simStepLog.value = "✅ Message automatically replied and securely logged!"
            delay(1500)
            _isSimulating.value = false
        }
    }
}
