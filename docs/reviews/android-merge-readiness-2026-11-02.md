# Android-focused migration merge review — 2 November 2026

## Recommendation

The shared KMP core / Android Compose / iOS SwiftUI migration can be integrated
into `main` before iOS Milestone 8 is complete. Treat integration and iOS release
approval as separate decisions. No new Android blocker was identified in the
reviewed code paths or passing checks below. The follow-up below resolves the
lint finding and commits the complete candidate; run PR CI before merging.

This is a review, not an executed merge or iOS release sign-off. Production source
was not edited during the review.

## Follow-up: lint fix and candidate commits

At the user's request, the iOS candidate was committed as `0a0b5917`, including
the playback recovery implementation, native database tests/fixture, presentation
fixes, packaging checks and M8 evidence. Local IDE state and generated outputs
were excluded. The Argent wrapper is executable as documented.

The Android follow-up removes the redundant weekday-chip state. Selection is
derived from `currentDays`; a toggle emits the updated day string to the editor.
Fresh `:androidApp:lintDebug`, `:androidApp:testDebugUnitTest` and
`:androidApp:assembleDebug` pass: zero lint errors and 132 app host tests with
zero failures/errors/skips. Native boundary/localization/bridge checks and all 30
Python regressions also pass. No new test was needed for this small state cleanup.
The validation log is `build/merge-review-2026-11-02/host/lint-fix-validation.log`.

The original review snapshot and initial failed lint result below are retained
as historical evidence. Only `RingDayChip.kt` differs from the 529 source hashes
in the build-25 manifest; iOS production source remains the tested build-25 source.
Prior emulator and minified-release checks predate the small weekday-chip fix.
iOS Milestone 8 remains open; the PR integrates the architecture without approving
an iOS release.

## Scope and candidate identity

- GitHub `main` was checked with `git ls-remote`: `8fd60fcf23d16f622c584dee03a06b25e15c7921`.
- Local `main` and `origin/main` match that revision. The current branch contains
  all of main and is 24 commits ahead.
- Reviewed branch: `codex/ios-first-release`, committed tip
  `c158fd353c04fdb28d4d32abd10d5795fd2ebaed`, plus its current working tree.
- At the initial review, the latest M8 fixes were uncommitted. A merge of that branch tip alone did **not**
  include the currently tested candidate. In particular, `IosAlarmAudioManager.kt`
  depends on the untracked `IosAlarmPlaybackRecovery.kt`; native SQLite regression
  tests and their fixture are also untracked. Commit these together with their
  build configuration, native changes and evidence records. Exclude unrelated IDE
  state, chat databases, Python caches and generated release mappings.
- All 529 file hashes listed in
  `build/native-ui-m8/2026-10-15/source-build25.json` match current files. This
  establishes source continuity with that evidence manifest, not a fresh iOS run.

## Findings

### Android lint fails on the moved weekday chip implementation

`androidApp/src/main/kotlin/com/timilehinaregbesola/mathalarm/presentation/alarmsettings/components/RingDayChip.kt:44`
creates `mutableStateOf(sel)` during composition without `remember`.
`:androidApp:lintDebug` fails with `UnrememberedMutableState` (one error, 135
warnings and seven hints). The implementation is identical to the old shared
file on `main`, so this is inherited code, not a newly introduced scheduling
defect. The migration now places it under Android app lint. Prefer using the
already-hoisted `currentDays`/`selected` value directly and emitting the changed
day string rather than adding a second state owner. Rerun lint after fixing it.

The existing CI host job builds/tests Debug but does not run this lint task or
build the minified Android release. Green existing CI alone would not establish
these two checks. The release build was separately verified in this review.

### No additional confirmed Android regression found

Review covered module/dependency changes, DI and ViewModel lifetimes, retained
commands/results, editor/preview navigation, notification handoff, completion and
snooze journaling, recovery, preferences/announcements, resources and ringtone
selection. Android's manifest, receiver, notification scheduler/service and
versioned Room schemas are unchanged from `main`. Moved Android migration SQL
matches the previous implementation exactly. Common scheduling/coordinator code
does change, so the migration is not merely a file move.

Android still owns its Compose renderer and real alarm audio service. iOS owns
SwiftUI and AlarmKit/audio adapters. `core` and `shared` are renderer-free; shared
feature models and application commands are retained below both renderers. The
iOS-only playback repair does not alter Android playback.

## Verification in this review

Evidence root: `build/merge-review-2026-11-02/` (local, ignored).

| Check | Result |
| --- | --- |
| Core Android host tests | 156 passed, freshly executed |
| Shared Android host tests | 203 passed, freshly executed |
| Android app host tests | 132 passed, explicitly rerun after the initial cached result |
| Android Debug APK | Passed |
| Android minified Release APK, R8/resource shrinking | Passed; not a Play-signed upgrade or physical runtime check |
| Android instrumentation APK | Passed |
| Compact/expanded editor, preview, settings and draft recreation | 3 passed on disposable API 35 emulator |
| Resume/delayed delivery, localized errors, ringing/queued deletion, overlap clearing, native ringtone picker and picker contract | 7 passed on the same emulator |
| Native renderer boundary, nine-locale/source-resource checks and Swift/Kotlin bridge pins | Passed |
| Python runner/release regressions | 30 passed |
| Android lint | Failed as described above |
| Real OS cold-start delivery | Passed on unchanged-runner retry: process exited, OS receiver/service/player start, completion and persisted cleanup |
| Real OS snooze delivery | Passed: process exited, initial delivery, accepted snooze, distinct second OS delivery, completion and persisted cleanup |

Host logs and counts are in `host/`; UI results are in `layout/` and
`android-regressions.txt`. The first emulator install was attempted before Android
package services were ready and failed; installation succeeded after the boot
completed signal. The first cold-start attempt failed before delivery because
the runner's one-shot `am kill` coincided with a Firebase transport job that kept
the app process alive. Its `cold-start/` failure and cleanup are retained. The
unchanged runner was retried after that background work settled and passed.
Successful delivery reports are `cold-start-retry/result.json` and
`snooze/result.json`. Playback telemetry is not acoustic evidence; this emulator
was launched without host audio. The task-created emulator was shut down and
deleted after verification; existing emulators and the connected Pixel were
preserved.

No physical Android data was touched. Fresh Doze/reboot/OEM/audibility and
Play-signed upgrade checks were not performed here. The dated 3 October M8 record
contains prior serial Doze and reboot passes; those are historical evidence.

## iOS status from previous chats and code

Read the latest outcomes in **Validate AlarmKit release readiness** and
**Validate AlarmKit release readiness (2)** and reconciled them with the code and
the [15 October M8 record](../native-ui-migration-milestone-8-2026-10-15.md).

Milestones 1–7 are complete against their recorded implementation, automated CI
and archive gates. Native screens, retained shared sessions and platform
separation are implemented. Milestone 8 remains open.

The latest candidate is iOS 2.3.1 build 25. Recorded evidence includes 226 shared
iOS tests, 19 hosted native tests, signed development archive/export and resource/
privacy audits. Build 24 had broader iPhone/iPad UI coverage; that full UI matrix
was not rerun after the build-25 iOS audio repair. Physical build-25 testing
confirmed termination recovery with the same question/progress and restored app
audio, completion followed by at least 100 seconds of silence, and configured
Mail cancellation. Earlier silent recovery was reproduced on build 24, fixed,
and retested rather than left as an unresolved confirmed failure.

Remaining iOS acceptance includes initial-delivery states, current-policy long
recovery/abandoned authentication, snooze and armed edit/disable/delete
cancellation, overlap/recurrence, permissions and PiP Settings return, audio
interruptions/routes/Silent/Focus, physical performance and SwiftUI warnings,
VoiceOver/adaptive/locale coverage, exact iOS 26.0, legacy registration upgrades,
timezone/DST/reboot/overnight reliability, distribution signing, TestFlight and
store/privacy metadata. Development export is not distribution acceptance.

These are iOS release gates. They need not hold the completed platform split
off `main`; keep them explicitly open and continue iOS work from the integrated
baseline. Android release approval should retain its own device/upgrade checks.
