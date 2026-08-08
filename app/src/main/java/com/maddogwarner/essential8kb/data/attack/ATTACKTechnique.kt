package com.maddogwarner.essential8kb.data.attack

/**
 * ATT&CK Enterprise tactics, in v19.1 matrix order. Only the tactics reachable
 * from this app's mappings are modelled; declaration order is the display order.
 */
enum class ATTACKTactic(val displayName: String) {
    INITIAL_ACCESS("Initial Access"),
    EXECUTION("Execution"),
    PERSISTENCE("Persistence"),
    PRIVILEGE_ESCALATION("Privilege Escalation"),
    STEALTH("Stealth"),
    DEFENSE_IMPAIRMENT("Defense Impairment"),
    CREDENTIAL_ACCESS("Credential Access"),
    LATERAL_MOVEMENT("Lateral Movement"),
    COLLECTION("Collection"),
    IMPACT("Impact"),
}

/** How an implementation step relates to an adversary technique. */
enum class ATTACKRelationship(val displayName: String) {
    PREVENT("Prevent"),
    DETECT("Detect"),
    RECOVER("Recover"),
    SUPPORT("Support");

    val countsTowardCoverage: Boolean get() = this != SUPPORT
}

data class ATTACKTechnique(
    val id: String,
    val name: String,
    val tactics: List<ATTACKTactic>,
    val url: String,
) {
    val isSubTechnique: Boolean get() = "." in id
    val parentID: String? get() = if (isSubTechnique) id.substringBefore('.') else null
}

data class ATTACKMapping(
    val stepID: String,
    val techniqueID: String,
    val relationships: List<ATTACKRelationship>,
    val note: String? = null,
) {
    val id: String get() = "$stepID|$techniqueID"
}
