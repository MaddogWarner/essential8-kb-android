package com.maddogwarner.essential8kb.data.attack

// MITRE ATT&CK® Enterprise v19.1 (released 28/04/2026).
// Technique IDs, names and tactics validated against the machine-readable
// ATT&CK STIX bundle (mitre-attack/attack-stix-data, collection 19.1).
// Re-verify on every ATT&CK major version bump (roughly April and October).
// ATT&CK® is a registered trademark of The MITRE Corporation.

object ATTACKCatalogue {
    const val attackVersion = "v19.1"
    const val attackVersionReleased = "28 April 2026"
    const val attackCopyrightYear = "2026"

    val all: List<ATTACKTechnique> = listOf(
        entry("T1003.001", "LSASS Memory", listOf(ATTACKTactic.CREDENTIAL_ACCESS), "https://attack.mitre.org/techniques/T1003/001/"),
        entry("T1021", "Remote Services", listOf(ATTACKTactic.LATERAL_MOVEMENT), "https://attack.mitre.org/techniques/T1021/"),
        entry("T1055", "Process Injection", listOf(ATTACKTactic.STEALTH, ATTACKTactic.PRIVILEGE_ESCALATION), "https://attack.mitre.org/techniques/T1055/"),
        entry("T1059", "Command and Scripting Interpreter", listOf(ATTACKTactic.EXECUTION), "https://attack.mitre.org/techniques/T1059/"),
        entry("T1059.001", "PowerShell", listOf(ATTACKTactic.EXECUTION), "https://attack.mitre.org/techniques/T1059/001/"),
        entry("T1059.005", "Visual Basic", listOf(ATTACKTactic.EXECUTION), "https://attack.mitre.org/techniques/T1059/005/"),
        entry("T1068", "Exploitation for Privilege Escalation", listOf(ATTACKTactic.PRIVILEGE_ESCALATION), "https://attack.mitre.org/techniques/T1068/"),
        entry("T1078", "Valid Accounts", listOf(ATTACKTactic.STEALTH, ATTACKTactic.PERSISTENCE, ATTACKTactic.PRIVILEGE_ESCALATION, ATTACKTactic.INITIAL_ACCESS), "https://attack.mitre.org/techniques/T1078/"),
        entry("T1078.003", "Local Accounts", listOf(ATTACKTactic.STEALTH, ATTACKTactic.PERSISTENCE, ATTACKTactic.PRIVILEGE_ESCALATION, ATTACKTactic.INITIAL_ACCESS), "https://attack.mitre.org/techniques/T1078/003/"),
        entry("T1110", "Brute Force", listOf(ATTACKTactic.CREDENTIAL_ACCESS), "https://attack.mitre.org/techniques/T1110/"),
        entry("T1110.001", "Password Guessing", listOf(ATTACKTactic.CREDENTIAL_ACCESS), "https://attack.mitre.org/techniques/T1110/001/"),
        entry("T1111", "Multi-Factor Authentication Interception", listOf(ATTACKTactic.CREDENTIAL_ACCESS), "https://attack.mitre.org/techniques/T1111/"),
        entry("T1127", "Trusted Developer Utilities Proxy Execution", listOf(ATTACKTactic.STEALTH, ATTACKTactic.EXECUTION), "https://attack.mitre.org/techniques/T1127/"),
        entry("T1137.001", "Office Template Macros", listOf(ATTACKTactic.PERSISTENCE), "https://attack.mitre.org/techniques/T1137/001/"),
        entry("T1189", "Drive-by Compromise", listOf(ATTACKTactic.INITIAL_ACCESS), "https://attack.mitre.org/techniques/T1189/"),
        entry("T1190", "Exploit Public-Facing Application", listOf(ATTACKTactic.INITIAL_ACCESS), "https://attack.mitre.org/techniques/T1190/"),
        entry("T1203", "Exploitation for Client Execution", listOf(ATTACKTactic.EXECUTION), "https://attack.mitre.org/techniques/T1203/"),
        entry("T1204.002", "Malicious File", listOf(ATTACKTactic.EXECUTION), "https://attack.mitre.org/techniques/T1204/002/"),
        entry("T1216", "System Script Proxy Execution", listOf(ATTACKTactic.STEALTH), "https://attack.mitre.org/techniques/T1216/"),
        entry("T1218", "System Binary Proxy Execution", listOf(ATTACKTactic.STEALTH), "https://attack.mitre.org/techniques/T1218/"),
        entry("T1485", "Data Destruction", listOf(ATTACKTactic.IMPACT), "https://attack.mitre.org/techniques/T1485/"),
        entry("T1486", "Data Encrypted for Impact", listOf(ATTACKTactic.IMPACT), "https://attack.mitre.org/techniques/T1486/"),
        entry("T1490", "Inhibit System Recovery", listOf(ATTACKTactic.IMPACT), "https://attack.mitre.org/techniques/T1490/"),
        entry("T1543.003", "Windows Service", listOf(ATTACKTactic.PERSISTENCE, ATTACKTactic.PRIVILEGE_ESCALATION), "https://attack.mitre.org/techniques/T1543/003/"),
        entry("T1548.002", "Bypass User Account Control", listOf(ATTACKTactic.PRIVILEGE_ESCALATION), "https://attack.mitre.org/techniques/T1548/002/"),
        entry("T1550.002", "Pass the Hash", listOf(ATTACKTactic.LATERAL_MOVEMENT), "https://attack.mitre.org/techniques/T1550/002/"),
        entry("T1550.003", "Pass the Ticket", listOf(ATTACKTactic.LATERAL_MOVEMENT), "https://attack.mitre.org/techniques/T1550/003/"),
        entry("T1553.002", "Code Signing", listOf(ATTACKTactic.DEFENSE_IMPAIRMENT), "https://attack.mitre.org/techniques/T1553/002/"),
        entry("T1556", "Modify Authentication Process", listOf(ATTACKTactic.DEFENSE_IMPAIRMENT, ATTACKTactic.PERSISTENCE, ATTACKTactic.CREDENTIAL_ACCESS), "https://attack.mitre.org/techniques/T1556/"),
        entry("T1557", "Adversary-in-the-Middle", listOf(ATTACKTactic.CREDENTIAL_ACCESS, ATTACKTactic.COLLECTION), "https://attack.mitre.org/techniques/T1557/"),
        entry("T1566", "Phishing", listOf(ATTACKTactic.INITIAL_ACCESS), "https://attack.mitre.org/techniques/T1566/"),
        entry("T1566.001", "Spearphishing Attachment", listOf(ATTACKTactic.INITIAL_ACCESS), "https://attack.mitre.org/techniques/T1566/001/"),
        entry("T1566.002", "Spearphishing Link", listOf(ATTACKTactic.INITIAL_ACCESS), "https://attack.mitre.org/techniques/T1566/002/"),
        entry("T1621", "Multi-Factor Authentication Request Generation", listOf(ATTACKTactic.CREDENTIAL_ACCESS), "https://attack.mitre.org/techniques/T1621/"),
        entry("T1685", "Disable or Modify Tools", listOf(ATTACKTactic.DEFENSE_IMPAIRMENT), "https://attack.mitre.org/techniques/T1685/"),
    )

    private val byID: Map<String, ATTACKTechnique> = all.associateBy(ATTACKTechnique::id)

    fun technique(id: String): ATTACKTechnique? = byID[id]

    private fun entry(
        id: String,
        name: String,
        tactics: List<ATTACKTactic>,
        url: String,
    ): ATTACKTechnique = ATTACKTechnique(id, name, tactics, url)
}
