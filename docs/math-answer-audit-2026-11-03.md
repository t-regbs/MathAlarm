# Math-answer audit — 3 November 2026

Investigated Play Store feedback that correct answers appeared to be rejected.
The review text, displayed equations, entered answers, and installed versions
were not available, so this audit cannot establish the cause of those reports.
The initial audit left production behavior unchanged. The follow-up fixes and
regression coverage are recorded below.

## Checks that passed

- Independently parsed and evaluated 139,000 displayed equations across all three
  presets, every non-empty custom operator subset, all custom operand-range
  combinations, and a mixed-difficulty challenge. Every stored answer matched.
- All generated answers were non-negative, fit the Android field's eight-character
  limit, and were accepted by the integer parser. Division had a nonzero divisor
  and no remainder; arithmetic was independently evaluated with Long values.
- 65 shared tests passed, including generation, localized answer parsing,
  question progression, restored progress, occurrence isolation, and stale
  submission handling.
- Generation code is unchanged from repository commit `2e24db74` (September 2026).
  This does not prove the exact source/binary used by either reviewer.

## Reproduced on Android 15 emulator

`MathAnswerReviewTest` passed both characterization cases against unchanged
production math code. Evidence: `/tmp/mathalarm-math-ui-audit.txt`.

### Old incorrect feedback remains after a correct answer

1. Open a two-question preview.
2. Submit an incorrect answer and observe the error snackbar.
3. Replace it with the independently calculated correct answer and submit.
4. The challenge advances to question index 1, proving acceptance, but the
   previous "Incorrect answer. Try again." message is still displayed.

`MathScreen.kt` displays errors through a suspending `showSnackbar` call in its
result collector. Successful intermediate progression neither dismisses that
message nor associates it with the old question. This can misrepresent the
latest submission. It does not show that a first, plain-integer correct answer
is rejected by the checker.

Recommended correction: scope answer feedback to the submission/question and
clear obsolete feedback on editing or successful progression. Keep completion
and snooze handling independent of a snackbar's display lifetime. Add desired-
behavior regression assertions when implementing; the initial device tests
intentionally characterized the observed defects.

### Input filtering can change a mathematically equivalent answer

The Android input handler retains only digit characters. Pasting a positive
integer answer followed by `.0` therefore adds a zero to its value (e.g. `30.0`
becomes `300`). The checker rejects that changed value. The device test generated
an addition question, entered its exact answer followed by `.0`, and verified
both the changed field value and the incorrect-answer response.

Recommended correction: avoid silently changing numeric meaning. Decide whether
to accept integer-valued decimal input or explicitly reject unsupported syntax
while preserving intelligible feedback. The app intentionally generates only
whole-number answers; changing integer validation indiscriminately is not needed.
The same digit-filtering expression is present in the September source snapshot.

## Follow-up fixes

- Preserve entered text instead of removing non-digit characters. Accept exact
  integer-valued decimals such as `30.0` using string validation, with no floating
  point conversion or rounding. Nonzero fractions, malformed syntax, and integer
  overflow remain invalid. Increase the field limit to accommodate decimal input.
- Clear obsolete incorrect-answer results on editing and successful progression,
  while retaining unrelated failures and the incorrect-attempt count.
- Display snackbars in a child coroutine so state collection and completion are
  not blocked by their display lifetime. Dismiss obsolete answer feedback on
  editing, question changes, completion, and snooze.
- Replace the two device characterization cases with desired-behavior regressions
  and add a completion regression. Cover fractional rejection before accepting
  `.0`, and verify the entered text is preserved.
- Add parser edge cases and a coordinator regression confirming that editing
  removes answer feedback while retaining an unrelated audio error. Retain the
  139,000-equation independent arithmetic check; generation is unchanged.

Validation: shared Android host tests, Android app unit tests, Android APK/test
builds, iOS simulator shared compilation, and the native UI boundary check pass.
The final run passed all 207 shared tests, 131 Android unit tests, and all three
Android 15 emulator regression cases (`MathAnswerReviewTest`).

## Limits

No arithmetic mismatch or rejection of a normal correct integer answer was
found in the tested paths. This is evidence, not a proof that every device,
keyboard, stored state, or historical version is bug-free. Exact review examples
would be needed to attribute either reported incident.
