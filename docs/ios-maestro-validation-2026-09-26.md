# iOS simulator validation — 26 September 2026

Tested the session build from `70b3015` on iPhone 17 Pro and iPad Pro 11-inch (M5), both running iOS/iPadOS 26.3. Used Maestro 2.10.0 from an isolated `/tmp` installation; the existing 1.33.1 installation was left unchanged. Existing saved alarms were preserved. No application source changes were retained in this validation pass.

## Confirmed results

| Check | Result | Evidence / limit |
| --- | --- | --- |
| iPhone editor → Test Alarm → Cancel | Passed | The sheet is absent during the challenge; the quoted-title draft returns intact. |
| iPhone wrong answer → correct answer | Passed | Wrong-answer feedback appears; correct answer closes preview and restores the draft. |
| iPhone tone selection | Passed | Selecting Digital enters preview state; Done closes the picker and retains Digital and the draft. This is UI evidence, not an audible-playback check. |
| iPhone editor dismissal | Passed | Dismiss returns directly to the list. Existing disabled alarm `Testing` remains present. |
| iPad portrait editor, challenge, tone, and dismissal | Passed | Complete repeatable flow passed: quoted draft survives cancel and correct completion, wrong-answer feedback appears, Digital selection persists, and Dismiss returns to the list. |
| iPad native sharing | Passed | Share opens an anchored native popover, captured visually, then dismissal returns to App Settings without a crash. Nothing was shared. |
| Skip next in new-alarm editor | Passed | The control is absent. Saved-alarm menus were not covered by this assertion. |

The complete iPhone flow passed at `build/ios-maestro/2026-09-26/phone-suite/2026-09-26_224941/editor-preview/`. The complete iPad portrait flow passed at `build/ios-maestro/2026-09-26/ipad-suite/2026-09-26_225151/editor-preview/`. Final iPad share artifacts are at `build/ios-maestro/2026-09-26/ipad-share/2026-09-26_225112/ipad-share/`. Each folder contains commands, logs, screenshots, and an artifact manifest.

## Unresolved observations

- **iPad landscape return:** launching the app, opening the editor, and entering Test Alarm succeeded. Maestro found Cancel in accessibility and reported its tap completed, but the subsequent editor assertion failed and the challenge remained on screen. Artifacts: `build/ios-maestro/2026-09-26/ipad-landscape/2026-09-26_224449/`. The cause is unconfirmed; this is not evidence sufficient to diagnose a native-bar overlap or to change navigation architecture. A speculative navigation change was fully reverted and never installed on the tested simulators. An earlier landscape run started on the home screen and is not an app-regression result.
- **Splash:** a terminated-app dark launch recording did not expose the launch artwork clearly enough to verify it. The previous transparent-asset checks still stand; light/dark cold-launch visual verification remains open. Recording and contact sheet: `build/ios-maestro/2026-09-26/launch/`.
- **Dark iPhone title contrast:** the captured alarm-list title appears dark against the dark background. Recheck this visually on the phone and across theme transitions before release; no appearance fix was made here.

## Test corrections and limits

Maestro's generic `hideKeyboard` failed; Enter/Done worked. Absolute screenshot paths were rejected by 2.10.0, so committed flows use relative names. Share is labelled `Share` in the settings UI, not `Share Math Alarm`. The native popover exposes `dismiss popup` to Maestro but not its individual actions; Copy was confirmed in the screenshot, rather than through an accessibility assertion. Editor Dismiss has no discard-confirmation dialog; the flow now expects the list directly. These initial test failures must not be counted as app defects.

Repeatable flows and setup are in `scripts/maestro/ios/README.md`. The editor flow does not save or schedule an alarm. A quoted editor title tests draft preservation, not Swift-to-Kotlin alarm handoff serialization.

Physical AlarmKit delivery, recurrence, notification/stop-intent entry, handoff recovery after termination, permissions, audible playback, vibration, and real snoozing remain release gates. The existing native unit tests and build/archive checks cover their respective code and packaging; these simulator UI results do not replace device delivery tests or App Store/TestFlight validation.
