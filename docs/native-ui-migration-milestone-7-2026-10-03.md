# Milestone 7 — permanent native tests, CI and packaging

## Scope and baseline

The approved migration plan remains authoritative. Work starts from the existing
uncommitted post-M6 visual/navigation/permission/Settings-return implementation.
`build/native-ui-m7/baseline/` records the initial status and tracked patch. Unrelated
IDE state, Kotlin error-log deletions and Android release mapping are preserved.
No dependency/toolchain version, persisted schema/key, registration identity or
physical device state is changed. Android remains Compose-owned; iOS is SwiftUI.

**Gate status: implemented and locally verified; not marked complete.** The updated
workflow has not run in hosted CI against this working-tree change. VoiceOver speech/
focus, live window resizing and configured-Mail composer cancellation also remain
unestablished for the precise environmental reasons below. Source inspection and
simulator captures are not substituted for those results.

## Implemented system

- Permanent `MathAlarmTests` and `MathAlarmUITests` targets use the distinct shared
  `MathAlarmNative` scheme, avoiding the pre-existing private `iosApp` scheme collision.
  Hosted XCTest imports the production app/framework and executes the 25 mounted
  first-process groups. Assertions report original file/line through XCTest before
  stopping on failed prerequisites. The UI target executes all 27 contract groups
  across actual process termination/relaunch and exercises ordinary Back/Cancel.
  The Debug harness remains and consumes exactly the same behavioral assertions.
- The permanent contract includes once-only authorization, exact originating draft
  Settings return/save, denied/failed/discarded/replaced cases, delivery deferral,
  nine silent tutorial loops, owner-scoped audio lease and delivery interruption.
  Shared factories, mounted owners, observer cancellation, readiness before exact
  queue acknowledgement, durable commands, retained results and fresh-process
  progress/preferences/announcement restoration remain production-backed.
- A native lifetime regression surfaced under hosted XCTest: the pinned bridge's weak
  cancellation wrapper could expire before an outgoing SwiftUI observer was constructed.
  Each retained session now also retains its observation wrapper. Structural owners
  and explicit SharedFeatures closure remain authoritative; late observation never
  revives a closed command/session. A dedicated XCTest reproduces this ordering.
- Existing CI host and Android delivery suites are retained. Both iPhone and iPad jobs
  run native KMP/XCTest/UI tests, harness parity, actual nine-locale capture phases,
  ordinary Maestro 2.10.0 flows and all Debug/Release SDK links plus archive packaging.
  Renderer imports, resolved graphs, framework API/export consumption, pinned Swift
  revisions, compiled catalogs/resources and Release exclusion of Debug fixtures are
  enforced. Artifacts upload even on failure; screenshot review remains explicit.
- Android UI CI runs two expanded tests (including recreation) and one compact test.
  Notification authorization is a stated fixture precondition. Tests advance the
  Compose clock through the existing delayed review request and preserve the exact
  once-only callback assertions.
- Cleanup is isolated in [its review record](reviews/native-ui-m7-cleanup.md): one dead
  platform URI shim, one unused catalog alias and obsolete shared build flags/resource
  processing. Room generation/refinement, schemas, platform tests, current Android
  dependencies, durable operations and compatibility readers are retained.

## Verification evidence

Evidence is local and ignored under `build/native-ui-m7/`; logs retain failed attempts
rather than rewriting their outcomes. Hosted CI has not been dispatched from this chat.

| Check | Result / evidence |
| --- | --- |
| Shared/Android host suites | 487 tests: core 156, shared 199, Android app 132; zero failures/errors/skips. `kotlin-android.log`, `kotlin-test-results.json`. |
| Native Kotlin/platform suites | 370 tests: core 155, shared 215; zero failures/errors/skips. Same logs. |
| Android app/test APKs | Build passes; `kotlin-android.log`, `android-test-build.log`. |
| Android expanded/recreation/compact UI | All three pass on task-owned API 35 emulator. `android-ui-clock.log`, `android-ui-clock/{expanded,compact}.txt`; emulator removed afterward. |
| Python contracts | 24 pass, including all 27 required native group names and reviewed Kotlin/Swift lock parity. |
| Native durable queue/identity/recovery | All three Foundation smoke groups pass; `queue-smoke.log`. |
| Renderer boundary | Source, resolved simulator graph and production generated framework headers pass; `ios-dependencies.log`. |
| Permanent iPhone XCTest/UI | Final four hosted plus two UI tests pass, including the 25-group hosted journey, 27-group fresh-process UI journey and exact-title Back/Cancel. `iphone-final.log`, `iphone-final.xcresult`; earlier passing run retained in `iphone-lifetime`. |
| Permanent iPad XCTest/UI | Final four hosted plus two UI tests pass, including exact-title Back/Cancel: `ipad-final.log`, `ipad-final.xcresult`. Initial results/failures remain in `ipad-native` and `ipad-ui-final`. Crash symbolication identified a five-second split-layout settlement timeout at the unchanged route/geometry assertion. The fixture now allows a bounded 15 seconds and captures the failing window; it neither retries nor relaxes the state/owner checks. |
| Retained Debug harness parity | All 27 groups pass on both families: `iphone-bridge.log`, `ipad-bridge.log`. |
| Debug/Release linking | All four device/simulator configurations pass, normally rebuilding Kotlin. `build-lifetime-2.log`, `link-{Debug,Release}-{iphoneos,iphonesimulator}.log`. |
| Release archive packaging | Unsigned archive succeeds; `archive.log`, `archive-package.log`, `MathAlarm.xcarchive`. All nine compiled locale catalogs, six exact CAF binaries, privacy manifest/reasons, nine exact localized videos and optimized posters/launch assets pass. Release Debug-fixture exclusion passes. |
| Resolved graph / framework | Both platform graphs and Debug/Release headers pass; `ios-dependencies.log`, `ios-device-dependencies.log`, `device-boundary.log`. |
| Nine-locale matrix | Both complete: 631 iPhone and 495 iPad PNGs, corresponding trait JSON, all 49 required families per locale plus additional viewports. `iphone-matrix.log`, `ipad-matrix.log`, `{iphone,ipad}-presentation/review.html`. Typed same-process fixture cleanup passes for every phase. |
| Native client flows | Both families pass Settings/announcement persistence, share cancellation, portrait/landscape keyboard/error/draft return, permission guard, Back/Cancel, disabled save/delete/undo and six-tone maximum-text selection/application/discard. `iphone-client/` and `ipad-client/`; final iPhone tone run is `sound-maximum-settled.log`. Both record actual maximum OS text size. The optional OS authorization-alert branch is skipped on both; this is not first-install grant evidence. |

Corrected failures are not counted as passes: a private scheme initially hid the test
bundles; the dedicated scheme fixes reproducibility. The original save fixture could
observe Room's initial desired row before its scheduled update; it now waits for the
actual enabled row with scheduled occurrences, retaining the same final assertion.
The hosted observation crash led to the retained-wrapper fix above. A Swift test-only
overload ambiguity was fixed. Android's first-install notification modal and controlled
Compose-clock delay caused fixture failures before their explicit preconditions were
added. Xcode recompresses PNG posters, so packaging verifies their format/dimensions
instead of falsely requiring byte identity; videos/tones/privacy remain byte-checked.
macOS Bash 3 empty arrays under nounset were corrected in the packaging runner.
The first maximum-text iPhone client run had entered Repeat immediately after the
keyboard Done action, so the subsequent sound-row lookup correctly failed. The flow
now starts with a fresh launch, waits for keyboard settlement, sends one Done tap and
asserts the editor/exact title before scrolling. Its rerun passes all six tone controls,
Clear Signal application to the exact draft and discard (`sound-maximum-settled.log`).
No visibility threshold or selection assertion was reduced. Portrait/landscape client
captures now have distinct filenames; the first iPhone portrait set was preserved
under `iphone-client/portrait-review/` before landscape ran.
The archive contains the app privacy manifest. The resolved RxSwift package is not a
linked product: this app consumes NativeCoroutinesAsync, not its optional RxSwift
adapter, so RxSwift's separate privacy bundles are not archive inputs.

### Screenshot review artifacts

Both `review.html` indexes link all full-resolution captures, including optional scrolled
viewports. `review-status.json` independently records capture completeness and visual
review status. Six families were sampled across all nine locales on both form factors:
light editor and dark accessibility-size sound, preview, settings, next-alarm list and
delivered error feedback. Contact sheets are under `{iphone,ipad}-review/`. These show
the retained violet identity and glyphs/wrapping; long compact navigation titles and
next-alarm subtitles ellipsize at accessibility size. Focused compact preview captures
can scroll the heading/upper task beneath the navigation bar. This is recorded for
interactive large-text review, not silently approved as a full visual/accessibility pass.
The separate ordinary-keyboard client captures establish actual software-keyboard
behavior. All-matrix linguistic/visual sign-off remains pending in the review artifact;
neither contact sheets nor file counts establish VoiceOver output.

`capture-binaries.json` identifies the installed executable and Debug dylib for each
matrix. The iPhone run precedes the Debug-only 15-second fixture settlement change;
the production implementation is identical. Final permanent tests use the latest build.

## Client availability and onward gates

The permanent capability attachments on iOS 27.0 report configured native Mail
unavailable and VoiceOver not running on both families. PiP capability is false on
iPhone and true on iPad; actual PiP playback over Settings was not established by this
probe. These are availability results, not spoken traversal, composer cancellation or physical-device evidence. Native Device
Hub automation was attempted through the installed Xcode app and timed out (-10005),
so live window resizing/VoiceOver client traversal could not be established there.
No external message or feedback was sent; no account was configured. Source assertions,
accessibility trees and simulator captures are not substituted for these missing checks.

M7 unsigned Release archive packaging is distinct from M8 signed distribution/store
acceptance. Physical AlarmKit delivery/recovery/locked intents, PiP over Settings and
actual toggle grant/return/save, acoustic output and routing, minimum iOS 26.0 runtime,
physical resizing/folds and multi-day reliability remain onward gates. Existing
[release readiness](ios-release-readiness-2026-09-24.md) and
[recovery matrix](ios-alarm-recovery-manual-test.md) remain in force.

Reproduction: [architecture/build](native-architecture.md), [testing](testing.md),
`bash scripts/ci_native_ios.sh` and `scripts/maestro/ios/README.md`.

## Final handoff

The implementation, production-framework consumption, boundary/resource tests,
simulator/client suites and unsigned archive checks are locally passing. The current
working tree includes the original user work and the M7 additions; nothing was committed,
pushed or submitted. Run the updated workflow on this exact change before closing its
hosted-CI gate. Retain the Debug harness until that migrated coverage is accepted.
Collect the unavailable native-client evidence on a supported isolated client; no Mail
message needs to be sent. The screenshot review artifacts retain their explicit full
visual/linguistic sign-off status. M8 physical/minimum-runtime/distribution acceptance
is still separate from the completed M7 archive packaging check.

The task-owned Android emulator and two task-owned iOS simulators are removed after
evidence capture. Existing user simulators and connected physical devices are preserved.
