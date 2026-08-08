package com.maddogwarner.essential8kb.data.attack

import com.maddogwarner.essential8kb.data.attack.ATTACKRelationship.DETECT
import com.maddogwarner.essential8kb.data.attack.ATTACKRelationship.PREVENT
import com.maddogwarner.essential8kb.data.attack.ATTACKRelationship.RECOVER
import com.maddogwarner.essential8kb.data.attack.ATTACKRelationship.SUPPORT

object ATTACKMappingData {
    /** One entry per (step, technique) pair. Source: mitreattacke8.md (26/07/2026). */
    val all: List<ATTACKMapping> = buildList {
        // 1. Application Control
        mappings("1-1-0", listOf("T1204.002", "T1059"), listOf(SUPPORT), "AppLocker enforcement depends on this service. Alone it blocks nothing.")
        mappings("1-1-1", listOf("T1204.002", "T1218"), listOf(PREVENT))
        mappings("1-1-2", listOf("T1204.002", "T1059.001"), listOf(PREVENT), "Removes the drop-and-run paths most commodity loaders rely on.")
        mappings("1-1-3", listOf("T1204.002", "T1059", "T1218"), listOf(DETECT))
        mapping("1-2-0", "T1059", listOf(PREVENT))
        mapping("1-2-0", "T1021", listOf(SUPPORT), "Constrains what an adversary can run after arriving over a remote service; does not block the remote service itself.")
        mappings("1-2-1", listOf("T1218", "T1685"), listOf(PREVENT), "Code integrity blocks the unsigned tooling commonly used to disable security products.")
        mappings("1-2-2", listOf("T1204.002", "T1059", "T1218"), listOf(DETECT))
        mappings("1-3-0", listOf("T1218", "T1127", "T1216"), listOf(PREVENT), "The block list exists specifically to close signed-binary, developer-utility and script-host proxy execution.")
        mapping("1-3-1", "T1068", listOf(PREVENT), "Closes the bring-your-own-vulnerable-driver path.")
        mapping("1-3-1", "T1543.003", listOf(PREVENT), "A vulnerable driver is loaded as a kernel service; blocking the driver blocks that service creation.")
        mappings("1-3-2", listOf("T1068", "T1685"), listOf(PREVENT), "HVCI blocks unsigned kernel code, the usual route to tampering with security tooling from the kernel.")

        // 2. Patch Applications
        mapping("2-1-0", "T1203", listOf(PREVENT))
        mappings("2-1-1", listOf("T1189", "T1203"), listOf(PREVENT))
        // 2-1-2 deliberately unmapped: defender asset inventory is not T1518.
        mapping("2-1-3", "T1203", listOf(PREVENT))
        mapping("2-1-3", "T1190", listOf(PREVENT), "Applies where the removed application was internet-facing.")
        mappings("2-2-0", listOf("T1203", "T1189"), listOf(PREVENT))
        mapping("2-2-1", "T1203", listOf(SUPPORT), "Patches only take effect after restart. The deadline closes the exposure window rather than blocking the technique.")
        mappings("2-3-0", listOf("T1203", "T1190"), listOf(PREVENT))

        // 3. Configure Microsoft Office Macro Settings
        mappings("3-1-0", listOf("T1566.001", "T1204.002"), listOf(PREVENT), "Depends on Mark-of-the-Web. Adversaries defeat it with container formats that do not propagate the mark — ISO, IMG, VHD, 7z — which ATT&CK tracks as T1553.005 Mark-of-the-Web Bypass.")
        mapping("3-1-1", "T1059.005", listOf(PREVENT))
        mappings("3-1-2", listOf("T1566.001", "T1059.005"), listOf(PREVENT), "Stops the user being able to click past the block.")
        mapping("3-2-0", "T1059.005", listOf(PREVENT))
        mapping("3-2-0", "T1553.002", listOf(PREVENT), "Only as strong as publisher scoping — a stolen or abused signing certificate defeats it.")
        mapping("3-2-1", "T1059.005", listOf(PREVENT, DETECT), "Inspects macro content at runtime, so obfuscated payloads are caught after de-obfuscation.")
        mapping("3-2-2", "T1059.005", listOf(DETECT))
        mappings("3-3-0", listOf("T1059.005", "T1553.002"), listOf(PREVENT), "Stronger signature validation closes downgrade and tampering paths in legacy signature formats.")
        mapping("3-3-1", "T1059.005", listOf(PREVENT))
        mapping("3-3-1", "T1137.001", listOf(PREVENT), "A writable Trusted Location or template path is an Office persistence route, not just an execution one.")

        // 4. User Application Hardening
        mappings("4-1-0", listOf("T1189", "T1203"), listOf(PREVENT), "Removes a legacy engine that no longer receives fixes.")
        mapping("4-1-1", "T1189", listOf(PREVENT))
        mapping("4-1-2", "T1189", listOf(PREVENT), "Targets malvertising. Edge Tracking Prevention alone is partial coverage — see the ML1 gap note in the app.")
        mapping("4-2-0", "T1059.001", listOf(DETECT))
        mappings("4-2-1", listOf("T1059", "T1204.002", "T1218", "T1055"), listOf(PREVENT), "Coverage depends on which ASR rules are enabled and whether they are in Block or Audit mode.")
        mappings("4-2-2", listOf("T1059", "T1059.001"), listOf(DETECT))
        mapping("4-3-0", "T1059.001", listOf(PREVENT), "PowerShell v2 predates ScriptBlock logging and AMSI; removing it closes the downgrade path used to evade both.")
        mapping("4-3-1", "T1059.001", listOf(PREVENT))
        mapping("4-3-2", "T1203", listOf(PREVENT))
        mapping("4-3-2", "T1127", listOf(PREVENT), "Legacy .NET ships the signed developer utilities (MSBuild, InstallUtil) used for proxy execution.")

        // 5. Restrict Administrative Privileges
        mappings("5-1-0", listOf("T1078.003", "T1548.002"), listOf(PREVENT))
        mapping("5-1-1", "T1078.003", listOf(PREVENT))
        mapping("5-1-1", "T1550.002", listOf(PREVENT), "Unique per-machine passwords stop a recovered local admin credential or hash working across the fleet. LAPS does not prevent the credential being dumped in the first place.")
        mappings("5-1-2", listOf("T1566", "T1078"), listOf(PREVENT), "Breaks the direct path from a phishing click to a privileged session.")
        mapping("5-2-0", "T1003.001", listOf(PREVENT))
        mapping("5-2-0", "T1550.002", listOf(PREVENT), "Protects the derived material an attacker would otherwise replay.")
        mapping("5-2-1", "T1003.001", listOf(PREVENT), "Weaker than Credential Guard; bypassable with a signed vulnerable driver, which is why 1-3-1 matters alongside it.")
        mappings("5-2-2", listOf("T1059.001", "T1078"), listOf(PREVENT), "Constrains what a compromised privileged session can actually do.")
        mappings("5-3-0", listOf("T1550.002", "T1550.003"), listOf(PREVENT), "Blocks NTLM, DES and RC4 for members and disallows delegation. Verify application compatibility first.")
        mappings("5-3-1", listOf("T1078", "T1021"), listOf(PREVENT), "Separates the administration plane from the browsing and email plane.")
        mappings("5-3-2", listOf("T1078", "T1078.003"), listOf(DETECT))

        // 6. Patch Operating Systems
        mappings("6-1-0", listOf("T1203", "T1068"), listOf(PREVENT))
        mappings("6-1-1", listOf("T1203", "T1068"), listOf(SUPPORT), "Bounds the exposure window; the patch itself does the preventing.")
        // 6-1-2 deliberately unmapped: defender asset inventory is not T1082.
        mappings("6-2-0", listOf("T1203", "T1068"), listOf(PREVENT))
        mapping("6-2-1", "T1068", listOf(PREVENT), "Driver flaws are a primary kernel privilege-escalation route.")
        mappings("6-3-0", listOf("T1203", "T1068"), listOf(PREVENT))
        mapping("6-3-0", "T1190", listOf(PREVENT), "Applies to internet-facing systems.")
        mappings("6-3-1", listOf("T1068", "T1203"), listOf(PREVENT), "Out-of-support builds stop receiving fixes entirely, so every other patching step degrades to nothing.")

        // 7. Multi-factor Authentication
        mappings("7-1-0", listOf("T1078", "T1110"), listOf(PREVENT))
        mapping("7-1-0", "T1111", listOf(PREVENT), "Phishing-resistant: the credential is bound to the device and cannot be replayed elsewhere.")
        mapping("7-1-1", "T1110.001", listOf(PREVENT), "The PIN is device-local and TPM-protected, so this resists local guessing rather than remote spraying.")
        mappings("7-1-2", listOf("T1078", "T1110"), listOf(PREVENT))
        mappings("7-2-0", listOf("T1078", "T1110"), listOf(PREVENT), "MFA that relies on push approval introduces its own attack surface — T1621 MFA Request Generation, or push fatigue. Prefer number matching or a phishing-resistant method.")
        mapping("7-2-1", "T1550.002", listOf(PREVENT), "Randomises the account's NT hash when set, but does not rotate it afterwards. A hash captured later stays valid until rotated, so rotate on a schedule.")
        mapping("7-2-1", "T1550.003", listOf(SUPPORT))
        mappings("7-3-0", listOf("T1078", "T1111", "T1556"), listOf(PREVENT))
        mappings("7-3-1", listOf("T1111", "T1557"), listOf(PREVENT), "Origin-bound credentials mean an adversary-in-the-middle proxy cannot relay the authentication.")
        mapping("7-3-1", "T1566.002", listOf(SUPPORT), "Neutralises credential theft via a phishing link. It does not stop the link being sent or clicked.")
        mappings("7-3-2", listOf("T1078", "T1110", "T1621"), listOf(DETECT), "Repeated denied MFA prompts are the signature of a push-fatigue attempt.")

        // 8. Regular Backups
        mappings("8-1-0", listOf("T1490", "T1486"), listOf(SUPPORT), "Prerequisite only. The capability helps once backups are scheduled (8-1-1) and protected (8-1-2).")
        mappings("8-1-1", listOf("T1486", "T1490", "T1485"), listOf(RECOVER))
        mappings("8-1-2", listOf("T1485", "T1490"), listOf(PREVENT), "A backup an adversary can reach with the credentials they already hold is not a backup.")
        mapping("8-1-3", "T1490", listOf(SUPPORT), "VSS is the first thing ransomware deletes — vssadmin delete shadows is squarely T1490. Treat shadow copies as user convenience, never as the backup.")
        mappings("8-2-0", listOf("T1078", "T1490"), listOf(PREVENT))
        mappings("8-2-1", listOf("T1485", "T1486"), listOf(PREVENT))
        mappings("8-3-0", listOf("T1486", "T1485"), listOf(DETECT), "Detects silent corruption or encryption of backup data. It does not reverse it.")
        mappings("8-3-1", listOf("T1485", "T1490", "T1078"), listOf(PREVENT))
        mappings("8-3-2", listOf("T1486", "T1490", "T1485"), listOf(RECOVER), "The only copy that reliably survives an adversary holding domain administrator.")
    }

    private val byStepID: Map<String, List<ATTACKMapping>> = all.groupBy(ATTACKMapping::stepID)
    private val byTechniqueID: Map<String, List<ATTACKMapping>> = all.groupBy(ATTACKMapping::techniqueID)

    fun mappings(stepID: String): List<ATTACKMapping> = byStepID[stepID].orEmpty()
    fun mappingsForTechnique(id: String): List<ATTACKMapping> = byTechniqueID[id].orEmpty()

    private fun MutableList<ATTACKMapping>.mapping(
        stepID: String,
        techniqueID: String,
        relationships: List<ATTACKRelationship>,
        note: String? = null,
    ) {
        add(ATTACKMapping(stepID, techniqueID, relationships, note))
    }

    private fun MutableList<ATTACKMapping>.mappings(
        stepID: String,
        techniqueIDs: List<String>,
        relationships: List<ATTACKRelationship>,
        note: String? = null,
    ) {
        techniqueIDs.forEach { techniqueID -> mapping(stepID, techniqueID, relationships, note) }
    }
}
