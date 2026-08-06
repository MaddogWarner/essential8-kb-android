package com.maddogwarner.essential8kb.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maddogwarner.essential8kb.data.EssentialControl
import com.maddogwarner.essential8kb.data.EssentialControlsData
import com.maddogwarner.essential8kb.data.MaturityLevel
import com.maddogwarner.essential8kb.data.MaturityLevelContent
import com.maddogwarner.essential8kb.data.OSScope
import com.maddogwarner.essential8kb.data.matches
import com.maddogwarner.essential8kb.store.ProgressStore
import com.maddogwarner.essential8kb.store.StepStatus
import com.maddogwarner.essential8kb.ui.components.SectionHeader
import kotlin.math.roundToInt

@Composable
fun ControlDetailScreen(
    control: EssentialControl,
    stepStatuses: Map<String, StepStatus>,
    progressStore: ProgressStore,
    targetLevel: MaturityLevel,
    osScope: OSScope,
    onMaturityLevelSelected: (MaturityLevel, MaturityLevelContent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allSteps = control.steps(targetLevel, osScope)
    val completedCount = progressStore.completedCount(allSteps, stepStatuses)
    val naCount = progressStore.notApplicableCount(allSteps, stepStatuses)
    val compliancePercentage = progressStore.compliancePercentage(allSteps, stepStatuses)

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
                    progress = { (compliancePercentage / 100.0).toFloat() },
                    color = if (compliancePercentage == 100.0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$completedCount of ${allSteps.size} steps complete",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (naCount > 0) {
                            Text(
                                text = "$naCount step(s) not applicable",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFF9800), // Orange
                            )
                        }
                    }
                    Text(
                        text = "${compliancePercentage.roundToInt()}% Compliant",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (compliancePercentage == 100.0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    if (compliancePercentage == 100.0) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = "All steps complete",
                            tint = Color(0xFF4CAF50),
                        )
                    }
                }
            }
            Text(
                text = "Measured against your target of ML${targetLevel.level}. Change the target on the home dashboard.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp)
            )
        }

        item {
            SectionHeader("Maturity Levels")
        }
        item {
            MaturityRow(MaturityLevel.ML1, control.ml1, stepStatuses, progressStore, targetLevel, osScope, onMaturityLevelSelected)
        }
        item {
            MaturityRow(MaturityLevel.ML2, control.ml2, stepStatuses, progressStore, targetLevel, osScope, onMaturityLevelSelected)
        }
        item {
            MaturityRow(MaturityLevel.ML3, control.ml3, stepStatuses, progressStore, targetLevel, osScope, onMaturityLevelSelected)
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
    stepStatuses: Map<String, StepStatus>,
    progressStore: ProgressStore,
    targetLevel: MaturityLevel,
    osScope: OSScope,
    onMaturityLevelSelected: (MaturityLevel, MaturityLevelContent) -> Unit,
) {
    val scopedSteps = content.steps.filter { it.matches(osScope) }
    val doneCount = progressStore.completedCount(scopedSteps, stepStatuses)
    val naCount = progressStore.notApplicableCount(scopedSteps, stepStatuses)
    val totalCount = scopedSteps.size
    val isBeyondTarget = level.level > targetLevel.level

    val progressText = if (naCount > 0) {
        "$doneCount/$totalCount ($naCount N/A)"
    } else {
        "$doneCount/$totalCount"
    }

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
            if (isBeyondTarget) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Beyond target",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.labelLarge,
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
