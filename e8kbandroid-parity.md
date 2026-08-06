# e8kbandroid-parity.md — iOS 1.4 → 1.8 Parity Spec

**Audience:** Codex. **Owner of plan/review:** Claude.
**Companion to:** `e8kbandroid.md` (the original port spec — still authoritative for everything already built).

---

## 0. Context

The Android app is a faithful port of **iOS v1.4**. iOS is now at **v1.8 (build 9)**. This spec closes the gap.

**iOS source of truth:** `/Users/maddog/Documents/Claudius/xcode/Essential 8 Knowledge Base/Essential 8 Knowledge Base/`
Referred to below as `<iOS>/`. Every behavioural question is settled by reading the Swift, not by inference.

**Already at parity — do not touch:** 8 controls, 67 steps, 21 audit-policy entries, 18 M365 protections, Global Search, splash walkthrough, compliance dashboard, ISM capsules, Reference Only Mode, N/A + reason, copy-to-clipboard.

### Locked decisions carried forward from `e8kbandroid.md`
Unchanged: app ID, Material 3 / Material You, no nav library (sealed `Screen` + back-stack `List` in `App.kt`), Kotlin data classes for content, DataStore Preferences only, no Hilt/Room/repository layer.

### New locked decisions for this phase
1. **Persistence stays DataStore Preferences.** Profiles serialise to a single JSON string under one key, exactly as `stepProgress` already does via `org.json`. Do **not** introduce Room or kotlinx.serialization.
2. **`org.json` only.** The existing `ProgressStore` uses `org.json.JSONObject`. Backup encode/decode uses the same. No new serialisation dependency.
3. **ATT&CK content is ported verbatim.** Technique IDs, names, tactics, relationships and notes are copied character-for-character from the Swift. Do not re-derive, re-word, or "improve" any mapping note.
4. **`ATTACKRelationship.SUPPORT` does not count toward coverage.** This mirrors iOS locked decision 5. See `countsTowardCoverage`.
5. **Per-profile vs global settings is a hard boundary.** Target maturity level, OS scope and M365 licence mode are **per-profile**. Splash, Reference Only, Deep Audit and Multiple Profiles are **global/device-level**. This is currently wrong on Android and Step 2 fixes it.
6. **Australian English and `dd/MM/yyyy` (or `dd/MM/yyyy HH:mm`) throughout**, matching the iOS `Locale(identifier: "en_AU")` formatters.

7. **Android ships as version 1.** `versionCode 1`, `versionName "1.0"`. Android is a new product on its own store listing; it does not inherit the iOS version number. Feature parity with iOS 1.8 does not imply version parity.
8. **`android:allowBackup="false"`.** Assessment data — including audit trails — must not ride Android Auto Backup to Google's servers. This is a compliance tool whose privacy policy states it transmits nothing; Auto Backup would contradict that. Users move data with the app's own export (Step 4).

### Ground rule — Step 0 is DONE

**The toolchain is installed and the baseline is green.** JDK 17.0.20, Gradle 9.6.1, cmdline-tools 19.0, SDK 36. `./gradlew :app:compileDebugKotlin` and `./gradlew test` both pass — **19/19 unit tests green**. Do not redo this.

Three things were fixed to get there; they are already in the working tree:

1. `ui/search/GlobalSearchScreen.kt:149` — `SectionHeader(title = …)` → `text = …`. The only compile error in the whole codebase.
2. `gradle/libs.versions.toml` + `app/build.gradle.kts` — added `org.json:json:20250517` as a `testImplementation`. **Why this matters:** AGP's mockable `android.jar` stubs `org.json`, so every `JSONObject` call in `ProgressStore` threw at test time, was swallowed by the `catch`, and then died on the *also*-stubbed `android.util.Log.e`. The visible failure was "Log not mocked"; the real cause was JSON. A real `org.json` on the test classpath fixes both. **Do not "fix" this with `unitTests.isReturnDefaultValues = true`** — that silences unmocked calls everywhere and would let the JSON tests pass while asserting nothing.
3. `ProgressStoreTest.kt:126-127` — the expectation was wrong, not the code. The comment claimed Control 1 ML3 has "2 ML3" steps; it has 3 (verified against `<iOS>/EssentialControlsData.swift`: ML1 4 + ML2 3 + ML3 3 = 10 cumulative). Corrected to `3 / (10 - 1) = 33.333%`.

**Standing warning from that last one:** a green test suite is not the same as a correct one. Test expectations in this repo have never been validated against a running build. When a new test disagrees with the data, check the Swift before assuming the code is wrong.

`src/androidTest` still has never been compiled — it needs a device or emulator. Expect a wave of errors there the first time it runs.

### On-device verification (06/08/2026, Pixel 7 / Android 16 / API 36)

The debug build was installed and driven on real hardware. **No crashes, no ANRs, zero app-originated logcat errors.** Confirmed working: splash walkthrough, home dashboard (ring, summary, per-control bars, ML picker), control detail, ML step screens with ISM capsules and copy buttons, three-state status menu, N/A reason dialog, Global Search (matched `HKLM` across controls), Windows Audit Policy, About, and **the nested M365 E3→P1/P2 selector** — the defect flagged in the sister web port is *not* present here. Progress survived a force-stop, so the DataStore round-trip is sound.

Two content facts confirmed against the running app: the home dashboard reports **67** in-scope steps, and Application Control reports **10** cumulative steps at ML3.

**Not covered, and still unverified:** light theme, dynamic colour, landscape, tablet/foldable sizes, TalkBack, controls 2–8 in depth, Reference Only Mode, Reset App Data, and the entire `src/androidTest` set. Do not treat the above as a substitute for the per-step gates.

---

## Step 1 — OS scope on the data model

Adds Workstation/Server scoping. This is first because Steps 2 and 4 both depend on `steps(upTo:scope:)`.

### 1.1 Model
In `data/Models.kt`:

```kotlin
/** Where an implementation step applies. BOTH is the safe default — only tag
 *  WORKSTATION or SERVER when the content is unambiguous. */
enum class OSScope(val rawValue: String) {
    WORKSTATION("workstation"), SERVER("server"), BOTH("both");
    companion object { fun fromRawValue(v: String?): OSScope = entries.firstOrNull { it.rawValue == v } ?: BOTH }
}
```

Add to `ImplementationStep`: `val osScope: OSScope = OSScope.BOTH` — defaulted, so the 47 untagged steps need no edit.

Add `fun ImplementationStep.matches(filter: OSScope): Boolean = filter == OSScope.BOTH || osScope == OSScope.BOTH || osScope == filter`
and `fun EssentialControl.steps(upTo: MaturityLevel, scope: OSScope): List<ImplementationStep>`.
Keep the existing single-arg `steps(upTo:)` — ATT&CK detail and search still use it.

### 1.2 Content
Port the `osScope:` tags from `<iOS>/EssentialControlsData.swift`. There are exactly **20** tagged steps: **16 `.workstation`, 2 `.server`**, and 2 further occurrences that are the parameter declaration/default in `EssentialControl.swift` — verify by grep before you start, and confirm your Kotlin has 16 WORKSTATION and 2 SERVER.

### 1.3 UI
- **OS Scope segmented picker** in the About screen "Preferences" section, above the Reference Only Mode switch. Three segments: Workstation / Server / Both. Footer text verbatim from `<iOS>/AboutView.swift` ("OS scope hides implementation steps that don't apply…").
- **Scope badge on steps:** in `MaturityLevelScreen`, when `step.osScope != BOTH`, show a "Workstation" or "Server" capsule. See `<iOS>/MaturityLevelView.swift:112`.
- Every compliance calculation, the home-screen dashboard, and control-complete ticks now filter by scope.

### 1.4 Two small fixes to land in the same step

**a. Deprecated icon — RTL correctness, not cosmetics.** Replace `Icons.Outlined.ManageSearch` with `Icons.AutoMirrored.Outlined.ManageSearch` at `data/Models.kt:24` and `ui/home/HomeScreen.kt:186`. The manifest sets `supportsRtl="true"`, and a non-mirrored magnifier renders backwards in RTL locales. This is the `doc.text.magnifyingglass` mapping recorded in `CLAUDE.md` — update that mapping table too so the two don't drift.

**b. Privacy statement — port the corrected text.** `data/AppInformation.kt` inherits a privacy string from iOS that is factually wrong: it claims the app does not "record, store, transmit" data, but the app stores assessments locally and (on iOS) syncs them via iCloud backup. Corrected wording, agreed 06/08/2026 and landed on the iOS branch `fix/privacy-statement-accuracy` for v1.9:

> Essential 8 Knowledge Base has no accounts, no analytics, and makes no network requests of its own — nothing you enter is ever sent to the developer or any third party. Your assessment progress, notes and audit history are stored only on your device. That data leaves your device only if you export a backup yourself, or through your device's own system backup. External reference links open in your browser. The app does not request access to the microphone, camera, location services, contacts, photos, or other device sensors.

The phrase "your device's own system backup" is deliberately platform-neutral — Android sets `allowBackup="false"` (locked decision 8) while iOS retains iCloud backup, and the sentence must stay true on both. **Do not reword it.** It also feeds the Play Data Safety declaration in Step 6, which must not contradict it.

**Gate 1:** Unit tests assert — 16 WORKSTATION + 2 SERVER + 49 BOTH = 67; `matches()` truth table for all 9 combinations; `steps(upTo=ML3, scope=WORKSTATION)` excludes exactly the 2 server steps; compliance percentage recalculates over the filtered set. Screenshot the About picker and a scoped step badge. Confirm the privacy text renders in full on the About screen without truncation.

**Gate 1 verification (06/08/2026, Pixel 7 / Android 16 / API 36):** The scope counts, truth table and filtered compliance tests pass. The About picker and a Workstation step badge were captured in `/private/tmp/e8kb-gate2-evidence/`. The corrected privacy statement was verified character-for-character in the running app's UI hierarchy and is protected by an exact-string unit test.

---

## Step 2 — Profiles (persistence rewrite)

The largest structural change. `ProgressStore` goes from a flat status map to a list of profiles with one active.

### 2.1 Model
Mirror `<iOS>/ProgressStore.swift` exactly:

```kotlin
data class AuditEntry(val id: String, val timestamp: Long, val previousState: StepState, val newState: StepState, val note: String?)

data class Profile(
    val id: String,                     // UUID string
    val name: String,
    val createdAt: Long,
    val stepProgress: Map<String, StepStatus>,
    val auditTrail: Map<String, List<AuditEntry>>,
    val targetMaturityLevelRaw: Int,    // default 3
    val osScopeFilterRaw: String,       // default "both"
    val microsoft365LicenseModeRaw: String, // default "none"
)
```
`AUDIT_ENTRIES_PER_STEP_CAP = 200`.

`timestamp` is epoch millis in Kotlin but **must serialise to ISO-8601 in backup JSON** (Step 3) — iOS uses `.iso8601`. Keep that conversion at the backup boundary only.

### 2.2 Store
DataStore keys, matching iOS `UserDefaults` names for cross-platform parity:
- `e8kb.profiles` — JSON array of profiles (string preference)
- `e8kb.activeProfileID` — string
- Global booleans: `showSplashOnStartup`, `referenceOnlyMode`, `deepAuditEnabled`, `multiProfileEnabled`

Operations: `switchProfile`, `createProfile(name)` (trim; empty → "New Profile"), `renameProfile` (trim; reject empty), `deleteProfile` (**refuse when only one profile remains**; if the active one is deleted, activate index 0), `resetAll`.

**Migration — get this right, it's the only irreversible part.** On first load with no `e8kb.profiles`, build a single "Default" profile from the existing `e8kb.stepProgressDict`, falling back to the legacy `e8kb.stepProgress` string-set, and hydrate its three per-profile settings from the existing global `targetMaturityLevel` / `osScopeFilter` / `microsoft365LicenseMode` keys. Existing users must not lose progress. Mirrors `<iOS>/ProgressStore.swift` `init`.

### 2.3 Per-profile settings move
`SettingsStore` currently owns `targetMaturityLevel` and `licenseMode` as global keys. Move both onto the active profile. `SettingsStore` retains only the four global booleans. Every reader updates accordingly.

### 2.4 UI
- About → "Assessment Features" section: **Multiple Profiles** and **Deep Audit Mode** switches, with the footer text verbatim from `<iOS>/AboutView.swift:52`.
- When Multiple Profiles is on, a **Profiles** row appears (subtitle = active profile name) navigating to a new `Screen.Profiles`.
- `ProfilesScreen` — port `<iOS>/ProfilesView.swift`. List of profiles with created date (`dd/MM/yyyy`) and a tick on the active one; tap to switch and pop back; **+** in the app bar to create. iOS uses swipe actions for rename/delete — on Android use an overflow menu per row (idiomatic Material 3; do not mimic iOS swipe chrome). Delete is confirmed by dialog with the verbatim warning: "Delete profile '<name>'? Its progress and audit history are permanently removed." Hide delete when only one profile exists.

**Gate 2:** Unit tests — migration from legacy string-set produces one Default profile with correct statuses; migration from `stepProgressDict` likewise; migration carries the three global settings onto the profile; create/rename/delete/switch behave, including the refuse-last-delete rule and active-reassignment; profile switch changes which statuses are visible; `resetAll` clears profiles *and* the four global booleans. Manually verify an app installed at the current build upgrades without data loss.

**Gate 2 verification (06/08/2026, Pixel 7 / Android 16 / API 36):** Ran `connectedDebugAndroidTest` first; all 8 device UI tests passed and the test deployment was removed on completion. The subsequent explicit uninstall returned `DELETE_FAILED_INTERNAL_ERROR` because the package was already absent; a clean state was confirmed by the absence of the app DataStore. Installed `df24f3a`, marked legacy step `1-1-0` complete, force-stopped the app and confirmed `e8kb.stepProgress` contained `1-1-0`. Installed the current APK with `adb install -r` and no intervening uninstall. The migrated DataStore retained the legacy key and added a Default profile whose `stepProgress["1-1-0"].state` was `Implemented`; the running UI showed Application Control at 1/4 with the same step exposed as `Implemented`. This device exercise covers the legacy StringSet path only because `df24f3a` never wrote `e8kb.stepProgressDict`; both formats remain covered against real DataStore files by unit tests. There is no released Android install base carrying legacy data.

---

## Step 3 — Deep Audit Mode

### 3.1 Recording
Extend `setStatus` to `setStatus(state, reason, note, stepId)`. When `deepAuditEnabled` is true **and the state actually changed**, append an `AuditEntry` to the active profile's `auditTrail[stepId]`, then trim from the front to the 200 cap.

Note precedence, exactly as iOS: `effectiveNote = if (state == NOT_APPLICABLE) (trimmedNote ?: reason) else trimmedNote`; blank → null.

`auditEntries(stepId)` returns the list **reversed** (newest first).

### 3.2 UI
- When Deep Audit is on, changing a step's status to Implemented or Not Implemented prompts for an optional audit note (iOS: `<iOS>/MaturityLevelView.swift:252`). The N/A dialog already exists — feed its reason through as the note.
- A **history affordance** on each step that has entries, opening `Screen.StepAuditHistory(stepTitle, entries)`.
- `StepAuditHistoryScreen` — port `<iOS>/StepAuditHistoryView.swift`. Each row: "`Previous → New`" (semibold), timestamp `dd/MM/yyyy HH:mm`, note if present. Port the accessibility label verbatim: "`<date>, changed from <prev> to <new>, note: <note or "no note">`".

**Gate 3:** Unit tests — no entry written when Deep Audit is off; no entry when the state is unchanged; the 200 cap trims oldest-first; N/A reason falls through to the note when no explicit note; `auditEntries` is newest-first. Audit trail survives a profile switch and round-trips through backup.

---

## Step 4 — Backup & Restore

Port `<iOS>/BackupFile.swift`. **Cross-platform compatible: an iOS backup must import on Android and vice versa.** Match the JSON field names and ISO-8601 dates exactly.

- `schemaVersion` **2** current, **1** minimum supported, **5,242,880** byte cap.
- Encode caps each profile's audit trail to the last 200 entries per step before writing.
- Decode: read the envelope's `schemaVersion` first; `< 1` → invalid; `> 2` → "unsupported schema"; `== 1` → migrate the legacy single-profile shape into one profile named "Imported" (see `LegacyBackupFile` / `LegacyBackupSettings`). Reject empty profile lists and duplicate profile IDs.
- Three error cases with the verbatim iOS messages: file too large, unsupported schema, invalid file.
- **Legacy settings must be validated on read** — unknown licence/maturity/scope strings fall back to defaults rather than being trusted. Assume a hostile file: it's user-supplied input from an untrusted path.
- `exportActiveProfile` (no global settings) / `exportAllProfiles` (with `GlobalSettingsBackup`).
- `importAsNewProfile` — assigns **fresh** IDs, suffixes " (imported)" on name collision, activates the first imported profile, force-enables `multiProfileEnabled` when >1 profile results.
- `importFullDevice` — replaces everything, applies the global settings envelope.

### Android specifics
Use the Storage Access Framework — `ActivityResultContracts.CreateDocument("application/json")` for export and `OpenDocument` for import. No `WRITE_EXTERNAL_STORAGE` permission, no legacy file paths, no `FileProvider` needed.

About screen "Backup & Restore" section, footer verbatim from `<iOS>/AboutView.swift`. Export shows a "This profile only / All profiles" chooser **only when >1 profile exists**, otherwise exports directly. Import shows a confirmation dialog, offering Replace (destructive) vs Import-as-new. Errors surface in a dialog.

**Gate 4:** Unit tests — v2 round-trip is lossless (statuses, N/A reasons, audit trail, all three per-profile settings); a v1 file imports as one "Imported" profile; schemaVersion 3 throws unsupported; a >5 MB payload throws file-too-large; malformed JSON throws invalid-file rather than crashing; duplicate profile IDs are rejected; the 200-cap applies on encode; unknown enum strings in a v1 file fall back to defaults. **Then: export from the iOS app, import on Android, and confirm the assessment matches.** That cross-platform check is the real gate.

---

## Step 5 — MITRE ATT&CK

The largest content addition. Seven Swift files, ~790 lines.

### 5.1 Content — port verbatim
| Kotlin file | Swift source | Content |
|---|---|---|
| `data/attack/ATTACKTechnique.kt` | `ATTACKTechnique.swift` | `ATTACKTactic` (10, **`allCases` order is display order** — preserve it), `ATTACKRelationship` (4, with `countsTowardCoverage = this != SUPPORT`), `ATTACKTechnique`, `ATTACKMapping` |
| `data/attack/ATTACKCatalogue.kt` | `ATTACKCatalogue.swift` | **35 techniques**, Enterprise **v19.1**, released **28 April 2026**. Keep `attackVersion`, `attackVersionReleased`, `attackCopyrightYear` and the source-comment header. |
| `data/attack/ATTACKMappingData.kt` | `ATTACKMappingData.swift` | **77 helper calls** flattening to the full mapping list, across all 8 controls. Port every note verbatim, **including the `// 2-1-2 deliberately unmapped` comment.** |
| `data/attack/ATTACKCoverage.kt` | `ATTACKCoverage.swift` | `TechniqueCoverage`, `CoverageStatus`, `ATTACKCoverageCalculator` |

The mapping rationale lives in `/Users/maddog/Documents/Claudius/xcode/Essential 8 Knowledge Base/mitreattacke8.md` — read it before porting, but the Swift is the source of truth for the data.

**The `.flatMap { $0 }` in the Swift is a Swift type-system workaround.** In Kotlin, write it idiomatically (`buildList` or `listOf(...).flatten()`) — the *data* is verbatim, the *plumbing* is Kotlin-idiomatic. Same principle everywhere in this spec.

### 5.2 Calculator
Port `ATTACKCoverageCalculator` faithfully — the subtleties matter:
- `inScopeStepIDs` is computed **once** per pass, not per technique (iOS notes this as a deliberate performance decision).
- Techniques with no in-scope mappings return null and are **not listed**.
- `contributingIDs` = steps whose relationships include any non-SUPPORT relationship. `supportingIDs` = steps with SUPPORT.
- N/A steps are excluded from both totals.
- `isIndirectOnly` = contributing set empty. `isNotApplicable` = contributing set non-empty but all its steps are N/A.
- `status` precedence: indirect → notApplicable → covered → partial → notCovered.
- A technique in several tactics appears under **each** group but is **counted once** in the summary.

### 5.3 UI
- `ui/attack/ATTACKCoverageScreen.kt` ← `ATTACKCoverageView.swift`. Summary row (Covered green / Partial orange / Not covered neutral), the "Measured against your target of ML*n*, *scope* scope." line, the untallied-techniques sentence with its singular/plural handling, then one section per tactic titled "`<Tactic> (<count>)`". Port the caveat and footer text verbatim.
- `ui/attack/ATTACKTechniqueDetailScreen.kt` ← `ATTACKTechniqueDetailView.swift`. Header with monospaced ID, name, tactic chips; then one section per relationship (Prevent / Detect / Recover / Support) listing mapped steps with status icon, control name, ML badge, "Not in your current scope" where applicable, and the mapping note; then the disclaimer and an external link to `attack.mitre.org`.
- Step-level **ATT&CK capsules** in `MaturityLevelScreen` (`<iOS>/MaturityLevelView.swift:333`), tapping through to technique detail. Place alongside the existing ISM capsules.
- Home screen: a "Threat Coverage" section with a "MITRE ATT&CK® Coverage" row — **hidden when Reference Only Mode is on**, matching iOS.
- About screen: a "MITRE ATT&CK®" section with disclaimer, coverage caveat, version note and attribution. Add the corresponding constants to `data/AppInformation.kt` from `<iOS>/AppInformation.swift`.
- **Global Search:** make ATT&CK technique IDs searchable, and update the placeholder to match iOS (`<iOS>/GlobalSearchView.swift:223`) — "Search GPOs, registries, commands, ISM or ATT&CK IDs…". Android currently omits ATT&CK from both. Searching `T1059` should return the steps mapped to it.

**Attribution is a legal requirement, not a nicety.** ATT&CK® is a registered trademark of The MITRE Corporation. Carry the `attackAttribution`, `attackDisclaimer`, `attackDisclaimerShort` and `attackCoverageCaveat` strings across verbatim and render them everywhere iOS does.

Colour: iOS uses purple for ATT&CK IDs and tactic chips. Map to a Material 3 role (`tertiary`) rather than a hardcoded hex, so it works in light, dark and dynamic colour.

**Gate 5:** Unit tests — catalogue has exactly 35 techniques with unique IDs; every `techniqueID` in the mapping data resolves to a catalogue entry (**no orphans**); every `stepID` in the mapping data resolves to a real step ID (**catches typos in the 0-based index scheme**); `countsTowardCoverage` excludes only SUPPORT; a technique mapped solely via SUPPORT reports `indirect`; a technique whose contributing steps are all N/A reports `notApplicable`; marking all contributing steps implemented yields `covered`; the summary counts a multi-tactic technique once; changing target ML or OS scope changes which techniques are listed. Screenshot the coverage screen and a technique detail.

---

## Step 6 — Release readiness

Not parity work, but the app cannot ship without it.

1. **`buildTypes` block** in `app/build.gradle.kts` — none currently exists. Add `release` with `isMinifyEnabled = true`, `isShrinkResources = true`, and `proguard-rules.pro`. Verify Compose and DataStore survive R8; add keep rules only where a real failure proves they're needed.
2. **Signing.** Generate an upload keystore. Credentials come from `~/.gradle/gradle.properties` or environment variables — **never** committed, never in `local.properties`, never hardcoded. Add the keystore to `.gitignore`.
3. **Versioning — decided.** Leave `versionCode 1` / `versionName "1.0"` as they are. Android ships as version 1 regardless of iOS being at 1.8. No change needed; do not "sync" the two.
4. **Launcher icon** — replace the placeholder adaptive icon with final art. Confirm the monochrome layer renders correctly under themed icons.
5. **Manifest — decided.** Set `android:allowBackup="false"`. Then review `res/xml/backup_rules.xml` and `res/xml/data_extraction_rules.xml`: with Auto Backup off, the former is inert, but the latter still governs device-to-device transfer, so exclude the DataStore file there too. Confirm the app's own export (Step 4) is the only path data leaves the device.
6. **Privacy policy page — SHIP BLOCKER, needs work.** `https://maddogwarner.com/privacy/essential-8-knowledge-base/` was corrected and republished on 06/08/2026, but it is **explicitly scoped to the iOS app** ("applies only to the Essential 8 Knowledge Base iOS app") and names iCloud backup directly. Google Play requires a privacy policy URL that covers *this* app. Either add a second page for Android or rewrite the existing one to cover both — in which case swap the iCloud sentence for the platform-neutral "your device's own system backup" phrasing and state that the Android app is excluded from Android Auto Backup. Source: `Code Projects/maddogwarnercom/src/pages/privacy/essential-8-knowledge-base.astro` (Astro; a push to `main` deploys to production).
7. **Play Console:** Data Safety declaration, content rating, feature graphic, phone and tablet screenshots. The Data Safety answers must match the privacy page — the app stores data locally and does not transmit it; with `allowBackup="false"` it is not backed up to Google either. Do not tick anything implying collection or transmission.

**Gate 6:** `./gradlew bundleRelease` produces a signed AAB. Install the release build on a physical device and smoke-test every screen — R8 breakage typically surfaces only in the release variant. Verify the AAB with `bundletool`.

---

## Review checkpoints (Claude)

Stop and request review at Gates **2**, **4** and **5**. Those three carry the risk:
- **Gate 2** — migration is irreversible; a mistake destroys existing users' progress.
- **Gate 4** — backup is a security boundary parsing untrusted input, and a cross-platform contract.
- **Gate 5** — 35 techniques and 77 mappings of verbatim content, plus trademark attribution.

Gates 1, 3 and 6 can proceed without blocking, but report results.

## Deliberately not changing

**Global Search does not autofocus its text field.** Reviewed on device 06/08/2026 and left alone: iOS uses `.searchable`, which also does not autofocus, so Android is already at parity. Adding a `FocusRequester` here would break that parity for a marginal gain. If it proves annoying in daily use, change both platforms together — not this one alone.

## Standing rules

- **Verbatim means verbatim.** GPO paths, registry keys, PowerShell, ATT&CK names and mapping notes are copied character-for-character. If something looks wrong in the Swift, flag it — do not silently correct it.
- **Idiomatic Kotlin, identical behaviour.** Swift workarounds do not get ported as workarounds; Swift *decisions* do.
- **Surgical changes.** Touch only what a step requires. No speculative abstractions, no adjacent tidying.
- **Australian English** in all user-facing strings.
- Update `CHANGELOG.md` per step. Do not push to `origin` without David's explicit approval.
- If a step turns out to be blocked, finish everything else in it and say plainly what was left and why.
