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
    }
}
