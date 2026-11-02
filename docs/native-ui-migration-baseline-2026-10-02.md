# Native UI migration baseline — 2 October 2026

This records the production starting point for Milestone 1 of the [approved plan](native-ui-migration-plan.md). It is a parity and reliability baseline, not release acceptance. The results-only [interop archive](research/native-ui-interop-2026-10-02/README.md) remains unchanged; its prototype checks are not production integration results.

## Source and environment

- Starting commit: `de821d42213f248442f1045e52885aa17d4388dc`, branch `codex/ios-first-release`.
- Pre-existing local changes: README; IDE deployment target and Firebender database/sidecars; removed Kotlin error logs; Xcode user interface state; untracked Android release mapping; the approved plan and research archive. They are outside migration implementation and must remain preserved.
- No applicable `AGENTS.md` was found in the repository or its ancestor directories during this audit. The approved plan and testing guide govern the work.
- Installed baseline: Xcode 27.0 (`27A266a`), JBR/OpenJDK 21.0.5, Gradle wrapper 9.7.1; catalog pins Kotlin 2.4.20, Coroutines 1.11.0, Lifecycle 2.11.0. Android minimum/compile/target SDKs are 26/37/37. iOS/iPadOS minimum is 26.0, with device families iPhone and iPad; Swift language mode is 5.0.
- Host suites run with `TZ=UTC`. Date-sensitive fakes use the documented Sunday 2030-01-06 baseline. Native simulator checks cannot prove AlarmKit audibility, authentication timing, or background intent execution.

## Feature and presentation inventory

| Feature | Existing behavior to retain | Baseline owner/contract | Parity evidence or remaining check |
| --- | --- | --- | --- |
| Alarm list | Loading/empty/list; creation/time sort; add/edit; toggle; delete/undo; clear confirmation; Android Skip Next/Undo; scheduling failure | `AlarmListViewModel`: nullable alarm `StateFlow`, preferences via `snapshotFlow`, `AlarmListEvent`, channel `UiEvent`; localized and English snackbar strings coexist | List/undo/sorting and command tests; phone/tablet reference captures; real-delivery checks remain platform gates |
| Editor | Time, repeat weekdays, label, enabled state, challenge, snooze, sound; save/cancel, dirty draft/discard, permission/failure retry, duplicate-save protection | `AlarmSettingsViewModel`: Compose `State`, public mutable title `TextFieldValue`, typed nested events plus transient shared event Flow; owner retained in navigation | Editor/draft/navigation tests and captures; native owner retention across detail replacement required in Milestone 2 |
| Challenge settings | Easy/Medium/Hard/Custom, 1–10 questions, normalized operations/ranges/mixing, nested draft retention | Shared `MathChallenge` rules; Compose subpages mutate editor events | Challenge model/generator/editor tests; native rendering/localization deferred to Milestone 3 |
| Sound library | Six stable iOS tones/fallback; Android system picker; select separately from preview; close/return preserves draft; delivered alarm interrupts preview | `AlarmSoundCatalog`, platform preview functions, Compose editor controls; `IosAlarmAudioManager` owns iOS playback | Catalog tests and prior physical Test Alarm report; acoustic output/interruption still manual |
| Test Alarm | Uses unsaved draft; wrong/correct answers and multiple questions; cancel/success returns to same editor | Preview route `fromSheet`; challenge VM has no occurrence/progress persistence for preview; disposal stops preview | Generator/challenge and preview-navigation tests; iPad landscape return remains unresolved historical UI evidence |
| Delivered challenge | Authoritative saved alarm, once-only consumption, persisted progress, stale `activeAt` guard, looping audio, completion/snooze/recovery | `AlarmMathViewModel` exposes Compose state; Compose effects initialize, acknowledge readiness, start audio and dispatch final completion | Existing occurrence/progress/snooze/recovery suites; failed-initialization retry and same-recurring-delivery identity lack coverage at baseline |
| Settings / What's New | Light/Dark/System, sort, persistent feature acknowledgement, feedback/share; platform visibility | Preferences contain Compose mutable state; settings logic resides in Compose; no feature settings VM | Preference/announcement tests, captures; Flow and semantic settings state/actions required in Milestone 2 |
| Cross-cutting | Nine locales, adaptive compact/tablet panes, permissions, review eligibility, analytics, accessibility and launch surfaces | Compose renderers/navigation/theme/resources in `shared/commonMain`; platform UI APIs are mixed into service APIs | Android renderer/permission/review tests; SwiftUI accessibility/localization and Duo/iPad adaptation are Milestone 3+ gates |

Existing native routes are `AlarmList`, `AppSettings`, `SettingsSheet(settingsAlarm, isTest)` and `AlarmMath(alarmJson, fromSheet, handoffJson)`, implementing Navigation 3 `NavKey`. They describe rendering/navigation and must leave shared feature contracts. Native controls will own focus, selection/marked text, keyboard, native navigation, date/number formatting, localized copy, accessibility and layout.

All nine Lyricist catalogs currently live under `shared/.../utils/strings`: English, German, Spanish, Portuguese, Russian, Chinese, Hindi, Bengali and Punjabi. Compose resources, icons, loading animation, theme and adaptive scene strategies also remain shared until Milestone 3. Their continued presence during Milestone 2 does not authorize adding UI dependencies to the new presentation contracts.

## Native entry points and public framework surface

Android `MainActivity` hosts Compose and forwards alarm entry/navigation information. Production receivers, `AlarmService`, notification actions, schedule/boot/time/permission recovery paths execute shared use cases. Playback ownership is in the Android service for real alarms; screen playback is used for previews. Android reviews and Skip Next remain Android capabilities.

iOS `iOSApp` initializes Koin and prewarms storage via functions in `MainViewController.kt`; activation calls schedule reconciliation and checks the durable native queue. `ContentView` wraps `MainViewController()` in `UIViewControllerRepresentable`, including blanket bottom/keyboard safe-area treatment. AppDelegate registers `AlarmKitKotlinBridge` with `AlarmSchedulerBridge`. `StopAlarmIntent` persists a handoff, arms 60-second background recovery and returns the foreground `OpenMathChallengeIntent`. Alert activation also handles delivery. The native UI root stays Compose for this work; replacing it with SwiftUI is Milestone 3.

The static Objective-C framework is named `app`. Its baseline export includes CALF UI, and the unused Alpha Swift export DSL declares `MathAlarmShared`. The production header surface is generated from public Kotlin declarations rather than a deliberate feature facade. Swift currently calls `MainViewControllerKt` startup/reconciliation/handoff helpers, `AlarmSchedulerBridge`, `NativeAlarmScheduler`, schedule request/completion DTOs, `NotificationDeeplinkHolder` and `IosAlarmAudioManager`. Shared public constructors expose use cases, permissions, logging/preferences and progress infrastructure; they are unsuitable as the intended narrow Swift feature API.

`PlatformApis.kt` mixes SDK services/capabilities/audio with Compose permission/picker launchers and a composition local. The service boundary includes domain-facing alarm/notification interactors, permission capability checks, scheduling authorization/registration/cancellation and audio. Share/email/settings presentation, permission dialogs, ringtone pickers and composition locals belong to native UI owners. A public bootstrap/factory facade and exported observable base/domain DTOs are required before SwiftUI can adopt production features without Koin/repository knowledge.

## Persisted compatibility inventory

| Persisted item | Baseline identity/format that must be retained |
| --- | --- |
| Room | `alarms` table, database schema version 9, checked-in migrations/schemas; Android database `alarm_history_database`; iOS Documents `alarm_history_database.db` |
| Alarm entity | Existing column names (`daysoftheweek`, `ison`, `tone`, etc.); retired `snoozeRequiresQuestion`; serialized pending timestamps; `activeAt`, snooze count/time, skipped date, schedule initialization/error/timezone; challenge settings and defaults |
| Theme/sort | `mathalarm_theme_option` persisted mapping Light=0/Dark=1/System=2; `mathalarm_alarm_sort_order` ordinal Creation=0/Time=1; existing Android preference migration retained |
| Announcements | `mathalarm_seen_announcement_` plus stable IDs `math-challenges-v1`, `skip-next-alarm-v1`, `snooze-settings-v1`; `mathalarm_announcement_catalog` and `mathalarm_announcement_batch`, newline separated |
| Challenge progress | `mathalarm_challenge_progress_<alarmId>` JSON: `activeAt`, exact `problems`, `questionIndex`, `startedAt`, `incorrectAnswers`; older records omit the last two with defaults; restore validates occurrence, count and index |
| Sound IDs | `alarm_daybreak`, `alarm_orbit` (default/fallback), `alarm_rally`, `alarm_glass_garden`, `alarm_stepping_stones`, `alarm_clear_signal`; bundled `.caf` files |
| Android registration | URI identity `mathalarm://alarm/<id>/day/<weekday>` or `/snooze`, stable broadcast PendingIntent; `scheduled_alarm_occurrences`, `active_alarm_playback`; legacy cancellation paths retained |
| iOS registration | Deterministic UUID from alarm ID and occurrence key; `day_0`…`day_6`, `snooze`, `recovery`; `MathAlarm.alarmDataStore` Codable UUID-string dictionary; baseline metadata holds alarm ID/difficulty/time/snooze/vibration/tone/title/created Date and optional recovery token/attempt; retained for repeating registrations |
| Pending native handoffs | `MathAlarm.pendingAlarmHandoffs.v1`, ordered Codable entries `{id: UUID, payload: String}`; identical payload deduplication; head-only acknowledgement |
| Native recovery | `MathAlarm.recoverySessions.v1`, alarm-ID dictionary of token UUID, attempt count and start Date; no attempt cap or elapsed-time expiry; token invalidation rejects stale/suspended schedules |

Shared handoff JSON starts as `AlarmHandoff(alarmId, activeAt?)`; Android notification entry may carry the full legacy `AlarmEntity` snapshot. iOS baseline `createAlarmHandoffJson` sends only `alarmId`. Any added delivery identity must be versioned and accept old queued payloads without clearing data. Preview must never write real occurrence/progress/native recovery records.

Analytics uses fixed semantic names and non-identifying arguments: editor opened, saved/save failed, permission prompted/result, ringing started, completed/snoozed/skipped, preview/challenge started/completed, review request/outcome and screen viewed. Android supplies Firebase; iOS supplies `NoopAnalyticsTracker`. Preserve those names and do not export analytics/persistence implementation as Swift feature infrastructure.

## Known reliability gaps before implementation

1. **Failed readiness retry:** `initializeChallenge` catches load/consume failure, generates fallback problems, persists/caches its key, returns false; the same key then returns true immediately. A pending handoff can be acknowledged despite never loading the authoritative occurrence. Required regression: failure, retry failure stays error/unacknowledged, then successful retry loads authoritative settings/progress.
2. **Recurring queue collision:** iOS emits `alarmId`-only payloads; exact-payload deduplication coalesces different unacknowledged deliveries of the same repeating alarm. Required regression: same alarm, different occurrence/delivery identity, ordered queue/relaunch/head acknowledgement; repeated same delivery restores one session.
3. **UI-bound accepted work:** editor/list/challenge commands launch in `viewModelScope`; observer/owner removal can cancel accepted scheduling or completion. Final correct answer emits a transient `CompleteAndClose`; the Compose collector dispatches the durable completion. Removing that collector can leave a solved challenge unresolved. Application work must own accepted commands beneath VMs while retaining `Usecases.command` mutex serialization.
4. **UI-triggered initialization/audio:** Compose effects initialize/acknowledge and start real audio. Disposing the real screen avoids stopping audio but does not provide an application session owner or restore unresolved sessions after handoff acknowledgement/process termination. Progress/audio/recovery must bind to occurrence, not a renderer.
5. **Retained delivery ordering:** navigation can accumulate/reselect delivered challenges without an explicit application queue owner. Later valid handoffs must remain unacknowledged while an unresolved real challenge is presented; completion of one cannot stop another's audio/recovery.
6. **Recovery error visibility:** native registration failure is logged/thrown; there is no shared application state for the error. Continuous recovery and locked/terminated intent execution remain incompletely verified physically.

Historical release issues remain open: actual recurrent/multiple-alarm delivery and registrations; audible sound/vibration/interruption/Bluetooth/DND; successful and rejected snooze/completion and edit/disable/delete cancellation; timezone/DST and permission revoke/regrant; first unlock/overnight/multi-day delivery; iPad landscape preview return; cold-launch artwork/dark title contrast; native localization and accessibility; privacy/support links; intended iOS marketing/build number; distribution signing/archive/TestFlight. User-reported 27 September recovery success applies to the proposed main test only, and initially used bounded recovery. It does not complete the continuous-recovery cancellation/terminated-app matrix.

## Reference visuals and device matrix

[Android reference captures](../captures/design-tool/README.md) contain 88 original light/dark compact/tablet screenshots from 21 September, debug 2.7.0, Android API 36 Resizable emulator (phone 1080×2400/420dpi portrait; tablet 1920×1200/240dpi landscape). They cover the four destinations, drafts/challenge settings, sound picker, announcements and list dialogs. They are historical parity references, not fresh delivery tests. `build/ios-release-review/startup.png` is a prior physical startup capture; prior iPad screenshots under `/tmp` are not durable repository evidence. The Duo screenshots in the research archive show experiments, not production iOS UI. No production closed/open Duo screenshots exist in this baseline.

The supported check matrix remains Android API 26+, CI delivery scenarios APIs 30/32/35/36 plus Pixel/manufacturer physical checks; iPhone and iPad iOS 26+ minimum/current runtimes; Duo closed/open portrait/landscape and transitions. Previously tested physical iPhone 11 runs iOS 26.3.1(a); prior simulator UI runs used iPhone 17 Pro/iPad Pro 11-inch (M5), iOS 26.3. Reference experiment used Xcode 27.1 beta/iOS 27.1 Duo. These historical results must not be conflated with the installed Xcode 27.0 baseline suites or future minimum-runtime bridge checks.

## Fresh baseline test results

Before source edits, the following forced existing tests to execute:

```sh
TZ=UTC ./gradlew :core:testAndroidHostTest :shared:testAndroidHostTest \
  :androidApp:testDebugUnitTest :core:iosSimulatorArm64Test \
  :shared:iosSimulatorArm64Test --continue --rerun-tasks --console=plain
python3 -B -m unittest discover -s scripts -p '*_test.py'
swiftc iosApp/iosApp/PendingDeeplinkStore.swift iosApp/tests/PendingDeeplinkStoreSmoke.swift \
  -o /tmp/mathalarm-baseline-handoff-smoke
/tmp/mathalarm-baseline-handoff-smoke
```

| Baseline check | Result |
| --- | --- |
| Python delivery runner | 2 tests passed, 0 failures |
| Swift production handoff/recovery smoke | Passed queue relaunch/order/dedup/head acknowledgement and recovery persistence, stale tokens, cancellation/isolation, 101 follow-ups and simulated next-day continuation |
| Core Android host | 135 passed, 0 failures/errors/skips |
| Shared Android host | 190 passed, 0 failures/errors/skips |
| Android app unit / Robolectric | 92 passed, 0 failures/errors/skips |
| Shared iOS simulator | 205 passed, 0 failures/errors/skips |
| Core iOS simulator | 134 passed, 0 failures/skips in a standalone rerun of the unchanged original binary; see interruption note below |

Android's fresh total is **417 passing tests** (135 core + 190 shared + 92 app); iOS totals **339 passing tests** (134 core + 205 shared). XML reports under each module's `build/test-results` supplied Android/shared iOS counts, with execution timestamps 2 October 2026, 10:36–10:39 UTC. The core standalone TeamCity log records 134 started/finished tests, zero failed/ignored, and process exit 0. No reproduced assertion failures occurred in the completed suites.

The initial core native process remained alive without new task output for over two minutes at `SkipNextAlarmTest.every weekday selection skips exactly one occurrence and undo restores it`. Process inspection found it sleeping beneath `simctl spawn --standalone`. To unblock baseline snapshot compilation and permit implementation, this owned test process received SIGTERM. Gradle consequently reported 107 tests / 1 failure and `Child process terminated with signal 15`; this is an **interrupted baseline execution**, not a failing application assertion or migration regression. The command exited 1 after 3m49s because of that interruption; `--continue` allowed all other suites to finish. The original linked core binary was copied to `/tmp/mathalarm-original-core-baseline.kexe` and rerun independently through the same simulator. That full rerun passed all 134 tests, resolving the incomplete check without rebuilding changing source. All shared main/test compilation and Android app main compilation completed before the source-mutation gate was released.

```sh
TZ=UTC xcrun simctl spawn --standalone 5AA0A44E-782E-41AF-A351-DECDE05D1FF4 \
  /tmp/mathalarm-original-core-baseline.kexe -- --ktest_no_exit_code --ktest_logger=TEAMCITY
```

Logs: `/tmp/mathalarm-migration-baseline-python.log`, `/tmp/mathalarm-migration-baseline-swift.log`, `/tmp/mathalarm-migration-baseline-gradle.log`, `/tmp/mathalarm-migration-baseline-core-native-rerun.log`. The initial sandboxed Gradle invocation could not write its existing wrapper cache lock; rerunning with approved cache access started the actual tests. This environment restriction is not an application test failure. Baseline compiler warnings include unresolved legacy `kotlin.Experimental` opt-in, expect/actual Beta notices, deprecated date/adaptive APIs and existing redundant checks.

## Milestone 1 gate and Milestone 2 handoff

Milestone 1 is **complete**: the inventory/parity/reliability record and fresh original suite totals are captured here, and production regressions for repeated failed readiness followed by successful retry, stale explicit occurrence identity, versioned handoff compatibility and ordered native deliveries pass in the migration suites. See the [progress record](native-ui-migration-progress.md) for final integrated evidence. Existing physical/release gaps are recorded separately from migration regressions; they do not become passing evidence merely because host tests pass.

Milestone 2 must convert preferences first, provide immutable toolkit-free feature state and semantic actions/results, adopt/pin the verified bridge in Gradle and SwiftPM, extract command/session/bootstrap/service ownership and fix readiness. Its production checks must establish Swift observation, Flow/suspend cancellation, mounted owner cleanup, retained editor identity, observer-independent accepted work and durable real challenge restoration. Android consumers may continue rendering in shared during this milestone. Moving renderers/resources into Android and replacing the iOS root are the concrete Milestone 3 handoff, after Milestone 2's documented gates pass.
