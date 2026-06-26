package com.maddogwarner.essential8kb.ui.maturity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maddogwarner.essential8kb.data.EssentialControl
import com.maddogwarner.essential8kb.data.ImplementationStep
import com.maddogwarner.essential8kb.data.MaturityLevel
import com.maddogwarner.essential8kb.data.MaturityLevelContent
import com.maddogwarner.essential8kb.data.Microsoft365AdditionalProtection
import com.maddogwarner.essential8kb.data.Microsoft365LicenseMode
import com.maddogwarner.essential8kb.ui.components.CopyableCommand
import com.maddogwarner.essential8kb.ui.components.SectionHeader

@Composable
fun MaturityLevelScreen(
    control: EssentialControl,
    level: MaturityLevel,
    content: MaturityLevelContent,
    completedStepIds: Set<String>,
    selectedLicenseMode: Microsoft365LicenseMode,
    onToggleStep: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MaturityLevelViewModel = viewModel(),
) {
    val completedCount = content.steps.count { it.id in completedStepIds }
    val protections = viewModel.microsoft365Protections(control, level, selectedLicenseMode)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        item {
            SectionHeader("What ${level.shortName} requires")
            Text(
                text = "$completedCount of ${content.steps.size} steps complete",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
            )
            Text(content.summary)
        }

        itemsIndexed(content.steps, key = { _, step -> step.id }) { index, step ->
            StepCard(
                index = index,
                step = step,
                isCompleted = step.id in completedStepIds,
                onToggleStep = onToggleStep,
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
}

@Composable
private fun StepCard(
    index: Int,
    step: ImplementationStep,
    isCompleted: Boolean,
    onToggleStep: (String) -> Unit,
) {
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
                IconButton(onClick = { onToggleStep(step.id) }) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = if (isCompleted) {
                            "Mark ${step.title} not implemented"
                        } else {
                            "Mark ${step.title} implemented"
                        },
                        tint = if (isCompleted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(step.title, style = MaterialTheme.typography.titleMedium)
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
