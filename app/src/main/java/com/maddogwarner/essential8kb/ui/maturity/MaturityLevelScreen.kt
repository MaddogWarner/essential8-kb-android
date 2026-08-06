package com.maddogwarner.essential8kb.ui.maturity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Warning
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maddogwarner.essential8kb.data.EssentialControl
import com.maddogwarner.essential8kb.data.ImplementationStep
import com.maddogwarner.essential8kb.data.MaturityLevel
import com.maddogwarner.essential8kb.data.MaturityLevelContent
import com.maddogwarner.essential8kb.data.Microsoft365AdditionalProtection
import com.maddogwarner.essential8kb.data.Microsoft365LicenseMode
import com.maddogwarner.essential8kb.data.OSScope
import com.maddogwarner.essential8kb.data.matches
import com.maddogwarner.essential8kb.store.ProgressStore
import com.maddogwarner.essential8kb.store.StepState
import com.maddogwarner.essential8kb.store.StepStatus
import com.maddogwarner.essential8kb.ui.components.CopyableCommand
import com.maddogwarner.essential8kb.ui.components.ISMControlsCapsules
import com.maddogwarner.essential8kb.ui.components.SectionHeader

@Composable
fun MaturityLevelScreen(
    control: EssentialControl,
    level: MaturityLevel,
    content: MaturityLevelContent,
    stepStatuses: Map<String, StepStatus>,
    progressStore: ProgressStore,
    selectedLicenseMode: Microsoft365LicenseMode,
    osScope: OSScope,
    onStatusChanged: (String, StepState, String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MaturityLevelViewModel = viewModel(),
) {
    val scopedSteps = content.steps.filter { it.matches(osScope) }
    val completedCount = progressStore.completedCount(scopedSteps, stepStatuses)
    val naCount = progressStore.notApplicableCount(scopedSteps, stepStatuses)
    val protections = remember(control, level, selectedLicenseMode) {
        viewModel.microsoft365Protections(control, level, selectedLicenseMode)
    }

    var activeStepIdForNA by remember { mutableStateOf<String?>(null) }
    var showingNAReasonDialog by remember { mutableStateOf(false) }
    var naReasonText by remember { mutableStateOf("") }

    val headerProgressText = if (naCount > 0) {
        "$completedCount of ${scopedSteps.size} steps complete ($naCount not applicable)"
    } else {
        "$completedCount of ${scopedSteps.size} steps complete"
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        item {
            SectionHeader("What ${level.shortName} requires")
            Text(
                text = headerProgressText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
            )
            Text(content.summary)
        }

        itemsIndexed(scopedSteps, key = { _, step -> step.id }) { index, step ->
            val status = stepStatuses[step.id] ?: StepStatus(StepState.NOT_IMPLEMENTED)
            StepCard(
                index = index,
                step = step,
                status = status,
                onStatusChanged = { state ->
                    if (state == StepState.NOT_APPLICABLE) {
                        activeStepIdForNA = step.id
                        naReasonText = status.reason.orEmpty()
                        showingNAReasonDialog = true
                    } else {
                        onStatusChanged(step.id, state, null)
                    }
                },
            )
        }

        content.gapNote?.let { gap ->
            item {
                GapNote(gap)
            }
        }

        if (protections.isNotEmpty()) {
            item {
                SectionHeader("M365 / MDE additions")
                Text(
                    text = "Mode: ${selectedLicenseMode.shortName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                )
            }
            itemsIndexed(protections, key = { index, protection -> "${protection.title}-$index" }) { _, protection ->
                Microsoft365ProtectionCard(protection)
            }
            item {
                Text(
                    text = "These licensed protections are additional or partial supports. They do not replace the core Essential Eight implementation steps above.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showingNAReasonDialog) {
        AlertDialog(
            onDismissRequest = { showingNAReasonDialog = false },
            title = { Text("Not Applicable Reason") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Provide a reason why this step is not applicable in your environment.")
                    OutlinedTextField(
                        value = naReasonText,
                        onValueChange = { naReasonText = it },
                        placeholder = { Text("Enter reason (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showingNAReasonDialog = false
                    val stepId = activeStepIdForNA
                    if (stepId != null) {
                        val reason = naReasonText.trim().ifEmpty { null }
                        onStatusChanged(stepId, StepState.NOT_APPLICABLE, reason)
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showingNAReasonDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StepCard(
    index: Int,
    step: ImplementationStep,
    status: StepStatus,
    onStatusChanged: (StepState) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Step ${index + 1}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        val iconVector = when (status.state) {
                            StepState.IMPLEMENTED -> Icons.Outlined.CheckCircle
                            StepState.NOT_APPLICABLE -> Icons.Outlined.Block
                            StepState.NOT_IMPLEMENTED -> Icons.Outlined.RadioButtonUnchecked
                        }
                        val tintColor = when (status.state) {
                            StepState.IMPLEMENTED -> Color(0xFF4CAF50)
                            StepState.NOT_APPLICABLE -> Color(0xFFFF9800)
                            StepState.NOT_IMPLEMENTED -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Icon(
                            imageVector = iconVector,
                            contentDescription = status.state.rawValue,
                            tint = tintColor,
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Implemented") },
                            onClick = {
                                menuExpanded = false
                                onStatusChanged(StepState.IMPLEMENTED)
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Not Applicable") },
                            onClick = {
                                menuExpanded = false
                                onStatusChanged(StepState.NOT_APPLICABLE)
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.Block, contentDescription = null, tint = Color(0xFFFF9800))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Not Implemented") },
                            onClick = {
                                menuExpanded = false
                                onStatusChanged(StepState.NOT_IMPLEMENTED)
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.RadioButtonUnchecked, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(step.title, style = MaterialTheme.typography.titleMedium)

                    if (step.osScope != OSScope.BOTH) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = if (step.osScope == OSScope.WORKSTATION) "Workstation" else "Server",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (status.state == StepState.NOT_APPLICABLE) {
                        val reasonText = if (status.reason.isNullOrBlank()) {
                            "Not Applicable (No reason provided)"
                        } else {
                            "Not Applicable: ${status.reason}"
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFF9800).copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.padding(top = 1.dp)
                            )
                            Text(
                                text = reasonText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    ISMControlsCapsules(controls = step.ismControls)

                    Text(step.description)
                    step.technicalDetails.forEach { detail ->
                        CopyableCommand(text = detail)
                    }
                }
            }
        }
    }
}

@Composable
private fun GapNote(gap: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionHeader("Beyond Windows built-in tooling")
                Text(
                    text = gap,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun Microsoft365ProtectionCard(protection: Microsoft365AdditionalProtection) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(protection.title, style = MaterialTheme.typography.titleMedium)
            Text(protection.coverage)
            protection.basicSettings.forEach { setting ->
                CopyableCommand(text = setting)
            }
        }
    }
}
