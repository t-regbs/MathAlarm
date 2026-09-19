# Skip next alarm review

## Expected behavior

- A weekly alarm skips its next selected date. The same weekday is scheduled the following week; other selected weekdays keep their next dates.
- A non-repeating alarm with multiple selected days removes one concrete occurrence. It never generates another weekly cycle during skip, undo, or recovery.
- Skipping the last occurrence in a finite schedule turns the alarm off, unless it still has active playback or a snooze. Undo before the skipped time restores that occurrence and enables the alarm.
- A single one-time alarm uses the on/off switch; it does not offer Skip next.
- A skip affects only its alarm ID. Other alarms, including alarms at exactly the same time, retain their schedules.
- Only one skip can be pending per alarm. Undo expires at the skipped local time. Calendar exceptions follow local wall-clock time when the timezone changes.
- A separate snooze remains scheduled. Editing the schedule or explicitly enabling it starts a fresh schedule and clears the skip.

## Review changes

An older snackbar could undo a newer skipped date on the same alarm. Undo now checks the date supplied by the originating card or snackbar before changing persisted or scheduled state.

The card and use case share skip eligibility, and skip uses the same occurrence calculation as recovery. Card spacing uses theme values where available and named dimensions otherwise. Unused action-menu labels and imports were removed.

What's New retains both math challenges and Skip next in a release-ordered catalog. It shows all unseen features as pages, with separate interactive previews and feature actions. Trying the skip example never changes actual alarms. Settings replays only the user's latest update group, not the full catalog.

## Regression coverage

`SkipNextAlarmTest` checks persisted state and scheduled occurrences for all 127 weekly weekday selections and all 120 eligible finite selections. It also covers independent alarms at matching times, duplicate skip attempts, stale undo, expiry, final-occurrence undo, timezone changes, snooze preservation, obsolete broadcasts, and scheduling-failure recovery.

Host and native simulator tests verify application scheduling rules and adapters. They do not establish physical-device audibility or overnight OS delivery reliability; those checks remain in `docs/testing.md`.

## Verification results

- Android host suites: 330 tests passed (119 core, 134 shared, 77 Android application).
- iOS simulator suites: 256 tests passed (118 core, 138 shared).
- Android debug APK build and `git diff --check` passed.
- API 35 emulator: visually inspected the announcement, exercised example Skip/Undo, verified View alarms navigation and Settings replay. The dedicated emulator was stopped afterward.

## Announcement history

Each feature has a stable ID in `AnnouncementFeature`. Seen status is persisted independently for each ID. The old single `mathalarm_last_announcement` value migrates only that exact feature; it cannot establish that earlier features were seen.

Automatic display snapshots the unseen catalog once per navigation session, so acknowledging a page does not remove it while browsing. The previous/next arrows, dismissal, and feature actions acknowledge the displayed page only. Unvisited pages remain eligible on a later fresh launch. Configuration restoration preserves the session and page; Settings replays the latest update group even after it has been read.

To add another announcement, retain existing entries and IDs, append a feature to the catalog, add localized copy and its preview/action, and extend the catalog tests. Do not infer seen status from the app version or assume seeing a newer announcement means older announcements were seen.

Catalog follow-up verification: 142 shared Android host tests and 146 shared iOS simulator tests passed, and the debug APK built successfully. Emulator checks covered legacy skip-only migration, both unseen pages, resuming with only the unacknowledged feature after restart, suppressing automatic display once both are acknowledged, and replaying the full catalog from Settings. The shared suites also exposed and corrected an existing test assertion that rejected the valid singular text “1 minute”.

The pagination row now groups previous/next arrows around short page indicators; Got it and the feature action remain in a separate row. Settings replay is a persisted snapshot of features the user had not seen when the announcement catalog last changed. Sequential updates replace this group with newly introduced features; skipped releases include all still-unseen features. Patch releases with no announcement changes preserve the group. A migration with no remaining unseen features falls back to the newest feature, since historical update groups were not previously recorded.

Pagination/update-group verification: 147 shared Android host tests and 151 shared iOS simulator tests passed, plus the Android debug build. Emulator checks confirmed that an already-updated user replays only the newest feature, while a fresh history includes both features with arrows and indicators separated from the action row. New regression cases cover sequential updates, skipped releases, partial acknowledgement, restart persistence, and migration without historical groups.

## Due snooze correction

The final review found that Skip/Undo could drop a snooze whose time had just passed while its broadcast was awaiting handling. Skip/Undo now preserve both its persisted timestamp and its existing platform registration when it is future or less than one minute overdue. A separate regular-occurrence cancellation operation on Android and iOS avoids cancelling or rescheduling that snooze. Full cancellation still includes snoozes, and recovery retains its existing expiry policy.

Regression coverage exercises Skip and Undo for weekly and finite alarms, including skipping the final finite occurrence, at future, exactly due, one-second-late, and grace-boundary times. The tests assert normal schedules, persisted snooze state, and successful delayed delivery. Platform adapter tests also verify that regular cancellation leaves the snooze registered while full cancellation removes it.

Due-snooze verification: all 346 Android host tests and 272 iOS simulator tests passed, along with the Android debug build and `git diff --check`.

## Late delivery and deletion follow-up

A second cancellation review found that Android's service could receive a previously queued start intent after its alarm had been deleted, disabled, snoozed, or completed. Playback now checks the current database row and active occurrence under the shared command lock before starting audio. The same check covers service restoration and promotion of queued alarms. Pending validation keeps the service alive until all waiting starts have been checked.

Regression coverage verifies deleted regular and snoozed deliveries stay silent through recovery, skipped deliveries remain rejected before and after recovery for single-day weekly, multi-day weekly, and finite schedules, and the next valid occurrence still rings. Android service tests cover obsolete start snapshots, deleted queue entries, and restoration after deletion.

Follow-up verification: 351 Android host tests and 121 core iOS simulator tests passed (472 total), plus the Android debug build and `git diff --check`. This pass used automated tests; physical-device alarm delivery was not exercised.
