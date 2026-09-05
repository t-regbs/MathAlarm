# Alarm reliability investigation — 5 September 2026

> Historical findings recorded before fixes. Implementation and subsequent validation are documented in [implementation.md](implementation.md). The four regression probes have been promoted into the normal test suites; the original failure expectations below describe the reviewed baseline.

The current checkout contains reproducible alarm bugs. We cannot identify the customer's exact cause without their app version, device details, settings, or logs. This review primarily covers Android and shared scheduling, completion, editing, snoozing, and recovery, with additional findings in the iOS scheduling adapters. Application source and pre-existing edits were left unchanged.

## What the nightly test tells us

`AlarmSettingsViewModel.OnTestClick` emits an alarm object; `AlarmBottomSheet` navigates directly to `AlarmMath(..., fromSheet = true)`. It does not schedule a future Android alarm or exercise a cold-start receiver/service handoff. Opening/testing the app successfully therefore does not prove that tomorrow's alarm is registered or that background playback works.

Opening the app may change device background restrictions, but that remains a hypothesis, not an established diagnosis. There is no Android startup/resume reconciliation call in the inspected code. Testing itself does not explicitly restore tomorrow's schedule.

Also verify the product's repeat semantics: selecting days and enabling “repeat weekly” are separate settings, and `repeat` defaults to false. Selected days alone schedule one occurrence per selected weekday; they do not create an indefinitely recurring alarm. This needs clearer UI, but it is not by itself proof of the customer's problem.

## Prioritized findings

### 1. High: scheduling failure is reported as success

`androidApp/.../utils/ContextExtensions.kt:53–67` returns normally when exact-alarm permission is unavailable and catches scheduling exceptions. `AlarmNotificationScheduler.kt:52–53` then logs success, and `core/.../usecases/ScheduleAlarm.kt:42–48` saves `isOn = true`. The result can be an enabled alarm with no OS alarm behind it. Snooze similarly dismisses the ringing alarm after a schedule call whose failure is hidden.

The UI has some permission gates, but they do not provide a success contract for scheduling or cover every system-event path. The current manifest uses `USE_EXACT_ALARM` on Android 13+, so a revocable exact-alarm grant is primarily relevant to its Android 12/12L path; this is not a claim that all modern installations lack permission.

**Fix:** return a scheduling result or propagate failures; persist a clear scheduling state, surface errors, and do not silence an alarm after a failed snooze. Reconcile saved desired state with scheduling work after interruption. Merely setting `isOn` after success is insufficient when it was already true before the operation.

### 2. High: changing an alarm's time leaves its old occurrence scheduled

**Reproduced with a regression probe.** `PendingIntentIdGenerator.kt:21–27` incorporates hour and minute in the identity. Saving an edit writes the new data before `ScheduleAlarm` reloads and cancels it. Cancellation consequently calculates the new identity and cannot find the old one. Changing 07:00 to 08:00 can leave both times scheduled. Since the receiver looks up the latest database row, the obsolete occurrence can ring using the newly edited alarm data.

**Fix:** use stable identities based on alarm ID and occurrence kind/day, independent of editable time; retain/cancel legacy identities during migration. Do not treat integer hashes as collision-free identifiers. For example, for the same alarm/day, 07:31 and 08:00 generate the same hash because `31 * 7 + 31 == 31 * 8 + 0`.

### 3. High: cancellation does not find snoozed occurrences

**Reproduced with a regression probe.** `core/.../usecases/SnoozeAlarm.kt` schedules a copy with the snoozed hour/minute but leaves the saved alarm's time unchanged. Android cancellation and pending-occurrence checks derive IDs from the original time. A 07:05 snooze for a 07:00 alarm survives normal cancellation. Turning the alarm off suppresses delivery while it stays off, but enabling it again before that stale snooze fires permits an unwanted ring.

**Fix:** give snoozes a stable, distinct occurrence identity and track their actual trigger time. Include them in cancel, completion, pending checks, and reboot recovery.

### 4. High: Undo delete restores the row without scheduling it

`shared/.../presentation/alarmlist/AlarmListViewModel.kt:67–70` only calls `addAlarm` on undo. Deletion already canceled the OS occurrences. An enabled alarm returns to the list with `isOn = true` but no scheduled trigger.

**Fix:** restore and schedule enabled alarms as one coordinated operation; preserve disabled status for disabled alarms. Report a scheduling failure instead of silently restoring a misleading enabled state.

### 5. Medium: one-time alarms crossing the week boundary can remain enabled after their final occurrence

**Reproduced with a regression probe.** Create a non-repeating Saturday/Monday alarm on Friday. After Saturday and then Monday have fired, there are no occurrences left. `CompleteAlarm.kt:43,58–60` still sees Saturday later in the current calendar week and keeps the alarm enabled—even though the selected Saturday was the one that already fired. The list then shows an enabled alarm that will not ring. A subsequent recovery pass can create fresh occurrences for it.

**Fix:** record concrete occurrence dates and completion state. Do not infer remaining one-time occurrences solely from weekday positions, or from a `PendingIntent` token existing. In the current scheduler a token is created before OS scheduling succeeds, so token existence is not proof of a registered alarm.

### 6. Medium: a slightly late trigger is silently delayed seven days

**Reproduced with a regression probe.** `ContextExtensions.kt:35–39` adds exactly seven days whenever the requested time is less than or equal to now. The helper has no repeat/one-time/snooze context. A trigger calculated before a suspension or clock change can become slightly late and be moved an entire week. The existing test suite explicitly expects this behavior.

**Fix:** calculate future recurrences in the domain layer using one clock snapshot and calendar dates. Define an explicit policy for a just-due occurrence versus a genuinely missed alarm. Remove the generic seven-day correction; fixed milliseconds also do not preserve local clock time across DST changes.

### 7. High: iOS uses the wrong weekday convention and ignores requested occurrence timestamps

Both `shared/src/iosMain/.../notification/IosAlarmScheduler.kt:282–291` and `iosApp/iosApp/AlarmKitWrapper.swift:756–758` treat index 0 as Monday. The shared UI and core calculator treat index 0 as Sunday. A Monday-only alarm is therefore mapped to Tuesday by both iOS scheduling implementations.

Separately, the iOS `AlarmInteractorImpl.schedule(alarm, timeInMillis)` ignores `timeInMillis` and schedules the whole alarm again. The notification fallback's one-time branch chooses the next hour/minute and ignores selected weekdays. The AlarmKit non-repeating branch likewise uses the next time with no selected-day date. This disagrees with shared multi-day one-time scheduling. Snoozing through this adapter cancels/replaces the original schedule using the snoozed hour/minute, potentially changing the entire recurring schedule.

**Fix:** define one shared weekday convention and an occurrence-aware platform contract. Use separate operations for recurring schedules and temporary snoozes. These findings are from source inspection; no iOS runtime tests were run.

### 8. Medium: alarm-service restarts and overlapping alarms are not isolated

`AlarmService.startAlarm` replaces `timingController` without stopping the old controller. A second alarm or notification-dismiss re-show leaves the first controller's delayed callbacks alive. Those callbacks can pause the new audio or restart the old alarm. `NotificationInteractorImpl.dismiss(notificationId)` ignores the ID and stops the entire service, so dismissing an older alarm can silence a newer one.

**Fix:** make duplicate starts idempotent, stop the previous controller before replacing it, and define how overlapping alarms are queued or combined. Require dismissal to match the active occurrence. These are source-level findings, not device reproductions.

## Overnight reliability improvements

- **Use the alarm-clock API for wake-up alarms.** Android currently uses `setExactAndAllowWhileIdle`. Android documents `setAlarmClock` as the most time-critical option and notes that it leaves low-power modes when necessary. This is a suitable improvement, not proof that Doze caused this report. [Android alarm documentation](https://developer.android.com/develop/background-work/services/alarms).
- **Handle clock/time-zone changes and safe startup reconciliation.** The manifest/receiver handle boot, package replacement, and exact-permission grant, but not `TIME_SET` or `TIMEZONE_CHANGED`. Precomputed epoch times can therefore disagree with the new local wake-up time. Recovery must use stored occurrence state so completed one-time alarms are not resurrected or snoozes overwritten.
- **Persist delivery evidence.** Record scheduling result, occurrence ID/time, receiver arrival, service start, audio start/failure, and dismissal/snooze. This distinguishes “never registered,” “never delivered,” and “delivered silently” without relying on the customer's memory or an in-app sound test.
- **Make audio failure recoverable.** The service checks whether a URI opens, but playback/decoder failure only logs an error and can leave a silent active alarm. Add a known bundled fallback and explicit playback error handling. Test alarm-stream volume, DND, custom-tone access, and notification/full-screen settings separately.
- **Test the complete background path.** Schedule a real future occurrence, lock the device, let the process die normally, enter Doze, and verify receiver, service, and audio. Repeat after reboot, app update, time-zone change, permission changes, and with closely spaced alarms/snoozes. Manufacturer-specific restrictions require testing on the affected model if it becomes known. [Android Doze testing guidance](https://developer.android.com/training/monitoring-device-state/doze-standby).

## Further cleanup

- `AlarmSettingsViewModel` uses `runBlocking` for database writes on the UI path. Replace it with one coroutine that awaits persistence and scheduling in order. Editing the time/days of a disabled existing alarm also marks it for scheduling, which turns it back on; preserve disabled status unless activation is intentional.
- The list switch mutates the shared `Alarm` object, then launches update and schedule/cancel separately. Use immutable state and serialize commands per alarm to prevent rapid-toggle races.
- Math-screen completion/snooze starts work in the screen ViewModel and immediately pops the screen. Navigation owns that ViewModel's lifetime. Await the operation before navigation so a slow database operation cannot be canceled on teardown.
- Test mode shares real completion and snooze handlers. `fromSheet` only changes audio startup; testing an existing alarm can run real alarm commands. Isolate preview state from live occurrences.
- `RescheduleFutureAlarms` computes the next time repeatedly to classify alarms as future or missed, although the production calculator normally returns a future recurrence. Replace this with one explicit recovery policy using stored occurrence timestamps and per-alarm error handling.
- Remove or consolidate the legacy `MathAlarmNotification` audio/notification path after verifying all callers. Keep one production owner for alarm playback. The service's notification vibration pattern also ignores the saved `alarm.vibrate` setting.

## Validation and reproduction

The existing suites passed before adding isolated probes:

- `:core:testAndroidHostTest`: **77 tests, 0 failures**.
- `:androidApp:testDebugUnitTest`: **65 tests, 0 failures**.

Four additional probes exercised the current implementation and failed their desired-behavior assertions, demonstrating findings 2, 3, 5, and 6. The Android probes use Robolectric API 30; the core probe uses the real completion use case with repository/interactor fakes. They do not simulate overnight Android or manufacturer behavior.

The probes now live in the normal core and Android test source sets. To run them from the repository root:

```sh
./gradlew :core:testAndroidHostTest --tests '*CompletionReliabilityReviewTest' \
  :androidApp:testDebugUnitTest --tests '*AlarmReliabilityReviewTest' \
  --continue --console=plain
```

The baseline produced four assertion failures: old-time cancellation, snooze cancellation, late-trigger handling, and completion of the final Monday occurrence. All four now pass after the fixes documented in implementation.md.

The existing Android scheduler replacement test only asserts that at least one alarm remains, rather than checking that exactly the correct occurrence remains. The API-30 tests also cannot establish Android 12+ foreground-service or permission behavior. Strengthening these assertions and adding device coverage should accompany the fixes.

Recommended implementation order: scheduling-result propagation and stable occurrence identities; undo/edit/snooze/completion corrections; platform recovery and background-delivery tests; service overlap/audio resilience; iOS contract corrections before the next iOS release.
