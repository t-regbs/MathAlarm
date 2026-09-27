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
- Stop at least six consecutive deliveries without solving. Each Stop should schedule another ring 60 seconds later. Continue beyond ten minutes to check that elapsed time does not end recovery; then solve and confirm it stops.

## Prototype limits

Recovery is a separate fixed AlarmKit alarm, scheduled 60 seconds after the handled Stop/open action. It uses the alarm's saved tone and challenge settings. Recovery has no attempt cap or time expiry; it continues while the challenge is unresolved and can ring while it is still being worked on. Completion, an accepted snooze, disable, delete, or edit cancels recovery. iOS still requires unlocking before the app's maths screen can be used.

The prototype depends on iOS executing the background Stop intent while locked. Compile and unit-test success do not prove that behavior. If scheduling fails, the native intent throws and logs the failure; app-level error presentation remains a release task. This is an experiment, not a guarantee that system dismissal cannot bypass maths.

After testing, disable or delete the temporary alarm.

## Recorded result

27 September 2026: the user confirmed **“ok that works”** after receiving the main recovery test steps on the installed iPhone build. This is user-reported success; no per-step timing or agent-observed recording was supplied. The cancellation checks and terminated-app case above remain separate tests.

The confirmed build initially had a five-follow-up cap and ten-minute session expiry. The user subsequently chose continuous recovery until the challenge is solved; both limits were removed. Extended physical testing of the updated behavior remains pending.
