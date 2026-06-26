package com.maddogwarner.essential8kb.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maddogwarner.essential8kb.data.EssentialControl
import com.maddogwarner.essential8kb.data.EssentialControlsData
import com.maddogwarner.essential8kb.data.MaturityLevel
import com.maddogwarner.essential8kb.data.MaturityLevelContent
import com.maddogwarner.essential8kb.data.allStepIds
import com.maddogwarner.essential8kb.ui.components.SectionHeader

@Composable
fun ControlDetailScreen(
    control: EssentialControl,
    completedStepIds: Set<String>,
    onMaturityLevelSelected: (MaturityLevel, MaturityLevelContent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allStepIds = control.allStepIds()
    val completedCount = allStepIds.count { it in completedStepIds }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        item {
            SectionHeader("Overview")
            CardBlock {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = control.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(control.name, style = MaterialTheme.typography.titleLarge)
                }
                Text(control.overview)
            }
        }

        item {
            SectionHeader("Baseline")
            CardBlock {
                Text("ML0 - No controls implemented", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = EssentialControlsData.ml0GenericDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = control.ml0Description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionHeader("Implementation Progress")
            CardBlock {
                LinearProgressIndicator(
                    progress = { if (allStepIds.isEmpty()) 0f else completedCount.toFloat() / allStepIds.size },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$completedCount of ${allStepIds.size} steps complete",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    if (allStepIds.isNotEmpty() && completedCount == allStepIds.size) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = "All steps complete",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        item {
            SectionHeader("Maturity Levels")
        }
        item {
            MaturityRow(MaturityLevel.ML1, control.ml1, completedStepIds, onMaturityLevelSelected)
        }
        item {
            MaturityRow(MaturityLevel.ML2, control.ml2, completedStepIds, onMaturityLevelSelected)
        }
        item {
            MaturityRow(MaturityLevel.ML3, control.ml3, completedStepIds, onMaturityLevelSelected)
        }
        item {
            Text(
                text = "Select a maturity level to see the specific configuration changes required.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MaturityRow(
    level: MaturityLevel,
    content: MaturityLevelContent,
    completedStepIds: Set<String>,
    onMaturityLevelSelected: (MaturityLevel, MaturityLevelContent) -> Unit,
) {
    val completedCount = content.steps.count { it.id in completedStepIds }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMaturityLevelSelected(level, content) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = level.shortName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(level.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = content.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "$completedCount/${content.steps.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
