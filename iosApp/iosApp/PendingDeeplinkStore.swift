import CryptoKit
import Foundation

/// A native delivery token is separate from the database's authoritative activeAt.
/// Weekly registration UUIDs repeat; date-specific tokens must not collapse next week's delivery.
enum NativeAlarmDeliveryIdentity {
    /// The shared application has already validated whether an unresolved repeating
    /// occurrence should own this delivery. This adapter preserves raw identity on nil.
    static func selectedOccurrence(deliveredAt: Int64?, authoritativeUnresolvedAt: Int64?) -> Int64? {
        authoritativeUnresolvedAt ?? deliveredAt
    }

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
    private let persist: ((Data) -> Bool)?

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

    init(userDefaults: UserDefaults = .standard, persist: ((Data) -> Bool)? = nil) {
        self.userDefaults = userDefaults
        self.persist = persist
    }

    private func loadQueue() -> [Handoff] {
        userDefaults.data(forKey: queueKey)
            .flatMap { try? JSONDecoder().decode([Handoff].self, from: $0) } ?? []
    }

    private func saveQueue(_ handoffs: [Handoff]) -> Bool {
        do {
            let encoded = try JSONEncoder().encode(handoffs)
            if let persist { return persist(encoded) }
            let previous = userDefaults.data(forKey: queueKey)
            userDefaults.set(encoded, forKey: queueKey)
            if userDefaults.synchronize() && userDefaults.data(forKey: queueKey) == encoded { return true }
            // Failed persistence must not advance the in-process head either.
            userDefaults.set(previous, forKey: queueKey)
            userDefaults.synchronize()
            return false
        } catch {
            print("Could not persist alarm handoff: \(error)")
            return false
        }
    }

    @discardableResult
    func setPendingDeeplink(_ json: String) -> Bool {
        lock.lock()
        defer { lock.unlock() }
        var handoffs = loadQueue()
        guard !handoffs.contains(where: { $0.payload == json }) else { return true }
        handoffs.append(Handoff(id: UUID(), payload: json))
        return saveQueue(handoffs)
    }

    /// Restored acknowledged occurrences take priority while existing queue order stays intact.
    /// Their authoritative activeAt makes restoration idempotent across process launches.
    @discardableResult
    func restoreUnresolvedHandoffs(_ payloads: [String]) -> Bool {
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
        return saveQueue(restored + queued.filter { queued in
            // Coalesced weekly alerts can share activeAt while retaining distinct
            // delivery IDs. Only the selected/enriched queue UUID moved to the prefix;
            // every later delivery stays queued until its own readiness acknowledgement.
            !restored.contains(where: { $0.id == queued.id })
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
        return saveQueue(handoffs)
    }

    /// Authoritative coalescing can enrich the exact head while preserving its UUID
    /// and all later queue entries. Replacement is neither readiness nor acknowledgement.
    func replacePendingHead(expectedPayload: String, replacement: String) -> Bool {
        lock.lock()
        defer { lock.unlock() }
        var handoffs = loadQueue()
        guard let head = handoffs.first, head.payload == expectedPayload else { return false }
        if expectedPayload == replacement { return true }
        handoffs[0] = Handoff(id: head.id, payload: replacement)
        return saveQueue(handoffs)
    }

    /// Removal is permitted only after authoritative shared validation declares
    /// this exact head obsolete. Failed or incomplete readiness is never rejection.
    func rejectObsoletePendingDeeplink(_ json: String) -> Bool {
        acknowledgePendingDeeplink(json)
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
        // Old tokens predate registration acknowledgement and decode as accepted.
        // A new reservation survives a crash before native acceptance and can be repaired.
        var registrationAccepted: Bool
        let activeAtMilliseconds: Int64?

        init(id: UUID, attempt: Int, startedAt: Date, registrationAccepted: Bool = true,
             activeAtMilliseconds: Int64? = nil) {
            self.id = id
            self.attempt = attempt
            self.startedAt = startedAt
            self.registrationAccepted = registrationAccepted
            self.activeAtMilliseconds = activeAtMilliseconds
        }
        private enum CodingKeys: String, CodingKey { case id, attempt, startedAt, registrationAccepted, activeAtMilliseconds }
        init(from decoder: Decoder) throws {
            let values = try decoder.container(keyedBy: CodingKeys.self)
            id = try values.decode(UUID.self, forKey: .id)
            attempt = try values.decode(Int.self, forKey: .attempt)
            startedAt = try values.decode(Date.self, forKey: .startedAt)
            registrationAccepted = try values.decodeIfPresent(Bool.self, forKey: .registrationAccepted) ?? true
            activeAtMilliseconds = try values.decodeIfPresent(Int64.self, forKey: .activeAtMilliseconds)
        }

        func matches(_ other: Session) -> Bool {
            id == other.id && attempt == other.attempt && startedAt == other.startedAt
        }
    }
    private let defaults: UserDefaults
    private let key = "MathAlarm.recoverySessions.v1"
    private let lock = NSLock()
    private let persist: ((Data) -> Bool)?
    init(userDefaults: UserDefaults = .standard, persist: ((Data) -> Bool)? = nil) {
        defaults = userDefaults
        self.persist = persist
    }
    private func load() -> [String: Session] {
        defaults.data(forKey: key).flatMap {
            try? JSONDecoder().decode([String: Session].self, from: $0)
        } ?? [:]
    }
    private func save(_ sessions: [String: Session]) -> Bool {
        guard let encoded = try? JSONEncoder().encode(sessions) else { return false }
        if let persist { return persist(encoded) }
        let previous = defaults.data(forKey: key)
        defaults.set(encoded, forKey: key)
        if defaults.synchronize() && defaults.data(forKey: key) == encoded { return true }
        defaults.set(previous, forKey: key)
        defaults.synchronize()
        return false
    }
    func reserve(alarmId: Int64, sourceSession: UUID?, sourceAttempt: Int?, now: Date = Date(),
                 activeAtMilliseconds: Int64? = nil) -> Session? {
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
            session = Session(id: UUID(), attempt: 0, startedAt: now, activeAtMilliseconds: activeAtMilliseconds)
        }
        session.attempt += 1
        session.registrationAccepted = false
        sessions[alarmKey] = session
        return save(sessions) ? session : nil
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
            sessions[alarmKey] = Session(id: sourceSession, attempt: sourceAttempt, startedAt: reservation.startedAt,
                                        activeAtMilliseconds: reservation.activeAtMilliseconds)
        } else if sourceSession == nil && reservation.attempt == 1 {
            sessions.removeValue(forKey: alarmKey)
        } else { return false }
        return save(sessions)
    }

    func isCurrent(alarmId: Int64, session: Session) -> Bool {
        lock.lock(); defer { lock.unlock() }
        return load()[String(alarmId)]?.matches(session) == true
    }
    func current(alarmId: Int64) -> Session? {
        lock.lock(); defer { lock.unlock() }
        return load()[String(alarmId)]
    }
    @discardableResult
    func markRegistrationAccepted(alarmId: Int64, reservation: Session) -> Bool {
        lock.lock(); defer { lock.unlock() }
        var sessions = load()
        guard var current = sessions[String(alarmId)], current.matches(reservation) else { return false }
        current.registrationAccepted = true
        sessions[String(alarmId)] = current
        return save(sessions)
    }
    /// Failed native cancellation leaves the unresolved token available for repair.
    /// A newer delivery/reservation always wins over this rollback.
    @discardableResult
    func restoreAfterFailedCancellation(alarmId: Int64, session: Session) -> Bool {
        lock.lock(); defer { lock.unlock() }
        var sessions = load()
        guard sessions[String(alarmId)] == nil else { return false }
        sessions[String(alarmId)] = session
        return save(sessions)
    }
    func accepts(alarmId: Int64, sessionId: UUID, attempt: Int?) -> Bool {
        lock.lock(); defer { lock.unlock() }
        let session = load()[String(alarmId)]
        return session?.id == sessionId && session?.attempt == attempt
    }
    @discardableResult
    func cancel(alarmId: Int64) -> Bool {
        lock.lock(); defer { lock.unlock() }
        var sessions = load()
        sessions.removeValue(forKey: String(alarmId))
        return save(sessions)
    }
}
