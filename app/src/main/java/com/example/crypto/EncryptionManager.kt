package com.example.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "ReplyPilotMasterSecretKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val PREFIX = "ENC:v1:"

    // Fallback key bytes for JVM test environments where AndroidKeyStore provider is absent
    private val JVM_FALLBACK_KEY_BYTES = byteArrayOf(
        0x4B, 0x61, 0x74, 0x68, 0x61, 0x74, 0x69, 0x5F,
        0x52, 0x65, 0x70, 0x6C, 0x79, 0x50, 0x69, 0x6C,
        0x6F, 0x74, 0x5F, 0x53, 0x65, 0x63, 0x72, 0x65,
        0x74, 0x4B, 0x65, 0x79, 0x32, 0x30, 0x32, 0x36
    )

    private fun getOrCreateSecretKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS)) {
                val keyEntry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                if (keyEntry?.secretKey != null) {
                    return keyEntry.secretKey
                }
            }

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        } catch (e: Throwable) {
            // JVM / Robolectric environment fallback: standard AES 256 SecretKey
            SecretKeySpec(JVM_FALLBACK_KEY_BYTES, "AES")
        }
    }

    fun isEncrypted(text: String?): Boolean {
        if (text.isNullOrEmpty()) return false
        return text.startsWith(PREFIX)
    }

    fun encrypt(plainText: String?): String {
        if (plainText.isNullOrEmpty()) return ""
        if (isEncrypted(plainText)) return plainText

        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val cipherBase64 = Base64.encodeToString(cipherText, Base64.NO_WRAP)
            "$PREFIX$ivBase64:$cipherBase64"
        } catch (e: Exception) {
            e.printStackTrace()
            plainText
        }
    }

    fun decrypt(text: String?): String {
        if (text.isNullOrEmpty()) return ""
        if (!isEncrypted(text)) return text

        return try {
            val payload = text.removePrefix(PREFIX)
            val parts = payload.split(":")
            if (parts.size != 2) return text

            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherText = Base64.decode(parts[1], Base64.NO_WRAP)

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val plainBytes = cipher.doFinal(cipherText)
            String(plainBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            "[Encrypted Payload]"
        }
    }

    fun getMaskedPreview(text: String?): String {
        if (text.isNullOrEmpty()) return ""
        val decrypted = decrypt(text)
        return if (decrypted.length > 4) {
            decrypted.take(2) + "••••••••" + decrypted.takeLast(2)
        } else {
            "••••••••"
        }
    }
}
