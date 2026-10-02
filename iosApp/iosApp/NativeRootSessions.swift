import SwiftUI
import KMPObservableViewModelSwiftUI
import app

extension Notification.Name {
    static let mathAlarmPendingDelivery = Notification.Name("MathAlarm.nativePendingDelivery")
}

struct NativeEditorSession: Identifiable {
    let id: String
    let alarmID: Int64?
    let model: AlarmSettingsViewModel
}

struct NativeChallengeSession: Identifiable {
    let id: String
    let payload: String
    let model: AlarmMathViewModel
}

/// Stable session IDs and factory references live above split/stack/detail branches.
/// No view disappearance or observation cancellation accepts an alarm command.
@MainActor
final class NativeWindowSessions: ObservableObject {
    private let pendingStore: PendingDeeplinkStore
    private let windowID = UUID().uuidString
    @Published private(set) var editors: [NativeEditorSession] = []
    @Published private(set) var editorPaths: [String: [NativeEditorDestination]] = [:]
    @Published private(set) var selectedEditorID: String?
    private var navigationObservers: [String: Int] = [:]
    private var latestNavigationObservers: [String: Int] = [:]
    @Published private(set) var challenge: NativeChallengeSession?
    @Published private(set) var challenges: [NativeChallengeSession] = []
    @Published private(set) var pendingPayload: String?
    @Published var deliveryPresented = false

    var selectedEditor: NativeEditorSession? { editors.first { $0.id == selectedEditorID } }

    init(pendingStore: PendingDeeplinkStore = .shared) {
        self.pendingStore = pendingStore
    }

    @discardableResult
    func openEditor(alarm: Alarm?) -> NativeEditorSession {
        // Re-selecting an existing row (or Add) restores its draft, never reinitializes it.
        if let existing = editors.first(where: { $0.alarmID == alarm?.alarmId }) {
            selectEditor(id: existing.id)
            return existing
        }
        let id = "native-editor/\(UUID().uuidString)"
        let model: AlarmSettingsViewModel
        if let alarm { model = SharedFeatures.shared.editor(sessionId: id, alarm: alarm) }
        else { model = SharedFeatures.shared.doNewEditor(sessionId: id) }
        let session = NativeEditorSession(id: id, alarmID: alarm?.alarmId, model: model)
        editors.append(session)
        editorPaths[id] = []
        selectEditor(id: id)
        return session
    }

    func selectEditor(id: String) {
        guard editors.contains(where: { $0.id == id }) else { return }
        if selectedEditorID != id {
            if let previous = selectedEditorID { navigationObservers.removeValue(forKey: previous) }
            selectedEditorID = id
        }
    }

    func beginNavigation(id: String, observer: Int) {
        guard selectedEditorID == id, observer >= (latestNavigationObservers[id] ?? 0) else { return }
        latestNavigationObservers[id] = observer
        navigationObservers[id] = observer
    }

    func ownsNavigation(id: String, observer: Int) -> Bool {
        selectedEditorID == id && navigationObservers[id] == observer
    }

    func endNavigation(id: String, observer: Int) {
        if navigationObservers[id] == observer { navigationObservers.removeValue(forKey: id) }
    }

    func setEditorPath(_ path: [NativeEditorDestination], id: String, observer: Int? = nil) {
        // SwiftUI can write an outgoing stack binding during detail teardown.
        // An old observer must not clear the retained path of an inactive draft.
        guard selectedEditorID == id, editors.contains(where: { $0.id == id }) else { return }
        if let observer, !ownsNavigation(id: id, observer: observer) { return }
        editorPaths[id] = path
    }

    /// Only explicit discard (or a future accepted save) ends a retained draft.
    func closeEditor(id: String) {
        guard editors.contains(where: { $0.id == id }) else { return }
        SharedFeatures.shared.closeEditor(sessionId: id)
        editors.removeAll { $0.id == id }
        editorPaths.removeValue(forKey: id)
        latestNavigationObservers.removeValue(forKey: id)
        navigationObservers.removeValue(forKey: id)
        if selectedEditorID == id { selectedEditorID = nil }
    }

    func refreshPendingDelivery() {
        guard let payload = pendingStore.peekPendingDeeplink() else { return }
        // An initialized unresolved challenge may not be replaced by a later handoff.
        if let challenge, challenge.model.state.readiness == .ready { return }
        // Current real delivery has priority. Later queued items remain in persisted order.
        guard pendingPayload != payload else { return }
        pendingPayload = payload
        if let decoded = IosApplication.shared.decodeAlarmHandoffJson(payload: payload) {
            let occurrence = decoded.activeAt.map { String($0.int64Value) } ?? decoded.deliveryId ?? "legacy"
            let id = "native-window/\(windowID)/occurrence/\(decoded.alarmId)/\(occurrence)"
            if let retained = challenges.first(where: { $0.id == id }) { challenge = retained }
            else {
                let retained = NativeChallengeSession(id: id, payload: payload,
                    model: SharedFeatures.shared.challenge(sessionId: id))
                challenges.append(retained)
                challenge = retained
            }
        } else {
            challenge = nil
        }
        // M3 does not initialize/consume/acknowledge this delivery. M5 must await
        // successful native challenge readiness before acknowledging the queue.
        deliveryPresented = true
    }

    isolated deinit {
        // The actual window lifetime ends here. Closing observations cannot resolve a
        // durable occurrence, cancel application commands or silence application audio.
        let editorIDs = editors.map(\.id)
        let challengeIDs = challenges.map(\.id)
        DispatchQueue.main.async {
            editorIDs.forEach { SharedFeatures.shared.closeEditor(sessionId: $0) }
            challengeIDs.forEach { SharedFeatures.shared.closeChallenge(sessionId: $0) }
        }
    }
}

/// This owner layer stays mounted while details/navigation destinations change.
/// Children use @ObservedViewModel; the owners alone use @StateViewModel.
@MainActor
struct NativeSessionOwners: View {
    @ObservedObject var sessions: NativeWindowSessions
    var body: some View {
        ZStack {
            ForEach(sessions.editors) { session in
                NativeEditorOwner(model: session.model).id(session.id)
            }
            ForEach(sessions.challenges) { session in
                NativeChallengeOwner(model: session.model).id(session.id)
            }
        }
        .frame(width: 0, height: 0)
        .accessibilityHidden(true)
        .allowsHitTesting(false)
    }
}

@MainActor
struct NativeEditorOwner: View {
    @StateViewModel var model: AlarmSettingsViewModel
    var body: some View {
        // Read the wrapper so its lazily retained ObservableViewModel is installed.
        let _ = model
        Color.clear
    }
}

@MainActor
struct NativeChallengeOwner: View {
    @StateViewModel var model: AlarmMathViewModel
    var body: some View {
        let _ = model
        Color.clear
    }
}
