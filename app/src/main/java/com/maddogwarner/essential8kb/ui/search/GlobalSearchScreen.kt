package com.maddogwarner.essential8kb.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maddogwarner.essential8kb.data.EssentialControl
import com.maddogwarner.essential8kb.data.EssentialControlsData
import com.maddogwarner.essential8kb.data.ImplementationStep
import com.maddogwarner.essential8kb.data.MaturityLevel
import com.maddogwarner.essential8kb.data.MaturityLevelContent
import com.maddogwarner.essential8kb.data.attack.ATTACKCatalogue
import com.maddogwarner.essential8kb.data.attack.ATTACKMappingData
import com.maddogwarner.essential8kb.ui.components.SectionHeader

data class SearchResult(
    val control: EssentialControl,
    val level: MaturityLevel,
    val content: MaturityLevelContent,
    val step: ImplementationStep,
    val stepIndex: Int,
    val matchedDetails: List<String>,
)

fun ImplementationStep.matchesSearchQuery(query: String): Boolean {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return false
    return title.contains(trimmed, ignoreCase = true) ||
            description.contains(trimmed, ignoreCase = true) ||
            technicalDetails.any { it.contains(trimmed, ignoreCase = true) } ||
            ismControls.any { it.contains(trimmed, ignoreCase = true) } ||
            ATTACKMappingData.mappings(id).any { mapping ->
                val technique = ATTACKCatalogue.technique(mapping.techniqueID) ?: return@any false
                technique.id.contains(trimmed, ignoreCase = true) ||
                        technique.parentID?.contains(trimmed, ignoreCase = true) == true ||
                        technique.name.contains(trimmed, ignoreCase = true) ||
                        mapping.note?.contains(trimmed, ignoreCase = true) == true
            }
}

fun ImplementationStep.matchingDetails(query: String): List<String> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return emptyList()
    val matchedTech = technicalDetails.filter { it.contains(trimmed, ignoreCase = true) }
    val matchedIsm = ismControls.filter { it.contains(trimmed, ignoreCase = true) }
    val matchedTechniques = ATTACKMappingData.mappings(id).mapNotNull { mapping ->
        val technique = ATTACKCatalogue.technique(mapping.techniqueID) ?: return@mapNotNull null
        val matches = technique.id.contains(trimmed, ignoreCase = true) ||
                technique.parentID?.contains(trimmed, ignoreCase = true) == true ||
                technique.name.contains(trimmed, ignoreCase = true) ||
                mapping.note?.contains(trimmed, ignoreCase = true) == true
        if (!matches) null else {
            val relationships = mapping.relationships.joinToString { it.displayName }
            "${technique.id} — ${technique.name} ($relationships)"
        }
    }
    return matchedTech + matchedIsm + matchedTechniques
}

@Composable
fun GlobalSearchScreen(
    onStepSelected: (EssentialControl, MaturityLevel, MaturityLevelContent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchText by remember { mutableStateOf("") }
    val trimmedQuery = searchText.trim()

    val searchResults = remember(trimmedQuery) {
        if (trimmedQuery.isEmpty()) {
            emptyList()
        } else {
            val list = mutableListOf<SearchResult>()
            EssentialControlsData.all.forEach { control ->
                MaturityLevel.entries.forEach { level ->
                    val content = control.content(level)
                    content.steps.forEachIndexed { index, step ->
                        if (step.matchesSearchQuery(trimmedQuery)) {
                            list.add(
                                SearchResult(
                                    control = control,
                                    level = level,
                                    content = content,
                                    step = step,
                                    stepIndex = index,
                                    matchedDetails = step.matchingDetails(trimmedQuery)
                                )
                            )
                        }
                    }
                }
            }
            list
        }
    }

    val groupedResults = remember(searchResults) {
        searchResults.groupBy { it.control.id }
    }

    val groupedList = remember(groupedResults) {
        EssentialControlsData.all.mapNotNull { control ->
            val results = groupedResults[control.id]
            if (results.isNullOrEmpty()) null else Pair(control, results)
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = { Text("Search GPOs, registries, commands, ISM or ATT&CK IDs…") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        if (trimmedQuery.isEmpty()) {
            EmptySearchState(
                icon = Icons.Outlined.Search,
                title = "Search technical details",
                description = "Search registry paths, GPO settings, commands, ISM control numbers, ATT&CK techniques, or controls across the entire knowledge base."
            )
        } else if (searchResults.isEmpty()) {
            EmptySearchState(
                icon = Icons.Outlined.SearchOff,
                title = "No results found",
                description = "Try searching for terms like 'HKLM', 'AppLocker', 'ISM-1490', 'T1059', 'sc config', or 'block'."
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                groupedList.forEach { (control, results) ->
                    item {
                        SectionHeader(text = control.name)
                    }
                    items(results, key = { it.step.id }) { result ->
                        SearchResultCard(
                            result = result,
                            onClick = { onStepSelected(result.control, result.level, result.content) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    result: SearchResult,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Step ${result.stepIndex + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = result.level.shortName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Text(
                text = result.step.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = result.step.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )

            if (result.matchedDetails.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "Matching Detail:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                    )
                    SelectionContainer {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            result.matchedDetails.forEach { detail ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFFEB3B).copy(alpha = 0.15f))
                                        .border(1.dp, Color(0xFFFFEB3B).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = detail,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySearchState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
