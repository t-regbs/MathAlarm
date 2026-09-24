# Android in-app reviews

MathAlarm uses Google Play Review 2.0.2 and local Settings storage for eligibility and cooldown tracking. Android records review request attempts and API outcomes through Firebase Analytics; eligibility does not depend on analytics or a backend. See [Android analytics events](analytics-events.md) for the event definitions and limitations.

## Eligibility and trigger

- Seven full days since the first app opening with this feature installed. Existing users start the observation period when they first open this version; old usage is not inferred.
- Successfully completed real alarms on three distinct local calendar dates. Previews, snoozes, stale completion commands, and failed dismissals do not count.
- At least 90 full days since the last request attempt, including unsuccessful requests or cases where Play does not show a card.
- Eligibility is captured on entering the foreground without a pending alarm deep link. Completing an alarm during that visit prevents a prompt until a later visit. Rotation retains this snapshot; returning from the background starts a new visit, including tasks originally opened by an alarm.
- A successful alarm save provides the opportunity. Only the editor that saved may close itself; a delayed save cannot close another tablet pane. The list must be visible, and announcements, dialogs, and snackbars must be absent. A short delay lets the UI settle. Navigating to another pane discards this opportunity, so backing out does not reuse an old save.
- Immediately before calling Play, and again after Play returns ReviewInfo, check that the activity is resumed, focused and unlocked, the opportunity is still valid, and no alarm is active or snoozed. Backgrounding or leaving the opportunity invalidates a pending response.

The store retains the first-use timestamp, up to three completion dates, the latest completion timestamp, and the latest request-attempt timestamp. The attempt has a separate key to avoid overwriting receiver-driven completion updates. Data follows the app's existing Android backup behavior; clearing app data resets tracking unless restored from backup.

Google determines whether to show its card. API completion does not reveal a rating, a submission, or whether the card appeared. Errors are logged and never shown to users. The shared store is initialized only by Android; iOS does not request reviews.

## Verification

```sh
./gradlew :core:testAndroidHostTest :shared:testAndroidHostTest :androidApp:testDebugUnitTest :androidApp:assembleDebug
```

Tests cover accepted completion tracking, distinct dates and persistence, waiting periods, cooldown persistence after later completions, eligibility gained during a visit, blocked UI, active alarms, delayed Play responses, backgrounding, and API failures.

The actual Google card requires a Play-enabled device and a matching Play application ID. The normal debug build has a `.debug` suffix, so it does not exercise the production listing. Use a build with the production ID through an internal test track or internal app sharing. The application's own eligibility rules still apply on test tracks, even when Google's quota does not. For a disposable QA build, the local thresholds can be shortened; never ship that change. Internal app sharing disables review submission.

On the Play-installed QA build, verify a qualified normal visit and successful save, then repeat while an alarm is snoozed, with a dialog open, and after backgrounding during a pending request. Verify alarm saving and ringing remain functional if Play is unavailable.

Reference: [Google's testing guide](https://developer.android.com/guide/playcore/in-app-review/test).

## Save interaction regression (2026-09-23)

`AlarmSaveInteractionTest` exercises actual touch input, Room and AlarmManager on the emulator. It sends two Save taps 80 ms apart, with five idle trials and five trials overlapping the normal resume-recovery operation for ten repeating alarms. No fake backend or artificial delay is used in the save or recovery path. Each trial inspects the persisted rows and cleans up its alarms.

| Build | Idle trials with duplicate rows | Busy trials with duplicate rows | Measured recovery duration |
| --- | --- | --- | --- |
| Save guard temporarily removed | 0 / 5 | 4 / 5 | 578–799 ms |
| Save guard restored | 0 / 5 | 0 / 5 | 543–1,028 ms |

This reproduces the overlap on an emulator; these trial counts are not an estimate of how frequently users encounter it. The guard is restored in the current code. Review eligibility and pane ownership have separate unit tests; `TabletPaneLayoutTest` exercises save, preview, Settings navigation, and draft restoration in the actual UI.
