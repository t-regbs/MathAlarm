# Milestone 7 — permanent native tests, CI and packaging

## Scope and baseline

The approved migration plan remains authoritative. Work starts from the existing
uncommitted post-M6 visual/navigation/permission/Settings-return implementation.
`build/native-ui-m7/baseline/` records the initial status and tracked patch. Unrelated
IDE state, Kotlin error-log deletions and Android release mapping are preserved.
No dependency/toolchain version, persisted schema/key, registration identity or
physical device state is changed. Android remains Compose-owned; iOS is SwiftUI.

**Gate status: COMPLETE — approved implementation/CI/archive gate.** Corrected hosted
Android and iPhone/iPad runs are green. CI verifies renderer boundaries, both native
UIs, shared behavior, all four Debug/Release SDK links and unsigned Release archive
packaging. Documentation describes the implemented architecture. Earlier failed runs
remain recorded below, with their fixes and successful reruns.

VoiceOver speech/focus, live window resizing and configured-Mail composer cancellation
remain unestablished for the precise environmental reasons below. Full visual/linguistic
sign-off is pending. These limits and M8 physical/distribution acceptance remain open;
source inspection and simulator captures are not substituted for those results.

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
rather than rewriting their outcomes. Hosted run details are in the final handoff below.

| Check | Result / evidence |
| --- | --- |
| Shared/Android host suites | 487 tests: core 156, shared 199, Android app 132; zero failures/errors/skips. `kotlin-android.log`, `kotlin-test-results.json`. |
| Native Kotlin/platform suites | 370 tests: core 155, shared 215; zero failures/errors/skips. Same logs. |
| Android app/test APKs | Build passes; `kotlin-android.log`, `android-test-build.log`. |
| Android expanded/recreation/compact UI | All three pass on task-owned API 35 emulator. `android-ui-clock.log`, `android-ui-clock/{expanded,compact}.txt`; emulator removed afterward. |
| Python contracts | 27 pass after the Doze runner correction (24 before it), including all 27 required native group names, reviewed Kotlin/Swift lock parity and exact fixture-state restoration. `python-ci-fix.log`. |
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

The continuation expands the sampled review to 25 iPhone and 17 iPad families across
the nine locales; exact names are in each `review-status.json`. Added samples cover
challenge configuration, repeat/snooze controls, scrolled announcement actions,
the 09:45 dial, one-enabled-alarm subtitle, synthetic RTL and unavailable-tone fallback.
The additional large-text editor, delivered progress and snooze announcement samples
include scrolled viewports where emitted; those optional views are absent when no
additional scrolling was needed. The iPad custom-challenge sample places the large
heading at the top edge; its source PNG/trait JSON is retained for interactive
accessibility review rather than being counted as complete visual approval.
All-family contact sheets are available under `{iphone,ipad}-review-all/`; these are
navigation aids, not a claim that every family has been reviewed. An initial contact
sheet crop hid a snooze switch; the full-resolution source showed it correctly, and
the contact-sheet generator was corrected to preserve the entire image. No production
UI change was needed from these additional samples.

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
working tree includes the original user work and the M7 additions. After explicit
approval, an isolated 80-file snapshot was published as
`f2b2226d3f450096bf421a30cacbed27af323609` on
`codex/native-ui-m7-ci-20261003`; the user's working branch and index remain unchanged.
The snapshot excludes changes to IDE history/state, logs and release mappings. Hosted
[run 37104158893](https://github.com/t-regbs/MathAlarm/actions/runs/37104158893)
exposed the Android Doze setup and two native fixture failures documented below; it
is retained as failing evidence. Both corrected native jobs now pass. The workflow
uses GitHub's hosted `xcode-27` Apple Silicon image with an explicit Xcode 27.0
(`27A266a`) path and version assertion. The Debug harness remains as an independent
parity check alongside the permanent tests.

The first hosted continuation exposed an existing API 32 fixture issue in prior
run `36992095751`: Doze configuration remained at its default thresholds after
writing `device_idle_constants`. Android 12–14's
[DeviceIdleController](https://android.googlesource.com/platform/frameworks/base/+/android-12.1.0_r1/apex/jobscheduler/service/java/com/android/server/DeviceIdleController.java)
reads DeviceConfig directly; later versions restore the Settings override. The runner
now selects that version-specific path, records per-key restoration before mutation,
and requires both effective service thresholds before forcing/verifying deep idle.
Three focused tests cover partial setup failure, exact restoration and rejection of
stale effective configuration. All 27 Python checks pass (`python-ci-fix.log`);
The first M7 run reproduces that exact setup timeout on API 32; cold-start, snooze
and reboot pass (`hosted-first-api32.log`). The correction is published in
`9e17a60a2f0b21f322c55195511c82c8bb2e0539`;
[Android-only run 37104690123](https://github.com/t-regbs/MathAlarm/actions/runs/37104690123)
verifies it at `6b3e45cd4685d221c06e0d6a24985ea81b22a0ec`, adding only the scoped
dispatch and its instructions. The original native run continued with identical iOS production
source and native runner between those revisions. The corrected run is **green**:
all 487 host tests, 27 Python checks, four real-OS delivery scenarios and the
expanded/recreation/compact native UI tests pass on APIs 30, 32, 35 and 36.
Artifacts are retained under `hosted-android/`, with exact run metadata in
`github-android-final.json`. Delivery, player-start,
reboot and occurrence assertions are unchanged.
The first hosted iPhone run passed native Kotlin, all six permanent native tests and
all 27 independent harness groups, then failed the next-alarm equality assertion in
the Chinese dark capture. The enable fixture waited for `isOn` and command completion,
but the Room observer can still expose the intermediate scheduling journal row then;
the production subtitle correctly excludes rows with `scheduleError`. The fixture now
waits for the accepted persisted row (`scheduleInitialized`, no error, nonempty pending
times) and exact equality of all persisted/registered occurrence times. The original
next-alarm equality, subtitle changes and cleanup assertions remain intact; disabling
also waits for empty persisted occurrences and no registered occurrence for that ID.
This is DEBUG verification synchronization, with no change to production scheduling or
subtitle behavior. Original failing evidence remains in `hosted-first/iphone/`.
The corrected Chinese base/delivered journey passes locally (68 captures, exact subtitle
checks and same-process typed cleanup), in `iphone-subtitle-zh/` and
`iphone-subtitle-zh.log`. The normal framework/test build also passes in
`build-subtitle-settlement.log`; all 27 Python contracts and boundary/localization/pin
checks pass. [Native run 37107199909](https://github.com/t-regbs/MathAlarm/actions/runs/37107199909)
started both form factors at `cdb54fb4ce232e63bf612307d13ac1746b7b2eed`, then
was superseded by the final-row client correction below.
Its explicit `scope=ios` skips the already-green, unchanged Android sources/tests;
default workflow runs still validate both platforms.

The original hosted iPad job passed the permanent suites, all 27 groups, all nine locale
captures and the earlier client flows. Its maximum-text final tone was fully visible:
Maestro logged `Visibility Percent: 1.0`, and the failure screenshot/hierarchy show
Clear Signal and its preview control inside the viewport. `centerElement: true` kept
trying to scroll the last row beyond the bottom limit and timed out. Only that final
row now uses `centerElement: false`; 100% visibility, both control assertions, actual
selection, exact-draft application and discard assertions remain unchanged. Evidence
is retained in `hosted-first/ipad/.../client/sound-maximum/`, including the failure PNG,
hierarchy and scroll log. This changes the client fixture, not app layout or text size.
[Replacement native run 37107437705](https://github.com/t-regbs/MathAlarm/actions/runs/37107437705)
validates both corrections at `e5c1720c58ca8f9e8fa39731891f367423df9cc1`.
The complete corrected tone flow passes locally on a fresh disposable iPad using
Maestro 2.10.0 and actual `accessibility-extra-extra-extra-large` OS text size:
`ipad-final-tone.log`, `ipad-final-tone/` and `ipad-final-tone-content-size.txt`.
All six controls, Clear Signal application to the exact draft, and discard pass.
That task-owned simulator is removed after capture.


### Final hosted gate evidence

[Native run 37107437705](https://github.com/t-regbs/MathAlarm/actions/runs/37107437705)
completed successfully on both families at `e5c1720c58ca8f9e8fa39731891f367423df9cc1`.
Each job passes 370 native Kotlin tests, six permanent XCTest/UI tests without skips,
all 27 retained harness groups, native client flows, four normal Kotlin-backed
Debug/Release device/simulator links, and unsigned Release archive packaging. The
nine-locale matrix contains 631 iPhone and 495 iPad captures with trait metadata and
all 49 required families per locale. Maximum OS text size is recorded and restored.
Boundary/framework consumption, pinned Swift resolution, nine compiled catalogs,
six stable tones, privacy manifests and nine localized guide videos/posters pass.
Controlled iOS scheduling remains separate from physical AlarmKit evidence.

The green [Android run 37104690123](https://github.com/t-regbs/MathAlarm/actions/runs/37104690123)
at `6b3e45cd4685d221c06e0d6a24985ea81b22a0ec` and this native run jointly establish the
CI gate. Android/shared production and test sources are identical between those
revisions; subsequent changes affect only native Debug/client fixtures, scoped dispatch
and documentation. The default workflow still runs both platforms.

Downloaded final native artifacts are under `hosted-final/{iphone,ipad}/`; each contains
`build/native-ui-m7-ci/` logs, xcresult, review HTML/captures, client evidence and the
actual archive. `hosted-final/verified-summary.json` records independent artifact
checks, and `{iphone,ipad}-native-summary.json` records xcresult summaries. Android
artifacts and their verified summary are under `hosted-android/`. Hosted metadata is
in `github-{android,ios}-final.json`. All paths are relative to `build/native-ui-m7/`.
The final branch update after this tested revision changes only the evidence documents.

Passing xcresults retain runtime warnings for synchronous audio-session activation,
mounted verification hosting views, transient invalid frame dimensions and publication
during view updates. No test failed or was skipped; these warnings are preserved for
follow-up diagnosis and are not represented as a warning-free runtime or physical
responsiveness result.

Collect the unavailable native-client evidence on a supported isolated client; no Mail
message needs to be sent. The screenshot review artifacts retain their explicit full
visual/linguistic sign-off status. M8 physical/minimum-runtime/distribution acceptance
is still separate from the completed M7 archive packaging check.

The task-owned Android emulator and all task-owned iOS simulators are removed after
evidence capture. Existing user simulators and connected physical devices are preserved.
