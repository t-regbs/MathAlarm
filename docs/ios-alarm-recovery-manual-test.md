# iOS lock-screen recovery prototype

Use a newly saved temporary alarm with an easy challenge, scheduled a few minutes ahead. Do not use Test Alarm: it does not exercise AlarmKit or recovery. Leave the app and lock the iPhone. Disconnect iPhone Mirroring while testing authentication.

## Main check

1. Let the alarm ring while locked.
2. Slide the system alarm control, then abandon or cancel unlocking. Leave the phone locked.
3. Wait 70 seconds. **Expected:** the alarm rings again, without first opening Math Alarm.
4. Unlock and solve the challenge. Wait another 70 seconds. **Expected:** no further recovery alarm.

Record whether the app still opens correctly after authenticating, whether the first recovery rings, and whether any error appears. If step 3 fails, the main bypass remains unresolved even if opening the app later starts its music.

## Cancellation checks

- With a recovery pending, successfully snooze. There should be no ring at the recovery's original one-minute deadline; the alarm should ring at the chosen snooze time.
- Repeat with a pending recovery, then disable or delete the temporary alarm. There should be no follow-up ring.
- Repeat with a pending recovery, then save an edit moving the alarm to a later time. The old recovery should not ring.
- Repeat with the app terminated before the initial alarm. This checks whether the background Stop intent can launch and schedule recovery without foreground activation.
- Give a wrong answer and leave the challenge unfinished. Recovery should remain armed. Complete it and confirm the next recovery is cancelled.

## Prototype limits

Recovery is a separate fixed AlarmKit alarm, scheduled 60 seconds after the handled Stop/open action. It uses the alarm's saved tone and challenge settings. Each session permits at most five follow-ups, and it can ring while the challenge is still being worked on. A later original delivery can start a new session after ten minutes.

The prototype depends on iOS executing the background Stop intent while locked. Compile and unit-test success do not prove that behavior. If scheduling fails, the native intent throws and logs the failure; app-level error presentation remains a release task. This is an experiment, not a guarantee that system dismissal cannot bypass maths.

After testing, disable or delete the temporary alarm.
