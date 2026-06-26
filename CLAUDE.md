# CLAUDE.md — Essential 8 Knowledge Base (Android)

## Knowledge base
Shared standards live in the KB at `/Users/maddog/Documents/Claudius/Knowlegde base/INDEX.md`.
Read the index first, then load only the page whose `load_when` trigger matches the task
(e.g. coding → secure-coding-standards.md, PRs → github-repo-management.md).

---

## Project
Native Android port of the iOS app *Essential 8 Knowledge Base* — an offline ASD Essential Eight quick-reference for Windows sysadmins. Authoritative build spec: `e8kbandroid.md` (same directory). iOS source of truth: `/Users/maddog/Documents/Claudius/xcode/Essential 8 Knowledge Base/`.

---

## Locked decisions
- **App ID:** `com.maddogwarner.essential8kb`
- **Repo name:** `essential8-kb-android`
- **UI:** Material 3, Material You dynamic colour, Material icons — no iOS chrome mimicry
- **Scope:** Feature parity + copy-to-clipboard on every command/GPO/registry/PowerShell snippet; no search
- **Licence:** MIT, `Copyright (c) 2026 MaddogWarner`
- **Icon:** adaptive-icon placeholder (shield / "E8" motif); final art swapped later

---

## Locked stack
| Concern | Choice |
|---|---|
| Language | Kotlin 2.2.20 (bundled with AGP 9 — no separate KGP or Compose-compiler version) |
| UI | Jetpack Compose, BOM `2026.06.00` → Material3 `1.4.0` |
| Architecture | Single `app` module, plain MVVM (Compose UI → ViewModel → static in-memory Kotlin data) |
| Navigation | No nav library — sealed-class screen state + `when`; `BackHandler` + small back-stack `List` in `App.kt` |
| Content | Kotlin `data class` / `enum` in code; no JSON, no kotlinx.serialization |
| Persistence | Jetpack DataStore (Preferences) only |
| Build | Gradle Kotlin DSL + version catalog, AGP 9.1.x, Gradle 9.1+, JDK 17 |
| SDK | compileSdk 36 / targetSdk 36 / minSdk 26 |
| Omit | Hilt · Room · repository layer · Nav Compose/Nav3 · kotlinx.serialization · Espresso · detekt |

---

## Codex handoff workflow
- **Claude** owns plan, architecture, content-parity decisions, and review.
- **Codex** implements against `e8kbandroid.md` step by step; each step ends with a Gate that must pass.
- Do not redo Codex's work — review and redirect. Flag plan deviations clearly.
- **Creating / pushing the `essential8-kb-android` GitHub repo is gated on David's explicit approval.** Do not do it unilaterally.

---

## Watch-outs (findings from spec review)

### Content counts — verified against iOS source (spec corrected 26/06/2026)
An early review draft over-counted. The verified figures (now in `e8kbandroid.md`) are **67 steps, 21 audit entries, 18 M365 protections**:
- **Audit entries:** 21 — `grep -c "AuditPolicyEntry(" WindowsAuditPolicyData.swift`.
- **M365 protections:** 18 = 8 (E3P1) + 2 (P2 identity, controls 5 & 7 only) + 8 (E5).
- **Steps:** 67 — `step(...)` helper calls in `EssentialControlsData.swift`.
Write the Step 2 unit tests to assert these exact figures.

### Step ID scheme — 0-based index, verify before writing tests
iOS generates IDs as `"\(controlID)-\(level.rawValue)-\(index)"` where `level.rawValue` is 1/2/3 and `index` starts at **0**. Example: first step of Control 1 ML1 = `"1-1-0"`. The spec correctly states this; just confirm the Kotlin data file reproduces it exactly before writing the ID-uniqueness test.

### SF Symbol → Material icon mapping
The spec defers the mapping to Codex / a table in `data/Models.kt`. The 8 SF Symbols to map are:
- `checkmark.shield` → `Icons.Outlined.Security` (or `Shield`)
- `arrow.down.app` → `Icons.Outlined.SystemUpdate` (or `Download`)
- `doc.text.magnifyingglass` → `Icons.Outlined.ManageSearch`
- `lock.shield` → `Icons.Outlined.Lock` (or `AdminPanelSettings`)
- `person.badge.key` → `Icons.Outlined.ManageAccounts` (or `Key`)
- `gearshape.2` → `Icons.Outlined.Settings` (or `SettingsSuggest`)
- `key.horizontal` → `Icons.Outlined.Key` (or `VpnKey`)
- `externaldrive.badge.checkmark` → `Icons.Outlined.Backup`

Final Android mapping implemented in `data/Models.kt`:
- `checkmark.shield` → `Icons.Outlined.Security`
- `arrow.down.app` → `Icons.Outlined.SystemUpdate`
- `doc.text.magnifyingglass` → `Icons.Outlined.ManageSearch`
- `lock.shield` → `Icons.Outlined.Lock`
- `person.badge.key` → `Icons.Outlined.ManageAccounts`
- `gearshape.2` → `Icons.Outlined.Settings`
- `key.horizontal` → `Icons.Outlined.Key`
- `externaldrive.badge.checkmark` → `Icons.Outlined.Backup`

Compile-time validation of these imports is still pending until JDK 17 + Gradle/Android SDK tooling is available in this checkout.

### M365 license-mode storage key
iOS stores the raw enum string under `@AppStorage("microsoft365LicenseMode")`. The Android DataStore key must use the same string key and store the same enum raw values (`none`, `e3P1`, `e3P2`, `e5`) so future cross-platform parity is preserved. Validate the read: fall back to `NONE` on any unexpected string.

### M365 settings screen — nested E3 sub-selection
The iOS UI shows a two-level selector: top-level None / E3 / E5, then when E3 is selected a nested P1/P2 sub-option expands beneath it (indented, 28pt leading). The spec describes this correctly ("preserve the iOS E3→P1/P2 nesting visually"). Codex must not flatten it to a plain 4-radio list — that was flagged as a defect in the sister web port.

### Technical detail content — port verbatim
GPO paths, registry keys, PowerShell commands, and `wbadmin`/`icacls` commands must be copied character-for-character from the Swift source. The spec says this; enforce it in review. Spot-check at least 2–3 controls during the Step 2 gate review.

### M365 additions are cumulative, not flat
`Microsoft365AdditionalControlsData.protections(for:level:licenseMode:)` returns e3P1 base + p2 identity (if e3P2 or e5) + e5 (if e5). The unit test for M365 must verify this accumulation: `E3_P1 ⊂ E3_P2 ⊂ E5` (by checking subset counts), not just that non-empty results are returned.

---

## Content counts (canonical — verified against iOS source)
- Controls: 8
- Steps total: 67 (ML1: 27, ML2: 20, ML3: 20 across all controls)
- Audit entries: 21 across 6 categories
- M365 unique protections: 18 (E3P1: 8, P2-identity add-ons: 2, E5 add-ons: 8)
