package com.maddogwarner.essential8kb.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.ManageSearch
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.ui.graphics.vector.ImageVector

/*
 * SF Symbols to Material icons:
 * checkmark.shield -> Security; arrow.down.app -> SystemUpdate;
 * doc.text.magnifyingglass -> ManageSearch; lock.shield -> Lock;
 * person.badge.key -> ManageAccounts; gearshape.2 -> Settings;
 * key.horizontal -> Key; externaldrive.badge.checkmark -> Backup.
 */
fun materialIconForSfSymbol(systemName: String): ImageVector = when (systemName) {
    "checkmark.shield" -> Icons.Outlined.Security
    "arrow.down.app" -> Icons.Outlined.SystemUpdate
    "doc.text.magnifyingglass" -> Icons.Outlined.ManageSearch
    "lock.shield" -> Icons.Outlined.Lock
    "person.badge.key" -> Icons.Outlined.ManageAccounts
    "gearshape.2" -> Icons.Outlined.Settings
    "key.horizontal" -> Icons.Outlined.Key
    "externaldrive.badge.checkmark" -> Icons.Outlined.Backup
    else -> Icons.Outlined.Security
}

data class EssentialControl(
    val id: Int,
    val name: String,
    val icon: ImageVector,
    val overview: String,
    val ml0Description: String,
    val ml1: MaturityLevelContent,
    val ml2: MaturityLevelContent,
    val ml3: MaturityLevelContent,
) {
    fun content(forLevel: MaturityLevel): MaturityLevelContent = when (forLevel) {
        MaturityLevel.ML1 -> ml1
        MaturityLevel.ML2 -> ml2
        MaturityLevel.ML3 -> ml3
    }
}

enum class MaturityLevel(val level: Int) {
    ML1(1),
    ML2(2),
    ML3(3);

    val shortName: String get() = "ML$level"
    val displayName: String get() = "Maturity Level $level"
}

data class MaturityLevelContent(
    val summary: String,
    val steps: List<ImplementationStep>,
    val gapNote: String? = null,
)

data class ImplementationStep(
    val id: String,
    val title: String,
    val description: String,
    val technicalDetails: List<String>,
)

enum class AuditRecommendation(val label: String) {
    SUCCESS("Success"),
    FAILURE("Failure"),
    BOTH("Success & Failure"),
    NOT_RECOMMENDED("Not Recommended"),
}

data class AuditPolicyEntry(
    val id: String,
    val category: String,
    val name: String,
    val description: String,
    val recommendation: AuditRecommendation,
    val considerations: String,
    val domainControllerOnly: Boolean,
)

enum class Microsoft365BaseSelection {
    NONE,
    E3,
    E5,
}

enum class Microsoft365LicenseMode(
    val rawValue: String,
    val displayName: String,
    val shortName: String,
    val description: String,
    val baseSelection: Microsoft365BaseSelection,
) {
    NONE(
        rawValue = "none",
        displayName = "None",
        shortName = "None",
        description = "No Microsoft 365 or Defender additions are shown in the control pages.",
        baseSelection = Microsoft365BaseSelection.NONE,
    ),
    E3_P1(
        rawValue = "e3P1",
        displayName = "Microsoft 365 E3 + Entra ID P1",
        shortName = "E3 + P1",
        description = "Shows additional and partial protections commonly available with Microsoft 365 E3, including Entra ID P1, Intune Plan 1 and Defender for Endpoint Plan 1.",
        baseSelection = Microsoft365BaseSelection.E3,
    ),
    E3_P2(
        rawValue = "e3P2",
        displayName = "Microsoft 365 E3 + Entra ID P2",
        shortName = "E3 + P2",
        description = "Shows Microsoft 365 E3 protections plus Entra ID P2 identity protections such as risk-based Conditional Access and Privileged Identity Management.",
        baseSelection = Microsoft365BaseSelection.E3,
    ),
    E5(
        rawValue = "e5",
        displayName = "Microsoft 365 E5",
        shortName = "E5",
        description = "Shows the Microsoft 365 E5 security stack, including Entra ID P2, Defender for Endpoint Plan 2, Defender for Office 365 Plan 2, Defender for Identity and Defender for Cloud Apps.",
        baseSelection = Microsoft365BaseSelection.E5,
    );

    companion object {
        fun fromRawValue(rawValue: String?): Microsoft365LicenseMode =
            entries.firstOrNull { it.rawValue == rawValue } ?: NONE
    }
}

data class Microsoft365AdditionalProtection(
    val title: String,
    val coverage: String,
    val basicSettings: List<String>,
)

data class ReferenceLink(
    val title: String,
    val url: String,
) {
    val id: String get() = url
    val hostDisplayName: String get() = java.net.URI(url).host ?: url

    init {
        require(title.isNotBlank()) { "Reference title must not be blank." }
        require(url.startsWith("https://")) { "Reference URL must use HTTPS." }
    }
}
