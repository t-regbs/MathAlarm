#!/usr/bin/env bash
# Dedicated CI emulator only; changes restored even after a failing assertion.
set -euo pipefail
serial="${ANDROID_SERIAL:-emulator-5554}"
out="${ANDROID_UI_OUTPUT:-build/alarm-device-results/native-ui}"
mkdir -p "$out"
# UI layout tests assume permissions, as in the existing delivery fixture.
# Authorization behavior has separate tests; a first-install OS modal hides Compose.
if (( $(adb -s "$serial" shell getprop ro.build.version.sdk | tr -d '\r') >= 33 )); then
  adb -s "$serial" shell pm grant com.timilehinaregbesola.mathalarm.debug android.permission.POST_NOTIFICATIONS
fi
adb -s "$serial" shell input keyevent KEYCODE_WAKEUP
adb -s "$serial" shell wm dismiss-keyguard
restore() { adb -s "$serial" shell wm size reset; adb -s "$serial" shell wm density reset; }
trap restore EXIT
adb -s "$serial" shell wm density 160
for mode in expanded compact; do
  if [[ "$mode" == expanded ]]; then
    adb -s "$serial" shell wm size 1280x800
    methods='com.timilehinaregbesola.mathalarm.TabletPaneLayoutTest#editorAndSettingsShareTheWindowAndProtectDrafts,com.timilehinaregbesola.mathalarm.TabletPaneLayoutTest#hiddenDraftRemainsProtectedAfterActivityRecreation'
    count=2
  else
    adb -s "$serial" shell wm size 540x960
    methods='com.timilehinaregbesola.mathalarm.TabletPaneLayoutTest#compactWindowKeepsTheFamiliarSheet'
    count=1
  fi
  adb -s "$serial" shell am instrument -w -r -e class "$methods" \
    com.timilehinaregbesola.mathalarm.debug.test/androidx.test.runner.AndroidJUnitRunner > "$out/$mode.txt"
  cat "$out/$mode.txt"
  grep -q "OK ($count test" "$out/$mode.txt"
  # Instrumentation exit 0 does not imply success; assumption skips are failures here.
  ! grep -Eq 'FAILURES|INSTRUMENTATION_FAILED|INSTRUMENTATION_STATUS_CODE: -[1234]' "$out/$mode.txt"
done
