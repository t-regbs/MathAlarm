import SwiftUI
import KMPObservableViewModelSwiftUI
import app

@MainActor
struct NativeAlarmList: View {
    @ObservedViewModel var model: AlarmListViewModel
    @ObservedObject var sessions: NativeWindowSessions
    let openEditor: (Alarm?) -> Void
    let openSettings: () -> Void
    @State private var clearConfirmation = false
    @State private var errorResult: AlarmListResult?
    @State private var message: String?
    @State private var canUndo = false

    var body: some View {
        List {
            Section {
                Text("Native UI development build")
                    .font(.caption).foregroundStyle(.secondary)
                Text("Editing, sounds, challenges and settings are being migrated in later milestones.")
                    .font(.caption).foregroundStyle(.secondary)
            }
            if model.state.loading {
                ProgressView("Loading alarms")
            } else if model.state.alarms.isEmpty {
                ContentUnavailableView("No alarms", systemImage: "alarm",
                    description: Text("Add a development draft to begin."))
            } else {
                ForEach(model.state.alarms, id: \.alarmId) { alarm in
                    HStack {
                        Button { openEditor(alarm) } label: {
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
                        .accessibilityLabel("Enable \(alarm.title.isEmpty ? "alarm" : alarm.title)")
                    }
                    .swipeActions {
                        Button("Delete", role: .destructive) {
                            model.onEvent(event: AlarmListEvent.OnDeleteAlarmClick(alarm: alarm))
                        }
                    }
                    .accessibilityIdentifier("alarm-row-\(alarm.alarmId)")
                }
            }
            if let message {
                Section {
                    Text(message)
                    if canUndo {
                        Button("Undo delete") {
                            model.onEvent(event: AlarmListEvent.OnUndoDeleteClick.shared)
                            canUndo = false
                            self.message = nil
                        }
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
                Button("Add alarm", systemImage: "plus") { openEditor(nil) }
                    .accessibilityIdentifier("add-alarm")
            }
            ToolbarItem(placement: .secondaryAction) {
                Button("Settings", systemImage: "gearshape", action: openSettings)
            }
            ToolbarItem(placement: .secondaryAction) {
                Button("Clear alarms", systemImage: "trash", role: .destructive) {
                    clearConfirmation = true
                }.disabled(model.state.loading || model.state.alarms.isEmpty)
            }
            if !sessions.editors.isEmpty {
                ToolbarItem(placement: .secondaryAction) {
                    Menu("Retained drafts", systemImage: "doc.text") {
                        ForEach(sessions.editors) { session in
                            Button(session.model.state.alarmTitle) {
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
        .alert("Alarm update failed", isPresented: Binding(get: { errorResult != nil }, set: {
            if !$0, let result = errorResult {
                model.acknowledgeResult(id: result.id)
                errorResult = nil
            }
        })) {
            Button("OK", role: .cancel) { }
        } message: {
            Text("The shared command could not update the alarm. Its saved state remains authoritative. Try again.")
        }
        .onAppear { presentResults() }
        .onChange(of: model.state.results.map(\.id)) { presentResults() }
    }

    private func presentResults() {
        guard errorResult == nil else { return }
        for result in model.state.results {
            if result.event is UiEvent.ShowError {
                errorResult = result
                return
            }
            if let notice = result.event as? UiEvent.ShowSnackbar {
                if notice.code == .deleted {
                    message = "Alarm deleted."
                    canUndo = notice.actionType == .undoDelete
                } else if notice.code == .empty {
                    message = "There are no alarms to clear."
                    canUndo = false
                } else if notice.code == .scheduled {
                    message = "Alarm scheduled."
                    canUndo = false
                }
                // Retain the native affordance before acknowledging the semantic result.
                model.acknowledgeResult(id: result.id)
            }
        }
    }
}

enum NativeEditorDestination: String, Hashable, CaseIterable {
    case challenge = "Challenge settings"
    case sound = "Sound library"
    case preview = "Test Alarm"
}

/// The same native stack is used by the app and mounted production verification.
@MainActor
struct NativeEditorStack: View {
    @ObservedObject var sessions: NativeWindowSessions
    var onDestinationAppeared: ((String, Int, NativeEditorDestination?) -> Void)? = nil

    var body: some View {
        if let editor = sessions.selectedEditor {
            NativeEditorNavigation(session: editor, sessions: sessions,
                onDestinationAppeared: onDestinationAppeared)
                .id(editor.id)
        } else {
            NavigationStack {
                ContentUnavailableView("Select an alarm", systemImage: "alarm",
                    description: Text("Choose an alarm or add a retained development draft."))
            }
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
    var onDestinationAppeared: ((String, Int, NativeEditorDestination?) -> Void)?
    @State private var path: [NativeEditorDestination] = []
    @State private var pathInitialized = false
    @StateObject private var navigationObserver: NativeNavigationObserver

    init(session: NativeEditorSession, sessions: NativeWindowSessions,
         onDestinationAppeared: ((String, Int, NativeEditorDestination?) -> Void)?) {
        self.session = session
        self.sessions = sessions
        self.onDestinationAppeared = onDestinationAppeared
        _navigationObserver = StateObject(wrappedValue: NativeNavigationObserver(sessions: sessions, id: session.id))
    }

    var body: some View {
        NavigationStack(path: $path) {
            NativeAlarmEditor(model: session.model, sessionID: session.id, sessions: sessions,
                onDestinationAppeared: { destination in
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
    var onDestinationAppeared: ((NativeEditorDestination?) -> Void)? = nil

    var body: some View {
        Form {
            Section("Development editor — Milestone 4 pending") {
                Text("This retained draft demonstrates native navigation and shared state. Saving and full editing are not available in this build.")
                    .foregroundStyle(.secondary)
            }
            Section("Retained draft") {
                TextField("Label", text: Binding(get: { model.state.alarmTitle }, set: {
                    model.onEvent(event: AddEditAlarmEvent.EnteredTitle(value: $0))
                }))
                .accessibilityIdentifier("editor-title")
                LabeledContent("Time", value: NativeAlarmPresentation.time(
                    hour: model.state.alarmTime.hour, minute: model.state.alarmTime.minute))
                LabeledContent("Draft", value: model.state.hasUnsavedChanges ? "Unsaved changes" : "Unchanged")
            }
            Section("Native destinations") {
                ForEach(NativeEditorDestination.allCases, id: \.self) { destination in
                    NavigationLink(destination.rawValue, value: destination)
                }
            }
        }
        .onAppear { onDestinationAppeared?(nil) }
        .navigationTitle(model.state.isSaved ? "Edit alarm" : "New alarm")
        .navigationDestination(for: NativeEditorDestination.self) { destination in
            NativeEditorSubpage(model: model, destination: destination)
                .onAppear { onDestinationAppeared?(destination) }
        }
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Discard draft", role: .cancel) {
                    if model.state.hasUnsavedChanges { discardConfirmation = true }
                    else { sessions.closeEditor(id: sessionID) }
                }
                .accessibilityIdentifier("discard-draft")
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
    let destination: NativeEditorDestination
    var body: some View {
        Form {
            Section {
                Text("Native development screen").font(.headline)
                Text("\(destination.rawValue) will be completed in Milestone \(destination == .preview ? 5 : 4).")
                Text("Your draft is retained when you return.")
            }
            Section("Retained label") { Text(model.state.alarmTitle) }
        }
        .navigationTitle(destination.rawValue)
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

    static func recurrence(_ alarm: Alarm) -> String {
        let symbols = Calendar.current.shortWeekdaySymbols
        let weekdays = alarm.repeatDays.enumerated().compactMap { index, value in
            value == "T" && index < symbols.count ? symbols[index] : nil
        }
        if weekdays.isEmpty { return "Once" }
        let days = weekdays.joined(separator: ", ")
        return alarm.repeat ? "Every \(days)" : days
    }
}
