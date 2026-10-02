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
                ContentUnavailableView("No alarms", systemImage: "alarm",
                    description: Text("Add an alarm to get started."))
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
                                    Label("Scheduling needs attention", systemImage: "exclamationmark.triangle")
                                        .font(.caption).foregroundStyle(.red)
                                }
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
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
                        guard sessions.ownsNavigation(id: session.id, observer: navigationObserver.generation),
                              sessions.editorPaths[session.id]?.last == destination else { return }
                        onDestinationAppeared?(session.id, navigationObserver.generation, destination)
                    }
                })
        }
        .onAppear { sessions.beginNavigation(id: session.id, observer: navigationObserver.generation) }
        .onDisappear {
            sessions.endNavigation(id: session.id, observer: navigationObserver.generation)
            pathInitialized = false
        }
        .task {
            guard !pathInitialized else { return }
            await Task.yield()
            guard !Task.isCancelled,
                  sessions.ownsNavigation(id: session.id, observer: navigationObserver.generation) else { return }
            path = sessions.editorPaths[session.id] ?? []
            pathInitialized = true
        }
        .onChange(of: path) { value in
            guard pathInitialized, sessions.editorPaths[session.id] != value else { return }
            sessions.setEditorPath(value, id: session.id, observer: navigationObserver.generation)
        }
        .onChange(of: sessions.editorPaths[session.id] ?? []) { value in
            guard pathInitialized, path != value,
                  sessions.ownsNavigation(id: session.id, observer: navigationObserver.generation) else { return }
            path = value
        }
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
                NativeDevelopmentScreen(title: NativeStrings.text("Test Alarm"), milestone: 5)
            }
        }
        .scrollDismissesKeyboard(.interactively)
    }
}

struct NativeDevelopmentScreen: View {
    let title: String
    let milestone: Int
    var body: some View {
        ContentUnavailableView(title, systemImage: "wrench.and.screwdriver",
            description: Text("Native development screen. This feature is scheduled for Milestone \(milestone)."))
            .navigationTitle(title)
    }
}

@MainActor
struct NativePendingDelivery: View {
    @ObservedObject var sessions: NativeWindowSessions
    var body: some View {
        Group {
            if let session = sessions.challenge { NativeChallengeDevelopment(model: session.model) }
            else {
                ContentUnavailableView("Pending alarm delivery", systemImage: "exclamationmark.triangle",
                    description: Text("This delivery could not be decoded. It remains in the durable queue; no alarm has been acknowledged or resolved."))
            }
        }
        .navigationTitle("Pending alarm")
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Return to development app") { sessions.deliveryPresented = false }
            }
        }
    }
}

@MainActor
struct NativeChallengeDevelopment: View {
    @ObservedViewModel var model: AlarmMathViewModel
    var body: some View {
        Form {
            Section("Native challenge — Milestone 5 pending") {
                Text("The native challenge is not ready in this development build. This delivery remains unacknowledged in the durable queue.")
                Text("Returning or closing the app does not complete, snooze or silence an unresolved alarm. Existing native recovery remains active.")
            }
            if let alarm = model.state.alarm {
                Section("Alarm") { Text(alarm.title) }
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
