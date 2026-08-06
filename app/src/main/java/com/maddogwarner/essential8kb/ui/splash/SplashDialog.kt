package com.maddogwarner.essential8kb.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.maddogwarner.essential8kb.data.AppInformation

@Composable
fun SplashDialog(
    showSplashOnStartup: Boolean,
    onShowSplashOnStartupChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    // Header Icon & Title
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            modifier = Modifier
                                .size(64.dp)
                                .graphicsLayer(alpha = 0.99f)
                                .drawWithCache {
                                    val brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFF00BCD4))
                                    )
                                    onDrawWithContent {
                                        drawContent()
                                        drawRect(brush, blendMode = BlendMode.SrcAtop)
                                    }
                                }
                        )

                        Text(
                            text = "Essential 8",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Knowledge Base",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // App Overview Section
                    Text(
                        text = AppInformation.aboutDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // What's New Section Title
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            text = "Welcome to Version 1.0",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // Feature list
                        Column(
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            modifier = Modifier.padding(bottom = 24.dp)
                        ) {
                            FeatureRow(
                                icon = Icons.Outlined.TrackChanges,
                                color = Color(0xFF3F51B5), // Indigo
                                title = "Target Maturity Level",
                                description = "Set your organisation's target (ML1–ML3) and measure dashboard compliance against it instead of everything."
                            )

                            FeatureRow(
                                icon = Icons.Outlined.Tag,
                                color = Color(0xFF009688), // Teal
                                title = "ISM Control Mapping",
                                description = "Verified ISM control identifiers are visible on mapped implementation steps and searchable in Global Search."
                            )

                            FeatureRow(
                                icon = Icons.Outlined.BarChart,
                                color = Color(0xFF2196F3), // Blue
                                title = "Compliance Dashboard",
                                description = "Track maturity level progress on the home screen with animated charts and compliance rings."
                            )

                            FeatureRow(
                                icon = Icons.Outlined.Search,
                                color = Color(0xFF9C27B0), // Purple
                                title = "Global Search",
                                description = "Find specific Group Policies, registry keys, and commands instantly across all controls."
                            )

                            FeatureRow(
                                icon = Icons.Outlined.VisibilityOff,
                                color = Color(0xFFF44336), // Red
                                title = "Reference Only Mode",
                                description = "Optionally hide the home screen compliance dashboard to use the app purely as a technical reference guide."
                            )
                        }
                    }
                }

                // Sticky Bottom Actions
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(bottom = 24.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Checkbox always show on startup
                    Row(
                        modifier = Modifier
                            .clickable { onShowSplashOnStartupChanged(!showSplashOnStartup) }
                            .padding(8.dp)
                            .semantics {
                                contentDescription = "Always show on startup: ${if (showSplashOnStartup) "Checked" else "Unchecked"}"
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (showSplashOnStartup) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                            contentDescription = null,
                            tint = if (showSplashOnStartup) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Always show on startup",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Get Started Button with gradient fill
                    val gradient = Brush.linearGradient(
                        colors = listOf(Color(0xFF2196F3), Color(0xFF4CAF50))
                    )
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(gradient),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text(
                            text = "Get Started",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    title: String,
    description: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "$title: $description"
            }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(28.dp)
                .width(32.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
