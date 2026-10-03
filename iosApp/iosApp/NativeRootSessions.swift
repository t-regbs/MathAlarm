import SwiftUI
import KMPObservableViewModelSwiftUI
import KMPObservableViewModelCore
import app
import KMPNativeCoroutinesAsync

extension Notification.Name {
    static let mathAlarmPendingDelivery = Notification.Name("MathAlarm.nativePendingDelivery")
}

struct NativeEditorSession: Identifiable {
    let id: String
    let alarmID: Int64?
    let model: AlarmSettingsViewModel
    // Keep the bridge cancellation lifetime with the retained session value too.
    // SwiftUI can construct an outgoing observer after its StateViewModel leaves
    // the hierarchy. The pinned bridge cannot recreate an expired cancellable.
    private let observationLifetime: ObservableViewModel<AlarmSettingsViewModel>
    init(id: String, alarmID: Int64?, model: AlarmSettingsViewModel) {
        self.id = id; self.alarmID = alarmID; self.model = model
        observationLifetime = observableViewModel(for: model)
    }
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
    var editorID: String? = nil
    var previewAlarm: Alarm? = nil
    var isPreview: Bool { editorID != nil }
    private let observationLifetime: ObservableViewModel<AlarmMathViewModel>
    init(id: String, payload: String, model: AlarmMathViewModel,
         editorID: String? = nil, previewAlarm: Alarm? = nil) {
        self.id = id; self.payload = payload; self.model = model
        self.editorID = editorID; self.previewAlarm = previewAlarm
        observationLifetime = observableViewModel(for: model)
    }
}

/// Stable session IDs and factory references live above split/stack/detail branches.
/// No view disappearance or observation cancellation accepts an alarm command.
@MainActor
final class NativeWindowSessions: ObservableObject {
    private let pendingStore: PendingDeeplinkStore
    private let restoreRecovery: ([String]) async -> Void
    private let inspectDelivery: (String) async throws -> AlarmHandoffDisposition
    private var windowEnded = false
    private let windowID = UUID().uuidString
    @Published private(set) var editors: [NativeEditorSession] = []
    @Published private(set) var editorPaths: [String: [NativeEditorDestination]] = [:]
    @Published private(set) var selectedEditorID: String?
    @Published private(set) var editorResultsRevision = 0
    @Published private(set) var permissionRequests: Set<String> = []
    private var initialPermissionResults: [String: Int64] = [:]
    private var alarmSettingsEditorID: String?
    private var alarmSettingsLeftApp = false
    let settingsGuide = NativeAlarmPermissionGuidePlayer()
    private var soundSelections: [String: NativeSoundSelection] = [:]
    private var navigationObservers: [String: Int] = [:]
    private var latestNavigationObservers: [String: Int] = [:]
    @Published private(set) var challenge: NativeChallengeSession?
    @Published private(set) var challenges: [NativeChallengeSession] = []
    @Published private(set) var pendingPayload: String?
    @Published var deliveryPresented = false {
        didSet { if deliveryPresented { settingsGuide.stop() } }
    }
    @Published private(set) var previews: [String: NativeChallengeSession] = [:]
    @Published private(set) var challengeFailures: [String: ChallengeResult] = [:]
    @Published private(set) var handoffWriteFailed = false
    private var initializationTasks: [String: Task<Void, Never>] = [:]
    private var replayTask: Task<Void, Never>?
    private var advanceAfterDismiss = false
    private var previewPresentations: [String: Int] = [:]
    private var nextPreviewPresentation = 0


    var selectedEditor: NativeEditorSession? { editors.first { $0.id == selectedEditorID } }
    var deliveryReplayInFlight: Bool { replayTask != nil }

    init(pendingStore: PendingDeeplinkStore = .shared,
         inspectDelivery: @escaping (String) async throws -> AlarmHandoffDisposition = { payload in
             try await asyncFunction(for: IosApplication.shared.handoffDisposition(payload: payload))
         }, restoreRecovery: @escaping ([String]) async -> Void = { payloads in
             await AlarmKitWrapperImpl.shared.restoreRecoveryForUnresolved(payloads: payloads)
         }) {
        self.pendingStore = pendingStore
        self.restoreRecovery = restoreRecovery
        self.inspectDelivery = inspectDelivery
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
            cancelAlarmSettingsSave()
            settingsGuide.stop()
            if let previous = selectedEditorID {
                navigationObservers.removeValue(forKey: previous)
                previewPresentations.removeValue(forKey: previous)
                previews[previous]?.model.stopPreview()
            }
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
        let leavingPreview = editorPaths[id]?.last == .preview && path.last != .preview
        editorPaths[id] = path
        if leavingPreview { closePreview(editorID: id, returning: false) }
        if path.last == .preview { openPreview(editorID: id) }
    }

    func soundSelection(sessionID: String) -> NativeSoundSelection? { soundSelections[sessionID] }

    func beginPermissionRequest(id: String) -> Bool {
        guard !windowEnded, editors.contains(where: { $0.id == id }), !permissionRequests.contains(id) else { return false }
        permissionRequests.insert(id)
        return true
    }

    func endPermissionRequest(id: String) { permissionRequests.remove(id) }

    /// The first Save asks iOS directly, once per retained permission result.
    /// A failed request remains available for an explicit retry or Settings guidance.
    @discardableResult
    func requestInitialAlarmPermission(id: String, resultID: Int64) -> Task<Void, Never>? {
        guard selectedEditorID == id, !deliveryPresented,
              AlarmSchedulerBridge.shared.authorizationStatus() == "notDetermined",
              initialPermissionResults[id] != resultID,
              let editor = editors.first(where: { $0.id == id }),
              editor.model.state.results.contains(where: {
                  $0.id == resultID && $0.event is AlarmSettingsViewModel.UiEventRequestExactAlarmPermission
              }) else { return nil }
        guard let request = requestAlarmPermission(id: id, retainingResultID: resultID) else { return nil }
        initialPermissionResults[id] = resultID
        return request
    }

    /// Resume this draft's save only after an affirmative authorization result.
    /// Denial/failure leaves it editable; it must not generate another save alert.
    @discardableResult
    func requestAlarmPermission(id: String, retainingResultID: Int64? = nil) -> Task<Void, Never>? {
        guard let editor = editors.first(where: { $0.id == id }),
              !editor.model.isClosed, beginPermissionRequest(id: id) else { return nil }
        return Task { @MainActor in
            defer { endPermissionRequest(id: id) }
            let authorized = await withCheckedContinuation { continuation in
                AlarmSchedulerBridge.shared.requestAuthorization { result in
                    continuation.resume(returning: result.boolValue)
                }
            }
            guard authorized, !editor.model.isClosed else { return }
            if let retainingResultID { editor.model.acknowledgeResult(id: retainingResultID) }
            editor.model.onEvent(event: AddEditAlarmEvent.OnSaveTodoClick.shared)
        }
    }

    /// Settings grants apply to the originating draft, never a different selection.
    @discardableResult
    func beginAlarmSettingsSave(id: String) -> Bool {
        guard !windowEnded, selectedEditorID == id, !deliveryPresented,
              let editor = editors.first(where: { $0.id == id }), !editor.model.isClosed else { return false }
        alarmSettingsEditorID = id
        alarmSettingsLeftApp = false
        return true
    }

    func cancelAlarmSettingsSave(id: String? = nil) {
        guard id == nil || alarmSettingsEditorID == id else { return }
        alarmSettingsEditorID = nil
        alarmSettingsLeftApp = false
    }

    func alarmSettingsSceneChanged(_ phase: ScenePhase) {
        guard alarmSettingsEditorID != nil else { return }
        if phase != .active { alarmSettingsLeftApp = true }
        else {
            if alarmSettingsLeftApp { settingsGuide.stop() }
            resumeAlarmSaveAfterSettings()
        }
    }

    func resumeAlarmSaveAfterSettings() {
        guard !windowEnded, alarmSettingsLeftApp, let id = alarmSettingsEditorID else { return }
        guard AlarmSchedulerBridge.shared.authorizationStatus() == "authorized" else {
            cancelAlarmSettingsSave(id: id)
            return
        }
        guard !deliveryPresented else { return }
        cancelAlarmSettingsSave(id: id)
        guard selectedEditorID == id, let editor = editors.first(where: { $0.id == id }),
              !editor.model.isClosed, !editor.model.state.isSaving else { return }
        editor.model.onEvent(event: AddEditAlarmEvent.OnSaveTodoClick.shared)
    }

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
        if selectedEditorID == id { settingsGuide.stop() }
        initialPermissionResults.removeValue(forKey: id)
        cancelAlarmSettingsSave(id: id)
        closePreview(editorID: id, returning: false)
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
        settingsGuide.stop()
        replayTask?.cancel()
        initializationTasks.values.forEach { $0.cancel() }
        initializationTasks.removeAll()
        previews.values.forEach { SharedFeatures.shared.closeChallenge(sessionId: $0.id) }
        previews.removeAll()
        challengeFailures.removeAll()
        soundSelections.values.forEach { $0.finish() }
        editors.forEach { SharedFeatures.shared.closeEditor(sessionId: $0.id) }
        challenges.forEach { SharedFeatures.shared.closeChallenge(sessionId: $0.id) }
        soundSelections.removeAll()
        permissionRequests.removeAll()
        initialPermissionResults.removeAll()
        cancelAlarmSettingsSave()
        editors.removeAll()
        challenges.removeAll()
        challenge = nil
        editorPaths.removeAll()
        navigationObservers.removeAll()
        latestNavigationObservers.removeAll()
        selectedEditorID = nil
        deliveryPresented = false
    }

    /// Replay is owned above navigation, never by a visible screen's task.
    func refreshPendingDelivery() {
        guard !windowEnded, replayTask == nil else { return }
        if let challenge, challenge.model.state.readiness == .ready || challenge.model.state.finishing {
            deliveryPresented = true
            guard !challenge.model.state.finishing else { return }
            replayTask = Task { @MainActor [weak self] in
                guard let self else { return }
                defer { self.replayTask = nil }
                while !self.windowEnded, !Task.isCancelled, !challenge.model.isClosed,
                      challenge.model.state.readiness == .ready, !challenge.model.state.finishing,
                      let payload = self.pendingStore.peekPendingDeeplink(),
                      let decoded = IosApplication.shared.decodeAlarmHandoffJson(payload: payload),
                      let alarm = challenge.model.state.alarm, alarm.alarmId == decoded.alarmId {
                    var associated = payload
                    if let deliveredAt = decoded.activeAt, let activeAt = alarm.activeAt,
                       deliveredAt.int64Value > activeAt.int64Value, let deliveryID = decoded.deliveryId {
                        do {
                            let selected = try await asyncFunction(for: IosApplication.shared.unresolvedOccurrenceForDelivery(
                                alarmId: alarm.alarmId, deliveredAt: deliveredAt))
                            guard !self.windowEnded, !Task.isCancelled, !challenge.model.isClosed,
                                  selected?.int64Value == activeAt.int64Value else { return }
                            associated = IosApplication.shared.createAlarmHandoffJson(alarmId: alarm.alarmId,
                                deliveryId: deliveryID, activeAt: selected)
                            guard self.pendingStore.replacePendingHead(expectedPayload: payload, replacement: associated) else {
                                self.handoffWriteFailed = true
                                return
                            }
                        } catch {
                            // Raw delivery remains queued; expose the failed association
                            // alongside the active challenge instead of silently hiding it.
                            self.handoffWriteFailed = true
                            return
                        }
                    }
                    guard self.acceptReadyDelivery(session: challenge, restoredPayload: associated) else { return }
                }
            }
            return
        }
        replayTask = Task { @MainActor [weak self] in
            guard let self else { return }
            defer { self.replayTask = nil }
            while !self.windowEnded, !Task.isCancelled, let payload = self.pendingStore.peekPendingDeeplink() {
                do {
                    let disposition = try await self.inspectDelivery(payload)
                    guard !self.windowEnded, !Task.isCancelled else { return }
                    if disposition == .obsolete {
                        guard self.pendingStore.rejectObsoletePendingDeeplink(payload) else {
                            self.handoffWriteFailed = true
                            self.pendingPayload = payload
                            self.deliveryPresented = true
                            return
                        }
                        if self.challenge?.payload == payload { self.retireUnreadyDelivery() }
                        self.pendingPayload = nil
                        IosApplication.shared.acknowledgeHandoff(payload: payload)
                        continue
                    }
                    self.presentDelivery(payload: payload)
                    return
                } catch {
                    // Authoritative loading failed. No readiness, rejection or acknowledgement.
                    self.presentDelivery(payload: payload)
                    return
                }
            }
            if self.challenge == nil && self.pendingStore.peekPendingDeeplink() == nil {
                self.pendingPayload = nil
                self.deliveryPresented = false
            }
        }
    }

    private func retireUnreadyDelivery() {
        guard let outgoing = challenge, outgoing.model.state.readiness != .ready,
              !outgoing.model.state.finishing else { return }
        initializationTasks.removeValue(forKey: outgoing.id)?.cancel()
        SharedFeatures.shared.closeChallenge(sessionId: outgoing.id)
        challenges.removeAll { $0.id == outgoing.id }
        challengeFailures.removeValue(forKey: outgoing.id)
        challenge = nil
    }

    private func presentDelivery(payload: String) {
        if challenge?.payload != payload { retireUnreadyDelivery() }
        pendingPayload = payload
        deliveryPresented = true
        guard let decoded = IosApplication.shared.decodeAlarmHandoffJson(payload: payload) else { return }
        let occurrence = decoded.activeAt.map { String($0.int64Value) } ?? decoded.deliveryId ?? "legacy"
        let id = "native-window/\(windowID)/occurrence/\(decoded.alarmId)/\(occurrence)"
        if let retained = challenges.first(where: { $0.id == id }) { challenge = retained }
        else {
            let retained = NativeChallengeSession(id: id, payload: payload, model: SharedFeatures.shared.challenge(sessionId: id))
            challenges.append(retained)
            challenge = retained
        }
        if let challenge { initialize(session: challenge) }
    }

    /// Test uses the editor's semantic draft snapshot. It cannot touch native delivery state.
    func openPreview(editorID: String) {
        guard !windowEnded, previews[editorID] == nil,
              selectedEditorID == editorID, editorPaths[editorID]?.last == .preview,
              let editor = editors.first(where: { $0.id == editorID }), !editor.model.isClosed else { return }
        editor.model.onEvent(event: AddEditAlarmEvent.OnTestClick.shared)
        guard let result = editor.model.state.results.last(where: { $0.event is AlarmSettingsViewModel.UiEventTestAlarm }),
              let event = result.event as? AlarmSettingsViewModel.UiEventTestAlarm else { return }
        let id = "maths-preview/\(editorID)/\(UUID().uuidString)"
        let preview = NativeChallengeSession(id: id, payload: "", model: SharedFeatures.shared.challenge(sessionId: id),
            editorID: editorID, previewAlarm: event.alarm)
        previews[editorID] = preview
        editor.model.acknowledgeResult(id: result.id)
        initialize(session: preview)
    }

    func initialize(session: NativeChallengeSession) {
        guard !windowEnded, !session.model.isClosed, initializationTasks[session.id] == nil else { return }
        initializationTasks[session.id] = Task { @MainActor [weak self] in
            guard let self else { return }
            defer { self.initializationTasks.removeValue(forKey: session.id) }
            do {
                let ready: KotlinBoolean
                if let draft = session.previewAlarm {
                    ready = try await asyncFunction(for: session.model.initializeChallenge(alarm: draft, preview: true))
                } else if let decoded = IosApplication.shared.decodeAlarmHandoffJson(payload: session.payload) {
                    ready = try await asyncFunction(for: session.model.initializeOccurrence(alarmId: decoded.alarmId, activeAt: decoded.activeAt))
                } else { return }
                guard !self.windowEnded, !Task.isCancelled, !session.model.isClosed else { return }
                self.receiveChallengeResults(id: session.id)
                if ready.boolValue {
                    if (self.challengeFailures[session.id]?.outcome as? ChallengeOutcome.Failure)?.error == .initialization {
                        self.challengeFailures.removeValue(forKey: session.id)
                    }
                    if !session.isPreview {
                        self.acceptReadyDelivery(session: session)
                        DispatchQueue.main.async { self.refreshPendingDelivery() }
                    }
                }
            } catch {
                // Cancelled observations cannot accept a handoff. Shared work may still finish.
            }
        }
    }

    @discardableResult
    private func acceptReadyDelivery(session: NativeChallengeSession, restoredPayload: String? = nil) -> Bool {
        let payload = restoredPayload ?? session.payload
        guard challenge?.id == session.id, session.model.state.readiness == .ready, !session.model.state.finishing,
              session.model.state.occurrenceId != nil,
              let decoded = IosApplication.shared.decodeAlarmHandoffJson(payload: payload),
              let alarm = session.model.state.alarm, alarm.alarmId == decoded.alarmId,
              (decoded.activeAt == nil && (decoded.version == 1 || payload == session.payload)) ||
              decoded.activeAt?.int64Value == alarm.activeAt?.int64Value else { return false }
        guard pendingStore.acknowledgePendingDeeplink(payload) else {
            // A replaced head can never be consumed by this completion callback.
            handoffWriteFailed = pendingStore.peekPendingDeeplink() == payload
            return false
        }
        handoffWriteFailed = false
        IosApplication.shared.acknowledgeHandoff(payload: payload)
        Task { @MainActor in await self.restoreRecovery([payload]) }
        return true
    }

    func retryDelivery() {
        guard !windowEnded else { return }
        handoffWriteFailed = false
        if let challenge {
            if challenge.model.state.readiness == .ready { refreshPendingDelivery() }
            else { initialize(session: challenge) }
        } else { refreshPendingDelivery() }
    }

    /// Owners consume retained IDs; borrowed children never turn dismissal into resolution.
    func receiveChallengeResults(id: String) {
        guard !windowEnded, let session = (challenges + Array(previews.values)).first(where: { $0.id == id }),
              !session.model.isClosed else { return }
        for result in session.model.state.results {
            if result.outcome is ChallengeOutcome.Failure {
                challengeFailures[id] = result
                session.model.acknowledgeResult(id: result.id)
            } else if result.outcome is ChallengeOutcome.Completed || result.outcome is ChallengeOutcome.Snoozed {
                guard session.model.state.readiness == .resolved else { continue }
                session.model.acknowledgeResult(id: result.id)
                if let editorID = session.editorID { closePreview(editorID: editorID) }
                else {
                    SharedFeatures.shared.closeChallenge(sessionId: id)
                    challenges.removeAll { $0.id == id }
                    challengeFailures.removeValue(forKey: id)
                    if challenge?.id == id {
                        challenge = nil
                        pendingPayload = nil
                        deliveryPresented = false
                        advanceAfterDismiss = true
                    }
                }
                return
            }
        }
    }

    func returnFromStaleOccurrence(sessionID: String) {
        guard let current = challenge, current.id == sessionID,
              current.model.state.readiness == .error, current.model.state.error == .staleOccurrence else { return }
        retireUnreadyDelivery()
        pendingPayload = nil
        deliveryPresented = false
        advanceAfterDismiss = true
    }

    func presentationEnded() {
        guard advanceAfterDismiss else { return }
        advanceAfterDismiss = false
        refreshPendingDelivery()
    }

    func dismissChallengeFailure(id: String, resultID: Int64) {
        guard challengeFailures[id]?.id == resultID else { return }
        challengeFailures.removeValue(forKey: id)
    }

    func retryChallengeFailure(session: NativeChallengeSession, resultID: Int64) {
        guard !session.model.isClosed, challengeFailures[session.id]?.id == resultID,
              let failure = challengeFailures[session.id]?.outcome as? ChallengeOutcome.Failure else { return }
        dismissChallengeFailure(id: session.id, resultID: resultID)
        switch failure.error {
        case .initialization: initialize(session: session)
        case .tone: session.model.retryAudio()
        case .dismiss:
            if let alarm = session.model.state.alarm { session.model.completeAlarm(alarm: alarm, preview: session.isPreview) }
        case .snooze:
            if let alarm = session.model.state.alarm {
                session.model.onEvent(event: MathScreenEvent.OnSnoozeClick(alarm: alarm.alarmId, preview: session.isPreview))
            }
        case .recovery:
            Task { @MainActor in await self.restoreRecovery([session.payload]) }
        case .update:
            if let problem = session.model.state.currentProblem {
                session.model.submitAnswer(questionIndex: session.model.state.questionIndex, problem: problem)
            }
        default: break // Incorrect answers are retried with the answer controls.
        }
    }

    func beginPreviewPresentation(editorID: String) -> Int {
        nextPreviewPresentation += 1
        let generation = nextPreviewPresentation
        guard selectedEditorID == editorID, editorPaths[editorID]?.last == .preview else { return generation }
        previewPresentations[editorID] = generation
        if let preview = previews[editorID], preview.model.state.readiness == .ready { preview.model.retryAudio() }
        return generation
    }

    func endPreviewPresentation(editorID: String, generation: Int) {
        guard previewPresentations[editorID] == generation else { return }
        previewPresentations.removeValue(forKey: editorID)
        previews[editorID]?.model.stopPreview()
    }

    func closePreview(editorID: String, returning: Bool = true, expectedSessionID: String? = nil) {
        if let expectedSessionID, previews[editorID]?.id != expectedSessionID { return }
        guard let preview = previews.removeValue(forKey: editorID) else { return }
        previewPresentations.removeValue(forKey: editorID)
        initializationTasks.removeValue(forKey: preview.id)?.cancel()
        SharedFeatures.shared.closeChallenge(sessionId: preview.id)
        challengeFailures.removeValue(forKey: preview.id)
        if returning, selectedEditorID == editorID {
            var path = editorPaths[editorID] ?? []
            if path.last == .preview { path.removeLast(); setEditorPath(path, id: editorID) }
        }
    }

    isolated deinit {
        // The actual window lifetime ends here. Closing observations cannot resolve a
        // durable occurrence, cancel application commands or silence application audio.
        let editorIDs = editors.map(\.id)
        let sounds = Array(soundSelections.values)
        let guide = settingsGuide
        let challengeIDs = challenges.map(\.id) + previews.values.map(\.id)
        DispatchQueue.main.async {
            guide.stop()
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
            ForEach(sessions.challenges + Array(sessions.previews.values)) { session in
                NativeChallengeOwner(model: session.model, sessionID: session.id, sessions: sessions).id(session.id)
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
    let sessionID: String
    @ObservedObject var sessions: NativeWindowSessions
    var body: some View {
        let _ = model
        Color.clear
            .onAppear { sessions.receiveChallengeResults(id: sessionID) }
            .onChange(of: model.state.results.map(\.id)) { sessions.receiveChallengeResults(id: sessionID) }
    }
}
