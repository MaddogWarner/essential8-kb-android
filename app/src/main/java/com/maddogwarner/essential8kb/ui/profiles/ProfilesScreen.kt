package com.maddogwarner.essential8kb.ui.profiles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maddogwarner.essential8kb.store.Profile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfilesScreen(
    profiles: List<Profile>,
    activeProfileId: String,
    showingCreate: Boolean,
    onCreateDismissed: () -> Unit,
    onSwitchProfile: (String) -> Unit,
    onCreateProfile: (String) -> Unit,
    onRenameProfile: (String, String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var profileName by remember { mutableStateOf("") }
    var profileToRename by remember { mutableStateOf<Profile?>(null) }
    var profileToDelete by remember { mutableStateOf<Profile?>(null) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("en-AU")) }

    LaunchedEffect(showingCreate) {
        if (showingCreate) profileName = ""
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        items(profiles, key = { it.id }) { profile ->
            var menuExpanded by remember(profile.id) { mutableStateOf(false) }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSwitchProfile(profile.id) },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(profile.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            dateFormatter.format(Date(profile.createdAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (profile.id == activeProfileId) {
                        Icon(Icons.Outlined.Check, contentDescription = "Active profile")
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Profile actions")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = {
                                menuExpanded = false
                                profileName = profile.name
                                profileToRename = profile
                            },
                        )
                        if (profiles.size > 1) {
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    menuExpanded = false
                                    profileToDelete = profile
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showingCreate) {
        ProfileNameDialog(
            title = "Create Profile",
            confirmLabel = "Create",
            value = profileName,
            onValueChange = { profileName = it },
            onConfirm = {
                onCreateProfile(profileName)
            },
            onDismiss = onCreateDismissed,
        )
    }

    profileToRename?.let { profile ->
        ProfileNameDialog(
            title = "Rename Profile",
            confirmLabel = "Save",
            value = profileName,
            onValueChange = { profileName = it },
            onConfirm = {
                profileToRename = null
                onRenameProfile(profile.id, profileName)
            },
            onDismiss = { profileToRename = null },
        )
    }

    profileToDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            title = { Text("Delete profile?") },
            text = { Text("Delete profile '${profile.name}'? Its progress and audit history are permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    profileToDelete = null
                    onDeleteProfile(profile.id)
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { profileToDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ProfileNameDialog(
    title: String,
    confirmLabel: String,
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("Profile name") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
