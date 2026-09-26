# iOS first-release readiness review

Reviewed 24 September 2026 against commit `8fd60fc`. Implementation is on `codex/ios-first-release`; the first batch is committed as `d6fef01` and later work is recorded below.

**Recommendation: do not submit this build yet.** Source changes address the identified handoff and authorization gaps, but physical alarm delivery, interruption recovery, final privacy details, and App Store packaging still need release-blocking verification.

## Scope and evidence

- Reviewed the Swift application, AlarmKit bridge, Kotlin iOS adapters, shared alarm lifecycle, Xcode configuration, assets, and CI.
- Xcode installed: 27.0 (`27A266a`). At initial review the project targeted iOS 15.0 and both iPhone and iPad; the agreed follow-up raises the minimum to 26.0.
- Connected physical device: iPhone 11, iOS 26.3.1 (a), wired/paired, Developer Mode enabled. It already has Math Alarm 2.3.1 (20); that installation is not evidence that the current source works.
- The first device build failed because no matching development profile was available without provisioning updates. Enabling Xcode automatic provisioning resolved this: the signed Debug build succeeded and installed on the connected iPhone.
- The first launch was blocked by developer trust. After the user trusted the app, the current build launched successfully. Startup logs show successful dependency/database setup and an authorized AlarmKit bridge; a device screenshot confirms the initial What's New screen rendered. No immediate startup crash was observed.
- Fresh native tests passed (314 tests), and the unsigned Release build passed. Full command outcomes are recorded below.
- **Physical alarm delivery, sound, snooze, recurrence, and completion were not exercised during this review.** Startup success must not be interpreted as an alarm-reliability pass.
- No App Store Connect account, existing store record, distribution agreement, or TestFlight status was inspected. Store tasks below are unverified requirements, not claims that those account-side items are missing.
- The initial review changed only this document. Subsequent source fixes and verification are listed below.

## Agreed release scope

- [x] **iOS/iPadOS 26+ for the first release.** Agreed with the user; Debug and Release deployment targets now use 26.0. AlarmKit is the supported delivery backend. The older local-notification implementation remains in source but older OS versions are outside the release scope.
- [x] **Support both iPhone and iPad.** Keep device families 1 and 2. Use iPad simulators for layout/navigation checks and the connected physical iPhone for alarm delivery and audio, as agreed with the user. AlarmKit is available on both platforms ([Apple overview](https://developer.apple.com/videos/play/wwdc2025/230/)); simulator results do not establish physical iPad audibility.
- [ ] **Define the iOS dismissal promise.** AlarmKit's stop control stops the system alarm before the math challenge is solved; this app then starts in-app playback. Test and document what happens if the person leaves/kills the app during that handoff or during the challenge. Do not promise that iOS makes dismissal impossible without solving math.

Apple references: [AlarmKit](https://developer.apple.com/documentation/alarmkit), [notification interruption behavior](https://developer.apple.com/design/human-interface-guidelines/managing-notifications), [AlarmKit scheduling and system button behavior](https://developer.apple.com/documentation/alarmkit/scheduling-an-alarm-with-alarmkit).

## P0 — fix or remove from the release scope

### Lock-screen dismissal bypass — reported 27 September

**User-observed product blocker.** On the locked physical iPhone, sliding the alarm control silenced the system alarm before authentication completed. Unlocking subsequently opened the app and restarted in-app music; abandoning unlock left no audible challenge enforcement. This was reported by the user, not reproduced by agent automation.

Source inspection confirms that `StopAlarmIntent` requests foreground opening, queues the challenge payload, and explicitly stops the native alarm. However, Apple documents that AlarmKit automatically performs the system stop action and treats the intent as additional behavior. The installed SDK also marks custom `stopButton` appearance unused/deprecated from iOS 26.1, and the current API supplies a system stop control. Renaming the control, removing our explicit `manager.stop`, or requiring intent authentication must not be presented as proven prevention of system dismissal.

- [x] Implement an experimental recovery path: the Stop intent explicitly requests background execution and allows execution while locked, persists the handoff, and schedules a fixed native alarm 60 seconds later before returning a separate foreground-opening intent. Direct activation of an alerting alarm also arms recovery. Physical behavior before authentication remains unverified.
- [x] Persist a recovery session token and attempt count; allow at most five follow-ups per session. Completion, accepted snooze, disable, delete, and edit cancel recovery. Failed snooze acceptance leaves recovery intact. Cancellation invalidates the token so a suspended schedule can be removed after OS acceptance and stale recovery intents are rejected. This token identifies the recovery session; it does not resolve the separate general handoff occurrence-identity task.
- [ ] Confirm locked-device intent execution and surface recovery registration failures appropriately in the app. The prototype logs scheduling failure and throws it from the native intent; successful recovery is not claimed if AlarmKit rejects it. Sessions expire for reuse by a later original delivery after ten minutes; the five-follow-up cap deliberately limits this experiment's enforcement.
- [ ] Test locked delivery → slide → abandon authentication; cancel authentication; unlock → wrong/correct answer; app terminated; and follow-up cancellation. A simulator can validate app navigation and recovery state, but does not establish real passcode/Face ID timing, locked-device execution, or audible recovery. Physical checks require manual observation because iPhone Mirroring disconnected during the user's alarm interaction.
- [ ] Resolve the supported iOS product promise before release. Recovery is a mitigation to evaluate, not evidence that iOS system dismissal can be prohibited.

References: [Apple's system button behavior](https://developer.apple.com/documentation/alarmkit/scheduling-an-alarm-with-alarmkit), [system-provided stop control](https://developer.apple.com/documentation/alarmkit/alarmpresentation/alert-swift.struct), [intent authentication policy](https://developer.apple.com/documentation/appintents/appintent/authenticationpolicy).

Prototype verification: **331 iOS tests** (134 core, 197 shared) passed with zero failures/errors/skips; Swift handoff/recovery persistence checks passed. Tests cover bounded retries, relaunch, stale-token rejection, cancellation invalidation, preserving weekly/snooze registrations, failed snooze retention, and accepted snooze/completion cleanup. The signed Debug iPhone build passed. Standalone native tests initially crashed when calling Apple's app-only notification service; notification removal was isolated for those tests and the final suite passed. These results do not verify locked-device intent execution. [Manual test steps](ios-alarm-recovery-manual-test.md). Logs: `/tmp/mathalarm-ios-recovery-tests.log`, `/tmp/mathalarm-ios-recovery-device-build.log`.

The signed prototype installed successfully on the connected iPhone after retrying a dropped CoreDevice connection. Automatic launch could not establish its required remote XPC service (CoreDevice error 4000); open the installed app manually. No scheduled alarms were created or changed during deployment, and physical recovery remains awaiting the user's test.

### 1. Make Skip next change the actual iOS delivery date

**Confirmed source defect.** The shared calculator moves the skipped weekday forward seven days, but both iOS backends discard that date for repeating alarms. AlarmKit creates a weekly relative schedule from hour/minute/weekday; the fallback does the same with a repeating calendar trigger. A Monday alarm skipped before Monday can therefore still ring that Monday while the UI says it is skipped.

Evidence: `core/.../provider/AlarmTimeCalculatorImpl.kt:72`, `iosApp/iosApp/AlarmKitWrapper.swift:557`, `shared/src/iosMain/.../notification/IosAlarmScheduler.kt:161`.

A read-only Foundation/UserNotifications probe on this Mac confirmed that the same Monday 07:00 weekly trigger returns 28 September 2026 at 07:00 BST, with no date exception. Skipping that occurrence requires 5 October instead. This probe verifies trigger calculation, not physical iPhone delivery.

- [x] Hide Skip next on iOS until a native date-exception strategy can preserve indefinite recurrence without relying on the app reopening. The iOS migration clears old skips and restores a future skipped one-time date where applicable. Android retains Skip next.
- [ ] Verify actual OS registrations/delivery for recurring single-day and multi-day alarms and a pending snooze. Skip/Undo are outside the iOS release scope; a fake bridge that only checks the requested timestamp is insufficient.

### 2. Use one owner for alarm audio and vibration

**Confirmed source defect on local-notification paths.** `iOSApp.swift` starts `AlarmAudioController`, while the challenge starts a separate `IosAudioPlayer`. Completing/snoozing the challenge stops `IosAudioPlayer` and `IosAlarmAudioManager`, but never the Swift controller. Its player and vibration timer can remain active. The foreground notification delegate also schedules two starts of the Swift controller.

Evidence: `iosApp/iosApp/iOSApp.swift:224,250`, `shared/src/commonMain/.../presentation/alarmmath/AlarmMathViewModel.kt:209`, `shared/src/iosMain/.../platform/PlatformApis.ios.kt` (`stopPlatformAlarmAudio`). The three implementations are `AlarmAudioController.swift`, `interactors/AudioPlayer.kt`, and `interactors/IosAlarmAudioManager.kt`.

- [x] Route notification entry, the challenge screen, tone preview, and stop paths through `IosAlarmAudioManager`; remove the separate Swift player. Foreground notifications now start playback once.
- [ ] Wrong answers must keep audio playing; completion, snooze, preview dismissal, and cancellation must stop all audio and vibration promptly.
- [ ] Test AlarmKit and local-notification entry separately if both remain in scope. In-app `PlatformVibrator.startWaveform` currently emits one pulse and ignores its waveform/repeat arguments; implement or clarify the supported vibration behavior.

### 3. Serialize every alarm handoff safely

**Confirmed source defect.** Two AppDelegate paths interpolate the alarm title and tone directly into JSON. A title containing a quote, backslash, or newline produces invalid JSON, which is later decoded by navigation. The stop intent already uses `JSONSerialization`; the other paths do not.

Evidence: `iosApp/iosApp/iOSApp.swift:97,231`; safe implementation in `iosApp/iosApp/AlarmKitWrapper.swift:173`.

- [x] Use the same `JSONSerialization` encoder for AlarmKit stop, alert activation, and notification entry. The challenge resolves saved settings by alarm ID.
- [x] Reject malformed or unidentified payloads before navigation; ignore disabled/deleted iOS alarms and duplicate navigation for the same alarm. Added escaped-title/malformed-payload tests. Native Swift encoder and physical handoff still need integration tests.

### 4. Complete or disable native countdown snooze

**Confirmed configuration gap; runtime impact needs device testing.** Unlimited snooze (`maxSnoozes == 0`) enables AlarmKit's countdown path. The project contains one app target, no widget extension, and only an alert presentation. Apple's AlarmKit guide expects a widget extension for countdown presentation and warns that alarms may be unexpectedly dismissed or fail to alert without it.

Evidence: `IosAlarmScheduler.kt:144`, `AlarmKitWrapper.swift:577,602,630`, `iosApp/iosApp.xcodeproj/project.pbxproj` target list. [Apple's scheduling guide](https://developer.apple.com/documentation/alarmkit/scheduling-an-alarm-with-alarmkit).

- [x] Disable AlarmKit countdown for the first release; all configured snoozes go through the shared challenge screen. The iOS bridge test checks both limited and unlimited snooze configurations omit countdown.
- [ ] Verify no snooze, limited snooze, unlimited snooze, lock-screen countdown, relaunch, and cancellation. Ensure the UI matches where snooze is actually available.

### 5. Package the privacy declarations

**Confirmed packaging gap.** No `PrivacyInfo.xcprivacy` is present in the tracked iOS app or the freshly built Debug app bundle. Swift directly uses `UserDefaults` for metadata and pending handoffs; shared settings also persist preferences. Required-reason API use must be declared in the shipped app, even when data stays on device.

- [x] Add the app privacy manifest for app-only `UserDefaults` access (`CA92.1`) and verify it is in the built app bundle. Inspect the final archive and bundled dependency manifests for any other required-reason API categories before submission.
- [ ] Add an accessible in-app privacy-policy link and complete the App Store privacy questionnaire based on the actual iOS binary. Android Firebase behavior should not be copied into the iOS answers without checking: the iOS DI module currently uses `NoopAnalyticsTracker`.
- [ ] Confirm support/privacy URLs are live and appropriate for iOS.

References: [required-reason APIs](https://developer.apple.com/documentation/bundleresources/describing-use-of-required-reason-api), [App Store privacy information](https://developer.apple.com/help/app-store-connect/manage-app-information/manage-app-privacy), [App Review preparation](https://developer.apple.com/app-store/review/).

## P1 — complete before the release candidate

### 6. Make alarm handoff durable and acknowledged

**Source-backed risk, not a reproduced device failure.** Pending handoffs are checked on activation and once after a fixed 0.5-second launch delay. An intent that writes afterward has no direct delivery signal. A single UserDefaults string also cannot queue two alarms. Metadata is deleted before the challenge has acknowledged entry.

Normal challenge initialization calls `consumeDueOccurrence` → `showAlarm` → `scheduleNextAlarm`, which recreates recurring metadata. Therefore metadata loss is an interrupted/failed-handoff risk, not an unconditional failure of every second recurrence.

Evidence: `iOSApp.swift:47,73,136`, `AlarmKitWrapper.swift:89,155`, `shared/.../framework/NotificationSnooze.kt:8`.

- [x] Replace the single handoff string with a durable ordered queue, deduplicate identical payloads, migrate legacy pending data, and acknowledge only after the challenge initializes. A Foundation-only Swift smoke test covers queue/relaunch behavior.
- [x] Retain metadata when a recurring alarm opens the challenge; clean it up on definitive native cancellation/deletion.
- [ ] Give each AlarmKit delivery a stable occurrence identity, so two unacknowledged deliveries of the *same* recurring alarm cannot coalesce as identical payloads. Then test cold launch, app already open, two simultaneous alarms, lock/unlock, interrupted launch, and the next recurring delivery without editing the alarm.

### 7. Surface real authorization and recover on resume

**Confirmed integration gap.** `AlarmPermissionImpl.hasExactAlarmPermission()` always returns true. Notification status returns a cached value before its asynchronous refresh completes and is separate from AlarmKit authorization. Startup requests both types of permission. On resume the iOS app calls a one-time migration; once its flag is set, it does not call the existing general `rescheduleFutureAlarms.onAppResume()` recovery path. Native alarm updates are only logged.

Evidence: `framework/app/permission/AlarmPermissionImpl.kt:19`, `PlatformApis.ios.kt:26,97`, `iOSApp.swift:30,118`, `MainViewController.kt:44`, `AlarmKitWrapper.swift:372`.

- [x] Use AlarmKit authorization as the iOS permission state, request it when saving/enabling rather than on launch, and route denial through the existing permission dialog/Settings action. Verify the full deny → allow UI flow on device.
- [x] Scope the first release to iOS/iPadOS 26+ AlarmKit; local Time Sensitive notifications are legacy fallback code, not the supported delivery backend.
- [x] Run saved/native schedule reconciliation on activation and after handoff acknowledgment. Check each expected weekday/snooze registration so one surviving snooze cannot hide a missing regular alarm; skip an active challenge. Real timezone, revoke/regrant, and interrupted-write checks remain pending.
- [ ] Test deny → Settings allow, revoke after saving, retry a failed save, timezone travel, DST, and interrupted schedule writes. An enabled card must not falsely imply an OS alarm is armed.

### 8. Propagate cancellation failures

**Confirmed source defect.** Native cancel methods catch and print errors, while the Kotlin interface returns no result. Shared disable/delete/edit paths can therefore persist success even if the OS alarm remains registered. A failure in the cancellation loop also prevents attempting remaining registrations.

Evidence: `AlarmKitWrapper.swift:483,683,698`, `alarm/AlarmSchedulerBridge.kt:31`, shared `AlarmListViewModel.kt:114`.

- [x] Return native cancellation errors through the bridge. iOS attempts every matching AlarmKit registration and propagates any failure; shared callers retain their saved state or record a schedule error. A simulator test injects failures and verifies all weekday keys were attempted.
- [ ] Reconcile/retry failures on resume and confirm real native registrations disappear after cancellation.
- [ ] Verify disable/delete after snooze, edit old-time cleanup, delete/undo, and bulk deletion. Include injected cancellation failures and actual native registration checks.

### 9. Remove remaining platform mismatches

- [x] Use the public project page as the provisional iOS share destination; replace it with the App Store URL when the listing exists.
- [x] Anchor `UIActivityViewController`'s popover to its presenting view; visually test sharing from iPad before release. See [Apple's presentation requirements](https://developer.apple.com/documentation/uikit/uiactivityviewcontroller).
- [ ] Audit VoiceOver labels/focus, large text, contrast, dark mode, keyboard dismissal, safe areas, rotation, and the alarm editor/math screen on the chosen supported devices. These are unverified UI gates, not claimed visual defects.
- [ ] Check modal presentation/dismissal on device: the startup log reported an unbalanced Compose appearance-transition warning, although the initial What's New screen rendered without visible clipping.
- [ ] Localize native permission/action/tone strings to match supported languages, or explicitly scope the first release to English.
- [x] Remove unused `fetch` background mode and critical-alert usage copy. Retain the audio background mode for in-app challenge playback; verify its real behavior on device.
- [x] Activate the playback audio session when alarm playback begins rather than on ordinary app launch; test coexistence with music, calls, headphones, and Bluetooth.

### 10. Make the build and test pipeline cover the shipped app

The initial iOS CI job ran only nightly/manually and only ran Kotlin simulator tests. The new PR job also builds the Swift simulator app and runs a Foundation-only handoff smoke test. It still cannot exercise real AlarmKit delivery or validate a distribution archive. There is no Swift/XCTest/UI-test target yet.

- [x] Add a shared Xcode scheme and document local Xcode 27.0/JDK 21 prerequisites.
- [x] Add a PR macOS check for native shared tests and the unsigned Swift simulator app, plus an unsigned Release archive packaging check on manual workflow runs. The hosted `xcode-27` workflow has not yet run; signed distribution archive/export remains pending.
- [ ] Expand native integration coverage beyond the new durable-handoff smoke test to serialization, AlarmKit schedule construction, cancellation, and audio ownership. Keep the physical-device test gate for system behavior.
- [ ] Set the intended iOS marketing version/build number. They are currently hard-coded to `2.3.1` / `20`; a first iOS release does not have to restart at 1.0, but the choice should be deliberate and future uploads need increasing build numbers.
- [ ] Validate signing, distribution archive/export, bundled resources/manifests, and TestFlight processing. A successful unsigned build does not prove these steps.

## Physical-device acceptance checklist

Use the connected iPhone for AlarmKit, and an additional older-OS device only if that fallback remains supported. Record commit, build, OS, expected/observed delivery time, audio route/volume, permission state, and whether sound was actually heard. A log reporting playback start is not acoustic verification.

| Check | Required result |
| --- | --- |
| Clean install, permissions allowed/denied | Clear permission state; save succeeds only when a usable alarm can be scheduled |
| Alarm while foregrounded, backgrounded, locked, and app terminated | Correct real OS delivery; correct challenge opens |
| Silent switch and Focus | Documented behavior on the selected backend; audible AlarmKit alarm |
| Wrong answer, then all required correct answers | Audio continues while wrong; all audio/vibration stops on completion |
| Leave/kill app during handoff and challenge | Recoverable challenge state and an explicitly understood dismissal behavior |
| No snooze, finite limit, unlimited | Correct duration/count, no stranded countdown, subsequent delivery succeeds |
| Recurring delivery at least twice without editing | Both deliveries open the right challenge; next occurrence stays armed |
| Skip/Undo | Controls absent on iOS; a migrated saved skip does not leave a misleading date exception or suppress an alarm |
| Edit, disable, delete, undo | Old registrations disappear; cancelled alarms never ring |
| Two alarms together/close together | Neither handoff overwrites the other; completing one does not silence the other |
| Quotes/newline/emoji in title | No malformed JSON, crash, or lost challenge |
| Each tone, missing tone, Bluetooth/headphones, interrupted audio | Audible intended output/fallback and correct stop behavior |
| Real reboot, first unlock, overnight and several-day recurrence | Expected delivery verified; pre-first-unlock behavior documented separately |
| Timezone/DST/time change, permission revoke/regrant | Native state, saved state, and UI agree |
| Upgrade over existing development/TestFlight install | Alarms/preferences retained; migration does not duplicate or silence delivery |
| Small phone/large text/VoiceOver; iPad if supported | Usable complete flow and accessible controls |

## Store and launch checklist

- [ ] Confirm Apple Developer distribution access, bundle ID, App Store Connect record, and selected device/OS scope.
- [ ] Prepare app name/subtitle/description/keywords/category, age rating, copyright, support URL, and privacy-policy URL.
- [ ] Capture current screenshots for all device families/locales being shipped; ensure promises reflect iOS behavior.
- [ ] Complete privacy, export-compliance, availability/pricing, and other submission questions for the final binary.
- [ ] Supply concise review notes explaining permission setup, how to schedule a near-future alarm, the math challenge, snooze, and background audio.
- [ ] Process a signed Release build in TestFlight and run the acceptance checklist on that build, including an overnight soak.
- [ ] Decide how crashes and alarm failures will be investigated. TestFlight/App Store diagnostics may suffice initially; a new analytics SDK is not a prerequisite.

References: [Apple submission preparation](https://developer.apple.com/app-store/submitting/), [screenshots](https://developer.apple.com/help/app-store-connect/manage-app-information/upload-app-previews-and-screenshots).

## Recommended implementation order

1. Set OS/device scope and the dismissal/snooze contract.
2. Fix native scheduling/Skip next, audio ownership, safe handoff, and cancellation.
3. Fix authorization/recovery; complete or remove native countdown.
4. Run native integration and physical-device acceptance tests.
5. Finish privacy/build packaging, iOS polish, and store materials.
6. Validate a TestFlight release candidate and complete the overnight/multi-day checks before submission.

## Command results

### iOS release branch, first implementation pass

- Branch: `codex/ios-first-release`. The branch retains prior uncommitted iOS launch/preview work. Unrelated IDE, Kotlin error log, Android mapping, and upgrade-test files remain outside these edits.
- Skip next is disabled on iOS because AlarmKit weekly recurrence has no start date or single-date exception. A one-time replacement would not rearm indefinitely if the app stayed closed. Existing iOS skip records are cleared and rescheduled during migration.
- Swift and Kotlin audio starts/stops now share `IosAlarmAudioManager`; handoff payloads use one JSON serializer and navigation rejects malformed, deleted, disabled, or duplicate alarm entries. AlarmKit's countdown is disabled until a widget extension and native snooze contract exist.
- AlarmKit cancellation errors now reach Kotlin. Each weekday cancellation is attempted even if another fails. The built simulator app includes a valid privacy manifest for app-only `UserDefaults` access.
- The Info.plist device capability was updated from obsolete `armv7` to `arm64` for the iOS/iPadOS 26+ release; final archive inspection remains pending.
- Shared tests: **187 iOS simulator and 180 Android host tests passed** (zero failures) after the implementation pass. Full iOS simulator app build passed. Later final builds also include the Info.plist change. Logs: `/tmp/mathalarm-ios-cancellation-tests.log`, `/tmp/mathalarm-ios-first-release-build.log`.

### Follow-up: handoff, recovery, platform polish, and CI

- The handoff queue survives relaunch and advances only after challenge initialization; a Swift smoke test passed. Recurring native metadata is retained until cancellation. A distinct identity for each delivery of the same recurring alarm remains open.
- AlarmKit authorization is queried and requested on demand. App activation reconciles expected weekday/snooze registrations without cancelling an active challenge. A simulator test covers a missing weekday masked by a surviving snooze. Denial, revocation, timezone travel, and the resumed UI still require physical tests.
- The iPad share sheet has a popover anchor; the iOS share destination is the public project page until an App Store URL exists. Unused background fetch/critical-alert declarations and startup audio-session activation were removed.
- A shared Xcode scheme and PR iOS CI job now build the Swift simulator app and run native tests plus a Swift handoff smoke test. Local verification: **322 iOS tests** (133 core, 189 shared) and **314 Android host tests** (134 core, 180 shared), all passing. The full unsigned simulator build and signed iPhone Debug build passed. The built app contains a valid privacy manifest, requires iOS 26.0, and declares iPhone/iPad families. The updated app launched on the iPad simulator in dark mode. CI itself, archive/TestFlight, and full iPad interaction remain unverified. Logs: `/tmp/mathalarm-ios-final-tests.log`, `/tmp/mathalarm-ios-final-build.log`, `/tmp/mathalarm-ios-final-device-build.log`; iPad screenshot: `/tmp/mathalarm-ipad-final.png`.
- The first device install attempt lost its CoreDevice connection (`IXRemoteErrorDomain` code 6); a retry **installed the signed Debug build successfully**. A subsequent CLI launch could not establish the remote XPC service (`CoreDeviceError` 4000). The revision is installed, but this attempt does not verify startup or physical alarm delivery; check it with the iPhone unlocked/awake before interpreting device results.
- An unsigned **Release archive succeeded** locally. Its app bundle has a valid `PrivacyInfo.xcprivacy`, minimum OS 26.0, and iPhone/iPad families `[1,2]`; no bundled dynamic frameworks appeared in the archive. The workflow now checks the same archive packaging on manual runs. This does not establish distribution signing, export, App Store validation, or TestFlight processing. Log: `/tmp/mathalarm-ios-final-archive.log`.
- Physical tests still needed for audible playback/vibration, recurring AlarmKit delivery, cancellation, and the sheet/preview return behavior. A simulator build cannot establish those outcomes.

### Follow-up: launch artwork and Test Alarm presentation

- Raised Debug/Release deployment targets to 26.0 and retained iPhone/iPad device families. Confirmed the built device app reports `MinimumOSVersion = 26.0` and `UIDeviceFamily = [1,2]`.
- Regenerated the iOS launch marks transparently from the existing vector master. Removed the generator's hard-coded white background, added `--ios-launch-only`, and connected the launch storyboard to a light/dark color asset. All three PNG sizes have a transparent corner pixel.
- Compact iOS Test Alarm now uses a full-screen navigation scene instead of placing a Compose dialog beneath the native UIKit sheet. The editor stays on the navigation back stack so its draft can be restored on return. Android retains its dialog overlay; expanded iPad windows retain the list/detail presentation.
- Added a regression test for hiding the sheet presentation while retaining the editor return destination. **183 iOS shared tests and 177 Android shared tests passed**, with zero failures/errors/skips.
- The iPad simulator Debug build and signed iPhone Debug build passed. Installed/launched both; inspected the initial iPad Pro 11-inch screen in dark mode. Installed and opened the update on the connected iPhone for user verification.
- Full interactive iPad preview/return checks remain pending: Device Hub repeatedly timed out through UI automation. A physical-iPhone check of sheet → Test Alarm → close → retained draft was requested from the user. The simulator launch recording did not clearly expose the launch mark, so asset correctness should not be confused with completed light/dark cold-launch visual verification.
- Logs: `/tmp/mathalarm-ios-ui-fixes-build.log`, `/tmp/mathalarm-ios-ui-fixes-tests.log`, `/tmp/mathalarm-ios-device-fixes-build.log`. Initial iPad screenshot: `/tmp/mathalarm-ipad-ui-fixes.png`.

### Initial review baseline

Fresh native tests passed: **132 core + 182 shared = 314 tests**, zero failures/errors/skips. The shared total includes six dedicated iOS adapter tests. The first invocation reused test results; the counts here were verified after forcing fresh execution of the test tasks:

```sh
./gradlew :core:iosSimulatorArm64Test --rerun :shared:iosSimulatorArm64Test --rerun --continue --console=plain
```

The marketing icon is 1024×1024 without alpha. All five bundled CAF tones were inspected and are 4.8–7.4 seconds long.

The signed Debug device build succeeded with automatic provisioning, then installed successfully:

```sh
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -destination 'id=<connected-device-UDID>' \
  -derivedDataPath /tmp/mathalarm-ios-device-review -allowProvisioningUpdates build
```

`codesign --verify --deep --strict` passed with system trust-service access. The initial iPhone launch was blocked by developer trust; after the user trusted the app, launch succeeded. A 25-second console capture showed successful startup and no immediate crash, then ended at the requested command timeout. Screenshot evidence: `/tmp/mathalarm-ios-startup.png` (also copied to `build/ios-release-review/startup.png`). This was an upgrade over the existing development install, not a clean-install permission test.

The unsigned Release build also succeeded:

```sh
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Release \
  -destination 'generic/platform=iOS' -derivedDataPath /tmp/mathalarm-ios-review \
  CODE_SIGNING_ALLOWED=NO build
```

Both Debug and Release app bundles contain no privacy manifest. A distribution-signed archive, App Store validation, and TestFlight processing remain untested. Compiler warnings include the inferred Kotlin framework bundle ID, an imported Compose type mapping, and the always-running Kotlin build phase; they did not fail either build.

Local logs: `/tmp/mathalarm-ios-device-build.log`, `/tmp/mathalarm-ios-release-build.log`, `/tmp/mathalarm-ios-tests-fresh.log`, and `/tmp/mathalarm-ios-launch.log`.

### Follow-up: Maestro simulator interaction, 26 September

- Used Maestro 2.10.0 on iPhone 17 Pro and iPad Pro 11-inch (M5), iOS/iPadOS 26.3. Added repeatable flows in `scripts/maestro/ios/` and a detailed report in [ios-maestro-validation-2026-09-26.md](ios-maestro-validation-2026-09-26.md).
- iPhone sheet → Test Alarm → Cancel and correct completion both restore a draft with quotes intact. Wrong-answer feedback, tone selection/preview state, and editor dismissal passed on both iPhone and iPad portrait. iPad native share-popover presentation/dismissal also passed.
- iPad landscape preview return remains unresolved: Maestro reports tapping Cancel, but the editor did not reappear. The native-bar-overlap diagnosis was withdrawn and its proposed change reverted; no application source change was retained. Confirm the actual interaction before choosing a fix.
- Launch artwork visual verification remains open: the launch recording did not show the mark clearly enough. Check dark iPhone navigation-title contrast as well.
- Simulator UI checks do not establish audible playback/vibration, real AlarmKit registration/delivery, recurrence, permissions, or interruption recovery. Those physical-device and App Store gates remain open.

### Follow-up: transient scheduling error after a physical recurring alarm

- The user exercised the Wed/Sat alarm on the connected iPhone, tapped Solve Math, and completed the challenge. A scheduling-failure message briefly appeared, then cleared. A read-only copy of the device database showed the alarm enabled with Wednesday/Saturday future timestamps, no active challenge/snooze, and `scheduleError = null`. This verifies saved state, not the current OS registrations. The original native error was not retained and its exact cause is unconfirmed.
- The delivery path (`consumeDueOccurrence` → `ShowAlarm` → `ScheduleNextAlarm`) was resubmitting every selected weekly registration. Added a separate advancement hook: iOS retains each existing AlarmKit weekday registration and installs only a missing one. Explicit saves/edits continue submitting updated settings; Android uses its existing rearm behavior. Real failures repairing missing registrations still propagate and remain visible.
- Regression tests cover two existing weekdays without any new schedule request, repair of only the missing weekday while preserving snooze, propagation of repair failure, explicit metadata edits, and routing delivery through the advancement hook. This removes unnecessary native scheduling during handoff; a physical repeat test is still needed to confirm the reported message no longer appears.

- Verification: **327 iOS tests** (134 core, 193 shared) and **315 Android host tests** (135 core, 180 shared) passed with zero failures/errors/skips. The signed Debug device build passed, installed, and launched on the connected iPhone. Startup diagnostics confirmed authorization and three actual AlarmKit registrations in `scheduled` state: Friday 07:57 and Wednesday/Saturday 23:24. The reported alarm's future native weekly registrations are present; this startup check does not reproduce its delivery/handoff. Logs: `/tmp/mathalarm-recurring-retain-tests.log`, `/tmp/mathalarm-recurring-retain-device-build.log`, `/tmp/mathalarm-recurring-retain-device-launch.log`.

### Follow-up: physical iPhone Mirroring validation, 27 September

- Computer Use successfully operated Math Alarm through iPhone Mirroring on the connected iPhone 11, iOS 26.3.1(a). The mirrored app exposes little accessibility content, so interactions used screenshot coordinates.
- Created a temporary `CUA recurring test` alarm for 00:09 on Sunday/Wednesday and backgrounded the app. Mirroring disconnected when the user used the phone around delivery; the user confirmed the alarm rang. After reconnecting, the alarm remained enabled, showed Wednesday 30 September as its next occurrence, and displayed no scheduling error. The agent did not observe or operate the real delivered alert/challenge while disconnected.
- A read-only saved-data snapshot after delivery showed two future occurrence timestamps for the temporary alarm, with no active challenge, snooze, or scheduling error. This establishes saved state, not native registration or completion of the delivered challenge.
- Agent-operated physical Test Alarm checks passed: the challenge occupied the full screen; an incorrect answer showed feedback and kept it open; the correct answer returned to the editor. Selecting Digital changed the tone preview control to Stop, and stopping restored Play. Cancelling another Test Alarm returned to the editor with the unsaved Digital selection retained.
- Closed the editor without saving the tone change and disabled the temporary alarm through the UI. Left it available for reuse and preserved the four pre-existing alarms. The app was left on the alarm list. The saved-data snapshot predates this final disable action.
- Still open: observed real alert → Solve Math → completion, actual snooze delivery, sound/vibration assessment, interrupted challenge recovery, permission denial/revocation, and the remaining iPad/release checks. This session does not establish the complete physical acceptance matrix or conclusively reproduce the earlier transient scheduling failure.
