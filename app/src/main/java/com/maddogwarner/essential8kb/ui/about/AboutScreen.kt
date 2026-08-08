package com.maddogwarner.essential8kb.ui.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.maddogwarner.essential8kb.data.AppInformation
import com.maddogwarner.essential8kb.data.ReferenceLink
import com.maddogwarner.essential8kb.data.OSScope
import com.maddogwarner.essential8kb.store.BackupException
import com.maddogwarner.essential8kb.store.BackupFile
import com.maddogwarner.essential8kb.ui.components.SectionHeader
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AboutScreen(
    osScope: OSScope,
    onOSScopeChanged: (OSScope) -> Unit,
    referenceOnlyMode: Boolean,
    onReferenceOnlyModeChanged: (Boolean) -> Unit,
    deepAuditEnabled: Boolean,
    onDeepAuditEnabledChanged: (Boolean) -> Unit,
    multiProfileEnabled: Boolean,
    onMultiProfileEnabledChanged: (Boolean) -> Unit,
    activeProfileName: String,
    profileCount: Int,
    onProfilesSelected: () -> Unit,
    onExportBackup: suspend (allProfiles: Boolean) -> BackupFile,
    onImportAsNewProfile: suspend (BackupFile) -> Unit,
    onImportFullDevice: suspend (BackupFile) -> Unit,
    onResetAppData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showingResetDialog by remember { mutableStateOf(false) }
    var showingExportOptions by remember { mutableStateOf(false) }
    var pendingExportAllProfiles by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var pendingImport by remember { mutableStateOf<BackupFile?>(null) }
    var backupErrorMessage by remember { mutableStateOf<String?>(null) }

    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val allProfiles = pendingExportAllProfiles
        if (uri == null) {
            pendingExportAllProfiles = null
        } else if (allProfiles == null) {
            backupErrorMessage = "The backup could not be prepared."
        } else {
            scope.launch {
                try {
                    val backup = onExportBackup(allProfiles)
                    withContext(Dispatchers.IO) {
                        val bytes = BackupFile.encode(backup)
                        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                            ?: error("Unable to open the selected document")
                    }
                } catch (error: Exception) {
                    // The picker already created the document, so remove the empty or
                    // partial file rather than leaving an unimportable backup behind.
                    withContext(Dispatchers.IO) {
                        runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri) }
                    }
                    backupErrorMessage = if (error is BackupException) {
                        error.message
                    } else {
                        "The backup could not be saved."
                    }
                } finally {
                    pendingExportAllProfiles = null
                }
            }
        }
    }
    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val bytes = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use(BackupFile::read)
                            ?: error("Unable to open the selected backup")
                    }
                    pendingImport = BackupFile.decode(bytes)
                } catch (error: Exception) {
                    backupErrorMessage = if (error is BackupException) {
                        error.message
                    } else {
                        "The selected backup could not be read."
                    }
                }
            }
        }
    }

    fun prepareExport(allProfiles: Boolean) {
        pendingExportAllProfiles = allProfiles
        val prefix = if (allProfiles) "AllProfiles" else sanitiseFileName(activeProfileName)
        val date = Instant.now()
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
        createDocument.launch("Essential8-$prefix-$date.json")
    }

    val openLink: (ReferenceLink) -> Unit = { reference ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(reference.url))
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No web browser found to open this link.", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        item {
            SectionHeader("Purpose")
            CardBlock {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(AppInformation.aboutDescription)
                }
                Text(
                    text = AppInformation.contentScope,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionHeader(AppInformation.aboutMeTitle)
            CardBlock {
                Text(AppInformation.aboutMeDescription)
            }
        }
        items(AppInformation.authorLinks, key = { it.url }) { reference ->
            ReferenceRow(reference, Icons.Outlined.Person, openLink)
        }

        item {
            SectionHeader(AppInformation.privacyTitle)
            CardBlock {
                Text(AppInformation.privacyPolicy)
            }
        }
        item {
            ReferenceRow(AppInformation.privacyPolicyLink, Icons.Outlined.PrivacyTip, openLink)
        }

        item {
            SectionHeader("Assessment Features")
            CardBlock {
                SettingSwitchRow(
                    title = "Multiple Profiles",
                    checked = multiProfileEnabled,
                    onCheckedChange = onMultiProfileEnabledChanged,
                )
                SettingSwitchRow(
                    title = "Deep Audit Mode",
                    checked = deepAuditEnabled,
                    onCheckedChange = onDeepAuditEnabledChanged,
                )
            }
            Text(
                text = "Multiple Profiles lets you track separate environments or organisations, each with its own progress, settings and audit history. Deep Audit Mode records a timestamped, optionally-annotated history for every status change in the active profile.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (multiProfileEnabled) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onProfilesSelected),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Profiles", style = MaterialTheme.typography.titleMedium)
                        Text(
                            activeProfileName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = "The active profile determines what every screen shows. Switching profiles changes the dashboard, steps and audit history.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        item {
            SectionHeader("Preferences")
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "OS Scope",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            OSScope.entries.forEachIndexed { index, scope ->
                                SegmentedButton(
                                    selected = osScope == scope,
                                    onClick = { onOSScopeChanged(scope) },
                                    shape = SegmentedButtonDefaults.itemShape(index, OSScope.entries.size),
                                    label = {
                                        Text(
                                            when (scope) {
                                                OSScope.WORKSTATION -> "Workstation"
                                                OSScope.SERVER -> "Server"
                                                OSScope.BOTH -> "Both"
                                            },
                                        )
                                    },
                                )
                            }
                        }
                    }
                    SettingSwitchRow(
                        title = "Reference Only Mode",
                        checked = referenceOnlyMode,
                        onCheckedChange = onReferenceOnlyModeChanged,
                    )
                }
            }
            Text(
                text = "OS scope hides implementation steps that don't apply to the selected environment and recalculates compliance over the remaining steps. Reference Only Mode hides the compliance dashboard on the home screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        item {
            SectionHeader("Tools & Feedback")
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showingResetDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Reset App Data",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        item {
            SectionHeader("Backup & Restore")
        }
        item {
            BackupActionRow(
                title = "Export Backup",
                icon = Icons.Outlined.FileUpload,
                onClick = {
                    if (profileCount > 1) showingExportOptions = true else prepareExport(false)
                },
            )
        }
        item {
            BackupActionRow(
                title = "Import Backup",
                icon = Icons.Outlined.FileDownload,
                onClick = { openDocument.launch(arrayOf("application/json", "text/json")) },
            )
            Text(
                text = "Backups are plain JSON containing your profiles — step statuses, N/A reasons, audit history and per-profile settings. Export this profile to share one assessment, or all profiles to move everything to another device. They never leave your device unless you share them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        item {
            SectionHeader("MITRE ATT&CK®")
            CardBlock {
                Text(AppInformation.attackDisclaimer)
                Text(AppInformation.attackCoverageCaveat)
                Text(AppInformation.attackVersionNote)
                Text(AppInformation.attackAttribution)
            }
        }

        item {
            SectionHeader("References")
        }
        items(AppInformation.referenceLinks, key = { it.url }) { reference ->
            ReferenceRow(reference, Icons.Outlined.Link, openLink)
        }
        item {
            Text(
                text = "External links open outside the app and should be used to verify current ASD and Microsoft guidance before implementation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showingResetDialog) {
        AlertDialog(
            onDismissRequest = { showingResetDialog = false },
            title = { Text("Reset App Data") },
            text = { Text("This clears all profiles, progress, audit history and app settings. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showingResetDialog = false
                        onResetAppData()
                    }
                ) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showingResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showingExportOptions) {
        AlertDialog(
            onDismissRequest = { showingExportOptions = false },
            title = { Text("Export Backup") },
            text = { Text("Choose what to include in the backup.") },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        showingExportOptions = false
                        prepareExport(false)
                    }) { Text("This profile only") }
                    TextButton(onClick = {
                        showingExportOptions = false
                        prepareExport(true)
                    }) { Text("All profiles") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showingExportOptions = false }) { Text("Cancel") }
            },
        )
    }

    pendingImport?.let { backup ->
        val isFullDevice = backup.globalSettings != null
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(if (isFullDevice) "Replace everything?" else "Import profile?") },
            text = {
                val count = backup.profiles.size
                val date = Instant.ofEpochMilli(backup.exportedAt)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                Text(
                    if (isFullDevice) {
                        "This replaces all profiles and app settings with the backup from $date ($count profile(s), app version ${backup.appVersion}). This cannot be undone."
                    } else {
                        "This adds $count profile(s) from the backup of $date (app version ${backup.appVersion}). Your existing profiles are unchanged."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    scope.launch {
                        try {
                            if (isFullDevice) onImportFullDevice(backup) else onImportAsNewProfile(backup)
                        } catch (error: Exception) {
                            backupErrorMessage = error.message ?: "The backup could not be imported."
                        }
                    }
                }) {
                    Text(
                        if (isFullDevice) "Replace" else "Import",
                        color = if (isFullDevice) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) { Text("Cancel") }
            },
        )
    }

    backupErrorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { backupErrorMessage = null },
            title = { Text("Backup Error") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { backupErrorMessage = null }) { Text("OK") }
            },
        )
    }
}

internal fun sanitiseFileName(name: String): String =
    name.replace(Regex("[^\\p{L}\\p{N}_-]"), "-").trim('-').ifEmpty { "Profile" }

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun BackupActionRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ReferenceRow(
    reference: ReferenceLink,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (ReferenceLink) -> Unit,
) {
    val url = reference.url
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(reference) },
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(reference.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = Uri.parse(url).host ?: url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CardBlock(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}
