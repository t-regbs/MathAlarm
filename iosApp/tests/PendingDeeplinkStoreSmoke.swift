import Foundation

@main
enum PendingDeeplinkStoreSmoke {
    static func main() {
        let suite = "MathAlarm.HandoffSmoke.\(UUID().uuidString)"
        guard let defaults = UserDefaults(suiteName: suite) else {
            fatalError("Could not create isolated defaults suite")
        }
        defer { defaults.removePersistentDomain(forName: suite) }

        let first = "{\"alarmId\":1,\"title\":\"Wake \\\"up\\\"\\n☀️\"}"
        let second = "{\"alarmId\":2}"
        let store = PendingDeeplinkStore(userDefaults: defaults)
        store.setPendingDeeplink(first)
        store.setPendingDeeplink(second)
        store.setPendingDeeplink(first)

        let relaunched = PendingDeeplinkStore(userDefaults: defaults)
        precondition(relaunched.peekPendingDeeplink() == first)
        precondition(!relaunched.acknowledgePendingDeeplink(second))
        precondition(relaunched.peekPendingDeeplink() == first)
        precondition(relaunched.acknowledgePendingDeeplink(first))
        precondition(relaunched.peekPendingDeeplink() == second)
        precondition(relaunched.acknowledgePendingDeeplink(second))
        precondition(!relaunched.hasPendingDeeplink())

        defaults.set(first, forKey: "MathAlarm.pendingAlarmKitDeeplink")
        precondition(relaunched.peekPendingDeeplink() == first)
        precondition(relaunched.acknowledgePendingDeeplink(first))
        precondition(!relaunched.hasPendingDeeplink())
        print("PendingDeeplinkStore smoke test passed")

        let recovery = AlarmRecoveryStore(userDefaults: defaults)
        let initial = recovery.reserve(alarmId: 1, sourceSession: nil, sourceAttempt: nil)!
        precondition(initial.attempt == 1)
        precondition(recovery.reserve(alarmId: 1, sourceSession: nil, sourceAttempt: nil) == nil)
        let restoredRecovery = AlarmRecoveryStore(userDefaults: defaults)
        precondition(restoredRecovery.isCurrent(alarmId: 1, session: initial))
        precondition(restoredRecovery.accepts(alarmId: 1, sessionId: initial.id, attempt: 1))
        var current = initial
        for attempt in 2...5 {
            current = restoredRecovery.reserve(alarmId: 1, sourceSession: current.id,
                                               sourceAttempt: current.attempt)!
            precondition(current.attempt == attempt)
        }
        precondition(!restoredRecovery.isCurrent(alarmId: 1, session: initial))
        precondition(!restoredRecovery.accepts(alarmId: 1, sessionId: initial.id, attempt: 1))
        precondition(restoredRecovery.reserve(alarmId: 1, sourceSession: current.id,
                                             sourceAttempt: current.attempt) == nil)
        restoredRecovery.cancel(alarmId: 1)
        precondition(!restoredRecovery.isCurrent(alarmId: 1, session: current))
        precondition(!restoredRecovery.accepts(alarmId: 1, sessionId: current.id, attempt: current.attempt))
        precondition(restoredRecovery.reserve(alarmId: 1, sourceSession: current.id,
                                             sourceAttempt: current.attempt) == nil)
        let next = restoredRecovery.reserve(alarmId: 1, sourceSession: nil, sourceAttempt: nil)!
        precondition(next.id != initial.id)
        _ = restoredRecovery.reserve(alarmId: 2, sourceSession: nil, sourceAttempt: nil)
        restoredRecovery.cancel(alarmId: 1)
        precondition(restoredRecovery.reserve(alarmId: 2, sourceSession: nil, sourceAttempt: nil) == nil)
        restoredRecovery.cancelAll()
        precondition(restoredRecovery.reserve(alarmId: 2, sourceSession: nil, sourceAttempt: nil) != nil)
        let later = Date().addingTimeInterval(601)
        precondition(restoredRecovery.reserve(alarmId: 1, sourceSession: next.id,
                                             sourceAttempt: next.attempt, now: later) == nil)
        precondition(restoredRecovery.reserve(alarmId: 2, sourceSession: nil,
                                             sourceAttempt: nil, now: later) != nil)
        print("AlarmRecoveryStore smoke test passed")
    }
}
