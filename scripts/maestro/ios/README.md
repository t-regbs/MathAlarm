# Native iOS simulator validation

The implemented app is SwiftUI. For current verification, use the pinned Maestro
2.10.0, Xcode 27.0 and a disposable English simulator with the production Debug app
from the `MathAlarmNative` scheme:

```sh
M7_UDID=DISPOSABLE_UDID MAESTRO_BIN=/path/to/2.10.0/maestro \
  bash scripts/ci_native_client.sh
```

The [permanent M7 entry point](#permanent-m7-ci-entry-point) and native flows below
describe the current system. Run `bash scripts/ci_native_ios.sh` for the complete
KMP/XCTest/UI, locale, native-client and packaging pipeline. Its controlled scheduler
does not establish physical AlarmKit reliability. See the
[current evidence record](../../../docs/native-ui-migration-milestone-7-2026-10-03.md).

## Historical September renderer-era flows

This section records the pre-migration flows and their original limitations. Use the
native entry point above for M7 acceptance. The historical setup used Maestro 2.10.0,
Xcode and an installed Debug build; it expected English and default new-alarm settings
(Easy, one question, Orbit tone).

```sh
MAESTRO_CLI_NO_ANALYTICS=1 maestro --device <simulator-UDID> test \
  --test-output-dir build/ios-maestro/<run-name> scripts/maestro/ios/editor-preview.yaml
```

Run `editor-preview.yaml` on iPhone portrait and iPad portrait. It checks sheet dismissal, quoted-title draft retention after cancelling and completing a preview, wrong-answer feedback, tone selection, and editor dismissal. It also asserts that Skip next is absent in the editor; that assertion does not inspect saved-alarm menus.

`sound-library.yaml` checks preview without selection, automatic preview completion, Back, Done, draft retention, and reopening. The library is a page in the existing alarm editor sheet. Run it on iPhone and iPad; it leaves no saved alarm.

`ipad-share.yaml` opens the iPad native share popover and captures it. It does not send the share content or select an external destination. The simulator should be in portrait and the app on the alarm list when starting. Native toolbar accessibility labels are supplied by UIKit and may change with iOS versions.

Screenshots are relative to Maestro's output folder. Use the keyboard's Enter/Done action; Maestro's `hideKeyboard` failed with the Compose editor in this environment.

Landscape preview return remains unresolved in the September 26 run. Do not treat a successful `tapOn` command as evidence that the intended control received the tap, or an accessibility visibility assertion as proof that it is visually unobstructed.

These flows do not establish physical AlarmKit delivery, audible playback, vibration, permissions, lock-screen behavior, recurrence, or background recovery. See the release checklist and validation report.

## Native Milestone 6 flows

The paragraphs above retain the September renderer-era checks and limitations.
Current SwiftUI flows are `native-m6-settings.yaml`, `native-m6-share.yaml`, and
`native-m6-runtime.yaml` (portrait keyboard, permission guard, landscape return).
They use native keyboard Done identifiers and preserve unsaved drafts. Run only
on disposable English simulators with no configured email account and undetermined
or denied AlarmKit permission. Sharing cancels without selecting a destination.
The optional OS denial branch must be reported as skipped when no system alert appears.
The maths keyboard flow also checks negative input, the answer field's clear action,
question/answer/Submit visibility with the software keyboard, inline incorrect-answer
feedback and its explicit dismissal. Portrait requires the whole task to be visible;
landscape scrolls to controls in its shorter viewport.
The landscape wrapper supplies `CHALLENGE_ORIENTATION` after app launch and sets
`REQUIRE_VISIBLE_TASK=false`; standalone keyboard runs default to portrait.
Inspect the rendered orientation or EXIF metadata, since native landscape captures
can retain portrait pixel dimensions with rotation metadata.
See [testing instructions](../../../docs/testing.md) and the
[Milestone 6 evidence](../../../docs/native-ui-migration-milestone-6-2026-10-02.md)
for actual runtime results and unresolved VoiceOver, physical and release gates.

`native-m6-permission-denial.yaml` checks that the first Save requests authorization
directly. An unsuccessful request offers Allow alarms, with no premature Settings link;
an explicit retry does not immediately repeat the dialog. Denial opens the permission
guide with the Apps → Math Alarm → Alarms route and one Go to Settings action. This flow
closes the guide, retains the draft and discards it. The optional system denial branch
remains conditional; never grant permission in this flow. The M7 capability probe
reported PiP unavailable on iPhone and available on iPad; neither establishes playback
over Settings, which remains a physical-client check.

`native-m6-disabled-crud.yaml` creates a disabled fixture, returns from a preview to
the same draft, saves, deletes, undoes and deletes it again. Supply a unique title
containing only letters, numbers and underscores, because selectors interpolate it
as a regular expression:

```sh
MAESTRO_CLI_NO_ANALYTICS=1 maestro --device <disposable-simulator-UDID> test \
  -e FIXTURE_TITLE=M6_disabled_UNIQUE_RUN_ID \
  --test-output-dir build/native-ui-m6/iphone/maestro-crud \
  scripts/maestro/ios/native-m6-disabled-crud.yaml
```

Never target an existing alarm or enable the fixture. The final delete removes the
saved fixture, and the disabled save does not accept an AlarmKit registration.

`native-m6-os-accessibility.yaml` uses native Settings controls on a disposable
English **402-point iPhone**. Both Reduce Motion and Reduce Transparency must start
**Off**. It asserts/captures them On, captures the native list/settings/editor, then
restores and asserts both Off. The switch hit targets depend on that screen width.
If interrupted after enabling either setting, restore its original Off state in
Settings before reusing the simulator. Report actual switch/app captures and
restoration; this is not VoiceOver evidence or an all-screen reduced-effects matrix.

`native-m6-sound.yaml` starts from the foreground ordinary English list with no
active draft and OS maximum accessibility text already selected. It scrolls to all
six tone and preview controls without playback, applies Clear Signal to the unsaved
draft, checks its summary/title, then discards. Record and restore the original OS
text category afterward. This client check supplements intermediate harness scroll
captures; a `*-bottom` filename alone does not prove the final row is visible.

The nine-locale presentation runner is separate from Maestro:

```sh
python3 -B scripts/verify_ios_native_presentation.py --udid <disposable-simulator-UDID> \
  --app /absolute/DerivedData/Build/Products/Debug-iphonesimulator/MathAlarm.app \
  --output build/native-ui-m6/iphone/presentation
```

By default it runs the base phase followed by a separate delivered phase in fresh
language/region processes for each of the nine locales. Use `--base-only` or
`--delivered-only` to select a phase, and `--locales en` for focused iteration.
The opt-in Debug harness starts once per process in an independent task rather than
a cancellable SwiftUI view task. The base phase saves disabled fixtures; the delivered
phase uses a controlled Debug scheduler, private handoff queue and no-op native
recovery hook, with no OS alarm registration. It captures delivered progress/feedback
and the retained draft after authoritative resolution. Both phases delete only their
exact fixture IDs and acknowledge production results in the same process with owners
still mounted, then restore presentation preferences before teardown.

Review PNGs and JSON alongside per-phase completion logs. The runner copies only
names emitted by the current process, excluding stale optional viewport pictures.
Final matrix/client results and review are recorded in the linked M6 record;
controlled delivery does not establish physical AlarmKit delivery, audible output,
VoiceOver speech/focus, interactive window resizing or release readiness.

## Permanent M7 CI entry point

`M7_UDID=DISPOSABLE_UDID MAESTRO_BIN=/path/to/2.10.0/maestro bash scripts/ci_native_client.sh`
runs settings/announcement persistence, share cancellation, maths keyboard and landscape,
permission guard, stable Back/Cancel, unique disabled CRUD and all six tone controls at
maximum OS text size. It restores the original text category. It selects no share target,
sends no feedback, and uses only disposable app data. The optional OS permission denial
branch still must be reported as skipped when no system alert appears.

The permanent `MathAlarmNative` XCTest scheme separately verifies all 27 production
groups, once-only permission/Settings return/save, tutorial interruption/audio leases,
mounted owners, durable commands and exact acknowledgements, including fresh-process
restoration. The nine-locale matrix retains the controlled next-alarm subtitle assertions.
`native_screenshot_review.py` builds the offline comparison index; humans/agents must
inspect pixels before recording visual approval.

### Remaining native accessibility/client evidence

Record runtime, device, orientation/window size, text size, expected/observed behavior
and evidence paths for each case. Do not convert a source/AX-tree inspection to a pass.

- VoiceOver: enable on a supported client; traverse list, editor/nested controls, tones,
  permission guide, maths Answer/Clear/Submit, error feedback and settings. Record spoken
  labels, order, focus after Back/Cancel, wrong-answer announcement and return to the same
  draft. Restore the prior setting. A successful XCTest accessibility lookup is not speech.
- iPad live resizing: while editing a named unsaved draft and nested route, resize the
  actual app window across compact/expanded widths; verify title, staged tone, navigation
  and draft survive, then discard. Layout replacement tests and orientation changes are
  separate evidence, not interactive resizing.
- Configured native Mail: on an isolated configured client, open feedback, inspect the
  intended recipient and empty subject/body, cancel and delete the unsent draft. Verify
  return to settings; exercise failure only through a controlled client mechanism. Never
  send. An unavailable-handler alert does not establish composer behavior.

Physical alarm/audio/PiP and distribution acceptance remain M8, independently of archive
packaging. Consult the current milestone record before marking any manual gate complete.

The maximum-text tone flow requires 100% visibility of each preview control. The final
Clear Signal row does not require centering: a list can reach its bottom scroll limit
with that row fully visible. Selection, exact-draft application and discard remain asserted.
