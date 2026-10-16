package com.example.ui.dashboard

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.EncryptionManager
import com.example.service.ServiceStateMonitor
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

@Composable
fun DashboardScreen(
    viewModel: ReplyPilotViewModel,
    onNavigateToSimulator: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val isA11yActive by viewModel.isAccessibilityActive.collectAsState()
    val isNotifActive by viewModel.isNotificationListenerActive.collectAsState()
    val isBatteryIgnored by viewModel.isBatteryOptimizationIgnored.collectAsState()
    val todayCount by viewModel.todayRepliesCount.collectAsState()
    val totalCount by viewModel.totalRepliesCount.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val recentMessages by viewModel.recentMessages.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Master Auto-Reply Banner Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("master_switch_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (settings.isMasterEnabled) CardDark else MaterialTheme.colorScheme.surfaceVariant
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (settings.isMasterEnabled) EmeraldPrimary else SurfaceBorder
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (settings.isMasterEnabled) EmeraldPrimary.copy(alpha = 0.2f)
                                        else AlertRed.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .scale(if (settings.isMasterEnabled) pulseScale else 1f)
                                        .clip(CircleShape)
                                        .background(
                                            if (settings.isMasterEnabled) EmeraldLight else AlertRed
                                        )
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = if (settings.isMasterEnabled) "ReplyPilot is ACTIVE" else "ReplyPilot is PAUSED",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (settings.isMasterEnabled) "Auto-responding to WhatsApp messages" else "Auto-replies currently paused",
                                    fontSize = 12.sp,
                                    color = TextSecondaryDark
                                )
                            }
                        }

                        Switch(
                            checked = settings.isMasterEnabled,
                            onCheckedChange = { viewModel.toggleMasterSwitch() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmeraldLight,
                                checkedTrackColor = EmeraldPrimary.copy(alpha = 0.5f),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceBorder
                            ),
                            modifier = Modifier.testTag("master_toggle_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QuickPill(
                            icon = Icons.Default.Psychology,
                            label = "Model: ${settings.selectedModel.removePrefix("gemini-")}",
                            tint = CyberTeal
                        )
                        QuickPill(
                            icon = Icons.Default.Lock,
                            label = if (settings.isLocalEncryptionEnabled) "AES-256 Encrypted" else "Plaintext",
                            tint = if (settings.isLocalEncryptionEnabled) EncryptShieldGreen else TextMuted
                        )
                    }
                }
            }
        }

        // 2. Permission & System Health Status
        item {
            Text(
                text = "Service Permissions",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Accessibility Service
                PermissionTile(
                    title = "Accessibility Service",
                    subtitle = if (isA11yActive) "Active: Reads & types into WhatsApp" else "Required: Enable to auto-type and click send",
                    isGranted = isA11yActive,
                    icon = Icons.Default.AccessibilityNew,
                    actionLabel = if (isA11yActive) "Configured" else "Enable in Settings",
                    onActionClick = {
                        context.startActivity(ServiceStateMonitor.getAccessibilitySettingsIntent())
                    },
                    testTag = "perm_accessibility_tile"
                )

                // Notification Access
                PermissionTile(
                    title = "Notification Direct Reply",
                    subtitle = if (isNotifActive) "Active: Instant background auto-replies" else "Recommended: Reply from background notifications",
                    isGranted = isNotifActive,
                    icon = Icons.Default.NotificationsActive,
                    actionLabel = if (isNotifActive) "Active" else "Grant Access",
                    onActionClick = {
                        context.startActivity(ServiceStateMonitor.getNotificationListenerSettingsIntent())
                    },
                    testTag = "perm_notification_tile"
                )

                // Battery Optimization
                PermissionTile(
                    title = "Background Battery Keeper",
                    subtitle = if (isBatteryIgnored) "Unrestricted: 24/7 background availability" else "Recommended: Prevent Android from killing service",
                    isGranted = isBatteryIgnored,
                    icon = Icons.Default.BatteryAlert,
                    actionLabel = if (isBatteryIgnored) "Optimized" else "Allow",
                    onActionClick = {
                        context.startActivity(ServiceStateMonitor.getBatteryOptimizationIntent(context.packageName))
                    },
                    testTag = "perm_battery_tile"
                )
            }
        }

        // 3. Stats & Metrics
        item {
            Text(
                text = "Activity Overview",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Today's Replies",
                    value = todayCount.toString(),
                    icon = Icons.Default.Bolt,
                    color = EmeraldLight,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Total Sent",
                    value = totalCount.toString(),
                    icon = Icons.Default.CheckCircle,
                    color = CyberTeal,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Custom Contacts",
                    value = contacts.size.toString(),
                    icon = Icons.Default.Psychology,
                    color = EncryptShieldGold,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToContacts
                )
            }
        }

        // 4. Simulator Quick Launch Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToSimulator() }
                    .testTag("simulator_banner"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardDark
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberTeal.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyberTeal.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Test Simulator",
                                tint = CyberTeal,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "WhatsApp Test Simulator",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Simulate incoming chats & test custom tones live",
                                fontSize = 12.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onNavigateToSimulator,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = CyberTeal.copy(alpha = 0.2f),
                            contentColor = CyberTeal
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Launch")
                    }
                }
            }
        }

        // 5. Recent Auto-Replies
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Auto-Replies",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp)
                )
                if (recentMessages.isNotEmpty()) {
                    Text(
                        text = "View All",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldLight,
                        modifier = Modifier
                            .clickable { onNavigateToHistory() }
                            .padding(end = 4.dp)
                    )
                }
            }
        }

        if (recentMessages.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "No replies yet",
                            tint = TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No auto-replies dispatched yet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondaryDark
                        )
                        Text(
                            text = "Use the Test Simulator to trigger your first AI response",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(recentMessages.take(4)) { msg ->
                val displayIncoming = if (EncryptionManager.isEncrypted(msg.incomingMessage)) {
                    EncryptionManager.decrypt(msg.incomingMessage)
                } else msg.incomingMessage

                val displayReply = if (EncryptionManager.isEncrypted(msg.replyMessage)) {
                    EncryptionManager.decrypt(msg.replyMessage)
                } else msg.replyMessage

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToHistory() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = msg.contactName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (msg.isEncrypted) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = "Encrypted",
                                        tint = EncryptShieldGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = msg.toneUsed,
                                    fontSize = 11.sp,
                                    color = CyberTeal
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "“$displayIncoming”",
                            fontSize = 12.sp,
                            color = TextSecondaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "↳ $displayReply",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = EmeraldLight,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionTile(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    icon: ImageVector,
    actionLabel: String,
    onActionClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isGranted) SurfaceBorder else AlertRed.copy(alpha = 0.5f)
        )
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
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isGranted) EmeraldPrimary.copy(alpha = 0.15f)
                            else AlertRed.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isGranted) EmeraldLight else AlertRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = TextSecondaryDark,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGranted) EmeraldPrimary.copy(alpha = 0.2f) else EmeraldPrimary,
                    contentColor = if (isGranted) EmeraldLight else Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = actionLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = TextSecondaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun QuickPill(
    icon: ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = CardDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextSecondaryDark,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
