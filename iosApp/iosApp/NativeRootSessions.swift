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
    // Scheduling can fail after insertion; the retained draft then owns the allocated ID.
    var resolvedAlarmID: Int64? {
        guard let id = model.state.alarmId?.int64Value, id != 0 else { return alarmID }
        return id
    }
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
    private var windowEnded = false
    private let windowID = UUID().uuidString
    @Published private(set) var editors: [NativeEditorSession] = []
    @Published private(set) var editorPaths: [String: [NativeEditorDestination]] = [:]
    @Published private(set) var selectedEditorID: String?
    @Published private(set) var editorResultsRevision = 0
    @Published private(set) var permissionRequests: Set<String> = []
    private var soundSelections: [String: NativeSoundSelection] = [:]
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
        precondition(!windowEnded, "An ended native window cannot create editor sessions")
        // Re-selecting an existing row (or Add) restores its draft, never reinitializes it.
        if let existing = editors.first(where: { $0.alarmID == alarm?.alarmId ||
            (alarm?.alarmId != nil && alarm?.alarmId != 0 && $0.resolvedAlarmID == alarm?.alarmId) }) {
            selectEditor(id: existing.id)
            return existing
        }
        let id = "native-editor/\(UUID().uuidString)"
        let model: AlarmSettingsViewModel
        if let alarm { model = SharedFeatures.shared.editor(sessionId: id, alarm: alarm) }
        else { model = SharedFeatures.shared.doNewEditor(sessionId: id) }
        let session = NativeEditorSession(id: id, alarmID: alarm?.alarmId, model: model)
        soundSelections[id] = NativeSoundSelection(sessionID: id, currentTone: model.state.tone)
        editors.append(session)
        editorPaths[id] = []
        selectEditor(id: id)
        return session
    }

    func selectEditor(id: String) {
        guard !windowEnded else { return }
        guard editors.contains(where: { $0.id == id }) else { return }
        if selectedEditorID != id {
            if let previous = selectedEditorID { navigationObservers.removeValue(forKey: previous) }
            selectedEditorID = id
        }
    }

    func beginNavigation(id: String, observer: Int) {
        guard !windowEnded else { return }
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

    func soundSelection(sessionID: String) -> NativeSoundSelection? { soundSelections[sessionID] }

    func beginPermissionRequest(id: String) -> Bool {
        guard !windowEnded, editors.contains(where: { $0.id == id }), !permissionRequests.contains(id) else { return false }
        permissionRequests.insert(id)
        return true
    }

    func endPermissionRequest(id: String) { permissionRequests.remove(id) }

    func receiveEditorResults(id: String) {
        guard !windowEnded else { return }
        acceptSavedResult(id: id)
        editorResultsRevision += 1
    }

    /// Acknowledge while the owner is active, then end only this accepted editing session.
    func acceptSavedResult(id: String) {
        guard let editor = editors.first(where: { $0.id == id }),
              let result = editor.model.state.results.first(where: { $0.event is AlarmSettingsViewModel.UiEventSaveAlarm }) else { return }
        editor.model.acknowledgeResult(id: result.id)
        closeEditor(id: id, acceptedSave: true)
    }

    /// Explicit discard or authoritative accepted save ends a retained draft.
    func closeEditor(id: String, acceptedSave: Bool = false) {
        guard let editor = editors.first(where: { $0.id == id }),
              acceptedSave || !editor.model.state.isSaving else { return }
        permissionRequests.remove(id)
        soundSelections.removeValue(forKey: id)?.finish()
        SharedFeatures.shared.closeEditor(sessionId: id)
        editors.removeAll { $0.id == id }
        editorPaths.removeValue(forKey: id)
        latestNavigationObservers.removeValue(forKey: id)
        navigationObservers.removeValue(forKey: id)
        if selectedEditorID == id { selectedEditorID = nil }
    }

    /// Structural window-owner teardown removes factory keys even if native presentation
    /// caches still retain old child values. It never resolves an occurrence or cancels a command.
    func closeWindow() {
        guard !windowEnded else { return }
        windowEnded = true
        soundSelections.values.forEach { $0.finish() }
        editors.forEach { SharedFeatures.shared.closeEditor(sessionId: $0.id) }
        challenges.forEach { SharedFeatures.shared.closeChallenge(sessionId: $0.id) }
        soundSelections.removeAll()
        permissionRequests.removeAll()
        editors.removeAll()
        challenges.removeAll()
        challenge = nil
        editorPaths.removeAll()
        navigationObservers.removeAll()
        latestNavigationObservers.removeAll()
        selectedEditorID = nil
        deliveryPresented = false
    }

    func refreshPendingDelivery() {
        guard !windowEnded else { return }
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
        let sounds = Array(soundSelections.values)
        let challengeIDs = challenges.map(\.id)
        DispatchQueue.main.async {
            sounds.forEach { $0.finish() }
            editorIDs.forEach { SharedFeatures.shared.closeEditor(sessionId: $0) }
            challengeIDs.forEach { SharedFeatures.shared.closeChallenge(sessionId: $0) }
        }
    }
}

/// This StateObject belongs to the structural owner layer, not to any visible detail.
/// Native navigation/alert caches may outlive a host; factory cleanup cannot wait for them.
@MainActor
private final class NativeWindowLifetime: ObservableObject {
    private weak var sessions: NativeWindowSessions?
    init(sessions: NativeWindowSessions) { self.sessions = sessions }
    isolated deinit {
        guard let sessions else { return }
        DispatchQueue.main.async { sessions.closeWindow() }
    }
}

/// This owner layer stays mounted while details/navigation destinations change.
/// Children use @ObservedViewModel; the owners alone use @StateViewModel.
@MainActor
struct NativeSessionOwners: View {
    @ObservedObject var sessions: NativeWindowSessions
    @StateObject private var lifetime: NativeWindowLifetime
    init(sessions: NativeWindowSessions) {
        self.sessions = sessions
        _lifetime = StateObject(wrappedValue: NativeWindowLifetime(sessions: sessions))
    }
    var body: some View {
        let _ = lifetime
        ZStack {
            ForEach(sessions.editors) { session in
                NativeEditorOwner(model: session.model, sessionID: session.id, sessions: sessions).id(session.id)
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
    let sessionID: String
    @ObservedObject var sessions: NativeWindowSessions
    var body: some View {
        // Read the wrapper so its lazily retained ObservableViewModel is installed.
        let _ = model
        Color.clear
            .onAppear { sessions.receiveEditorResults(id: sessionID) }
            .onChange(of: model.state.results.map(\.id)) { sessions.receiveEditorResults(id: sessionID) }
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
