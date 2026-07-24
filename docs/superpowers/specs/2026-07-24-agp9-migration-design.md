# AGP 9 Migration Design

## Goal

Migrate MathAlarm from Android Gradle Plugin 8.11.1 to the latest stable AGP 9 release while preserving the existing Android and iOS application behavior. Use AGP 9 built-in Kotlin and the new Android DSL without compatibility opt-outs, and leave the existing `androidApp -> shared -> core` module structure intact.

## Baseline

The project has already completed the structural work required by AGP 9:

- `androidApp` is a pure Android application module.
- `shared` and `core` are Kotlin Multiplatform modules using `com.android.kotlin.multiplatform.library`.
- Android sources use `main`, `androidMain`, `androidHostTest`, and `androidDeviceTest` as appropriate.
- The local JDK is 21 and CI uses JDK 17; both satisfy AGP 9's JDK 17 minimum.

The current Gradle wrapper is 8.13 and must be upgraded. The current full `./gradlew build` first fails because `androidx.compose.ui:ui-test-junit4` has no version on the Android instrumented-test classpath. After that dependency is resolved, the build exposes a stale alarm-order assertion and an AlarmKit-only iOS test that eagerly initializes `UNUserNotificationCenter` outside an application bundle. The migration includes the BOM import, the one-line ordering correction, and lazy notification-center initialization required to make the full build green while preserving runtime notification behavior.

## Toolchain Versions

Use this stable build-tool set:

- Android Gradle Plugin: `9.3.1`
- Gradle wrapper: `9.6.1`
- Kotlin Gradle Plugin and Kotlin-managed coordinates: `2.3.20`
- Kotlin Symbol Processing: `2.3.10`
- Compose Multiplatform plugin and directly versioned Compose core artifacts: `1.11.1`
- AndroidX Compose BOM: `2026.06.01`
- Google Services plugin: `4.5.0`
- Firebase Crashlytics Gradle plugin: `3.0.7`
- Ben Manes Versions plugin: `0.54.0`
- Version Catalog Update plugin: `1.1.0`

Other runtime library versions remain unchanged. Compose artifacts with independent release trains, including Material 3, are not forced to the Compose Multiplatform plugin version.

## Plugin And DSL Migration

Remove `org.jetbrains.kotlin.android` from `androidApp`, the root plugin declarations, and the version catalog. AGP 9 built-in Kotlin compiles the application's Kotlin sources. Keep `org.jetbrains.kotlin.multiplatform` on `shared` and `core`, because built-in Kotlin does not replace KMP support.

Do not add `android.builtInKotlin=false`, `android.newDsl=false`, or other legacy compatibility flags. Keep KSP rather than introducing kapt. Upgrade Gradle-facing plugins to the versions listed above so they can run on Gradle 9 and the AGP 9 DSL. Root plugin declarations that are only consumed by modules should use `apply false`.

In `androidApp/build.gradle.kts`:

- Keep `com.android.application`, Google Services, Crashlytics, Compose compiler, and Compose Multiplatform.
- Remove the Kotlin Android plugin.
- Retain JVM 17 through Android `compileOptions`, remove the redundant top-level Kotlin compiler block and import, and let AGP built-in Kotlin derive the same JVM target from those Android options.
- Move `compileSdk` to the top-level `android` block while retaining application ID, minimum SDK, target SDK, version, signing, build types, BuildConfig, Compose, lint, and test behavior.
- Use the current `packaging.resources` DSL for resource exclusions.

In `shared` and `core`:

- Retain Kotlin Multiplatform and `com.android.kotlin.multiplatform.library`.
- Retain existing Android KMP source sets, tests, Compose resources, KSP, Room, and iOS targets.
- Retain `withHostTestBuilder` in both KMP modules and `withDeviceTestBuilder` in `core`. AGP 9.3.1 still supports these APIs, and the device-test builder is required to preserve `sourceSetTreeName = "test"` before configuring the instrumentation runner.
- Do not move source files or change Kotlin package declarations.

## Namespaces

AGP 9 enables unique package names by default. Assign these Android namespaces:

- `androidApp`: `com.timilehinaregbesola.mathalarm`
- `shared`: `com.timilehinaregbesola.mathalarm.shared`
- `core`: `com.timilehinaregbesola.mathalarm.core`

The Android application ID remains `com.timilehinaregbesola.mathalarm`. Namespace changes affect generated Android classes only; they do not rename source packages, alter persisted data, change manifest component class names, or change the iOS framework.

## Compose Test Dependency

Add an AndroidX Compose BOM catalog entry at `2026.06.01` and import it on the `androidTestImplementation` classpath before the existing versionless `androidx.compose.ui:ui-test-junit4` dependency. This preserves the test dependency while fixing the baseline `./gradlew build` resolution failure. No Android instrumented tests are added as part of this migration.

## Behavior And Error Handling

This is a build-system migration. It does not change alarm scheduling, notification outcomes, persistence, UI behavior, application identity, version information, database schemas, package names, or iOS APIs. The iOS scheduler delays notification-center acquisition and category registration until the first notification operation; AlarmKit-only pending checks no longer initialize an unused notification dependency.

If migration reveals a removed AGP DSL or variant API in project build scripts, migrate it to the supported AGP 9 API. If a third-party Gradle plugin fails, use its stable AGP 9-compatible release. Do not add runtime wrappers or silently enable legacy AGP behavior. If no compatible stable plugin exists, stop and report the exact blocker rather than weakening the migration.

Existing Kotlin hierarchy-template and source warnings are outside this migration unless they become build blockers. Unrelated dirty worktree files must not be reverted, overwritten, staged, or committed.

## Verification

Use existing tests and build artifacts rather than adding product tests for build configuration:

1. Run `./gradlew --version` and confirm Gradle 9.6.1 with JDK 17 or newer.
2. Run `./gradlew build` and require a zero exit status, including Android test classpath resolution.
3. Run `./gradlew :core:testAndroidHostTest :shared:testAndroidHostTest :androidApp:testDebugUnitTest`.
4. Run `./gradlew :androidApp:assembleDebug :androidApp:assembleRelease` to exercise manifests, resources, Google Services, Crashlytics, BuildConfig, packaging, and AGP 9 defaults.
5. Run `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` to verify that shared iOS framework production still works.
6. Search build configuration to confirm there is no `org.jetbrains.kotlin.android`, `org.jetbrains.kotlin.kapt`, `android.builtInKotlin=false`, `android.newDsl=false`, removed variant API, or duplicate Android namespace.
7. Run `git diff --check` and inspect status to confirm only intended build configuration and documentation changed.

An Xcode application build is not required for this migration. CI remains on JDK 17 and continues using the checked-in Gradle wrapper and existing Android test/build tasks.
