# Native UI migration — Milestone 5 verification, 2 October 2026

The approved migration plan remains authoritative. This change implements native maths preview and delivered challenge without changing the renderer boundary or pinned bridge/toolchain versions. **Milestone 5 is complete against its documented implementation gate**, with the measured evidence below. Physical recovery and release gates remain open.

## Changes and ownership

`NativeChallengeView` renders shared readiness, problems, raw answer entry, validation feedback, progress, finishing, completion and snooze using SwiftUI. Kotlin generates problems, normalizes integer input (including supported Unicode digits/signs), validates answers and owns challenge rules. Native formatting and the existing nine-locale String Catalog supply presentation, accessible maths labels and feedback. The screen supplies focus, keyboard Done/Clear/Submit controls, accessibility announcements, scroll dismissal, safe areas and accessibility-size stacked buttons. It fills the available pane; preview and real delivery have distinct titles and controls.

`NativeWindowSessions` owns per-editor preview sessions and delivered presentation above navigation/layout branches. Factory models are constructed through `SharedFeatures`, retained by `NativeSessionOwners` with `@StateViewModel` and borrowed with `@ObservedViewModel`. No feature model is constructed in a view body. Preview uses the editor's retained semantic Test Alarm snapshot and never saves/registers an alarm, consumes a delivery, changes persisted occurrence/progress or cancels recovery. Explicit Cancel/completion removes only that preview and returns to its previous nested route. Stale outgoing cancellation/presentation callbacks cannot close a replacement preview.

Real initialization and accepted commands remain application-owned. Removing observation, dismissing a cover, cancelling an initialization waiter or closing factory/window owners does not complete/snooze a real occurrence, silence unresolved real audio, erase progress or cancel recovery. Real playback takes priority through the existing single platform audio owner; hidden previews cannot reclaim it. Preview cleanup stops only its own lease. Retained editor models, nested routes, staged tone and pending permission guards survive interruption and layout/detail replacement.

Shared APIs remain narrow: captured-question-index `submitAnswer`, audio retry, semantic handoff disposition and authoritative delivery association/unresolved identity checks. Readiness/result/domain DTOs stay renderer-free. `app` remains the static framework. Database names, schema/migrations, preference/progress keys, announcement/sound/registration identities and legacy v1/v2 handoffs are unchanged. Android-only Skip Next/review and absence of an iOS vibration switch are preserved.

## Readiness, queue, restoration and result handling

Launch, scene activation and root retry load durable unresolved occurrences independently of the native queue. A failed read or native restoration write presents a truthful retryable failure and leaves queued handoffs intact. Restored progress contains the exact questions, index, start time and incorrect count. Recovery repair and alert collection run at service lifetime, not from a challenge view's observation.

Native alert collection queues all deliveries in stable order before stopping/arming individual alerts. AppIntents retain exact delivery identity before recovery operations; failed authoritative lookup preserves raw delivery identity. Native acknowledgement requires shared READY, a durable unresolved occurrence ID and matching alarm/activeAt. It removes only the exact queue head; later entries remain. Obsolete authoritative identities are explicitly rejected rather than reported as successful readiness. Malformed, failed, cancelled or incomplete readiness remains queued. Legacy alarmId-only handoffs remain compatible.

Failures retain the current question, answer and occurrence. Initialization retry reuses generated questions even after the first progress write fails. A final answer is cleared only after durable accepted resolution. Captured question index, identity, in-flight and result-ID guards reject duplicate/stale work. Native owners acknowledge retained result IDs while active, before closing factories or navigating. Accepted completion/snooze ends the cover and advances the queue; observational dismissal does neither. Post-acceptance cleanup debt does not downgrade an accepted result.

## Repeating-occurrence policy

The existing row/progress/recovery formats provide one unresolved occurrence per alarm. Persisting concurrent independent occurrences of the same repeating alarm would require a broader occurrence-table/progress-key/registration migration; it is outside this approved milestone. The resolved policy is **one unresolved challenge per repeating alarm**. Further repeating alerts coalesce into that saved occurrence, retaining date-specific native delivery IDs and the exact current problems/progress. Shared state must confirm association; native tokens cannot choose the owner. Earlier timestamps, disabled alarms and independent one-time alarms do not coalesce. Different alarms remain independently durable and ordered.

Association is serialized through `Usecases.command`. Before acknowledgement it durably retires only an exact known later delivered timestamp from `pendingTimes`; a write failure preserves the raw queued identity for retry. This prevents legacy replay from resurrecting an already coalesced alert after completion. Unknown future weekly alerts join the current occurrence without adding another progress/recovery identity. Uncoalesced known later repeating timestamps survive until explicitly consumed. Legacy identity-only consumption selects the earliest due member. The policy was explained before integration and requires no material architectural change.

## Fault ordering and crash windows

The existing `scheduleError` column carries a small write-ahead journal; no schema changes were introduced.

| Window | Authoritative behavior |
| --- | --- |
| Desired schedule write fails | Native registration is not attempted; unresolved state remains |
| Desired persisted, native registration rejected | Preserve prior active identity/count/snooze and error/reconciliation evidence |
| Native registration accepted, final storage fails | Do not cancel unresolved recovery/playback; explicitly retry an active replacement; reconcile eligible future schedules |
| Completion native cancellation fails before accepted write | Keep exact unresolved occurrence, answer and progress; retry the same command |
| Completion accepted write fails after native cancellation | Active identity/recovery evidence remains durable; restoration reopens it and an explicit completion retry accepts it |
| Snooze desired write/native acceptance/final write | Journal exact timestamp before native acceptance; retry/restart replays that timestamp/count rather than creating another delay |
| Pending snooze expires before reconciliation | Cancel stale snooze, retain original unresolved occurrence; do not manufacture a new delay |
| Accepted completion/snooze/edit cleanup fails | Persist accepted state plus cleanup marker before cancelling recovery/notifications; retain marker/status and retry cleanup at launch, including disabled rows |
| Cleanup cancellation succeeds, marker-clear write fails | Retain cleanup debt; idempotent reconciliation retries without reversing acceptance |
| Readiness/progress/recovery reservation writes fail | Keep questions/current answer/occurrence/raw queue or prior recovery token; permit authoritative retry |
| Ancillary read fails after accepted resolution | No extra coordinator read can downgrade accepted resolution or erase cleanup debt |

An accepted cleanup failure can leave a native recovery/notification registration until reconciliation succeeds. Acceptance, cleanup debt and shared failure remain durable; acceptance does not imply every failing native cleanup has already succeeded. Failed completion persistence restores the unresolved session and requires explicit command retry; reconciliation does not automatically replay completion.

Recovery reservations are persisted before native acceptance and repaired after a crash. A mismatched old token is replaced only after shared confirmation that its occurrence is obsolete. Eligibility is checked again after registration acceptance. Cancellation failure restores evidence; older callbacks cannot overwrite newer tokens. An accepted Android edit dismisses only the affected real playback after durable acceptance. Cross-alarm recovery/audio/state isolation is covered by focused tests.

## Baseline and final evidence

Verified the ordered milestone commits `41a97c7`, `ef7136c`, `4c2072a`, `f27063c` before editing. Preserved unrelated IDE/Firebender state, old Kotlin error-log deletions, Xcode user state and release mapping. No push.

A forced fresh M4 baseline reproduced **458 Android tests (136 core / 190 shared / 132 app)** and **341 iOS tests (135 core / 206 shared)** with zero failures/errors/skips. Native queue/recovery smoke passed. The ordinary sandbox initially prevented Gradle lock and adb socket access; authorized tool execution resolved those environment restrictions.

| Check | Final result / evidence |
| --- | --- |
| Core/shared/Android host suites and Debug/test APKs | **487 Android (156 core / 199 shared / 132 app), 370 iOS (155 core / 215 shared)**; zero failures/errors/skips; `/tmp/mathalarm-m5-final-kotlin-2.log` |
| Fault windows | 20 core fault cases per platform; 50 shared maths cases per platform, including preview/audio ownership, readiness/progress persistence, failed retry, duplicate/stale commands, accepted work after owner cleanup and coalesced legacy replay |
| Android navigation/draft protection | Three instrumentation checks passed on disposable API 30 tablet resized for expanded/compact windows; `/tmp/mathalarm-m5-android-expanded-2.log`, `/tmp/mathalarm-m5-android-compact.log` |
| Native queue/recovery | Foundation smoke passed, including injected writes, exact-head replacement, acknowledgement/restart, legacy restoration, recovery reservation/acceptance/cancellation faults, DST and continuous token chain; `/tmp/mathalarm-m5-native-smoke` |
| Verification scripts/catalogs | 19 Python tests; source boundary and all-nine-locale/resource checks passed |
| Renderer-free resolved graph/exports | Scanner passed final `app.framework` and `/tmp/mathalarm-m5-dependencies-final.log`; no Compose/CALF renderer exports/dependencies; approved bridges remain ObservableViewModel 1.1.0 / NativeCoroutines 1.0.6 |
| Normal unskipped iOS framework/app | PASS, `/tmp/mathalarm-m5-xcode-final-12.log`; Xcode 27.0, iOS 27 SDK; final normal Kotlin framework compilation, no skip/fallback |
| Production mounted iPhone/iPad groups and process restart | **20 groups passed on both iPhone and iPad**, including native input/focus, ordered replay, accepted failure/retry/result acknowledgement, retained drafts and fresh-process progress restoration; `build/native-ui-m5/iphone/bridge.log`, `build/native-ui-m5/ipad/bridge.log` |
| Native preview/delivered visual QA and normal launch | PASS within measured iPhone/iPad matrix: full pane height, toolbar/safe areas, focused native input, dark/accessibility-size preview and delivered progress; captures and ordinary launch logs in `build/native-ui-m5/{iphone,ipad}/`. Simulator hardware-keyboard configuration means software-keyboard layout/Done interaction is not established |

Integration failures were corrected rather than accepted: Kotlin trailing-lambda compatibility in usecase constructors, Kotlin public-property smart cast, generated Swift enum/nested/scalar names, and new final-answer retention expectations. Native verification exposed a prior callback assumption: cached SwiftUI destinations need not emit another `onAppear`. Checks now require selected retained editor/route and a settled attached visible native navigation controller/title, with timeout screenshots/hierarchy. In-process SwiftUI automation identifiers are unavailable without an accessibility client; permanent XCTest/client verification remains M7. Mounted checks also caught cached-stack disappearance while its native controller remained visible; explicit selection/window/generation boundaries now revoke navigation ownership, while retained routes rehydrate on reappearance. A failed-readiness replay now legitimately reopens presentation, so editor-alert verification explicitly dismisses that error presentation while retaining the queued occurrence. These are distinguished from production state failures.

The emulator's default userdata partition exceeded available disk space. Only disposable task-created devices/image files were removed; tests used the already installed API 30 image. The earlier API 35 instrumentation baseline remains historical. Final builds retain existing SDK/deprecation warnings; no new failing test/build remains. Simulator fixtures and controlled scheduling establish application/native integration, not physical delivery or acoustic output.

## Remaining gates and Milestone 6 handoff

Physical AlarmKit scheduled delivery, locked/background/terminated AppIntents, continuous recovery beyond system UI, recovery cancellation, audible output, Bluetooth/DND routing, repeating coalescing over multiple days, permission revocation, physical folds and real crash/power-loss storage guarantees remain open. Minimum supported iOS 26.0 runtime was unavailable; iOS 27 simulator results do not establish it. VoiceOver/TalkBack client behavior, native keyboards on physical iPhone/iPad and the full nine-locale visual/RTL matrix require follow-up. Mounted checks verify automatic first-responder focus, native text insertion into shared state, number/punctuation keyboard type and Go return key. Captures verify pane layout, focus, dark appearance and accessibility-size text. The software keyboard was not displayed under the simulator hardware-keyboard configuration; its layout and Done interaction remain unverified. Source/catalog accessibility labels and announcements are checked, while VoiceOver client behavior remains open.

Release signing, unsigned/current-source Release/archive/TestFlight, distribution/privacy/support URLs, launch visuals and overnight/multi-day reliability are not closed here. Preserve the gates in `ios-release-readiness-2026-09-24.md` and `ios-alarm-recovery-manual-test.md`.

Milestone 6 should implement app-wide theme/sort, What's New, feedback/share and remaining native presentation/launch parity; revalidate nine locales, RTL, Dynamic Type and accessibility across all screens. Preserve these factory/session/result/readiness/queue/audio boundaries, repeating-occurrence policy and fault journal. Run the expanded production harness after every ownership/navigation change. Milestone 7 adds permanent XCTest/CI integration; Milestone 8 closes physical recovery/release gates. Do not introduce a Compose wrapper or claim full parity/release readiness.
