package com.example.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.EncryptionManager
import com.example.data.local.MessageHistoryItem
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CardDark
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EncryptShieldGold
import com.example.ui.theme.EncryptShieldGreen
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondaryDark
import com.example.viewmodel.ReplyPilotViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: ReplyPilotViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.allMessages.collectAsState()
    val showRawCiphertext by viewModel.showRawCiphertext.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    var inspectMessage by remember { mutableStateOf<MessageHistoryItem?>(null) }

    val filteredMessages = messages.filter { msg ->
        val query = searchQuery.lowercase()
        val plainIncoming = EncryptionManager.decrypt(msg.incomingMessage)
        val plainReply = EncryptionManager.decrypt(msg.replyMessage)

        msg.contactName.lowercase().contains(query) ||
                plainIncoming.lowercase().contains(query) ||
                plainReply.lowercase().contains(query)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("history_screen")
    ) {
        // Top Security & Control Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, EncryptShieldGreen.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(EncryptShieldGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security",
                            tint = EncryptShieldGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Local Hardware Encryption",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (showRawCiphertext) "Viewing raw AES-256 ciphertext" else "Decrypted on-device (AES-256-GCM)",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                // Ciphertext toggle button
                FilledTonalButton(
                    onClick = { viewModel.toggleRawCiphertextView() },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (showRawCiphertext) EncryptShieldGold.copy(alpha = 0.2f) else EmeraldPrimary.copy(alpha = 0.2f),
                        contentColor = if (showRawCiphertext) EncryptShieldGold else EmeraldLight
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("toggle_ciphertext_button")
                ) {
                    Icon(
                        imageVector = if (showRawCiphertext) Icons.Default.VisibilityOff else Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showRawCiphertext) "Raw Cipher" else "Decrypted",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar + Clear Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search logs or contacts...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("history_search_input"),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextSecondaryDark
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = SurfaceBorder
                )
            )

            if (messages.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardDark)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                        .testTag("clear_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear History",
                        tint = AlertRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Message List
        if (filteredMessages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matching records found" else "Message history is empty",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondaryDark
                    )
                    Text(
                        text = "Auto-replied WhatsApp chats will be logged here securely",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredMessages, key = { it.id }) { msg ->
                    HistoryItemCard(
                        item = msg,
                        showRawCiphertext = showRawCiphertext,
                        onInspect = { inspectMessage = msg }
                    )
                }
            }
        }

        // Clear History Confirmation Dialog
        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("Clear All Message Logs?") },
                text = { Text("This will permanently wipe all local encrypted auto-reply logs from the device database.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearMessageHistory()
                            showClearDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                    ) {
                        Text("Clear All")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("Cancel")
                    }
                },
                containerColor = CardDark
            )
        }

        // Crypto Inspection Dialog
        inspectMessage?.let { item ->
            CryptoInspectDialog(
                item = item,
                onDismiss = { inspectMessage = null }
            )
        }
    }
}

@Composable
fun HistoryItemCard(
    item: MessageHistoryItem,
    showRawCiphertext: Boolean,
    onInspect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = remember(item.timestamp) {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }

    val displayIncoming = if (showRawCiphertext) {
        item.incomingMessage
    } else {
        EncryptionManager.decrypt(item.incomingMessage)
    }

    val displayReply = if (showRawCiphertext) {
        item.replyMessage
    } else {
        EncryptionManager.decrypt(item.replyMessage)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onInspect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.contactName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = when (item.channel) {
                            "ACCESSIBILITY" -> CyberTeal.copy(alpha = 0.15f)
                            "NOTIFICATION_DIRECT_REPLY" -> EmeraldPrimary.copy(alpha = 0.15f)
                            else -> EncryptShieldGold.copy(alpha = 0.15f)
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = when (item.channel) {
                                "ACCESSIBILITY" -> "Auto-Type"
                                "NOTIFICATION_DIRECT_REPLY" -> "Notif Reply"
                                else -> "Simulated"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (item.channel) {
                                "ACCESSIBILITY" -> CyberTeal
                                "NOTIFICATION_DIRECT_REPLY" -> EmeraldLight
                                else -> EncryptShieldGold
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.isEncrypted) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted",
                            tint = EncryptShieldGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Incoming Bubble
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Incoming message:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = displayIncoming,
                        fontSize = 12.sp,
                        color = TextSecondaryDark,
                        fontFamily = if (showRawCiphertext) FontFamily.Monospace else FontFamily.Default,
                        maxLines = if (showRawCiphertext) 2 else 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Outgoing Auto-Reply Bubble
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF003D32),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI Auto-Reply (Tone: ${item.toneUsed}):",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldLight
                        )
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Details",
                            tint = EmeraldLight.copy(alpha = 0.7f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = displayReply,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE9EDEF),
                        fontFamily = if (showRawCiphertext) FontFamily.Monospace else FontFamily.Default,
                        maxLines = if (showRawCiphertext) 3 else 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun CryptoInspectDialog(
    item: MessageHistoryItem,
    onDismiss: () -> Unit
) {
    val plainIncoming = EncryptionManager.decrypt(item.incomingMessage)
    val plainReply = EncryptionManager.decrypt(item.replyMessage)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = EncryptShieldGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cryptographic Details", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Algorithm: AES-256-GCM (NoPadding)", fontSize = 12.sp, color = CyberTeal)
                Text("Key Store: AndroidKeyStore (Hardware-backed)", fontSize = 12.sp, color = TextSecondaryDark)
                Text("Key Alias: ReplyPilotMasterSecretKey", fontSize = 12.sp, color = TextSecondaryDark)
                Spacer(modifier = Modifier.height(10.dp))

                Text("Ciphertext in Database:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = item.replyMessage.take(120) + if (item.replyMessage.length > 120) "..." else "",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = EncryptShieldGold,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Decrypted Plaintext:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(plainReply, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)

                if (item.memoryUsed.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Memory Context Injected: ${item.memoryUsed}", fontSize = 11.sp, color = TextMuted)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black)
            ) {
                Text("Done")
            }
        },
        containerColor = CardDark
    )
}
