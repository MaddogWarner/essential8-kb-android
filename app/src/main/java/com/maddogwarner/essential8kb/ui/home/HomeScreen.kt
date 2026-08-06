package com.maddogwarner.essential8kb.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ManageSearch
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maddogwarner.essential8kb.data.EssentialControl
import com.maddogwarner.essential8kb.data.MaturityLevel
import com.maddogwarner.essential8kb.data.OSScope
import com.maddogwarner.essential8kb.store.ProgressStore
import com.maddogwarner.essential8kb.store.StepState
import com.maddogwarner.essential8kb.store.StepStatus
import com.maddogwarner.essential8kb.ui.components.ComplianceBarChart
import com.maddogwarner.essential8kb.ui.components.ComplianceRing
import com.maddogwarner.essential8kb.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    controls: List<EssentialControl>,
    stepStatuses: Map<String, StepStatus>,
    progressStore: ProgressStore,
    targetLevel: MaturityLevel,
    osScope: OSScope,
    onTargetLevelChanged: (MaturityLevel) -> Unit,
    referenceOnlyMode: Boolean,
    onControlSelected: (EssentialControl) -> Unit,
    onAuditPolicySelected: () -> Unit,
    onMicrosoft365Selected: () -> Unit,
    onAboutSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val inScopeSteps = controls.flatMap { it.steps(targetLevel, osScope) }
    val overallTotalSteps = inScopeSteps.size
    val overallImplementedSteps = inScopeSteps.count { stepStatuses[it.id]?.state == StepState.IMPLEMENTED }
    val overallNASteps = inScopeSteps.count { stepStatuses[it.id]?.state == StepState.NOT_APPLICABLE }
    val overallPendingSteps = overallTotalSteps - overallImplementedSteps - overallNASteps
    val overallCompliancePercentage = progressStore.compliancePercentage(inScopeSteps, stepStatuses)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        if (!referenceOnlyMode) {
            item {
                SectionHeader("Maturity Dashboard")
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Target Picker
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Target Maturity Level",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                            )
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                MaturityLevel.entries.forEachIndexed { index, level ->
                                    SegmentedButton(
                                        selected = targetLevel == level,
                                        onClick = { onTargetLevelChanged(level) },
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = MaturityLevel.entries.size
                                        ),
                                        label = { Text(level.shortName) }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Compliance Ring & Stats
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ComplianceRing(percentage = overallCompliancePercentage)

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Compliance Summary",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )

                                LegendItem(color = Color(0xFF4CAF50), text = "Implemented: $overallImplementedSteps")
                                LegendItem(color = Color(0xFFFF9800), text = "Not Applicable: $overallNASteps")
                                LegendItem(color = MaterialTheme.colorScheme.outlineVariant, text = "Pending: $overallPendingSteps")
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Breakdown Chart
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Maturity Breakdown by Control",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                            )
                            ComplianceBarChart(
                                controls = controls,
                                statuses = stepStatuses,
                                targetLevel = targetLevel,
                                osScope = osScope,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionHeader("ASD Essential Eight")
        }
        items(controls, key = { it.id }) { control ->
            val isComplete = progressStore.isControlComplete(control, targetLevel, osScope, stepStatuses)
            ControlRow(
                control = control,
                isComplete = isComplete,
                onClick = { onControlSelected(control) },
            )
        }
        item {
            Text(
                text = "Content is scoped to controls achievable using built-in Windows OS tooling. Verify against the current ASD Essential Eight Maturity Model before implementation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        item {
            SectionHeader("Event Logging")
        }
        item {
            UtilityRow(
                title = "Windows Audit Policy",
                subtitle = "ASD recommended minimum Windows Security Audit Policy settings for detection and response.",
                icon = { Icon(Icons.Outlined.ManageSearch, contentDescription = null) },
                onClick = onAuditPolicySelected,
            )
        }

        item {
            SectionHeader("Settings")
        }
        item {
            UtilityRow(
                title = "M365 Additional Controls",
                subtitle = "Choose the Microsoft 365 security additions shown on maturity-level pages.",
                icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                onClick = onMicrosoft365Selected,
            )
        }
        item {
            UtilityRow(
                title = "About & Privacy",
                subtitle = "Purpose, privacy position and external references.",
                icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                onClick = onAboutSelected,
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ControlRow(
    control: EssentialControl,
    isComplete: Boolean,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        ListItem(
            headlineContent = { Text(control.name) },
            supportingContent = { Text("Mitigation ${control.id}") },
            leadingContent = {
                Icon(
                    imageVector = control.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            trailingContent = {
                if (isComplete) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "All steps complete",
                        tint = Color(0xFF4CAF50), // Match Green iOS checkmark
                    )
                }
            },
        )
    }
}

@Composable
private fun UtilityRow(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
