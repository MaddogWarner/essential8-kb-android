# Changelog

All notable changes to this project are documented in this file.

## Unreleased

### Added
- Ported iOS features (v1.2 - v1.4) to the Android Jetpack Compose codebase.
- Added target-scoped maturity level filtering on the home screen.
- Implemented Compliance Dashboard with animated `ComplianceRing` and `ComplianceBarChart` stacked components.
- Upgraded progress persistence layer to support multi-state step states (Implemented, Not Applicable with custom reasons, Not Implemented) serialized via JSON.
- Created `GlobalSearchScreen` matching steps on titles, descriptions, technical commands, or ISM IDs.
- Created `SplashDialog` walkthrough onboarding showcasing new features.
- Added `Reference Only Mode` switch and app data reset button on the About screen.
- Added workstation/server OS scope filtering, scope-aware compliance calculations and step scope badges.
- Added profile-based assessments with per-profile progress, target maturity, OS scope and Microsoft 365 settings.
- Added profile creation, switching, renaming and guarded deletion, plus migration of existing assessment data into a Default profile.
- Added device-level Multiple Profiles and Deep Audit Mode preferences in preparation for audit history support.
- Added cross-platform JSON backup and restore using Android's system document picker, including active-profile export, full-device export, non-destructive profile import and confirmed full-device replacement.
- Added schema-v1 backup migration, schema-v2 validation, a 5 MB safety limit and per-step audit-history caps for untrusted backup files.
- Added R8-minified, resource-shrunk release builds with external upload-signing configuration.
- Replaced the placeholder launcher icon with adaptive cyber dog and padlock artwork, including a themed monochrome layer.

### Changed

- Upgraded the toolchain to Kotlin 2.4.10, AGP 9.3.1, Gradle 9.7.0, Compose BOM 2026.08.00, Lifecycle 2.11.0 and compileSdk 37. `targetSdk` remains 36.

### Fixed

- Prevented the Create Profile dialog from reopening when revisiting the Profiles screen.
- Made the persisted Show Splash on Startup preference take effect before deciding whether to display onboarding.
- Routed status toggles through the shared status mutation path so future audit recording cannot be bypassed.
- Pinned Espresso 3.7.0 for Android 15 and later input compatibility in Compose UI tests.
- Moved Essential Eight step ID aggregation into the data layer so UI code no longer imports a store-layer helper.
- Updated Windows Audit Policy recommendation badges to use Material colour roles that adapt across light and dark themes.
- Isolated Android instrumentation navigation tests from persisted app DataStore state to reduce test flakiness.
- Centralised global DataStore preference keys and restored status changes to the shared active-profile mutation path.
- Removed the empty document left behind by the system document picker when a backup export fails.
- Disabled Android Auto Backup and excluded the assessment DataStore from cloud backup and device-to-device transfer.
- Made the audit-policy search icon mirror correctly in right-to-left layouts.
- Scaled the launcher artwork into the adaptive-icon safe zone so the ears and padlock are no longer clipped by launcher masks.
- Corrected the in-app privacy policy, which still claimed assessment data could leave the device through system backup.
