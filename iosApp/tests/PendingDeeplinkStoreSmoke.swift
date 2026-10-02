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

        // Legacy v1 JSON remains in place and ordered ahead of distinct v2 deliveries.
        let recurringFirst = "{\"alarmId\":1,\"version\":2,\"deliveryId\":\"first\"}"
        let recurringNext = "{\"alarmId\":1,\"version\":2,\"deliveryId\":\"next\"}"
        store.setPendingDeeplink(first)
        store.setPendingDeeplink(recurringFirst)
        store.setPendingDeeplink(recurringNext)
        store.setPendingDeeplink(recurringFirst)
        let upgraded = PendingDeeplinkStore(userDefaults: defaults)
        for payload in [first, recurringFirst, recurringNext] {
            precondition(upgraded.peekPendingDeeplink() == payload)
            precondition(upgraded.acknowledgePendingDeeplink(payload))
        }
        precondition(!upgraded.hasPendingDeeplink())
        // Restore an already-acknowledged unresolved occurrence before a later delivery.
        let unresolved = "{\"alarmId\":1,\"activeAt\":1000}"
        store.setPendingDeeplink(recurringNext)
        store.restoreUnresolvedHandoffs([unresolved, unresolved])
        let restoredQueue = PendingDeeplinkStore(userDefaults: defaults)
        restoredQueue.restoreUnresolvedHandoffs([unresolved])
        precondition(restoredQueue.peekPendingDeeplink() == unresolved)
        precondition(restoredQueue.acknowledgePendingDeeplink(unresolved))
        precondition(restoredQueue.peekPendingDeeplink() == recurringNext)
        precondition(restoredQueue.acknowledgePendingDeeplink(recurringNext))
        precondition(!restoredQueue.hasPendingDeeplink())
        let alreadyDelivered = "{\"alarmId\":1,\"activeAt\":1000,\"version\":2,\"deliveryId\":\"first-delivery\"}"
        let laterOccurrence = "{\"alarmId\":1,\"activeAt\":2000,\"version\":2,\"deliveryId\":\"later-delivery\"}"
        store.setPendingDeeplink(alreadyDelivered)
        store.setPendingDeeplink(laterOccurrence)
        let queueBeforeRestoration = try! JSONSerialization.jsonObject(with: defaults.data(forKey: "MathAlarm.pendingAlarmHandoffs.v1")!) as! [[String: String]]
        store.restoreUnresolvedHandoffs([unresolved])
        let queueAfterRestoration = try! JSONSerialization.jsonObject(with: defaults.data(forKey: "MathAlarm.pendingAlarmHandoffs.v1")!) as! [[String: String]]
        precondition(queueAfterRestoration == queueBeforeRestoration)
        let semanticRestored = PendingDeeplinkStore(userDefaults: defaults)
        semanticRestored.restoreUnresolvedHandoffs([unresolved])
        precondition(semanticRestored.peekPendingDeeplink() == alreadyDelivered)
        precondition(semanticRestored.acknowledgePendingDeeplink(alreadyDelivered))
        precondition(semanticRestored.peekPendingDeeplink() == laterOccurrence)
        precondition(semanticRestored.acknowledgePendingDeeplink(laterOccurrence))
        precondition(!semanticRestored.hasPendingDeeplink())
        store.setPendingDeeplink(first)
        store.setPendingDeeplink(laterOccurrence)
        let legacyQueue = try! JSONSerialization.jsonObject(with: defaults.data(forKey: "MathAlarm.pendingAlarmHandoffs.v1")!) as! [[String: String]]
        store.restoreUnresolvedHandoffs([unresolved])
        let enrichedQueue = try! JSONSerialization.jsonObject(with: defaults.data(forKey: "MathAlarm.pendingAlarmHandoffs.v1")!) as! [[String: String]]
        precondition(enrichedQueue.count == 2)
        precondition(enrichedQueue[0]["id"] == legacyQueue[0]["id"])
        precondition(enrichedQueue[0]["payload"] == unresolved)
        precondition(enrichedQueue[1] == legacyQueue[1])
        precondition(store.acknowledgePendingDeeplink(unresolved))
        precondition(store.peekPendingDeeplink() == laterOccurrence)
        precondition(store.acknowledgePendingDeeplink(laterOccurrence))
        let unknownVersion2 = "{\"alarmId\":1,\"version\":2,\"deliveryId\":\"unknown-delivery\"}"
        store.setPendingDeeplink(unknownVersion2)
        store.restoreUnresolvedHandoffs([unresolved])
        precondition(store.acknowledgePendingDeeplink(unresolved))
        precondition(store.peekPendingDeeplink() == unknownVersion2)
        precondition(store.acknowledgePendingDeeplink(unknownVersion2))
        print("PendingDeeplinkStore smoke test passed")

        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(secondsFromGMT: 0)!
        let sunday = calendar.date(from: DateComponents(year: 2030, month: 1, day: 6, hour: 7, minute: 1))!
        let initialIdentity = NativeAlarmDeliveryIdentity.identifier(
            registrationID: "weekly-sunday", scheduledAtMilliseconds: nil, occurrenceKey: "day_0",
            hour: 7, minute: 0, now: sunday, calendar: calendar)
        let duplicateIdentity = NativeAlarmDeliveryIdentity.identifier(
            registrationID: "weekly-sunday", scheduledAtMilliseconds: nil, occurrenceKey: "day_0",
            hour: 7, minute: 0, now: sunday.addingTimeInterval(60), calendar: calendar)
        let laterIdentity = NativeAlarmDeliveryIdentity.identifier(
            registrationID: "weekly-sunday", scheduledAtMilliseconds: nil, occurrenceKey: "day_0",
            hour: 7, minute: 0, now: sunday.addingTimeInterval(7 * 24 * 60 * 60), calendar: calendar)
        precondition(UUID(uuidString: initialIdentity) != nil)
        precondition(initialIdentity == duplicateIdentity)
        precondition(initialIdentity != laterIdentity)
        let fixedIdentity = NativeAlarmDeliveryIdentity.identifier(
            registrationID: "snooze", scheduledAtMilliseconds: 1000, occurrenceKey: "snooze",
            hour: 7, minute: 0, now: sunday, calendar: calendar)
        precondition(fixedIdentity == NativeAlarmDeliveryIdentity.identifier(
            registrationID: "snooze", scheduledAtMilliseconds: 1000, occurrenceKey: "snooze",
            hour: 7, minute: 0, now: sunday.addingTimeInterval(86400), calendar: calendar))
        var london = calendar
        london.timeZone = TimeZone(identifier: "Europe/London")!
        let springAfter = calendar.date(from: DateComponents(year: 2030, month: 3, day: 31, hour: 2, minute: 31))!
        let springExpected = calendar.date(from: DateComponents(year: 2030, month: 3, day: 31, hour: 1, minute: 30))!
        let springMillis = NativeAlarmDeliveryIdentity.occurrenceMilliseconds(
            scheduledAtMilliseconds: nil, occurrenceKey: "day_0", hour: 1, minute: 30,
            now: springAfter, calendar: london)
        precondition(springMillis == Int64(springExpected.timeIntervalSince1970 * 1000))
        let autumnAfter = calendar.date(from: DateComponents(year: 2030, month: 10, day: 27, hour: 2, minute: 1))!
        let autumnExpected = calendar.date(from: DateComponents(year: 2030, month: 10, day: 27, hour: 0, minute: 30))!
        precondition(NativeAlarmDeliveryIdentity.occurrenceMilliseconds(
            scheduledAtMilliseconds: nil, occurrenceKey: "day_0", hour: 1, minute: 30,
            now: autumnAfter, calendar: london) == Int64(autumnExpected.timeIntervalSince1970 * 1000))
        print("Native delivery identity smoke test passed")

        let recovery = AlarmRecoveryStore(userDefaults: defaults)
        let initial = recovery.reserve(alarmId: 1, sourceSession: nil, sourceAttempt: nil)!
        precondition(initial.attempt == 1)
        precondition(recovery.reserve(alarmId: 1, sourceSession: nil, sourceAttempt: nil) == nil)
        let restoredRecovery = AlarmRecoveryStore(userDefaults: defaults)
        precondition(restoredRecovery.isCurrent(alarmId: 1, session: initial))
        precondition(restoredRecovery.accepts(alarmId: 1, sessionId: initial.id, attempt: 1))
        var current = initial
        for attempt in 2...100 {
            current = restoredRecovery.reserve(alarmId: 1, sourceSession: current.id,
                                               sourceAttempt: current.attempt)!
            precondition(current.attempt == attempt)
        }
        precondition(!restoredRecovery.isCurrent(alarmId: 1, session: initial))
        precondition(!restoredRecovery.accepts(alarmId: 1, sessionId: initial.id, attempt: 1))
        // Many attempts or elapsed time cannot dismiss an unresolved challenge.
        let tomorrow = Date().addingTimeInterval(24 * 60 * 60)
        current = AlarmRecoveryStore(userDefaults: defaults).reserve(
            alarmId: 1, sourceSession: current.id, sourceAttempt: current.attempt, now: tomorrow
        )!
        precondition(current.attempt == 101)
        precondition(restoredRecovery.reserve(alarmId: 1, sourceSession: nil,
                                             sourceAttempt: nil, now: tomorrow) == nil)
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
        restoredRecovery.cancel(alarmId: 2)
        precondition(restoredRecovery.reserve(alarmId: 2, sourceSession: nil, sourceAttempt: nil) != nil)
        let later = Date().addingTimeInterval(601)
        precondition(restoredRecovery.reserve(alarmId: 1, sourceSession: next.id,
                                             sourceAttempt: next.attempt, now: later) == nil)
        precondition(restoredRecovery.reserve(alarmId: 2, sourceSession: nil,
                                             sourceAttempt: nil, now: later) == nil)
        let failedInitial = recovery.reserve(alarmId: 3, sourceSession: nil, sourceAttempt: nil)!
        precondition(recovery.rollbackReservation(alarmId: 3, reservation: failedInitial,
                                                 sourceSession: nil, sourceAttempt: nil))
        let retriedInitial = recovery.reserve(alarmId: 3, sourceSession: nil, sourceAttempt: nil)!
        precondition(retriedInitial.id != failedInitial.id)
        let failedFollowup = recovery.reserve(alarmId: 3, sourceSession: retriedInitial.id,
                                              sourceAttempt: retriedInitial.attempt)!
        precondition(recovery.rollbackReservation(alarmId: 3, reservation: failedFollowup,
                                                 sourceSession: retriedInitial.id,
                                                 sourceAttempt: retriedInitial.attempt))
        precondition(recovery.accepts(alarmId: 3, sessionId: retriedInitial.id, attempt: retriedInitial.attempt))
        let retriedFollowup = recovery.reserve(alarmId: 3, sourceSession: retriedInitial.id,
                                               sourceAttempt: retriedInitial.attempt)!
        let laterAccepted = recovery.reserve(alarmId: 3, sourceSession: retriedFollowup.id,
                                             sourceAttempt: retriedFollowup.attempt)!
        precondition(!recovery.rollbackReservation(alarmId: 3, reservation: retriedFollowup,
                                                  sourceSession: retriedInitial.id,
                                                  sourceAttempt: retriedInitial.attempt))
        precondition(recovery.isCurrent(alarmId: 3, session: laterAccepted))
        recovery.cancel(alarmId: 3)
        precondition(!recovery.rollbackReservation(alarmId: 3, reservation: laterAccepted,
                                                  sourceSession: retriedFollowup.id,
                                                  sourceAttempt: retriedFollowup.attempt))
        precondition(!recovery.isCurrent(alarmId: 3, session: laterAccepted))
        print("AlarmRecoveryStore smoke test passed")
    }
}
