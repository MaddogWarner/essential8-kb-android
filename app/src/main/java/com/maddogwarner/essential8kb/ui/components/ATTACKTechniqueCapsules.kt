package com.maddogwarner.essential8kb.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maddogwarner.essential8kb.data.attack.ATTACKCatalogue
import com.maddogwarner.essential8kb.data.attack.ATTACKMapping
import com.maddogwarner.essential8kb.data.attack.ATTACKTechnique

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ATTACKTechniqueCapsules(
    mappings: List<ATTACKMapping>,
    onTechniqueSelected: (ATTACKTechnique) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ids = mappings.mapTo(mutableSetOf()) { it.techniqueID }
    val techniques = ATTACKCatalogue.all.filter { it.id in ids }
    if (techniques.isEmpty()) return

    FlowRow(
        modifier = modifier.semantics {
            contentDescription = "ATT&CK techniques: " +
                    techniques.joinToString { "${it.id} ${it.name}" }
        },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        techniques.forEach { technique ->
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                shape = RoundedCornerShape(100.dp),
                modifier = Modifier.clickable { onTechniqueSelected(technique) },
            ) {
                Text(
                    text = technique.id,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}
