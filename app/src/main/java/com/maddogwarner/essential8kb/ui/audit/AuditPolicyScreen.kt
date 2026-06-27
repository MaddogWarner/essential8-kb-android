package com.maddogwarner.essential8kb.ui.audit

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
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maddogwarner.essential8kb.data.AuditPolicyEntry
import com.maddogwarner.essential8kb.data.WindowsAuditPolicyData
import com.maddogwarner.essential8kb.ui.components.RecommendationBadge
import com.maddogwarner.essential8kb.ui.components.SectionHeader

@Composable
fun AuditPolicyScreen(
    modifier: Modifier = Modifier,
) {
    val entriesByCategory = remember { WindowsAuditPolicyData.entries.groupBy { it.category } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        item {
            CardBlock {
                Text("ASD Recommended Minimum Audit Policy", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = WindowsAuditPolicyData.overview,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        entriesByCategory.forEach { (category, entries) ->
            item { SectionHeader(category) }
            items(entries, key = { it.id }) { entry ->
                AuditEntryCard(entry)
            }
        }
    }
}

@Composable
private fun AuditEntryCard(entry: AuditPolicyEntry) {
    CardBlock {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(entry.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = entry.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RecommendationBadge(entry.recommendation)
        }

        if (entry.domainControllerOnly) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Dns,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Domain controllers only",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = entry.considerations,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
