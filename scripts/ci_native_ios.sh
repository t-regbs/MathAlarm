#!/usr/bin/env bash
# Dedicated/disposable simulator only. No physical delivery or distribution claim.
set -euo pipefail
cd "$(dirname "$0")/.."
out="${M7_OUTPUT:-build/native-ui-m7-ci}"
mkdir -p "$out"
out=$(cd "$out" && pwd)
derived="$out/xcode"
project=(-project iosApp/iosApp.xcodeproj -scheme MathAlarmNative -derivedDataPath "$derived" -disableAutomaticPackageResolution CODE_SIGNING_ALLOWED=NO)
python3 -B scripts/verify_ios_release.py
python3 -B scripts/verify_native_ui_boundaries.py
python3 -B scripts/verify_native_localization.py
./gradlew :core:iosSimulatorArm64Test :shared:iosSimulatorArm64Test --continue > "$out/kotlin-ios.log" 2>&1
./gradlew :shared:dependencies --configuration iosSimulatorArm64CompileKlibraries > "$out/ios-dependencies.log"
./gradlew :shared:dependencies --configuration iosArm64CompileKlibraries > "$out/ios-device-dependencies.log"
swiftc iosApp/iosApp/PendingDeeplinkStore.swift iosApp/tests/PendingDeeplinkStoreSmoke.swift -o "$out/queue-smoke"
"$out/queue-smoke" > "$out/queue-smoke.log"
# Exact runtime/device identifiers are explicit and recorded in each run.
runtime="${M7_RUNTIME:-com.apple.CoreSimulator.SimRuntime.iOS-27-0}"
device="${M7_DEVICE:-com.apple.CoreSimulator.SimDeviceType.iPhone-17}"
udid=$(xcrun simctl create 'MathAlarm M7 CI disposable' "$device" "$runtime")
cleanup() { xcrun simctl shutdown "$udid" >/dev/null 2>&1 || true; xcrun simctl delete "$udid"; }
trap cleanup EXIT
printf '%s\n%s\n%s\n' "$udid" "$runtime" "$device" > "$out/simulator.txt"
xcrun simctl boot "$udid"
xcrun simctl bootstatus "$udid" -b
xcodebuild "${project[@]}" -configuration Debug -destination "platform=iOS Simulator,id=$udid" \
  -parallel-testing-enabled NO -collect-test-diagnostics never -resultBundlePath "$out/native-tests.xcresult" test > "$out/native-tests.log" 2>&1
app="$derived/Build/Products/Debug-iphonesimulator/MathAlarm.app"
python3 -B scripts/verify_native_ui_boundaries.py --framework shared/build/bin/iosSimulatorArm64/debugFramework/app.framework --dependencies "$out/ios-dependencies.log"
python3 -B scripts/verify_ios_release.py --app "$app" | tee "$out/package-Debug-iphonesimulator.log"
# Retained independent harness is a migration parity check until all permanent gates pass.
python3 -B scripts/verify_ios_shared_bridge.py --udid "$udid" --app "$app" --output "$out/bridge.log"
python3 -B scripts/verify_ios_native_presentation.py --udid "$udid" --app "$app" --output "$out/presentation"
python3 -B scripts/native_screenshot_review.py "$out/presentation"
# Maestro 2.10.0 must be provisioned by CI, never silently use an older driver.
M7_UDID="$udid" M7_OUTPUT="$out/client" bash scripts/ci_native_client.sh
for configuration in Debug Release; do
  for platform in 'iOS' 'iOS Simulator'; do
    xcodebuild "${project[@]}" -configuration "$configuration" -destination "generic/platform=$platform" build > "$out/link-$configuration-${platform// /_}.log" 2>&1
    sdk=iphoneos; [[ "$platform" == 'iOS Simulator' ]] && sdk=iphonesimulator
    target=iosArm64; [[ "$sdk" == iphonesimulator ]] && target=iosSimulatorArm64
    graph="$out/ios-device-dependencies.log"; [[ "$sdk" == iphonesimulator ]] && graph="$out/ios-dependencies.log"
    kind=$(printf '%s' "$configuration" | tr '[:upper:]' '[:lower:]')
    python3 -B scripts/verify_native_ui_boundaries.py --framework "shared/build/bin/$target/${kind}Framework/app.framework" --dependencies "$graph"
    flags=(--root "$PWD"); [[ "$configuration" == Release ]] && flags+=(--release)
    python3 -B scripts/verify_ios_release.py --app "$derived/Build/Products/$configuration-$sdk/MathAlarm.app" "${flags[@]}" | tee "$out/package-$configuration-$sdk.log"
  done
done
xcodebuild "${project[@]}" -configuration Release -destination 'generic/platform=iOS' \
  -archivePath "$out/MathAlarm.xcarchive" archive > "$out/archive.log" 2>&1
python3 -B scripts/verify_ios_release.py --release --app "$out/MathAlarm.xcarchive/Products/Applications/MathAlarm.app" | tee "$out/archive-package.log"
