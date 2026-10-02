import SwiftUI
import KMPObservableViewModelSwiftUI
import app
import AlarmKit
import UIKit

@MainActor
struct NativeAlarmList: View {
    @ObservedViewModel var model: AlarmListViewModel
    @ObservedObject var sessions: NativeWindowSessions
    let openEditor: (app.Alarm?) -> Void
    let openSettings: () -> Void
    @State private var clearConfirmation = false
    @State private var errorResult: AlarmListResult?
    @State private var message: String?

    var body: some View {
        List {
            if model.state.loading {
                ProgressView("Loading alarms")
            } else if model.state.alarms.isEmpty {
                ContentUnavailableView {
                    Label {
                        Text("No alarms")
                            .lineLimit(nil)
                            .multilineTextAlignment(.center)
                            .fixedSize(horizontal: false, vertical: true)
                    } icon: { Image(systemName: "alarm") }
                } description: { Text("Add an alarm to get started.") }
            } else {
                ForEach(model.state.alarms, id: \.alarmId) { alarm in
                    HStack {
                        Button { model.onEvent(event: AlarmListEvent.OnEditAlarmClick(alarm: alarm)) } label: {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(NativeAlarmPresentation.time(hour: alarm.hour, minute: alarm.minute))
                                    .font(.title2.monospacedDigit())
                                if !alarm.title.isEmpty { Text(alarm.title) }
                                Text(NativeAlarmPresentation.recurrence(alarm))
                                    .font(.caption).foregroundStyle(.secondary)
                                if alarm.scheduleError != nil {
                                    Label {
                                        Text("Scheduling needs attention").foregroundStyle(Color.primary)
                                    } icon: {
                                        Image(systemName: "exclamationmark.triangle").foregroundStyle(Color.red)
                                    }
                                    .font(.caption)
                                }
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                        .accessibilityElement(children: .combine)
                        .accessibilityHint(Text("Change the alarm time, label and settings."))
                        .accessibilityIdentifier("edit-alarm-\(alarm.alarmId)")
                        Toggle("Enabled", isOn: Binding(get: { alarm.isOn }, set: {
                            model.setEnabled(alarm: alarm, enabled: $0)
                        }))
                        .labelsHidden()
                        .accessibilityLabel(Text("Enabled") + Text(" " + NativeAlarmPresentation.time(hour: alarm.hour, minute: alarm.minute)) + Text(alarm.title.isEmpty ? "" : " " + alarm.title))
                    }
                    .swipeActions {
                        Button("Delete", role: .destructive) {
                            model.onEvent(event: AlarmListEvent.OnDeleteAlarmClick(alarm: alarm))
                        }
                    }
                    .disabled(model.state.pendingOperations > 0)
                    .listRowBackground(sessions.selectedEditor?.resolvedAlarmID == alarm.alarmId ? Color.accentColor.opacity(0.12) : nil)
                    .accessibilityIdentifier("alarm-row-\(alarm.alarmId)")
                }
            }
            if message != nil || model.state.canUndoDelete {
                Section {
                    if let message { Text(message) }
                    if model.state.canUndoDelete {
                        Button("Undo delete") {
                            model.onEvent(event: AlarmListEvent.OnUndoDeleteClick.shared)
                        }.disabled(model.state.pendingOperations > 0)
                    }
                }
            }
            if sessions.pendingPayload != nil {
                Button("Pending alarm delivery") { sessions.deliveryPresented = true }
            }
        }
        .navigationTitle("Math Alarm")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button("Add alarm", systemImage: "plus") { model.onEvent(event: AlarmListEvent.OnAddAlarmClick.shared) }
                    .accessibilityIdentifier("add-alarm")
            }
            ToolbarItem(placement: .secondaryAction) {
                Button("Settings", systemImage: "gearshape", action: openSettings)
                    .accessibilityIdentifier("app-settings")
            }
            ToolbarItem(placement: .secondaryAction) {
                Button("Clear alarms", systemImage: "trash", role: .destructive) {
                    clearConfirmation = true
                }.disabled(model.state.loading || model.state.alarms.isEmpty || model.state.pendingOperations > 0)
            }
            if !sessions.editors.isEmpty {
                ToolbarItem(placement: .secondaryAction) {
                    Menu("Retained drafts", systemImage: "doc.text") {
                        ForEach(sessions.editors) { session in
                            Button(session.model.state.alarmTitle.isEmpty ? NativeStrings.text("New alarm") : session.model.state.alarmTitle) {
                                if let alarm = model.state.alarms.first(where: { $0.alarmId == session.alarmID }) {
                                    openEditor(alarm)
                                } else if session.alarmID == nil {
                                    openEditor(nil)
                                } else {
                                    sessions.selectEditor(id: session.id)
                                }
                            }
                        }
                    }
                }
            }
        }
        .confirmationDialog("Delete all alarms?", isPresented: $clearConfirmation,
                            titleVisibility: .visible) {
            Button("Delete all alarms", role: .destructive) {
                model.onEvent(event: AlarmListEvent.OnClearAlarmsClick.shared)
            }
        }
        .alert("Alarm update failed", isPresented: Binding(get: { errorResult != nil && !sessions.deliveryPresented }, set: {
            if !$0 { errorResult = nil } // Observation removal cannot acknowledge a retained failure.
        })) {
            let presentedResultID = errorResult?.id
            Button("OK", role: .cancel) {
                guard !sessions.deliveryPresented, !model.isClosed, let result = errorResult,
                      result.id == presentedResultID, model.state.results.contains(where: { $0.id == result.id }) else { return }
                model.acknowledgeResult(id: result.id)
                errorResult = nil
            }
        } message: {
            Text(NativeStrings.error((errorResult?.event as? UiEvent.ShowError)?.error ?? .update))
        }
        .onAppear { presentResults() }
        .onChange(of: model.state.results.map(\.id)) { presentResults() }
        .onChange(of: sessions.deliveryPresented) { presented in if !presented { presentResults() } }
        .onChange(of: model.state.canUndoDelete) { available in if !available { message = nil } }
    }

    private func presentResults() {
        if let result = errorResult, !model.state.results.contains(where: { $0.id == result.id }) { errorResult = nil }
        guard errorResult == nil else { return }
        for result in model.state.results {
            if let navigation = result.event as? UiEvent.Navigate {
                openEditor(navigation.alarm.alarmId == 0 ? nil : navigation.alarm)
                model.acknowledgeResult(id: result.id)
                continue
            }
            if result.event is UiEvent.ShowError {
                errorResult = result
                if let error = result.event as? UiEvent.ShowError { NativeAccessibility.announce(NativeStrings.error(error.error)) }
                return
            }
            if let notice = result.event as? UiEvent.ShowSnackbar {
                if notice.code == .deleted {
                    message = NativeStrings.text("Alarm deleted.")
                } else if notice.code == .empty {
                    message = NativeStrings.text("There are no alarms to clear.")
                } else if notice.code == .scheduled {
                    message = NativeStrings.text("Alarm scheduled.")
                }
                // Retain the native affordance before acknowledging the semantic result.
                if let message { NativeAccessibility.announce(message) }
                model.acknowledgeResult(id: result.id)
            }
        }
    }
}

enum NativeEditorDestination: String, Hashable, CaseIterable {
    case repeatSettings = "Repeat"
    case snooze = "Snooze"
    case challenge = "Challenge settings"
    case sound = "Sound library"
    case preview = "Test Alarm"
}

/// The same native stack is used by the app and mounted production verification.
@MainActor
struct NativeEditorStack: View {
    @ObservedObject var sessions: NativeWindowSessions
    var onDestinationAppeared: ((String, Int, NativeEditorDestination?) -> Void)? = nil
    @State private var errorResult: AlarmEditorResult?
    @State private var errorSessionID: String?

    var body: some View {
        Group {
            if let editor = sessions.selectedEditor {
                NativeEditorNavigation(session: editor, sessions: sessions,
                    requestingPermission: sessions.permissionRequests.contains(editor.id),
                    onDestinationAppeared: onDestinationAppeared)
                    .id(editor.id)
            } else {
                NavigationStack {
                    ContentUnavailableView("Select an alarm", systemImage: "alarm",
                        description: Text("Choose an alarm or add a new one."))
                }
            }
        }
        .onAppear { presentResults() }
        .onChange(of: sessions.editorResultsRevision) { presentResults() }
        .onChange(of: sessions.selectedEditorID) { _ in presentResults() }
        .onChange(of: sessions.deliveryPresented) { presented in if !presented { presentResults() } }
        .alert(errorResult?.event is AlarmSettingsViewModel.UiEventRequestExactAlarmPermission
               ? NativeStrings.text("Alarm permission required") : NativeStrings.text("Unable to save alarm"),
               isPresented: Binding(get: { errorResult != nil && sessions.selectedEditorID == errorSessionID && !sessions.deliveryPresented },
                                    set: { if !$0 { errorResult = nil } })) {
            let presentedSessionID = errorSessionID
            let presentedResultID = errorResult?.id
            if errorResult?.event is AlarmSettingsViewModel.UiEventRequestExactAlarmPermission {
                Button("Allow alarms") { requestPermission(sessionID: presentedSessionID, resultID: presentedResultID) }
                Button("Open Settings") {
                    guard presentedEditor(sessionID: presentedSessionID, resultID: presentedResultID) != nil else { return }
                    acknowledgeError(sessionID: presentedSessionID, resultID: presentedResultID)
                    if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) }
                }
            } else {
                Button("Try again") {
                    guard let editor = presentedEditor(sessionID: presentedSessionID, resultID: presentedResultID) else { return }
                    acknowledgeError(sessionID: presentedSessionID, resultID: presentedResultID)
                    editor.model.onEvent(event: AddEditAlarmEvent.OnSaveTodoClick.shared)
                }
            }
            Button("Keep editing", role: .cancel) { acknowledgeError(sessionID: presentedSessionID, resultID: presentedResultID) }
        } message: {
            if let failure = errorResult?.event as? AlarmSettingsViewModel.UiEventShowError {
                Text(NativeStrings.error(failure.error))
            } else if let validation = errorResult?.event as? AlarmSettingsViewModel.UiEventValidationFailed {
                Text(NativeStrings.validation(validation.validation))
            } else {
                Text("Allow alarms to schedule this alarm, then try saving again.")
            }
        }
    }

    private func presentedEditor(sessionID: String?, resultID: Int64?) -> NativeEditorSession? {
        guard errorSessionID == sessionID, errorResult?.id == resultID, !sessions.deliveryPresented,
              let editor = sessions.selectedEditor,
              editor.id == errorSessionID, !editor.model.isClosed,
              let result = errorResult, editor.model.state.results.contains(where: { $0.id == result.id }) else { return nil }
        return editor
    }

    private func acknowledgeError(sessionID: String?, resultID: Int64?) {
        guard let editor = presentedEditor(sessionID: sessionID, resultID: resultID), let result = errorResult else { return }
        editor.model.acknowledgeResult(id: result.id)
        errorResult = nil
    }

    private func presentResults() {
        guard let editor = sessions.selectedEditor, !editor.model.isClosed else {
            errorResult = nil
            errorSessionID = nil
            return
        }
        if errorSessionID != editor.id { errorResult = nil }
        errorSessionID = editor.id
        if let result = errorResult, !editor.model.state.results.contains(where: { $0.id == result.id }) { errorResult = nil }
        guard errorResult == nil else { return }
        errorResult = editor.model.state.results.first { result in
            result.event is AlarmSettingsViewModel.UiEventShowError ||
            result.event is AlarmSettingsViewModel.UiEventValidationFailed ||
            result.event is AlarmSettingsViewModel.UiEventRequestExactAlarmPermission
        }
    }

    private func requestPermission(sessionID: String?, resultID: Int64?) {
        guard let editor = presentedEditor(sessionID: sessionID, resultID: resultID),
              sessions.beginPermissionRequest(id: editor.id) else { return }
        acknowledgeError(sessionID: sessionID, resultID: resultID)
        Task { @MainActor in
            defer { sessions.endPermissionRequest(id: editor.id) }
            _ = try? await AlarmManager.shared.requestAuthorization()
            guard !editor.model.isClosed else { return }
            editor.model.onEvent(event: AddEditAlarmEvent.OnSaveTodoClick.shared)
        }
    }
}

/// Monotonic rendering identities reject late appearances from outgoing stacks.
@MainActor
private final class NativeNavigationObserver: ObservableObject {
    private static var nextGeneration = 0
    let generation: Int
    init(sessions: NativeWindowSessions, id: String) {
        Self.nextGeneration += 1
        generation = Self.nextGeneration
        // Reserve ownership as SwiftUI installs this StateObject, before outgoing writes.
        sessions.beginNavigation(id: id, observer: generation)
    }
}

/// Restore routes after destinations register with a newly mounted native stack.
/// The window registry survives replacement; this view owns only its visible path.
@MainActor
private struct NativeEditorNavigation: View {
    let session: NativeEditorSession
    @ObservedObject var sessions: NativeWindowSessions
    let requestingPermission: Bool
    var onDestinationAppeared: ((String, Int, NativeEditorDestination?) -> Void)?
    @State private var path: [NativeEditorDestination] = []
    @State private var pathInitialized = false
    @StateObject private var navigationObserver: NativeNavigationObserver
    private var observer: Int { navigationObserver.generation }

    init(session: NativeEditorSession, sessions: NativeWindowSessions, requestingPermission: Bool,
         onDestinationAppeared: ((String, Int, NativeEditorDestination?) -> Void)?) {
        self.session = session
        self.sessions = sessions
        self.requestingPermission = requestingPermission
        self.onDestinationAppeared = onDestinationAppeared
        _navigationObserver = StateObject(wrappedValue: NativeNavigationObserver(sessions: sessions, id: session.id))
    }

    var body: some View {
        NavigationStack(path: $path) {
            NativeAlarmEditor(model: session.model, sessionID: session.id, sessions: sessions,
                requestingPermission: requestingPermission, onDestinationAppeared: { destination in
                    Task { @MainActor in
                        await Task.yield()
                        guard sessions.ownsNavigation(id: session.id, observer: observer),
                              sessions.editorPaths[session.id]?.last == destination else { return }
                        onDestinationAppeared?(session.id, observer, destination)
                    }
                })
        }
        .onAppear {
            sessions.beginNavigation(id: session.id, observer: observer)
            // Cached NavigationStacks can reappear without re-running their task.
            Task { @MainActor in await restorePath() }
        }
        .onDisappear {
            // SwiftUI may emit disappearance for a cached stack while its nested
            // native controller remains visible. Selection/window end or a newer
            // stack generation revokes ownership; this signal cannot do so.
            trace("disappeared")
        }
        .task(id: observer) { await restorePath() }
        .onChange(of: path) { value in
            guard pathInitialized, sessions.ownsNavigation(id: session.id, observer: observer),
                  sessions.editorPaths[session.id] != value else { trace("local write ignored"); return }
            trace("local write accepted")
            sessions.setEditorPath(value, id: session.id, observer: observer)
        }
        .onChange(of: sessions.editorPaths[session.id] ?? []) { value in
            guard sessions.ownsNavigation(id: session.id, observer: observer) else { trace("retained write rejected"); return }
            guard pathInitialized else {
                Task { @MainActor in await restorePath() }
                return
            }
            guard path != value else { return }
            trace("retained write accepted")
            path = value
        }
    }

    private func restorePath() async {
        guard sessions.ownsNavigation(id: session.id, observer: observer) else { trace("hydration rejected"); return }
        pathInitialized = false
        await Task.yield()
        guard !Task.isCancelled, sessions.ownsNavigation(id: session.id, observer: observer) else { trace("hydration cancelled"); return }
        path = sessions.editorPaths[session.id] ?? []
        pathInitialized = true
        trace("hydrated")
    }

    private func trace(_ action: String) {
        #if DEBUG
        guard SharedBridgeVerification.enabled else { return }
        print("BRIDGE TRACE navigation \(action) id=\(session.id) observer=\(observer) owned=\(sessions.ownsNavigation(id: session.id, observer: observer)) initialized=\(pathInitialized) local=\(path) retained=\(sessions.editorPaths[session.id] ?? [])")
        #endif
    }
}

@MainActor
struct NativeAlarmEditor: View {
    @ObservedViewModel var model: AlarmSettingsViewModel
    let sessionID: String
    @ObservedObject var sessions: NativeWindowSessions
    @State private var discardConfirmation = false
    var requestingPermission = false
    var onDestinationAppeared: ((NativeEditorDestination?) -> Void)? = nil

    var body: some View {
        Form {
            NativeEditorControls(model: model)
            if model.state.validation != .none {
                Section { Label(NativeStrings.validation(model.state.validation), systemImage: "exclamationmark.triangle") }
            }
            if model.state.isSaving {
                Section { ProgressView("Saving…").accessibilityIdentifier("editor-saving") }
            }
        }
        .disabled(model.state.isSaving || requestingPermission)
        .scrollDismissesKeyboard(.interactively)
        .onAppear { onDestinationAppeared?(nil) }
        .navigationTitle(model.state.isSaved ? NativeStrings.text("Edit alarm") : NativeStrings.text("New alarm"))
        .navigationDestination(for: NativeEditorDestination.self) { destination in
            NativeEditorSubpage(model: model, sessionID: sessionID, sessions: sessions, destination: destination)
                .id(destination)
                .onAppear { onDestinationAppeared?(destination) }
        }
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel", role: .cancel) {
                    if model.state.hasUnsavedChanges { discardConfirmation = true }
                    else { sessions.closeEditor(id: sessionID) }
                }
                .disabled(model.state.isSaving || requestingPermission)
                .accessibilityIdentifier("discard-draft")
            }
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") { model.onEvent(event: AddEditAlarmEvent.OnSaveTodoClick.shared) }
                    .disabled(model.state.isSaving || requestingPermission)
                    .accessibilityIdentifier("save-alarm")
            }
        }
        .confirmationDialog("Discard this unsaved draft?", isPresented: $discardConfirmation,
                            titleVisibility: .visible) {
            Button("Discard draft", role: .destructive) { sessions.closeEditor(id: sessionID) }
            Button("Keep editing", role: .cancel) { }
        }

    }

}

@MainActor
struct NativeEditorSubpage: View {
    @ObservedViewModel var model: AlarmSettingsViewModel
    let sessionID: String
    @ObservedObject var sessions: NativeWindowSessions
    let destination: NativeEditorDestination
    var body: some View {
        Group {
            switch destination {
            case .repeatSettings: NativeRepeatSettings(model: model)
            case .challenge: NativeChallengeSettings(model: model)
            case .snooze: NativeSnoozeSettings(model: model)
            case .sound:
                if let selection = sessions.soundSelection(sessionID: sessionID) {
                    NativeSoundPicker(model: model, selection: selection, onClose: {
                        var path = sessions.editorPaths[sessionID] ?? []
                        if path.last == .sound { path.removeLast(); sessions.setEditorPath(path, id: sessionID) }
                    })
                }
            case .preview:
                NativeMathPreview(editorID: sessionID, sessions: sessions)
            }
        }
        .scrollDismissesKeyboard(.interactively)
    }
}

@MainActor
struct NativeMathPreview: View {
    let editorID: String
    @ObservedObject var sessions: NativeWindowSessions
    @State private var presentationGeneration: Int?
    var body: some View {
        Group {
            if let preview = sessions.previews[editorID] {
                NativeOwnedChallenge(session: preview, sessions: sessions)
            } else {
                ProgressView("Preparing challenge…")
            }
        }
        .navigationBarBackButtonHidden()
        .onAppear {
            sessions.openPreview(editorID: editorID)
            presentationGeneration = sessions.beginPreviewPresentation(editorID: editorID)
        }
        .onDisappear {
            if let generation = presentationGeneration {
                sessions.endPreviewPresentation(editorID: editorID, generation: generation)
            }
        }
    }
}

@MainActor
struct NativeOwnedChallenge: View {
    let session: NativeChallengeSession
    @ObservedObject var sessions: NativeWindowSessions
    var body: some View {
        let result = sessions.challengeFailures[session.id]
        let failure = (result?.outcome as? ChallengeOutcome.Failure)?.error
        NativeChallengeView(model: session.model, preview: session.isPreview,
            retryInitialization: { sessions.initialize(session: session) },
            cancelPreview: session.editorID.map { editorID in { sessions.closePreview(editorID: editorID, expectedSessionID: session.id) } },
            returnFromStale: { sessions.returnFromStaleOccurrence(sessionID: session.id) },
            failure: failure, failureResultID: result?.id,
            retryFailure: result.flatMap { result in
                failure == .incorrectAnswer || failure == .staleOccurrence ? nil : {
                    sessions.retryChallengeFailure(session: session, resultID: result.id)
                }
            },
            dismissFailure: result.map { result in {
                sessions.dismissChallengeFailure(id: session.id, resultID: result.id)
            } })
            .id(session.id)
    }
}

@MainActor
struct NativePendingDelivery: View {
    @ObservedObject var sessions: NativeWindowSessions
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency
    var body: some View {
        Group {
            if let session = sessions.challenge {
                NativeOwnedChallenge(session: session, sessions: sessions)
            } else {
                ContentUnavailableView {
                    Label("Pending alarm delivery", systemImage: "exclamationmark.triangle")
                } description: {
                    Text("Delivery could not be decoded. It remains queued for retry.")
                } actions: {
                    Button("Try again") { sessions.retryDelivery() }
                    Button("Return to alarms") { sessions.deliveryPresented = false }
                }
            }
        }
        .safeAreaInset(edge: .bottom) {
            if sessions.handoffWriteFailed {
                VStack {
                    Text("Unable to retain delivery acknowledgement. Try again.")
                    Button("Try again") { sessions.retryDelivery() }
                }
                .padding()
                .frame(maxWidth: .infinity)
                .background {
                    if reduceTransparency { Color(uiColor: .systemBackground) }
                    else { Rectangle().fill(.regularMaterial) }
                }
            }
        }
    }
}

/// Native locale/calendar formatting never changes stored alarm values.
enum NativeAlarmPresentation {
    static func time(hour: Int32, minute: Int32) -> String {
        let date = Calendar.current.date(from: DateComponents(year: 2001, month: 1, day: 1,
            hour: Int(hour), minute: Int(minute))) ?? Date()
        return date.formatted(date: .omitted, time: .shortened)
    }

    static func recurrence(_ alarm: app.Alarm) -> String {
        let symbols = Calendar.current.shortWeekdaySymbols
        let weekdays = alarm.repeatDays.enumerated().compactMap { index, value in
            value == "T" && index < symbols.count ? symbols[index] : nil
        }
        if weekdays.isEmpty { return NativeStrings.text("Once") }
        let days = ListFormatter.localizedString(byJoining: weekdays)
        return alarm.repeat ? String(format: NativeStrings.text("Every %@"), days) : days
    }
}
