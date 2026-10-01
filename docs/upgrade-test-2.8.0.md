# Pixel upgrade check: 2.7.0 to 2.8.0

Performed on 23 September 2026 on a physical Pixel 9 running Android 17, with Europe/London as the alarm timezone.

## Build and scope

- Baseline: source tag `v2.7.0`, version code 29, database schema 7.
- Candidate: `origin/main` at `7a75288`, with version 2.8.0 / code 30, database schema 9.
- Both debug builds used the temporary application ID `com.timilehinaregbesola.mathalarm.upgradecheck` and the same local signing key. The temporary Firebase client configuration was adjusted to match this ID.
- Installed the baseline, created alarms through its UI, acknowledged its math-challenge announcement, then installed the candidate with `adb install -r`. App data was not cleared between versions.
- The baseline was stopped to capture its database before updating. Scheduling checks therefore verify recovery after opening the upgraded app, not unattended package-replacement recovery.

The Pixel's existing Play-installed app has a different signing certificate from local builds. It was left unchanged. This test verifies application migration on physical hardware; it does not verify delivery of a Play-signed release, release minification, or upgrading the user's actual production data.

## Results

| Check | Result |
| --- | --- |
| Database upgrade | Passed: schema 7 migrated to 9, retaining both baseline alarms. |
| Existing alarm configuration | Passed: every original database field matched before/after for an enabled weekly alarm and a disabled one-time alarm, including title, selected day, time, tone URI, vibration, three-question challenge, enabled state, and pending occurrence. |
| Existing snooze policy | Passed: the enabled alarm retained five-minute unlimited snoozes; the snooze-off alarm retained `snooze = 0`. Migrated limits and counters were zero. |
| Notification permission | Passed: the grant from 2.7.0 remained granted after updating. |
| Schedule recovery | Passed: Android AlarmManager contained the weekly alarm at its original 24 September, 22:07 occurrence after opening 2.8.0. |
| Skip next | Passed through UI: the migrated weekly alarm moved to 1 October, 22:07 in both the UI and AlarmManager. |
| Skip persistence and Undo | Passed: skipped state survived process restart; Undo restored 24 September, 22:07. Original database fields still matched afterward. |
| Announcement migration | Passed: the acknowledged math-challenge announcement stayed acknowledged. Only Skip next and snooze settings were queued; both displayed, and neither repeated after acknowledgement and restart. |
| New alarm defaults | Passed: a newly saved 2.8.0 alarm displayed and persisted five-minute snoozes with a maximum of three. |

No failure was observed in these checks. Raw before/after/final database and preference snapshots were saved locally under `/private/tmp/mathalarm-upgrade-check/`; they are temporary test evidence, not committed artifacts.

The disposable test app was removed after testing, which removes its fixture alarms. No reboot, ringing/audibility, active-snooze migration, tablet, or actual Play review-card checks were included in this upgrade run. A Play internal-track update is still needed to verify the signed production upgrade path.
