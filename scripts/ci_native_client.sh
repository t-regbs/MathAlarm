#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${M7_UDID:?Set a disposable simulator UDID}"
out="${M7_OUTPUT:-build/native-ui-m7-client}"
maestro="${MAESTRO_BIN:-maestro}"
export MAESTRO_CLI_NO_ANALYTICS=1
mkdir -p "$out"
"$maestro" --version | tee "$out/maestro-version.txt"
grep -q '2.10.0' "$out/maestro-version.txt"
size=$(xcrun simctl ui "$M7_UDID" content_size)
printf '%s\n' "$size" > "$out/content-size-original.txt"
restore() {
  xcrun simctl ui "$M7_UDID" content_size "$size"
  xcrun simctl ui "$M7_UDID" content_size > "$out/content-size-restored.txt"
}
trap restore EXIT
for flow in native-m6-settings native-m6-share native-m6-runtime native-editor-back-cancel; do
  "$maestro" --device "$M7_UDID" test --test-output-dir "$out/$flow" "scripts/maestro/ios/$flow.yaml" > "$out/$flow.log" 2>&1
done
"$maestro" --device "$M7_UDID" test -e "FIXTURE_TITLE=M7_disabled_$(date +%s)" \
  --test-output-dir "$out/crud" scripts/maestro/ios/native-m6-disabled-crud.yaml > "$out/crud.log" 2>&1
xcrun simctl ui "$M7_UDID" content_size accessibility-extra-extra-extra-large
xcrun simctl ui "$M7_UDID" content_size > "$out/content-size-maximum.txt"
xcrun simctl launch "$M7_UDID" com.timilehinaregbesola.mathalarm
"$maestro" --device "$M7_UDID" test --test-output-dir "$out/sound-maximum" scripts/maestro/ios/native-m6-sound.yaml > "$out/sound-maximum.log" 2>&1
