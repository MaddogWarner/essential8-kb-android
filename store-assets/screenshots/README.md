# Play Store screenshots

Captured from the R8-minified release build on a Pixel 7 (Android 16, 1080x2400)
with SystemUI demo mode enabled, so the status bar shows a fixed 9:30 clock, full
battery and no notification icons.

| File | Screen |
|---|---|
| `01-home-dashboard.png` | Home — target maturity, compliance ring, per-control breakdown |
| `02-step-detail.png` | ML1 steps — deny paths, copy buttons, ISM and ATT&CK capsules |
| `03-attack-coverage.png` | MITRE ATT&CK coverage — summary and tactic grouping |
| `04-global-search.png` | Global Search — highlighted GPO and ATT&CK matches |
| `05-m365-additions.png` | Microsoft 365 licence mode with the E3 -> P1/P2 nesting |
| `06-audit-policy.png` | Windows Audit Policy recommendations |

## Which set to upload

- `native/` — untouched 1080x2400 captures (20:9).
- `play-9x16/` — the same images scaled to 1080x1920 and letterboxed with the app
  background colour, for the case where the Console rejects 20:9. Nothing is
  cropped and the padding is invisible against the dark UI.

Try `native/` first; fall back to `play-9x16/` only if the Console complains.

## Reproducing

Progress shown is seeded, not hand-tapped: a schema-v2 backup with 32 steps
implemented and 2 marked Not Applicable is imported through Backup & Restore
using the "Replace everything?" path, which requires a `globalSettings` object in
the JSON. Target maturity ML2, both OS scopes, Microsoft 365 mode E3 + P1.
