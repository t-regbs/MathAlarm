# Android analytics events

Android sends these custom events through Firebase Analytics. The shared app code uses an
`AnalyticsTracker`; iOS currently supplies a no-op tracker. Firebase's automatic events continue
to work as before. No alarm title, ringtone URI, answer, alarm ID, or scheduled time is sent.

| Event | When it is recorded | Parameters |
| --- | --- | --- |
| `app_screen_viewed` | Top navigation destination changes, including tablet detail panes | `screen`, `layout` (`single_pane` or `two_pane`) |
| `alarm_editor_opened` | An editor model is initialized | `mode` (`create` or `edit`) |
| `alarm_saved` | An alarm save command succeeds | `mode`, `repeat`, `difficulty`, `snooze_enabled`, `question_count`, `snooze_minutes` |
| `alarm_save_failed` | A save is blocked by exact alarm permission or its operation fails | `reason` (`exact_alarm_permission` or `operation_error`) |
| `permission_prompted` | A notification runtime prompt or permission settings handoff is opened | `permission_type`, `route` (`runtime` or `settings`) |
| `permission_result` | Notification runtime callback or return from permission settings | `permission_type`, `result` (`granted`, `denied`, or `still_missing`) |
| `alarm_ringing_started` | The service starts an active alarm occurrence; restarts of that occurrence are deduplicated | `from_snooze` |
| `alarm_completed` | An active occurrence is successfully dismissed | none |
| `alarm_snoozed` | An active occurrence is successfully snoozed | none |
| `alarm_skipped` | Skip next succeeds | none |
| `alarm_preview_started` | A test alarm challenge opens | none |
| `challenge_started` | A new challenge starts; restored in-progress occurrences are not counted again | `preview`, `difficulty`, `question_count` |
| `challenge_completed` | A challenge finishes and the alarm action succeeds | `preview`, `duration_seconds`, `incorrect_answers` |
| `review_request_attempted` | The app records an in-app review attempt before contacting Play | none |
| `review_request_outcome` | A recorded attempt ends | `result` (`request_failed`, `launch_failed`, `context_changed`, `cancelled`, or `flow_finished`) |

`flow_finished` means the Play review API completed. Play does not reveal whether its card appeared
or a review was submitted. `still_missing` means a user returned from settings without granting
access; it does not imply an explicit denial. Challenge duration and incorrect answer count persist
across process recreation for active alarm occurrences.

To inspect custom events while developing, enable Firebase Analytics DebugView for the app on a
test device. Event parameters that need reporting in standard Firebase reports must be registered
as custom definitions in Firebase. Keep parameter values bounded and non-identifying.
