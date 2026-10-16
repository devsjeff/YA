package com.example.ui.settings

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.ServiceStateMonitor
import com.example.ui.contacts.PRESET_TONES
import com.example.ui.theme.CardDark
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EncryptShieldGreen
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondaryDark
import com.example.viewmodel.ReplyPilotViewModel

val AVAILABLE_MODELS = listOf(
    "gemini-2.5-flash",
    "gemini-1.5-flash",
    "gemini-2.0-flash"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ReplyPilotViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()

    var apiKeyInput by remember(settings.geminiApiKey) { mutableStateOf(settings.geminiApiKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var isModelDropdownOpen by remember { mutableStateOf(false) }
    var isToneDropdownOpen by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: AI API & Engine
        item {
            SectionHeader(title = "AI Engine & API Key", icon = Icons.Default.Psychology)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Gemini API Key",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ReplyPilot uses your Google Gemini API key to craft personalized responses.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            viewModel.updateApiKey(it)
                        },
                        placeholder = { Text("Enter your Gemini API key...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                Icon(
                                    imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle API Key visibility",
                                    tint = TextSecondaryDark
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = SurfaceBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Model Selector Dropdown
                    Text(
                        text = "AI Model Selection",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    ExposedDropdownMenuBox(
                        expanded = isModelDropdownOpen,
                        onExpandedChange = { isModelDropdownOpen = !isModelDropdownOpen }
                    ) {
                        OutlinedTextField(
                            value = settings.selectedModel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isModelDropdownOpen) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("model_selector"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = SurfaceBorder
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = isModelDropdownOpen,
                            onDismissRequest = { isModelDropdownOpen = false }
                        ) {
                            AVAILABLE_MODELS.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model) },
                                    onClick = {
                                        viewModel.updateModel(model)
                                        isModelDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Global Persona Bio
                    Text(
                        text = "Your Persona & Current Status",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Global background injected into every reply (e.g., 'I am traveling in Japan until Friday').",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = settings.userPersonaBio,
                        onValueChange = { viewModel.updateUserPersonaBio(it) },
                        placeholder = { Text("e.g. Currently in an all-day design sprint.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(88.dp)
                            .testTag("persona_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = SurfaceBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Default Tone Selector
                    Text(
                        text = "Global Default Tone (for unscheduled contacts)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    ExposedDropdownMenuBox(
                        expanded = isToneDropdownOpen,
                        onExpandedChange = { isToneDropdownOpen = !isToneDropdownOpen }
                    ) {
                        OutlinedTextField(
                            value = settings.defaultTone,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isToneDropdownOpen) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = SurfaceBorder
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = isToneDropdownOpen,
                            onDismissRequest = { isToneDropdownOpen = false }
                        ) {
                            PRESET_TONES.forEach { tone ->
                                DropdownMenuItem(
                                    text = { Text(tone) },
                                    onClick = {
                                        viewModel.updateDefaultTone(tone)
                                        isToneDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Safety & Timing
        item {
            SectionHeader(title = "Safety & Delay Controls", icon = Icons.Default.Speed)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Typing Delay Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Typing Delay (Simulate Human Speed)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${settings.typingDelaySeconds} seconds",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberTeal
                        )
                    }

                    Slider(
                        value = settings.typingDelaySeconds.toFloat(),
                        onValueChange = { viewModel.updateTypingDelay(it.toInt()) },
                        valueRange = 1f..15f,
                        steps = 13,
                        colors = SliderDefaults.colors(
                            thumbColor = EmeraldLight,
                            activeTrackColor = EmeraldPrimary
                        ),
                        modifier = Modifier.testTag("typing_delay_slider")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Cooldown Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Cooldown Per Contact",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${settings.cooldownMinutes} minutes",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberTeal
                        )
                    }

                    Slider(
                        value = settings.cooldownMinutes.toFloat(),
                        onValueChange = { viewModel.updateCooldown(it.toInt()) },
                        valueRange = 0f..30f,
                        steps = 29,
                        colors = SliderDefaults.colors(
                            thumbColor = EmeraldLight,
                            activeTrackColor = EmeraldPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Anti-Loop Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Anti-Loop Safeguard",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Prevents infinite reply ping-pongs and self-replies",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                        Switch(
                            checked = settings.isAntiLoopProtectionEnabled,
                            onCheckedChange = { viewModel.updateAntiLoop(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmeraldLight,
                                checkedTrackColor = EmeraldPrimary.copy(alpha = 0.5f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Signature Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Append AI Signature",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Attach '${settings.signatureText}' to sent messages",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                        Switch(
                            checked = settings.appendSignature,
                            onCheckedChange = {
                                viewModel.updateSignature(it, settings.signatureText)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmeraldLight,
                                checkedTrackColor = EmeraldPrimary.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        }

        // Section: Privacy & Security
        item {
            SectionHeader(title = "Privacy & Local Encryption", icon = Icons.Default.Security)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, EncryptShieldGreen.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = EncryptShieldGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AES-256-GCM Local Encryption",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Encrypts all contact memory notes and chat history before saving to SQLite database using Android Keystore.",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }

                        Switch(
                            checked = settings.isLocalEncryptionEnabled,
                            onCheckedChange = { viewModel.updateLocalEncryption(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EncryptShieldGreen,
                                checkedTrackColor = EncryptShieldGreen.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.testTag("encryption_toggle_switch")
                        )
                    }
                }
            }
        }

        // Section: System Shortcuts
        item {
            SectionHeader(title = "System Shortcuts", icon = Icons.Default.Tune)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsActionRow(
                        title = "Open Android Accessibility Settings",
                        subtitle = "Enable or re-enable ReplyPilot Accessibility Service",
                        icon = Icons.Default.AccessibilityNew,
                        onClick = {
                            context.startActivity(ServiceStateMonitor.getAccessibilitySettingsIntent())
                        }
                    )
                    SettingsActionRow(
                        title = "Open Notification Access Settings",
                        subtitle = "Allow ReplyPilot to reply from background notifications",
                        icon = Icons.Default.NotificationsActive,
                        onClick = {
                            context.startActivity(ServiceStateMonitor.getNotificationListenerSettingsIntent())
                        }
                    )
                }
            }
        }

        // About & Version
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "About",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ReplyPilot AI v1.0",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "WhatsApp Auto-Reply Assistant • Hardware-Encrypted Logs",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = EmeraldLight,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EmeraldLight,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}
