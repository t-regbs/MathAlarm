# Milestone 8 — device validation and release readiness, 3 October 2026

**Status: OPEN. Do not submit.** Milestones 1–7 retain their documented completed gates. This record adds implementation and measured acceptance evidence; it does not turn simulator results into physical delivery, audibility or store acceptance. Physical observation was initially deferred; the user subsequently connected the iPhone 11 and physical validation resumed as recorded below.

**15 October follow-up:** [Fresh build/device evidence](native-ui-migration-milestone-8-2026-10-15.md)
is the current record. Build 25 fixes a reproduced build 24 audio-restoration
failure; actual terminated fallback preserves question 2 and restores app sound,
user-confirmed. Fresh native/adaptive tests, active upgrade preservation, signed
development packaging and physical configured-Mail cancellation pass. Milestone 8
remains open. [Argent setup and verification](argent-device-automation-2026-10-15.md)
now includes the connected iPhone runner as well as simulator/Duo operation through
`./scripts/argent`. Earlier Device Hub timeouts retain their historical scope.
Automation does not replace acoustic, locked-device or long elapsed-time observations.

## Physical iPhone session — resumed 3 October, evening BST

The connected iPhone 11 now runs **iOS 27.0.1 (24A446)**, rather than the earlier
26.3.1(a) inventory. UDID remains `00008030-001A4CE436E2802E`. CoreDevice reports
paired/connected, Developer Mode enabled and transport `localNetwork`. Device Hub
is running, but both stable and beta instances return `timeoutReached` through
CUA; phone interaction requires the user while deployment/data inspection uses
the documented device CLI.

**Physical in-place upgrade preservation PASSED:** read-only copies of Documents
(database/WAL/SHM) and preferences were retained before replacing 2.3.1(20) with
the existing signed Release development export 2.3.1(21). Signature/profile were
verified; the profile includes this phone. Ordinary launch used no test harness
flags. Repeated data copies show **all database rows identical**, the four existing
alarm IDs 5–8 intact, and **no preference changes**. This establishes preservation
of this native build's existing data, not every historical schema or active OS
registration upgrade. The initial queue/recovery/native metadata stores were empty.

Startup console confirms Koin/Room initialization and authorized AlarmKit. It then
records signal 9 termination; whether that was user/OS initiated is not established,
so this is not a sustained startup-stability pass. No source crash is inferred from
signal 9 alone. The user confirms only reopening MathAlarm on the phone, with no
Device Hub/Xcode install/run. The installed app queries both report build 21;
the bundle URL changed between them, retained as an identity/connection limit
rather than evidence of a different source build.

**First physical recovery/completion run PASSED, user observed:** the disposable
alarm ID 9 (`M8 disposable A`) was due at **18:35 BST**. At the 18:35:40–42 snapshot,
the phone remained locked and recovery attempt 1 was durably accepted, started at
approximately **18:35:04.988** with one pending handoff. The user reports the
**18:36 follow-up was audible**, swiped it, unlocked and reached the maths screen
without a problem. The user then confirms solving and **no further ring for at
least 70 seconds**. The 18:36:36–39 snapshot has the one-shot fixture disabled,
activeAt cleared, pendingTimes empty, no native registration/recovery session or
pending handoff; a later 18:40 snapshot confirms that state remained cleared.
The four original alarms remain unchanged in every snapshot. Ring times are
minute-level user observations, not measured acoustic first-sample latency.

The extended run reuses the same disposable fixture with three questions.
Other cancellation paths, routing and the remaining physical matrix are still
**PENDING**; progress restoration and accepted snooze are recorded below.
The 18:42 snapshot shows three questions saved but the selected 18:37 Saturday
time has already passed, giving the next deadline on 10 October. This is an
invalid setup for today's extended run, not a missed delivery. A future time
today was requested before beginning that test. Original alarm rows remain equal.
The corrected 18:46:01–04 snapshot matches today's **18:46 BST** native/shared
deadline, three questions and no scheduling error. Extended delivery observation
starts from this corrected setup; the next-week setup is excluded.
The recovery session started at **18:46:03.028**. Samples at 18:47, 18:49 and
18:50 show accepted attempts 2, 4 and 5 in the same session, one queued handoff
and unchanged original rows. The phone is passcode-locked at the first sampled
Stop. **Native acceptance beyond ten minutes PASSED:** at 18:56:11 (608.537 s
from the original start) attempt 10 is accepted; at 18:57:22 (679.125 s) attempt
11 is accepted in the same session. The counter therefore advances after the
boundary, with one handoff and unchanged original rows. A near-boundary lock
query still reports passcode required. The user confirms ten minutes elapsed
and unlocked at the next ring, made one wrong answer and advanced only one
question. **Continuous audibility and wrong-answer continuity PASSED, user
observed:** “yes the recoveries were audible through the 10 mins and it kept
playing through wrong answer.” Exact acoustic start latency remains unmeasured.
See `extended-recovery-timeline.json`.

**Physical durable progress/restoration PASSED:** at 18:58:37–39 the three-question
fixture has question index 1, one incorrect answer and an empty native handoff
queue. PID 679 was force-terminated with SIGKILL; the subsequent process query
returns no MathAlarm process. The 18:59:46–48 terminated snapshot retains the
exact full progress record. Ordinary no-argument launch creates PID 830; the
19:00:26–28 snapshot matches all problems, activeAt, questionIndex, startedAt and
incorrectAnswers exactly, with the queue still empty. Recovery remains in the
original session and advances to accepted attempt 13. The user confirms “Same
second question, sound playing” (10 × 3). All original rows remain unchanged.
Artifacts: `terminate-progress.json`, `processes-after-termination.json`,
`relaunch-progress.json`, `snapshots/progress-*/observed-state.json`,
`progress-terminated-comparison.json`, `progress-relaunch-comparison.json`.
This verifies forced process termination/restoration, not user app-switcher
force-quit policy, terminated initial delivery or reboot.

**Physical accepted snooze / foreground delivery PASSED:** snooze accepted at
approximately **19:02:02.441 BST**, with the fixed native deadline **19:07:02.441**.
Snapshots at 19:03 and 19:05 show the old recovery, handoff and progress cleared,
snoozeCount 1, activeAt nil and the matching accepted snooze registration. The
procedure kept MathAlarm visible and the phone unlocked; CoreDevice lock queries
corroborate the unlocked state. The user reports the ring on time, with no premature
ring, and Stop opening the first challenge. The 19:09 snapshot has the exact new
snooze occurrence activeAt, a fresh question index 0/problems/start time and a new
accepted recovery session. Minute-level observation is not acoustic timing.
At **19:13:46–49**, the disposable row is deleted, native registrations/recovery,
progress and handoffs are empty, and all four original rows still equal the backup.
Second-run completion audibility/no-follow-up confirmation remains pending; the
first run's explicit solve/no-ring confirmation is already passed.
Artifacts: `snooze-deadline.json`, `snapshots/snooze-*/observed-state.json`,
`snapshots/post-snooze-cleanup-check/observed-state.json`.

**Foreground policy feedback:** on build 21 the user observed the every-minute
system recovery interrupting a longer solve. This matches the then-current policy,
and does not itself show lost progress. The user explicitly chose deferral while
actively solving. Candidate **2.3.1 (22)** implements a 20-second renewal of a
90-second native fallback while the ready real challenge is visible/active,
returning to a 60-second deadline on departure. Native acceptance precedes removal
of the predecessor; process termination therefore leaves an OS fallback. Existing
recovery tokens/JSON remain readable, with an additional deterministic standby
registration slot. Completion/snooze/edit/disable/delete clean up both slots.
Candidate 22 verification and physical acceptance are separate from build 21's
observations. **Physical 21 → 22 upgrade PASSED:** all four original rows, schema-9
identity and all preferences remain identical in read-only before/after copies.
Ordinary launch initializes Room and the app query reports 2.3.1(22). Artifacts:
`physical-iphone-11/upgrade21-to22-preservation.json`, `install22.json`,
`source-at-install22.json`, `candidate22/identity.json`, `apps-after-build22.json`,
`console22.log`, `snapshots/{before,after}-build22/`. The exported executable SHA256
is `8fc517885e164253f17411e4c057e65dbb30d2643971fbce4b0a29f6c1c7e0b1`.

**Physical foreground deferral PASSED, user observed:** fixture ID 10
(`M8 foreground22`) is due **19:38 BST**, three easy questions. At 19:38:33–36 and
19:41:11–14, the exact full progress record remains at index 1 with one wrong
answer, the queue empty and recovery accepted in its original session/attempt 1.
The native log shows alternating slots accepted before predecessor cancellation,
leaving one scheduled fallback after each renewal. The user confirms no system
notification/pop-up during maths solving and that the change works. At 19:45:13–17
the fixture is resolved/disabled with activeAt, progress, recovery, metadata and
handoffs cleared; original rows remain identical. Acoustic completion/no-later-ring
confirmation is still separate. Lock/background/termination fallback requires a
new unfinished occurrence; the completed occurrence must not re-ring. That next
run is requested. Artifacts: `physical-iphone-11/foreground22-progress-comparison.json`,
`snapshots/foreground22-*/observed-state.json`, `console22.log`.
The two progress snapshots are **2m37.777s apart**; the later capture is over
three minutes after delivery, not a second independent three-minute progress
interval. The latest user confirmation again reports no system pop-up during
solving. A 19:49:16–19 snapshot still shows the completed disposable fixture
disabled and all its native/progress/queue state empty, with original rows equal;
it is not an unfinished fallback test (`snapshots/fallback22-setup-check/`).

**SQLite privacy remediation in candidate 22:** Room now uses the supported iOS
`NativeSQLiteDriver` linked to system `sqlite3`, avoiding bundled SQLite's filesystem
capacity API imports. Database name, schema 9, Room identity and all persisted
formats remain unchanged; Android keeps its existing driver coordinates. A public
20,480-byte database fixture from the earlier disposable bundled-driver upgrade
run verifies complete row preservation, durable-field writes and reopen with the
actual production factory. Fresh shared/Android tests pass **866** (491 Android /
375 iOS), no failures/errors/skips, in `sqlite-system/shared-tests.log` and
`shared-results.json`. The signed archive/import audit and actual binary upgrade
now pass as recorded below. The first candidate 22 Xcode
attempt stopped at duplicate MainActor annotation compilation; it was corrected
before tests and is retained as failed build evidence, not a device defect.

### Candidate 22 follow-up evidence

- Full iPhone 17 / iOS 26.3.1 suite: **22 passed, zero failed, one iPad-only skip**
  (`sqlite-system/iphone-fixed.xcresult`, `iphone-full-summary.json`). This precedes
  the final repair-token selection refinement. Final-source serial hosted checks
  pass **15 tests**, no failures/skips, in `iphone-serial-final.xcresult` / `.log`.
  The first final-source hosted rerun fails its synthetic Repeat transition at
  five seconds, retaining the right route while a presentation layer is still
  moving. Ordinary Repeat/Back then passes three times, followed by the identical
  serial hosted suite, without a source/assertion/deadline change. Concurrent
  simulator client automation is a possible contention cause, not a proven cause.
  Keep that failure: `iphone-final-hosted.xcresult`, `failed-hosted-captures/`,
  `repeat-repro.log`, `repeat-repro/`. Final-source iPad Pro 11-inch (M5) /
  iPadOS 26.3.1 checks pass **17 tests**, no failures/skips: 15 hosted tests plus
  dirty-editor sidebar switching and repeated maths preview. Artifacts:
  `sqlite-system/ipad-final.xcresult`, `ipad-final.log`, `ipad-final-summary.json`.
  Invalid-frame and publishing-during-update warnings remain in this passing run;
  their live resizing impact is not established.
- Actual bundled-driver 21 → system-driver 22 upgrade on disposable iPhone 17 /
  iOS 26.3.1: complete disabled fixture row, user_version 9, Room identity and all
  preferences identical before announcement acknowledgement. Ordinary UI verifies
  the retained fixture, Dark and Time. **PASSED**, `sqlite-system/upgrade-{before,after}/`,
  `upgrade-comparison.json`, `upgrade-{seed,preferences,verify}.log` and captures.
  The old preference capture has a mistakenly renamed build22 filename; the old
  binary is retained `previous/MathAlarm.app` build21, not build22.
- Candidate 22 signed Release archive, strict signature, resources/minimum OS/
  families/version and bounded required-reason import audit: **PASSED**, zero
  undeclared category findings (`sqlite-system/MathAlarm22.xcarchive`, `archive22.log`,
  `archive22-audit.json`, `archive22-signature.log`). Archive executable SHA256
  `f710616de12ec7533136b89e07689fc1632111c4edd2c2396ae71008bfb90889`.
  Development IPA export **PASSED**, `sqlite-system/export22/MathAlarm.ipa`,
  `export22.log`; this is still not App Store/TestFlight distribution.
- Python 30 tests and source/localization boundaries pass. Foundation smoke first
  failed sandbox persistence in its isolated UserDefaults suite; the same executable
  passes with the suite writable. Artifacts: `sqlite-system/python.log`,
  `boundaries.log`, `localization.log`, `queue-smoke-elevated.log`.

### Lock/unlock defect reproduced; candidate23 correction

The fresh fixture ID10 is due **19:57 BST**, with three questions. The user
advances to question 2, locks, unlocks and sees **“Alarm recovery needs attention.
Try again.”** Sound continues from before locking. This is a **FAILED build22
lock/unlock check**, even though audible continuity and progress are retained;
continuous app audio is not proof of a new native recovery delivery.
The 19:59:25–28 read-only copy retains question index1 (60 − 16), the full saved
problems/start/occurrence, an accepted session/attempt1 and empty handoff queue.
All original rows remain unchanged. Stable and beta Device Hub screen reads time
out (`-10005`); the exact visible wording is user supplied, not an inspected screenshot.

`console22-lock-error-evidence.log` shows a native replacement accepted, followed
by `CancellationError()` in renewal's post-acceptance shared check. The scene
task is cancelled on locking; that cancellation must not abort an accepted service
mutation or become a user-facing recovery failure. Candidate **2.3.1(23)** finishes
the complete refresh in an independent Main-actor task: authoritative check,
native acceptance, post-acceptance resolution check and predecessor cleanup.
The scene still controls future renewals; departure tightens the retained fallback.
Genuine scheduling/persistence errors continue to report failure. Shared business
logic and persisted formats are unchanged. Two focused hosted regressions cancel
the screen before the initial shared check and after native acceptance, verifying
that service work remains active and predecessor cleanup finishes. Candidate23
verification passes **17 hosted iPhone tests**, no failures/skips, on disposable
iPhone 17/iOS26.3.1. Both new cancellation regressions pass, alongside native
replacement acceptance/rejection/resolution/cleanup ordering. The signed Release
archive, strict signature/resources/minimumOS/families/version and bounded privacy
import audit pass with zero undeclared category findings. Development export passes;
no distribution upload. Archive executable SHA256
`8fa984b749cd6621ff9bb0c381f76f04d5f4fc50d041a46c53377b2a985fc621`;
exported executable SHA256 `cb7524a7517a9391574beaba9510f73d1eab22b16f287f90df9470b1974d6b4c`.
Artifacts: `recovery-cancellation23/iphone-tests.xcresult`, `iphone-summary.json`,
`archive23.log`, `signature23.log`, `audit23.json`, `export23/MathAlarm.ipa`.

**Physical build23 installed in place:** ordinary launch restores the active
19:57 occurrence. Read-only before/after copies show every alarm row, schema,
Room identity, full question2 progress and decoded recovery session unchanged;
stable preferences are equal. Native registration slots renew during capture and
are explicitly excluded from static equality. Native session JSON key ordering
changes; decoded data remains equal, with the failed raw-string comparator retained.
The phone's app query reports 2.3.1(23). Physical repeated lock/unlock acceptance
**PASSES by subsequent user observation**, separate from build22's foreground pass
and failed departure check. The user confirms locking/unlocking on the original
question2, and repeating after snoozing/returning, without any errors. This closes
the visible cancellation-warning regression on installed build23; native fallback after
abrupt termination remains separate.
Artifacts: `physical-iphone-11/upgrade22-to23-active-preservation.json`,
`progress-upgrade23-comparison.json`, `snapshots/{before,after}-build23/`,
`candidate23/identity.json`, `source-at-install23.json`, `install23.json`,
`apps-after-build23.json`, `console23.log`.
The initial console command's explicit 15-second overall timeout ends log capture;
it does not establish an app crash. The first sandboxed xcresult-summary export
cannot write Xcode's report cache; the elevated export succeeds. Both are tooling
limits, not acceptance passes.

At **20:09:10–14**, the state has changed to accepted snoozeCount1, due
**20:12:27.530 BST** (five-minute acceptance inferred at 20:07:27.530).
The old activeAt, progress, recovery session/both recovery slots and handoffs are
cleared; no recovery failure is persisted. Original rows remain unchanged.
**Build23 snooze state cleanup PASSED**; actual acoustic timing/no early ring
remains unconfirmed. Later user feedback closes lock/unlock warning observation.
This snapshot cannot be an
unfinished-question lock/unlock pass: snooze has ended that old occurrence.
Artifacts: `physical-iphone-11/snooze23-deadline.json`,
`snapshots/lock23-retest-state/observed-state.json`.
At **20:12:49–52**, a fresh occurrence is active at the exact accepted snooze
deadline, with new problems/start time and new accepted recovery session
`6B246544-D7C4-4DF2-ABC7-A8FB957BC0F7`. The queue is empty, no recovery failure is
persisted, and the user has already advanced to question2 (15 ÷ 3). Original
rows remain equal. **New occurrence bookkeeping PASSED**; actual audible ring
time/no early recovery remain unconfirmed. Subsequent user feedback confirms no
warning on locking/unlocking this new challenge too.
CoreDevice reports unlocked shortly before due, which does not prove app foreground
state. Artifacts: `physical-iphone-11/snooze23-occurrence.json`,
`snapshots/snooze23-after-due/observed-state.json`, `snooze23-before-due-lock.json`.
The updated standalone Foundation smoke also compiles and passes queue, delivery
identity and 101-follow-up recovery checks (`recovery-cancellation23/queue-smoke23.log`).
At **20:14:34–37**, the new occurrence is already completed: fixture disabled,
activeAt/snoozedUntil nil, recovery/metadata/progress/handoffs empty, no persisted
recovery failure and original rows equal. **Completion state cleanup PASSED**
(`physical-iphone-11/snapshots/lock23-user-confirmed/observed-state.json`). The
announced termination check is not performed on this resolved occurrence; it
requires a fresh unfinished fixture. A 100-second no-later-ring observation and
final deletion of ID10 are requested. At **20:18:41–44**, the user reports “its
gone” and the read-only copy verifies **fixture deletion PASSED**: ID10 absent,
no registrations/recovery/progress/handoffs/failure, and all four original rows
identical. `snapshots/final23-fixture-removed/observed-state.json` records cleanup.
The 20:17:19 copy already has cleared state more than 100s after the prior completed
copy; this is storage evidence. The user does not separately state no later ring,
so the requested acoustic100s observation remains unconfirmed without another prompt.

Build22 failure artifacts: `physical-iphone-11/snapshots/lock22-error-2000/`,
`console22-lock-error-evidence.log`, `observations.json`; new build artifacts under
`recovery-cancellation23/`, including `source-build23.json` and `iphone-tests.xcresult`.

Evidence under `build/native-ui-m8/physical-iphone-11/`: `device-details.json`,
`apps-before.json`, `apps-after.json`, `candidate-identity.json`, `install.json` /
`.log`, `before/`, `after-upgrade/`, `upgrade-preservation.json`, `console.log`,
`launch.json`, `observations.json`, `snapshots/*/observed-state.json` and corresponding
raw copies. These are local ignored artifacts; raw copies
contain existing user data and must not be published with public test fixtures.
No existing alarm was edited, disabled, deleted or intentionally delivered.

## Candidate and environment

- Repository branch `codex/ios-first-release`, base commit `c158fd353c04fdb28d4d32abd10d5795fd2ebaed`, plus uncommitted M8 changes. Initial build21 manifest is `build/native-ui-m8/source-build.json`; build22 is identified by `sqlite-system/source-build22.json` and `physical-iphone-11/source-at-install22.json`. Current **2.3.1(23)** is identified by `recovery-cancellation23/source-build23.json` and `physical-iphone-11/source-at-install23.json`. App Store Connect build uniqueness is unverified. Shared Kotlin business logic, Android Compose and persisted formats remain unchanged; iOS database driver, native fallback slots and refresh lifetime change as above.
- Xcode 27.0 (`27A266a`), Swift 6.4 toolchain, existing Swift language mode 5. Installed runtimes: 18.3, 18.5, 26.3.1, 26.5, 27.0 and 27.1. **Exact iOS 26.0 is unavailable**. Build deployment target remains 26.0 for both configurations and device families remain iPhone/iPad.
- Disposable iPhone 17 / iPad Pro 11-inch (M5), iOS/iPadOS **26.3.1 (23D8133)**: `A37E450A-DF4A-4F04-990A-ECA16F8901E9` / `F44912D1-F5E6-4790-88C5-7743B0C73910`. Separate upgrade iPhone 17 **27.0 (24A434)**: `3D4EE28A-2F76-44A5-992F-CA97D5560F36`.
- Candidate22 follow-up uses new disposable 26.3.1 simulators: iPhone `16CF940A-5CFB-4865-9F44-FD9DF6C2328E`, upgrade iPhone `0A87B268-1089-4F07-B3B2-0A0C1D8D0792`, iPad `867EC657-617B-4DAF-977D-4F0A2603848E`. These are separate from the initial build21 devices above; result bundles identify the actual device for each run.
- Disposable Android AVD `mathalarm_m8_disposable`, Pixel 6 profile, API 35 Google APIs arm64, serial `emulator-5580`; exact image fingerprint in `environment/android-fingerprint.txt` below. At initial inspection the physical iPhone 11 was on 26.3.1(a), installed 2.3.1(20); it was initially left untouched, then upgraded/tested in the resumed session above. The wireless Pixel 9 remains untouched. Existing user Duo simulator and unrelated IDE/log/mapping changes were preserved.
- Evidence root below means **`build/native-ui-m8/`**, ignored local artifacts, not uploaded or durably hosted. Previous reliability-review results and binaries are retained in `build/ios-reliability-review/`. Older M7/visual-review directories were not present during inspection; linked historical reports remain evidence of those runs, not newly retained artifact copies. Preserve/export the local evidence root before cleaning build outputs.

## Confirmed defects and changes

1. **Supported-runtime teardown crash.** The unchanged hosted suite on 26.3.1 produced five crashes out of nine tests. Crash reports point to `swift::TaskLocal::StopLookupScope::~StopLookupScope` / `swift_task_deinitOnExecutorImpl` from native window-owner isolated deinitialization. Production window/session and permission-guide teardown now use ordinary `deinit`, snapshot cleanup resources/IDs without reading actor-isolated published getters, and dispatch Kotlin/AVKit cleanup to Main. A focused test releases actual owners inside a task-local scope and checks model closure. No occurrence is resolved by observer removal. This matches the runtime defect discussed in the primary [Swift issue #88036](https://github.com/swiftlang/swift/issues/88036); our observation is specifically on 26.3.1, not proof that every older runtime is repaired. Artifacts: `baseline-hosted.xcresult`, `baseline-summary.json`, `environment/crash-editor.ips`, `environment/crash-mounted.ips`; passing final results below.
2. **Unsupported hosted verification hierarchy.** The synthetic multiple-retained-draft iPad journey timed out and emitted warnings about inserting a hosting view into another hosting controller's managed view. The Debug fixture now temporarily mounts in a plain UIKit root container and restores the original root. Behavioral assertions remain intact. Final hosted suites pass on both devices and that hierarchy warning disappears. Ordinary navigation continues to keep one active editor. This is a fixture correction, not a new production draft feature.
   The iPad sidebar UI fixture also now launches before rotating and waits for a landscape window. A failed run's synthesized tap recorded portrait orientation and coordinates (780,188) against a landscape Add frame (164.5,36,47,36). Ordinary Maestro Add passed; the corrected XCTest switching journey passes. Artifacts: `ipad-failed-tap.json`, `ipad-ui-attachments-all/`, `ipad-add-repro.log`, `ipad-switching-fixed.xcresult`. No production layout change was justified by this instrumentation failure.
3. **Release metadata and privacy visibility.** Info.plist resolves Xcode marketing/build settings, now 2.3.1/22. Native Settings → Help has the live privacy-policy link in nine locales. The manifest adds app-container file timestamps C617.1 alongside UserDefaults CA92.1. Packaging tooling checks version, minimum OS/families, executable hashes and bounded API imports with focused regressions. Build21 fails the strict SQLite import audit; candidate22 removes those imports through the system driver and passes, as described above.

## Executed checks

The following table records the **initial build21** validation. Candidate22's
fresh tests, final-source reruns, upgrades and archive are recorded separately
above; historical passes do not silently become acceptance of the new policy.

| Check / realistic steps and expected result | Observed result and limits | Status / artifacts under evidence root |
| --- | --- | --- |
| Full native iPhone suite: regular list/edit/Cancel/Back, wrong/correct preview answers, enabled alarm edits through the real simulator AlarmKit manager, all 27 controlled bridge groups across fresh processes | 18 passed, 0 failed, 1 iPad-only skip. Hosted suite plus UI suite; distinct runtime evidence from c158fd3's earlier 16-test run. This build includes the teardown fix; it predates the final Debug containment and privacy-link edits. | **PASSED (simulator scope)** `iphone-final.xcresult`, `iphone-final-summary.json`, `iphone-final.log` |
| Final-source hosted tests on iPhone and iPad: production factories, teardown, observation, command/result ownership, mounted challenge/restoration/settings, latency and capabilities | 11 passed per device, no failures/skips. Controlled scheduling does not prove OS delivery. The mounted journey checks >150 behavioral assertions and restoration cleanup; fresh-process restoration is separately covered by the full iPhone UI suite. | **PASSED** `iphone-hosted-final.xcresult`, `ipad-hosted-final.xcresult`, corresponding `*-summary.json` / `.log` |
| Final-source iPad: switch dirty alarm, resolve exit, repeatedly edit enabled alarm with real simulator AlarmKit, preview/back and literal label continuity | Enabled edits and preview pass; switching initially fails at Add because XCTest used portrait tap coordinates. Corrected launch/rotation setup passes the entire switching journey. No assertion was removed. | **PASSED with corrected instrumentation** `ipad-ui-selected.xcresult` (2 passed / 1 failed), `ipad-switching-fixed.xcresult` (1 passed), corresponding summaries/logs; ordinary Add `ipad-add-repro.log` |
| Native privacy link: ordinary launch → More → Settings → Help; visible label and Done return | Passed, screenshot inspected; label and controls readable in Dark appearance. Link's destination fetched separately; the flow does not send feedback or publish anything. | **PASSED** `iphone-privacy.log`, `iphone-privacy/**/takeScreenshot/native-privacy-policy-link.png`; checked-in `scripts/maestro/ios/native-m8-release-links.yaml` |
| In-place upgrade: install retained build 20, use UI to save disabled `M8_upgrade_preserved`, choose Dark/Time, terminate, install Debug 21 without uninstall/reset, compare database and preferences before acknowledging announcements | All rows and preferences equal, including existing schema hash, alarm settings and seen/unseen announcement state. Post-upgrade UI shows the disabled fixture and Dark/Time; screenshots inspected. This covers the previous native development binary, not older released schemas, physical OS registrations or interrupted active occurrences. | **PASSED (storage/UI scope)** `upgrade-before/data.json`, `upgrade-after/data.json`, `upgrade-comparison.json`, `upgrade-seed.log`, `upgrade-preferences.log`, `upgrade-verify-final.log`, `upgrade-verify-final/**/takeScreenshot/` |
| Legacy queued payloads, exact acknowledgement, durable progress, different deliveries, continuous recovery, stale cancellation and London DST identities | Hosted tests and native smoke pass, including 101 controlled follow-ups and relaunch. These remain deterministic state/scheduler checks; actual recurring dates, reboot and multi-day delivery remain physical gates. | **PASSED (controlled scope)** `queue-smoke.log`, `queue-smoke` executable, native xcresults |
| Shared/Android baseline suites and app/test APK build | Gradle invocation succeeds, mostly cached/up-to-date. Prior c158fd3 retained fresh results contain 491 Android + 374 iOS = 865; this run is not described as 865 newly executed tests. | **PASSED (cached verification)** `shared-tests.log`, `android-build.log`; prior `build/ios-reliability-review/shared-results.json` |
| Python regression suite, native renderer/localization boundaries and bridge pins | 30 Python tests pass; boundary/catalog/pin checks pass. Catalog verifier now includes native Link labels. | **PASSED** `python-final.log`, `final-verification.json`, `packaging.log` / `.json` |
| Android compact/expanded editor/settings/preview/draft navigation | Three instrumentation tests pass (two expanded, one compact), disabled disposable fixtures cleaned up. | **PASSED** `android-ui-final.log`, `android-ui/expanded.txt`, `android-ui/compact.txt` |
| Android process exit → locked cold start; production completion; snooze → distinct second real OS delivery | Both runners pass with OS broadcasts, service/player start and persisted cleanup. Player-start telemetry is not acoustic output. | **PASSED** `android-cold-start/result.json`, `android-snooze/result.json`, event/log/state files alongside |
| Android deep idle → real alarm delivery, original idle configuration restored; reboot → boot/unlock → real delivery without app reopening | Both fresh serial runs pass, including production completion persistence/fixture cleanup. No acoustic measurement. | **PASSED** `android-doze-final/result.json`, `idle-entry.txt`, `deviceidle.txt`, `cleanup.txt`; `android-reboot-final/result.json` and state/log files |
| Signed Release archive, code signature, production resources and version increase over known build 20 | Archive succeeds; deep/strict signature valid. Nine catalogs, six unchanged CAF files, manifest, nine guide videos/posters and release exclusion of Debug fixtures pass. Metadata is 2.3.1(21), minimum 26.0, families [1,2]. | **PASSED (development signing)** `MathAlarm.xcarchive`, `signed-archive-final.log`, `archive-final-signature.log`, `packaging.json` / `.log` |
| Local IPA export using existing development signing | Debugging-method export succeeds, without provisioning updates. Only Apple Development identity is available, team AAM7WVDGCW; existing development profile expires **8 October 2026 21:06:55 UTC**. This IPA is not a TestFlight artifact. | **PASSED (development export)** `export/final-development/MathAlarm.ipa`, `export/final-development.log`, `environment/signing.txt` |
| Local App Store export, destination export, no upload | Fails: no matching App Store provisioning profile and local App Store Connect authentication/provider unavailable. This says nothing about whether account-side agreements/store records exist. | **UNAVAILABLE / export FAILED** `export/app-store-automatic.log`; distribution identity/profile/account setup remains required |

## Audio and SwiftUI warning investigation

The new test times eight real production guide audio-session acquire/release calls on Main and checks every lease is released. Final iPhone activation median **0.189 ms**, maximum **1.647 ms**; iPad median **0.217 ms**, maximum **0.470 ms**. Deactivation maxima **0.025 / 0.039 ms**. Raw observations, including capabilities, are in `iphone-hosted-final-observations.json` / `ipad-hosted-final-observations.json`; the original preliminary sample is `audio-timing.json`. These are simulator synchronous session calls, not audio first-sample latency, continuous ringing, acoustic measurements or evidence against physical main-thread stalls. The SDK's newer asynchronous activation API cannot simply be assumed available on minimum iOS 26.0. Physical first sound, UI frame stalls, interruptions and routing still need measurement before changing audio threading.

A bounded physical Time Profiler attachment by reported PID returned `Cannot
find process for provided pid: 679`, although CoreDevice still lists that
MathAlarm process. No samples or latency conclusion come from this tooling
failure (`physical-iphone-11/setup-time-profiler.log`, `processes-1843.json`).
One attempt by process name also returned `Cannot find process matching name:
MathAlarm` (`setup-time-profiler-by-name.log`). Physical main-thread and frame
measurements therefore remain unavailable through these attachment attempts.
After ordinary foreground relaunch, one retry against the new PID 830 also
returned `Cannot find process for provided pid: 830` (`foreground-time-profiler.log`).
No physical latency or frame/stall samples were produced.
Instruments lists the phone and elevated signing inspection confirms
`get-task-allow = true`; neither establishes the cause of attachment failure.
Sandbox-only entitlement display warned about an invalid blob, while elevated
display decoded the expected development entitlements. Treat that sandbox
warning and the initial attempt to parse codesign's text dictionary as XML as
tooling failures, not candidate signing defects (`candidate-entitlements*.log`,
`candidate-entitlements-final.txt`).

After correcting containment, the unsupported hosting-view warning disappears. Invalid-frame warnings remain during mounted preview/keyboard transitions; publishing-during-update warnings remain around the mounted settings share/anchor journey. Final logs contain respectively 8/7 invalid-frame entries and 9 publishing entries per device, including XCTest warning echoes, plus one appearance-transition warning during fixture root restoration. All behavioral assertions pass; no visible failure was reproduced in the ordinary preview/settings paths checked here. Root cause and live resizing impact of the remaining warnings are **unresolved**, not declared harmless. Preserve the logged transition times for Instruments/runtime-breakpoint follow-up. No speculative production state rewrite was made to silence logs.

Capability observations: configured Mail false and VoiceOver not running on both clients; native PiP false on iPhone, true on iPad. iPad PiP support alone proves no Settings handoff or originating-draft return. Device Hub UI automation timed out, so interactive Duo transitions/window resizing could not be established. Accessibility identifiers, orientation screenshots and Dynamic Type simulations from M6/M7 remain their documented evidence, not spoken VoiceOver or live resize acceptance.

## Remaining release blockers

- **Build21 privacy audit FAILED; candidate22 remediation PASSED:** build21 imports `_statfs` / `_fstatfs` from bundled SQLite 2.7.1. Its filesystem flag/type usage is not evidence for Apple's observable low-disk-space reason E174.1, so no unsupported purpose was declared. Candidate22 links the supported Room system driver and no longer imports these APIs; the strict archive audit has zero undeclared category findings. Schema-9 regressions and actual simulator/physical upgrades pass as above. File timestamp C617.1 remains based on app-container database metadata. This is bounded import detection, not comprehensive privacy/store acceptance. Historical artifacts: `archive-final-audit.json` / `.log`, `environment/bundled-sqlite-imports.txt`; remediation: `sqlite-system/archive22-audit.json` / `.log`. Primary references: [Room native driver](https://developer.android.com/kotlin/multiplatform/room), [Apple permitted API purposes](https://developer.apple.com/documentation/bundleresources/app-privacy-configuration/nsprivacyaccessedapitypes/nsprivacyaccessedapitype).
- **Policy content/store details PENDING:** [privacy policy](https://sites.google.com/view/math-alarm-privacy-policy) and [support repository](https://github.com/t-regbs/MathAlarm) returned HTTP 200. The linked policy is an older generic policy; refresh/confirm its iOS-specific accuracy and complete the privacy questionnaire from the shipped iOS binary. In-app accessibility and liveness are established, legal/product accuracy and store acceptance are not. Noop iOS analytics does not justify copying Android Firebase declarations.
- **Physical reliability PARTIAL:** build21 locked audible recovery beyond ten minutes, wrong-answer sound continuity, exact progress after forced termination/relaunch, first completion/no-ring cancellation and foreground five-minute snooze delivery/no premature recovery pass. Build22 foreground recovery deferral and exact progress pass; its lock/unlock fails with a cancellation warning while progress and sound continue. Build23 corrects the service lifetime, preserves the active occurrence on in-place upgrade, and passes user-observed lock/unlock before and after snooze. Snooze/final completion state cleanup passes. New-policy lock/background/terminated native fallback and acoustic cancellation after 100 seconds remain pending. Initial delivery in the remaining app states, edit/disable/delete with recovery armed, actual recurring single/multiple-day delivery without editing, different simultaneous deliveries, denied/revoked/regranted permission, PiP → Settings → original draft, interruptions/headphones/Bluetooth/silent/Focus, reboot/timezone changes/DST/overnight/multi-day reliability remain pending. Exact acoustic start latency and physical stalls are unmeasured. Use the updated [manual checklist](ios-alarm-recovery-manual-test.md). September's prototype success does not substitute for these candidate checks.
- **Client/minimum runtime PENDING or UNAVAILABLE:** exact iOS 26.0, live resizing/Duo transitions, spoken VoiceOver focus/announcements, configured-Mail cancellation, physical iPad audio and nine-locale linguistic/accessibility signoff. Current simulator capabilities do not substitute for these checks.
- **Distribution/TestFlight/store PENDING:** development signing and local export pass; distribution export does not. Renew development provisioning for later device work, establish distribution profile/identity and authenticated provider, confirm unique store build/version and store assets/review notes/agreements, then separately authorize upload/submission. TestFlight processing/install/upgrade has not been exercised.

## Invalid attempts and cleanup

Retain failed attempts as evidence: two teardown-fix compile iterations failed on actor-isolated published getters before cleanup ID snapshots; ordinary baseline CRUD and first upgrade UI check hit announcement overlays; a Maestro driver mismatch preceded the pinned 2.10.0 runs; an Android UI invocation targeted a nonexistent emulator before the serial environment was corrected. These are setup/instrumentation failures, not product acceptance passes. The first iPad hosted failure was repaired by the containment change. One iPad test invocation selected a nonexistent class and ran zero tests; iPad UI conclusions use `ipad-ui-selected` and the corrected `ipad-switching-fixed` only.

The initial Android Doze and reboot runners overlapped on their shared fixture. Both attempts are invalid (`android-doze/`, `android-reboot/`), regardless of partial telemetry. The disposable AVD was recreated and final checks run serially. No delivery conclusions use those invalid results. **Initial cleanup passed:** the three build21 task-owned iOS simulators and task-owned Android AVD were removed. `cleanup.json` / `cleanup.log` record successful commands and confirm the existing Duo simulator remains.

**Candidate22 simulator cleanup PASSED:** its three task-owned simulators were
removed after retaining results/upgrade copies/captures; the existing Duo remains
booted and untouched. `sqlite-system/cleanup22.json` and
`devices-{before,after}-cleanup.json` identify each removed device. Physical
fixture ID10 was reused for the 19:57 occurrence, preserved into build23 and
subsequently completed/deleted. Final read-only verification at 20:18 confirms only
the four original alarms remain unchanged, with all test recovery/progress/queue
state cleared. **Physical fixture cleanup PASSED.**
At the end of the build22 phase, `sqlite-system/source-consistency22.json` verifies
its 25 non-documentation manifest files match that archive. Subsequent build23
source changes have a separate manifest and results; the build22 consistency
report is historical. No source commit, upload or submission was made.

Candidate23's single disposable simulator `AF801990-C7DD-46C6-8C4D-D24929462156`
was also removed after retaining its 17-test result; Duo remains untouched.
`recovery-cancellation23/cleanup23.json` and `source-consistency23.json` establish
cleanup and all 25 non-documentation source hashes matching the new archive.
