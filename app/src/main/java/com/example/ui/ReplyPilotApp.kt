package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.ServiceStateMonitor
import com.example.ui.contacts.ContactsScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.history.HistoryScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.simulator.SimulatorScreen
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CardDark
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.DeepDarkBg
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondaryDark
import com.example.viewmodel.ReplyPilotViewModel

enum class NavigationDestination(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("dashboard", "Dashboard", Icons.Default.Dashboard, Icons.Outlined.Dashboard),
    CONTACTS("contacts", "Contacts", Icons.Default.People, Icons.Outlined.People),
    SIMULATOR("simulator", "Simulator", Icons.Default.Chat, Icons.Outlined.Chat),
    HISTORY("history", "History", Icons.Default.Lock, Icons.Outlined.Lock),
    SETTINGS("settings", "Settings", Icons.Default.Settings, Icons.Outlined.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplyPilotApp(
    viewModel: ReplyPilotViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentDestination by remember { mutableStateOf(NavigationDestination.DASHBOARD) }
    val settings by viewModel.settings.collectAsState()
    val isA11yActive by viewModel.isAccessibilityActive.collectAsState()

    // Periodically refresh permission status when screen is viewed
    LaunchedEffect(Unit) {
        viewModel.refreshServiceStatuses()
    }

    // Handle back button when on sub-screens
    BackHandler(enabled = currentDestination != NavigationDestination.DASHBOARD) {
        currentDestination = NavigationDestination.DASHBOARD
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DeepDarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⚡",
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ReplyPilot",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "WhatsApp AI Auto-Responder",
                                fontSize = 10.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (settings.isMasterEnabled) EmeraldPrimary.copy(alpha = 0.2f) else AlertRed.copy(alpha = 0.15f),
                            modifier = Modifier.clickable { viewModel.toggleMasterSwitch() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (settings.isMasterEnabled) EmeraldLight else AlertRed)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (settings.isMasterEnabled) "ACTIVE" else "PAUSED",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (settings.isMasterEnabled) EmeraldLight else AlertRed
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardDark,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CardDark,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationDestination.entries.forEach { destination ->
                    val isSelected = currentDestination == destination
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = destination },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = destination.title
                            )
                        },
                        label = {
                            Text(
                                text = destination.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = EmeraldLight,
                            indicatorColor = EmeraldLight,
                            unselectedIconColor = TextSecondaryDark,
                            unselectedTextColor = TextSecondaryDark
                        ),
                        modifier = Modifier.testTag("nav_${destination.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Notice banner if Accessibility is not enabled yet
            AnimatedVisibility(
                visible = !isA11yActive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = AlertRed.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AlertRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Accessibility service is off. Tap Enable to auto-type replies.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Button(
                            onClick = {
                                context.startActivity(ServiceStateMonitor.getAccessibilitySettingsIntent())
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AlertRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Enable", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Screen Content Routing
            Box(modifier = Modifier.weight(1f)) {
                when (currentDestination) {
                    NavigationDestination.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToSimulator = { currentDestination = NavigationDestination.SIMULATOR },
                        onNavigateToContacts = { currentDestination = NavigationDestination.CONTACTS },
                        onNavigateToHistory = { currentDestination = NavigationDestination.HISTORY }
                    )
                    NavigationDestination.CONTACTS -> ContactsScreen(viewModel = viewModel)
                    NavigationDestination.SIMULATOR -> SimulatorScreen(viewModel = viewModel)
                    NavigationDestination.HISTORY -> HistoryScreen(viewModel = viewModel)
                    NavigationDestination.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
