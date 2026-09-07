#!/usr/bin/env bash
set -euo pipefail
serial=emulator-5554
adb -s "$serial" install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb -s "$serial" install -r androidApp/build/outputs/apk/androidTest/debug/androidApp-debug-androidTest.apk
scenarios=(cold-start snooze)
if [[ "${EXTENDED:-false}" == true ]]; then
  scenarios+=(doze reboot)
fi
status=0
for scenario in "${scenarios[@]}"; do
  python3 scripts/test_alarm_delivery.py --serial "$serial" --scenario "$scenario" \
    --output "build/alarm-device-results/$scenario" || status=1
done
mkdir -p build/alarm-device-results/resume
resume_report=build/alarm-device-results/resume/instrumentation.txt
adb -s "$serial" shell input keyevent KEYCODE_WAKEUP
adb -s "$serial" shell wm dismiss-keyguard
adb -s "$serial" shell am instrument -w -r \
  -e class com.timilehinaregbesola.mathalarm.AlarmResumeRecoveryTest,com.timilehinaregbesola.mathalarm.AlarmErrorPresentationTest,com.timilehinaregbesola.mathalarm.AlarmDeletionLifecycleTest \
  com.timilehinaregbesola.mathalarm.debug.test/androidx.test.runner.AndroidJUnitRunner \
  > "$resume_report" || status=1
cat "$resume_report"
# adb can return success even when an instrumentation assertion fails.
grep -q 'OK (5 tests)' "$resume_report" || status=1
exit "$status"
