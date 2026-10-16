package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettingsState(
    val isMasterEnabled: Boolean = true,
    val geminiApiKey: String = "",
    val selectedModel: String = "gemini-2.5-flash",
    val defaultTone: String = "Warm & Friendly",
    val customTonePrompt: String = "",
    val userPersonaBio: String = "I am currently at work or in transit. Reply naturally on my behalf.",
    val typingDelaySeconds: Int = 3,
    val cooldownMinutes: Int = 5,
    val isLocalEncryptionEnabled: Boolean = true,
    val isAntiLoopProtectionEnabled: Boolean = true,
    val appendSignature: Boolean = false,
    val signatureText: String = "🤖 [ReplyPilot AI]",
    val isAccessibilityEngineEnabled: Boolean = true,
    val isNotificationEngineEnabled: Boolean = true
)

class AppPreferences(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("replypilot_settings", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<AppSettingsState> = _settingsFlow.asStateFlow()

    fun getEffectiveApiKey(): String {
        val userKey = prefs.getString("gemini_api_key", "") ?: ""
        if (userKey.isNotBlank()) return userKey.trim()

        return try {
            // Check injected BuildConfig if present
            val buildConfigField = BuildConfig::class.java.getField("GEMINI_API_KEY")
            val injected = buildConfigField.get(null) as? String ?: ""
            injected.trim()
        } catch (e: Exception) {
            ""
        }
    }

    private fun loadSettings(): AppSettingsState {
        val storedKey = prefs.getString("gemini_api_key", "") ?: ""
        val effectiveKey = if (storedKey.isNotBlank()) storedKey else getEffectiveApiKey()

        return AppSettingsState(
            isMasterEnabled = prefs.getBoolean("master_enabled", true),
            geminiApiKey = effectiveKey,
            selectedModel = prefs.getString("selected_model", "gemini-2.5-flash") ?: "gemini-2.5-flash",
            defaultTone = prefs.getString("default_tone", "Warm & Friendly") ?: "Warm & Friendly",
            customTonePrompt = prefs.getString("custom_tone_prompt", "") ?: "",
            userPersonaBio = prefs.getString(
                "user_persona_bio",
                "I am currently busy or in a meeting. Respond politely on my behalf."
            ) ?: "",
            typingDelaySeconds = prefs.getInt("typing_delay_seconds", 3),
            cooldownMinutes = prefs.getInt("cooldown_minutes", 5),
            isLocalEncryptionEnabled = prefs.getBoolean("local_encryption_enabled", true),
            isAntiLoopProtectionEnabled = prefs.getBoolean("anti_loop_enabled", true),
            appendSignature = prefs.getBoolean("append_signature", false),
            signatureText = prefs.getString("signature_text", "🤖 [ReplyPilot AI]") ?: "🤖 [ReplyPilot AI]",
            isAccessibilityEngineEnabled = prefs.getBoolean("accessibility_engine_enabled", true),
            isNotificationEngineEnabled = prefs.getBoolean("notification_engine_enabled", true)
        )
    }

    fun updateMasterEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("master_enabled", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(isMasterEnabled = enabled)
    }

    fun updateApiKey(apiKey: String) {
        prefs.edit().putString("gemini_api_key", apiKey.trim()).apply()
        _settingsFlow.value = _settingsFlow.value.copy(geminiApiKey = apiKey.trim())
    }

    fun updateSelectedModel(model: String) {
        prefs.edit().putString("selected_model", model).apply()
        _settingsFlow.value = _settingsFlow.value.copy(selectedModel = model)
    }

    fun updateDefaultTone(tone: String) {
        prefs.edit().putString("default_tone", tone).apply()
        _settingsFlow.value = _settingsFlow.value.copy(defaultTone = tone)
    }

    fun updateCustomTonePrompt(prompt: String) {
        prefs.edit().putString("custom_tone_prompt", prompt).apply()
        _settingsFlow.value = _settingsFlow.value.copy(customTonePrompt = prompt)
    }

    fun updateUserPersonaBio(bio: String) {
        prefs.edit().putString("user_persona_bio", bio).apply()
        _settingsFlow.value = _settingsFlow.value.copy(userPersonaBio = bio)
    }

    fun updateTypingDelay(seconds: Int) {
        prefs.edit().putInt("typing_delay_seconds", seconds).apply()
        _settingsFlow.value = _settingsFlow.value.copy(typingDelaySeconds = seconds)
    }

    fun updateCooldown(minutes: Int) {
        prefs.edit().putInt("cooldown_minutes", minutes).apply()
        _settingsFlow.value = _settingsFlow.value.copy(cooldownMinutes = minutes)
    }

    fun updateLocalEncryption(enabled: Boolean) {
        prefs.edit().putBoolean("local_encryption_enabled", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(isLocalEncryptionEnabled = enabled)
    }

    fun updateAntiLoop(enabled: Boolean) {
        prefs.edit().putBoolean("anti_loop_enabled", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(isAntiLoopProtectionEnabled = enabled)
    }

    fun updateSignatureSettings(append: Boolean, text: String) {
        prefs.edit()
            .putBoolean("append_signature", append)
            .putString("signature_text", text)
            .apply()
        _settingsFlow.value = _settingsFlow.value.copy(
            appendSignature = append,
            signatureText = text
        )
    }

    fun updateEnginePreferences(accessibility: Boolean, notification: Boolean) {
        prefs.edit()
            .putBoolean("accessibility_engine_enabled", accessibility)
            .putBoolean("notification_engine_enabled", notification)
            .apply()
        _settingsFlow.value = _settingsFlow.value.copy(
            isAccessibilityEngineEnabled = accessibility,
            isNotificationEngineEnabled = notification
        )
    }
}
