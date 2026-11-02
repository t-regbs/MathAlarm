# iOS AlarmKit recovery and Milestone 8 device checklist

**15 October candidate:** **2.3.1(25)** is installed in place on the connected
iPhone 11/iOS 27.0.1. Fresh baseline IDs **5, 6, 7, 8, 11** are all original alarms;
preserve all five. Both upgrades retain exact rows/schema/preferences; 24 → 25 also
retains the active question 2 record and recovery session. Build 24 background
delivery, wrong-answer sound/foreground deferral and terminated native fallback
pass by user observation; restored app sound **fails on 24**. Build 25 playback
repair **passes the actual terminated native fallback/Stop/unlock retest**: exact
`9 × 7` at question 2 and app sound return, user-confirmed. Physical configured-Mail
Cancel/Delete Draft also passes. Completion and fixture deletion clear all test
state and preserve the five originals; **100-second acoustic no-ring observation
passes**, user-confirmed “yes silent.” Development profile expires **22 October,
19:33:03 UTC**. See the
[fresh validation record](native-ui-migration-milestone-8-2026-10-15.md) for build
hashes, current results and pending observations. The 3 October candidate/results
below are historical and do not establish current-binary acoustic acceptance.

## Next physical checks on build 25

Use a new clearly named disposable alarm for each run; ID12 has been deleted.
Record due/ring/Stop/operation times, build/device/OS, route and expected versus
observed behavior. Do not run the destructive simulator test harness on the phone.

| Check | Current status / concrete steps |
| --- | --- |
| Completion | **PASSED on build 25**: state clears after 21:44:32 BST completion; user confirms “yes silent” to the explicit ≥100-second no-ring/sound check. Armed snooze/edit/disable/delete remain separate |
| Initial foreground/locked/terminated delivery | Background initial delivery passed on 24. For each remaining state, create a near-future once alarm, enter that state before due, observe native timing/audibility and Stop → maths. Terminated fallback passed on 25 but is a different check from terminated initial delivery |
| Abandoned authentication / continuous recovery | Historical build 21 has audible recovery beyond ten minutes. Repeat on current policy: Stop while locked, abandon unlock, repeat beyond ten minutes with actual ring records, then unlock and solve. Active solving must have sound/progress without native interruption |
| Armed snooze/edit/disable/delete | Start an unresolved fixture and perform one operation per run. Wait 100 seconds past the old fallback; snooze must ring only at its accepted future time. A rejected snooze must retain recovery. Deleting a completed fixture does not prove armed deletion cancellation |
| Multiple/recurring deliveries | Use separate fixtures and natural recurring weekdays without editing. Verify ordered exact occurrence progress, no duplicate/lost delivery, cancellation and cleanup |
| Permission / tutorial / PiP | Create a dirty fixture draft; deny/revoke/regrant Alarm permission, use tutorial PiP → Settings → return, verify the exact originating draft and no duplicate save. Restore permission and discard only fixture data |
| Routes / interruptions / Silent / Focus | Current native speaker recovery and resumed app sound pass. Observe call/Siri/other audio interruption, headphones/Bluetooth connect/disconnect, Silent/Focus and vibration separately. Measure actual timing; AVAudioPlayer acceptance is not an audible-output result |
| Adaptive/accessibility | Simulator iPhone/iPad and sampled Duo pass; configured-Mail cancellation passes physically. Observe spoken VoiceOver/focus, physical iPad/window resizing, current Dynamic Type/keyboard/locales/appearance and Duo preview transitions |
| Upgrade / minimum OS / time | Active 24 → 25 preservation passes. Still verify old queued payload/native registration delivery, exact iOS 26.0 execution (unavailable locally), timezone/DST, reboot, overnight and multiple natural recurring dates |

Runtime warnings and physical main-thread/audio timing remain open; no physical
profiler samples were captured. Distribution/TestFlight/store gates remain in the
[release audit](ios-release-readiness-2026-09-24.md). Milestone 8 is not complete.

## Current candidate and pending device work — 3 October 2026

**The full physical matrix remains OPEN; validation has resumed** after the user
connected the iPhone 11. It now runs iOS 27.0.1 (24A446). Build 21 was installed
in place after local data backup; all four existing alarm rows and preferences
remain identical. The first locked Stop/recovery cycle passed: the user heard the
18:36 follow-up, unlocked into maths, solved and observed no further ring for at
least 70 seconds. Native state confirms recovery/queue cleanup and preserved
original alarms. A second run shows native recovery acceptance advancing from
attempt 10 to 11 after ten minutes in the original session, still one queued
handoff; the user confirms every recovery audible and sound continuing through
a wrong answer. Exact progress and the same visible second question/sound survive
forced termination and ordinary relaunch, with the queue empty. Accepted five-minute
snooze then rings foregrounded at about 19:07 with no premature recovery, and Stop
opens a fresh occurrence/question 0. The disposable row and all native/progress
state are subsequently deleted; original rows still match. Edit/disable cancellation,
other delivery states and the remaining matrix are pending.
Details are in `build/native-ui-m8/physical-iphone-11/`.
Use the
[M8 evidence record](native-ui-migration-milestone-8-2026-10-03.md) for simulator,
upgrade and development archive/export results. Current candidate **2.3.1(23)** is
installed, based on `c158fd3` plus the changes hashed in
`build/native-ui-m8/physical-iphone-11/source-at-install23.json`. The physical
20 → 21 and 21 → 22 upgrades preserve all four original rows and preferences;
21 → 22 also preserves schema identity while switching to system SQLite.
The 22 → 23 upgrade preserves the active occurrence, full partial progress and
decoded recovery session as well as every alarm row/schema/stable preference.
Use a freshly signed candidate if
testing resumes after the current profile expires on 8 October. Record the actual candidate
commit/source hash, version/build, device/model/OS and timezone before testing.
Do not reuse a future rebuild's results under an earlier binary identity.

Candidate **22 foreground deferral PASSED**, user observed, after device feedback:
no system notification/pop-up interrupts active solving. Two snapshots 2m38s apart
retain the exact full progress record and empty queue, the later one over three
minutes after delivery, with
accepted native fallback renewals. Later completion clears recovery/progress/queue;
acoustic no-later-ring confirmation is still separate. The fresh unfinished
lock/background/termination run remains pending. The policy defers recovery while the
ready maths screen is visible/active, renewing a 90-second OS fallback every 20
seconds. Leaving/locking tightens to the normal 60-second interval; process death
retains the last fallback (up to 90 seconds). Physical build-21 passes do not verify
this new policy. On 22, leave an unfinished challenge visible for at least three
minutes: sound/progress continue with no system recovery interruption. Then separately
lock, background and terminate; require an audible fallback and exact progress
restoration. Solve/snooze and wait **100 seconds** (past both native slots' prior
deadlines), then verify no recovery. A transition callback normally restores a
60-second deadline; a suspended/terminated process can retain the last fallback
for up to 90 seconds. Record that case separately from the historical 70-second
build-21 observations.

**Build22 lock/unlock FAILED:** the fresh 19:57 fixture retains question2 and sound
but presents “Alarm recovery needs attention. Try again.” User wording matches a
logged scene-task `CancellationError` after native replacement acceptance.
Candidate23 protects the complete refresh from screen cancellation; 17 hosted
tests, signed archive/audit/export and physical in-place upgrade preservation pass.
It is installed; the user confirms physical lock/unlock **passes both before and
after snooze**, without errors. Keep the
build22 failure in the record and repeat
lock/unlock on the actual new binary. Continuous app audio alone does not establish
a newly delivered system fallback.
The next build23 capture instead shows an accepted five-minute snooze due
20:12:27.530 BST, old progress/recovery/queue cleared and no persisted recovery
failure. State cleanup passes; actual audible timing/no early recovery is pending.
Use a fresh unfinished occurrence for any
remaining lock/termination test; the snoozed question2 should not resume.
The 20:12:49 capture shows the correct new snooze activeAt, fresh problems/start
and accepted new recovery session, already at question2 (15 ÷ 3), queue empty and
no persisted recovery failure. New occurrence bookkeeping passes; actual audible
timing still awaits observer confirmation; the later user report confirms visible
warning absence on lock/unlock. At 20:14 the new occurrence is completed with all
recovery/progress/queue cleared. The user confirms fixture deletion and the 20:18
copy verifies ID10 absent, all test state cleared and original alarms unchanged.
The requested 100s no-ring observation is not separately stated; cleared state
more than 100s later is storage evidence only. Abrupt termination/native
fallback requires a fresh unfinished occurrence and is not tested after completion.

Use new alarms named `M8 disposable A` and, where needed, `M8 disposable B`.
Record their IDs/settings and initial alarm count. Preserve existing alarms,
permissions and preferences; restore temporarily changed permissions/routes/Focus
and delete only these fixtures afterward. Test Alarm/preview does not exercise
native delivery. Disconnect mirroring during authentication tests. Keep wall-clock
timestamps for expected due time, actual audible start, system Stop, unlock,
in-app first sound, follow-up and authoritative resolution; record screen video/log
locations separately from observer-confirmed sound.

| Run | Concrete steps and expected outcome | Current status |
| --- | --- | --- |
| Delivery states | Schedule A a few minutes ahead, separately foregrounded, backgrounded, locked and terminated. Verify actual sound and challenge appears only after native delivery; unlock opens the right occurrence. | **PARTIAL:** build 21 locked follow-up/unlock and foreground snooze delivery passed; background/terminated initial delivery pending |
| Abandoned authentication / continuous recovery | Locked ring → Stop → abandon/cancel unlock → leave locked for 100 seconds. Expect re-ring without opening app. Repeat actual Stops for **over ten minutes**, then solve and wait 100 seconds: no extra recovery. Record every deadline/start. | **PARTIAL:** build21 audible locked recovery beyond ten minutes passed; build22 departure/termination fallback and ≥100s cancellation pending |
| Foreground deferral | Stop an actual delivery, make a wrong answer and advance one of three questions. Leave the ready screen visible/active ≥3 minutes; no system recovery should interrupt. Lock/background/terminate separately; expect fallback within 90 seconds (observe for 100), then unchanged partial progress on return without a cancellation warning. | **PARTIAL:** build22 no foreground pop-up/progress passed; lock/unlock warning failed while sound continued. Build23 lock/unlock passes before and after snooze. New-policy native fallback remains pending |
| Cancellation | In separate runs with recovery armed: solve; accept snooze; edit due time; disable; delete. On build22 wait at least 100 seconds past resolution and the chosen snooze/new time as appropriate. Expect only authoritative future deliveries. A failed snooze must retain unresolved recovery and retry UI. | **PARTIAL:** build21 solve/no further ring ≥70 seconds and accepted snooze/no premature recovery/actual five-minute delivery passed; build22 native cleanup passed, ≥100s acoustic cancellation, edit/disable and delete-with-armed-recovery pending |
| Durable progress / multiple deliveries | Use several questions; advance and make a wrong answer, terminate/relaunch. Compare questions/index/start time/incorrect count exactly. Deliver B while A is unresolved, resolve A and verify B remains reachable with its own recovery. | **PARTIAL:** forced termination/relaunch exact progress, visible question and sound passed; different overlapping deliveries pending |
| Permissions / originating draft | Start with new draft; deny the system request, explicitly retry, open tutorial, use PiP → Settings → Alarms grant → app. Expect the same draft/settings and one accepted save. Revoke, deliver/save/retry as applicable, then regrant. Interruption by preview/real alarm must release tutorial audio without stopping real audio. Restore original permissions. | **PENDING** |
| Audio/routes/interruption | Wrong answer keeps actual sound; solve/snooze stops it promptly. Repeat with call/Siri/another audio app, wired headphones and Bluetooth connection/disconnection, silent switch and Focus. Record output route, audibility, interruption begin/end, restart delay and visible input responsiveness; use Instruments if stalls occur. | **PARTIAL:** actual wrong-answer sound and post-relaunch sound passed; routes/interruptions/physical stall measurement pending |
| Native clients | iPhone/iPad portrait/landscape and live window resizing, keyboard/large Dynamic Type, light/dark/Reduce Motion/Transparency, nine locales; spoken VoiceOver focus/feedback/return, configured-Mail open/cancel (send nothing), available Duo closed/open transitions with dirty draft/preview retained. | **PENDING** |
| Upgrade / legacy / minimum OS | Preserve a disposable old-version alarm, preferences and queued unresolved progress through in-place upgrade, then actual delivery/cancellation. Exercise old queued payload formats with authoritative saved data. Test exact iOS 26.0 separately; 26.3.1 simulator success is not that gate. | **PARTIAL:** 21 → 22 complete rows/schema/preferences preserved on simulator and iPhone; 22 → 23 active occurrence/progress/recovery session preserved on iPhone. Older queued payload/registration upgrade pending, exact26.0 unavailable |
| Time/reboot/long duration | Record timezone change and DST behavior, reboot before due delivery and recover after unlock. Leave a single-day and multi-day recurring alarm enabled **without editing** across actual overnight/multi-day cycles; an unresolved later cycle must coalesce progress and completion must not resurrect an older payload. | **PENDING** |

For each run append: `status (passed/failed/unavailable/pending), commit/source hash,
build, device, OS, fixture ID/settings, steps, expected, observed, timestamps,
audio route/observer, artifact paths, cleanup`. If no person observed sound, record
**audibility not measured**, even if playback telemetry succeeded. The main-thread
audio measurements and remaining SwiftUI warnings in the M8 record need physical
latency/live resize follow-up; do not infer that they are harmless.

## Initial recovery test and historical prototype evidence

Use a newly saved temporary alarm with an easy challenge, scheduled a few minutes ahead. Do not use Test Alarm: it does not exercise AlarmKit or recovery. Leave the app and lock the iPhone. Disconnect iPhone Mirroring while testing authentication.

## Main check

1. Let the alarm ring while locked.
2. Slide the system alarm control, then abandon or cancel unlocking. Leave the phone locked.
3. Wait 100 seconds on candidate22. **Expected:** the alarm rings again, without first opening Math Alarm (normally at 60 seconds outside solving; retained foreground fallback up to 90).
4. Unlock and solve the challenge. Wait another 100 seconds. **Expected:** no further recovery alarm.

Record whether the app still opens correctly after authenticating, whether the first recovery rings, and whether any error appears. If step 3 fails, the main bypass remains unresolved even if opening the app later starts its music.

## Cancellation checks

- With a recovery pending, successfully snooze. There should be no ring at the recovery's original one-minute deadline; the alarm should ring at the chosen snooze time.
- Repeat with a pending recovery, then disable or delete the temporary alarm. There should be no follow-up ring.
- Repeat with a pending recovery, then save an edit moving the alarm to a later time. The old recovery should not ring.
- Repeat with the app terminated before the initial alarm. This checks whether the background Stop intent can launch and schedule recovery without foreground activation.
- Give a wrong answer and leave the challenge unfinished. Recovery remains armed; on candidate 22 its deadline renews while the ready screen is visible/active. Leave/lock to observe the fallback. Complete it and confirm both recovery slots are cancelled.
- Stop at least six consecutive deliveries without solving. Each Stop should schedule another ring 60 seconds later. Continue beyond ten minutes to check that elapsed time does not end recovery; then solve and confirm it stops.

## Prototype limits

The historical prototype used a separate fixed AlarmKit alarm, scheduled 60 seconds after the handled Stop/open action, including while solving. It uses the saved tone/settings and has no attempt cap or time expiry. Candidate 22 refines the foreground policy as described above, preserving an OS fallback. Completion, accepted snooze, disable, delete or an invalidating edit cancels recovery. iOS still requires unlocking before the maths screen can be used.

The prototype depends on iOS executing the background Stop intent while locked. Compile and unit-test success do not prove that behavior. If scheduling fails, the native intent throws and logs the failure; current shared/native challenge and restoration flows also present retained failures with retry (implemented after the prototype). Physical intent execution and recovery failure behavior remain acceptance tasks. Recovery does not guarantee that system dismissal cannot bypass maths.

After testing, disable or delete the temporary alarm.

## Recorded result

27 September 2026: the user confirmed **“ok that works”** after receiving the main recovery test steps on the installed iPhone build. This is user-reported success; no per-step timing or agent-observed recording was supplied. The cancellation checks and terminated-app case above remain separate tests.

The confirmed September build initially had a five-follow-up cap and ten-minute session expiry. The user subsequently chose continuous recovery until the challenge is solved; both limits were removed. The October build21 extended run above now verifies audible recovery beyond ten minutes. Candidate22's refined foreground/departure policy needs the remaining separate checks above.


## Milestone 5 physical follow-up (still open)

The [native Milestone 5 record](native-ui-migration-milestone-5-2026-10-02.md) covers implementation, controlled scheduling, native token persistence faults and simulator process restart. The earlier paragraph about app-level failure presentation describes the prototype: current native challenge/restore flows present shared initialization/recovery failures and allow retry. Physical behavior below remains unverified.

- Deliver an alarm, advance partway, acknowledge its handoff, then terminate/relaunch before resolution. Check identical questions, index, start time and incorrect count; recovery must continue independently of an empty native queue.
- Keep a repeating alarm unresolved across its next actual repeating cycle without editing/rescheduling it. The later date-specific token must join the same challenge, preserve exact progress and retire only its matching known schedule timestamp. Complete once; a legacy handoff must not recreate the already coalesced delivery. This requires multi-day evidence, not clock simulation.
- Deliver a different alarm while the first is unresolved. Check stable ordered presentation, independent durable occurrence/recovery evidence and one real audio owner. Resolve/snooze the first; only its recovery/playback stops, and the second remains reachable.
- Interrupt multiple dirty drafts, a nested route, staged tone and maths preview. Resolve the real occurrence and verify return to the same draft/settings/route; preview cleanup must not cancel real recovery.
- Repeat with locked/background/terminated Stop intents, denied/revoked permission, Bluetooth/DND routing and sustained continuous recovery. Record timing, device/runtime, source commit, acoustic evidence and cancellation outcomes separately.

Do not treat the simulator's controlled scheduler or token-chain smoke as proof of native physical delivery, audibility or uninterrupted recovery.


## Milestone 6 native presentation follow-up

The [Milestone 6 record](native-ui-migration-milestone-6-2026-10-02.md) records native app settings, announcements, feedback/share, nine-locale catalogs and packaged sounds, plus current simulator presentation/client evidence. It supersedes renderer-era placeholder and keyboard descriptions only where runtime evidence is listed. Theme changes and supplementary presentation preserve retained drafts, routes, staged tones and permission guards; delivery/restoration/audio boundaries remain application-owned.

These implementation checks do not close physical delivery or release acceptance. Recheck actual permission denial/revocation/regrant and configured Mail cancellation/errors, iPhone/iPad portrait/landscape and live window resizing, VoiceOver spoken traversal/focus/announcements, minimum iOS 26.0, and native launch cold-start timing. For delivered alarms, keep every Milestone 5 physical continuous-recovery, acoustic/Bluetooth/DND, background/locked/terminated handoff and multi-day coalescing check above. Add interruption of app settings and an unacknowledged announcement; resolve/snooze authoritatively, then verify the same draft, nested route, staged sound and announcement page return without incidental acknowledgement. Do not send feedback or share content to an external recipient merely to test presentation.

Milestone 7 owns permanent XCTest/UI targets, reproducible CI/packaging checks and approved cleanup. Milestone 8 owns physical reliability, signed archive/TestFlight/privacy/store review and release acceptance. Simulator passes are not release readiness.
