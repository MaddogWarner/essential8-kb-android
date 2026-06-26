# Essential 8 Knowledge Base for Android

Native Android quick-reference for system administrators implementing the ASD Essential Eight using built-in Windows OS tooling.

## Purpose

Open the app, pick one of the eight mitigations, choose a Maturity Level (ML1, ML2 or ML3), and read the specific configuration changes required to meet that level. The app is designed for offline use on a phone next to an administration console.

The optional Microsoft 365 Additional Controls setting lets administrators select a licence mode so maturity-level pages can show separate Microsoft 365 and Microsoft Defender additions without mixing them into the built-in Windows guidance.

## Scope

- Covers the November 2023 release of the ASD Essential Eight Maturity Model.
- Documents configuration achievable using built-in Windows OS tooling where possible.
- Calls out gaps where a maturity-level requirement cannot be fully met with built-in Windows tooling alone.
- Shows Microsoft 365 additions only when enabled locally.
- Stores progress and Microsoft 365 mode locally with Jetpack DataStore.
- Performs no analytics, network calls, account access, or data collection.

## Building

This project targets:

- Kotlin 2.2.20 bundled with AGP 9.1.x
- Jetpack Compose with Compose BOM `2026.06.00`
- compileSdk 36, targetSdk 36, minSdk 26
- JDK 17

Build and test:

```sh
./gradlew assembleDebug
./gradlew test
./gradlew lint
```

## Verification

Before release, validate:

- The app builds and launches on API 26 and API 36.
- All eight controls and all maturity levels render.
- Step completion persists across restart.
- Copy buttons place the exact displayed command, GPO path, registry key, or PowerShell snippet on the clipboard.
- Microsoft 365 mode persists and controls maturity-level additions.
- About and reference links open in the external browser.
- No `INTERNET` permission is present.

## Disclaimer

The content provided is a reference, not authoritative guidance. Configuration changes can lock users out or break business-critical software. Test in a representative non-production environment first, and validate against the current ASD Essential Eight Maturity Model and Microsoft documentation before applying changes in production.
