# Native UI and shared ViewModel research results

Verified on 2 October 2026 for MathAlarm's migration to Android Compose and iOS SwiftUI. The experiments support sharing Kotlin feature ViewModels as well as domain and application logic. The selected integration is the existing Objective-C framework export with KMP-ObservableViewModel and KMP-NativeCoroutines. The [approved migration plan](../../native-ui-migration-plan.md) applies these findings to the production app.

The experiment source, standalone projects, build products, temporary package checkouts, and installed prototype apps were removed at the user's request. This directory retains the results, methodology, compiler diagnostics, and screenshots. It is an evidence archive rather than a runnable reproducer. Production application code was not migrated by these experiments.

## Toolchain and release research

JetBrains lists Kotlin **2.4.20**, released 7 September 2026, as the latest stable release checked for this decision. MathAlarm already uses that version. Kotlin 2.4 added native suspend and Flow export; 2.4.20 expanded sealed hierarchy and cross-language inheritance support and SwiftPM integration. Native Swift export remains **Alpha**, even with a stable Kotlin compiler. Importing Swift package APIs into Kotlin and exporting Kotlin APIs to Swift are separate integration concerns. [Kotlin releases](https://kotlinlang.org/docs/releases.html), [Kotlin 2.4 changes](https://kotlinlang.org/docs/whatsnew24.html), [Kotlin 2.4.20 changes](https://kotlinlang.org/docs/whatsnew2420.html), [Swift export](https://kotlinlang.org/docs/native-swift-export.html).

| Dependency | Version resolved in the experiments |
| --- | --- |
| Kotlin | 2.4.20 |
| Coroutines | 1.11.0 |
| JetBrains AndroidX Lifecycle | 2.11.0 |
| KMP-ObservableViewModel | 1.1.0 |
| KMP-NativeCoroutines | 1.0.6 |
| Repository Gradle wrapper | 9.7.1 |

ObservableViewModel's published 1.1.0 compatibility matrix lists Coroutines 1.10.1 and Lifecycle 2.9.2; NativeCoroutines 1.0.6 lists Coroutines 1.10.1. The newer versions above were explicitly resolved and exercised locally. This establishes evidence for this combination, rather than maintainer certification of every combination. Production must pin the resolved versions and retain integration checks when dependencies change. The Kotlin plugin/library and Swift package versions of each bridge must match. [ObservableViewModel](https://github.com/rickclephas/KMP-ObservableViewModel), [NativeCoroutines](https://github.com/rickclephas/KMP-NativeCoroutines).

## Shared ViewModels with native UIs

The framework experiment used one Kotlin `AlarmEditorViewModel` implementation in a Compose Android app and a SwiftUI iOS app. Kotlin owned draft state, validation, saving, failures, retries, and duplicate-save protection. Persistence was fake; no production database or alarm scheduler was involved.

SwiftUI owned native controls and navigation. A stable `@StateViewModel` session owner sat above the split view and its detail stack; children used `@ObservedViewModel`. Replacing the native detail layout retained the draft. State used the ObservableViewModel Flow APIs and the NativeCoroutines bridge. A Swift observation adapter contained no second implementation of editor behavior.

| Evidence | Result | Retained record |
| --- | --- | --- |
| JVM common and lifecycle tests | 7 passed, 0 failures | [JVM report](bridge-jvm-tests.json) |
| Swift integration on open iPhone Duo | 9 passed, 0 failures | [iOS report](bridge-ios-runtime.json) |
| Android API 35 emulator UI | 6 manual checks passed | [Android report](bridge-android-runtime.json) |
| Open Duo editor and integration results | Native SwiftUI screen inspected | [Screenshot](bridge-duo-open.png) |

The seven JVM tests covered validation, save success, failure/retry, repeated saves, coroutine/Flow cancellation, cleanup before work dispatch, and actual `ViewModelStore` retention/clearing. The nine Swift checks covered typed edits, Swift Observation invalidation, save success/failure/retry, Flow and suspend cancellation, explicit cleanup, detail replacement, and automatic SwiftUI owner cleanup. Android checks covered validation, editing, saving, failure/retry, and portrait-landscape-portrait activity recreation. Original emulator rotation settings were restored.

The Swift lifetime checks mounted a hosting controller in the running application's window, waited for its content to appear, then removed that content through SwiftUI. An offscreen hosting controller did not establish ownership. Observation of a raw Kotlin object also needed the observation wrapper initialized first; the normal ownership wrappers perform that setup.

iOS used Xcode 27.1 beta and the Duo simulator running iOS 27.1. Both iOS simulator and macOS Kotlin frameworks linked, and the Android debug APK and SwiftUI app built. The retained screenshot is the open inner Duo display.

## Native Swift export comparison

The comparison used Kotlin 2.4.20's native Swift export without either bridge library. The working design exported a final `AlarmEditorSession` facade around an internal AndroidX ViewModel. It forwarded the ViewModel's same typed state and commands and owned its `ViewModelStore`. A main-actor Swift `@Observable` adapter observed state without implementing validation or saving again.

The generated API exposed `StateFlow<EditorState>` as a typed Swift Flow/AsyncSequence and suspend saving as `async throws`. Kotlin enums became Swift enums; immutable Kotlin data classes remained reference objects with read-only properties. Export did not automatically create Swift Observation, Sendable conformance, or feature ownership.

| Variant | Observed result | Retained record |
| --- | --- | --- |
| Directly exported public AndroidX ViewModel | API generated; Kotlin native link failed | [Link diagnostic](native-swift-export-direct-link-error.log) |
| Internal ViewModel with final facade, macOS Swift 5 mode | All 11 checks passed | [macOS runtime log](native-swift-export-macos-runtime.log) |
| Facade, macOS Swift 6 with scoped compatibility import | All 11 checks passed during the run | Successful run described here; separate compatibility log not retained |
| Facade, Swift 6 iOS app with compatibility import | All 11 checks passed on Duo | [iOS runtime log](native-swift-export-ios-runtime.log), [screenshot](native-swift-export-duo-open.png) |
| Strict Swift 6 without compatibility import | Actor-isolation compilation failure | [Swift diagnostic](native-swift-export-swift6-errors.log) |

The direct export failed at `linkSwiftExportBinaryDebugStaticMacosArm64` while generating a reverse bridge for the final `ViewModel.addCloseable(key, closeable)` overload. The generated declaration was not found in the ViewModel vtable. This is a specific compiler/library failure in the tested versions; it does not establish that Kotlin inheritance is generally unsupported.

The successful Swift 6 runs used a scoped `@preconcurrency import AlarmEditor` and Void-returning cancellation tasks. Without compatibility treatment, the generated concurrent async method rejected sending the main-actor model; an earlier Task returning the generated result enum also exposed missing Sendable conformance. The retained strict diagnostic records the actor-isolation error. The compatibility import relaxes imported concurrency checking and does not prove arbitrary objects are safe across actors. No blanket `@unchecked Sendable` was added.

The 11 checks covered typed initial state, Observation invalidation, validation, async success and typed failure, Swift Task cancellation, propagation of cancellation to Kotlin save work, observation unsubscription, owner cleanup, exactly-once `onCleared`, and rejection of saves after closure. Fake save work was deliberately lifecycle-owned and explicitly cancelled when its caller cancelled. Production durable alarm commands require a different lifetime below the ViewModel.

The macOS harness used Apple Swift 6.4 and deployment target 15.0. A macOS 14 build failed because generated coroutine support used `Synchronization.Mutex`. The iOS app compiled with deployment target 18.0 and ran on iOS 27.1 with Xcode 27.1 beta; older iOS builds/runtimes were not validated. MathAlarm's current iOS minimum is 26.

The facade generated five Swift modules and five C bridge/runtime targets, merged with Kotlin into a static library. The directly exported lifecycle API generated a larger package surface. Export workers intermittently logged an IntelliJ coroutine class-loading diagnostic even when the facade ultimately linked and ran. The approximately 40.8 MB debug library was an unoptimized experiment product, not a release-size comparison.

## Practice and architectural conclusion

Current JetBrains and Android documentation supports shared Kotlin ViewModels with native UI. JetBrains describes ObservableViewModel integration for SwiftUI; Android documents ownership and clearing of a shared ViewModel from iOS. This makes shared presentation a supported architectural direction, while the particular observation/export library remains an integration choice. [JetBrains ViewModel guidance](https://kotlinlang.org/docs/multiplatform/compose-viewmodel.html), [Android KMP ViewModel guidance](https://developer.android.com/kotlin/multiplatform/viewmodel).

John O'Reilly has documented both platform-specific ViewModels and shared ViewModels. The PeopleInSpace sample inspected for this research consumes Kotlin presentation from both Compose and SwiftUI. These examples demonstrate viable choices; they do not establish a single industry standard. For MathAlarm, keeping validation, draft handling, command results, and challenge progression in one implementation fits the existing MVVM architecture. [Separate ViewModel comparison](https://johnoreilly.dev/posts/swift-kotlin-viewmodel-kmm-comparison/), [Shared ViewModel discussion](https://johnoreilly.dev/posts/kmm-viewmodel/), [PeopleInSpace snapshot](https://github.com/joreilly/PeopleInSpace/tree/5fed3308132406f2cd2139bcd6391bdaeef62338).

**Decision:** share domain, application, data, and feature presentation logic; implement every Android view in Compose and every iOS view in SwiftUI. Use the verified framework bridge initially. Keep state and actions independent of either UI toolkit so the interop mechanism can change later without replacing feature behavior.

Native Swift export is a credible future option through a narrow facade. Defer it until the direct lifecycle link problem, concurrency annotations, packaging, and full production API pass the app's integration gates on a suitably stable exporter. The shared ViewModel boundary remains valid whichever export mechanism is eventually used.

## Limits and retained evidence

The experiments did not validate production Room integration, AlarmKit, real delivery or recovery, audio, scene/process restoration, distribution archives, or complete feature parity. Detail-tree replacement proved retention under a stable owner; it did not prove a real closed/open device transition. Device Hub automation timed out, so actual Duo folding and both orientations remain manual acceptance gates. Only the open display was inspected in this run.

[Evidence manifest](evidence-manifest.json) records file sizes and SHA-256 checksums. Runtime JSON/logs and compact diagnostics are retained unchanged, including historical paths into the deleted experiment projects.
