package com.maddogwarner.essential8kb.ui.home

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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ManageSearch
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maddogwarner.essential8kb.data.EssentialControl
import com.maddogwarner.essential8kb.data.allStepIds
import com.maddogwarner.essential8kb.ui.components.SectionHeader

@Composable
fun HomeScreen(
    controls: List<EssentialControl>,
    completedStepIds: Set<String>,
    onControlSelected: (EssentialControl) -> Unit,
    onAuditPolicySelected: () -> Unit,
    onMicrosoft365Selected: () -> Unit,
    onAboutSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        item {
            SectionHeader("ASD Essential Eight")
        }
        items(controls, key = { it.id }) { control ->
            val isComplete = control.allStepIds().let { ids ->
                ids.isNotEmpty() && ids.all { it in completedStepIds }
            }
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
                        tint = MaterialTheme.colorScheme.primary,
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
