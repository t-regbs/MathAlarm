import CryptoKit
import Foundation

/// A native delivery token is separate from the database's authoritative activeAt.
/// Weekly registration UUIDs repeat; date-specific tokens must not collapse next week's delivery.
enum NativeAlarmDeliveryIdentity {
    static func identifier(registrationID: String, scheduledAtMilliseconds: Int64?,
                           occurrenceKey: String?, hour: Int, minute: Int,
                           now: Date = Date(), calendar: Calendar = .current) -> String {
        let milliseconds = occurrenceMilliseconds(scheduledAtMilliseconds: scheduledAtMilliseconds,
            occurrenceKey: occurrenceKey, hour: hour, minute: minute, now: now, calendar: calendar)
        let bytes = Array(SHA256.hash(data: Data("mathalarm/delivery/\(registrationID)/\(milliseconds)".utf8)))
        return UUID(uuid: (bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7],
                           bytes[8], bytes[9], bytes[10], bytes[11], bytes[12], bytes[13], bytes[14], bytes[15])).uuidString
    }

    static func occurrenceMilliseconds(scheduledAtMilliseconds: Int64?, occurrenceKey: String?,
                                       hour: Int, minute: Int, now: Date = Date(),
                                       calendar: Calendar = .current) -> Int64 {
        let deliveredAt: Date
        if let scheduledAtMilliseconds {
            deliveredAt = Date(timeIntervalSince1970: Double(scheduledAtMilliseconds) / 1000)
        } else {
            var components = DateComponents(hour: hour, minute: minute, second: 0)
            if let occurrenceKey, occurrenceKey.hasPrefix("day_"),
               let day = Int(occurrenceKey.dropFirst(4)), (0...6).contains(day) {
                components.weekday = day + 1
            }
            let latest = calendar.nextDate(after: now.addingTimeInterval(1), matching: components,
                                           matchingPolicy: .nextTimePreservingSmallerComponents,
                                           repeatedTimePolicy: .first, direction: .backward) ?? now
            // Backward search selects the later overlap on Foundation. Resolve forward
            // from that local day's start to match Kotlin's earlier-offset occurrence.
            deliveredAt = calendar.nextDate(after: calendar.startOfDay(for: latest).addingTimeInterval(-1),
                                            matching: components, matchingPolicy: .nextTimePreservingSmallerComponents,
                                            repeatedTimePolicy: .first, direction: .forward) ?? latest
        }
        let milliseconds = Int64((deliveredAt.timeIntervalSince1970 * 1000).rounded())
        return milliseconds
    }

}


/// Persists deliveries until the matching challenge is ready to handle them.
final class PendingDeeplinkStore {
    static let shared = PendingDeeplinkStore()

    private let userDefaults: UserDefaults
    private let queueKey = "MathAlarm.pendingAlarmHandoffs.v1"
    private let lock = NSLock()

    private struct Handoff: Codable {
        let id: UUID
        let payload: String
    }

    private struct Occurrence: Equatable {
        let alarmId: Int64
        let activeAt: Int64
    }

    private func payloadObject(_ payload: String) -> [String: Any]? {
        guard let data = payload.data(using: .utf8),
              let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return nil }
        return object
    }

    private func occurrence(_ payload: String) -> Occurrence? {
        guard let object = payloadObject(payload),
              let alarmId = object["alarmId"] as? NSNumber,
              let activeAt = object["activeAt"] as? NSNumber else { return nil }
        return Occurrence(alarmId: alarmId.int64Value, activeAt: activeAt.int64Value)
    }

    private func legacyAlarmId(_ payload: String) -> Int64? {
        guard let object = payloadObject(payload), let alarmId = object["alarmId"] as? NSNumber,
              object["activeAt"] == nil || object["activeAt"] is NSNull else { return nil }
        if let version = object["version"] as? NSNumber, version.intValue != 1 { return nil }
        return alarmId.int64Value
    }

    private func sameOccurrence(_ first: String, _ second: String) -> Bool {
        if first == second { return true }
        guard let first = occurrence(first), let second = occurrence(second) else { return false }
        return first == second
    }

    init(userDefaults: UserDefaults = .standard) {
        self.userDefaults = userDefaults
    }

    private func loadQueue() -> [Handoff] {
        userDefaults.data(forKey: queueKey)
            .flatMap { try? JSONDecoder().decode([Handoff].self, from: $0) } ?? []
    }

    private func saveQueue(_ handoffs: [Handoff]) {
        do {
            userDefaults.set(try JSONEncoder().encode(handoffs), forKey: queueKey)
            userDefaults.synchronize()
        } catch {
            assertionFailure("Could not persist alarm handoff: \(error)")
        }
    }

    func setPendingDeeplink(_ json: String) {
        lock.lock()
        defer { lock.unlock() }
        var handoffs = loadQueue()
        guard !handoffs.contains(where: { $0.payload == json }) else { return }
        handoffs.append(Handoff(id: UUID(), payload: json))
        saveQueue(handoffs)
    }

    /// Restored acknowledged occurrences take priority while existing queue order stays intact.
    /// Their authoritative activeAt makes restoration idempotent across process launches.
    func restoreUnresolvedHandoffs(_ payloads: [String]) {
        lock.lock()
        defer { lock.unlock() }
        let queued = loadQueue()
        var restored: [Handoff] = []
        for payload in payloads where !restored.contains(where: { sameOccurrence($0.payload, payload) }) {
            // Keep the queued native token and queue UUID when the durable occurrence
            // was already delivered. A legacy restoration must not duplicate its v2 delivery.
            if let delivered = queued.first(where: { sameOccurrence($0.payload, payload) }) {
                restored.append(delivered)
            } else if let known = occurrence(payload),
                      let legacy = queued.first(where: { legacyAlarmId($0.payload) == known.alarmId }) {
                // Pre-v2 queues coalesced alarmId-only payloads. Authoritative restoration
                // enriches that record in place, retaining its persisted queue identity.
                restored.append(Handoff(id: legacy.id, payload: payload))
            } else {
                restored.append(Handoff(id: UUID(), payload: payload))
            }
        }
        saveQueue(restored + queued.filter { queued in
            !restored.contains(where: {
                $0.id == queued.id || sameOccurrence($0.payload, queued.payload) ||
                    (occurrence($0.payload)?.alarmId == legacyAlarmId(queued.payload) && legacyAlarmId(queued.payload) != nil)
            })
        })
    }

    func peekPendingDeeplink() -> String? {
        lock.lock()
        defer { lock.unlock() }
        return loadQueue().first?.payload
    }

    /// Acknowledge only the item currently presented to navigation.
    func acknowledgePendingDeeplink(_ json: String) -> Bool {
        lock.lock()
        defer { lock.unlock() }
        var handoffs = loadQueue()
        guard handoffs.first?.payload == json else { return false }
        handoffs.removeFirst()
        saveQueue(handoffs)
        return true
    }

    func hasPendingDeeplink() -> Bool {
        peekPendingDeeplink() != nil
    }
}

/// Recovery belongs to an unresolved challenge, not to its weekly registration.
/// Tokens prevent an in-flight schedule from surviving completion/cancellation.
final class AlarmRecoveryStore {
    static let shared = AlarmRecoveryStore()
    struct Session: Codable, Equatable {
        let id: UUID
        var attempt: Int
        let startedAt: Date
    }
    private let defaults: UserDefaults
    private let key = "MathAlarm.recoverySessions.v1"
    private let lock = NSLock()
    init(userDefaults: UserDefaults = .standard) { defaults = userDefaults }
    private func load() -> [String: Session] {
        defaults.data(forKey: key).flatMap {
            try? JSONDecoder().decode([String: Session].self, from: $0)
        } ?? [:]
    }
    private func save(_ sessions: [String: Session]) {
        defaults.set(try? JSONEncoder().encode(sessions), forKey: key)
        defaults.synchronize()
    }
    func reserve(alarmId: Int64, sourceSession: UUID?, sourceAttempt: Int?, now: Date = Date()) -> Session? {
        lock.lock(); defer { lock.unlock() }
        var sessions = load()
        let alarmKey = String(alarmId)
        var session: Session
        if let sourceSession {
            guard let existing = sessions[alarmKey], existing.id == sourceSession,
                  existing.attempt == sourceAttempt else { return nil }
            session = existing
        } else {
            // The intent and app activation can see the same initial delivery.
            guard sessions[alarmKey] == nil else { return nil }
            session = Session(id: UUID(), attempt: 0, startedAt: now)
        }
        session.attempt += 1
        sessions[alarmKey] = session
        save(sessions)
        return session
    }
    /// A rejected native registration must not consume the retry token. A newer accepted
    /// attempt or explicit cancellation always wins over this rollback.
    @discardableResult
    func rollbackReservation(alarmId: Int64, reservation: Session,
                             sourceSession: UUID?, sourceAttempt: Int?) -> Bool {
        lock.lock(); defer { lock.unlock() }
        var sessions = load()
        let alarmKey = String(alarmId)
        guard sessions[alarmKey] == reservation else { return false }
        if let sourceSession, let sourceAttempt,
           sourceSession == reservation.id, sourceAttempt == reservation.attempt - 1 {
            sessions[alarmKey] = Session(id: sourceSession, attempt: sourceAttempt, startedAt: reservation.startedAt)
        } else if sourceSession == nil && reservation.attempt == 1 {
            sessions.removeValue(forKey: alarmKey)
        } else { return false }
        save(sessions)
        return true
    }

    func isCurrent(alarmId: Int64, session: Session) -> Bool {
        lock.lock(); defer { lock.unlock() }
        return load()[String(alarmId)] == session
    }
    func accepts(alarmId: Int64, sessionId: UUID, attempt: Int?) -> Bool {
        lock.lock(); defer { lock.unlock() }
        let session = load()[String(alarmId)]
        return session?.id == sessionId && session?.attempt == attempt
    }
    func cancel(alarmId: Int64) {
        lock.lock(); defer { lock.unlock() }
        var sessions = load()
        sessions.removeValue(forKey: String(alarmId))
        save(sessions)
    }
}
