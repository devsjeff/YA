package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.crypto.EncryptionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ReplyPilot", appName)
  }

  @Test
  fun `local encryption and decryption test`() {
    val sampleMessage = "Hey Mom! I will be home by 6 PM for dinner."
    val encrypted = EncryptionManager.encrypt(sampleMessage)
    
    assertTrue(EncryptionManager.isEncrypted(encrypted))
    assertNotEquals(sampleMessage, encrypted)

    val decrypted = EncryptionManager.decrypt(encrypted)
    assertEquals(sampleMessage, decrypted)
  }

  @Test
  fun `masked preview format test`() {
    val sample = "Confidential Contact"
    val masked = EncryptionManager.getMaskedPreview(sample)
    assertTrue(masked.contains("••••••••"))
  }
}
