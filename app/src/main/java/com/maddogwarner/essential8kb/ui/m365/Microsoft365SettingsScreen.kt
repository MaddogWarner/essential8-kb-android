package com.maddogwarner.essential8kb.ui.m365

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maddogwarner.essential8kb.data.Microsoft365LicenseMode
import com.maddogwarner.essential8kb.ui.components.SectionHeader

@Composable
fun Microsoft365SettingsScreen(
    selectedMode: Microsoft365LicenseMode,
    onModeSelected: (Microsoft365LicenseMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        item {
            SectionHeader("Purpose")
            Text(
                text = "Different Microsoft 365 licensing levels provide different identity, endpoint, email and cloud-app protections. These settings add separate M365/MDE sections to the Essential Eight control pages so the core Windows guidance stays distinct from licensed cloud protections.",
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        item {
            SectionHeader("License Mode")
        }
        item {
            RadioCard(
                title = "None",
                subtitle = Microsoft365LicenseMode.NONE.description,
                selected = selectedMode == Microsoft365LicenseMode.NONE,
                onClick = { onModeSelected(Microsoft365LicenseMode.NONE) },
            )
        }
        item {
            RadioCard(
                title = "E3",
                subtitle = "Microsoft 365 E3 baseline. Choose the Entra ID plan available to the organisation.",
                selected = selectedMode == Microsoft365LicenseMode.E3_P1 || selectedMode == Microsoft365LicenseMode.E3_P2,
                onClick = { onModeSelected(Microsoft365LicenseMode.E3_P1) },
            )
        }
        if (selectedMode == Microsoft365LicenseMode.E3_P1 || selectedMode == Microsoft365LicenseMode.E3_P2) {
            item {
                Column(
                    modifier = Modifier.padding(start = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RadioCard(
                        title = "P1",
                        subtitle = "Entra ID P1 with Conditional Access and MFA policy controls.",
                        selected = selectedMode == Microsoft365LicenseMode.E3_P1,
                        onClick = { onModeSelected(Microsoft365LicenseMode.E3_P1) },
                    )
                    RadioCard(
                        title = "P2",
                        subtitle = "Adds Entra ID P2 identity risk and Privileged Identity Management capabilities.",
                        selected = selectedMode == Microsoft365LicenseMode.E3_P2,
                        onClick = { onModeSelected(Microsoft365LicenseMode.E3_P2) },
                    )
                }
            }
        }
        item {
            RadioCard(
                title = "E5",
                subtitle = Microsoft365LicenseMode.E5.description,
                selected = selectedMode == Microsoft365LicenseMode.E5,
                onClick = { onModeSelected(Microsoft365LicenseMode.E5) },
            )
        }
        item {
            Text(
                text = "Default is None. The selection is stored locally on this device and does not leave the app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (selectedMode != Microsoft365LicenseMode.NONE) {
            item {
                SectionHeader("Active Additions")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("Current mode: ${selectedMode.shortName}", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = selectedMode.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RadioCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { this.selected = selected }
            .clickable(role = Role.RadioButton, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = if (selected) Icons.Outlined.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
