package com.maddogwarner.essential8kb.data

import com.maddogwarner.essential8kb.data.attack.ATTACKCatalogue

object AppInformation {
    const val marketingVersion = "1.0"

    const val aboutTitle = "About Essential 8"

    const val aboutDescription = "Essential 8 Knowledge Base is designed to give administrators just the technical details they need for each Essential Eight control as a quick reference."

    const val contentScope = "The guidance is scoped to practical Windows administration details and should be checked against the current ASD Essential Eight Maturity Model before implementation."

    const val aboutMeTitle = "About Me"

    const val aboutMeDescription = "MadDogWarner is not affiliated with ASD, Microsoft or The MITRE Corporation in any way. This project is a passion project built to provide a clear, easy to understand security tool that helps technical teams uplift Essential Eight practices to the masses."

    const val attackDisclaimerShort = "Derived mapping — not an ASD or MITRE product."

    const val attackDisclaimer = "ASD does not publish an Essential Eight to MITRE ATT&CK mapping. These technique links are derived and curated by the author from the ASD Essential Eight to ISM mapping and MITRE ATT&CK technique descriptions, and are provided as an aid to understanding — not as an authoritative or certified mapping. Verify against your own threat model before relying on them."

    const val attackAttribution = "© ${ATTACKCatalogue.attackCopyrightYear} The MITRE Corporation. This work is reproduced and distributed with the permission of The MITRE Corporation. MITRE ATT&CK® and ATT&CK® are registered trademarks of The MITRE Corporation."

    const val attackCoverageCaveat = "MITRE does not claim ATT&CK enumerates every possible adversary behaviour, and using ATT&CK does not guarantee full defensive coverage. A technique shown as covered means the mapped implementation steps are complete — not that your environment is defended against it."

    const val attackVersionNote = "Mapped against MITRE ATT&CK® Enterprise ${ATTACKCatalogue.attackVersion}, released ${ATTACKCatalogue.attackVersionReleased}."

    val authorLinks: List<ReferenceLink> = listOf(
        ReferenceLink(
            title = "MadDogWarner website",
            url = referenceUrl("https://maddogwarner.com"),
        ),
        ReferenceLink(
            title = "MadDogWarner GitHub",
            url = referenceUrl("https://github.com/MadDogWarner"),
        ),
    )

    const val privacyTitle = "Privacy Policy"

    const val privacyPolicy = "Essential 8 Knowledge Base has no accounts, no analytics, and makes no network requests of its own — nothing you enter is ever sent to the developer or any third party. Your assessment progress, notes and audit history are stored only on your device. That data leaves your device only if you export a backup yourself, or through your device's own system backup. External reference links open in your browser. The app does not request access to the microphone, camera, location services, contacts, photos, or other device sensors."

    val privacyPolicyLink = ReferenceLink(
        title = "App privacy policy",
        url = referenceUrl("https://maddogwarner.com/privacy/essential-8-knowledge-base/"),
    )

    val referenceLinks: List<ReferenceLink> = listOf(
        ReferenceLink(
            title = "ASD Essential Eight maturity model",
            url = referenceUrl("https://www.cyber.gov.au/business-government/asds-cyber-security-frameworks/essential-eight/essential-eight-maturity-model"),
        ),
        ReferenceLink(
            title = "ASD Information Security Manual",
            url = referenceUrl("https://www.cyber.gov.au/resources-business-and-government/essential-cyber-security/ism"),
        ),
        ReferenceLink(
            title = "Microsoft Defender for Endpoint plans",
            url = referenceUrl("https://learn.microsoft.com/en-us/microsoft-365/security/defender-endpoint/defender-endpoint-plan-1-2"),
        ),
        ReferenceLink(
            title = "Microsoft Defender service description",
            url = referenceUrl("https://learn.microsoft.com/en-us/office365/servicedescriptions/microsoft-365-service-descriptions/microsoft-365-tenantlevel-services-licensing-guidance/microsoft-defender-service-description"),
        ),
        ReferenceLink(
            title = "Microsoft Entra Conditional Access",
            url = referenceUrl("https://learn.microsoft.com/en-us/entra/identity/conditional-access/overview"),
        ),
        ReferenceLink(
            title = "Microsoft Entra MFA licensing",
            url = referenceUrl("https://learn.microsoft.com/en-us/entra/identity/authentication/concept-mfa-licensing"),
        ),
        ReferenceLink(
            title = "MITRE ATT&CK Enterprise matrix",
            url = referenceUrl("https://attack.mitre.org/"),
        ),
        ReferenceLink(
            title = "MITRE ATT&CK terms of use",
            url = referenceUrl("https://attack.mitre.org/resources/legal-and-branding/terms-of-use/"),
        ),
    )

    private fun referenceUrl(url: String): String {
        require(url.startsWith("https://")) { "Invalid reference URL: $url" }
        return url
    }
}
