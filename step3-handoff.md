# Step 3 handoff — Deep Audit Mode

Authoritative spec: `e8kbandroid-parity.md` § "Step 3 — Deep Audit Mode" (lines 150–164). This file adds the review context Codex needs; it does not replace the spec. Where the two disagree, the spec wins.

## Starting state

Gate 2 is closed. Commit `d1a2dba` ("feat: complete Android parity gates 1 and 2") is signed and the working tree is clean. **Not pushed** — pushing to `origin` is gated on David's explicit approval.

Verified at Gate 2: 20/20 unit tests, lint, `assembleDebug`, androidTest compilation, 8/8 device UI tests on Pixel 7 / API 36.

## What already exists — do not rebuild it

Step 2 landed the audit **model and persistence**, so Step 3 is recording logic and UI only:

- `AuditEntry` data class — `store/ProgressStore.kt:50`
- `auditTrail: Map<String, List<AuditEntry>>` on `Profile` — `store/ProgressStore.kt:63`
- JSON round-trip — `auditTrailToJson` / `auditTrailFromJson`, `store/ProgressStore.kt:369,385`
- `deepAuditEnabled` global flag and its About toggle — `store/SettingsStore.kt:42,76`

Check each of these against the spec before extending. If a field is missing, add it; do not redefine what is there.

## Watch-outs from the Gate 2 review

1. **Hook recording into `setStatus()`, not a third path.** `toggle()` was refactored at Gate 2 specifically so it delegates to `setStatus()` (`store/ProgressStore.kt:138-146`), matching `ProgressStore.swift:236-238`. If audit recording is attached anywhere else, toggles silently stop being recorded — the exact hole that refactor was done to close. Add a test that asserts a `toggle()` produces an audit entry.

2. **Record only on an actual state change.** The spec requires no entry when the state is unchanged. Compare against the current state *before* mutating, inside the same `mutateActiveProfile` block, so a concurrent write cannot slip between the read and the compare.

3. **Note precedence is exact.** `effectiveNote = if (state == NOT_APPLICABLE) (trimmedNote ?: reason) else trimmedNote`; blank trims to null. Do not "improve" this.

4. **The 200 cap trims oldest-first**, from the front, after appending.

5. **`auditEntries(stepId)` returns newest-first (reversed).** The stored list stays oldest-first; reverse on read, not on write, or the cap trims the wrong end.

6. **Corrupt-profile JSON currently discards all profile data** with only a `Log.e` (`store/ProgressStore.kt:334-337`). This is exact iOS parity and is **not** to be changed in Step 3 — flagged only so it is not mistaken for a new bug. It is a decision for David at Step 4, when backup gives it a recovery path.

## UI notes

- New screen goes on the existing sealed class in `App.kt:49-60` as `data class StepAuditHistory(...)` — no nav library, follow the established `BackHandler` + back-stack pattern.
- Port `<iOS>/StepAuditHistoryView.swift` for row layout; the N/A dialog already exists in `ui/maturity/MaturityLevelScreen.kt` — feed its reason through as the note rather than adding a second dialog.
- Timestamp format `dd/MM/yyyy HH:mm`, `Locale("en", "AU")` — consistent with the existing date formatting from Step 2.
- Port the accessibility label **verbatim**: `"<date>, changed from <prev> to <new>, note: <note or "no note">"`.
- The history affordance appears only on steps that have entries.

## Gate 3 exit criteria

Unit tests must cover:
- no entry written when Deep Audit is off
- no entry when the state is unchanged
- the 200 cap trims oldest-first
- N/A reason falls through to the note when no explicit note is given
- `auditEntries` is newest-first
- audit trail survives a profile switch
- a `toggle()` records an entry (added on review of the Gate 2 refactor)

Backup round-tripping is deferred to Step 4, which is where backup exists.

Plus: `./gradlew test`, `lint`, `assembleDebug`, androidTest compilation, `git diff --check`.

## Process requirements

- **Commit per step from here.** The tree is clean; the Gate 1+2 combined commit was a one-off caused by interleaved uncommitted work, and is not a precedent.
- **Signing:** commits are SSH-signed. The key *is* loaded in the agent — if signing fails with "incorrect passphrase", `SSH_AUTH_SOCK` is missing from the shell. Do not ask David for a passphrase.
- **Do not push, tag, or create the remote repository.** Local commits only.
- Report honestly what was and was not verified on device, as at Gate 2.

## Known non-blockers carried forward

`android:allowBackup="true"` and the `<include>` rules in `res/xml/backup_rules.xml` and `res/xml/data_extraction_rules.xml` contradict locked decision 8 and the already-published privacy policy. This is **Step 6 item 5** and a ship blocker before Play submission — not Step 3 work. Leave it alone for now; do not fix it opportunistically.
