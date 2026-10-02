# Native UI migration progress — 2 October 2026

The [approved plan](native-ui-migration-plan.md) remains the architectural authority. Milestones 1–5 have completed their documented implementation gates. Milestone 5 native maths preview, delivered challenge, queue readiness/restoration and fault ordering are verified below; physical recovery and release gates remain open. Compose remains Android-owned, shared production code renderer-free, and iOS entirely SwiftUI. App-wide settings and remaining presentation parity remain Milestone 6. This is not full parity or release readiness. Earlier milestone sections retain historical snapshots; the [Milestone 5 record](native-ui-migration-milestone-5-2026-10-02.md) describes current source and evidence. The [baseline](native-ui-migration-baseline-2026-10-02.md) and [results-only experiment archive](research/native-ui-interop-2026-10-02/README.md) remain unchanged.

## Milestone gates

| Milestone | Status | Evidence required before completion |
| --- | --- | --- |
| 1 — Establish the baseline | **COMPLETE** | Concrete inventory/parity matrix, original test results and limitations, supported toolchain/device matrix, focused initialization-retry and delivery-identity regressions |
| 2 — Independent shared presentation and operations | **COMPLETE** | Existing suites and focused regressions pass; resolved Kotlin/Swift bridge versions agree; production Swift observation/cancellation/cleanup/identity checks pass; renderer consumers build; accepted work/session ownership and UI-free startup are verified |
| 3 — Extract Android UI and replace iOS root | **COMPLETE** | Compose lives in Android UI, shared builds without renderer dependencies, native SwiftUI root builds/launches through UI-free bootstrap, editor owners survive detail/layout replacement |
| 4 — Complete native list and editing | **COMPLETE** | List/editor/subpage/sound parity, validation/failure retry/duplicate guards, accepted result acknowledgement, retained drafts/routes across layout/detail replacement, Android editor/tablet regressions; measured limits below |
| 5 — Complete preview and delivered alarms | **COMPLETE — implementation gate** | Native challenge, readiness before exact acknowledgement, ordered replay, independent durable restoration and process restart, retained drafts/routes, audio ownership and fault ordering; physical recovery/release gates remain open |

The final verification record below ties completion to the approved gates: feature contracts contain no Compose/UI types; existing Android consumers build; the production bootstrap and bridge checks work without constructing a Compose controller; observer-removal regressions preserve accepted commands and real sessions; pinned Kotlin/Swift dependencies resolve and work together. Those Milestone 2 results are retained below; the completed renderer extraction and native root have their own Milestone 3 verification record.

## Implemented shared contracts

Feature ViewModels inherit `SharedFeatureViewModel`, which uses KMP-ObservableViewModel and supports explicit, idempotent owner closure. Factories and lifecycle/bootstrap entry points provide native callers with feature instances; constructors retain their DI and repository dependencies inside Kotlin. The domain `Alarm` fields are also read-only; changes use `copy`, preserving all persisted names and mappings. Mutable implementation state is private, and Swift observes immutable `StateFlow` aggregates through NativeCoroutines-generated state accessors.

| Feature | Public state and semantic operations | Native presentation ownership |
| --- | --- | --- |
| Alarm list | `AlarmListState`; loading, domain alarms, retained result IDs, sorting from preference Flow; add/edit/toggle/delete/undo/clear/Skip Next actions and result acknowledgement | Row content, localized action/error copy, confirmation and undo affordances, route/selection ownership |
| Alarm editor | `AlarmEditorState`; numeric `TimeState`, plain string title, weekdays/repeat/challenge/snooze/tone/enabled fields, normalized challenge settings, validation/dirty/saving state, retained `AlarmEditorResult` IDs; `AddEditAlarmEvent`, initialize once and acknowledge result | Text cursor/marked text, keyboard/focus, date/time formatting, pickers/subpage navigation, discard UI and accepted-save navigation |
| Maths challenge | `ChallengeState`; explicit initializing/ready/error, domain occurrence and problems, plain answer, progress and typed retained outcomes; initialize/answer/submit/snooze/acknowledge | Maths rendering, localized feedback, input controls and focus, navigation after accepted resolution |
| App settings | `AppSettingsState`; theme/sort, stable supported announcement IDs, latest manual batch and acknowledged IDs; semantic select/acknowledge actions; retained typed `AppSettingsFailure` IDs for failed writes | Theme application, localized labels/error copy, What's New rendering, feedback/share/external presentation |

Preference observability was converted before the list model: `AlarmPreferencesImpl` now keeps private `MutableStateFlow` storage with read-only theme, sort and seen-announcement flows. Theme/sort/announcement preference keys and migration behavior are unchanged. Announcement batches still use the released stable IDs and supported platform catalog. Native settings actions persist before reporting changed state. A failed preference write leaves the prior selection intact, retains a semantic failure until acknowledged, and permits retry. Supported announcement IDs and the newest manual batch are distinct, so an older unseen announcement remains acknowledgeable after a new batch appears; automatic presentation uses all supported unseen IDs. A failed initial announcement-bookkeeping write still creates the settings owner with a typed retained failure and a catalog fallback; `refreshAnnouncements` retries persistence without blocking delivered-alarm rendering.

The editor publishes immutable drafts and explicit validation (`NOT_INITIALIZED`, `INVALID_TIME`, `INVALID_DAYS`, `NONE`). Its initial-draft comparison excludes constructor timestamp defaults. It retains an allocated database ID immediately after insertion, so a later scheduling failure and retry update the same row. It initializes once per retained session, freezes editable fields while a save is accepted, rejects duplicate saves, and updates the clean baseline after success. Challenge normalization and allowed snooze values remain shared behavior. `TimeState` contains only hour/minute; formatting is in the existing renderer. Compose stores `TextFieldValue` locally and dispatches only its string text.

Important results remain in state with monotonically increasing IDs until acknowledged (owner-local for list/editor/settings; application-session sequence for challenge outcomes, including retries). A native consumer acknowledges the exact ID after presenting the result; navigation results are acknowledged while the native owner is still active, before navigation removes its observer. Observation cancellation does not clear results or suppress later command outcomes. The editor's production `awaitResult(afterId)` suspend API observes the first retained result newer than an acknowledged/result cursor; cancelling its Swift/Kotlin waiter removes observation only, while an accepted save continues in the application scope. Native code must not manufacture successful initialization/completion from a cached key or interpret an unacknowledged failure as success.

## Native API and resolved bridge

Swift starts services through `IosApplication.initialize(scheduler:)`; reconciliation,
prewarming, versioned handoff creation/acknowledgement and durable unresolved-session
restoration no longer require `MainViewController`. The latter remains a compatibility
renderer until Milestone 3. Factories use one private Kotlin DI container:
`SharedFeatures.list`, `settings`, `editor(sessionId, alarm)`, `newEditor(sessionId)`,
`challenge(sessionId)`, and explicit session-end close methods. Objective-C export maps
Kotlin `newEditor` to Swift `doNewEditor(sessionId:)`; the production harness demonstrates
the actual generated API. NativeCoroutines exposes direct observed `model.state`,
`model.stateFlow`, and cancellable `awaitResult(afterId:)`/challenge initialization.
`IosApplication.decodeAlarmHandoffJson` provides a pure shared DTO decoder;
`initializeOccurrence(alarmId, activeAt)` loads saved challenge configuration below
presentation, so Swift does not manufacture a full saved alarm or use a repository.
Decoding alone never consumes or acknowledges a delivery.
Call native lifecycle/factory/actions on Main.

Feature constructors are internal. Kotlin/Android infrastructure stays available where
required but is refined out of Objective-C export; progress storage is internal. The
Room KSP-generated native implementation/constructor declarations need the same
`HiddenFromObjC` refinement as their ports, so a scoped pre-compilation hook applies it
only to those three generated files after KSP, including cached outputs. It changes
export visibility only, with no generated SQL or schema change. Recheck this hook and
headers after Room/KSP upgrades. Legacy CALF/UI exports and the unused Alpha Swift
export DSL stay until the approved Milestone 3 extraction; the new feature/factory
contracts expose no renderer types or repository/DI constructor dependencies.

Actual `iosSimulatorArm64CompileKlibraries` resolution confirms ObservableViewModel
**1.1.0**, NativeCoroutines core/annotations **1.0.6**, Kotlin stdlib **2.4.20**,
Coroutines **1.11.0**, and Lifecycle **2.11.0**. The bridge's older transitive baselines
resolve upward to the approved versions. `Package.resolved` pins the Swift bridges to
ObservableViewModel 1.1.0 (`9c73fbf9c879131715fd71e9e243a4ba10fd53a3`) and
NativeCoroutines 1.0.6 (`54a45fbb15bc0bebc54081f87683830d3bc9e84e`); its transitive
RxSwift package is 6.10.2. Evidence: `/tmp/mathalarm-migration-dependencies.log`,
version catalog and checked-in Xcode SwiftPM resolution. Existing renderer imports
remain for Milestone 3, rather than treating the current entire module as UI-free.

## Ownership and cancellation

| Work | Owner and cancellation rule |
| --- | --- |
| List/settings observation and editor UI state | Native window/screen/session owner; `close()` clears observation, rejects new actions, and performs owner cleanup exactly once |
| Accepted editor save/scheduling | `Usecases.launchCommand` in the application scope; serialized with the existing `Usecases.command` mutex. Owner closure or cancelled collection does not cancel this work. Its outcome can still update retained state after the editor closes |
| Real occurrence initialization/progress/audio/completion/snooze | Application challenge coordinator and durable occurrence identity; replacement ViewModels observe the same session. Real alarm work is independent of screen subscriptions |
| Preview challenge and tone playback | Preview owner; cancellation/closure may stop only its preview. Preview never consumes/persists a real occurrence or its progress. A real delivery takes audio priority |
| Native queue acknowledgement | Native delivery queue owner, after authoritative occurrence initialization is actually ready and unresolved occurrence state is durable |
| Recovery/reconciliation/prewarming | Application/native service lifetime; `IosApplication` initializes scheduler/services before these paths run, without constructing a Compose controller |

`SharedFeatureViewModel.close()` does not cancel the application command scope. The editor owns no audio stop operation. Application session invalidation observes persisted alarm changes in application scope, so removing an editor/challenge UI observer cannot abort invalidation or accepted durable work. Native observers must not call completion/snooze simply as cleanup. Retained factory maps require `closeEditor(sessionId)`/`closeChallenge(sessionId)` at actual session end; automatic ViewModel cleanup alone does not remove their map keys. Keep that responsibility above layout/detail branches.

The accepted-result ordering audit identified an existing recovery risk: schedule edits persist intermediate desired state before OS registration completes, while iOS metadata/snooze cancellation previously cancelled recovery unconditionally. The migration therefore moves recovery cancellation to an explicit `AlarmInteractor.cancelRecovery` service operation. Completion cancels recovery before committing its accepted resolution; scheduling cancels regular/snooze registrations, submits replacement scheduling, then cancels recovery and commits final state after acceptance. Failed scheduling retains the prior active identity/count/snooze in its error state. Editor code delegates registration cancellation to scheduling instead of pre-emptively cancelling all registrations. Application session invalidation observes persistence in the application scope and accepts only final successful state (`scheduleError == null`), so intermediate or failed edits cannot silently stop an unresolved real session. Metadata-only updates/snooze cancellation preserve independent recovery. This retains the approved architecture and fixes service ordering below presentation; focused scheduling, completion and metadata-update regressions cover this acceptance ordering.

Durable real sessions use authoritative `activeAt` identity; native delivery tokens identify deliveries rather than only the recurring alarm ID. Failed initialization remains an error and is retryable. Later valid deliveries remain ordered and unacknowledged while the current occurrence is unresolved. After process death, restoring durable active occurrences is required in addition to replaying the native pending queue. Restoration matches known `(alarmId, activeAt)` across v1/v2 payloads, retains queued v2 tokens/UUIDs, and does not create a duplicate that blocks later deliveries. Pre-v2 alarmId-only entries are enriched with the authoritative restored occurrence while retaining their queue UUID; the existing queue key/shape stays intact, and unknown v2 tokens or distinct later occurrences are preserved. Shared acknowledgement also matches the known occurrence across payload versions only after actual readiness.

## Focused verification added

The production project, rather than recreated experiments, contains checks for:

- An accepted save waiting behind another application command completes after its editor owner closes, writes the saved alarm, preserves scheduled occurrence evidence and retains/acknowledges its result.
- Cancelling a production suspend result waiter leaves the accepted save running; its later permission failure remains retained, retry writes/registers the alarm, and successful save clears dirty state while the earlier failure remains available for acknowledgement.
- Invalid editor time writes/registers nothing, retains a typed failure, accepts corrected input and successfully saves on retry.
- Preview uses the unsaved editor draft without writing a saved alarm or registering an occurrence; acknowledgement retains the same draft.
- Preference changes update shared settings state, acknowledged announcements and selected theme/sort survive recreation, and a closed settings owner stops observing/rejects new actions.
- Failed preference persistence retains the prior selection and typed failure across a successful retry, then clears only the acknowledged failure ID. Failed announcement bookkeeping during startup still creates the owner; refresh retries storage successfully without dropping the retained failure.
- Challenge initialization failure followed by retry, occurrence/queue identity, stale-command guards and real-session observer replacement are checked in the coordinated migration suites.
- Failed progress writes retain the current problem and answer with a typed error; retry persists before advancing. Real initialization survives cancellation while queued behind the command mutex. Partial audio setup and preview-owner removal cannot stop a real audio owner or leak preview resources.
- Swift production-framework checks cover generated state observation, Flow/task cancellation, retained editor identity through detail replacement and mounted SwiftUI owner removal. Their measured final runtime result is recorded below.

### Milestone 2 verification record

Original tests passed **417 Android + 339 iOS**, plus 2 Python tests and the native
queue/recovery smoke. The initial core native baseline orchestration was interrupted;
a full unchanged-binary rerun passed all 134 tests. There were no reproduced baseline
assertion failures. Historical device/release gaps remain in the baseline and linked
manual records.

The completed integrated migration suite passes **440 Android + 363 iOS**:

| Final check | Result / evidence |
| --- | --- |
| Core Android host | 136 passed, no failures/errors/skips |
| Shared Android host | 212 passed, no failures/errors/skips |
| Android app unit/Robolectric | 92 passed, no failures/errors/skips |
| Core iOS simulator | 135 passed, no failures/errors/skips |
| Shared iOS simulator | 228 passed, no failures/errors/skips |
| Android Debug app | `assembleDebug` passed, existing renderers consume the new contracts |
| Python delivery runner suite | 2 passed |
| Foundation native queue/recovery smoke | Passed, including ordered prepend/relaunch, distinct delivery IDs, London DST gap/overlap identity, registration failure rollback, stale/in-flight cancellation and extended recovery |
| Resolved Gradle and Swift bridges | Passed; pinned versions and revisions above |
| Production Swift mounted observation/lifetime checks | 7 groups passed on the final unskipped Debug framework/app, including mounted owner removal, DTO decoding and identity-only failed initialization |
| iOS unsigned Release device framework/app | Passed on final source; unskipped optimized arm64 framework and generic-device app build, signing disabled |
| Simulator and device public headers | Passed; bootstrap/feature/domain APIs present, no Koin/repository/database/DAO/progress-store/review-store exports |
| Physical-device reliability/native presentation | Not run in this milestone; open gates below |

Commands are in [testing.md](testing.md#production-shared-ui-bridge-checks). Final test
log: `/tmp/mathalarm-migration-verified-tests.log` (`BUILD SUCCESSFUL`, 1 minute 1 second),
with module XML reports under `build/test-results` on 2 October 2026. This adds 23
passing Android cases and 24 passing iOS cases to the original totals. Integration
compile/test issues were corrected before final verification (including the new
cancelled-initialization test's coroutine extension import and an announcement fixture
that initially confused the released all-unseen batch behavior with restored batch
history). No new failure remains in the final suites. No schema file changed.

Final unskipped Xcode builds passed: Debug simulator framework/app (`/tmp/mathalarm-migration-final-xcode-debug.log`, Kotlin build 1m15s) and unsigned generic-device Release framework/app (`/tmp/mathalarm-migration-final-xcode-release.log`, Kotlin build 6m). All seven production runtime groups passed in `build/ios-shared-bridge.log`; both final framework headers passed the infrastructure/export audit. The disposable verification simulators were deleted, preserving the existing user simulator.

**Milestone 2 is complete:** toolkit-free feature contracts and adapted Android consumers, UI-free bootstrap, observer-independent accepted work/session ownership, pinned resolved bridges and actual production Swift observation/cancellation/cleanup/retained-identity checks satisfy the approved gate. Production runtime checks use iOS 26.5 with Xcode 27.0; minimum supported iOS 26.0 runtime validation remains open. This historical Milestone 2 result does not claim audible output, AlarmKit physical delivery, distribution signing or TestFlight/archive completion. Milestone 3 completion is recorded separately below.

## Milestone 2 handoff (completed by the Milestone 3 record below)

1. Move the current Compose screens/components/navigation/theme/resources/catalogs and renderer tests to `androidApp`, preserving their new aggregate-state consumers. Keep UI localization extensions, `TextFieldValue`, formatted time, route types and platform presentation launchers in native UI packages.
2. Remove shared renderer dependencies, the obsolete Swift export configuration and CALF framework export after Android extraction. Preserve the framework name `app`, domain DTOs, feature state APIs, Koin runtime ownership and application services.
3. Replace the iOS app root with SwiftUI. Use the UI-free `IosApplication` bootstrap and native lifecycle/queue entry points; do not initialize Koin or prewarm Room by creating a view/controller.
4. Own one editor session ID above split/stack layout and nested pickers/Test Alarm. Construct its ViewModel via `SharedFeatures.editor`/`newEditor`, retain it with `@StateViewModel`, observe children with `@ObservedViewModel`, and close/remove the session only when saved/discarded and its editing session ends. Do not construct feature ViewModels in `body`.
5. Own real challenges by retained occurrence identity. Decode native payloads with `IosApplication.decodeAlarmHandoffJson` and call the identity-only `initializeOccurrence` API. Bind replacement views to the application session, prioritize real deliveries without discarding editor drafts, and acknowledge native deliveries only after actual readiness. Native view/task cleanup cancels observation, not the unresolved alarm.
6. Carry the production Swift interop harness into CI/native owner tests. Continue testing real owner removal, cancellation and retained identity after dependency changes; an offscreen host or historical experiment result does not establish the production gate.
7. Build/launch Android and the new SwiftUI root, check shared dependency/import boundaries, and verify draft retention during actual compact/expanded transitions. Full native parity remains Milestones 4–6; no Compose fallback should be introduced on iOS.

## Milestone 3 implementation and verification

The extraction keeps the approved architecture: `core` owns domain contracts; `shared` owns application operations, data, platform service adapters and feature ViewModels; `androidApp` owns Compose; `iosApp` owns SwiftUI presentation and navigation. Framework name `app` and the pinned bridge/toolchain versions remain unchanged. There is no iOS Compose fallback.

### Changes against the approved scope

- Compose screens, components, theme, navigation, nine Lyricist catalogs, renderer helpers and seven renderer test classes moved into `androidApp`, with their packages, route identities and test tags preserved. Shared handoff codecs and domain calculations remain in Kotlin. Android platform presentation launchers/dialogs/ringtone picking moved alongside their callers; service adapters remain below UI.
- Original UI resource bytes moved into Android drawables/assets. The pinned Compose resource plugin does not generate resources for the AGP 9 Android application module, so Android uses `R.drawable` and the native asset manager. All 32 tracked source assets remain byte-identical; sound resources and IDs were not changed. The icon generation script targets Android resources.
- Shared Compose/CALF/Lyricist/navigation renderer dependencies, resource generation, CALF export and obsolete Swift export DSL were removed. Explicit `koin-core-viewmodel` and the already resolved Lifecycle 2.11.0 replace transitive renderer dependencies. Obsolete iOS controller/presentation entry points and unused UI navigation events were removed. Native service implementations are hidden from Swift export while established contracts/bootstrap remain available.
- The SwiftUI root starts through `IosApplication`, renders the shared list model and uses native split/stack navigation. List actions, result/error presentation, deletion confirmation and undo use shared events. Editor, sound, preview, challenge and settings development screens clearly state their later milestone status. No save, challenge readiness or real-delivery acknowledgement is simulated.
- A window registry owns stable editor/session IDs and retained per-editor routes above detail/layout branches. Always-mounted `@StateViewModel` owners retain factory models; children use `@ObservedViewModel`. Switching details or nested routes does not close a draft. Explicit discard closes its factory session; window end closes retained factory keys. Native navigation generations reject outgoing stack writes, and restored routes hydrate only after destination registration. No shared ViewModel is constructed in `body`.
- Native unresolved-occurrence restoration completes before queue replay is presented. Pending real deliveries retain their persisted queue order and identity. The M3 challenge owner remains idle and unacknowledged until M5 establishes authoritative readiness. Cancelling observations or removing native owners does not complete/snooze a real occurrence, stop application audio, erase progress, or cancel accepted commands.

No database name, schema migration, preference key, announcement ID, registration identity, legacy handoff format or persisted progress format changed. Archived experiments remain results-only; verification was extended in the production app and scripts.

### Verification and baseline comparison

The original baseline had no reproduced assertion failures (417 Android / 339 iOS). The fresh M1–2 starting implementation documented 440 Android / 363 iOS. The extraction keeps all 440 Android tests: core 136, shared 172, Android app 132. The iOS suites contain core 135 and shared 188 (323 total); the difference is exactly the 40 renderer cases moved from shared to Android, rather than deleted behavior coverage. All final reports have zero failures/errors/skips.

Integration caught and corrected Android resource-generation assumptions, formerly transitive ViewModel dependencies, Swift actor cleanup compilation and native nested-route restoration. The mounted routing check also caught a test assumption: SwiftUI can retain the root without repeating `onAppear` after pop. Verification now checks the settled native visible stack/title field for that transition; nested routes still require appearances from the current rendering generation.

### Milestone 3 final gate evidence

| Check | Final result / evidence |
| --- | --- |
| Full shared/core/Android suites and Android Debug/test APKs | Passed, `/tmp/mathalarm-m3-final-tests-build.log`; totals 440 Android / 323 iOS as explained above |
| Final affected source after shared API/export cleanup | Shared Android 172, app 132, shared iOS 188 and `assembleDebug` passed, `/tmp/mathalarm-m3-final-affected-tests.log`; unchanged core reports remain 136 Android / 135 iOS |
| Moved renderer tests | All 40 passed in Android app; seven classes; route/tag/resource audit passed |
| Android representative navigation/layout | Three instrumentation tests passed on API 35: compact familiar sheet; expanded editor/settings draft protection; hidden draft after activity recreation. Logs `/tmp/mathalarm-m3-android-compact.log` and `/tmp/mathalarm-m3-android-expanded.log`; screenshot evidence `build/native-ui-m3/android/`. Emulator size/density restored after checks |
| Shared source, graph and framework exports | `verify_native_ui_boundaries.py` passed against final simulator `app.framework` and `/tmp/mathalarm-m3-dependencies.log`; no Compose/CALF/Lyricist renderer dependencies/imports/exports; feature/bootstrap APIs present and infrastructure hidden |
| Normal unskipped iOS Debug framework/app | Xcode build passed, `/tmp/mathalarm-m3-xcode-debug.log`, Xcode 27.0 / iOS 26.5; no Kotlin skip or Compose fallback |
| Production mounted Swift ownership/navigation | **10 groups passed on iPhone and iPad**, `build/ios-shared-bridge-m3.log` and `build/ios-shared-bridge-m3-ipad.log`. Both runs use the production owner layer and native editor stack, two retained drafts, nested sound/challenge routes, compact/expanded/detail replacement, visible pop-to-editor title, Swift observation, Flow/suspend cancellation, explicit closure and actual mounted/window cleanup |
| Native queue/recovery regression checks | Foundation queue, delivery identity and recovery smoke passed; retained native idle challenge queue bytes/order remain identical through observer/window cleanup; shared accepted-command/progress/audio/recovery suites pass |
| Verification scripts | 10 Python tests passed, including transcript completeness and renderer boundary regressions |
| Normal native root launch/visual inspection | Final app launched on iPhone 17 Pro and iPad Pro 11-inch M5; native list/empty/sidebar/detail and safe areas inspected. Screenshots `build/native-ui-m3/ios-iphone-root.png` and `ios-ipad-root.png` |
| Physical alarm, minimum iOS 26.0, archive/distribution | Not established by this milestone. Current-source Release/device/archive was not rerun; prior M2 unsigned build is historical evidence only |

**Milestone 3 is complete against its approved gate.** Android rendering is confined to `androidApp`, shared builds and exports without UI renderers, the production iOS root builds and launches entirely in SwiftUI, and mounted native owner checks preserve editor identity through layout/detail/nested-destination replacement. No architectural deviation or bridge version change was required. All new iOS presentation is SwiftUI. The two disposable verification simulators and test emulator are cleaned up after verification, preserving the existing user simulator and physical devices.

### Reviewable commit boundaries

Milestone 1 (`41a97c7`) records the approved plan, baseline and retained research. Milestone 2 (`ef7136c`) records shared contracts, operation ownership, UI-free startup and the adapted existing Compose consumers. Its boundary was reconstructed in a temporary checkout without changing the completed working files, then freshly verified: 440 Android / 363 iOS tests, Android Debug build, native framework link, normal Xcode Debug build, all seven production Swift bridge groups and native queue/delivery/recovery smoke passed. Logs: `/tmp/mathalarm-milestone-commits/m2-verification.log`, `m2-xcode.log` and `m2-bridge.log`. The staged tree was checked byte-for-byte against that tested checkout.

The following Milestone 3 commit contains the Android extraction, renderer dependency removal, native SwiftUI root/owners and extended production verification documented above. Its functional source matches the already verified final implementation. IDE state, Firebender files, old Kotlin error-log deletions, Xcode user state and Android release mapping remain outside these commits. No physical device was changed; the additional boundary-verification simulator was removed.

## Milestone 4 implementation and gate

The list consumes shared loading, empty, sorted alarms and retained results. Native controls dispatch create/edit, enable/disable, delete, undo and confirmed clear actions. Selected rows and the retained-draft menu restore the same session. Scheduling/cancellation failures use localized typed errors, with no optimistic success or Swift scheduling implementation.

Native Forms now provide time, label, enabled, weekdays/repeat, Easy/Medium/Hard/Custom challenge, question count, mixed difficulty counts, operations/ranges and snooze interval/maximum controls. Kotlin continues to normalize, validate, compare drafts, persist and register alarms. Save/Cancel use native toolbar roles; dirty cancellation confirms discard. Saving freezes controls and rejects duplicate actions. A failed save retains its draft, including an allocated row ID after insertion, and permits retry. Retained `SaveAlarm` IDs are acknowledged by the still-mounted owner before that specific factory session is closed. Failed result IDs remain until explicit handling; observer or presentation removal does not acknowledge them.

Owners are constructed through `SharedFeatures`, held by `@StateViewModel` above presentation branches, and borrowed by `@ObservedViewModel`. Per-session draft/routes, permission-request duplicate guards and staged sound choices survive compact/expanded replacement, detail replacement and returning from nested destinations. A structural `@StateObject` window-lifetime token closes factory sessions when the owner layer is removed; layout changes do not remove that layer. Native Form/alert caches can retain an **inert** outgoing registry beyond hosting-controller teardown. Verified cleanup removes every factory key and closes its models independently of that cache; late delivery/navigation callbacks cannot recreate owners. A separate mounted probe verifies automatic `@StateViewModel` closure before explicit factory removal. No arbitrary deallocation deadline is claimed.

Sound selection stages a choice separately from preview playback. Done applies the shared tone event; Back confirms discarded staged changes. Presentation replacement preserves selection; disappearance/backgrounding stops only that presentation's preview lease, and stale outgoing callbacks cannot stop its replacement. `AlarmTonePreview` delegates to the existing platform audio owner; real alarm audio blocks/interrupts previews. All six CAF resources and sound IDs remain intact, with Orbit fallback for missing tones. No player/arbitration was added to views.

Narrow shared additions are `ToggleEnabled`, semantic challenge mixing/count/operation intents, list `pendingOperations`/`canUndoDelete`, and owner-scoped tone preview completion/stop contracts. Explicit editor disable cancels before committing disabled occurrence state; cancellation failure preserves the old occurrence. Metadata edits preserve a concurrently changed enabled state unless the user explicitly edited it. Enabling or retrying failed registration schedules through existing use cases. Regression coverage also preserves already-disabled active metadata. No framework name, bridge version, database/schema/key, registration/announcement identity, legacy handoff format or persisted progress format changed. Android-only Skip Next/review and absence of an iOS vibration toggle remain unchanged.

A native String Catalog contains 152 presentation keys in all nine existing locales (`en`, `es`, `de`, `ru`, `pt`, `hi`, `pa`, `bn`, `zh`), including plural forms. Existing translations are ported where applicable; sound brand names remain stable. Time, weekdays, lists and numbers use native formatting. Native controls expose labels, selected sound traits and test identifiers; preview targets are at least 44 points. Mounted captures verify representative light/dark and accessibility-size layouts on both simulator families. Keyboard focus is exercised; software-keyboard occlusion, screen-reader traversal, physical resizing/folding and comprehensive linguistic review remain manual evidence, not inferred from these captures.

### Final verification

| Check | Result / evidence |
| --- | --- |
| Rechecked starting state | HEAD `4c2072a`, prior `ef7136c` and `41a97c7` verified; unchanged suites/build passed in `/tmp/mathalarm-m4-baseline-tests.log`; baseline Python 10 and native queue/recovery smoke passed. No baseline assertion failure reproduced |
| Forced final core/shared/Android suites | **458 Android / 341 iOS**, zero failures/errors/skips; `/tmp/mathalarm-m4-verified-tests.log`. Android: core 136, shared 190, app 132. iOS: core 135, shared 206. The 18 new common regressions pass on both platforms; all moved renderer tests remain in Android |
| Android editor/navigation/draft protection | **3 instrumentation checks passed** on final APK, API 35: compact sheet, expanded editor/settings, hidden draft after recreation. `/tmp/mathalarm-m4-android-compact-final.log`, `/tmp/mathalarm-m4-android-expanded-final.log`; captures `build/native-ui-m4/android/`. Disposable emulator configuration restored |
| Native boundaries/dependencies/exports | Production `app.framework` and resolved graph passed `verify_native_ui_boundaries.py`; `/tmp/mathalarm-m4-dependencies.log`. Renderer-free shared imports/exports and approved Kotlin 2.4.20, Coroutines 1.11.0, Lifecycle 2.11.0, ObservableViewModel 1.1.0, NativeCoroutines 1.0.6 remain verified |
| Normal unskipped iOS framework/app | Debug Xcode build passed, `/tmp/mathalarm-m4-xcode-verified.log`, Xcode 27.0 / SDK 27.0 / simulator runtime 26.5; normal native root launched on iPhone 17 Pro and iPad Pro 11-inch M5 |
| Production Swift verification | **12 groups passed on both iPhone and iPad**, `build/ios-shared-bridge-m4.log`, `build/ios-shared-bridge-m4-ipad.log`: observation/cancellation, multiple retained drafts, native nested routes, staged sound/lease replacement, validation retry, duplicate save, acknowledged accepted result/session end, disabled-fixture list delete/undo/clear, independent automatic owner and structural factory cleanup |
| Audio/handoff/recovery | Seven preview lease regressions plus existing platform/application audio, accepted command, progress and recovery suites pass. Foundation queue/recovery smoke passed (`/tmp/mathalarm-m4-handoff-smoke.log`). Both mounted runs preserve unresolved native queue bytes/order/readiness through editor/preview/observer/window cleanup |
| Localization/scripts/resources | **17 Python tests passed**; nine-locale production audit passed. String Catalog compiled into all nine `.lproj` bundles; plural formatting sampled at 1/2/5/10 for each locale. All six bundled tones packaged |
| Presentation evidence | `build/native-ui-m4/iphone/`, `ipad/`, `ios-iphone-root.png`, `ios-ipad-root.png`; native Forms, repeat/snooze/challenge/sound, list/undo/empty, dark/accessibility-size and label focus inspected. Mounted actions are semantic integration checks, not a full automated tap/VoiceOver suite |

Integration exposed an incorrect first implementation that cleared occurrence counters on metadata edits of already-disabled alarms; it was corrected and covered before final suites. A Swift `AlarmKit.Alarm` name ambiguity was corrected by qualifying shared models. Native transition/alert captures now wait for observable settlement instead of treating `onAppear` as animation completion. Settled capture checks exposed a lingering validation alert after detail replacement: editor alert presentation moved above the replaceable navigation detail. List/editor alerts yield to delivery presentation, retain unacknowledged shared errors and replay them on return; actions capture and guard the matching active draft/result before retry or acknowledgement. Permission-request guards are session-owned and survive stack replacement; mounted checks verify duplicate reservation and retained guards without invoking an OS permission prompt. Gradle cache sandbox access and an Xcode build-database disk-full failure were environment failures; cache access was approved and only this task's disposable verification resources were cleaned up before successful final builds. No new failing test/build remains.

**Milestone 4 is complete against its approved implementation/parity gate.** This includes sound-tone preview only. Real pending handoffs still remain idle and unacknowledged; no editor/picker/preview cleanup resolves them, clears progress, stops real audio or cancels recovery. Framework name `app`, narrow Swift APIs and approved toolchain remain unchanged. The milestone is independently committed without unrelated IDE/generated changes or pushing.

### Concrete Milestone 5 handoff

1. Implement the native maths Test Alarm destination against the retained editor draft and shared preview challenge contract. The M4 `preview` route is clearly labelled development and preserves its editor. Keep sound-tone preview separate; route audio through the one platform owner.
2. Implement delivered challenge initialization/readiness/error/retry, answer/progress, accepted completion and snooze. Only authoritative `.ready` with durable unresolved state may acknowledge the matching pending queue item. Preserve later items and restore unresolved occurrences independently of native queue replay.
3. Test interruption of editor/tone/maths preview by a real occurrence and return to the same draft/path. Test multiple ordered deliveries, stale/duplicate commands, same-alarm overlapping-occurrence policy and restart after acknowledgement before resolution.
4. Extend production Swift checks around real readiness, cancelled observation, completion/snooze failures and recovery/progress persistence. Add crash/fault injection around registration acceptance versus storage and resolution cancellation versus final writes; do not make cleanup authoritative resolution.
5. Re-run current shared/Android suites, the three Android layout checks, normal unskipped app build and both simulator verification families. Carry all physical AlarmKit, audible-output, keyboard/VoiceOver, minimum-runtime, fold, overnight and release/archive gates below; app-wide settings and remaining localization/accessibility parity remain M6.

## Remaining physical/release evidence

The [testing guide](testing.md), [iOS release-readiness record](ios-release-readiness-2026-09-24.md) and [recovery manual matrix](ios-alarm-recovery-manual-test.md) remain required. Simulator/host results cannot establish audible output, locked authentication/recovery timing, terminated background intents, uninterrupted extended recovery, Bluetooth/DND routing, recurring AlarmKit delivery, permission revocation/regrant, timezone travel, or overnight/multi-day reliability.

Manual Duo folding in both orientations, iPhone compact and iPad resizing/keyboard/toolbar/full-column checks remain native UI gates. Prior iPad landscape preview return, launch artwork and dark title contrast issues need revalidation against SwiftUI. Expected timestamp derivation is not proof of AlarmKit relative weekly delivery timing. A stale/unknown timestamp is rejected rather than consuming a newer occurrence; physical DST/timezone delivery must also verify cleanup of already-armed recovery for rejected stale deliveries.

Room writes and native registration/cancellation are separate operations. The observer-removal regressions prove accepted work keeps running in the application scope; they do not prove every process-crash or post-OS-acceptance storage-failure window is compensated. Schedule replacement writes desired state before OS acceptance, and completion cancels native registrations before its final Room update. Add crash/fault injection around those existing ordering windows in Milestone 5 and the release reliability matrix; retain the unresolved/reconciliation evidence when reporting a failure.

The native recovery/progress formats remain keyed per alarm. Distinct delivery tokens prevent queue collisions, but simultaneous unresolved occurrences of the same repeating alarm cannot have independent recovery registrations/progress; verify and resolve that policy in Milestone 5 before claiming multiple-delivery reliability. Legacy queued payloads and old metadata remain accepted; exact occurrence identity can be absent until authoritative loading. DST identity derivation has gap/overlap smoke coverage, while physical timezone travel remains open.

Existing archive/TestFlight/privacy/support/signing/version gates are not closed by this migration work.


## Milestone 5 implementation and gate

See the [full Milestone 5 verification and Milestone 6 handoff](native-ui-migration-milestone-5-2026-10-02.md) for ownership, API, repeating-occurrence policy, readiness/queue/result ordering, crash windows, exact counts, baseline distinctions and open physical gates. Native maths Test Alarm and delivered challenge replace their development destinations. Shared Kotlin retains problem generation, validation, progress persistence and application coordination; SwiftUI owns presentation/input/navigation.

The fault-ordering description in the earlier Milestone 2 snapshot is superseded: completion/snooze/edit now persist accepted state and cleanup debt **before** recovery/notification cleanup. Desired snooze persistence journals the exact native timestamp before acceptance; reconciliation repairs it without creating another delay. Readiness persists exact unresolved progress before acknowledgement, and restoration independently reloads acknowledged unresolved occurrences.

One unresolved challenge per repeating alarm is the explicit persistence-compatible policy. Later repeating alerts retain date-specific native tokens and coalesce only after shared durable association; exact known schedule membership is retired to prevent legacy replay. Distinct coalesced queued tokens remain independently acknowledgeable. Different alarms remain ordered. No schema, preference/progress key, bridge version or framework-name migration was required.

Final Kotlin/Android suites pass: 487 Android (156 core / 199 shared / 132 app), 370 iOS (155 core / 215 shared), zero failures/errors/skips. Three Android instrumentation checks, 19 Python checks, native queue/recovery fault smoke, nine-locale source/resource checks and renderer-free source/resolved graph/framework export checks pass. **Twenty production Swift groups pass on both iPhone and iPad**, including native answer focus/input and fresh-process progress restoration. The normal unskipped framework/app build and ordinary simulator launches pass; preview/delivered captures confirm full-pane layout, safe areas, toolbar placement, dark appearance and accessibility-size text. **Milestone 5 is complete against its documented implementation gate.** Physical recovery/release gates remain open independently, together with the minimum-runtime, accessibility-client, software-keyboard and full locale/RTL matrices listed in the verification record.

### Concrete Milestone 6 handoff

Implement app-wide theme/sort, What's New, feedback/share, launch visuals and remaining native presentation parity. Recheck all nine locales/RTL, Dynamic Type, keyboard and accessibility across every screen. Preserve retained factory owners, per-draft navigation/presentation/permission state, semantic result acknowledgement, readiness-before-exact-head-acknowledgement, durable restoration, repeating coalescing and single-owner real/preview audio arbitration. Extend the production checks for settings while permanent XCTest/CI remains Milestone 7. Physical recovery, locked intents, audible output, routing, multi-day recurrence, minimum runtime and distribution remain Milestone 8/release gates; do not introduce Compose fallback.
