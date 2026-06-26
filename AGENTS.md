# AGENTS.md

## Knowledge base
Shared standards live in the KB at `/Users/maddog/Documents/Claudius/Knowlegde base/INDEX.md`.
Read the index first, then load only the page whose `load_when` trigger matches the task
(e.g. coding -> secure-coding-standards.md, PRs -> github-repo-management.md).

## Project
Native Android port of the iOS app `Essential 8 Knowledge Base`, implemented against `e8kbandroid.md`.

The iOS source of truth is:

`/Users/maddog/Documents/Claudius/xcode/Essential 8 Knowledge Base/`

## Implementation Rules
- Use Australian English in docs and user-facing copy unless porting source text verbatim.
- Treat `e8kbandroid.md` as the authoritative build specification.
- Preserve technical content character-for-character from the Swift source for GPO paths, registry keys, PowerShell, CMD, and backup commands.
- Keep the Android app offline. Do not add `INTERNET`, analytics, accounts, telemetry, or backend integrations.
- Do not add Hilt, Room, Navigation Compose/Nav3, kotlinx.serialization, Espresso, detekt, or additional modules.
- Persist completed step IDs and Microsoft 365 mode with Jetpack DataStore only.
- Use the Microsoft 365 DataStore key `microsoft365LicenseMode` and raw values `none`, `e3P1`, `e3P2`, `e5`.
- Keep the Microsoft 365 E3 selector visually nested as E3 -> P1/P2.
- Do not initialise, create, or push the GitHub repository without David's explicit approval.

## Commands
```sh
./gradlew assembleDebug
./gradlew test
./gradlew lint
```

## Verification Expectations
- Unit tests must assert 8 controls, 67 implementation steps, 21 audit entries, and cumulative Microsoft 365 protections.
- Compose UI tests should cover the home screen, control navigation, Microsoft 365 settings, and About/Privacy references.
- Report any unavailable local verification honestly, including missing JDK, Gradle, Android SDK, emulator, or network access.
