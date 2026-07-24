# KMP Default Structure Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Separate the Android application from the Kotlin Multiplatform shared library using `androidApp`, `shared`, and the existing `core` module.

**Architecture:** Rename the current mixed-purpose `app` module to `shared`, convert its Android target to an Android-KMP library, and create a Kotlin Android application module that depends on `shared` and `core`. Keep KMP `actual` implementations and their direct helpers in `shared`; move Android application entry points, app wiring, packaging, resources, and JVM Android tests to `androidApp`.

**Tech Stack:** Kotlin 2.3.0, Kotlin Multiplatform, Compose Multiplatform 1.10.0-rc02, Android Gradle Plugin 8.11.1, Android-KMP library plugin, Gradle Kotlin DSL, Room KMP, KSP, Xcode.

## Global Constraints

- Preserve Android application ID `com.timilehinaregbesola.mathalarm`, version code `27`, and version name `2.5.0`.
- Preserve Android compile/target SDK 36, minimum SDK 26, and JVM target 17.
- Preserve iOS framework `baseName = "app"` and existing Swift `import app` statements.
- Preserve the dependency direction `androidApp -> shared -> core`.
- Do not change alarm behavior, database schemas, package names, UI behavior, or public platform APIs.
- Do not revert or overwrite unrelated worktree changes, especially `MathScreen.kt` and current iOS asset changes.
- Use the existing tests after migration; do not add test-first migration coverage.
- Do not create commits unless the user explicitly requests one.

---

### Task 1: Split Android Application From Shared KMP Code

**Files:**
- Rename: `app/` to `shared/`
- Create: `androidApp/build.gradle.kts`
- Create: `androidApp/.gitignore`
- Modify: `settings.gradle.kts:28-30`
- Modify: `build.gradle.kts:1-14`
- Modify: `gradle/libs.versions.toml:115-128`
- Modify: `shared/build.gradle.kts`
- Modify: `shared/src/androidMain/kotlin/com/timilehinaregbesola/mathalarm/platform/PlatformApis.android.kt:27,131`
- Move: Android app entry-point and implementation sources from `shared/src/androidMain/kotlin/` to `androidApp/src/main/kotlin/`
- Move: `shared/src/androidMain/AndroidManifest.xml` to `androidApp/src/main/AndroidManifest.xml`
- Move: `shared/src/androidMain/res/` to `androidApp/src/main/res/`
- Move: `shared/src/androidMain/ic_launcher-playstore.png` to `androidApp/src/main/ic_launcher-playstore.png`
- Move: `shared/src/debug/` to `androidApp/src/debug/`
- Move: `shared/src/release/` to `androidApp/src/release/`
- Move: `shared/src/test/` to `androidApp/src/test/`
- Move: `shared/google-services.json` to `androidApp/google-services.json`
- Move: `shared/proguard-rules.pro` to `androidApp/proguard-rules.pro`
- Move: `shared/release/` to `androidApp/release/`

**Interfaces:**
- Consumes: Existing `core` domain APIs and all existing shared Compose APIs.
- Produces: Gradle modules `:androidApp`, `:shared`, and `:core`; Android application code imports shared APIs through `implementation(project(":shared"))`.

- [ ] **Step 1: Rename the mixed-purpose module and register the final module graph**

Rename `app` to `shared`, create `androidApp`, and replace the settings includes with:

```kotlin
include(":androidApp")
include(":shared")
include(":core")
rootProject.name = "MathAlarm"
```

Keep the existing repository configuration unchanged.

- [ ] **Step 2: Ensure both Android plugin types are available at the root**

Retain the existing application, Kotlin Android, Kotlin Multiplatform, Compose, and Android-KMP library aliases in `build.gradle.kts`. Do not upgrade versions. The relevant declarations remain:

```kotlin
alias(libs.plugins.android.gradle) apply false
alias(libs.plugins.kotlin.gradle) apply false
alias(libs.plugins.compose.compiler) apply false
alias(libs.plugins.composeMultiplatform) apply false
alias(libs.plugins.kotlinMultiplatform) apply false
alias(libs.plugins.android.kotlin.multiplatform.library) apply false
```

- [ ] **Step 3: Convert `shared` from an Android application to an Android-KMP library**

In `shared/build.gradle.kts`, replace the Android application, Google Services, and Crashlytics plugins with the Android-KMP library plugin. Keep serialization, KSP, Compose compiler, Compose Multiplatform, Kotlin Multiplatform, Room, and their existing configuration:

```kotlin
plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidx.room)
}
```

Replace `androidTarget {}` with this target inside `kotlin {}`:

```kotlin
androidLibrary {
    namespace = "com.timilehinaregbesola.mathalarm"
    compileSdk = libs.versions.android.compile.sdk.get().toInt()
    minSdk = libs.versions.android.min.sdk.get().toInt()
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors = false
        freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn", "-Xopt-in=kotlin.Experimental")
    }
    androidResources {
        enable = true
    }
    withHostTestBuilder {
    }
}
```

Delete the top-level `android {}` application block and Android application test dependencies. Reduce `androidMain.dependencies` to the dependencies used by the retained Android actuals:

```kotlin
androidMain.dependencies {
    implementation(compose.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.android.material)
}
```

Keep Room compiler configurations for Android/iOS, Lyricist metadata KSP, common tests, iOS targets, Swift export, framework `baseName = "app"`, and existing KSP task ordering. `withHostTestBuilder` preserves an Android host on which `commonTest` can run.

- [ ] **Step 4: Keep only KMP-required Android implementations in `shared`**

Retain these files under `shared/src/androidMain/kotlin`:

```text
com/timilehinaregbesola/mathalarm/interactors/AudioPlayer.kt
com/timilehinaregbesola/mathalarm/platform/AppNightMode.android.kt
com/timilehinaregbesola/mathalarm/platform/PlatformApis.android.kt
com/timilehinaregbesola/mathalarm/utils/PickRingtone.kt
```

Move every other Kotlin file currently under `shared/src/androidMain/kotlin` to the same package-relative path under `androidApp/src/main/kotlin`. This places `MainActivity`, `AlarmApplication`, manifest components, Android DI, notification scheduling/services, permission implementations, and app utilities in the application module.

In `PlatformApis.android.kt`, remove the `BuildConfig` import and make the existing Koin-backed context authoritative:

```kotlin
actual fun getApplicationId(): String = getKoinContext().packageName
```

- [ ] **Step 5: Move Android packaging and test assets to `androidApp`**

Move the Android manifest, `res`, debug/release resources, local JVM tests, Google Services file, ProGuard rules, launcher store image, and checked-in release output listed in this task's Files section. Keep `commonMain`, `commonTest`, `iosMain`, `iosSimulatorArm64Test`, `composeResources`, Room schemas, and iOS framework sources under `shared`.

- [ ] **Step 6: Configure the Android application module**

Create `androidApp/build.gradle.kts` with Kotlin Android, Android application, Google Services, Crashlytics, Compose compiler, and Compose Multiplatform plugins. Configure JVM 17 and reproduce the existing application `android {}` block exactly, including namespace, application ID, versions, SDK levels, build types, signing behavior, lint exclusions, Compose/BuildConfig features, packaging exclusions, and unit-test options.

Add direct dependencies required by moved sources, beginning with the module boundaries:

```kotlin
dependencies {
    implementation(project(":shared"))
    implementation(project(":core"))

    implementation(libs.runtime)
    implementation(libs.foundation)
    implementation(libs.material3)
    implementation(libs.ui)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.android.material)
    implementation(libs.lyricist)
    implementation(libs.kermit)
    implementation(libs.kermit.crashlytics)
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.datetime)
    implementation(libs.multiplatform.settings.no.arg)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.sqlite.driver.android)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.core)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test)
}
```

Resolve compile errors by adding only catalog dependencies directly imported by moved Android source files. Do not expose shared internals or introduce dependency wrappers.

- [ ] **Step 7: Validate the Android split**

Run:

```bash
./gradlew projects :shared:tasks :androidApp:tasks
./gradlew :core:testAndroidHostTest :shared:testAndroidHostTest :androidApp:testDebugUnitTest :androidApp:assembleDebug
```

Expected: all commands exit 0; Gradle lists `androidApp`, `shared`, and `core`; existing Android tests pass; the APK is produced at `androidApp/build/outputs/apk/debug/`.

---

### Task 2: Redirect iOS Integration To `shared`

**Files:**
- Modify: `iosApp/iosApp.xcodeproj/project.pbxproj:209,354,383`
- Verify unchanged: `iosApp/iosApp/ContentView.swift`
- Verify unchanged: `iosApp/iosApp/iOSApp.swift`
- Verify unchanged: `iosApp/iosApp/AlarmKitWrapper.swift`

**Interfaces:**
- Consumes: `:shared` framework with `baseName = "app"`.
- Produces: Xcode build phase invoking `:shared:embedAndSignAppleFrameworkForXcode` while Swift continues importing `app`.

- [ ] **Step 1: Update the Xcode Gradle build phase**

Change only the Gradle task path in the shell script:

```text
:app:embedAndSignAppleFrameworkForXcode
```

to:

```text
:shared:embedAndSignAppleFrameworkForXcode
```

- [ ] **Step 2: Update both framework search paths**

Replace both occurrences of:

```text
$(SRCROOT)/../app/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)
```

with:

```text
$(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)
```

Leave framework name `app` and all Swift imports unchanged.

- [ ] **Step 3: Verify the shared iOS framework and Xcode project**

Run:

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -configuration Debug CODE_SIGNING_ALLOWED=NO build
```

Expected: the Gradle framework link exits 0 and emits an `app.framework`; the Xcode build exits 0. If no simulator runtime is available, retain the successful Gradle link as evidence and report the exact Xcode toolchain error rather than changing project behavior.

---

### Task 3: Update Automation And Project Documentation

**Files:**
- Modify: `.github/workflows/build.yml:29-45`
- Modify: `README.md:10-16,65-66`

**Interfaces:**
- Consumes: final Gradle module/task names and Android APK location.
- Produces: CI and contributor documentation aligned with the migrated structure.

- [ ] **Step 1: Redirect CI Android tasks and artifact path**

Keep the `core` test command. Replace app commands with:

```yaml
- name: Run Shared Module Tests
  run: ./gradlew :shared:testAndroidHostTest

- name: Run Android App Tests
  run: ./gradlew :androidApp:testDebugUnitTest

- name: Build Debug APK
  run: ./gradlew :androidApp:assembleDebug
```

Update the artifact path to:

```yaml
path: androidApp/build/outputs/apk/debug/*.apk
```

- [ ] **Step 2: Document the new module ownership**

Replace the README architecture module list with:

```markdown
- **`:androidApp`** - Android application entry points, packaging, platform wiring, and Android resources
- **`:shared`** - Shared Compose UI, data layer, and Android/iOS platform implementations
- **`:core`** - Shared domain logic and business rules
- **`iosApp/`** - Native iOS application consuming the framework produced by `:shared`
```

Keep the existing iOS build instructions and clarify that `iosApp` consumes the shared framework.

- [ ] **Step 3: Run final verification and inspect only intended changes**

Run:

```bash
./gradlew :core:testAndroidHostTest :shared:testAndroidHostTest :androidApp:testDebugUnitTest :androidApp:assembleDebug :shared:linkDebugFrameworkIosSimulatorArm64
git status --short
git diff --check
```

Expected: Gradle exits 0, Android tests pass, the debug APK and iOS simulator framework are generated, `git diff --check` exits 0, and status contains the structural migration plus pre-existing unrelated changes without deletions or reversions of user work.
