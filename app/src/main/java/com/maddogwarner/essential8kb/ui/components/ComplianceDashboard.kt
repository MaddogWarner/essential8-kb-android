package com.maddogwarner.essential8kb.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maddogwarner.essential8kb.data.EssentialControl
import com.maddogwarner.essential8kb.data.MaturityLevel
import com.maddogwarner.essential8kb.data.OSScope
import com.maddogwarner.essential8kb.store.StepState
import com.maddogwarner.essential8kb.store.StepStatus
import kotlin.math.roundToInt

@Composable
fun ComplianceRing(
    percentage: Double,
    modifier: Modifier = Modifier,
) {
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = Spring.StiffnessLow
        ),
        label = "ComplianceRingAnimation"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(80.dp)
    ) {
        val strokeWidth = 10.dp
        val strokeWidthPx = with(LocalDensity.current) { strokeWidth.toPx() }
        val backgroundStrokeColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
        val gradient = Brush.linearGradient(
            colors = listOf(
                Color(0xFF2196F3), // Blue
                Color(0xFF4CAF50), // Green
                Color(0xFF00BCD4)  // Teal
            )
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw background circle
            drawCircle(
                color = backgroundStrokeColor,
                style = Stroke(width = strokeWidthPx)
            )
            // Draw progress arc
            drawArc(
                brush = gradient,
                startAngle = -90f,
                sweepAngle = (animatedPercentage / 100f) * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${percentage.roundToInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Compliant",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun ComplianceBarChart(
    controls: List<EssentialControl>,
    statuses: Map<String, StepStatus>,
    targetLevel: MaturityLevel,
    osScope: OSScope,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        controls.forEach { control ->
            val allSteps = control.steps(targetLevel, osScope)
            val totalSteps = allSteps.size
            if (totalSteps > 0) {
                val implemented = allSteps.count { statuses[it.id]?.state == StepState.IMPLEMENTED }
                val na = allSteps.count { statuses[it.id]?.state == StepState.NOT_APPLICABLE }
                val pending = totalSteps - implemented - na

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = control.shortName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(36.dp),
                    )

                    // Stacked bar container
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        if (implemented > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(implemented.toFloat())
                                    .fillMaxHeight()
                                    .background(Color(0xFF4CAF50)) // Green
                            )
                        }
                        if (na > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(na.toFloat())
                                    .fillMaxHeight()
                                    .background(Color(0xFFFF9800)) // Orange
                            )
                        }
                        if (pending > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(pending.toFloat())
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.outlineVariant) // Gray/Pending
                            )
                        }
                    }
                }
            }
        }
    }
}
