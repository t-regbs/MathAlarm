# Milestone 7 isolated cleanup review

These changes are confined to `shared/build.gradle.kts`, `gradle/libs.versions.toml`
and the three platform service declaration/implementation files. Review them
separately from native tests/CI. No versions change.

- Remove the unused `toPlatformMediaSource` identity shim on both platforms and
  its expect declaration. Repository search found no consumers, including tests.
- Remove the unused explicit Compose components-resources catalog alias. Android's
  Compose resource DSL remains supplied by its existing plugin.
- Remove shared Android resource processing (no shared Android resources remain)
  and obsolete `-Xopt-in=kotlin.RequiresOptIn` / nonexistent `kotlin.Experimental`
  flags. Remove the redundant default `allWarningsAsErrors = false` assignment.
- Keep Android CALF/Compose dependencies: the native Android renderer consumes them.
  iOS Compose/CALF entry points and Swift-export DSL were already removed in M3.
- Keep AppCompat/night-mode adapters, legacy preference and queue readers, retired
  Room columns, native recovery codecs, preview-stop APIs and ObjC refinement.
  These have current consumers or preserve persisted compatibility.

Room schema directory, all three KSP processors, generated ObjC visibility refinement,
platform test source sets, SharedFeatures factories, retained owners, durable commands,
registration IDs, preferences/progress keys and audio arbitration remain intact.

Review only this cleanup with:

```sh
git diff -- shared/build.gradle.kts gradle/libs.versions.toml \
  shared/src/commonMain/kotlin/com/timilehinaregbesola/mathalarm/platform/PlatformServices.kt \
  shared/src/androidMain/kotlin/com/timilehinaregbesola/mathalarm/platform/PlatformApis.android.kt \
  shared/src/iosMain/kotlin/com/timilehinaregbesola/mathalarm/platform/PlatformServices.ios.kt
```
