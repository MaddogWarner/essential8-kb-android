# Step 5 handoff — MITRE ATT&CK

Authoritative spec: **`e8kbandroid-parity.md` § "Step 5 — MITRE ATT&CK"** (lines 190–230). This file adds review context; it does not replace the spec. Where they disagree, the spec wins — except for the two corrections in "Spec corrections" below, which were verified against the Swift source on 08/08/2026.

## Read this first — which spec you are following

There are two plans in this repo with **different step numbering**:

| | `e8kbandroid.md` (original build spec) | `e8kbandroid-parity.md` |
|---|---|---|
| Step 5 | "Remaining screens" — AuditPolicy, M365, About | **MITRE ATT&CK** |
| Step 6 | "Polish" — theme, accessibility, lint | Release readiness |

**`e8kbandroid-parity.md` is the active plan.** Gates 2, 3 and 4 all ran against it. The original build spec is finished and historical — do not work from its numbering. The last handoff cycle re-closed the original spec's Step 5 gate by mistake; the tests were kept (commit `2369ddb`) but no parity work was done.

## Starting state

`main` is at `2369ddb`, clean tree, **4 commits ahead of `origin/main`, nothing pushed, no tags**. Gate 4 is closed (`e181a5c`).

Last verified: 35/35 unit tests, lint 0 errors, `assembleDebug`, `assembleDebugAndroidTest`, `git diff --check`.

**Not verified on device since Gate 3.** The four Compose tests added in `2369ddb` have never executed — no device was available. Run the full instrumentation suite when hardware is back, and report Steps 4 and 5 device results together.

## Verified content counts — use these exact figures in tests

Counted from the Swift source on 08/08/2026, not from the spec:

| Figure | Value | How to reproduce |
|---|---|---|
| Techniques in catalogue | **35** | `grep -c 'entry("T' ATTACKCatalogue.swift` |
| Mapping helper calls | **77** | `grep -cE '^\s+mappings?\(' ATTACKMappingData.swift` (35 `mapping()` + 42 `mappings()`) |
| Flattened (step, technique) pairs | **129** | `mappings()` takes a *list* of technique IDs |
| Unique techniques referenced | 35 | every catalogue entry is mapped — no dead entries |
| Unique steps referenced | 65 (of 67) | the 2 unmapped ones are deliberate |
| Tactics | 10 | `ATTACKTactic` |
| Relationships | 4 | Prevent / Detect / Recover / Support |

**77 is the number of source lines, not the number of mappings.** A test asserting `ATTACKMappingData.all.size == 77` will fail. The flattened list has **129** entries. Assert 129 for the list size and, if you want to pin the source shape too, note 77 in a comment.

I pre-ran the two integrity checks the gate requires — both pass on the iOS side, so a failure in Kotlin means a porting typo, not bad source data:
- Orphans (mapped technique not in catalogue): **none**
- Dead catalogue entries (never mapped): **none**

## Spec corrections

**1. There are two deliberately-unmapped steps, not one.** The spec calls out only `// 2-1-2 deliberately unmapped`. There is a second at `ATTACKMappingData.swift:73`:

```
// 6-1-2 deliberately unmapped: defender asset inventory is not T1518.   (line 26)
// 6-1-2 deliberately unmapped: defender asset inventory is not T1082.   (line 73)
```

Port **both** comments verbatim. This is why 65 of 67 steps are mapped. Do not write a test asserting every step ID appears in the mapping data — it will fail correctly. Assert the inverse instead: every step ID *in the mapping data* resolves to a real step.

**2. Tactic names are deliberately not ATT&CK's official names.** `ATTACKTactic` uses `stealth = "Stealth"` and `defenseImpairment = "Defense Impairment"` where official ATT&CK has "Defense Evasion". This is an intentional editorial choice in the iOS app. **Port verbatim; do not "correct" it.** Also note the spelling is US "Defense", not AU "Defence" — that is correct here because it is a proper noun from ATT&CK, so leave it. If you think it is wrong, flag it — do not change it.

## Watch-outs

1. **`ATTACKTactic.allCases` order is display order.** Kotlin `enum class` declaration order gives you this for free — but only if you declare them in the Swift order: Initial Access, Execution, Persistence, Privilege Escalation, Stealth, Defense Impairment, Credential Access, Lateral Movement, Collection, Impact.

2. **`countsTowardCoverage = this != SUPPORT`.** One line, easy to invert. The gate tests it directly.

3. **`inScopeStepIDs` is computed once per pass, not per technique.** iOS flags this as a deliberate performance decision. Preserve it — with 129 mappings across 67 steps, recomputing per technique is a real cost on a mid-range device.

4. **Status precedence is ordered:** indirect → notApplicable → covered → partial → notCovered. Implement as an ordered `when`, not independent booleans.

5. **A technique in several tactics renders under each group but counts once in the summary.** `T1078` is in four tactics — use it as the test fixture.

6. **Techniques with no in-scope mappings return null and are not listed.** They feed the "untallied techniques" sentence instead, which has singular/plural handling to port.

7. **Attribution is a legal requirement.** `attackAttribution`, `attackDisclaimer`, `attackDisclaimerShort`, `attackCoverageCaveat` — verbatim, rendered everywhere iOS renders them. ATT&CK® is a registered trademark of The MITRE Corporation. Carry the `ATTACKCatalogue.swift` source-comment header across too, including the "re-verify on every major version bump" note.

8. **Colour: use the `tertiary` Material role, never a hex.** iOS uses purple for ATT&CK IDs and tactic chips. A hardcoded hex breaks dynamic colour and dark mode.

9. **Global Search must gain ATT&CK IDs**, and the placeholder becomes "Search GPOs, registries, commands, ISM or ATT&CK IDs…". Searching `T1059` must return the steps mapped to it. Android currently omits ATT&CK from both.

10. **Home screen ATT&CK row is hidden when Reference Only Mode is on**, matching iOS. Easy to miss; there is an existing `referenceOnlyMode` flow to gate on.

11. **`.flatMap { $0 }` in the Swift is a type-system workaround.** Write `buildList` in Kotlin. The data is verbatim; the plumbing is idiomatic.

## Gate 5 exit criteria

Unit tests:
- catalogue has exactly 35 techniques, all IDs unique
- mapping list flattens to exactly 129 entries
- every `techniqueID` in the mapping data resolves to a catalogue entry (no orphans)
- every `stepID` in the mapping data resolves to a real step ID — catches typos in the 0-based index scheme
- `countsTowardCoverage` is false for SUPPORT and true for the other three
- a technique mapped solely via SUPPORT reports `indirect`
- a technique whose contributing steps are all N/A reports `notApplicable`
- marking all contributing steps implemented yields `covered`
- the summary counts a multi-tactic technique once (use `T1078`)
- changing target ML or OS scope changes which techniques are listed

Plus: `./gradlew test`, `lint`, `assembleDebug`, `assembleDebugAndroidTest`, `git diff --check`.

Screenshots of the coverage screen and a technique detail are in the spec's gate. **Defer them with the device tests** and say so in the report rather than skipping silently.

## Process requirements

- **Commit at the end of the step.** Signed. The tree is clean.
- **Signing:** commits are SSH-signed. The key *is* loaded in the agent — if signing fails with "incorrect passphrase", `SSH_AUTH_SOCK` is missing from the shell. Do not ask David for a passphrase.
- **Do not push, tag, or create the remote repository.** Local commits only.
- **Verbatim means verbatim.** Technique names, tactic names and mapping notes are copied character-for-character. If something looks wrong in the Swift, flag it — do not silently correct it.
- Report honestly what was and was not verified on device.

## Known non-blockers carried forward

- `android:allowBackup="true"` contradicts the already-published privacy policy, which states the Android app is excluded from Android Auto Backup. **Step 6 item 5** and a ship blocker before Play submission. Leave it alone in Step 5.
- `ProgressStore.init` launches an unstructured coroutine on a never-cancelled `CoroutineScope(Dispatchers.IO)`. Pre-existing. `AppNavigationTest.kt:140` constructs a `ProgressStore` inside a `waitUntil` poll loop and so leaks one per iteration — worth hoisting out of the lambda if you are in that file anyway.
- `AppNavigationTest` asserts `"E5"`, `"P1"`, `"P2"` and `"Current mode: …"` without `scrollToText`, unlike the rest of the file. In a `LazyColumn` an off-screen node is not composed, so these may fail on a smaller screen. Unproven — those tests have never run.
- Spec Step 6 item 6 is **stale**: it says the privacy policy is scoped to iOS only and needs a rewrite. Website commit `402b7ac` already extended it to cover both platforms. The remaining work is making the manifest match the policy, not editing the policy.
