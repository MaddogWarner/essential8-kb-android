package com.maddogwarner.essential8kb.ui.attack

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.maddogwarner.essential8kb.data.AppInformation
import com.maddogwarner.essential8kb.data.EssentialControl
import com.maddogwarner.essential8kb.data.EssentialControlsData
import com.maddogwarner.essential8kb.data.ImplementationStep
import com.maddogwarner.essential8kb.data.MaturityLevel
import com.maddogwarner.essential8kb.data.OSScope
import com.maddogwarner.essential8kb.data.attack.ATTACKMapping
import com.maddogwarner.essential8kb.data.attack.ATTACKMappingData
import com.maddogwarner.essential8kb.data.attack.ATTACKRelationship
import com.maddogwarner.essential8kb.data.attack.ATTACKTechnique
import com.maddogwarner.essential8kb.data.matches
import com.maddogwarner.essential8kb.store.StepState
import com.maddogwarner.essential8kb.store.StepStatus
import com.maddogwarner.essential8kb.ui.components.SectionHeader

private data class ATTACKMappedStep(
    val mapping: ATTACKMapping,
    val control: EssentialControl,
    val level: MaturityLevel,
    val step: ImplementationStep,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ATTACKTechniqueDetailScreen(
    technique: ATTACKTechnique,
    targetLevel: MaturityLevel,
    osScope: OSScope,
    stepStatuses: Map<String, StepStatus>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mappedSteps = ATTACKMappingData.mappingsForTechnique(technique.id).mapNotNull { mapping ->
        EssentialControlsData.all.firstNotNullOfOrNull { control ->
            MaturityLevel.entries.firstNotNullOfOrNull { level ->
                control.content(level).steps.firstOrNull { it.id == mapping.stepID }
                    ?.let { ATTACKMappedStep(mapping, control, level, it) }
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = technique.id,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(technique.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        technique.tactics.forEach { tactic ->
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                shape = RoundedCornerShape(100.dp),
                            ) {
                                Text(
                                    text = tactic.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        ATTACKRelationship.entries.forEach { relationship ->
            val matches = mappedSteps.filter { relationship in it.mapping.relationships }
            if (matches.isNotEmpty()) {
                item { SectionHeader(relationship.displayName) }
                items(matches, key = { "${relationship.name}-${it.mapping.id}" }) { item ->
                    MappedStepRow(item, targetLevel, osScope, stepStatuses)
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = AppInformation.attackDisclaimer,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.clickable {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, technique.url.toUri()))
                            } catch (_: ActivityNotFoundException) {
                                Toast.makeText(context, "No web browser found to open this link.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                        Text("View on attack.mitre.org", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun MappedStepRow(
    item: ATTACKMappedStep,
    targetLevel: MaturityLevel,
    osScope: OSScope,
    stepStatuses: Map<String, StepStatus>,
) {
    val state = stepStatuses[item.step.id]?.state ?: StepState.NOT_IMPLEMENTED
    val inScope = item.level.level <= targetLevel.level && item.step.matches(osScope)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val (icon, colour) = when (state) {
                    StepState.IMPLEMENTED -> Icons.Outlined.CheckCircle to Color(0xFF4CAF50)
                    StepState.NOT_APPLICABLE -> Icons.Outlined.Block to Color(0xFFFF9800)
                    StepState.NOT_IMPLEMENTED -> Icons.Outlined.RadioButtonUnchecked to MaterialTheme.colorScheme.onSurfaceVariant
                }
                Icon(icon, contentDescription = state.rawValue, tint = colour)
                Text(
                    text = item.control.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(100.dp),
                ) {
                    Text(
                        item.level.shortName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Text(item.step.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            if (!inScope) {
                Text(
                    text = "Not in your current scope",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item.mapping.note?.let { note ->
                Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
