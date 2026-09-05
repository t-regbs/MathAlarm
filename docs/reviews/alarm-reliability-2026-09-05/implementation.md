# Alarm reliability fixes

Branch: `codex/fix-alarm-reliability`.

## Implemented

- Scheduling errors propagate to the UI and are saved on the alarm. Failed snooze leaves the current alarm ringing. Saved occurrence plans permit recovery after interrupted scheduling.
- Android uses stable URI PendingIntent identities per alarm/day and a separate snooze identity, independent of editable clock time. Legacy identities are canceled where reconstructable; obsolete deliveries are rejected against saved occurrence timestamps.
- Undo restores enabled alarms to the OS schedule. Disabled alarms remain disabled after editing. UI operations are serialized and asynchronous; completion and snooze finish before navigation.
- One-time completion tracks concrete remaining occurrences, including across a week boundary. Recovery preserves their dates and future snoozes instead of recreating completed occurrences. The low-level scheduler no longer moves late triggers seven days forward.
- Android wake-up alarms use `setAlarmClock`. Startup/resume, boot, package replacement, permission grants and clock/time-zone changes reconcile saved schedules. Boot clears stale active playback state.
- Service duplicate starts are idempotent, overlapping alarms queue, and dismissals match alarm IDs. Playback state survives process death and is checked against the database before restarting. Audio tries the chosen URI, the system tone, then bundled audio; vibration follows the saved setting. Delivery diagnostics persist a bounded event history.
- iOS uses Sunday-first weekday indices and exact timestamps for one-time alarms and snoozes. Snoozes no longer replace recurrence. Native scheduling awaits AlarmKit acceptance; notification fallback checks sound permission. Stable native IDs and an upgrade migration replace legacy schedules.
- Preview completion/snooze is isolated from live alarms. Repeat and preview explanations clarify that the preview is a sound/math test, not a future delivery test. The unused second Android playback implementation was removed.
- Room schema 5 adds occurrence, snooze, playback and scheduling-error state through an additive migration from schema 4.

## Device evidence

On an API 35 emulator, an instrumented test scheduled a real future alarm and exited. With the app process absent and the screen locked, Android delivered the alarm about 900 ms after its requested time; audio started about 1.1 seconds after the requested time. The deliberately invalid custom ringtone triggered the fallback path. Killing the ringing process caused Android to restart the sticky service and resume playback from persisted state in a new process. The test alarm was then dismissed and emulator battery/idle overrides reset.

The imminent alarm-clock wake-up prevented the emulator from remaining in forced deep idle. This is therefore cold-start/locked-screen and process-recovery evidence, not a completed overnight Doze or manufacturer-specific reliability test. The customer’s exact cause remains unconfirmed without their device details or logs.

## Automated validation

The final Android/host run passed **90 core + 91 shared + 73 Android tests (254 total)**, including all four original regression probes. `:androidApp:assembleDebug` succeeded. Tests cover the original four reproduced defects plus failure propagation, occurrence recovery, stale deliveries, snooze isolation, undo, preview isolation, schema migration, permissions, overlap and audio fallback.

The native iOS simulator suite passed **95 tests**, bringing the platform test total to **349 tests with zero failures**. The final Xcode iOS simulator application build succeeded with code signing disabled. `git diff --check` passed. Existing Gradle/Kotlin deprecation warnings remain outside this alarm repair.

Commands:

```sh
./gradlew :core:testAndroidHostTest :shared:testAndroidHostTest :androidApp:testDebugUnitTest :androidApp:assembleDebug
./gradlew :shared:iosSimulatorArm64Test
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' -derivedDataPath /tmp/mathalarm-ios-fix-build CODE_SIGNING_ALLOWED=NO build
```

The first combined final run completed all Android/host tasks but caught a nullable iOS settings access; that was fixed before the successful native suite and Xcode build. An earlier disk-full incident also damaged generated Gradle caches; the affected regenerable caches were removed before final validation.

## Final review pass

Consolidated normal-occurrence installation in one shared helper, removed the unused recovery dependency and duplicate archived probe sources, and preserved coroutine cancellation in playback delivery. Recovery now retains an explicit incomplete marker until every requested OS schedule is accepted. Added regression coverage for interrupted recovery and clearing stale active one-time alarms after boot. The full Gradle regression/build run passed after these changes.
