# e8kbandroid.md — Build Spec: Essential 8 Knowledge Base (Android)

**Authoritative build specification.** Authored by Claude; implemented by Codex. Claude reviews output, runs gates, fixes bugs. Build the steps in order; run each step's **Gate** before moving on. Do not broaden scope beyond this spec.

---

## 1. Goal & scope

Port the shipping iOS SwiftUI app **essential8-kb-ios** to a new **native Android** app with **feature parity**, as a clean, simple, modern Jetpack Compose codebase with **no unnecessary complexity**.

The app is an **offline** ASD Essential Eight quick-reference for Windows system administrators: eight mitigations across maturity levels ML0–ML3 with GPO/registry/PowerShell implementation steps, a Windows audit-policy reference, Microsoft 365 license-tier additions, per-step progress tracking, and an About/Privacy screen. No network, no backend, no accounts.

**iOS source of truth (read directly, port content verbatim):**
`/Users/maddog/Documents/Claudius/xcode/Essential 8 Knowledge Base/`

### Locked decisions
- **Design:** Native **Material 3** — Material You dynamic colour, Material icons, system dark mode. Do **not** mimic iOS chrome.
- **Scope:** Feature parity **plus** explicit copy-to-clipboard buttons on every command / GPO path / registry key / PowerShell snippet. **No search feature.**
- **Application ID:** `com.maddogwarner.essential8kb`
- **App name (label):** `Essential 8 Knowledge Base`
- **Version:** start at `versionName = "1.0"`, `versionCode = 1`.
- **Licence:** MIT, `Copyright (c) 2026 MaddogWarner` (copy from iOS `LICENSE`).
- **Icon:** generate a simple **adaptive-icon placeholder** (shield / "E8" motif) so the app builds; final art swapped later.

---

## 2. Target stack (locked, verified June 2026)

| Concern | Choice |
|---|---|
| Language | **Kotlin 2.2.20** (bundled with AGP 9 — no separate KGP or Compose-compiler version to declare) |
| UI | **Jetpack Compose**, Compose **BOM `2026.06.00`** → Material3 `1.4.0`. Use M3 defaults; expressive components only where they fit. |
| Architecture | Single **`app`** module, plain **MVVM**: Compose UI → lightweight ViewModel exposing `StateFlow<UiState>` → static in-memory Kotlin data |
| Navigation | **No nav library.** Sealed-class screen state + `when`; back handled via `BackHandler` and a small back-stack `List` held in `App.kt` |
| Content storage | **Kotlin `data class` / `enum`** declared in code (compile-time safe). **No JSON / kotlinx.serialization.** |
| Persistence | Jetpack **DataStore (Preferences)** for completed step IDs + M365 license mode |
| Build | Gradle **Kotlin DSL** + version catalog (`gradle/libs.versions.toml`), **AGP 9.1.x**, Gradle 9.1+, JDK 17 |
| SDK | `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26` |
| Theming | `dynamicLight/DarkColorScheme()` on API 31+, hand-defined fallback below; drive dark mode off `isSystemInDarkTheme()` |
| Testing | JUnit4 unit tests + 2–3 Compose UI tests via `createComposeRule()`. No Espresso, no detekt. |
| Lint/format | Built-in Android Lint. Optional **Spotless + ktlint** formatting gate (exempt `Composable`/`Test` from function-naming rule via `.editorconfig`). |

### Explicitly OMIT (over-engineering for static read-only content)
Hilt/DI · Room · repository layer · domain/use-case layer · multi-module · Navigation Compose/Nav3 · kotlinx.serialization · Espresso · detekt.

> If you believe any omission is wrong for a concrete reason, **stop and surface it** — do not silently add a framework.

---

## 3. Feature inventory (parity target)

Read these iOS files for exact behaviour and content. **Technical details (commands, GPO paths, registry keys) must be ported verbatim — do not paraphrase, reword, or "improve" them.**

| iOS file | Port to |
|---|---|
| `EssentialControl.swift`, `Microsoft365LicenseMode.swift` | `data/Models.kt` |
| `EssentialControlsData.swift` (815 lines, 8 controls × ML1–3) | `data/EssentialControlsData.kt` |
| `WindowsAuditPolicyData.swift` (21 entries, 6 categories) | `data/WindowsAuditPolicyData.kt` |
| `Microsoft365AdditionalControlsData.swift` (18 protections) | `data/Microsoft365Data.kt` |
| `AppInformation.swift` (about/privacy copy + reference links) | `data/AppInformation.kt` |
| `ProgressStore.swift` (UserDefaults key `e8kb.stepProgress`) | `store/ProgressStore.kt` (DataStore) |
| `Microsoft365SettingsView.swift` (`@AppStorage("microsoft365LicenseMode")`) | `store/SettingsStore.kt` (DataStore) |
| `HomeView.swift` | `ui/home/HomeScreen.kt` |
| `ControlDetailView.swift` | `ui/detail/ControlDetailScreen.kt` |
| `MaturityLevelView.swift` | `ui/maturity/MaturityLevelScreen.kt` (+ ViewModel) |
| `WindowsAuditPolicyView.swift` | `ui/audit/AuditPolicyScreen.kt` |
| `Microsoft365SettingsView.swift` | `ui/m365/Microsoft365SettingsScreen.kt` |
| `AboutView.swift` | `ui/about/AboutScreen.kt` |

### Data model (Kotlin)
```kotlin
data class EssentialControl(
    val id: Int,                 // 1..8 (= "Mitigation N")
    val name: String,
    val icon: ImageVector,       // map SF Symbol → nearest Material icon
    val overview: String,
    val ml0Description: String,
    val ml1: MaturityLevelContent,
    val ml2: MaturityLevelContent,
    val ml3: MaturityLevelContent,
) { fun content(for level: MaturityLevel): MaturityLevelContent = … }

enum class MaturityLevel(val level: Int) { ML1(1), ML2(2), ML3(3);
    val shortName get() = "ML$level"; val displayName get() = "Maturity Level $level" }

data class MaturityLevelContent(
    val summary: String,
    val steps: List<ImplementationStep>,
    val gapNote: String? = null,
)

data class ImplementationStep(
    val id: String,              // "<controlId>-<mlLevel>-<stepIndex>" — must match iOS scheme
    val title: String,
    val description: String,
    val technicalDetails: List<String>,
)

enum class AuditRecommendation(val label: String) {
    SUCCESS("Success"), FAILURE("Failure"),
    BOTH("Success & Failure"), NOT_RECOMMENDED("Not Recommended") }

data class AuditPolicyEntry(
    val id: String, val category: String, val name: String,
    val description: String, val recommendation: AuditRecommendation,
    val considerations: String, val domainControllerOnly: Boolean,
)

enum class Microsoft365LicenseMode { NONE, E3_P1, E3_P2, E5;
    /* displayName, shortName, description, baseSelection — port from Swift */ }

data class Microsoft365AdditionalProtection(
    val title: String, val coverage: String, val basicSettings: List<String>,
)
```
> **Step ID parity is critical** — saved progress is keyed by step ID. iOS builds IDs via a `step(controlID, level, index, …)` helper as `"\(controlID)-\(level.rawValue)-\(index)"` with **index starting at 0** (first ML1 step of control 1 = `"1-1-0"`). Reproduce this exactly. Persisted M365 mode must use enum raw values matching iOS: `none`, `e3P1`, `e3P2`, `e5` (from `Microsoft365LicenseMode.swift`); validate on read and fall back to `none`.

### Screens
1. **Home** — list of 8 controls (icon + name + "Mitigation N", trailing complete-check when all that control's steps are done) + an **Event Logging** entry (→ Audit Policy) + an **M365 Additional Controls** entry (→ M365 Settings) + **About & Privacy** entry. Use M3 list items / cards with section headers + footers mirroring iOS section copy.
2. **Control Detail** — overview (icon + name + text), ML0 baseline, progress (`LinearProgressIndicator` + "N of X steps complete", "All steps complete" when done), three ML rows (ML1/ML2/ML3) each with summary + per-level "N/X" step count. Title "Mitigation N".
3. **Maturity Level** — header "What ML*n* requires" + progress count; one card per step: completion checkbox (toggles + persists), title, description, then each `technicalDetail` in a **monospaced box with a copy-to-clipboard icon button**. Optional **gap note** (warning-styled). When license mode ≠ NONE, an **M365 additions** section listing applicable protections (title, coverage, settings boxes) + the disclaimer that additions don't replace core steps.
4. **Windows Audit Policy** — overview text, then entries grouped under their 6 categories; each: name, description, a coloured **recommendation badge**, "Domain controllers only" tag when set, considerations footnote.
5. **Microsoft 365 Settings** — purpose text + single-choice selector **None / E3+P1 / E3+P2 / E5** (radio semantics; preserve the iOS E3→P1/P2 nesting visually). Persists; drives conditional M365 content on the Maturity Level screen. Show active-mode summary when ≠ None.
6. **About & Privacy** — purpose, "About Me" + author links, privacy statement + policy link, references list. All links open in the external browser via `Intent.ACTION_VIEW` (no in-app webview). On Android this is a normal screen (with back), not a modal sheet.

### Cross-cutting
- **Progress** persists across restarts (DataStore `Set<String>` of step IDs).
- **M365 license mode** persists (DataStore enum name string).
- **Icons:** map each SF Symbol to the nearest `androidx.compose.material.icons` icon; keep a single mapping table in `data/Models.kt` or the data file. Note exact mappings in `CLAUDE.md`.
- **Accessibility:** content descriptions on icon-only buttons (checkbox, copy), `heading()` semantics on section headers, meaningful labels on the license radios.
- English only. No analytics. No `INTERNET` permission (links use the browser intent, which needs none).

---

## 4. Project structure
```
app/src/main/java/com/maddogwarner/essential8kb/
  MainActivity.kt              // setContent { AppRoot() }
  App.kt                       // sealed Screen state + back stack + BackHandler + AppTheme wrapper
  data/
    Models.kt
    EssentialControlsData.kt
    WindowsAuditPolicyData.kt
    Microsoft365Data.kt
    AppInformation.kt
  store/
    ProgressStore.kt
    SettingsStore.kt
  ui/
    theme/   Color.kt Theme.kt Type.kt
    home/    HomeScreen.kt
    detail/  ControlDetailScreen.kt
    maturity/ MaturityLevelScreen.kt  MaturityLevelViewModel.kt
    audit/   AuditPolicyScreen.kt
    m365/    Microsoft365SettingsScreen.kt
    about/   AboutScreen.kt
    components/ CopyableCommand.kt  RecommendationBadge.kt  SectionHeader.kt
app/src/test/java/com/maddogwarner/essential8kb/          // JUnit unit tests
app/src/androidTest/java/com/maddogwarner/essential8kb/   // Compose UI tests
gradle/libs.versions.toml
```

---

## 5. Build sequence (each step ends with a Gate that must pass)

### Step 1 — Scaffold
Create the Gradle project: root + `app` `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties` (JDK 17, AndroidX, non-transitive R), wrapper for Gradle 9.1+. Configure AGP 9.1.x, Kotlin 2.2.20, SDK 36/36/26, Compose BOM 2026.06.00, Material3, `androidx.activity:activity-compose`, `androidx.lifecycle:lifecycle-viewmodel-compose`, `androidx.datastore:datastore-preferences`, Material icons (extended only if needed). `AndroidManifest.xml` with `MainActivity` (no exported extras, no INTERNET), app label, placeholder **adaptive icon** (`ic_launcher` foreground/background + monochrome). Minimal `MainActivity` + `AppTheme` + empty `AppRoot()`.
**Gate:** `./gradlew assembleDebug` succeeds; app launches to a blank themed screen.

### Step 2 — Data layer
Port `Models.kt` and all four data files **verbatim** from the Swift sources, plus the SF-Symbol→Material icon mapping.
**Gate:** `./gradlew test` — unit test asserts: exactly 8 controls; unique control IDs; every step ID unique and matches the `"<c>-<ml>-<i>"` format (index **0-based**, 67 steps total); all ML summaries and step lists non-empty; 21 audit entries; M365 protections respect mode (NONE empty; E3_P1 ⊂ E3_P2; E5 has E5-specific content) — mirror `Essential_8_Knowledge_BaseTests.swift`.

### Step 3 — Persistence
`ProgressStore` (toggle / isCompleted / completedCount / isControlComplete over a DataStore-backed `Set<String>`) and `SettingsStore` (read/write `Microsoft365LicenseMode`). Expose as `Flow`/`StateFlow`.
**Gate:** `./gradlew test` — instrumented or Robolectric-free unit test round-trips a toggle and a license-mode write/read.

### Step 4 — Core screens
`App.kt` navigation (sealed `Screen`, back stack, `BackHandler`), `HomeScreen`, `ControlDetailScreen`, `MaturityLevelScreen` (+ ViewModel) including copy buttons, progress, and the conditional M365 section. Shared `CopyableCommand` and `SectionHeader` components.
**Gate:** `./gradlew assembleDebug`; Compose UI test — Home renders "Application Control" and "Regular Backups"; tapping a control opens its detail.

### Step 5 — Remaining screens
`AuditPolicyScreen` (+ `RecommendationBadge`), `Microsoft365SettingsScreen` (selector persists + drives M365 section), `AboutScreen` (links via browser intent).
**Gate:** `./gradlew assembleDebug`; Compose UI test — selecting E3 then E5 updates the M365 settings UI; About shows Privacy + References and an ASD link. Mirror `Essential_8_Knowledge_BaseUITests.swift`.

### Step 6 — Polish
Dynamic colour + dark-mode theme, typography, accessibility semantics pass, final lint/format.
**Gate:** `./gradlew test` passes; `./gradlew lint` clean (or only justified, documented warnings); Spotless check passes if enabled.

---

## 6. Repo & security conventions (KB)

Per `secure-coding-standards.md` §5, `github-repo-management.md`, `claude-codex-workflow.md`:
- Working files in this commit: `.gitignore` (Android/Gradle/Studio), `README.md`, `LICENSE` (MIT, as above), `CLAUDE.md` + `AGENTS.md` (each carrying the **KB pointer block** — copy the block format from the iOS repo / INDEX.md), and this `e8kbandroid.md`.
- **Secure by design:** assume hostile input is N/A (no input) but still validate the license-mode string read from DataStore and fall back to `NONE` on anything unexpected; no secrets/PII anywhere; external links restricted to the known reference URLs in `AppInformation`. Fully offline — **no `INTERNET` permission**.
- Surgical scope: every line traces to this spec. Don't add speculative features, settings, or screens.

### Confirmation gates (do NOT do without David's explicit go)
- **Creating / initialising / pushing the `essential8-kb-android` GitHub repo.** Default branch `main`. Add `MaddogWarner/essential8-kb-android — Android port of the Essential 8 quick-reference app` to `Code Projects/reporeview/repos.md` only once created.
- Publishing any artifact / release.

---

## 7. Verification (end-to-end, before sign-off)
1. `./gradlew assembleDebug` builds; app installs and launches on emulators at **API 26** and **API 36**.
2. Manual smoke: browse all 8 controls → each ML; toggle several steps and **restart the app** — completion persists; tap a copy button and confirm the exact command is on the clipboard; switch M365 license mode and confirm additions appear/disappear on the Maturity Level screen; open Windows Audit Policy; open About and confirm a reference link launches the browser.
3. `./gradlew test` (unit) and the Compose UI tests are green; `./gradlew lint` clean.
4. **Content fidelity:** spot-check 2–3 controls' step `technicalDetails` against the Swift source for verbatim accuracy (GPO paths, registry keys, PowerShell). Confirm step-ID scheme matches iOS so progress keys line up.

---

## 8. Acceptance criteria
- Feature parity with essential8-kb-ios + copy-to-clipboard buttons; no search.
- Clean single-module Compose/MVVM codebase; none of the omitted frameworks present.
- All content ported verbatim; 8 controls, ML0–ML3, 67 implementation steps, 21 audit entries, 18 M365 protections, About/Privacy.
- Progress and M365 mode persist via DataStore.
- All gates and the verification checklist pass. Repo not pushed until David approves.
