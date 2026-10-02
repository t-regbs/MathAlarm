# Testing Math Alarm

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
window outside Android’s early-wake margin, then restores the original settings. Android normally avoids
deep idle shortly before alarm-clock events. This accelerated fixture complements
the overnight physical-device check.
Do not use an emulator containing alarms you need to preserve. A failed run is
not retried automatically. Polling is bounded and waits for observable state;
it does not assume an operation finished after a fixed sleep.

Artifacts include event timestamps, alarm-manager state, audio state, logcat,
instrumentation results and a machine-readable result. `audio_started` proves
that the playback path reached `MediaPlayer.start`; it does **not** measure acoustic
output. `result.json` records audibility as **not measured**.

## Production shared UI bridge checks

Milestones 1–2 use the production `app` framework and the production Debug app,
with pinned Kotlin/Swift ObservableViewModel 1.1.0 and NativeCoroutines 1.0.6.
See [migration baseline](native-ui-migration-baseline-2026-10-02.md) and
[progress and lifecycle decisions](native-ui-migration-progress.md) for measured
results and the Milestone 3 handoff.

Run the host/native suites and build Android:

```sh
./gradlew :core:testAndroidHostTest :shared:testAndroidHostTest \
  :androidApp:testDebugUnitTest :core:iosSimulatorArm64Test \
  :shared:iosSimulatorArm64Test :androidApp:assembleDebug --continue
./gradlew :shared:dependencies --configuration iosSimulatorArm64CompileKlibraries
python3 -B -m unittest discover -s scripts -p '*_test.py'
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
Seven groups check bootstrap/handoff identity, generated state observation, retained editor identity through detail replacement, typed validation without persistence, suspend/Flow cancellation, mounted-owner removal and explicit factory cleanup. It creates no alarm registration or real occurrence, terminates its test app afterward. Use a
disposable simulator because installing the app and bootstrapping its isolated
storage are part of the check. Delete that simulator afterward.

`SKIP_KOTLIN_BUILD=YES` permits a Swift-only iteration against an already generated
framework; it does not establish verification of changed Kotlin source. A final
bridge check must rebuild normally. The harness is excluded from Release and does
not replace an Xcode unit/UI target (Milestone 7) or physical reliability checks.
The normal iOS renderer remains Compose until Milestone 3; the bootstrap and bridge harness do not construct that renderer.

Retained native session owners must call `SharedFeatures.closeEditor(sessionId)`
or `closeChallenge(sessionId)` when the session actually ends; layout/detail
replacement only replaces observers. A challenge owner closing never resolves a
real alarm. Regression tests also check accepted saves/completion/initialization
after owner cancellation, failed readiness/progress writes and retry, stable
occurrence/delivery identity, stale guards, and preview/audio cleanup arbitration.

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
