package com.example.ui.simulator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.EncryptionManager
import com.example.ui.theme.CardDark
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EncryptShieldGreen
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondaryDark
import com.example.viewmodel.ReplyPilotViewModel
import com.example.viewmodel.SimChatMessage

val QUICK_TEST_PROMPTS = listOf(
    "Hey! Are you free to meet up today?",
    "Did you get a chance to review the file?",
    "Where should we go for lunch?",
    "Can you call me as soon as you see this?"
)

@Composable
fun SimulatorScreen(
    viewModel: ReplyPilotViewModel,
    modifier: Modifier = Modifier
) {
    val contacts by viewModel.contacts.collectAsState()
    val simMessages by viewModel.simMessages.collectAsState()
    val isSimulating by viewModel.isSimulating.collectAsState()
    val simStepLog by viewModel.simStepLog.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var selectedContactName by remember { mutableStateOf(contacts.firstOrNull()?.contactName ?: "Mom") }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Keep selectedContactName in sync if contacts load
    LaunchedEffect(contacts) {
        if (contacts.isNotEmpty() && !contacts.any { it.contactName == selectedContactName }) {
            selectedContactName = contacts.first().contactName
        }
    }

    // Scroll to bottom when messages change
    LaunchedEffect(simMessages.size) {
        if (simMessages.isNotEmpty()) {
            listState.animateScrollToItem(simMessages.size - 1)
        }
    }

    val selectedContact = contacts.find { it.contactName == selectedContactName }
    val decryptedMemory = remember(selectedContact) {
        val raw = selectedContact?.memoryNotes ?: ""
        if (EncryptionManager.isEncrypted(raw)) {
            EncryptionManager.decrypt(raw)
        } else raw
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("simulator_screen")
    ) {
        // 1. Contact Selector Bar
        Surface(
            color = CardDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Select Simulation Contact",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondaryDark
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val contactNames = if (contacts.isNotEmpty()) contacts.map { it.contactName } else listOf("Mom", "Alex (Work)", "Sarah")
                    items(contactNames) { name ->
                        FilterChip(
                            selected = selectedContactName == name,
                            onClick = { selectedContactName = name },
                            label = { Text(name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = EmeraldLight,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = TextSecondaryDark
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedContactName == name,
                                borderColor = if (selectedContactName == name) EmeraldPrimary else SurfaceBorder
                            )
                        )
                    }
                }

                // Active Tone & Memory Insight
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = CyberTeal,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tone: ${selectedContact?.customTone ?: settings.defaultTone}",
                            fontSize = 11.sp,
                            color = CyberTeal,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Delay: ${settings.typingDelaySeconds}s",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                if (decryptedMemory.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "🧠 Memory: $decryptedMemory",
                        fontSize = 11.sp,
                        color = TextSecondaryDark,
                        maxLines = 1
                    )
                }
            }
        }

        // 2. Chat transcript
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(simMessages, key = { it.id }) { msg ->
                SimChatBubble(message = msg)
            }
        }

        // 3. Pipeline / Thinking status indicator
        AnimatedVisibility(
            visible = isSimulating,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                color = CardDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberTeal.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = CyberTeal
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = simStepLog,
                        fontSize = 12.sp,
                        color = CyberTeal,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 4. Quick Prompts suggestions
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(QUICK_TEST_PROMPTS) { prompt ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CardDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier.clickable {
                        inputText = prompt
                    }
                ) {
                    Text(
                        text = prompt,
                        fontSize = 11.sp,
                        color = TextSecondaryDark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 5. Input Bar
        Surface(
            color = CardDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Type test message from $selectedContactName...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("simulator_text_input"),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = SurfaceBorder
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val textToSend = inputText
                            inputText = ""
                            viewModel.runSimulatorMessage(selectedContactName, textToSend)
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary)
                        .testTag("simulator_send_button"),
                    enabled = !isSimulating && inputText.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Simulate Incoming",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SimChatBubble(message: SimChatMessage) {
    val isIncoming = message.isIncoming

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isIncoming) Arrangement.Start else Arrangement.End
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isIncoming) 2.dp else 16.dp,
                bottomEnd = if (isIncoming) 16.dp else 2.dp
            ),
            color = if (isIncoming) CardDark else Color(0xFF005C4B), // WhatsApp dark green bubble
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isIncoming) SurfaceBorder else Color(0xFF007A63)
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message.senderName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isIncoming) CyberTeal else EmeraldLight
                    )
                    if (message.statusBadge.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (message.statusBadge.contains("AES-256")) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = EncryptShieldGreen,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                            Text(
                                text = message.statusBadge,
                                fontSize = 9.sp,
                                color = if (isIncoming) TextMuted else EmeraldLight
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isIncoming) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Sent",
                            tint = CyberTeal,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}
