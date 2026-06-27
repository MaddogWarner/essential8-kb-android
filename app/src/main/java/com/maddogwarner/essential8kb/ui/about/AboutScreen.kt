package com.maddogwarner.essential8kb.ui.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.maddogwarner.essential8kb.data.AppInformation
import com.maddogwarner.essential8kb.data.ReferenceLink
import com.maddogwarner.essential8kb.ui.components.SectionHeader

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val openLink: (ReferenceLink) -> Unit = { reference ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(reference.url))
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No web browser found to open this link.", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        item {
            SectionHeader("Purpose")
            CardBlock {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(AppInformation.aboutDescription)
                }
                Text(
                    text = AppInformation.contentScope,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionHeader(AppInformation.aboutMeTitle)
            CardBlock {
                Text(AppInformation.aboutMeDescription)
            }
        }
        items(AppInformation.authorLinks, key = { it.url }) { reference ->
            ReferenceRow(reference, Icons.Outlined.Person, openLink)
        }

        item {
            SectionHeader(AppInformation.privacyTitle)
            CardBlock {
                Text(AppInformation.privacyPolicy)
            }
        }
        item {
            ReferenceRow(AppInformation.privacyPolicyLink, Icons.Outlined.PrivacyTip, openLink)
        }

        item {
            SectionHeader("References")
        }
        items(AppInformation.referenceLinks, key = { it.url }) { reference ->
            ReferenceRow(reference, Icons.Outlined.Link, openLink)
        }
        item {
            Text(
                text = "External links open outside the app and should be used to verify current ASD and Microsoft guidance before implementation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReferenceRow(
    reference: ReferenceLink,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (ReferenceLink) -> Unit,
) {
    val url = reference.url
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(reference) },
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(reference.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = Uri.parse(url).host ?: url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
