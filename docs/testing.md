# Testing Math Alarm

Argent is available through `./scripts/argent` for native UI operation. The
[15 October setup/verification record](argent-device-automation-2026-10-15.md)
covers the pinned local install, telemetry opt-out, verified simulator controls
and USB-iPhone limits. Use semantic UI waits and explicit disposable target IDs;
Argent's destructive reinstall command must not be used on existing user data.

## Milestone 8 acceptance and release checks

The [M8 evidence record](native-ui-migration-milestone-8-2026-10-03.md) distinguishes
simulator/controlled passes, setup failures, unavailable access and pending physical
acceptance. Do not rerun completed M1–7 gates simply to replace their history.
Candidate22 has 866 freshly executed shared/Android tests, a full iPhone run with
22 passes/one iPad-only skip before the final token-selection refinement, and
final-source serial hosted iPhone 15 / selected iPad 17 passes. Signed development
archive/export and bounded strict privacy audit pass; physical foreground recovery
deferral passes by user observation. Remaining physical/distribution gates stay
open. Preserve the failed concurrent synthetic Repeat-transition run alongside
ordinary serial reproduction and the identical passing hosted rerun; run native
UI automation serially across these clients to avoid competing test activity.
Use disposable devices only: the bridge harness clears its fixture database, and
Android delivery runners share alarm ID 900001. **Run Android delivery scenarios
serially on a device**, waiting for each exit/result and cleanup before the next.
Never overlap Doze/reboot/snooze on that fixture or use a user's physical installation.

Create a dedicated iOS 26.3.1 simulator (exact 26.0 must be tested separately when
available). For the older-runtime teardown regression and hosted journey:

```sh
xcodebuild -project iosApp/iosApp.xcodeproj -scheme MathAlarmNative \
  -configuration Debug -destination 'platform=iOS Simulator,id=DISPOSABLE_UDID' \
  -derivedDataPath build/native-ui-m8/xcode -disableAutomaticPackageResolution \
  -parallel-testing-enabled NO -collect-test-diagnostics never \
  -only-testing:MathAlarmTests -resultBundlePath build/native-ui-m8/hosted.xcresult \
  CODE_SIGNING_ALLOWED=NO test
```

`testWindowAndGuideReleaseInsideTaskLocalScope` checks actual owner teardown and
eventual factory cleanup; it catches the isolated-deinit crash reproduced on 26.3.1.
`testRecordMainThreadAudioSessionLatency` attaches eight production session lease
timings and verifies no lease remains; it does not measure acoustic sound or first
audio samples. Capture warnings and OS build alongside timings. Hosted fixture
containment uses a plain UIKit controller; it restores the original root afterward.
The iPad sidebar UI test launches before rotating so XCTest's tap coordinates agree
with the landscape hierarchy. Preserve failures and diagnose screenshots/event
records before retrying; no generic retry loop should conceal an assertion failure.

`testScreenCancellationBeforeRecoveryCheckKeepsServiceWorkActive` and
`testScreenCancellationFinishesAcceptedRecoveryReplacement` reproduce the scene
cancellation boundaries behind the build22 physical lock/unlock error. Production
refresh uses `NativeRecoveryDeadlineReplacement.finishRefresh` so locking cancels
future renewals while an accepted service operation finishes its shared check and
predecessor cleanup. These regressions do not prove a physical native re-ring;
verify the new installed build separately with an unfinished occurrence.
Candidate23 passes these checks within all 17 hosted iPhone tests on iOS26.3.1;
its signed Release/resource/privacy audit and local development export pass.
Physical 22 → 23 installation preserves the full active progress and recovery
session. The user confirms physical lock/unlock without errors before and after
snooze; final read-only fixture cleanup preserves all originals. New-policy native
termination fallback remains separate. The build22 visible warning stays a failed
check, not a simulator setup error.

For an upgrade, use a **separate** disposable simulator and normal launch: install
the retained old binary, save a disabled uniquely named alarm and nondefault
theme/sort, terminate, snapshot database/preferences, then `simctl install` the new
binary **without uninstalling or clearing data**. Compare stored rows/keys before
acknowledging any remaining announcements, then inspect the real UI. Do not use
`--verify-shared-bridge` on the preservation fixture. A native build 20 → 21 pass
does not establish earlier released schemas or physical AlarmKit registration
preservation. The M8 report records that limit explicitly.

Run the native privacy-link flow with pinned Maestro 2.10.0 on an English disposable
simulator; it checks presentation/return without sending feedback:

```sh
MAESTRO_CLI_NO_ANALYTICS=1 maestro --device DISPOSABLE_UDID test \
  --test-output-dir build/native-ui-m8/privacy-link \
  scripts/maestro/ios/native-m8-release-links.yaml
```

Verify the **final** Release archive separately from store acceptance:

```sh
python3 -B scripts/verify_ios_release.py --release --previous-build 20 \
  --app build/native-ui-m8/MathAlarm.xcarchive/Products/Applications/MathAlarm.app \
  --report build/native-ui-m8/packaging.json
python3 -B scripts/verify_ios_release.py --release --previous-build 20 \
  --audit-required-reasons \
  --app build/native-ui-m8/MathAlarm.xcarchive/Products/Applications/MathAlarm.app \
  --report build/native-ui-m8/privacy-audit.json
codesign --verify --deep --strict \
  build/native-ui-m8/MathAlarm.xcarchive/Products/Applications/MathAlarm.app
```

The first command checks resources, metadata and pins; the second additionally
fails on bounded undeclared binary imports. A valid reason needs source/purpose
review, not just a symbol match. At M8, packaging passes while the strict audit
fails on SQLite `statfs`/`fstatfs`; no unsupported reason was added to manufacture
readiness. Keep both reports. `--previous-build` compares a known installed build,
not App Store Connect history. The scanner is not comprehensive privacy certification.

Build/archive with existing automatic development signing if available, then local
`xcodebuild -exportArchive` with a plist whose `method` is `debugging` and
`destination` is `export`. A development IPA is only development evidence. Test
distribution separately using `method=app-store-connect,destination=export`;
do not upload/substitute `destination=upload` as part of acceptance. Account/profile
failures, signature/profile expiry, actual version/build and binary hashes belong in
the report. Current local export plists/logs and development IPA are under
`build/native-ui-m8/export/`. No provisioning updates or store uploads were made.

Physical delivery, sound, interruptions, permission/PiP return, exact minimum OS,
VoiceOver/Mail/live resize/Duo and reboot/overnight/multi-day reliability remain the
[manual acceptance matrix](ios-alarm-recovery-manual-test.md). Archive or player
telemetry success cannot close those gates. Preserve the evidence root before
cleaning build artifacts; remove only task-owned fixtures/devices after recording
their results.

## Fast checks (every PR)

```sh
./gradlew :core:testAndroidHostTest :shared:testAndroidHostTest :androidApp:testDebugUnitTest --continue
python3 -B -m unittest discover -s scripts -p '*_test.py'
```

The domain and ViewModel suites cover recurrence, persisted occurrences, editing,
undo, failure propagation, snoozing, and command ordering. Calendar tests enumerate
all 127 nonempty weekday masks and exercise DST/year boundaries. A regression fix
must include an assertion about the final persisted state **and** the scheduled
occurrences, rather than merely verifying that a method was called.

`AlarmSystemIntegrationTest` runs real use cases, Room and Android adapters in
Robolectric. It manually delivers broadcasts; it is not a device end-to-end test.
Its Room query context and application scope share a `TestCoroutineScheduler`.
Use `runTest`, `runCurrent` or `advanceUntilIdle` to finish the work being asserted.
Do not add sleeps or retry a failed assertion until it turns green. Close databases
and cancel scopes at teardown.

Fake clocks default to Sunday 2030-01-06 06:00, never today's date. Set explicit dates
for date-sensitive cases. Pure calendar tests inject a timezone; production timezone
providers remain dynamic so travel/system-zone changes are still observed. Tests
that exercise system-zone adapters intentionally use the same zone for inputs and
expected instants. CI runs host suites with `TZ=UTC`.

Android host tests fail on unimplemented framework calls instead of silently
returning null/zero. Use Robolectric for Android behavior and explicit fakes for
application boundaries.

## Real Android delivery

### Adaptive layout checks

`TabletPaneLayoutTest` uses disabled fixture alarms and cleans them up afterward.
On a window at least 840dp wide, run
`editorAndSettingsShareTheWindowAndProtectDrafts` to verify side-by-side editing,
the embedded time picker and challenge preview, draft protection, app-settings
Back navigation, and saving to the correct alarm. On a phone window below 600dp,
run `compactWindowKeepsTheFamiliarSheet` to check the existing sheet and preview
return behavior. Each test saves screenshots in the debug app's external files
directory. Window-size assumptions skip the test on an unsuitable device.

Build the app and test APKs as below, install them on a disposable emulator, and
grant exact-alarm and notification permissions before running these UI tests.
For example:

```sh
adb -s emulator-5554 shell am instrument -w \
  -e class 'com.timilehinaregbesola.mathalarm.TabletPaneLayoutTest#editorAndSettingsShareTheWindowAndProtectDrafts' \
  com.timilehinaregbesola.mathalarm.debug.test/androidx.test.runner.AndroidJUnitRunner
```

### Alarm delivery checks

Build/install the debug app and test APKs on a disposable emulator:

```sh
./gradlew :androidApp:assembleDebug :androidApp:assembleDebugAndroidTest
adb -s emulator-5554 install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb -s emulator-5554 install -r androidApp/build/outputs/apk/androidTest/debug/androidApp-debug-androidTest.apk
python3 scripts/test_alarm_delivery.py --serial emulator-5554 --scenario cold-start
```

Pass `--adb /path/to/adb` when platform-tools is not on PATH. Use a separate
`--output build/alarm-device-results/SCENARIO` for each run.

| Scenario | What the runner verifies |
| --- | --- |
| `cold-start` | Future alarm registration, process exit, locked screen, real OS broadcast, service and player start, completion persisted |
| `snooze` | Cold-start path, snooze command stops the first alarm, a distinct real occurrence rings about a minute later, completion clears it |
| `doze` | Confirm deep idle before waiting for the OS delivery path |
| `reboot` | Seed an occurrence five minutes ahead, actually reboot, unlock credential storage, observe delivery without reopening the app |

The runner exits instrumentation **before** killing the process or rebooting.
It never injects the alarm-fire broadcast. It sends the production snooze/complete
commands to exercise those receiver paths; button navigation and solving math are
covered separately by the physical-device checklist. `am kill` is intentional:
Android force-stop suppresses scheduled background work until the user launches
the app again and is a different product scenario.

The fixture uses alarm ID 900001, a missing custom sound URI to exercise fallback,
and a one-minute snooze. It cleans up that alarm after each run. It resets the test
app's delivery log and grants required permissions on the disposable emulator.
The Doze fixture schedules three minutes ahead and temporarily sets
`min_time_to_alarm=0,idle_to=30000`. This permits a confirmed 30-second initial idle
window outside Android’s early-wake margin, then restores the original settings.
Android 12–14 require per-key `device_config` overrides in the `device_idle` namespace;
Android 11 and 15+ support the global Settings override. The runner preserves prior
values, restores partial setup failures and verifies both effective service thresholds
before requiring actual deep-idle entry. Android normally avoids
deep idle shortly before alarm-clock events. This accelerated fixture complements
the overnight physical-device check.
Do not use an emulator containing alarms you need to preserve. A failed run is
not retried automatically. Polling is bounded and waits for observable state;
it does not assume an operation finished after a fixed sleep.

Artifacts include event timestamps, alarm-manager state, audio state, logcat,
instrumentation results and a machine-readable result. `audio_started` proves
that the playback path reached `MediaPlayer.start`; it does **not** measure acoustic
output. `result.json` records audibility as **not measured**.

## Production native UI and shared bridge checks

Milestone 8 adds an iOS-only `NativeSQLiteCompatibilityTest` using the production
database factory and a public legacy schema-9 snapshot. Gradle supplies its fixture
path to Kotlin/Native simulator tests; the test copies to a unique temporary file,
checks the complete old row, writes durable occurrence fields, closes/reopens and
removes only its temporary database. Native frameworks/tests and Xcode link system
SQLite with `-lsqlite3`; Android driver coordinates are unchanged.

`ProductionIntegrationTests` also executes the actual native recovery replacement
primitive with controlled OS acceptance/rejection: the previous fallback survives
suspension/rejection, resolution during acceptance cancels the replacement, and
every old cancellation is attempted on failure. These checks establish ordering,
not physical sound or scene timing. Candidate 22's visible ready challenge renews
an accepted 90-second AlarmKit fallback every 20 seconds; leaving tightens to 60
seconds. Validate actual foreground continuity and lock/background/termination
fallback with the [device checklist](ios-alarm-recovery-manual-test.md).

Milestones 1–6 use the production `app` framework and the production Debug app,
with pinned Kotlin/Swift ObservableViewModel 1.1.0 and NativeCoroutines 1.0.6.
See [migration baseline](native-ui-migration-baseline-2026-10-02.md) and
[progress and lifecycle decisions](native-ui-migration-progress.md) for measured
results and the Milestone 7 handoff.

Run the host/native suites (including the renderer tests now in `androidApp`) and build Android:

```sh
./gradlew :core:testAndroidHostTest :shared:testAndroidHostTest \
  :androidApp:testDebugUnitTest :core:iosSimulatorArm64Test \
  :shared:iosSimulatorArm64Test :androidApp:assembleDebug --continue
./gradlew :shared:dependencies --configuration iosSimulatorArm64CompileKlibraries
python3 -B -m unittest discover -s scripts -p '*_test.py'
python3 -B scripts/verify_native_ui_boundaries.py
python3 -B scripts/verify_native_localization.py
swiftc -module-cache-path /tmp/mathalarm-swift-module-cache \
  iosApp/iosApp/PendingDeeplinkStore.swift iosApp/tests/PendingDeeplinkStoreSmoke.swift \
  -o /tmp/mathalarm-handoff-smoke
/tmp/mathalarm-handoff-smoke
```

Create a disposable iOS 26+ simulator, build the `iosApp` Xcode scheme Debug for
that simulator with normal Kotlin framework compilation, then run:

```sh
python3 -B scripts/verify_ios_shared_bridge.py --udid DISPOSABLE_SIMULATOR_UDID \
  --app /absolute/DerivedData/Build/Products/Debug-iphonesimulator/MathAlarm.app \
  --output build/ios-shared-bridge.log
```

The explicit `--verify-shared-bridge` Debug launch path mounts a real SwiftUI owner
and observed child in a UIKit window, using `SharedFeatures` and `IosApplication`.
Twenty-seven groups check bootstrap/handoff identity, generated state observation, the actual
production `NativeSessionOwners` with compact/expanded/detail replacement, multiple
retained drafts/routes, draft tone choices and permission guards; editor validation,
duplicate guards, result acknowledgement, cancellation and structural owner cleanup.
The registry's multiple-session fixtures test ownership across native replacement;
ordinary user navigation now keeps one active editor. The focused XCTest cases
`testEditorExitDiscardsOnlyAfterExplicitChoiceAndAddStartsFresh` and
`testSwitchingAlarmsResolvesChangesAndNeverAccumulatesUserEditors` cover the exit
policy. UI tests cover Cancel/Keep editing/Discard changes, nested Back after changing
snooze/challenge values (button and edge swipe), and iPad sidebar switching. The
main editor uses explicit Cancel/Save; split-column changes never request an exit.
Save closes the editor, unchanged editors
leave immediately, and unsaved edits must be resolved before leaving or switching.
There is no user-facing retained-drafts menu; previews and interruptions preserve
the current edit without creating another user-managed draft.
Milestone 5 adds actual native answer-field focus/placeholder/keyboard/input binding and native maths preview validation/progress/cancellation/completion,
real readiness before exact queue acknowledgement, ordered different-alarm replay,
return to the identical draft after accepted resolution, failed command retry,
duplicate completion/snooze guards and acknowledged unresolved progress restoration.
The runner terminates the first process after acknowledgement and progress persistence,
then launches `--verify-m5-restoration` in a fresh process to compare every problem,
index, start time and incorrect count before accepted resolution.

Milestone 6 adds native theme/sort persistence while multiple drafts, routes, staged
sound and permission guards retain their owners; frozen announcement pages survive
interruption without acknowledgement; explicit browsing/finish persists acknowledgement;
latest batch reopening and fresh-process settings/seen-ID restoration; native share in
the tapped active scene with iPad source rect, cancellation/error callbacks and injected
unavailable-mailto handling. No feedback or share recipient is selected. The first
launch explicitly supplies `--verify-m6-settings-fresh` for disposable fixture keys;
never use that flag on an existing user's installation.

The permission-save regressions check that the first Save requests native authorization
once, refusal preserves the unsaved draft, and approval persists that exact alarm and
its controlled scheduled occurrences. Returning from Settings after a grant resumes the
same draft once; returning without a grant, discarding the draft, changing selection or
failing to open Settings cannot save another draft. Delivery defers save resumption.
Nine six-second silent tutorial assets and owner-scoped audio leases are verified; an
incoming delivery stops the guide. Unsupported PiP opens Settings directly.

The permission sheet has one “Go to Settings” action. It starts the tutorial in native
Picture in Picture, waits for AVKit's start callback, then opens app Settings. The video
continues over Settings and stops on return. There is no separate tutorial selection.
Use `--verify-alarm-settings-guide` only on a disposable Debug simulator to inspect the
guide without a draft, save or permission grant. The simulator used for this refinement
reports PiP unsupported; inline playback, the single-action Settings fallback and
controlled save behavior are simulator evidence. Verify the actual floating window,
Settings navigation, toggle change, return/save and interruption on an iPhone.

Navigation settlement requires the attached native top controller to be visible,
with stable pane/container geometry and no active transition animation. A cached
UIKit transition coordinator alone is not evidence of an unfinished transition;
route identity, readiness and owner assertions remain independent.

Earlier groups save disabled fixtures. M5 groups use production Room, shared
usecases/coordinator and native owners with a controlled `NativeAlarmScheduler`;
they do not register physical AlarmKit alarms. Native recovery requests are captured
at the service boundary; the separate Foundation smoke checks queue writes,
registration reservations, repair, cancellation failures, restart, DST and continuous
recovery token behavior. Use a disposable simulator: fixtures alter its app storage.
The runner cleans fixtures and terminates its app; delete the simulator afterward.

`SKIP_KOTLIN_BUILD=YES` permits a Swift-only iteration against an already generated
framework; it does not establish verification of changed Kotlin source. A final
bridge check must rebuild normally. The harness is excluded from Release. It now shares assertions with permanent M7
XCTest/UI targets (see below), and never replaces physical reliability checks.
The normal iOS root is entirely SwiftUI. Native list/editor/nested settings, tone
preview, maths preview and delivered challenge are implemented. App-wide settings, What’s New, feedback/share and presentation parity are implemented in Milestone 6;
see its verification record for the gate status and unavailable client checks. Native launch/activation
restores durable unresolved occurrences independently of the delivery queue. Failed
readiness keeps the exact delivery queued; accepted readiness persists unresolved
progress before acknowledging the exact head. One unresolved challenge per repeating
alarm coalesces later alerts through shared durable association; different alarms
remain ordered. See the Milestone 5 progress record for failure-ordering decisions.

Simulator checks do not establish physical AlarmKit delivery, audible playback,
locked/background AppIntents, continuous recovery, Bluetooth/DND routing, fold
transitions, minimum supported iOS 26.0 compatibility or release readiness. Preserve
those gates in the release-readiness and manual recovery documents.

Check the freshly rebuilt framework and resolved native graph as well as source:

```sh
./gradlew :shared:dependencies --configuration iosSimulatorArm64CompileKlibraries \
  > /tmp/mathalarm-native-dependencies.log
python3 -B scripts/verify_native_ui_boundaries.py \
  --framework shared/build/bin/iosSimulatorArm64/debugFramework/app.framework \
  --dependencies /tmp/mathalarm-native-dependencies.log
```

The original mounted captures under app Documents/native-ui-m4 show representative native light/dark, accessibility-size and label-focus layouts. They do not establish software-keyboard occlusion, VoiceOver traversal, linguistic review of every locale, acoustic output or physical resizing. UIKit can cache outgoing Form/alert values: structural window-owner removal must close factory keys even when an inert registry remains cached.

Retained native session owners must call `SharedFeatures.closeEditor(sessionId)`
or `closeChallenge(sessionId)` when the session actually ends; layout/detail
replacement only replaces observers. A challenge owner closing never resolves a
real alarm. Regression tests also check accepted saves/completion/initialization
after owner cancellation, failed readiness/progress writes and retry, stable
occurrence/delivery identity, stale guards, and preview/audio cleanup arbitration.


### Native settings, keyboard and locale presentation verification

Use the normal production build on disposable iPhone and iPad simulators. For the
nine-locale visual matrix, run actual views in fresh language/region processes:

```sh
python3 -B scripts/verify_ios_native_presentation.py --udid DISPOSABLE_SIMULATOR_UDID \
  --app /absolute/DerivedData/Build/Products/Debug-iphonesimulator/MathAlarm.app \
  --output build/native-ui-m6/iphone/presentation
python3 -B scripts/verify_native_localization.py \
  --app /absolute/DerivedData/Build/Products/Debug-iphonesimulator/MathAlarm.app
```

The default runner executes a base phase and a separate delivered phase in fresh processes
for each locale. `--base-only` and `--delivered-only` select one phase; `--locales en`
provides a focused iteration. The opt-in Debug-only harness starts one independent task
per process, so SwiftUI task cancellation or repeated mounting cannot restart the run.
It also checks the app's 12-hour display at midnight/noon, padded minutes and all nine
localized day periods, including a locale with an explicit 24-hour preference.
The base phase retains factory owners, saves two uniquely named **disabled** fixtures
(no OS scheduling), captures list/editor/subpages/sound/fallback/preview/settings/announcements
in light and dark accessibility3, and captures lower scroll viewports where content exceeds
the window. Cleanup deletes only the exact fixture IDs through production commands and
acknowledges results in the same process while owners remain mounted; it restores the prior
theme before teardown.

The base phase also captures the real editor dial at 00:00, 03:15, 07:20 and 09:45
in normal light and dark appearances, plus increased-contrast editor views. The
contrast capture temporarily overrides the UIKit window trait and restores its
previous value or absence afterward; JSON records the contrast actually rendered.
These changes affect only an unsaved draft and exercise no additional scheduling.

For the next-alarm subtitle, the base phase briefly enables its saved fixtures
through the controlled Debug scheduler. It checks the displayed occurrence against
the earliest registered request, captures native large-title and scrolled bar
layouts, disables the nearest alarm and checks the following occurrence, then
disables both and confirms that the subtitle and controlled registrations are gone.
The fixtures are subsequently deleted by their exact IDs as above. No AlarmKit
registration is made by this phase.

The delivered phase uses the existing controlled Debug scheduler, a private handoff queue
and a no-op native recovery hook. It exercises native delivered progress and feedback in
light/dark accessibility3, authoritative resolution and return to the retained draft/route.
Its enabled fixture never registers with AlarmKit; resolution and exact-ID cleanup complete
before owners are removed. This is controlled occurrence/presentation evidence, not OS
alarm delivery. Locale logs, PNGs and JSON record phase completion, actual window/control
traits and scroll geometry. A synthetic RTL capture checks layout direction without adding
a supported locale. Launch storyboard captures establish rendered native assets/layout,
not OS cold-launch timing. Successful capture requires separate visual review; final matrix
results must be recorded after both phases complete on each device family.
The runner copies only capture names emitted by the current process and removes prior
images for the selected phase, so stale optional lower-viewport captures cannot pass a run.

Native client flows are in `scripts/maestro/ios/native-m6-*.yaml`. Use Maestro 2.10+
(the installed 1.33 driver failed in this environment), disable analytics, and target
only a disposable simulator:

```sh
MAESTRO_CLI_NO_ANALYTICS=1 maestro --device DISPOSABLE_SIMULATOR_UDID test \
  --test-output-dir build/native-ui-m6/iphone/maestro \
scripts/maestro/ios/native-m6-runtime.yaml
```

`native-m6-sound.yaml` inspects all six native tone/preview controls, applies Clear
Signal only to an unsaved draft, then discards without playback or saving. Start from
the ordinary English list with no active draft and OS maximum accessibility text set;
record and restore the original OS text category afterward. Intermediate harness
`*-bottom` pictures alone are not proof that the final tone row was reached.

Settings exercises all theme/sort controls, restart persistence, explicit What's New
acknowledgement/reopening and native unavailable-feedback handling. Share waits for settled
native presentation and cancels without choosing a destination. Keyboard flows tap actual
editor and maths fields, capture software keyboards, use their Done controls, verify
negative input and the independent trailing Clear action, dismiss wrong-answer feedback,
and return to the exact draft; landscape restores portrait afterward. Regular portrait
also asserts question and Submit visibility while the software keyboard is open.
Permission flows verify the native guard/Keep editing/Allow alarms and draft protection.
The OS denial prompt branch is conditional: a skipped branch is **not** a first-install
permission-client pass. Hardware keyboard preferences must permit the software keyboard.

`native-m6-disabled-crud.yaml` saves, deletes, undoes and deletes a uniquely named disabled
alarm, including preview return before Save. Supply a new alphanumeric/underscore title
with `-e FIXTURE_TITLE=M6_disabled_UNIQUE_RUN_ID`; its selectors interpolate that title as
a regular expression. Never enable the fixture or target an existing alarm. The final
delete removes the fixture without accepting an OS registration.

`native-m6-os-accessibility.yaml` operates native Settings on a disposable English
**402-point iPhone**. It requires Reduce Motion and Reduce Transparency initially **Off**,
asserts and captures both switches On, then captures the app's list/settings/editor under
those OS settings. It restores and asserts both switches Off. Do not run it against another
initial state or screen width. If interrupted after enabling a switch, restore the original
Off state through Settings before reusing the simulator. Report its actual client captures
and restoration separately; this flow does not establish effects across every screen.

Accessibility identifiers/client hierarchy are not evidence of VoiceOver speech or focus
traversal. Preserve explicit unavailable checks for VoiceOver client behavior, unexercised
Reduce Motion/Transparency layouts, interactive window resizing, physical folds, minimum
runtime and alarm reliability in the milestone record; do not replace them with catalog
assertions. These instructions do not declare final matrix or OS-client checks passed.

## CI

Every PR, including a PR targeting `codex/**`, runs host suites and API 35
cold-start/snooze checks. Scheduled and manually dispatched runs additionally
exercise APIs 30, 32, 36, Doze, reboot and native iOS simulator suites. Reports are
uploaded even on failure. GitHub only starts the nightly schedule once this workflow
is on the default branch; use workflow dispatch for earlier broad validation.

```sh
./gradlew :core:iosSimulatorArm64Test :shared:iosSimulatorArm64Test --continue
```

Native tests verify the shared rules and iOS adapter contract. They do not establish
that AlarmKit notifications are audible on a physical iPhone.

## Physical-device release checks

Run on a Pixel/reference Android device and at least one supported manufacturer
with different battery restrictions. Include the affected customer model when
known. Repeat the relevant checks on a physical iPhone before an iOS release.
Record app commit/version, OS/device, timezone, battery restriction mode, alarm
volume, DND, notification/full-screen permissions, selected tone and observed times.

1. Create a future alarm through the UI; leave the app, lock the screen and wait.
   Confirm sound is audible from the intended speaker, vibration follows settings,
   and the notification opens the math screen.
2. Enter a wrong answer: audio continues. Solve the required questions: sound stops,
   the screen closes, and the persisted enabled/disabled state matches recurrence.
3. Snooze through the UI: sound stops promptly and returns at the displayed snooze
   time. Solve it after the second delivery. Confirm the next recurring alarm remains.
4. Repeat after a real reboot and first unlock, and after prolonged screen-off/Doze.
   Separately document behavior before first unlock; the current database requires
   credential storage, so post-unlock success is not proof of pre-unlock delivery.
5. Test a removed/inaccessible custom tone and confirm an audible fallback. Check
   alarm-stream volume, DND, Bluetooth routing and silent-mode behavior individually.
   Record muted/unrouted sound as a failed audibility check even if telemetry says
   playback started. Use an external microphone/recording with timestamps when
   reproducible acoustic evidence is needed.
6. Schedule two close alarms. Complete the first and verify the second still rings.
   Disable/delete a snoozed alarm and verify it never rings again. Edit a time and
   verify the old occurrence does not ring. Undo deletion and verify real delivery.
7. Change timezone/time, update the app, and change exact-alarm/notification settings.
   Verify scheduled times and visible errors agree with what the OS actually accepts.
8. Leave a repeating alarm overnight for several days. Record every expected and
   observed occurrence; the in-app preview is not an overnight reliability check.

Use this record for each case:

| Case | Expected time | Receiver time | Audio event | Audible? | UI/state result | Pass/fail + evidence |
| --- | --- | --- | --- | --- | --- | --- |
| | | | | | | |

## Stacking this change

`codex/reliable-alarm-tests` starts at `codex/fix-alarm-reliability`. Open its PR
against that branch so reviewers see only the testing changes. After the parent
merges, retarget to `main`. If the parent was squash-merged, rebase the testing
commits onto `main` using the old parent tip as the boundary before retargeting;
otherwise the old reliability commits may appear again in the diff.

## Milestone 7 permanent native gate

`MathAlarmNative` is the repository's unambiguous shared scheme. `MathAlarmTests`
imports the production app and production `app` framework. Its mounted journey
forwards the original behavioral assertions to XCTest with source file/line and
checks all 25 first-process groups. `MathAlarmUITests` independently executes the
entire 27-group contract across two OS processes and tests ordinary Back/Cancel.
The first process persists acknowledged unresolved progress; the UI runner terminates
it before starting restoration. Reconstructing an owner in one process is not that gate.
Both targets disable parallel execution because bootstrap, Room and native windows
are process-scoped. The Debug harness is retained and shares the same assertions.

Use only a disposable iOS 27 simulator, with no user alarms or accounts:

```sh
xcodebuild -project iosApp/iosApp.xcodeproj -scheme MathAlarmNative \
  -configuration Debug -destination 'platform=iOS Simulator,id=DISPOSABLE_UDID' \
  -derivedDataPath build/native-tests -disableAutomaticPackageResolution \
  -parallel-testing-enabled NO -collect-test-diagnostics never \
  -resultBundlePath build/native-tests.xcresult CODE_SIGNING_ALLOWED=NO test
```

The hosted test launch intentionally leaves the production app window to the fixture
and suppresses ordinary launch restoration; all tested services/factories and mounted
owners are production implementations. The UI app explicitly clears that hosted-test
setting. The 27 integration groups use a controlled scheduler for OS acceptance; Room, durable
commands, queue ordering, readiness, result acknowledgement, observation and audio
ownership remain real. The enabled-alarm editing UI regression separately uses the
simulator's real AlarmKit service, grants authorization through the system UI, and
saves repeated label, snooze and sound changes. Run it only on a disposable simulator.
On the iOS 27 iPad simulator, XCTest's Settings switch actions did not grant
authorization, while an independent Maestro tap of the visible switch did. Grant
Alarms in Settings → Apps → Math Alarm before rerunning this one live-service test
on iPad. Without that prerequisite it reports an explicit skip; the iPhone scenario
continues to exercise permission setup. A skip does not establish live scheduling
on that device. Controlled permission/return tests still run on both families.
Preview regressions check readiness before navigation, wrong-answer feedback, repeated
entry/exit and literal label preservation. These checks do not establish physical
delivery or audible output. See the [reliability review](native-ui-reliability-review-2026-10-03.md).

The full local/CI command creates and deletes its own simulator, builds normally,
runs native KMP and XCTest/UI suites (including all retained-harness groups), nine locales on actual
views, Maestro 2.10.0 client flows, all four Debug/Release SDK links, resource checks
and an unsigned Release archive:

```sh
M7_OUTPUT=build/native-ui-m7-iphone bash scripts/ci_native_ios.sh
M7_DEVICE=com.apple.CoreSimulator.SimDeviceType.iPad-Pro-11-inch-M5-12GB \
  M7_OUTPUT=build/native-ui-m7-ipad bash scripts/ci_native_ios.sh
```

Set `MAESTRO_BIN` to a pinned 2.10.0 executable if it is not on PATH. The workflow
verifies the release ZIP checksum. Use a new output directory per run (xcresult
creation deliberately refuses to overwrite prior evidence). CI runs both families
on the GitHub-hosted `xcode-27` Apple Silicon image with Xcode 27.0 (27A266a) explicitly selected for PRs, pushes, dispatch
and schedule; it requires the iOS 27 runtime. It keeps Android host/delivery coverage
and adds expanded/recreated/compact Android UI instrumentation on dedicated emulators.
The Android layout fixture explicitly grants notification permission before running;
first-install authorization is a separate scenario.

Full validation is the default. For a platform-specific fixture change, an explicitly
scoped dispatch can run alongside an existing full run without cancelling it:

```sh
gh workflow run build.yml --ref BRANCH -f scope=android
gh workflow run build.yml --ref BRANCH -f scope=ios
```

Each focused run skips the other platform and establishes only its selected platform's
gate. Link both runs and their exact source revisions when relying on unchanged evidence.

`presentation/review.html` compares every required capture family and additional scrolled
viewport across nine locales and links its actual trait/scroll JSON. `review-status.json` records capture completeness
and initially says visual review is pending. Inspect pixels and record the reviewer,
findings and exact run; do not treat generated HTML or file presence as visual approval.
Native XCTest reports, client screenshots/logs, presentation PNG/JSON and archive logs
are uploaded even on failure. Controlled next-alarm assertions compare the subtitle
against accepted scheduler requests and verify removal after disabling the fixtures.

`verify_ios_release.py` checks locked Swift revisions against Kotlin bridge versions,
all compiled locale keys/plurals and launch assets, the six unchanged CAF binaries,
privacy reasons, nine exact video binaries and native-optimized poster dimensions.
Release binaries must exclude Debug verification symbols. Xcode may recompress PNGs,
so byte-for-byte poster identity is intentionally not assumed.

Unsigned archive creation proves packaging only. Physical AlarmKit/recovery, PiP over
Settings, acoustic/routing output, iOS 26.0 minimum runtime and signed distribution
acceptance remain Milestone 8 gates. VoiceOver speech/focus, live window resizing and
configured native Mail cancellation must be recorded as client evidence where available;
accessibility identifiers, injected callbacks or screenshots do not substitute for them.
See the [M7 record](native-ui-migration-milestone-7-2026-10-03.md) for this run's actual
results and unavailable checks, and the [architecture/build guide](native-architecture.md).

### iPhone Duo layout review

Build with Xcode 27.1 and the iOS 27.1 SDK to review Duo's native full-screen
layout. A build linked against 27.0 uses different screen accommodation; running
that binary on a 27.1 runtime alone does not opt into the full-screen experience.
Use a disposable `com.apple.CoreSimulator.SimDeviceType.iPhone-Duo` device with
the `com.apple.CoreSimulator.SimRuntime.iOS-27-1` runtime. Select the toolchain
per command with `DEVELOPER_DIR`, without changing the machine's global selection.

In Device Hub, check closed, unfolded portrait/landscape, partially folded, and
Split View on both sides. Change poses with a dirty editor and a nested settings
page open; confirm the label, settings, and navigation survive without an exit
prompt. Also inspect the keyboard, Test Alarm, deletion notice, and accessibility
text sizes. A static screenshot does not establish state preservation through a
pose transition. UI tests locate overflow across native bars, and exercise the
window-edge Back gesture only when native Back sits at that edge.

Keep foreground controls in the system safe area, allowing asymmetric insets.
The native split view, forms, and toolbars provide adaptive layout. For custom
centered content, review the fold's division reserved region before adding
special positioning. See Apple's [Duo preparation guidance](https://developer.apple.com/videos/play/tech-talks/111461/)
and [adaptive layout guidance](https://developer.apple.com/videos/play/tech-talks/111463/).

The 2026-10-03 Duo review used Xcode 27.1 (27A9269) and iOS 27.1. The alarm-list
check passed on the unfolded display, including native Edit deletion, swipe,
notice expiry, and persistent Undo. Changed Snooze and Math challenge settings
returned to the same configuration without prompting. A client-assisted live
open → closed → open check also passed with a dirty editor and its Snooze page
open: the route, label, enabled value, and changed Snooze value survived both
transitions. Explicit Cancel produced the expected exit confirmation afterward.
Evidence is under `build/duo-ui-review/`, including `inner-tests.xcresult`,
`final-editor-tests.xcresult`, and `transition-tests-2.xcresult`. The temporary
human-assisted fixture is saved there as `DuoTransitionReview.swift.txt`; it is
excluded from the unattended test suite. Partially folded layouts and Split View
on both sides were not covered by this review.
