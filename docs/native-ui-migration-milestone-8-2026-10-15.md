# Milestone 8 fresh candidate validation — 15 October 2026

**Milestone 8 remains OPEN.** Build **2.3.1(25)** fixes a reproduced physical
audio-restoration failure and passes the bounded physical retest. This record continues the
[3 October evidence](native-ui-migration-milestone-8-2026-10-03.md); it does not
restart Milestones 1–7 or transfer earlier physical observations to a new binary.
Submission/publication is a separate action.

## Candidate and preservation

- Branch `codex/ios-first-release`, base commit
  `c158fd353c04fdb28d4d32abd10d5795fd2ebaed`, with the retained working-tree changes.
  The current candidate includes the newer native editor/list/sound-picker work.
  Exact build-input hashes (527 files):
  `build/native-ui-m8/2026-10-15/source-build24.json`.
- Current version **2.3.1(25)**, increased from 23 through 24 in both configurations.
  Build 25 input hashes (529 files) are in `source-build25.json`; its exported
  executable SHA-256 is
  `53f46540ef1e6458b65ccea631f047eb3d88bacf51a888b81c9c2e71f36ca50f`.
  No commit was made in this run. Build 24 remains separately identified below. Xcode
  **27.0 (27A266a)**, development team `AAM7WVDGCW`. The exported candidate's
  executable SHA-256 is
  `6202cb7bb1aa88a3b4bb612b6061d1ac7a897278839d7ccb9a50b26afddbd9d4`.
- Connected physical **iPhone 11**, **iOS 27.0.1 (24A446)**, wired,
  Developer Mode enabled. Host timezone Europe/London (BST); evidence uses UTC.
  The development profile includes this device and expires **22 October 2026,
  19:33:03 UTC**. This is development signing, not distribution acceptance.
- Fresh read-only baseline contains **five** original alarms, IDs **5, 6, 7,
  8, 11**, all disabled; no unresolved occurrence, queue or recovery failure.
  Today's baseline supersedes the older four-alarm count for cleanup. Never
  classify ID11 as disposable from an earlier report.
- Normal in-place installation **23 → 24 PASSED**: all five full alarm rows,
  schema 9, Room identity `8c10025227af8ddfb481753e68f28426`, and all preferences
  remain identical before first launch. No uninstall or physical hosted test
  harness was used. The normal production launch reports AlarmKit authorized
  and zero native registrations, consistent with the disabled alarms.
  This first upgrade did not contain an active occurrence.
- In-place **24 → 25 PASSED with unfinished question 2**: every alarm row, all five
  originals, exact full progress, decoded recovery session, schema/identity and all
  stable preferences match before first launch. Ordinary launch restores the same
  question and app sound by user observation (`physical/upgrade25-preservation.json`).

Private evidence root: **`build/native-ui-m8/2026-10-15/`** (ignored).
`physical/before-install/` and `physical/after-install-before-launch/` contain
read-only data copies; `physical/upgrade-prelaunch-prefs-diff.json` is empty.
`physical/install24.json`, `physical/apps-after-install.json`,
`physical/console24.log`, `export24-identity.json` establish deployment identity.
Do not publish databases, preferences, signing artifacts, local server tokens or
Mail screenshots containing private account fields.

## Fresh verification

| Check / expected outcome | Observed / status | Evidence under the private root |
| --- | --- | --- |
| Compile current native test targets on iOS 26.3.1 | **PASSED**, builds 24 and 25 | `sim-build.log`, `sim-build25.log` |
| Renderer boundaries and native localization | **PASSED** | `boundaries.log`, `localization.log` |
| Python release/contract regressions | Initial **FAILED**: three Python-required group names still said “staged sound”, while current Swift checks say “draft tone”. Updated only these labels after inspecting retained owner/draft assertions. **PASSED: 30 tests**, all 27 groups still required | `python-tests.log`, `python-fixed.log`, `verification-source-changes.json` |
| Fresh iPhone hosted integration | **PASSED: 19 tests**; includes cancellation-safe native recovery replacement, durable queue compatibility, owner cleanup, audio-session lease timings and current editor/list behavior | `iphone-tests.log`, `iphone-tests.xcresult` |
| Build 24 iPhone 17 / iOS 26.3.1 suite | **PASSED**: 28 total native tests (19 hosted + 9 UI), one iPad-only UI skip, zero failures | `iphone-tests.log`, `iphone-tests.xcresult`, `iphone-summary.json` |
| Build 24 iPad Pro 11 M5 / iPadOS 26.3.1 | **PASSED: 24 tests**, no failures/skips: 19 hosted + 5 UI, including all 27 groups, unsaved alarm switching, preview, sound-picker Back and explicit Cancel | `ipad-tests.log`, `ipad-tests.xcresult`, `ipad-summary.json` |
| Signed Release archive and development export | **PASSED** | `archive24.log`, `export24.log`, `MathAlarm24.xcarchive/`, `export24/` |
| Archive/export signature, resources, required-reason privacy imports | **PASSED**: strict signature verification, nine locale catalogs, six tones, nine guide videos/posters; no Debug fixtures or undeclared required-reason imports. MinimumOSVersion26.0 and both device families packaged | `codesign-archive24.log`, `codesign-export24.log`, `archive24-audit.json`, `export24-audit.json` |
| Actual exact iOS 26.0 compatibility | **UNAVAILABLE** in installed runtime inventory; a deployment target is not execution evidence | `environment/simulators.json` |
| Argent physical automation | **PASSED**: signed runner, screenshot/tree, current-control taps and text input on the iPhone. First UI-automation handshake failed; reset/retry succeeded. Retained setup failures are separate from MathAlarm behavior | `physical/argent-runner-first-failure.log`, `argent/phone-description24-second.json`, `argent/phone-title-value.json` |
| Current Duo open/half-open/closed | **PASSED**, build 24/iOS 27.1: dirty title and changed two-question nested route survive transitions; Cancel/discard leaves zero persisted alarms. This is sampled adaptive evidence, not all preview/accessibility/locale gates | `duo-validation24.json`, `argent/duo-*.json`, `argent/duo-open-view.png`, `argent/duo-closed-challenge-shot.png` |
| Physical background delivery / active-solving deferral | **PASSED**, user-confirmed 20:59 BST audible delivery and Stop → maths, literal “+” intact; wrong-answer sound continuous and no system interruptions while maths visible | `physical/observer24-initial.json`, `argent/phone-delivery24.png`, `argent/phone-wrong-question2.png` |
| Terminated native fallback / exact durable progress | **PASSED**, user-confirmed audible ring in the ≤90s observation window after SIGKILL; exact questions/index/start/incorrect count restored | `physical/termination24-record.json`, `physical/final-prekill/`, `physical/fallback-outcome/`, `physical/observer24-fallback-failure.json` |
| App sound after native fallback Stop/unlock | **FAILED on build 24**: user reports restored 9 × 7 with no app sound. Retained failure, followed by build 25 repair/retest below | `physical/observer24-fallback-failure.json`, `argent/phone-restored-no-audio.json` |
| Final build 25 shared iOS regressions | **226 PASS**, zero failures/errors/skips, including six new playback-recovery tests | `audio-repair-tests-final.log`, `shared-ios25-junit/`, `shared-ios25-summary.json` |
| Final build 25 native hosted tests | **19 PASS**, zero failures/skips, all 27 production groups retained. Full UI suite exercised on build 24, not rerun after iOS-only audio repair | `iphone-hosted25.log`, `iphone-hosted25.xcresult`, `iphone-hosted25-summary.json` |
| Build 25 signed archive/development export/signature/resources/privacy | **PASSED**, zero audit errors or undeclared required-reason imports; minimum 26.0, families 1 + 2 | `MathAlarm25.xcarchive/`, `export25/`, `archive25-audit.json`, `export25-audit.json`, corresponding logs |
| Build 25 terminated fallback / progress / app sound | **PASSED bounded retest**: audible native recovery; Stop/unlock restores exact question 2 and audible app sound. Original rows unchanged, no recovery failure | `physical/termination25-record.json`, `physical/prekill25/`, `physical/restored25/`, `physical/fallback25-comparison.json`, `physical/observer25-recovery.json` |
| Build 25 completion / fixture cleanup | **PASSED state and acoustic observation**: user confirms “yes silent” to no ring/sound for at least 100 seconds after completion. Progress/recovery/queue/failure/native metadata cleared; only ID12 deleted; exact five originals retained | `physical/completed25/`, `physical/quiet25/`, `physical/final25/`, `physical/cleanup25-summary.json`, `physical/observer25-completion.json` |
| Build 25 physical configured-Mail cancellation | **PASSED**: composer opens; Cancel → Delete Draft → Settings. No message sent or draft retained. Mail tree exposes no controls, screenshots establish them; semantic wait verifies return | `argent/phone-mail25-private-shot.png`, `argent/phone-mail25-cancel-shot.png`, `argent/phone-mail25-returned.json` |
| Live configured privacy/support links | **PASSED HTTP 200**; policy content/store accuracy still pending | `live-links.json` |
| Distribution / TestFlight / store acceptance | **PENDING**; only Apple Development identity available today. Development export is not App Store distribution or an upload | `environment/signing-identities.txt` (private); earlier distribution failure retained in 3 October record |

The checker-label repair happened after the build 24 application build and affects no app
source. `verification-source-changes.json` records that sole build-manifest delta.
Simulator test hooks reset disposable data and must never run on the user's phone.

## Confirmed audio failure and build 25 repair

Disposable **ID12**, `M8 24 termination +`, three easy questions, Orbit, once on
Thursday 20:59 BST. The user confirms background ring at the expected time,
audibility and Stop opening maths. At question 2, `9 × 7`, a wrong `999` gives the
expected feedback while sound continues. No system interruption is reported
through several minutes of active solving.

At **21:06:36.945 BST**, SIGKILL terminates only the verified build 24 MathAlarm
PID 982; absence is checked at 21:06:37.660. The phone is then locked without
reopening the app. The user reports **“rang on time, 9 x 7 returns but no app
sound”** after native Stop/unlock. A new process PID 1085 is verified. Full durable
progress matches exactly, including activeAt `1792094340000`, questionIndex 1,
all three problems, start time `1792094393910` and incorrectAnswers 1. Native queue
is empty and all five originals remain identical. Exact audible ring seconds
were not supplied; do not invent latency measurements.

The old platform manager ignored `setActive`/`AVAudioPlayer.play` false returns and
claims an active loop without any foreground/interruption restart. A cold
background Stop/foreground audio transition is a plausible trigger; its exact
OS error/timing is **not captured**. A short Time Profiler attachment again fails
to find the verified PID; no trace/stall measurement is established.

**Build 25 repair is implemented and the reproduced physical scenario PASSED.** Only iOS
playback infrastructure changes: retain a rejected request, check activation/play
acceptance, retry briefly and on foreground/interruption-end/route/media-reset
events, reuse a healthy player, and cancel delayed work on stop/replacement.
Shared occurrence/business behavior, Android/Compose, schema and stored formats
remain unchanged. Six focused iOS regressions cover rejected activation/player
start, healthy-loop continuity, interruption end, completion/snooze cancellation
and replacement during activation. **226 shared iOS tests pass** on the final
source (`audio-repair-tests-final.log`); initial missing NSError pointer-extension
import compilation failure is retained in `audio-repair-tests.log`.
Build 25 source hashes: `source-build25.json`; native build-for-testing, 19 hosted
tests, signed archive, development export and strict packaging audits pass.

After the active in-place upgrade the user confirms the same `9 × 7` and sound.
At **21:31:18.078 BST**, only verified build 25 PID 1195 is killed; absence is
checked at 21:31:18.713. The phone stays locked without reopening MathAlarm before
native recovery. The user hears native recovery, then reports **“jusr rang again
and 9 x 7 is back, preserved at q2 with audio after unlocking.”** Read-only copies
confirm exact full progress, empty queue, no recovery failure and all originals
unchanged. Exact audible ring seconds were not supplied. This passes the reproduced
missing-sound scenario, not the full route/interruption matrix.

The first native ring was initially described as disappearing/stopping; the user
then clarified **“i think i swipped it away and then it came back sfter.”** A swipe
while unresolved followed by recovery is consistent with the current policy.
No automatic-stop defect is confirmed; exact actions/timing were not captured.
The proposed extra isolation run was withdrawn. All statements remain in
`physical/observer25-recovery.json`.

Correct answers `63` and `188` complete the occurrence at approximately
**21:44:32 BST**. An immediate preferences copy still contains prior progress and
recovery while the database is resolved; the later **21:46:51 BST** copy shows
progress/recovery/native metadata empty. Record the preference-flush observation
without claiming an enduring cleanup failure. Only ID12 is then deleted. The final
**21:49:05 BST** copy shows exactly the five baseline rows and no fixture state.
The user subsequently answers **“yes silent”** to the explicit no-ring/sound
observation for at least 100 seconds after completion. **Completion acoustic
cancellation PASSED**, separately from the state copies. This does not establish
armed snooze/edit/disable/delete cancellation or overnight silence.

Configured Mail is exercised on the same physical build: the visible composer
opens, Cancel offers Delete Draft, and discard returns to Settings by semantic
wait. No send or saved draft. The Mail tree exposes no controls, so fresh screenshots
establish Cancel/discard coordinates; private account fields stay in ignored evidence.
Settings Done returns to the alarm list. Final data preservation/cleanup still passes.

Argent typed time-picker digits without changing the native wheel value; actual
wheel swipes and read-back establish 20:59. An automated Submit during a keyboard
layout transition missed its target; semantic wait failed and saved progress
correctly stayed at question 0. Waiting for a stable current tree and retrying
advances to question 1. Subsequent taps use a 600 ms stable-tree guard. These are
retained automation/observation failures, not evidence of an answer-processing bug.

Runtime observations remain open: build 24 native results record 10 iPhone/6 iPad
negative-frame warnings and 2 publishing warnings per client, plus one notice-expiry
multiple-updates-per-frame log per client. Eight production audio-session lease
samples per simulator are accepted/released: activation medians 0.190/0.180 ms,
maxima 0.367/0.341 ms (iPhone/iPad). Build 25 hosted results still contain both
warning categories. `runtime-observations.json` records exact values.
These do not measure physical audibility, sound startup, main-thread stalls or
prove the SwiftUI warnings harmless.

## Remaining acceptance gates

The unfinished terminated-fallback/progress/audio scenario now passes on build 25.
Completion cancellation now passes both state checks and user-confirmed
**100-second acoustic quiet**. Snooze requires its own quiet interval and only the
authoritative future delivery. Keep active-solving deferral, lock/unlock warning
regressions and all five original rows intact.

Still required: remaining foreground/locked/terminated **initial** delivery states
(current background initial passes), current-policy abandoned authentication and
continuous recovery beyond ten minutes; current snooze and armed edit/disable/delete
cancellation; overlapping deliveries and recurring single/multiple weekdays
without editing; permission denial/revocation/regrant and tutorial/PiP → Settings
→ exact originating draft; audio interruptions/routes, Silent/Focus and physical
latency/stall measurements; actual VoiceOver speech/focus, physical iPad/window
behavior, full current Dynamic Type/keyboard/locale/appearance and Duo preview
matrix (sampled transitions and physical Mail cancellation now pass); legacy
queued/native-registration upgrade delivery; exact 26.0 execution; timezone/DST,
reboot, overnight and multi-day observations; current Android regression evidence
as needed, distribution signing/export and final support/privacy/store metadata.
No fresh Android rerun is claimed today: this repair changes iOS only; the 3 October
Android delivery/adaptive evidence remains dated. Broaden checks if common/Android
changes or failures warrant it. Distribution identity/profile, provider access,
App Store build uniqueness, policy accuracy/privacy questionnaire, final assets and
TestFlight processing/install/upgrade remain unverified. Historical passes remain
scoped to their recorded builds/devices. A simulator,
native acceptance log or cleared state cannot prove sound or absence of a later ring.

Three task-created simulators have been removed after testing
(`simulator-cleanup.json`); other simulators are preserved. Physical Argent runner
and local server are stopped; MathAlarm and ArgentRunner remain installed. Preserve
the other working tree, user simulators and five baseline alarms. Append actual observations, due
and ring times, route, status and cleanup evidence as each gate is exercised.
