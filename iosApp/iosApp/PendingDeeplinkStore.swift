import Foundation

/// Persists deliveries until the matching challenge is ready to handle them.
final class PendingDeeplinkStore {
    static let shared = PendingDeeplinkStore()

    private let userDefaults: UserDefaults
    private let pendingDeeplinkKey = "MathAlarm.pendingAlarmKitDeeplink"
    private let queueKey = "MathAlarm.pendingAlarmHandoffs.v1"
    private let lock = NSLock()

    private struct Handoff: Codable {
        let id: UUID
        let payload: String
    }

    init(userDefaults: UserDefaults = .standard) {
        self.userDefaults = userDefaults
    }

    private func loadQueue() -> [Handoff] {
        let saved = userDefaults.data(forKey: queueKey)
            .flatMap { try? JSONDecoder().decode([Handoff].self, from: $0) } ?? []
        if let legacy = userDefaults.string(forKey: pendingDeeplinkKey),
           !saved.contains(where: { $0.payload == legacy }) {
            return [Handoff(id: UUID(), payload: legacy)] + saved
        }
        return saved
    }

    private func saveQueue(_ handoffs: [Handoff]) {
        do {
            userDefaults.set(try JSONEncoder().encode(handoffs), forKey: queueKey)
            userDefaults.removeObject(forKey: pendingDeeplinkKey)
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
    func cancelAll() {
        lock.lock(); defer { lock.unlock() }
        save([:])
    }
}
