package com.maddogwarner.essential8kb.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maddogwarner.essential8kb.data.AuditRecommendation

@Composable
fun RecommendationBadge(
    recommendation: AuditRecommendation,
    modifier: Modifier = Modifier,
) {
    val colourScheme = MaterialTheme.colorScheme
    val (containerColour, contentColour) = when (recommendation) {
        AuditRecommendation.BOTH -> colourScheme.primaryContainer to colourScheme.onPrimaryContainer
        AuditRecommendation.SUCCESS -> colourScheme.tertiaryContainer to colourScheme.onTertiaryContainer
        AuditRecommendation.FAILURE -> colourScheme.errorContainer to colourScheme.onErrorContainer
        AuditRecommendation.NOT_RECOMMENDED -> colourScheme.surfaceVariant to colourScheme.onSurfaceVariant
    }

    Text(
        text = recommendation.label,
        style = MaterialTheme.typography.labelSmall,
        color = contentColour,
        modifier = modifier
            .background(
                color = containerColour,
                shape = MaterialTheme.shapes.small,
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
