# KMP Default Structure Migration Design

## Goal

Migrate MathAlarm to the recommended Kotlin Multiplatform project structure by separating the Android application entry points from the multiplatform shared code. Preserve current Android and iOS behavior and retain the existing `core` domain boundary.

## Resulting Structure

- `androidApp`: Android application module, packaging, entry points, app wiring, Android resources, and Android application tests.
- `shared`: Kotlin Multiplatform library containing shared Compose UI, data code, Compose resources, iOS integration, and required Android platform implementations.
- `core`: Existing Kotlin Multiplatform domain module, unchanged except for references required by module renaming.
- `iosApp`: Existing Xcode application consuming the framework produced by `shared`.

The dependency direction is `androidApp -> shared -> core`. The iOS app consumes the framework from `shared`.

## Module Migration

Rename the existing `app` module and directory to `shared`. Convert its Android target from the Android application plugin and `androidTarget {}` to the Android-KMP library plugin and `androidLibrary {}`. Keep KSP, Room, serialization, Compose Multiplatform, common and iOS sources, common tests, Compose resources, schemas, and iOS framework configuration in this module.

Create `androidApp` with the Kotlin Android and Android application plugins. Move the Android application ID, versions, SDK settings, build types, ProGuard configuration, Firebase and Google Services configuration, packaging options, Android resources, manifest, entry points, app-specific dependency injection, and JVM Android tests into this module. Add an implementation dependency on `shared`.

Android `actual` declarations required to satisfy declarations in `shared/commonMain` remain in `shared/androidMain`. Any helper directly required by those actual implementations also remains there. Android application entry points and implementations that are only reached from the Android manifest or app wiring move to `androidApp/src/main`.

The shared implementation of `getApplicationId()` will use the runtime Android package name rather than the application module's generated `BuildConfig`, because the shared library no longer owns an application ID.

## Platform Integration

Keep the iOS framework base name as `app`, preserving existing Swift `import app` statements. Update the Xcode build phase from `:app:embedAndSignAppleFrameworkForXcode` to `:shared:embedAndSignAppleFrameworkForXcode` and update framework search paths from `app/build` to `shared/build`.

Update `settings.gradle.kts`, root plugin aliases where necessary, README module documentation, CI Gradle tasks, and APK artifact paths to use `androidApp` and `shared`.

## Behavior And Data

This is a structural migration. It does not change alarm scheduling, persistence, notifications, UI behavior, package names, application ID, version information, database schema, or iOS framework import name. Existing Room schemas and platform-specific implementations remain associated with the shared code that defines their interfaces.

## Error Handling

No new runtime error path is introduced. Build failures caused by misplaced dependencies or source ownership will be resolved by moving the dependency to the module that compiles the referencing source, without adding compatibility wrappers or changing runtime behavior.

## Verification

Use the existing tests and builds rather than adding test-first migration coverage:

- Validate Gradle project configuration and task discovery.
- Run the existing `core` Android host tests.
- Compile and test the shared module for Android and common code.
- Run `androidApp` JVM tests and assemble the debug APK.
- Link the shared iOS simulator framework.
- Build the Xcode application when supported by the local toolchain.
- Confirm CI and artifact paths reference the new modules.

Existing unrelated worktree changes, including the edited `MathScreen.kt`, generated Kotlin metadata, IDE state, and current iOS asset changes, must not be reverted or overwritten.
