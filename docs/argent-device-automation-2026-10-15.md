# Argent device automation — 15 October 2026

Argent **0.29.0** is installed in `build/argent-tooling`. Use the project wrapper:

```sh
./scripts/argent setup                    # Recreate the pinned local installation
./scripts/argent run list-devices
./scripts/argent tools describe describe
./scripts/argent run describe --udid TARGET_UDID
./scripts/argent run screenshot --udid TARGET_UDID --out build/argent-screen.png
```

The wrapper sets `DO_NOT_TRACK=1` before installation or execution.
`.argent/config.json` also disables telemetry for commands run directly in this
project. Both opt-outs were verified; no telemetry device ID was created.
Application dependencies and editor permission/MCP configurations are unchanged.
Argent's local server starts on demand. No global npm installation is needed.

**Later 15 October physical validation succeeded.** The connected iPhone 11,
iOS 27.0.1 (24A446), runs fresh signed MathAlarm 2.3.1(25). Argent's signed runner
operates normal controls, screenshots, text entry and task-fixture cleanup without
reinstalling MathAlarm. In-place 23 → 24 → 25 upgrades preserve all five original
alarms; the second also preserves an active challenge. Human observation establishes
native audibility and restored app audio. See the
[fresh M8 record](native-ui-migration-milestone-8-2026-10-15.md).
The evaluation table below describes the earlier disconnected phase only.

## Verified local operation

Host: Xcode 27.0 (`27A266a`), Node 26.7.0, npm 11.19.0, macOS arm64.
Disposable iPhone Duo, iOS 27.1, UDID `C2273638-150F-4EEE-BBF4-7F0917EA5421`.
The retained simulator **MathAlarm 2.3.1(23)** was installed only there; its identity
and executable hash are in `build/argent-evaluation-2026-10-15/app-identity.json`.
This verifies the tool operating SwiftUI; it is not acceptance of the newer
working-tree application changes or any pending Milestone 8 physical gate.

| Operation and expected outcome | Observed | Status / local evidence |
| --- | --- | --- |
| Inventory and boot explicit disposable target | Local devices listed; Duo boots headlessly | PASSED: `argent-devices.json`, `cli-devices.json`, `boot.json` |
| Launch, screenshot and accessibility tree | MathAlarm opens; cover-panel image and normalized control frames available | PASSED: `launch.json`, `initial-screen.json`, `initial-shot.json` |
| Touch and text input | Got it/Add/title controls respond; field contains `Argent smoke draft` | PASSED: `dismiss-announcement.json`, `open-draft.json`, `focus-title.json`, `typed-draft.json` |
| Open, half-open, close | Inner/cover panels switch; exact unsaved title remains in each tree | PASSED: `unfolded-draft.json`, `half-open-draft.json`, `closed-draft.json` |
| Cancel/discard unsaved fixture | Final screen shows No alarms | PASSED after semantic UI wait: `cancel-draft.json`, `discard-confirmed.json`, `wait-empty-list.json`, `final-screen.json`, `final-shot.json` |
| Physical iPhone 11 during initial evaluation | Paired, disconnected; no runner installed or phone interaction | UNAVAILABLE then; superseded by the later physical run above |
| Cleanup | Zero persisted alarms; disposable simulator removed, all existing simulator names/states preserved; local server stopped | PASSED: `verification-summary.json`, `cleanup.json`, `server-stopped.json` |

Evidence root: **`build/argent-evaluation-2026-10-15/`**, ignored local artifacts.
An immediate post-discard tree still shows the departing editor; asserting the
empty list against that snapshot failed. The later screenshot visibly shows the
empty list and `await-ui-element` passes. Use semantic waits before asserting
navigation completion, then get a fresh tree before calculating new coordinates.
The first sandboxed server start failed binding loopback (`EPERM`); elevated local
execution worked. These setup/observation failures are retained, not product bugs.
npm 11 notes five native dependency install scripts are not approved by its
allowScripts policy. The operations above work; additional debugging/profiling
features have not been verified or had that policy changed.

## Physical use and validation limits

Argent supports **USB-connected, unlocked iPhones** with Developer Mode. Its first
hardware interaction builds/signs/installs an **ArgentRunner** using Xcode's Apple
Development identity; developer trust may need a manual step. Physical iPads,
power/app-switcher buttons and physical native profiling are unsupported. Rotation
on hardware and actual audibility still require observation. See the official
[physical-iPhone guide](https://docs.swmansion.com/argent/docs/features/physical-ios-devices/).
Use a currently signed MathAlarm candidate for hardware validation. The retained
3 October development exports' profile expired on 8 October; Argent's new runner
does not re-sign an existing MathAlarm app. Inspect the installed candidate before
resuming tests and preserve data during any necessary in-place upgrade.

Always specify the target UDID. `launch-app` can restart an already running app;
account for that when validating durable progress. **`reinstall-app` clears data
and permissions**, so preserve the MathAlarm installation and use ordinary in-place
Xcode/device deployment for upgrades. Use only disposable alarms and verify cleanup.
See the official [tools reference](https://docs.swmansion.com/argent/docs/reference/tools/).

The later physical runner's first UI-automation handshake timed out; reset and retry
with the unlocked phone succeeded. Time-picker keyboard input reported success but
did not change the wheel; wheel swipes/read-back were required. A Submit during a
keyboard transition missed, so subsequent taps wait for a stable tree and semantic
navigation. Mail's remote composer exposes an empty accessibility tree; fresh
screenshots allowed Cancel/Delete Draft and a semantic wait verified Settings return.
These tooling limitations are retained separately from product failures.

Fresh build 24 Duo transitions preserve a changed nested draft; all three task-created
simulators were removed after tests. Physical fixture ID12 is deleted, exact original
IDs 5, 6, 7, 8, 11 remain and all test occurrence state is empty. Physical runner and
local server were stopped; both MathAlarm and ArgentRunner remain installed.
Evidence: `build/native-ui-m8/2026-10-15/` (ignored/private). Never publish copied
preferences, Mail account fields, signing material or local server tokens.

Argent can help automate native navigation, screenshots, keyboard and Duo work.
Locked/terminated AlarmKit delivery, permission transitions, audio routes, VoiceOver
speech, reboot/DST and overnight/multi-day reliability keep their separate evidence
requirements in the [device checklist](ios-alarm-recovery-manual-test.md).
