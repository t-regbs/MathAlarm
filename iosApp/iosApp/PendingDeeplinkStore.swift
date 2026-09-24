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
