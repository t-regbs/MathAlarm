# Native UI migration progress — 2 October 2026

The [approved plan](native-ui-migration-plan.md) is the architectural authority. Milestone 1 establishes the production baseline and does not claim completed native UI or release readiness.

| Milestone | Status | Evidence |
| --- | --- | --- |
| 1 — Establish the baseline | **COMPLETE** | [Inventory, parity matrix, persisted compatibility, supported toolchain and baseline results](native-ui-migration-baseline-2026-10-02.md) |
| 2 — Independent shared presentation and operations | Not started | Next implementation scope |
| 3 — Extract Android UI and replace iOS root | Not started | Follows the shared-contract gate |

The production baseline passes **417 Android + 339 iOS** cases, two Python checks and the native queue/recovery smoke. The initial core native orchestration was interrupted; a full rerun of the unchanged original binary passed all 134 tests. No application assertion failure was reproduced. Historical physical-device evidence and unresolved release gates remain separate from simulator/host results.

The focused audit identifies initialization failure/retry, distinct recurring delivery identity, queue replay and unresolved restoration, accepted-command lifetime, progress failure/retry, audio arbitration and native owner cancellation as required regression coverage in Milestone 2. The [results-only interop archive](research/native-ui-interop-2026-10-02/README.md) supports the pinned bridge choice; it is not production feature verification.

Next: introduce UI-free feature state/actions and application-owned accepted work, adapt existing Compose consumers, add those regressions, wire the native bootstrap and verify actual production Swift observation/cancellation/owner cleanup before extracting rendering. Preserve all database names/schema migrations, preference keys, announcement/sound IDs, registration identities and legacy payload/progress compatibility.
