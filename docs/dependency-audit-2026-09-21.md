# Dependency audit — 21 September 2026

Published versions were checked against Google Maven, Maven Central, the Gradle Plugin Portal, and Gradle release metadata. Stable releases are preferred; Navigation3 remains on its existing beta and adaptive Navigation3 advances from alpha to RC because that artifact has no stable release.

## Version changes

| Catalog version | Before | After |
| --- | --- | --- |
| `android_gradle_plugin` | 9.3.1 | 9.4.1 |
| `androidx_activity_compose` | 1.12.2 | 1.13.0 |
| `androidx_ktx` | 1.17.0 | 1.19.0 |
| `androidx_room` | 2.8.4 | 2.8.5 |
| `compose` | 2026.06.01 | 2026.09.00 |
| `coroutines` | 1.10.2 | 1.11.0 |
| `crashlytics_plugin` | 3.0.7 | 3.0.8 |
| `firebase` | 32.7.4 | 34.19.0 |
| `kermit` | 2.0.8 | 2.2.0 |
| `kotlin` | 2.3.20 | 2.4.20 |
| `kotlinx_serialization` | 1.9.0 | 1.11.0 |
| `test_mockk` | 1.14.7 | 1.14.11 |
| `test_robolectric` | 4.16 | 4.17 |
| `test_kotest` | 6.0.7 | 6.2.5 |
| `lyricist` | 1.7.0 | 1.9.0 |
| `ksp` | 2.3.10 | 2.3.12 |
| `jetbrainsLifecycleViewmodel` | 2.10.0-alpha07 | 2.11.0 |
| `appcompat` | 1.7.1 | 1.8.0 |
| `datetime` | 0.7.1 | 0.8.0 |
| `koin-bom` | 4.1.1 | 4.2.2 |
| `composeMultiplatform` | 1.10.3 | 1.12.0 |
| `compottie` | 2.0.2 | 2.3.1 |
| `compose-material3-adaptive` | 1.3.0-alpha02 | 1.3.0-rc01 |
| `calf` | 0.11.0 | 0.14.0 |
| SQLite | 2.6.2 | 2.7.1 |
| Gradle wrapper | 9.6.1 | 9.7.1 |
| Dependency updates plugin | 0.54.0 | 0.64.0 |
| Catalog update plugin | 1.1.0 | 1.1.1 |

## Cleanup

- Share one Compose Multiplatform version across runtime, UI, foundation, resources, and tooling preview. Material3 retains its independent latest stable version (1.9.0).
- Use the Kotlin plugin version for kotlin-test and let Kotlin supply its standard library automatically.
- Align coroutine runtime and test versions, SQLite drivers, and multiplatform settings artifacts.
- Remove unused Hilt, Timber, Lottie, snapshot navigation, and other obsolete version entries.
- Remove unused DataStore dependency and its unreferenced Context extension. No stored preferences are deleted or migrated.
- Keep Kermit Crashlytics only in the Android app, where its writer is used.
- Remove unused app-level Koin Compose and redundant Android Compose preview declarations.
- Scope the Android SQLite driver to host tests; production uses the shared bundled driver.
- Remove the duplicate iOS SQLite dependency and use the default Kotlin source-set hierarchy.
- Remove the obsolete AndroidX snapshot repository and Timber lint suppressions.
- Declare the coroutines dependency directly in shared, which imports its APIs.
- Update deprecated Android KMP DSL and the dependency-update plugin ID.

## Firebase regression fix

Firebase BoM 34.19.0 uses Crashlytics 20.1.1. Analytics uses the main module, since KTX artifacts are retired. The explicit registrar constructor keep rule protects reflective discovery with AGP 9 strict R8 rules. Minification and resource shrinking remain enabled.

## Sources

- [Firebase Android release notes](https://firebase.google.com/support/release-notes/android)
- [Compose compatibility](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html)
- [Kotlin releases](https://kotlinlang.org/docs/releases.html)
- [AGP release notes](https://developer.android.com/build/releases/agp-9-4-0-release-notes)
- [Gradle release metadata](https://services.gradle.org/versions/current)

## Validation

- Android release assembly (R8 minification and resource shrinking enabled): passed.
- Crashlytics mapping upload task: passed.
- Core host tests: 130 passed.
- Shared host tests: 159 passed.
- Android app unit tests: 82 passed.
- Android instrumentation test APK compilation: passed; device tests were not run.
- Final release installed and launched on the emulator. An induced crash was captured and its report upload returned HTTP 200 after restart.
- Full iOS app build for the arm64 simulator with Xcode: passed (`CODE_SIGNING_ALLOWED=NO`).
