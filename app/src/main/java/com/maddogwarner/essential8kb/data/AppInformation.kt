package com.maddogwarner.essential8kb.data

object AppInformation {
    const val aboutTitle = "About Essential 8"

    const val aboutDescription = "Essential 8 Knowledge Base is designed to give administrators just the technical details they need for each Essential Eight control as a quick reference."

    const val contentScope = "The guidance is scoped to practical Windows administration details and should be checked against the current ASD Essential Eight Maturity Model before implementation."

    const val aboutMeTitle = "About Me"

    const val aboutMeDescription = "MadDogWarner is not affiliated with ASD or Microsoft in any way. This project is a passion project built to provide a clear, easy to understand security tool that helps technical teams uplift Essential Eight practices to the masses."

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

    const val privacyPolicy = "Essential 8 Knowledge Base does not collect, record, store, transmit, or share any user data. The app does not require account access and does not request access to the microphone, camera, location services, contacts, photos, or other device sensors."

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
    )

    private fun referenceUrl(url: String): String {
        require(url.startsWith("https://")) { "Invalid reference URL: $url" }
        return url
    }
}
