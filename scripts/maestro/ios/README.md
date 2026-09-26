# iOS simulator validation

Use Maestro 2.10.0 or later, Xcode, and an installed Math Alarm Debug build on an iOS/iPadOS 26+ simulator. Flows expect English and default new-alarm settings (Easy, one question, Classic tone). They create an unsaved draft, test it, and dismiss it; they do not clear app data or modify existing saved alarms.

```sh
MAESTRO_CLI_NO_ANALYTICS=1 maestro --device <simulator-UDID> test \
  --test-output-dir build/ios-maestro/<run-name> scripts/maestro/ios/editor-preview.yaml
```

Run `editor-preview.yaml` on iPhone portrait and iPad portrait. It checks sheet dismissal, quoted-title draft retention after cancelling and completing a preview, wrong-answer feedback, tone selection, and editor dismissal. It also asserts that Skip next is absent in the editor; that assertion does not inspect saved-alarm menus.

`ipad-share.yaml` opens the iPad native share popover and captures it. It does not send the share content or select an external destination. The simulator should be in portrait and the app on the alarm list when starting. Native toolbar accessibility labels are supplied by UIKit and may change with iOS versions.

Screenshots are relative to Maestro's output folder. Use the keyboard's Enter/Done action; Maestro's `hideKeyboard` failed with the Compose editor in this environment.

Landscape preview return remains unresolved in the September 26 run. Do not treat a successful `tapOn` command as evidence that the intended control received the tap, or an accessibility visibility assertion as proof that it is visually unobstructed.

These flows do not establish physical AlarmKit delivery, audible playback, vibration, permissions, lock-screen behavior, recurrence, or background recovery. See the release checklist and validation report.
