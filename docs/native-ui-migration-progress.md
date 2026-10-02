# Native UI migration progress — 2 October 2026

The approved plan remains the architectural authority. Milestones 1–2 are complete; Compose renderers and the iOS Compose root remain until Milestone 3. This is not native feature parity or release-readiness evidence.


## Milestone gates

| Milestone | Status | Evidence required before completion |
| --- | --- | --- |
| 1 — Establish the baseline | **COMPLETE** | Concrete inventory/parity matrix, original test results and limitations, supported toolchain/device matrix, focused initialization-retry and delivery-identity regressions |
| 2 — Independent shared presentation and operations | **COMPLETE** | Existing suites and focused regressions pass; resolved Kotlin/Swift bridge versions agree; production Swift observation/cancellation/cleanup/identity checks pass; renderer consumers build; accepted work/session ownership and UI-free startup are verified |
| 3 — Extract Android UI and replace iOS root | **Not started** | Compose lives in Android UI, shared builds without renderer dependencies, native SwiftUI root builds/launches through UI-free bootstrap, editor owners survive detail/layout replacement |

The final verification record below ties completion to the approved gates: feature contracts contain no Compose/UI types; existing Android consumers build; the production bootstrap and bridge checks work without constructing a Compose controller; observer-removal regressions preserve accepted commands and real sessions; pinned Kotlin/Swift dependencies resolve and work together. Renderer removal and the SwiftUI root remain Milestone 3.

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

**Milestone 2 is complete:** toolkit-free feature contracts and adapted Android consumers, UI-free bootstrap, observer-independent accepted work/session ownership, pinned resolved bridges and actual production Swift observation/cancellation/cleanup/retained-identity checks satisfy the approved gate. Production runtime checks use iOS 26.5 with Xcode 27.0; minimum supported iOS 26.0 runtime validation remains open. This historical Milestone 2 result does not claim audible output, AlarmKit physical delivery, distribution signing or TestFlight/archive completion. Milestone 3 has not started.

## Concrete handoff for Milestone 3

1. Move the current Compose screens/components/navigation/theme/resources/catalogs and renderer tests to `androidApp`, preserving their new aggregate-state consumers. Keep UI localization extensions, `TextFieldValue`, formatted time, route types and platform presentation launchers in native UI packages.
2. Remove shared renderer dependencies, the obsolete Swift export configuration and CALF framework export after Android extraction. Preserve the framework name `app`, domain DTOs, feature state APIs, Koin runtime ownership and application services.
3. Replace the iOS app root with SwiftUI. Use the UI-free `IosApplication` bootstrap and native lifecycle/queue entry points; do not initialize Koin or prewarm Room by creating a view/controller.
4. Own one editor session ID above split/stack layout and nested pickers/Test Alarm. Construct its ViewModel via `SharedFeatures.editor`/`newEditor`, retain it with `@StateViewModel`, observe children with `@ObservedViewModel`, and close/remove the session only when saved/discarded and its editing session ends. Do not construct feature ViewModels in `body`.
5. Own real challenges by retained occurrence identity. Decode native payloads with `IosApplication.decodeAlarmHandoffJson` and call the identity-only `initializeOccurrence` API. Bind replacement views to the application session, prioritize real deliveries without discarding editor drafts, and acknowledge native deliveries only after actual readiness. Native view/task cleanup cancels observation, not the unresolved alarm.
6. Carry the production Swift interop harness into CI/native owner tests. Continue testing real owner removal, cancellation and retained identity after dependency changes; an offscreen host or historical experiment result does not establish the production gate.
7. Build/launch Android and the new SwiftUI root, check shared dependency/import boundaries, and verify draft retention during actual compact/expanded transitions. Full native parity remains Milestones 4–6; no Compose fallback should be introduced on iOS.

## Remaining physical/release evidence

The [testing guide](testing.md), [iOS release-readiness record](ios-release-readiness-2026-09-24.md) and [recovery manual matrix](ios-alarm-recovery-manual-test.md) remain required. Simulator/host results cannot establish audible output, locked authentication/recovery timing, terminated background intents, uninterrupted extended recovery, Bluetooth/DND routing, recurring AlarmKit delivery, permission revocation/regrant, timezone travel, or overnight/multi-day reliability.

Manual Duo folding in both orientations, iPhone compact and iPad resizing/keyboard/toolbar/full-column checks remain native UI gates. Prior iPad landscape preview return, launch artwork and dark title contrast issues need revalidation against SwiftUI. Expected timestamp derivation is not proof of AlarmKit relative weekly delivery timing. A stale/unknown timestamp is rejected rather than consuming a newer occurrence; physical DST/timezone delivery must also verify cleanup of already-armed recovery for rejected stale deliveries.

Room writes and native registration/cancellation are separate operations. The observer-removal regressions prove accepted work keeps running in the application scope; they do not prove every process-crash or post-OS-acceptance storage-failure window is compensated. Schedule replacement writes desired state before OS acceptance, and completion cancels native registrations before its final Room update. Add crash/fault injection around those existing ordering windows in Milestone 5 and the release reliability matrix; retain the unresolved/reconciliation evidence when reporting a failure.

The native recovery/progress formats remain keyed per alarm. Distinct delivery tokens prevent queue collisions, but simultaneous unresolved occurrences of the same repeating alarm cannot have independent recovery registrations/progress; verify and resolve that policy in Milestone 5 before claiming multiple-delivery reliability. Legacy queued payloads and old metadata remain accepted; exact occurrence identity can be absent until authoritative loading. DST identity derivation has gap/overlap smoke coverage, while physical timezone travel remains open.

Existing archive/TestFlight/privacy/support/signing/version gates are not closed by this migration work.
