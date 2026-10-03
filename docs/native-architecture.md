# Native application architecture and build

Android renders every screen with Compose in `androidApp`; iOS renders every screen
with SwiftUI in `iosApp`. `core` contains domain rules/contracts. `shared` contains
renderer-free application coordination, Room/preferences, feature ViewModels and SDK
adapters. Android-only CALF, Lyricist, Compose resources and navigation stay in Android.

The static `app` framework exports `core`, the approved ObservableViewModel base and
the narrow `IosApplication` / `SharedFeatures` interface. Room implementations remain
hidden from ObjC; KSP generates all three platform implementations and versioned schemas.
One bootstrap/coordinator owns accepted operations independently of native observation.
Do not replace production framework consumption with a test-only Kotlin framework.

Swift `NativeWindowSessions` retains factory-created editor/challenge owners above
replaceable navigation columns. Owners use StateViewModel; children observe. Each retained session also holds its bridge
observation wrapper, so an outgoing SwiftUI observer cannot recreate an already expired
bridge cancellable. Explicit factory closure still ends the shared session immediately. Closing
an editor calls the factory's close API. Replacing a pane only replaces observation.
Closing a challenge does not resolve an alarm. Native result handling acknowledges the
exact retained result ID; successful delivery readiness persists unresolved progress
before acknowledging the exact native queue head. Process startup restores unresolved
occurrences independently of the queue. Audio uses the existing real/preview/guide
ownership arbitration. Persistence keys, schema versions and registration identities
are compatibility contracts, not migration cleanup candidates.

All parity screens are implemented, including settings, announcements, sharing,
permissions/Settings return, tones and preview/delivered maths. The post-M6 violet visual
identity and navigation fixes are the baseline. Minimum deployment is iOS 26.0; running
on newer simulators does not verify that minimum. Physical AlarmKit, PiP over Settings,
acoustic/routing/recovery and distribution acceptance remain M8 gates.

## Reproducible local build

Use the existing Xcode 27.0, JDK 21, Kotlin 2.4.20, Coroutines 1.11.0 and Lifecycle
2.11.0 toolchain. Kotlin and Swift bridge versions remain ObservableViewModel 1.1.0
and NativeCoroutines 1.0.6; commit/review Package.resolved together with Gradle pins.
No toolchain upgrade is part of M7. Android compile/target SDK remain 37/minimum 26.

```sh
./gradlew :androidApp:assembleDebug
xcodebuild -project iosApp/iosApp.xcodeproj -scheme MathAlarmNative \
  -configuration Debug -destination 'generic/platform=iOS Simulator' \
  -disableAutomaticPackageResolution CODE_SIGNING_ALLOWED=NO build
```

Use the shared `MathAlarmNative` scheme for tests; it avoids collision with an existing
private `iosApp` scheme. The normal Kotlin build phase must run for final evidence.
`SKIP_KOTLIN_BUILD=YES` is only a Swift iteration aid and does not verify changed Kotlin.
The normal Xcode build compiles the production static framework for the selected SDK
and configuration. Debug/Release simulator/device linking and an unsigned Release
archive are checked by `scripts/ci_native_ios.sh`. Unsigned packaging is not signing,
TestFlight, store submission or physical delivery acceptance.

See [testing](testing.md), [M7 evidence](native-ui-migration-milestone-7-2026-10-03.md)
and the [isolated cleanup review](reviews/native-ui-m7-cleanup.md).
