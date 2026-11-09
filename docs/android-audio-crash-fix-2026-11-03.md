# Android audio crash investigation and verified changes

## Integration status — 9 November 2026

The original investigation below records an earlier working-tree experiment.
At integration review, the helper/tests remained untracked and the production
service and preview wrapper still used their original MediaPlayer paths. The
9 November change wires both paths to AndroidAudioPlayback, supplies the bundled
preview fallback through app DI, and forwards asynchronous terminal preview
errors to ChallengeCoordinator with an ownership check. Preview stop closes its
worker; service destruction closes the service worker without blocking main.

Fresh verification of this integration is recorded at the end of this document;
the earlier counts and temporary evidence paths below are historical.

## Scope

Crashlytics version 2.8.0 (30), queried 2 November 2026: 6 fatal events, 203 ANRs,
281 non-fatal events over 30 days. This change addresses the demonstrated media
threading and preview failure defects only. It does not claim to resolve the
191-event unknown-root-cause Vivo cluster or the foreground-service crashes.

## Reproductions before changing production code

Robolectric tests exercising the original player implementation failed:

- `PreviewAudioRegressionTest.unavailableSelectedToneStillPlaysDefaultTone`:
  a selected content URI throwing IOException left preview playback silent even
  when the default alarm tone was available.
- `PreviewAudioRegressionTest.sourceOpeningAndReleaseDoNotRunOnMainThread`:
  recorded `setDataSource`, `prepare`, and `release` on main.
- `AlarmServiceLifecycleTest.serviceSourceOpeningDoesNotBlockMainThread`:
  recorded `setDataSource` and `release` on main.

The service thread findings match production ANR stacks blocked in media Binder
transactions. These are controlled reproductions of the mechanism, not a
reproduction of an OEM-specific ANR or a measurement of field ANR reduction.
Before-fix XML evidence was saved in `/tmp/mathalarm-preview-before.xml` and
`/tmp/mathalarm-service-before.xml`.

## Implementation

`AndroidAudioPlayback` creates and accesses each MediaPlayer on a dedicated
HandlerThread/Looper. Preparation still uses the platform's `prepareAsync()`.
Source opening and release also run on that thread. Stop/replacement invalidates
callbacks immediately, then queues release; service destruction releases the
player before quitting its Looper without joining on main. Release is used
without an unnecessary stop/isPlaying round trip and is valid in Error and
Preparing states.

Both scheduled playback and Android preview use this implementation. Preview
now tries the selected tone, default alarm tone, and bundled fallback. When all
sources fail, a callback on main produces the existing localized tone error and
stops preview vibration. The default shared-interface implementation preserves
the synchronous iOS player behavior.

Foreground-service promotion, scheduling, sticky restart policy, durable alarm
state, permissions, and notification behavior were not changed. In particular,
no broad exception catch or blind restart loop was added to hide the unresolved
foreground-service failures.

## Verification

- Original reproductions now pass.
- A blocked source-opening call leaves main responsive; stopping before it
  returns prevents playback and a subsequent alarm can play.
- Permission-denied sources fall back; exhausted sources report one terminal
  error on main.
- Asynchronous media errors fall back; stale errors from the replaced player
  cannot stop its replacement.
- Asynchronous preview failure reaches the challenge UI and releases audio.
- Full Android host suite: 137 tests passed; shared host suite: 205 passed.
  Two additional asynchronous-error/late-prepared regressions were then added
  and passed with the focused audio suite.
- Debug app and instrumentation APKs built successfully.
- Shared iOS simulator target compiled successfully.
- Android 15 emulator: real MediaPlayer missing-content-URI fallback and rapid
  cancellation/replacement tests passed (2 tests). These verify player start,
  not acoustic audibility.
- Real OS `snooze` delivery scenario passed: scheduled delivery after process
  exit, unreadable-tone fallback, snooze stops playback, a second actual alarm
  delivery starts playback, completion stops the service, persisted state is
  verified, and the test alarm is cleaned up. Evidence:
  `/tmp/mathalarm-audio-snooze/result.json` and `events.json`.

## Remaining investigation

The dominant Vivo ANR cluster has no usable stack/logs in the two inspected
samples. Foreground promotion failure on null-intent sticky restart also needs
further device evidence: Android documents sticky foreground-service restarts
as exempt from ordinary background-start restrictions. Neither received a
speculative patch. Main-thread playback-state persistence remains a separate
risk; these media changes do not claim to fix it.

Moving calls off main cannot make a stalled media server respond faster; it
prevents those calls from holding the UI/service main thread. Device-specific
playback failures and overall field ANR rates still need monitoring after a
release. The emulator APK is a fresh local debug build, not the Play binary.

## References

- [MediaPlayer API and thread ownership](https://developer.android.com/reference/android/media/MediaPlayer)
- [MediaPlayer in a service](https://developer.android.com/media/platform/mediaplayer/background)
- [Player state and resource lifecycle](https://developer.android.com/media/platform/mediaplayer/state-resources)
- [Sticky-service restart semantics](https://developer.android.com/reference/android/app/Service#START_STICKY)

## Fresh integration verification — 9 November 2026

The preview regressions were run before integration: both failed, demonstrating
silent fallback failure and main-thread setDataSource/prepare/release. They now
pass. The final host suites passed with no failures, errors or skips: core 168,
shared 211, Android app 157 (536 total). Eight delivery-runner Python regressions
also pass after incorporating the latest upstream CI fixes.

Commands:

```sh
./gradlew :core:testAndroidHostTest :shared:testAndroidHostTest \
  :androidApp:testDebugUnitTest :androidApp:lintDebug \
  :androidApp:assembleDebug :androidApp:assembleDebugAndroidTest \
  :androidApp:bundleRelease :shared:compileKotlinIosSimulatorArm64 --continue
```

Debug/instrumentation APKs, the minified release AAB and shared iOS simulator
compilation passed. Debug lint reported 0 errors, 57 warnings and 7 hints.
The AAB uses the existing debug signing configuration; it is build validation,
not an upload-signed release candidate.

On the existing API 35 emulator, both AndroidAudioPlaybackDeviceTest tests pass:
missing-content fallback to bundled audio, and rapid cancellation followed by a
new alarm. No physical device or production application package was changed.
These tests establish actual MediaPlayer start, not acoustic audibility.

The pre-existing local formal suite was reviewed and rerun for AndroidDelivery,
AndroidQueue, AudioFallback, Challenge and ChallengeAudio. All five baselines and
restored baselines pass; all five deliberate source mutations produce the
expected Safety counterexample. AudioFallback now maps to AndroidAudioPlayback's
player-identity guard within one request generation. The model property and
bounds are unchanged. Other covered ownership/occurrence guards are unchanged;
the containing-file fingerprints were refreshed after review. These are bounded
abstractions, not proof of MediaPlayer threading or OS delivery. Async preview
failure and stale callback behavior are checked by the implementation tests.

The untracked formal-verification workspace is separate work and is not included
in the audio-fix commit. Local evidence from this run:

- `/tmp/mathalarm-audio-before-tests.log`
- `/tmp/mathalarm-audio-focused-2.log`
- `/tmp/mathalarm-audio-validation.log`
- `/tmp/mathalarm-audio-device.log`
- `/tmp/mathalarm-audio-after-formal/` and `/tmp/mathalarm-audio-after-challenges/`
- `/tmp/mathalarm-audio-charts/` (baseline/mutation/restored reports and Mermaid charts)

The reviewed fallback chart below intentionally hides rejected stale callbacks;
TLC explores them even though they produce no visible state transition.

```mermaid
stateDiagram-v2
  [*] --> Chosen
  Chosen --> Ringing: chosen tone prepared
  Chosen --> Default: chosen tone fails
  Default --> Ringing: current default prepared
  Default --> Bundled: default tone fails
  Bundled --> Ringing: current fallback prepared
  Bundled --> Failed: bundled tone fails
```

Removing the player identity guard produces the expected two-step counterexample:
chosen tone fails, then its stale prepared callback starts the superseded player.
This is mutation evidence, not a failure of the integrated implementation.

The real OS snooze scenario also passed on API 35: process exit, scheduled
receiver/service/player start with inaccessible-tone fallback, accepted snooze,
a distinct second OS delivery, completion, service shutdown, persisted-state
verification and fixture cleanup. Evidence is in
`/tmp/mathalarm-audio-snooze-2026-11-09/result.json` and `events.json`.
This run used the pre-fetch delivery runner; upstream's subsequently integrated
change only retries torn telemetry XML reads, and its eight regressions pass.
A final focused rerun of all five backend tests also passed after extending the
fallback regression to invoke both stale prepared and stale error callbacks.

Physical audibility, OEM-specific behavior, field ANR reduction, and Play-signed
upgrade acceptance remain release checks; this commit does not claim those pass.
