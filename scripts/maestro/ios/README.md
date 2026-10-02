# iOS simulator validation

Use Maestro 2.10.0 or later, Xcode, and an installed Math Alarm Debug build on an iOS/iPadOS 26+ simulator. Flows expect English and default new-alarm settings (Easy, one question, Orbit tone). They create an unsaved draft, test it, and dismiss it; they do not clear app data or modify existing saved alarms.

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
See [testing instructions](../../../docs/testing.md) and the
[Milestone 6 evidence](../../../docs/native-ui-migration-milestone-6-2026-10-02.md)
for actual runtime results and unresolved VoiceOver, physical and release gates.

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
