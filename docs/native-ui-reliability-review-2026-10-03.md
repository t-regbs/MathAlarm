# Native UI reliability review — 3 October 2026

Review scope: the reported iOS challenge transition and editor-save failures, plus
the shared feature contracts, native owners, scheduling, recovery, sound ownership,
and milestone 1–7 verification. Milestone 8 physical/device/release gates remain open.

## Reproduced defects and changes

| Finding | Reproduction/evidence | Change |
| --- | --- | --- |
| Editing an enabled alarm can fail with “Unable to save alarm” | Created an enabled alarm using real AlarmKit, reopened it, changed its label, and saved. The simulator daemon rejected its existing UUID: `Not scheduling an alarm with a duplicate ID`, followed by `AlarmServiceError.Code.invalidInput`. The old code treated `AlarmManager.schedule` as an upsert. | Replace only that native registration before scheduling its edited configuration. Retain stable IDs and concrete trigger dates. If replacement fails, restore the previous metadata and make a best-effort restoration of the previous native schedule. Shared failure/reconciliation behavior remains authoritative. Snooze/recovery registrations are independent. |
| Test Alarm navigates before any maths exists | Production XCTest called the same session/path action as native navigation and observed `IDLE` with no problem. | Prepare preview maths synchronously from the unsaved shared draft, without database access, a coroutine bridge round trip, or waiting for real initialization. Start playback when the preview is presented. Production simulator preparation measured approximately 4–6 ms in the focused runs. This measures state preparation, not end-to-end frame or audio latency. |
| Real challenge can show an intermediate/restoration spinner | Native delivery presented its cover before shared initialization; foreground restoration also reset readiness while a ready challenge remained retained. | Present the delivered challenge after initialization succeeds or produces an actionable error. Keep the retained challenge visible during foreground restoration. Durable progress still precedes acknowledgement. |
| Reopening a label replaces literal `+` with spaces | Shared regression saved/reopened `Coffee + maths`; it failed against the previous implementation. The domain object was already decoded. | Remove the obsolete title replacement and update the old test that incorrectly expected corruption. |
| Reverted time/day edits can invalidate an occurrence | Shared regression changed and restored the same time and weekdays, then saved. Dirty state was false, but the sticky reschedule flag still cleared `activeAt` and replaced pending times. | Determine rescheduling from the final draft versus its initial schedule, and remove the sticky flag. |
| Repeated preview presentation restarts audio | Native preview presentation calls the retry/resume entry point when the view is replaced. The coordinator unconditionally rebuilt playback. | Keep playback when the same session already owns it. A focused regression verifies one start across repeated presentation calls and cleanup on close. Avoid deactivating the iOS audio session when no tone preview exists. |

## Cleanup and retained safeguards

- Separate the in-memory preview path from durable real-occurrence initialization;
  remove the now-unnecessary preview branches inside the real initializer.
- Reuse native AlarmKit configuration construction for scheduling and restoration.
- Remove the duplicate standalone bridge run from `ci_native_ios.sh`. Permanent
  XCTest/UI tests already execute the same 27 groups, including fresh-process
  restoration. Keep the standalone script for diagnostics and the contract-consistency
  checks. No failure, recovery, cancellation, or ownership coverage is deleted.
- Correct the migration plan header: milestone 7 is complete; milestone 8 remains open.
- Preserve genuine permission/storage/OS failure reporting. Removing the error state
  would allow the UI to claim an alarm is saved/scheduled without OS acceptance.

## Verification

Final artifacts are in `build/ios-reliability-review/` (local, ignored build output).
Earlier baseline artifacts listed below were produced and inspected during this
review, then removed during disk-space cleanup. Their observed results are retained
here; those deleted files are not available for inspection.

- `baseline-paths.xcresult`: preview regression fails against the old initialization.
- `baseline-editor.log`: the literal-label and reverted-edit regressions fail before
  the fixes (57 tests, exactly those two failures).
- `real-edit-baseline.xcresult` and `edit-alarmkit.log`: real enabled-alarm save failure
  and the AlarmKit duplicate-ID diagnostic.
- `shared-final.log` and `shared-results.json`: clean rebuild passes 491 Android
  host tests and 374 Kotlin/Native iOS tests, with no failures, errors or skips.
  All 86 requested Gradle tasks executed after the build-output cleanup.
- Source and generated-framework renderer boundaries, native localization,
  packaged resources/bridge pins, CI shell syntax, and the two M7 contract tests pass.

- `native-final.xcresult`, `native-final.log` and `iphone-summary.json`: the clean
  iPhone 17 / iOS 27.0 suite passes 16 tests, zero failures, with the one iPad-only
  test correctly skipped. This includes all nine hosted integration tests, all 27
  groups across fresh processes, repeated real AlarmKit saves, and repeated preview
  entry/exit with wrong-answer feedback and a literal `+` label. Preview state
  preparation measured 5.05 ms in this run.
- `edit-alarmkit-final.log`: the simulator's `mobiletimerd` records five successful
  registration attempts using the same stable alarm ID without duplicate-ID rejection.
  The UI test independently verifies accepted saves and deletes its fixture.
- `iphone-preview/` and `iphone-edit/`: exported test screenshots inspected for the
  rendered maths/feedback, preserved label and enabled alarm after successive edits.
- On iPad Pro 11-inch (M5) / iOS 27.0, three targeted scenarios pass across separate
  runs: sidebar switching with unsaved changes (`ipad-verified.xcresult`), repeated
  preview entry/exit (`ipad-regressions-final.xcresult`), and real AlarmKit edits
  (`ipad-live-final.xcresult`, `ipad-live-summary.json`). The final live-service test
  passes in 66.5 seconds with zero failures or skips after granting system permission.
  It creates an enabled alarm, accepts two label edits, a snooze edit and a sound edit,
  then deletes its fixture. The earlier mixed runs contain the setup failures below
  and must not be treated as entirely passing suites.

The first final native run was interrupted by a full disk; Xcode could not complete
its result bundles. The clean iPhone run above replaces that incomplete evidence.
An iPad test-without-building attempt then stopped before tests because no reusable
test manifest existed after cleanup; the replacement runs through the project scheme.
The iPad sidebar and preview regressions pass. Its live AlarmKit test hit a separate
XCTest setup limitation: neither switch actions, coordinate taps, a known orientation
nor restarting Settings enabled Alarms. The same visible switch enabled successfully
through Maestro. The live iPad test now requires prior system authorization and
reports an explicit skip when that prerequisite is absent, rather than treating it
as a successful scheduling test. The final run after the independently verified grant
passes. `ipad-granted-final.png` records the enabled system permission;
`ipad-edit-final/` contains the inspected final screenshot, showing the enabled alarm
with both saved label edits and no save-error popup.

Initial UI attempts stopped in test setup: the authorization dialog was not presented,
Settings could retain its Apps list, and its accessible switch frame included the
label. The test now follows Settings if needed, locates the app via Search Apps, taps
the actual Alarms switch, and asserts that it is on. Those setup failures are not
counted as app regressions or passes. A subsequent fixed-code run saved both label
edits but failed cleanup because previous attempts reused a label; fixtures now use
unique labels. The existing user's simulator and unrelated workspace changes were
preserved; the new tests run on disposable simulators.

XCTest app-element screenshots after iPad rotation were cropped/rotated incorrectly.
The capture helper now uses the full display; replacement captures in
`ipad-screen-preview/` show the complete sidebar and maths controls correctly.

The hosted integration journey still emits runtime warnings about synchronous audio
session activation on the main thread, transient SwiftUI frame/state updates, and
its mounted hosting-controller fixtures. No user-visible failure was reproduced by
the passing UI flows or inspected screenshots. These warnings are not evidence of
zero performance risk: physical audio interruption/latency and live resize profiling
remain useful follow-up work. This change avoids unnecessary audio restarts and
deactivation, but does not redesign audio threading or dismiss those warnings.

This review does not establish physical locked-device delivery, acoustic behavior,
multi-day reliability, iOS 26.0 minimum-runtime acceptance, signing/distribution,
VoiceOver traversal, or fold/window acceptance. The milestone 8 checklist remains
the authority for those gates.
