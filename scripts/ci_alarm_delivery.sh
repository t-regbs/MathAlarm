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
exit "$status"
