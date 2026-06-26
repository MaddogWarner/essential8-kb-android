package com.maddogwarner.essential8kb.data

private fun step(
    controlID: Int,
    level: MaturityLevel,
    index: Int,
    title: String,
    description: String,
    technicalDetails: List<String> = emptyList(),
): ImplementationStep = ImplementationStep(
    id = "$controlID-${level.level}-$index",
    title = title,
    description = description,
    technicalDetails = technicalDetails,
)

object EssentialControlsData {
    val all: List<EssentialControl> = listOf(
        applicationControl,
        patchApplications,
        configureOfficeMacros,
        userApplicationHardening,
        restrictAdministrativePrivileges,
        patchOperatingSystems,
        multiFactorAuthentication,
        regularBackups
    )

    const val ml0GenericDescription: String =
        "ML0 means no controls implemented for this mitigation. The system is in its default state and the threats this control is designed to address are not mitigated."

    // MARK: - 1. Application Control

    val applicationControl = EssentialControl(
        id = 1,
        name = "Application Control",
        icon = materialIconForSfSymbol("checkmark.shield"),
        overview = "Prevents execution of unapproved or malicious executables, libraries, scripts, installers, compiled HTML, control panel applets and drivers. Stops users (and malware) running anything that hasn't been allow-listed.",
        ml0Description = "No application control. Any user — or any process running as that user — can execute arbitrary binaries, scripts and installers from any location, including user-writable folders such as %TEMP%.",
        ml1 = MaturityLevelContent(
            summary = "Application control enforced on workstations, restricting execution from standard user profile directories and temporary folders.",
            steps = listOf(
                step(1, MaturityLevel.ML1, 0,
                    title = "Enable the Application Identity service",
                    description = "AppLocker depends on the AppIDSvc service. Set it to start automatically on all in-scope workstations.",
                    technicalDetails = listOf(
                        "Command: sc config AppIDSvc start= auto",
                        "GPO: Computer Configuration → Windows Settings → Security Settings → System Services → Application Identity = Automatic"
                    )
                ),
                step(1, MaturityLevel.ML1, 1,
                    title = "Create AppLocker default rules",
                    description = "Generate the default allow rules for each rule collection so signed Windows and Program Files binaries continue to run.",
                    technicalDetails = listOf(
                        "GPO: Computer Configuration → Windows Settings → Security Settings → Application Control Policies → AppLocker",
                        "For each collection (Executable, Windows Installer, Script, Packaged app, DLL): right-click → Create Default Rules",
                        "Set Enforcement = Enforce rules for all collections"
                    )
                ),
                step(1, MaturityLevel.ML1, 2,
                    title = "Block execution from user-writable locations",
                    description = "Default rules already deny anything outside Windows and Program Files for standard users. Verify and add explicit deny rules for %TEMP%, %LOCALAPPDATA% and the user profile root if custom paths exist.",
                    technicalDetails = listOf(
                        "Deny path: %OSDRIVE%\\Users\\*",
                        "Deny path: %LOCALAPPDATA%\\Temp\\*",
                        "Deny path: %TEMP%\\*"
                    )
                ),
                step(1, MaturityLevel.ML1, 3,
                    title = "Log block events",
                    description = "AppLocker writes to a dedicated event log so blocked executions are auditable.",
                    technicalDetails = listOf(
                        "Event log path: Applications and Services Logs → Microsoft → Windows → AppLocker",
                        "Channels: EXE and DLL, MSI and Script, Packaged app-Deployment, Packaged app-Execution"
                    )
                )
            ),
            gapNote = null
        ),
        ml2 = MaturityLevelContent(
            summary = "Application control also enforced on internet-facing servers, with execution events centrally logged.",
            steps = listOf(
                step(1, MaturityLevel.ML2, 0,
                    title = "Extend AppLocker enforcement to servers",
                    description = "Apply the same allow-listing policy to internet-facing Windows servers. Test in audit mode first to identify business-required binaries outside Program Files.",
                    technicalDetails = listOf(
                        "Set Enforcement = Audit only, then review AppLocker event log",
                        "Promote to Enforce rules once exceptions are captured"
                    )
                ),
                step(1, MaturityLevel.ML2, 1,
                    title = "Deploy Windows Defender Application Control (WDAC)",
                    description = "WDAC provides stronger, kernel-level enforcement than AppLocker and survives admin tampering. Build a base policy from a clean reference machine.",
                    technicalDetails = listOf(
                        "PowerShell: New-CIPolicy -FilePath base.xml -Level Publisher -UserPEs -ScanPath C:\\",
                        "Convert: ConvertFrom-CIPolicy base.xml SIPolicy.p7b",
                        "Deploy via GPO: Computer Configuration → Administrative Templates → System → Device Guard → Deploy Windows Defender Application Control"
                    )
                ),
                step(1, MaturityLevel.ML2, 2,
                    title = "Forward AppLocker / WDAC events centrally",
                    description = "Use Windows Event Forwarding to ship execution and block events to a collector for retention and analysis.",
                    technicalDetails = listOf(
                        "GPO: Computer Configuration → Administrative Templates → Windows Components → Event Forwarding → Configure target Subscription Manager",
                        "Collector configured with: wecutil qc"
                    )
                )
            ),
            gapNote = null
        ),
        ml3 = MaturityLevelContent(
            summary = "Application control on all workstations and servers, with Microsoft's recommended block rules and vulnerable driver blocklist enforced.",
            steps = listOf(
                step(1, MaturityLevel.ML3, 0,
                    title = "Apply Microsoft's recommended block rules",
                    description = "Microsoft publishes a list of well-known binaries (e.g. bash.exe, cdb.exe, cscript.exe variants) that bypass application control. Merge these into your WDAC policy.",
                    technicalDetails = listOf(
                        "Reference: Microsoft recommended block rules XML, merged via Merge-CIPolicy",
                        "PowerShell: Merge-CIPolicy -PolicyPaths base.xml, MicrosoftRecommendedBlockRules.xml -OutputFilePath merged.xml"
                    )
                ),
                step(1, MaturityLevel.ML3, 1,
                    title = "Enable the vulnerable driver blocklist",
                    description = "Windows ships with a Microsoft-maintained list of known-vulnerable drivers. Enabling this prevents loading drivers commonly abused for BYOVD attacks.",
                    technicalDetails = listOf(
                        "UI: Windows Security → Device Security → Core Isolation → Microsoft Vulnerable Driver Blocklist = On",
                        "Registry: HKLM\\SYSTEM\\CurrentControlSet\\Control\\CI\\Config → VulnerableDriverBlocklistEnable = 1 (DWORD)"
                    )
                ),
                step(1, MaturityLevel.ML3, 2,
                    title = "Enforce Memory Integrity (HVCI)",
                    description = "Hypervisor-protected Code Integrity ensures only signed kernel code can run, complementing WDAC for user-mode code.",
                    technicalDetails = listOf(
                        "GPO: Computer Configuration → Administrative Templates → System → Device Guard → Turn On Virtualization Based Security = Enabled, Virtualization Based Protection of Code Integrity = Enabled with UEFI lock",
                        "Registry: HKLM\\SYSTEM\\CurrentControlSet\\Control\\DeviceGuard\\Scenarios\\HypervisorEnforcedCodeIntegrity → Enabled = 1"
                    )
                )
            ),
            gapNote = "Microsoft Smart App Control (reputation-based execution) augments this control on Windows 11 but relies on Microsoft's Intelligent Security Graph cloud service. Behavioural / heuristic execution restriction beyond static allow-listing typically requires an EDR product."
        )
    )

    // MARK: - 2. Patch Applications

    val patchApplications = EssentialControl(
        id = 2,
        name = "Patch Applications",
        icon = materialIconForSfSymbol("arrow.down.app"),
        overview = "Apply patches for vulnerabilities in internet-facing services, office productivity suites, web browsers, email clients, PDF software and security products within defined timeframes. Remove apps no longer supported by their vendor.",
        ml0Description = "No regular patching cycle for applications. Unsupported applications remain installed and exploited vulnerabilities are left unpatched indefinitely.",
        ml1 = MaturityLevelContent(
            summary = "Office productivity, browsers, email and security products patched within one month; internet-facing services within two weeks (or 48 hours where an exploit exists). Unsupported applications removed.",
            steps = listOf(
                step(2, MaturityLevel.ML1, 0,
                    title = "Enable Microsoft Update for Office",
                    description = "Lets Office (M365 Apps / Office 2021+) receive updates automatically through the Microsoft Update channel.",
                    technicalDetails = listOf(
                        "GPO: Computer Configuration → Administrative Templates → Microsoft Office (Machine) → Updates → Enable Automatic Updates = Enabled",
                        "GPO: Update Channel = Monthly Enterprise Channel (or Current Channel for faster cadence)"
                    )
                ),
                step(2, MaturityLevel.ML1, 1,
                    title = "Enable Microsoft Edge auto-update",
                    description = "Edge updates ship through its own updater service. Keep the override allowing updates in place.",
                    technicalDetails = listOf(
                        "GPO: Computer Configuration → Administrative Templates → Microsoft Edge Update → Applications → Microsoft Edge → Update policy override = Always allow updates",
                        "Registry: HKLM\\SOFTWARE\\Policies\\Microsoft\\EdgeUpdate → UpdateDefault = 1 (DWORD)"
                    )
                ),
                step(2, MaturityLevel.ML1, 2,
                    title = "Inventory installed applications",
                    description = "You can't patch what you can't see. Use built-in tools to enumerate installed software across the fleet.",
                    technicalDetails = listOf(
                        "PowerShell: Get-Package | Select Name, Version, ProviderName",
                        "PowerShell: winget list --accept-source-agreements",
                        "Registry: HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\*"
                    )
                ),
                step(2, MaturityLevel.ML1, 3,
                    title = "Uninstall unsupported applications",
                    description = "Vendor-unsupported software (e.g. legacy Java, Flash, end-of-life Office versions) must be removed.",
                    technicalDetails = listOf(
                        "PowerShell: Get-Package -Name '<name>' | Uninstall-Package",
                        "PowerShell: winget uninstall <id>"
                    )
                )
            ),
            gapNote = "Application-level vulnerability scanning at the cadence required (fortnightly for internet-facing, monthly otherwise) is not provided by Windows alone — a dedicated scanner is needed."
        ),
        ml2 = MaturityLevelContent(
            summary = "Office productivity, browsers, email and security products patched within two weeks. Internet-facing services patched within two weeks (or 48 hours if an exploit exists).",
            steps = listOf(
                step(2, MaturityLevel.ML2, 0,
                    title = "Tighten Office and Edge update cadence",
                    description = "Move M365 Apps to Current Channel for faster security update delivery, and shorten deferral windows.",
                    technicalDetails = listOf(
                        "GPO: Microsoft Office (Machine) → Updates → Update Channel = Current Channel",
                        "GPO: Microsoft Edge Update → Auto-update check period override = 60 minutes or less"
                    )
                ),
                step(2, MaturityLevel.ML2, 1,
                    title = "Enforce restart deadlines",
                    description = "Patches only mitigate once they are applied and the machine has restarted. Configure short deadlines for restart.",
                    technicalDetails = listOf(
                        "GPO: Computer Configuration → Administrative Templates → Windows Components → Windows Update → Manage end user experience → Specify deadlines for automatic updates and restarts",
                        "Quality update deadline: 2 days, grace period: 1 day"
                    )
                )
            ),
            gapNote = "Weekly vulnerability scanning of office productivity / browsers etc. is required and cannot be performed by built-in Windows tooling."
        ),
        ml3 = MaturityLevelContent(
            summary = "Internet-facing services patched within 48 hours when an exploit exists, otherwise within two weeks. Daily vulnerability scanning.",
            steps = listOf(
                step(2, MaturityLevel.ML3, 0,
                    title = "Emergency patch deployment",
                    description = "When an exploited vulnerability is disclosed, push the patch immediately using WSUS / Windows Update for Business deadline of zero days.",
                    technicalDetails = listOf(
                        "GPO: Specify deadlines for automatic updates and restarts → Quality update deadline = 0 (deploy immediately, allow grace period of 0)",
                        "PowerShell (per-host expedite): Install-Module PSWindowsUpdate; Get-WindowsUpdate -Install -AcceptAll -AutoReboot"
                    )
                )
            ),
            gapNote = "Daily authenticated vulnerability scanning across internet-facing services and end-user productivity software is out of scope for Windows built-in tooling; a dedicated vulnerability management product is required."
        )
    )

    // MARK: - 3. Configure Microsoft Office Macro Settings

    val configureOfficeMacros = EssentialControl(
        id = 3,
        name = "Configure MS Office Macros",
        icon = materialIconForSfSymbol("doc.text.magnifyingglass"),
        overview = "Restrict the use of Microsoft Office macros to only users with a demonstrated business requirement, block macros from the internet, and ensure security settings cannot be changed by users.",
        ml0Description = "Macros run with default Office settings. Users can enable any macro, including macros embedded in documents downloaded from the internet or received via email.",
        ml1 = MaturityLevelContent(
            summary = "Macros disabled for users without a documented business need. Macros from internet-sourced files blocked. Macro security settings cannot be changed by users.",
            steps = listOf(
                step(3, MaturityLevel.ML1, 0,
                    title = "Block macros from the internet (Mark-of-the-Web)",
                    description = "Office blocks macros in files that carry the internet zone identifier. Enable the policy for each Office app.",
                    technicalDetails = listOf(
                        "GPO: User Configuration → Administrative Templates → Microsoft <App> <Version> → <App> Options → Security → Trust Center → Block macros from running in Office files from the Internet = Enabled",
                        "Registry: HKCU\\Software\\Policies\\Microsoft\\Office\\16.0\\<app>\\Security → blockcontentexecutionfrominternet = 1 (DWORD)",
                        "Apply for each of: word, excel, powerpoint, outlook, access, visio, project, publisher"
                    )
                ),
                step(3, MaturityLevel.ML1, 1,
                    title = "Disable VBA macros without notification",
                    description = "For users without a business requirement, the VBA Macro Notification Setting should be 'Disabled without notification' so no UI bypass is shown.",
                    technicalDetails = listOf(
                        "GPO: User Configuration → Administrative Templates → Microsoft <App> <Version> → <App> Options → Security → Trust Center → VBA Macro Notification Settings = Disabled without notification",
                        "Registry: HKCU\\Software\\Policies\\Microsoft\\Office\\16.0\\<app>\\Security → vbawarnings = 4 (DWORD)"
                    )
                ),
                step(3, MaturityLevel.ML1, 2,
                    title = "Lock down the Trust Center",
                    description = "Stop users adding trusted locations / trusted publishers themselves.",
                    technicalDetails = listOf(
                        "GPO: <App> Options → Security → Trust Center → Disable all trusted locations = Enabled (or restrict to administrator-defined locations only)",
                        "Registry: HKCU\\Software\\Policies\\Microsoft\\Office\\16.0\\<app>\\Security\\Trusted Locations → AllLocationsDisabled = 1"
                    )
                )
            ),
            gapNote = null
        ),
        ml2 = MaturityLevelContent(
            summary = "Macros only run from Trusted Locations (with write access limited to approvers) or where digitally signed by a trusted publisher. Antivirus scanning of macros enabled. Macro execution events logged.",
            steps = listOf(
                step(3, MaturityLevel.ML2, 0,
                    title = "Allow only digitally signed macros",
                    description = "Set the macro notification setting so only macros signed by a trusted publisher run silently; all others are blocked.",
                    technicalDetails = listOf(
                        "GPO: <App> Options → Security → Trust Center → VBA Macro Notification Settings = Disable all except digitally signed macros",
                        "Registry: HKCU\\Software\\Policies\\Microsoft\\Office\\16.0\\<app>\\Security → vbawarnings = 3 (DWORD)"
                    )
                ),
                step(3, MaturityLevel.ML2, 1,
                    title = "Enable AMSI scanning of macros",
                    description = "Office passes macro contents to AMSI so Microsoft Defender (or another AMSI provider) can inspect them before execution.",
                    technicalDetails = listOf(
                        "GPO: User Configuration → Administrative Templates → Microsoft <App> → Security Settings → Macro Runtime Scan Scope = Enable for all documents",
                        "Registry: HKCU\\Software\\Policies\\Microsoft\\Office\\16.0\\<app>\\Security → MacroRuntimeScanScope = 2 (DWORD)"
                    )
                ),
                step(3, MaturityLevel.ML2, 2,
                    title = "Enable VBA macro logging",
                    description = "Office writes macro execution events to the Windows Application event log, source 'Microsoft Office <ver>'.",
                    technicalDetails = listOf(
                        "Registry: HKCU\\Software\\Policies\\Microsoft\\Office\\16.0\\Common\\Security → EnableLogging = 1 (DWORD)",
                        "Event log: Windows Logs → Application, Source = Microsoft Office <ver>"
                    )
                )
            ),
            gapNote = null
        ),
        ml3 = MaturityLevelContent(
            summary = "Macros only run when signed with a V3 signature by a trusted publisher. Write access to trusted locations restricted to vetted personnel. Macro events centrally logged and analysed.",
            steps = listOf(
                step(3, MaturityLevel.ML3, 0,
                    title = "Require V3 (XML-DSig) signatures",
                    description = "V3 signatures cover VBA projects more completely than legacy signatures. After re-signing approved VBA projects, enable the Office policy that only trusts V3-signed macros.",
                    technicalDetails = listOf(
                        "GPO: User Configuration → Policies → Administrative Templates → Microsoft Office 2016 → Security Settings → Trust Center → Only trust VBA macros that use V3 signatures = Enabled",
                        "Office Cloud Policy Service: Only trust VBA macros that use V3 signatures = Enabled",
                        "Use the Readiness Toolkit and SignTool/Office signing workflow to inventory and re-sign approved VBA projects before enforcement"
                    )
                ),
                step(3, MaturityLevel.ML3, 1,
                    title = "Restrict write access to Trusted Locations",
                    description = "Trusted Locations should live on a network share where NTFS ACLs restrict write to a small approval group; users have read-only access.",
                    technicalDetails = listOf(
                        "icacls \\\\fileserver\\Macros /grant 'DOMAIN\\MacroApprovers:(M)' /grant 'DOMAIN\\Domain Users:(RX)' /inheritance:r"
                    )
                )
            ),
            gapNote = "Centralised logging and analysis of macro execution events typically lands in a SIEM. Windows Event Forwarding ships the events; analysis tooling is separate."
        )
    )

    // MARK: - 4. User Application Hardening

    val userApplicationHardening = EssentialControl(
        id = 4,
        name = "User Application Hardening",
        icon = materialIconForSfSymbol("lock.shield"),
        overview = "Configure web browsers and other user-facing applications to disable risky features — Java, web ads, legacy browsers, PowerShell v2, Office OLE — and prevent users from changing those settings.",
        ml0Description = "Browsers run with default configurations. Java is enabled, web advertisements are processed, Internet Explorer 11 may be present and PowerShell v2 may be available.",
        ml1 = MaturityLevelContent(
            summary = "Browsers do not process Java or web advertisements from the internet. Internet Explorer 11 disabled or removed. Browser settings cannot be changed by users.",
            steps = listOf(
                step(4, MaturityLevel.ML1, 0,
                    title = "Disable Internet Explorer 11",
                    description = "IE 11 is unsupported and should be blocked from launching as a standalone browser.",
                    technicalDetails = listOf(
                        "GPO: Computer Configuration → Administrative Templates → Windows Components → Internet Explorer → Disable Internet Explorer 11 as a standalone browser = Enabled, never notify",
                        "Registry: HKLM\\SOFTWARE\\Policies\\Microsoft\\Internet Explorer\\Main → DisableInternetExplorerApp = 1 (DWORD)",
                        "PowerShell: Disable-WindowsOptionalFeature -Online -FeatureName Internet-Explorer-Optional-amd64"
                    )
                ),
                step(4, MaturityLevel.ML1, 1,
                    title = "Block Java in Microsoft Edge",
                    description = "Edge does not process Java applets natively. Ensure no third-party Java plugin is installed and that NPAPI/legacy plugin support remains disabled.",
                    technicalDetails = listOf(
                        "PowerShell: Get-Package -Name '*Java*' | Uninstall-Package",
                        "GPO: Microsoft Edge → Block third party cookies = Enabled (defence in depth)"
                    )
                ),
                step(4, MaturityLevel.ML1, 2,
                    title = "Block web advertisements",
                    description = "Use Edge's built-in tracking prevention at Strict, which also blocks the majority of ad networks.",
                    technicalDetails = listOf(
                        "GPO: Computer Configuration → Administrative Templates → Microsoft Edge → Tracking prevention = Strict",
                        "Registry: HKLM\\SOFTWARE\\Policies\\Microsoft\\Edge → TrackingPrevention = 3 (DWORD)"
                    )
                )
            ),
            gapNote = null
        ),
        ml2 = MaturityLevelContent(
            summary = "Microsoft Office is prevented from creating child processes. PowerShell module and script-block logging enabled. Attack Surface Reduction rules deployed.",
            steps = listOf(
                step(4, MaturityLevel.ML2, 0,
                    title = "Enable PowerShell logging",
                    description = "Module logging captures pipeline execution; script-block logging captures the actual code, including obfuscated scripts after de-obfuscation.",
                    technicalDetails = listOf(
                        "GPO: Computer Configuration → Administrative Templates → Windows Components → Windows PowerShell → Turn on Module Logging = Enabled, Module Names = *",
                        "GPO: Turn on PowerShell Script Block Logging = Enabled",
                        "Event log: Microsoft-Windows-PowerShell/Operational, IDs 4103 / 4104"
                    )
                ),
                step(4, MaturityLevel.ML2, 1,
                    title = "Deploy Attack Surface Reduction rules",
                    description = "ASR rules are part of Microsoft Defender Antivirus. Start in audit mode, review event log, then enforce.",
                    technicalDetails = listOf(
                        "PowerShell: Add-MpPreference -AttackSurfaceReductionRules_Ids D4F940AB-401B-4EFC-AADC-AD5F3C50688A -AttackSurfaceReductionRules_Actions Enabled  # Block Office apps from creating child processes",
                        "Other key rules: BE9BA2D9-53EA-4CDC-84E5-9B1EEEE46550 (block executable content from email), 3B576869-A4EC-4529-8536-B80A7769E899 (block Office from creating executables)",
                        "GPO: Computer Configuration → Administrative Templates → Windows Components → Microsoft Defender Antivirus → Microsoft Defender Exploit Guard → Attack Surface Reduction"
                    )
                ),
                step(4, MaturityLevel.ML2, 2,
                    title = "Enable command-line process auditing",
                    description = "Captures the full command line for every process creation event (4688), essential for incident response.",
                    technicalDetails = listOf(
                        "GPO: Computer Configuration → Administrative Templates → System → Audit Process Creation → Include command line in process creation events = Enabled",
                        "GPO: Computer Configuration → Windows Settings → Security Settings → Advanced Audit Policy Configuration → Detailed Tracking → Audit Process Creation = Success"
                    )
                )
            ),
            gapNote = null
        ),
        ml3 = MaturityLevelContent(
            summary = "PowerShell v2 disabled. PowerShell constrained language mode enforced. .NET Framework 3.5 (and earlier) removed where not required.",
            steps = listOf(
                step(4, MaturityLevel.ML3, 0,
                    title = "Remove PowerShell v2",
                    description = "Windows PowerShell 2.0 lacks modern logging and AMSI integration, making it a common downgrade target.",
                    technicalDetails = listOf(
                        "PowerShell: Disable-WindowsOptionalFeature -Online -FeatureName MicrosoftWindowsPowerShellV2Root",
                        "Server: Uninstall-WindowsFeature PowerShell-V2"
                    )
                ),
                step(4, MaturityLevel.ML3, 1,
                    title = "Enforce Constrained Language Mode",
                    description = "Restricts PowerShell to a safer subset of language elements, blocking arbitrary .NET method invocation. Microsoft documents application control policy enforcement as the durable way to place PowerShell in Constrained Language Mode.",
                    technicalDetails = listOf(
                        "Preferred: deploy WDAC or AppLocker application control policy; PowerShell automatically enters ConstrainedLanguage when an enforced policy is detected",
                        "Verify: ${'$'}ExecutionContext.SessionState.LanguageMode returns ConstrainedLanguage",
                        "Temporary validation only: __PSLockdownPolicy = 4 is not a supported long-term enterprise enforcement mechanism"
                    )
                ),
                step(4, MaturityLevel.ML3, 2,
                    title = "Remove legacy .NET Framework",
                    description = ".NET 3.5 (which includes 2.0) supports older, weaker crypto and is rarely needed on modern endpoints.",
                    technicalDetails = listOf(
                        "PowerShell: Disable-WindowsOptionalFeature -Online -FeatureName NetFx3"
                    )
                )
            ),
            gapNote = null
        )
    )

    // MARK: - 5. Restrict Administrative Privileges

    val restrictAdministrativePrivileges = EssentialControl(
        id = 5,
        name = "Restrict Administrative Privileges",
        icon = materialIconForSfSymbol("person.badge.key"),
        overview = "Limit administrative privileges to the minimum set of users and systems required. Separate privileged and unprivileged accounts. Prevent privileged accounts from accessing the internet, email and web services.",
        ml0Description = "Users have local administrator rights on their workstations. The same account is used for day-to-day activity and administration. Shared admin accounts exist.",
        ml1 = MaturityLevelContent(
            summary = "Requests for privileged access validated on first request and reviewed annually. Privileged users have a separate unprivileged account for email and web browsing. Privileged accounts cannot access the internet, email or external web services.",
            steps = listOf(
                step(5, MaturityLevel.ML1, 0,
                    title = "Remove standard users from local Administrators",
                    description = "Day-to-day user accounts must not be members of the local Administrators group on their workstation.",
                    technicalDetails = listOf(
                        "GPO: Computer Configuration → Preferences → Control Panel Settings → Local Users and Groups → Update local group 'Administrators (built-in)' with explicit membership",
                        "PowerShell: Get-LocalGroupMember Administrators ; Remove-LocalGroupMember Administrators -Member <user>"
                    )
                ),
                step(5, MaturityLevel.ML1, 1,
                    title = "Deploy Windows LAPS",
                    description = "Windows LAPS is built into Windows 11 22H2+ and Windows Server 2019+ (via update). It randomises and rotates the local administrator password and stores it in AD or Entra ID.",
                    technicalDetails = listOf(
                        "AD schema: Update-LapsADSchema",
                        "Grant rights: Set-LapsADComputerSelfPermission -Identity 'OU=Workstations,DC=corp,DC=local'",
                        "GPO: Computer Configuration → Administrative Templates → System → LAPS → Configure password backup directory = Active Directory; Password complexity = Large letters + small letters + numbers + specials; Password length = 20+; Password age = 30 days or less"
                    )
                ),
                step(5, MaturityLevel.ML1, 2,
                    title = "Block internet / email for privileged accounts",
                    description = "Use Group Policy 'Deny logon' rights to prevent admin accounts authenticating to email, web proxy and internet-connected workstations.",
                    technicalDetails = listOf(
                        "GPO (on user workstations): Computer Configuration → Windows Settings → Security Settings → Local Policies → User Rights Assignment → Deny log on locally / Deny log on through Remote Desktop Services = <Privileged Users group>",
                        "Block on proxy: separate AD group for privileged accounts denied at the proxy / firewall layer"
                    )
                )
            ),
            gapNote = null
        ),
        ml2 = MaturityLevelContent(
            summary = "Privileged access requests revalidated every 12 months or sooner. Privileged accounts (other than break-glass) cannot log on to non-privileged operating environments. Credential Guard enabled.",
            steps = listOf(
                step(5, MaturityLevel.ML2, 0,
                    title = "Enable Credential Guard",
                    description = "Stores derived domain credentials in a virtualisation-based secure container, defeating pass-the-hash and pass-the-ticket attacks.",
                    technicalDetails = listOf(
                        "GPO: Computer Configuration → Administrative Templates → System → Device Guard → Turn On Virtualization Based Security = Enabled, Credential Guard Configuration = Enabled with UEFI lock",
                        "Registry: HKLM\\SYSTEM\\CurrentControlSet\\Control\\LSA → LsaCfgFlags = 1 (DWORD)"
                    )
                ),
                step(5, MaturityLevel.ML2, 1,
                    title = "Protect LSASS",
                    description = "Run LSASS as a protected process so non-protected processes cannot read its memory.",
                    technicalDetails = listOf(
                        "Registry: HKLM\\SYSTEM\\CurrentControlSet\\Control\\LSA → RunAsPPL = 1 (DWORD)",
                        "Registry: HKLM\\SYSTEM\\CurrentControlSet\\Control\\LSA → RunAsPPLBoot = 1 (DWORD)"
                    )
                ),
                step(5, MaturityLevel.ML2, 2,
                    title = "Apply Just Enough Administration (JEA)",
                    description = "JEA exposes only specified PowerShell cmdlets/parameters to delegated administrators via constrained session configurations.",
                    technicalDetails = listOf(
                        "PowerShell: New-PSRoleCapabilityFile -Path C:\\JEA\\Roles\\HelpDesk.psrc -VisibleCmdlets 'Get-Service','Restart-Service'",
                        "PowerShell: New-PSSessionConfigurationFile -SessionType RestrictedRemoteServer -Path C:\\JEA\\HelpDesk.pssc -RoleDefinitions @{ 'CORP\\HelpDesk' = @{ RoleCapabilities = 'HelpDesk' } }",
                        "Register: Register-PSSessionConfiguration -Path C:\\JEA\\HelpDesk.pssc -Name HelpDesk"
                    )
                )
            ),
            gapNote = null
        ),
        ml3 = MaturityLevelContent(
            summary = "Privileged users use a separate privileged operating environment (PAW). Privileged accounts are members of Protected Users. All admin actions centrally logged and reviewed.",
            steps = listOf(
                step(5, MaturityLevel.ML3, 0,
                    title = "Add privileged accounts to Protected Users",
                    description = "Protected Users prevents NTLM authentication, DES/RC4 Kerberos and credential caching for those accounts.",
                    technicalDetails = listOf(
                        "PowerShell: Add-ADGroupMember -Identity 'Protected Users' -Members <admin-account>",
                        "Caveat: members cannot use Kerberos delegation or sign in offline"
                    )
                ),
                step(5, MaturityLevel.ML3, 1,
                    title = "Privileged Access Workstation (PAW)",
                    description = "Dedicated hardened workstation, used only for admin tasks, with no email, web or productivity apps. Use Microsoft's PAW reference build (GPO templates).",
                    technicalDetails = listOf(
                        "Restrict membership of local Administrators on PAW to a single break-glass account",
                        "Block outbound internet except to required management endpoints (Windows Firewall outbound rules in Group Policy)",
                        "Forward security events to a collector with: wecutil cs paw-subscription.xml"
                    )
                ),
                step(5, MaturityLevel.ML3, 2,
                    title = "Audit privileged account use",
                    description = "Enable advanced auditing for account logon, account management and sensitive privilege use, and forward to a collector.",
                    technicalDetails = listOf(
                        "GPO: Computer Configuration → Windows Settings → Security Settings → Advanced Audit Policy Configuration → Account Logon = Success/Failure; Account Management = Success/Failure; Privilege Use → Audit Sensitive Privilege Use = Success/Failure"
                    )
                )
            ),
            gapNote = "True time-bound (Just-In-Time) privileged access at scale generally relies on Microsoft Entra Privileged Identity Management or a third-party PAM solution. On-prem JIT can be approximated with scheduled group membership but is not built into AD natively."
        )
    )

    // MARK: - 6. Patch Operating Systems

    val patchOperatingSystems = EssentialControl(
        id = 6,
        name = "Patch Operating Systems",
        icon = materialIconForSfSymbol("gearshape.2"),
        overview = "Apply patches for operating systems within defined timeframes. Use only operating system versions that are still supported by their vendor.",
        ml0Description = "Operating systems are not patched on a regular cycle. Out-of-support OS versions (e.g. Windows 7, Server 2008 R2) remain in production.",
        ml1 = MaturityLevelContent(
            summary = "OS patches applied within one month (within two weeks for internet-facing). Only vendor-supported OS versions in use.",
            steps = listOf(
                step(6, MaturityLevel.ML1, 0,
                    title = "Configure Windows Update for Business",
                    description = "WUfB delivers quality and feature updates straight from Microsoft, configurable via Group Policy.",
                    technicalDetails = listOf(
                        "GPO: Computer Configuration → Administrative Templates → Windows Components → Windows Update → Manage updates offered from Windows Update → Select when Quality Updates are received = Defer 0 days",
                        "GPO: Configure Automatic Updates = Enabled, Auto download and schedule install"
                    )
                ),
                step(6, MaturityLevel.ML1, 1,
                    title = "Enforce a quality-update deadline",
                    description = "A deadline forces a restart after a defined period, so the patch actually takes effect.",
                    technicalDetails = listOf(
                        "GPO: Specify deadlines for automatic updates and restarts → Quality update deadline = 7 days, Grace period = 2 days",
                        "Registry: HKLM\\SOFTWARE\\Policies\\Microsoft\\Windows\\WindowsUpdate → ConfigureDeadlineForQualityUpdates = 7 (DWORD)"
                    )
                ),
                step(6, MaturityLevel.ML1, 2,
                    title = "Inventory OS versions",
                    description = "Identify and replace any out-of-support OS instances.",
                    technicalDetails = listOf(
                        "PowerShell: Get-CimInstance Win32_OperatingSystem | Select Caption, Version, BuildNumber, OSArchitecture",
                        "Across a domain: Invoke-Command -ComputerName (Get-ADComputer -Filter *).Name -ScriptBlock { ... }"
                    )
                )
            ),
            gapNote = null
        ),
        ml2 = MaturityLevelContent(
            summary = "OS patches applied within two weeks (48 hours for internet-facing when an exploit exists). Driver and firmware updates applied within one month.",
            steps = listOf(
                step(6, MaturityLevel.ML2, 0,
                    title = "Tighten deferrals and deadlines",
                    description = "Reduce deferral to zero and deadline to two weeks for workstations; tighter still for internet-facing servers.",
                    technicalDetails = listOf(
                        "GPO: Quality update deadline = 14 days, Grace period = 1 day",
                        "For internet-facing servers (separate OU / WUfB ring): Quality update deadline = 2 days, Grace period = 0"
                    )
                ),
                step(6, MaturityLevel.ML2, 1,
                    title = "Include driver updates from Windows Update",
                    description = "Windows Update can deliver vendor-signed driver and firmware (DCH) updates.",
                    technicalDetails = listOf(
                        "GPO: Do not include drivers with Windows Updates = Disabled",
                        "Registry: HKLM\\SOFTWARE\\Policies\\Microsoft\\Windows\\WindowsUpdate → ExcludeWUDriversInQualityUpdate = 0 (DWORD)"
                    )
                )
            ),
            gapNote = "Weekly authenticated vulnerability scanning of OS is required at ML2 and is not delivered by Windows Update / WUfB alone."
        ),
        ml3 = MaturityLevelContent(
            summary = "Internet-facing OS patches applied within 48 hours when an exploit exists. Workstation and non-internet-facing OS within two weeks. Use of latest or N-1 OS release.",
            steps = listOf(
                step(6, MaturityLevel.ML3, 0,
                    title = "Expedite critical patches",
                    description = "For exploited vulnerabilities, push immediately rather than waiting for the standard ring schedule.",
                    technicalDetails = listOf(
                        "PowerShell: Install-Module PSWindowsUpdate -Force; Get-WindowsUpdate -KBArticleID KB5XXXXXX -Install -AcceptAll -AutoReboot",
                        "GPO: Specify deadlines for automatic updates and restarts → Quality update deadline = 0 days (for emergency-patching OU)"
                    )
                ),
                step(6, MaturityLevel.ML3, 1,
                    title = "Stay on N or N-1 Windows feature releases",
                    description = "Older feature releases reach end-of-servicing and stop receiving security updates. Track and upgrade.",
                    technicalDetails = listOf(
                        "GPO: Select the target Feature Update version = <current N or N-1, e.g. 24H2>",
                        "Registry: HKLM\\SOFTWARE\\Policies\\Microsoft\\Windows\\WindowsUpdate → TargetReleaseVersion = 1, TargetReleaseVersionInfo = 24H2"
                    )
                )
            ),
            gapNote = "Daily authenticated vulnerability scanning required at ML3 is out of scope for Windows built-in tooling."
        )
    )

    // MARK: - 7. Multi-factor Authentication

    val multiFactorAuthentication = EssentialControl(
        id = 7,
        name = "Multi-factor Authentication",
        icon = materialIconForSfSymbol("key.horizontal"),
        overview = "Require more than one factor to authenticate to systems and services. Use phishing-resistant MFA where possible — particularly for privileged accounts and internet-facing services.",
        ml0Description = "Single-factor (password-only) authentication for all systems and services.",
        ml1 = MaturityLevelContent(
            summary = "MFA used by organisation's users (and any third-party users) authenticating to the organisation's internet-facing services. MFA used to authenticate to third-party online services that store sensitive customer data.",
            steps = listOf(
                step(7, MaturityLevel.ML1, 0,
                    title = "Enable Windows Hello for Business",
                    description = "WHfB binds an asymmetric key pair to the TPM, unlocked by PIN or biometric. The factors are 'something you have' (the device/TPM) and 'something you know/are' (PIN/biometric).",
                    technicalDetails = listOf(
                        "GPO: Computer Configuration → Administrative Templates → Windows Components → Windows Hello for Business → Use Windows Hello for Business = Enabled",
                        "GPO: Use a hardware security device = Enabled (forces TPM-backed keys)",
                        "Registry: HKLM\\SOFTWARE\\Policies\\Microsoft\\PassportForWork → Enabled = 1, RequireSecurityDevice = 1"
                    )
                ),
                step(7, MaturityLevel.ML1, 1,
                    title = "Enforce PIN complexity",
                    description = "Set minimum PIN length and complexity so the local factor is meaningful.",
                    technicalDetails = listOf(
                        "GPO: Windows Hello for Business → PIN Complexity → Minimum PIN length = 6, Require digits = Allowed, Require uppercase letters / lowercase letters / special characters = configured per policy",
                        "Registry: HKLM\\SOFTWARE\\Policies\\Microsoft\\PassportForWork\\PINComplexity → MinimumPINLength = 6"
                    )
                ),
                step(7, MaturityLevel.ML1, 2,
                    title = "Enable smart-card support for legacy logon",
                    description = "For internet-facing services that don't speak modern auth, AD CS-issued smart cards provide a second factor.",
                    technicalDetails = listOf(
                        "GPO: Computer Configuration → Windows Settings → Security Settings → Local Policies → Security Options → Interactive logon: Require Windows Hello for Business or smart card = Enabled (where appropriate)"
                    )
                )
            ),
            gapNote = null
        ),
        ml2 = MaturityLevelContent(
            summary = "MFA used by privileged users (other than for accessing the same systems they administer with personal accounts). MFA verifier matches the authenticator (phishing-resistant) for privileged users.",
            steps = listOf(
                step(7, MaturityLevel.ML2, 0,
                    title = "Require MFA for all privileged accounts",
                    description = "Force WHfB or smart-card authentication for any account with admin rights; block password-only sign-in.",
                    technicalDetails = listOf(
                        "GPO: User Configuration → Administrative Templates → System → Logon → Turn off picture password sign-in = Enabled; Turn off convenience PIN sign-in = Enabled (forces WHfB rather than convenience PIN for privileged user OUs)",
                        "Disable interactive logon with password for the privileged user group via fine-grained password policy / SmartCardRequired AD attribute"
                    )
                ),
                step(7, MaturityLevel.ML2, 1,
                    title = "Set SmartcardLogonRequired on admin accounts",
                    description = "AD attribute that forces smart-card / WHfB cert-trust authentication and prevents NTLM password use.",
                    technicalDetails = listOf(
                        "PowerShell: Set-ADUser -Identity <admin> -SmartcardLogonRequired ${'$'}true",
                        "Note: rotates the password to a random value; combine with regular password rotation"
                    )
                )
            ),
            gapNote = null
        ),
        ml3 = MaturityLevelContent(
            summary = "MFA is phishing-resistant for all users. MFA events centrally logged. Successful and unsuccessful MFA events reviewed.",
            steps = listOf(
                step(7, MaturityLevel.ML3, 0,
                    title = "Deploy WHfB with certificate trust",
                    description = "Certificate-trust deployment (vs key-trust) issues a smart-card-style cert from AD CS, which is phishing-resistant and works against on-prem AD.",
                    technicalDetails = listOf(
                        "Requires: AD CS Enterprise CA, Network Device Enrolment Service (NDES) and Microsoft Intune / Configuration Manager (or equivalent) for cert delivery",
                        "GPO: Windows Hello for Business → Use certificate for on-premises authentication = Enabled"
                    )
                ),
                step(7, MaturityLevel.ML3, 1,
                    title = "Enable FIDO2 security key sign-in",
                    description = "FIDO2 security keys are phishing-resistant and work with WHfB / AD CS in hybrid scenarios.",
                    technicalDetails = listOf(
                        "GPO: Computer Configuration → Administrative Templates → System → Logon → Turn on security key sign-in = Enabled",
                        "Registry: HKLM\\SOFTWARE\\Microsoft\\Policies\\PassportForWork\\SecurityKey → UseSecurityKeyForSignin = 1"
                    )
                ),
                step(7, MaturityLevel.ML3, 2,
                    title = "Log and forward authentication events",
                    description = "Windows logs auth events to the Security log; forward them centrally via WEF.",
                    technicalDetails = listOf(
                        "Audit policy: Account Logon → Audit Credential Validation = Success/Failure; Audit Kerberos Authentication Service = Success/Failure",
                        "Key event IDs: 4624 (successful logon), 4625 (failed), 4768 (Kerberos AS-REQ), 4776 (NTLM auth)"
                    )
                )
            ),
            gapNote = "Phishing-resistant MFA for cloud / SaaS services generally requires an identity provider (Entra ID, Okta, etc.) — Windows built-in tooling covers on-prem AD only."
        )
    )

    // MARK: - 8. Regular Backups

    val regularBackups = EssentialControl(
        id = 8,
        name = "Regular Backups",
        icon = materialIconForSfSymbol("externaldrive.badge.checkmark"),
        overview = "Perform and retain backups of important data, software and configuration settings in accordance with business continuity requirements. Test restoration. Protect backups from modification or deletion by unprivileged accounts.",
        ml0Description = "No backups, or backups exist but are untested and accessible by ordinary user accounts.",
        ml1 = MaturityLevelContent(
            summary = "Backups of important data, software and configuration performed and retained per business continuity requirements. Restoration tested when initially implemented and then annually. Unprivileged accounts cannot access backups belonging to other accounts.",
            steps = listOf(
                step(8, MaturityLevel.ML1, 0,
                    title = "Install Windows Server Backup",
                    description = "Built-in image / file backup tool for Windows Server. Free, scriptable, supports VSS-aware applications.",
                    technicalDetails = listOf(
                        "PowerShell: Install-WindowsFeature Windows-Server-Backup -IncludeManagementTools",
                        "Ad-hoc backup: wbadmin start backup -backupTarget:E: -include:C: -allCritical -vssFull -quiet"
                    )
                ),
                step(8, MaturityLevel.ML1, 1,
                    title = "Schedule daily backups",
                    description = "wbadmin can schedule recurring backups; schedule via Task Scheduler for more granular cadence.",
                    technicalDetails = listOf(
                        "Command: wbadmin enable backup -addtarget:\\\\backup\\server1 -include:C: -allCritical -schedule:23:00 -user:CORP\\backupsvc -password:<pwd>",
                        "Task Scheduler: schtasks /create /tn 'Daily Backup' /tr 'wbadmin start backup -backupTarget:E: -include:C: -allCritical -quiet' /sc daily /st 23:00 /ru SYSTEM"
                    )
                ),
                step(8, MaturityLevel.ML1, 2,
                    title = "Restrict access to backup destinations",
                    description = "NTFS / share permissions on the backup target must exclude ordinary users.",
                    technicalDetails = listOf(
                        "icacls E:\\Backups /inheritance:r /grant:r 'SYSTEM:(OI)(CI)F' 'CORP\\Backup Operators:(OI)(CI)F' /remove 'Users' 'Authenticated Users'"
                    )
                ),
                step(8, MaturityLevel.ML1, 3,
                    title = "Enable Volume Shadow Copies for file servers",
                    description = "Provides point-in-time snapshots that users can self-restore via 'Previous Versions'.",
                    technicalDetails = listOf(
                        "vssadmin add shadowstorage /for=D: /on=D: /maxsize=10%",
                        "Schedule: vssadmin create shadow /for=D: via Task Scheduler"
                    )
                )
            ),
            gapNote = null
        ),
        ml2 = MaturityLevelContent(
            summary = "Restoration of backups tested in a disaster-recovery exercise quarterly. Unprivileged accounts cannot modify, delete or access their own backups.",
            steps = listOf(
                step(8, MaturityLevel.ML2, 0,
                    title = "Separate backup credentials",
                    description = "Backups must run under a dedicated service account that is not a domain administrator and is not used interactively.",
                    technicalDetails = listOf(
                        "Create: New-ADUser -Name svc_backup -AccountPassword (Read-Host -AsSecureString) -Enabled ${'$'}true",
                        "Add to Backup Operators on the backup target only, not to Domain Admins",
                        "Deny interactive logon: GPO → User Rights Assignment → Deny log on locally = svc_backup"
                    )
                ),
                step(8, MaturityLevel.ML2, 1,
                    title = "Restrict user access to their own backups",
                    description = "Users should not be able to read, restore, modify or delete their own backups — only authorised restorers should.",
                    technicalDetails = listOf(
                        "Apply: icacls <backup root> /inheritance:r /grant:r 'SYSTEM:(OI)(CI)F' 'CORP\\Restore Admins:(OI)(CI)F'",
                        "Audit access: GPO → Advanced Audit Policy → Object Access → Audit File System = Success/Failure, with SACL on backup folders"
                    )
                )
            ),
            gapNote = null
        ),
        ml3 = MaturityLevelContent(
            summary = "Restoration of backups tested in a disaster-recovery exercise at least annually. Privileged accounts (excluding backup administrators) cannot modify or delete backups. Backups protected from destruction.",
            steps = listOf(
                step(8, MaturityLevel.ML3, 0,
                    title = "Use ReFS with integrity streams for backup volumes",
                    description = "ReFS detects (and with mirror/parity, corrects) silent corruption of backup data.",
                    technicalDetails = listOf(
                        "Format: Format-Volume -DriveLetter E -FileSystem ReFS -AllocationUnitSize 65536",
                        "Enable integrity on the folder: Set-FileIntegrity -FileName E:\\Backups -Enable ${'$'}true -Enforce ${'$'}true"
                    )
                ),
                step(8, MaturityLevel.ML3, 1,
                    title = "Lock down with role separation",
                    description = "Only the backup administrator role (separate from general Domain / Server Admins) holds modify / delete rights on the backup data.",
                    technicalDetails = listOf(
                        "Create dedicated 'Backup Admins' AD group; remove Domain Admins from backup ACLs",
                        "Apply: icacls <backup root> /inheritance:r /grant:r 'SYSTEM:(OI)(CI)F' 'CORP\\Backup Admins:(OI)(CI)F'"
                    )
                ),
                step(8, MaturityLevel.ML3, 2,
                    title = "Offline / air-gapped copy",
                    description = "Retain at least one copy on offline media (e.g. rotated tape or removable disk) disconnected from the network outside backup windows.",
                    technicalDetails = listOf(
                        "Operational control: rotate removable media weekly to offsite storage",
                        "Encrypt removable media: BitLocker To Go via manage-bde -on <drive> -RecoveryPassword"
                    )
                )
            ),
            gapNote = "True immutable / WORM backup storage is not natively provided by Windows Server Backup. Achieving it requires either ReFS + strict role separation (partial), object-storage with object-lock, or a dedicated backup product."
        )
    )
}
