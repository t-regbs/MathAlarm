# AGP 9 Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate MathAlarm from AGP 8.11.1 to AGP 9.3.1 with built-in Kotlin, stable compatible build plugins, unique KMP Android namespaces, and a green Android/iOS Gradle build.

**Architecture:** Preserve the existing `androidApp -> shared -> core` dependency graph. First update Gradle-facing plugins and resolve the existing Compose test dependency while AGP remains at 8.11.1; then update AGP, Gradle, and Android DSL configuration as one atomic build migration. No application source code or platform API changes are required.

**Tech Stack:** AGP 9.3.1, Gradle 9.6.1, Kotlin 2.3.20, KSP 2.3.10, Compose Multiplatform 1.11.1, AndroidX Compose BOM 2026.06.01, Gradle Kotlin DSL, Kotlin Multiplatform, Room KMP, Firebase Gradle plugins.

## Global Constraints

- Keep the module graph `androidApp -> shared -> core` and the existing iOS framework integration.
- Keep Android application ID `com.timilehinaregbesola.mathalarm`, version code `27`, version name `2.5.0`, minimum SDK `26`, target/compile SDK `36`, and JVM target `17`.
- Use AGP `9.3.1`, Gradle `9.6.1`, Kotlin `2.3.20`, KSP `2.3.10`, and Compose Multiplatform `1.11.1`.
- Use AGP 9 built-in Kotlin in `androidApp`; do not add `android.builtInKotlin=false` or `android.newDsl=false`.
- Keep KMP plus `com.android.kotlin.multiplatform.library` in `shared` and `core`.
- Keep existing KMP test builders because AGP 9.3.1 supports them and `core` needs `sourceSetTreeName = "test"`.
- Do not change Kotlin package declarations, runtime behavior, database schemas, manifests, app identity, or iOS APIs.
- Do not modify or stage unrelated dirty worktree files.
- Do not create implementation commits unless the user explicitly requests them.

---

### Task 1: Stabilize Build Plugins And Test Dependency

**Files:**
- Modify: `gradle/libs.versions.toml:1-64,65-118`
- Modify: `build.gradle.kts:1-14`
- Modify: `androidApp/build.gradle.kts:108-116`

**Interfaces:**
- Consumes: Existing AGP 8.11.1 build, version catalog aliases, and Android test dependency configuration.
- Produces: A build that resolves all test configurations under AGP 8.11.1 and a stable plugin set ready for Gradle 9.

- [ ] **Step 1: Reproduce the existing full-build failure**

Run:

```bash
./gradlew build
```

Expected: FAIL at `:androidApp:generateDebugAndroidTestLintModel` because `androidx.compose.ui:ui-test-junit4` has no version. Record any additional failure before editing; do not attribute a different baseline failure to the migration.

- [ ] **Step 2: Update stable Gradle-facing tool versions while retaining AGP 8.11.1**

In `gradle/libs.versions.toml`, set these exact values and leave unlisted versions unchanged:

```toml
[versions]
android_gradle_plugin = "8.11.1"
componentsResources = "1.11.1"
compose = "2026.06.01"
crashlytics_plugin = "3.0.7"
foundation = "1.11.1"
google_services = "4.5.0"
kotlin = "2.3.20"
ksp = "2.3.4"
runtime = "1.11.1"
composeMultiplatform = "1.11.1"
kotlinStdlib = "2.3.20"
kotlinTest = "2.3.20"
ui = "1.11.1"
uiToolingPreview = "1.11.1"
```

This aligns the Compose Multiplatform plugin with the directly versioned Compose core artifacts while leaving independently versioned artifacts such as Material 3 unchanged.

- [ ] **Step 3: Add the AndroidX Compose BOM alias**

Add this entry in the `[libraries]` section of `gradle/libs.versions.toml` next to the other AndroidX Compose entries:

```toml
androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "compose" }
```

Keep the existing versionless test artifact:

```toml
androidx-compose-ui-test = { module = "androidx.compose.ui:ui-test-junit4" }
```

- [ ] **Step 4: Upgrade root utility plugins and avoid applying KSP to the root project**

Replace the root `plugins` block in `build.gradle.kts` with:

```kotlin
plugins {
    id("com.github.ben-manes.versions") version "0.54.0"
    id("nl.littlerobots.version-catalog-update") version "1.1.0"
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.android.gradle) apply false
    alias(libs.plugins.kotlin.gradle) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.crashlytics.gradle) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.android.lint) apply false
}
```

Do not change the root `clean` task in this task.

- [ ] **Step 5: Import the Compose BOM for Android instrumented tests**

In `androidApp/build.gradle.kts`, place the BOM before the existing Compose test artifact:

```kotlin
androidTestImplementation(libs.androidx.test.ext.junit)
androidTestImplementation(libs.androidx.test.espresso.core)
androidTestImplementation(platform(libs.androidx.compose.bom))
androidTestImplementation(libs.androidx.compose.ui.test)
```

- [ ] **Step 6: Verify the stable toolchain on AGP 8.11.1**

Run:

```bash
./gradlew build
```

Expected: PASS. In particular, `:androidApp:generateDebugAndroidTestLintModel` resolves `ui-test-junit4` through the BOM. Existing non-fatal Kotlin hierarchy and source warnings may remain.

- [ ] **Step 7: Inspect the Task 1 diff**

Run:

```bash
git diff --check
```

Expected: `git diff --check` exits 0. The scoped diff contains only the version updates, BOM alias/import, root utility plugin updates, and root KSP `apply false` change described above.

---

### Task 2: Migrate Android Build Configuration To AGP 9

**Files:**
- Modify: `gradle/wrapper/gradle-wrapper.properties:1-6`
- Modify: `gradle/libs.versions.toml:1-8,119-132`
- Modify: `build.gradle.kts:1-14`
- Modify: `androidApp/build.gradle.kts:1-72`
- Modify: `shared/build.gradle.kts:29-44`
- Modify: `core/build.gradle.kts:7-25`

**Interfaces:**
- Consumes: Stable plugin aliases and Compose BOM from Task 1.
- Produces: AGP 9.3.1 Android application and KMP library modules using built-in Kotlin, unique namespaces, and the supported AGP 9 DSL.

- [ ] **Step 1: Upgrade the Gradle wrapper**

In `gradle/wrapper/gradle-wrapper.properties`, replace only `distributionUrl`:

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.6.1-bin.zip
```

- [ ] **Step 2: Upgrade AGP and KSP, then remove the Kotlin Android plugin alias**

In `gradle/libs.versions.toml`, set:

```toml
android_gradle_plugin = "9.3.1"
ksp = "2.3.10"
```

Delete this plugin alias entirely:

```toml
kotlin-gradle = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

Retain these AGP-backed plugin aliases:

```toml
android-gradle = { id = "com.android.application", version.ref = "android_gradle_plugin" }
android-kotlin-multiplatform-library = { id = "com.android.kotlin.multiplatform.library", version.ref = "android_gradle_plugin" }
android-lint = { id = "com.android.lint", version.ref = "android_gradle_plugin" }
```

- [ ] **Step 3: Remove the root Kotlin Android declaration**

Delete this line from the root `build.gradle.kts` plugin block:

```kotlin
alias(libs.plugins.kotlin.gradle) apply false
```

Keep all other Task 1 plugin declarations unchanged.

- [ ] **Step 4: Adopt AGP 9 built-in Kotlin in the Android application**

In `androidApp/build.gradle.kts`, delete:

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
```

Delete the Kotlin Android plugin from the module:

```kotlin
alias(libs.plugins.kotlin.gradle)
```

Delete the redundant top-level compiler block:

```kotlin
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}
```

The resulting plugin block must be:

```kotlin
plugins {
    alias(libs.plugins.android.gradle)
    alias(libs.plugins.google.services)
    alias(libs.plugins.crashlytics.gradle)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.composeMultiplatform)
}
```

- [ ] **Step 5: Move application SDK and packaging settings to canonical AGP 9 DSL locations**

At the start of the existing `android` block, place `compileSdk` beside the namespace and remove it from `defaultConfig`:

```kotlin
android {
    namespace = "com.timilehinaregbesola.mathalarm"
    compileSdk = libs.versions.android.compile.sdk.get().toInt()

    defaultConfig {
        applicationId = "com.timilehinaregbesola.mathalarm"
        versionCode = 27
        versionName = "2.5.0"
        minSdk = libs.versions.android.min.sdk.get().toInt()
        targetSdk = libs.versions.android.target.sdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
```

Keep the existing JVM 17 `compileOptions`, which AGP built-in Kotlin uses as its default JVM target:

```kotlin
compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
```

Replace the existing `packagingOptions` block with:

```kotlin
packaging {
    resources {
        excludes.add("META-INF/DEPENDENCIES")
        excludes.add("META-INF/LICENSE")
        excludes.add("META-INF/LICENSE.txt")
        excludes.add("META-INF/license.txt")
        excludes.add("META-INF/NOTICE")
        excludes.add("META-INF/NOTICE.txt")
        excludes.add("META-INF/notice.txt")
        excludes.add("META-INF/AL2.0")
    }
}
```

Keep existing build types, signing, lint, BuildConfig, Compose, and test options unchanged.

- [ ] **Step 6: Assign unique KMP Android namespaces without changing source packages**

In `shared/build.gradle.kts`, change only the namespace inside `kotlin { androidLibrary { ... } }`:

```kotlin
namespace = "com.timilehinaregbesola.mathalarm.shared"
```

In `core/build.gradle.kts`, change only the namespace inside `kotlin { androidLibrary { ... } }`:

```kotlin
namespace = "com.timilehinaregbesola.mathalarm.core"
```

Retain `withHostTestBuilder` in both modules. Retain this complete device-test configuration in `core`:

```kotlin
withDeviceTestBuilder {
    sourceSetTreeName = "test"
}.configure {
    instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
}
```

- [ ] **Step 7: Confirm the wrapper and project configure under AGP 9**

Run:

```bash
./gradlew --version
./gradlew projects
```

Expected: both commands exit 0. The first reports Gradle 9.6.1 and JDK 17 or newer; the second lists `:androidApp`, `:shared`, and `:core` without a Kotlin Android/built-in Kotlin conflict or duplicate namespace error.

- [ ] **Step 8: Run focused Android tests and assembly**

Run:

```bash
./gradlew :core:testAndroidHostTest :shared:testAndroidHostTest :androidApp:testDebugUnitTest :androidApp:assembleDebug
```

Expected: PASS. The debug APK is emitted under `androidApp/build/outputs/apk/debug/`.

- [ ] **Step 9: Inspect the Task 2 diff**

Run:

```bash
git diff --check
```

Expected: `git diff --check` exits 0. No application source, manifest, resource, package, Gradle property, or iOS project file is changed.

---

### Task 3: Verify The Complete AGP 9 Migration

**Files:**
- Verify unchanged: `gradle.properties`
- Verify unchanged: `.github/workflows/build.yml`
- Verify unchanged: Android/iOS application sources and resources

**Interfaces:**
- Consumes: AGP 9.3.1 build configuration produced by Tasks 1 and 2.
- Produces: Evidence that Android tests/builds, the full Gradle lifecycle, and the shared iOS framework work without legacy AGP flags or plugins.

- [ ] **Step 1: Run the full Gradle build**

Run:

```bash
./gradlew build
```

Expected: PASS, including `:androidApp:generateDebugAndroidTestLintModel`. Existing non-fatal Kotlin hierarchy-template and source warnings may remain.

- [ ] **Step 2: Build both Android application variants explicitly**

Run:

```bash
./gradlew :androidApp:assembleDebug :androidApp:assembleRelease
```

Expected: PASS. Debug and release outputs exist under `androidApp/build/outputs/`; Google Services, Crashlytics, BuildConfig, manifest processing, and packaging tasks complete.

- [ ] **Step 3: Link the shared iOS simulator framework**

Run:

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

Expected: PASS and a debug `app.framework` is produced for `iosSimulatorArm64`. An Xcode application build is not required.

- [ ] **Step 4: Confirm legacy plugins, opt-outs, and removed APIs are absent**

Run:

```bash
rg -n "org\.jetbrains\.kotlin\.android|org\.jetbrains\.kotlin\.kapt|android\.builtInKotlin=false|android\.newDsl=false|applicationVariants|libraryVariants|variantFilter|BaseExtension" --glob "*.kts" --glob "*.toml" --glob "*.properties" .
```

Expected: no output and exit status 1, meaning no forbidden pattern is present.

- [ ] **Step 5: Confirm Android namespaces are unique**

Run:

```bash
rg -n "namespace =" androidApp/build.gradle.kts shared/build.gradle.kts core/build.gradle.kts
```

Expected output contains exactly these three distinct values:

```text
com.timilehinaregbesola.mathalarm
com.timilehinaregbesola.mathalarm.shared
com.timilehinaregbesola.mathalarm.core
```

- [ ] **Step 6: Confirm CI remains compatible and inspect final changes**

Read `.github/workflows/build.yml` and verify it still uses JDK 17, the checked-in wrapper, and these tasks:

```text
:core:testAndroidHostTest
:shared:testAndroidHostTest
:androidApp:testDebugUnitTest
:androidApp:assembleDebug
```

Then run:

```bash
git diff --check
```

Expected: `git diff --check` exits 0. Status includes only the intended Gradle/version-catalog changes plus pre-existing unrelated worktree changes; no unrelated file has been reverted, staged, or overwritten.
