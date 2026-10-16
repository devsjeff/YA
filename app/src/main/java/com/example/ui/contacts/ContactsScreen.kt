package com.example.ui.contacts

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.EncryptionManager
import com.example.data.local.ContactProfile
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

val PRESET_TONES = listOf(
    "Warm & Friendly",
    "Crisp & Professional",
    "Casual & Witty",
    "Direct & Concise",
    "Playful & Affectionate",
    "Custom Instructions"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: ReplyPilotViewModel,
    modifier: Modifier = Modifier
) {
    val contacts by viewModel.contacts.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var editingContact by remember { mutableStateOf<ContactProfile?>(null) }
    var isSheetOpen by remember { mutableStateOf(false) }

    val filteredContacts = contacts.filter { contact ->
        val matchesSearch = contact.contactName.contains(searchQuery, ignoreCase = true) ||
                contact.phoneOrHandle.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "VIP" -> contact.isVip
            "Active" -> contact.isAutoReplyEnabled && !contact.isBlacklisted
            "Blacklisted" -> contact.isBlacklisted
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("contacts_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contact_search_input"),
                placeholder = { Text("Search contact or phone number...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSecondaryDark
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedContainerColor = CardDark,
                    unfocusedContainerColor = CardDark
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val filters = listOf("All", "VIP", "Active", "Blacklisted")
                items(filters) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = EmeraldLight,
                            containerColor = CardDark,
                            labelColor = TextSecondaryDark
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedFilter == filter,
                            borderColor = if (selectedFilter == filter) EmeraldPrimary else SurfaceBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Contact List
            if (filteredContacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching contacts found" else "No contacts added yet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondaryDark
                        )
                        Text(
                            text = "Tap + to add a custom tone & memory profile",
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
                    items(filteredContacts, key = { it.id }) { contact ->
                        val decryptedMemory = if (EncryptionManager.isEncrypted(contact.memoryNotes)) {
                            EncryptionManager.decrypt(contact.memoryNotes)
                        } else contact.memoryNotes

                        ContactCard(
                            contact = contact,
                            decryptedMemory = decryptedMemory,
                            onEdit = {
                                editingContact = contact
                                isSheetOpen = true
                            },
                            onToggleAutoReply = { isEnabled ->
                                viewModel.saveContact(
                                    contactName = contact.contactName,
                                    phoneOrHandle = contact.phoneOrHandle,
                                    customTone = contact.customTone,
                                    memoryNotes = decryptedMemory,
                                    isAutoReplyEnabled = isEnabled,
                                    isVip = contact.isVip,
                                    isBlacklisted = contact.isBlacklisted,
                                    existingId = contact.id
                                )
                            },
                            onToggleVip = {
                                viewModel.saveContact(
                                    contactName = contact.contactName,
                                    phoneOrHandle = contact.phoneOrHandle,
                                    customTone = contact.customTone,
                                    memoryNotes = decryptedMemory,
                                    isAutoReplyEnabled = contact.isAutoReplyEnabled,
                                    isVip = !contact.isVip,
                                    isBlacklisted = contact.isBlacklisted,
                                    existingId = contact.id
                                )
                            }
                        )
                    }
                }
            }
        }

        // Add Floating Action Button
        FloatingActionButton(
            onClick = {
                editingContact = null
                isSheetOpen = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_contact_fab"),
            containerColor = EmeraldPrimary,
            contentColor = Color.Black,
            shape = CircleShape
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Contact")
        }

        // Edit/Add Contact Bottom Sheet
        if (isSheetOpen) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { isSheetOpen = false },
                sheetState = sheetState,
                containerColor = CardDark
            ) {
                ContactEditorSheetContent(
                    contact = editingContact,
                    onSave = { name, phone, tone, memory, isAuto, isVip, isBlacklisted ->
                        viewModel.saveContact(
                            contactName = name,
                            phoneOrHandle = phone,
                            customTone = tone,
                            memoryNotes = memory,
                            isAutoReplyEnabled = isAuto,
                            isVip = isVip,
                            isBlacklisted = isBlacklisted,
                            existingId = editingContact?.id ?: 0L
                        )
                        isSheetOpen = false
                    },
                    onDelete = {
                        editingContact?.let { viewModel.deleteContact(it) }
                        isSheetOpen = false
                    },
                    onDismiss = { isSheetOpen = false }
                )
            }
        }
    }
}

@Composable
fun ContactCard(
    contact: ContactProfile,
    decryptedMemory: String,
    onEdit: () -> Unit,
    onToggleAutoReply: (Boolean) -> Unit,
    onToggleVip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (contact.isVip) EncryptShieldGold.copy(alpha = 0.5f) else SurfaceBorder
        )
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (contact.isBlacklisted) AlertRed.copy(alpha = 0.2f)
                                else EmeraldPrimary.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = contact.contactName.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = if (contact.isBlacklisted) AlertRed else EmeraldLight,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = contact.contactName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (contact.isVip) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "VIP",
                                    tint = EncryptShieldGold,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            if (contact.isBlacklisted) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = AlertRed.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Blacklisted",
                                        color = AlertRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        if (contact.phoneOrHandle.isNotBlank()) {
                            Text(
                                text = contact.phoneOrHandle,
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleVip) {
                        Icon(
                            imageVector = if (contact.isVip) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Toggle VIP",
                            tint = if (contact.isVip) EncryptShieldGold else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Switch(
                        checked = contact.isAutoReplyEnabled && !contact.isBlacklisted,
                        onCheckedChange = { onToggleAutoReply(it) },
                        enabled = !contact.isBlacklisted,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EmeraldLight,
                            checkedTrackColor = EmeraldPrimary.copy(alpha = 0.5f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tone & Memory Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = CyberTeal.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = CyberTeal,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = contact.customTone,
                            fontSize = 11.sp,
                            color = CyberTeal,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (contact.isEncrypted) {
                    Surface(
                        color = EncryptShieldGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = EncryptShieldGreen,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Encrypted Memory",
                                fontSize = 10.sp,
                                color = EncryptShieldGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (decryptedMemory.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🧠 Memory: $decryptedMemory",
                    fontSize = 12.sp,
                    color = TextSecondaryDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ContactEditorSheetContent(
    contact: ContactProfile?,
    onSave: (String, String, String, String, Boolean, Boolean, Boolean) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val initialMemory = remember(contact) {
        if (contact != null) {
            if (EncryptionManager.isEncrypted(contact.memoryNotes)) {
                EncryptionManager.decrypt(contact.memoryNotes)
            } else contact.memoryNotes
        } else ""
    }

    var name by remember { mutableStateOf(contact?.contactName ?: "") }
    var phone by remember { mutableStateOf(contact?.phoneOrHandle ?: "") }
    var selectedTone by remember { mutableStateOf(contact?.customTone ?: "Warm & Friendly") }
    var memoryNotes by remember { mutableStateOf(initialMemory) }
    var isAutoReply by remember { mutableStateOf(contact?.isAutoReplyEnabled ?: true) }
    var isVip by remember { mutableStateOf(contact?.isVip ?: false) }
    var isBlacklisted by remember { mutableStateOf(contact?.isBlacklisted ?: false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("contact_editor_sheet")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (contact == null) "New Contact Profile" else "Edit Contact Profile",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (contact != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Contact",
                        tint = AlertRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Contact Name
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Contact Name (matches WhatsApp chat title)") },
            placeholder = { Text("e.g. Mom, Alex (Work), Sarah") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("contact_name_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldPrimary,
                unfocusedBorderColor = SurfaceBorder
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Phone or identifier
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone / Handle (optional)") },
            placeholder = { Text("e.g. +1 555-0199") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldPrimary,
                unfocusedBorderColor = SurfaceBorder
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Tone Selector
        Text(
            text = "Custom AI Tone for this Contact",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(PRESET_TONES) { tone ->
                FilterChip(
                    selected = selectedTone == tone,
                    onClick = { selectedTone = tone },
                    label = { Text(tone, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberTeal.copy(alpha = 0.2f),
                        selectedLabelColor = CyberTeal,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = TextSecondaryDark
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedTone == tone,
                        borderColor = if (selectedTone == tone) CyberTeal else SurfaceBorder
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Memory Notes
        Text(
            text = "Contact Memory Notes (Encrypted Locally)",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Facts and background the AI should keep in mind when auto-replying.",
            fontSize = 11.sp,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = memoryNotes,
            onValueChange = { memoryNotes = it },
            placeholder = {
                Text("e.g. Mom: Always ask how her knees are feeling. Let her know I'm visiting this Sunday.")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .testTag("contact_memory_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldPrimary,
                unfocusedBorderColor = SurfaceBorder
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Auto-Reply Enabled", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("Allow automated replies for this contact", fontSize = 11.sp, color = TextMuted)
            }
            Switch(
                checked = isAutoReply && !isBlacklisted,
                onCheckedChange = { isAutoReply = it },
                enabled = !isBlacklisted
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("VIP Contact", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("Pin to top with gold priority star", fontSize = 11.sp, color = TextMuted)
            }
            Switch(checked = isVip, onCheckedChange = { isVip = it })
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Blacklist", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AlertRed)
                Text("Never send auto-replies to this person", fontSize = 11.sp, color = TextMuted)
            }
            Switch(checked = isBlacklisted, onCheckedChange = { isBlacklisted = it })
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Save Button
        Button(
            onClick = {
                if (name.isNotBlank()) {
                    onSave(name, phone, selectedTone, memoryNotes, isAutoReply, isVip, isBlacklisted)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("save_contact_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldPrimary,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Save Contact Profile", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
