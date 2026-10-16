package com.example.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiAiEngine {
    private const val TAG = "GeminiAiEngine"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun generateWhatsAppReply(
        apiKey: String,
        model: String,
        contactName: String,
        incomingMessage: String,
        customTone: String,
        memoryNotes: String,
        userPersona: String,
        recentContext: List<Pair<String, String>> = emptyList(),
        appendSignature: Boolean = false,
        signatureText: String = ""
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext fallbackLocalReply(contactName, incomingMessage, customTone, memoryNotes)
        }

        try {
            val systemInstructions = buildString {
                append("You are an AI assistant replying to incoming WhatsApp messages on behalf of the user. ")
                append("Respond directly with only the reply text as a real human messaging on WhatsApp. ")
                append("Do NOT include greeting headers like 'Subject:' or quotation marks. ")
                append("Keep the response concise, natural, and conversational (1 to 3 sentences maximum). ")
                
                if (userPersona.isNotBlank()) {
                    append("\nUser Persona / Current State: $userPersona")
                }
                append("\nContact Name: $contactName")
                if (customTone.isNotBlank()) {
                    append("\nRequired Tone: $customTone")
                }
                if (memoryNotes.isNotBlank()) {
                    append("\nMemory Notes regarding this contact: $memoryNotes")
                }
            }

            val conversationText = buildString {
                if (recentContext.isNotEmpty()) {
                    append("Recent conversation history:\n")
                    recentContext.takeLast(4).forEach { (sender, text) ->
                        append("$sender: $text\n")
                    }
                }
                append("Incoming message from $contactName: \"$incomingMessage\"\n")
                append("Generate an appropriate reply matching the specified tone and memory notes.")
            }

            val fullPrompt = "$systemInstructions\n\n$conversationText"

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        put("role", "user")
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", fullPrompt) })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)

                val genConfig = JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 250)
                }
                put("generationConfig", genConfig)
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                Log.w(TAG, "Gemini API error code: ${response.code}, body: $responseBody")
                return@withContext fallbackLocalReply(contactName, incomingMessage, customTone, memoryNotes)
            }

            val responseJson = JSONObject(responseBody)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    var generatedText = parts.getJSONObject(0).optString("text", "").trim()
                    // Strip enclosing quotes if generated
                    if (generatedText.startsWith("\"") && generatedText.endsWith("\"") && generatedText.length > 2) {
                        generatedText = generatedText.substring(1, generatedText.length - 1).trim()
                    }

                    if (appendSignature && signatureText.isNotBlank()) {
                        generatedText = "$generatedText\n$signatureText"
                    }

                    return@withContext generatedText
                }
            }

            fallbackLocalReply(contactName, incomingMessage, customTone, memoryNotes)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating AI response", e)
            fallbackLocalReply(contactName, incomingMessage, customTone, memoryNotes)
        }
    }

    private fun fallbackLocalReply(
        contactName: String,
        incomingMessage: String,
        tone: String,
        memory: String
    ): String {
        val lower = incomingMessage.lowercase()
        val baseTone = tone.lowercase()

        return when {
            lower.contains("hello") || lower.contains("hey") || lower.contains("hi") -> {
                when {
                    baseTone.contains("professional") -> "Hello $contactName, thanks for reaching out. I'm currently occupied, but I'll review and get back to you shortly."
                    baseTone.contains("playful") || baseTone.contains("witty") -> "Hey hey! What's cookin'? Currently on the move but couldn't leave you hanging!"
                    baseTone.contains("concise") || baseTone.contains("direct") -> "Hi. In meetings right now, will follow up soon."
                    else -> "Hey $contactName! Thanks for the message. I'm tied up for a moment, but I'll reply properly as soon as I can!"
                }
            }
            lower.contains("are you free") || lower.contains("available") || lower.contains("meet") -> {
                when {
                    baseTone.contains("professional") -> "Thanks for checking. My schedule is full at the moment, but let's connect later today."
                    else -> "Hey! A bit swamped right now, but let me check my calendar and ping you back soon!"
                }
            }
            lower.contains("where") || lower.contains("when") -> {
                if (memory.isNotBlank()) {
                    "Hey! Checking on that right now based on our notes. Talk soon!"
                } else {
                    "Just saw your message—let me look into that and reply in a bit!"
                }
            }
            else -> {
                when {
                    baseTone.contains("professional") -> "Acknowledged. Thanks for the note, $contactName. I'll get back to you shortly."
                    baseTone.contains("playful") -> "Got your ping! Running around right now, talk in a sec 😄"
                    else -> "Thanks for reaching out! I've noted your message and will reply as soon as I'm free."
                }
            }
        }
    }
}
