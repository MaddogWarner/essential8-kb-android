package com.maddogwarner.essential8kb.data

object Microsoft365AdditionalControlsData {
    /// M365 protections are licensed-product additions that apply across maturity levels;
    /// the level is included so coverage copy can match the page being viewed.
    fun protections(controlID: Int, level: MaturityLevel, licenseMode: Microsoft365LicenseMode): List<Microsoft365AdditionalProtection> {
        if (licenseMode == Microsoft365LicenseMode.NONE) return emptyList()

        val protections = e3P1Protections(controlID, level).toMutableList()

        if (licenseMode == Microsoft365LicenseMode.E3_P2 || licenseMode == Microsoft365LicenseMode.E5) {
            protections.addAll(p2IdentityProtections(controlID, level))
        }

        if (licenseMode == Microsoft365LicenseMode.E5) {
            protections.addAll(e5Protections(controlID, level))
        }

        return protections
    }

    private fun e3P1Protections(controlID: Int, level: MaturityLevel): List<Microsoft365AdditionalProtection> =
        when (controlID) {
        1 -> listOf(
                Microsoft365AdditionalProtection(
                    title = "Defender for Endpoint P1 attack surface reduction",
                    coverage = "Partially supports Application Control ${level.shortName} by reducing common execution paths, but it does not replace AppLocker or WDAC allow-listing.",
                    basicSettings = listOf(
                        "Microsoft Defender portal: Endpoints > Configuration management > Attack surface reduction rules",
                        "Enable ASR rules that block executable content from email, webmail and Office child processes",
                        "Use Intune endpoint security policies to deploy Defender Antivirus, firewall and attack surface reduction baselines"
                    )
                )
            )
        2 -> listOf(
                Microsoft365AdditionalProtection(
                    title = "Intune app inventory and update deployment",
                    coverage = "Partially supports Patch Applications ${level.shortName} by improving visibility and deployment control for managed apps.",
                    basicSettings = listOf(
                        "Intune admin center: Apps > Monitor > Discovered apps",
                        "Deploy supported Microsoft Store, Win32 and Microsoft 365 Apps updates through Intune",
                        "Use device compliance reports to identify stale or unmanaged endpoints"
                    )
                )
            )
        3 -> listOf(
                Microsoft365AdditionalProtection(
                    title = "Cloud-managed Office macro controls",
                    coverage = "Supports Configure Microsoft Office Macros ${level.shortName} by applying Office policy settings through Intune instead of only Group Policy.",
                    basicSettings = listOf(
                        "Intune admin center: Devices > Configuration > Settings catalog > Microsoft Office security settings",
                        "Block macros from running in Office files from the internet",
                        "Disable unsigned VBA macros and require trusted locations to be explicitly controlled"
                    )
                )
            )
        4 -> listOf(
                Microsoft365AdditionalProtection(
                    title = "Defender for Endpoint P1 web and network protection",
                    coverage = "Supports User Application Hardening ${level.shortName} by adding managed browser, network and endpoint hardening controls.",
                    basicSettings = listOf(
                        "Microsoft Defender portal: Settings > Endpoints > Advanced features > Network protection = On",
                        "Deploy ASR rules for Office, script and executable abuse paths",
                        "Use Intune security baselines for Microsoft Edge and Defender Antivirus"
                    )
                )
            )
        5 -> listOf(
                Microsoft365AdditionalProtection(
                    title = "Entra ID P1 Conditional Access for administrator access",
                    coverage = "Partially supports Restrict Administrative Privileges ${level.shortName} by enforcing access conditions for cloud admin roles; it does not remove local admin rights by itself.",
                    basicSettings = listOf(
                        "Entra admin center: Protection > Conditional Access > New policy",
                        "Require MFA for all administrator roles",
                        "Block legacy authentication and require compliant or hybrid joined devices for admin portals"
                    )
                )
            )
        6 -> listOf(
                Microsoft365AdditionalProtection(
                    title = "Intune update rings and compliance reporting",
                    coverage = "Supports Patch Operating Systems ${level.shortName} for enrolled Windows endpoints by managing update cadence and restart behaviour.",
                    basicSettings = listOf(
                        "Intune admin center: Devices > Windows > Update rings for Windows 10 and later",
                        "Set quality update deferrals, deadlines and grace periods",
                        "Use compliance policies to mark devices non-compliant when minimum OS versions are not met"
                    )
                )
            )
        7 -> listOf(
                Microsoft365AdditionalProtection(
                    title = "Entra ID P1 Conditional Access MFA",
                    coverage = "Supports Multi-factor Authentication ${level.shortName} for Microsoft 365 and Entra-integrated apps.",
                    basicSettings = listOf(
                        "Entra admin center: Protection > Conditional Access > New policy",
                        "Require MFA for administrators and users accessing Microsoft 365 cloud apps",
                        "Exclude emergency access accounts and monitor sign-in logs during rollout"
                    )
                )
            )
        8 -> listOf(
                Microsoft365AdditionalProtection(
                    title = "OneDrive known folder move and cloud retention",
                    coverage = "Partially supports Regular Backups ${level.shortName} for user files stored in Microsoft 365, but it is not a full endpoint or server backup replacement.",
                    basicSettings = listOf(
                        "Intune admin center: Settings catalog > OneDrive > Silently move Windows known folders to OneDrive",
                        "Microsoft Purview portal: Data lifecycle management > Retention policies for SharePoint and OneDrive",
                        "Keep separate backup coverage for servers, line-of-business data and non-synced endpoint paths"
                    )
                )
            )
        else -> emptyList()
        }
    }

    private fun p2IdentityProtections(controlID: Int, level: MaturityLevel): List<Microsoft365AdditionalProtection> =
        when (controlID) {
        5 -> listOf(
                Microsoft365AdditionalProtection(
                    title = "Entra ID P2 Privileged Identity Management",
                    coverage = "Supports Restrict Administrative Privileges ${level.shortName} for cloud roles by making privileged access just-in-time, time-bound and approval-aware.",
                    basicSettings = listOf(
                        "Entra admin center: Identity governance > Privileged Identity Management",
                        "Make admin role assignments eligible instead of permanent",
                        "Require MFA, justification and approval for high-impact role activation"
                    )
                )
            )
        7 -> listOf(
                Microsoft365AdditionalProtection(
                    title = "Entra ID P2 risk-based access controls",
                    coverage = "Supports Multi-factor Authentication ${level.shortName} by adapting MFA and access decisions to user and sign-in risk.",
                    basicSettings = listOf(
                        "Entra admin center: Protection > Conditional Access > User risk and sign-in risk policies",
                        "Require phishing-resistant MFA or password reset for high-risk users",
                        "Monitor Identity Protection risk detections before enforcing tenant-wide policies"
                    )
                )
            )
        else -> emptyList()
        }
    }

    private fun e5Protections(controlID: Int, level: MaturityLevel): List<Microsoft365AdditionalProtection> =
        when (controlID) {
        1 -> listOf(
                Microsoft365AdditionalProtection(
                    title = "Defender for Endpoint P2 investigation and hunting",
                    coverage = "Adds detection, investigation and response around Application Control ${level.shortName}, but WDAC or AppLocker remain the enforcement controls.",
                    basicSettings = listOf(
                        "Microsoft Defender portal: Endpoints > Advanced features > Enable EDR in block mode where appropriate",
                        "Use advanced hunting to find script, LOLBin and unsigned executable activity",
                        "Review device timeline and incidents for blocked or suspicious execution attempts"
                    )
                )
            )
        2 -> listOf(
                Microsoft365AdditionalProtection(
                    title = "Defender Vulnerability Management core capabilities",
                    coverage = "Supports Patch Applications ${level.shortName} with continuous software inventory, exposure visibility and security recommendations.",
                    basicSettings = listOf(
                        "Microsoft Defender portal: Endpoints > Vulnerability management > Recommendations",
                        "Prioritise exposed applications with known exploited vulnerabilities",
                        "Track remediation status after app updates are deployed"
                    )
                )
            )
        3 -> listOf(
                Microsoft365AdditionalProtection(
                    title = "Defender for Office 365 Plan 2 attachment and link protection",
                    coverage = "Adds email and collaboration protection around macro-borne threats for Configure Microsoft Office Macros ${level.shortName}.",
                    basicSettings = listOf(
                        "Microsoft Defender portal: Email & collaboration > Policies & rules > Threat policies",
                        "Enable Safe Attachments and Safe Links using Standard or Strict preset security policies",
                        "Use Threat Explorer and AIR to investigate malicious Office documents"
                    )
                )
            )
        4 -> listOf(
                Microsoft365AdditionalProtection(
                    title = "Defender for Cloud Apps session and app governance",
                    coverage = "Partially supports User Application Hardening ${level.shortName} by controlling risky cloud app sessions and unsanctioned SaaS usage.",
                    basicSettings = listOf(
                        "Microsoft Defender portal: Cloud Apps > Policies > App discovery policies",
                        "Sanction approved cloud apps and mark risky apps as unsanctioned",
                        "Use Conditional Access App Control for monitored or blocked browser sessions"
                    )
                )
            )
        5 -> listOf(
                Microsoft365AdditionalProtection(
                    title = "Defender for Identity privileged account monitoring",
                    coverage = "Adds detection around Restrict Administrative Privileges ${level.shortName} by monitoring identity abuse and lateral movement signals.",
                    basicSettings = listOf(
                        "Microsoft Defender portal: Settings > Identities > Sensors",
                        "Deploy Defender for Identity sensors to domain controllers",
                        "Review identity security posture recommendations for privileged accounts"
                    )
                )
            )
        6 -> listOf(
                Microsoft365AdditionalProtection(
                    title = "Defender for Endpoint P2 OS exposure management",
                    coverage = "Supports Patch Operating Systems ${level.shortName} by highlighting missing OS updates and exposed devices.",
                    basicSettings = listOf(
                        "Microsoft Defender portal: Endpoints > Vulnerability management > Weaknesses",
                        "Filter recommendations by operating system and exposed devices",
                        "Use remediation tasks to coordinate patching with endpoint administrators"
                    )
                )
            )
        7 -> listOf(
                Microsoft365AdditionalProtection(
                    title = "E5 identity and cloud-app signal integration",
                    coverage = "Enhances Multi-factor Authentication ${level.shortName} with Entra ID P2 risk, Defender for Cloud Apps session controls and Defender XDR incident context.",
                    basicSettings = listOf(
                        "Use Conditional Access policies with sign-in risk, user risk and session controls",
                        "Require phishing-resistant MFA for administrators and high-risk access paths",
                        "Review Defender XDR incidents that combine identity, endpoint and cloud app signals"
                    )
                )
            )
        8 -> listOf(
                Microsoft365AdditionalProtection(
                    title = "Purview and audit support for Microsoft 365 data recovery",
                    coverage = "Partially supports Regular Backups ${level.shortName} for Microsoft 365 data governance and investigation, but does not replace immutable backup storage.",
                    basicSettings = listOf(
                        "Microsoft Purview portal: Data lifecycle management > Retention labels and policies",
                        "Enable audit search and review high-impact deletion or exfiltration events",
                        "Use separate backup products or storage controls for immutable recovery requirements"
                    )
                )
            )
        else -> emptyList()
        }
    }
}
