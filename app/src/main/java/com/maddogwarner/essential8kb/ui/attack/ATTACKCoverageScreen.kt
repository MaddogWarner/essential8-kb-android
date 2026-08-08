package com.maddogwarner.essential8kb.ui.attack

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maddogwarner.essential8kb.data.AppInformation
import com.maddogwarner.essential8kb.data.MaturityLevel
import com.maddogwarner.essential8kb.data.OSScope
import com.maddogwarner.essential8kb.data.attack.ATTACKCoverageCalculator
import com.maddogwarner.essential8kb.data.attack.ATTACKTechnique
import com.maddogwarner.essential8kb.data.attack.CoverageStatus
import com.maddogwarner.essential8kb.data.attack.TechniqueCoverage
import com.maddogwarner.essential8kb.store.StepStatus
import com.maddogwarner.essential8kb.ui.components.SectionHeader

@Composable
fun ATTACKCoverageScreen(
    targetLevel: MaturityLevel,
    osScope: OSScope,
    stepStatuses: Map<String, StepStatus>,
    controlFilter: Int?,
    onTechniqueSelected: (ATTACKTechnique) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groups = ATTACKCoverageCalculator.grouped(targetLevel, osScope, stepStatuses, controlFilter)
    val unique = ATTACKCoverageCalculator.uniqueCoverage(groups)
    val untallied = unique.count { it.status == CoverageStatus.INDIRECT || it.status == CoverageStatus.NOT_APPLICABLE }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        item {
            SectionHeader("Summary")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        SummaryCount("Covered", unique.count { it.status == CoverageStatus.COVERED }, Color(0xFF4CAF50))
                        SummaryCount("Partial", unique.count { it.status == CoverageStatus.PARTIAL }, Color(0xFFFF9800))
                        SummaryCount("Not covered", unique.count { it.status == CoverageStatus.NOT_COVERED }, MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = "Measured against your target of ML${targetLevel.level}, ${osScope.displayName()} scope.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (untallied > 0) {
                        val subject = if (untallied == 1) "technique is" else "techniques are"
                        Text(
                            text = "$untallied further $subject listed below as Indirect or Not applicable and are not counted above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = AppInformation.attackCoverageCaveat,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        groups.forEach { group ->
            item { SectionHeader("${group.tactic.displayName} (${group.coverage.size})") }
            items(group.coverage, key = { "${group.tactic.name}-${it.id}" }) { coverage ->
                CoverageRow(coverage) { onTechniqueSelected(coverage.technique) }
            }
        }

        item {
            Text(
                text = "A technique that belongs to several tactics appears under each. Techniques with no steps in your current maturity and OS scope are not listed. ${AppInformation.attackDisclaimerShort}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SummaryCount(label: String, count: Int, colour: Color) {
    Column {
        Text("$count", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colour)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CoverageRow(coverage: TechniqueCoverage, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = coverage.technique.id,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Text(coverage.technique.name, style = MaterialTheme.typography.bodyMedium)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = coverage.status.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = coverage.status.colour(),
                )
                if (coverage.contributingTotal > 0) {
                    Text(
                        text = "${coverage.contributingImplemented} of ${coverage.contributingTotal} steps",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CoverageStatus.colour(): Color = when (this) {
    CoverageStatus.COVERED -> Color(0xFF4CAF50)
    CoverageStatus.PARTIAL, CoverageStatus.NOT_APPLICABLE -> Color(0xFFFF9800)
    CoverageStatus.NOT_COVERED, CoverageStatus.INDIRECT -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun OSScope.displayName(): String = when (this) {
    OSScope.WORKSTATION -> "Workstation"
    OSScope.SERVER -> "Server"
    OSScope.BOTH -> "Both"
}
