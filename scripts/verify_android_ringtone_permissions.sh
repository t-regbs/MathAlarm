#!/usr/bin/env bash
set -euo pipefail
serial="${1:?Usage: verify_android_ringtone_permissions.sh emulator-SERIAL (dedicated API26..32 emulator with app and test APKs installed)}"
case "$serial" in
  emulator-*) ;;
  *) echo 'Pass an explicit dedicated emulator serial.' >&2; exit 2 ;;
esac
out="${ANDROID_UI_OUTPUT:-build/alarm-device-results/ringtone-permissions}"
package='com.timilehinaregbesola.mathalarm.debug'
permission='android.permission.READ_EXTERNAL_STORAGE'
sdk="$(adb -s "$serial" shell getprop ro.build.version.sdk | tr -d '\r')"
if (( sdk < 26 || sdk > 32 )); then
  echo "Storage permission verification requires API26..32, found API$sdk." >&2
  exit 2
fi
adb -s "$serial" shell pm path "$package" | grep -q '^package:'
adb -s "$serial" shell pm path "$package.test" | grep -q '^package:'
mkdir -p "$out"
reset_permission() {
  adb -s "$serial" shell am force-stop "$package"
  adb -s "$serial" shell pm revoke "$package" "$permission"
}
cleanup() {
  result=$?
  trap - EXIT
  if ! reset_permission; then
    echo 'Could not restore the denied storage permission fixture.' >&2
    result=1
  fi
  exit "$result"
}
# Revoking this permission kills the target process, so cleanup must outlive instrumentation.
trap cleanup EXIT
reset_permission
adb -s "$serial" shell input keyevent KEYCODE_WAKEUP
adb -s "$serial" shell wm dismiss-keyguard
adb -s "$serial" shell am instrument -w -r \
  -e class com.timilehinaregbesola.mathalarm.RingtonePermissionDeviceTest \
  "$package.test/androidx.test.runner.AndroidJUnitRunner" > "$out/result.txt"
cat "$out/result.txt"
grep -q '^OK (1 test)' "$out/result.txt"
! grep -Eq 'FAILURES|INSTRUMENTATION_FAILED|INSTRUMENTATION_STATUS_CODE: -[1234]' "$out/result.txt"
