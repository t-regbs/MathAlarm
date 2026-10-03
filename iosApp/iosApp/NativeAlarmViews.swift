import SwiftUI
import KMPObservableViewModelSwiftUI
import app
import UIKit

/// Keep supplementary status in the native navigation bar. Its layout and
/// collapse behavior belong to SwiftUI, alongside the list's large title.
private struct NativeNextAlarmSubtitle: ViewModifier {
    let alarms: [app.Alarm]
    let loading: Bool
    @Environment(\.scenePhase) private var scenePhase
    @State private var now = Date()

    func body(content: Content) -> some View {
        content
            .navigationSubtitle(Text(verbatim: loading ? "" : NativeAlarmPresentation.nextAlarmSubtitle(alarms, now: now)))
            .task {
                while !Task.isCancelled {
                    now = Date()
                    do { try await Task.sleep(for: .seconds(60)) }
                    catch { return }
                }
            }
            .onChange(of: scenePhase) { _, phase in
                if phase == .active { now = Date() }
            }
            .onReceive(NotificationCenter.default.publisher(for: UIApplication.significantTimeChangeNotification)) { _ in
                now = Date()
            }
            .onReceive(NotificationCenter.default.publisher(for: .NSSystemTimeZoneDidChange)) { _ in
                now = Date()
            }
    }
}

@MainActor
struct NativeAlarmList: View {
    @ObservedViewModel var model: AlarmListViewModel
    @ObservedObject var sessions: NativeWindowSessions
    let openEditor: (app.Alarm?) -> Void
    let openSettings: () -> Void
    @State private var clearConfirmation = false
    @State private var errorResult: AlarmListResult?
    @State private var message: String?
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    var body: some View {
        Group {
            if !model.state.loading && model.state.alarms.isEmpty {
                emptyContent
            } else {
                alarmList
            }
        }
        .matAlarmContent()
        .navigationTitle("Math Alarm")
        .modifier(NativeNextAlarmSubtitle(alarms: model.state.alarms, loading: model.state.loading))
        .toolbar {
            listToolbar
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

    private var emptyContent: some View {
        GeometryReader { geometry in
            ScrollView {
                VStack(spacing: 20) {
                    MatAlarmEmptyState(addAlarm: { model.onEvent(event: AlarmListEvent.OnAddAlarmClick.shared) })
                    if hasListStatus {
                        VStack(spacing: 12) { listStatus }
                            .padding(.horizontal, 28)
                    }
                }
                // Balance the group against the large navigation title. Padding
                // lifts its visual center without clipping overflowing text.
                .padding(.bottom, min(64, geometry.size.height * 0.1))
                .frame(maxWidth: .infinity)
                .frame(minHeight: geometry.size.height)
            }
            .scrollBounceBehavior(.basedOnSize)
        }
    }

    private var alarmList: some View {
        List {
            if model.state.loading {
                ProgressView("Loading alarms")
            } else {
                ForEach(model.state.alarms, id: \.alarmId) { alarm in
                    Section {
                        let layout = dynamicTypeSize.isAccessibilitySize
                            ? AnyLayout(VStackLayout(alignment: .leading, spacing: 16))
                            : AnyLayout(HStackLayout(alignment: .center, spacing: 16))
                        layout {
                            Button { model.onEvent(event: AlarmListEvent.OnEditAlarmClick(alarm: alarm)) } label: {
                                VStack(alignment: .leading, spacing: 10) {
                                    MatAlarmTimeLabel(hour: alarm.hour, minute: alarm.minute, enabled: alarm.isOn)
                                    if !alarm.title.isEmpty { Text(alarm.title).font(.headline).foregroundStyle(.primary) }
                                    Text(NativeAlarmPresentation.recurrence(alarm))
                                        .font(.subheadline).foregroundStyle(.secondary)
                                        .fixedSize(horizontal: false, vertical: true)
                                    Text(NativeStrings.questions(alarm.questionCount))
                                        .font(.caption.weight(.medium))
                                        .foregroundStyle(MatAlarmPalette.accent)
                                        .padding(.horizontal, 10).padding(.vertical, 5)
                                        .background(MatAlarmPalette.wash, in: Capsule())
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
                            .fixedSize()
                            .accessibilityLabel(Text("Enabled") + Text(" " + NativeAlarmPresentation.time(hour: alarm.hour, minute: alarm.minute)) + Text(alarm.title.isEmpty ? "" : " " + alarm.title))
                        }
                        .padding(.vertical, 10)
                        .swipeActions {
                            Button("Delete", role: .destructive) {
                                model.onEvent(event: AlarmListEvent.OnDeleteAlarmClick(alarm: alarm))
                            }
                        }
                        .disabled(model.state.pendingOperations > 0)
                        .listRowBackground(sessions.selectedEditor?.resolvedAlarmID == alarm.alarmId
                            ? MatAlarmPalette.wash : MatAlarmPalette.surface)
                        .accessibilityIdentifier("alarm-row-\(alarm.alarmId)")
                    }
                }
            }
            if hasListStatus { Section { listStatus } }
        }
        .listStyle(.insetGrouped)
    }

    private var hasListStatus: Bool {
        message != nil || model.state.canUndoDelete || sessions.pendingPayload != nil
    }

    @ViewBuilder
    private var listStatus: some View {
        if let message { Text(message) }
        if model.state.canUndoDelete {
            Button("Undo delete") {
                model.onEvent(event: AlarmListEvent.OnUndoDeleteClick.shared)
            }.disabled(model.state.pendingOperations > 0)
        }
        if sessions.pendingPayload != nil {
            Button("Pending alarm delivery") { sessions.deliveryPresented = true }
        }
    }

    @ToolbarContentBuilder
    private var listToolbar: some ToolbarContent {
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
    private var permissionNeedsSettings: Bool {
        errorResult?.event is AlarmSettingsViewModel.UiEventRequestExactAlarmPermission
            && AlarmSchedulerBridge.shared.authorizationStatus() == "denied"
    }

    var body: some View {
        Group {
            if let editor = sessions.selectedEditor {
                NativeEditorNavigation(session: editor, sessions: sessions,
                    requestingPermission: sessions.permissionRequests.contains(editor.id),
                    onDestinationAppeared: onDestinationAppeared)
                    .id(editor.id)
            } else {
                NavigationStack {
                    ScrollView {
                        MatAlarmEmptyState(selection: true)
                            .frame(maxWidth: 480)
                            .frame(maxWidth: .infinity)
                            .padding(.top, 80)
                    }
                    .matAlarmContent()
                }
            }
        }
        .onAppear { presentResults() }
        .onChange(of: sessions.editorResultsRevision) { presentResults() }
        .onChange(of: sessions.permissionRequests) { presentResults() }
        .onChange(of: sessions.selectedEditorID) { _ in presentResults() }
        .onChange(of: sessions.deliveryPresented) { presented in if !presented { presentResults() } }
        .alert(errorResult?.event is AlarmSettingsViewModel.UiEventRequestExactAlarmPermission
               ? NativeStrings.text("Alarm permission required")
               : NativeStrings.text("Unable to save alarm"),
               isPresented: Binding(get: { errorResult != nil && sessions.selectedEditorID == errorSessionID
                   && !sessions.deliveryPresented && !permissionNeedsSettings
                   && !sessions.permissionRequests.contains(errorSessionID ?? "") },
                                    set: { if !$0 && !permissionNeedsSettings { errorResult = nil } })) {
            let presentedSessionID = errorSessionID
            let presentedResultID = errorResult?.id
            if errorResult?.event is AlarmSettingsViewModel.UiEventRequestExactAlarmPermission {
                Button("Allow alarms") { requestPermission(sessionID: presentedSessionID, resultID: presentedResultID) }
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
            } else if AlarmSchedulerBridge.shared.authorizationStatus() == "denied" {
                Text(NativeStrings.text("Turn on Alarms for Math Alarm in Settings, then return to finish saving.")
                     + "\n\n" + NativeStrings.text("If Settings opens its main page, go to Apps → Math Alarm → Alarms."))
            } else {
                Text("Math Alarm needs permission to schedule alarms. Tap Allow alarms to try again.")
            }
        }
        .sheet(isPresented: Binding(get: {
            permissionNeedsSettings && sessions.selectedEditorID == errorSessionID && !sessions.deliveryPresented
        }, set: {
            if !$0 { acknowledgeError(sessionID: errorSessionID, resultID: errorResult?.id) }
        }), onDismiss: {
            if !sessions.settingsGuide.floating { sessions.settingsGuide.stop() }
        }) {
            let id = errorSessionID
            let resultID = errorResult?.id
            NativeAlarmPermissionGuide(player: sessions.settingsGuide, openSettings: {
                openAlarmSettings(sessionID: id, resultID: resultID)
            }, keepEditing: { acknowledgeError(sessionID: id, resultID: resultID) })
        }
    }

    private func openAlarmSettings(sessionID: String?, resultID: Int64?) {
        guard let editor = presentedEditor(sessionID: sessionID, resultID: resultID),
              let url = URL(string: UIApplication.openSettingsURLString),
              sessions.beginAlarmSettingsSave(id: editor.id) else { sessions.settingsGuide.stop(); return }
        acknowledgeError(sessionID: sessionID, resultID: resultID)
        UIApplication.shared.open(url) { opened in
            if !opened {
                Task { @MainActor in
                    sessions.cancelAlarmSettingsSave(id: editor.id)
                    sessions.settingsGuide.stop()
                }
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
        if let permission = editor.model.state.results.first(where: {
            $0.event is AlarmSettingsViewModel.UiEventRequestExactAlarmPermission
        }) {
            sessions.requestInitialAlarmPermission(id: editor.id, resultID: permission.id)
        }
        guard !sessions.permissionRequests.contains(editor.id) else { errorResult = nil; return }
        guard errorResult == nil else { return }
        errorResult = editor.model.state.results.first { result in
            result.event is AlarmSettingsViewModel.UiEventShowError ||
            result.event is AlarmSettingsViewModel.UiEventValidationFailed ||
            result.event is AlarmSettingsViewModel.UiEventRequestExactAlarmPermission
        }
    }

    private func requestPermission(sessionID: String?, resultID: Int64?) {
        guard let editor = presentedEditor(sessionID: sessionID, resultID: resultID) else { return }
        acknowledgeError(sessionID: sessionID, resultID: resultID)
        sessions.requestAlarmPermission(id: editor.id)
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
            let wasOwner = sessions.ownsNavigation(id: session.id, observer: observer)
            if !wasOwner { pathInitialized = false }
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
        // Native Back can make an already hydrated stack appear again before
        // its path binding publishes. Rehydrating here would push the old route.
        guard !pathInitialized else { return }
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
            Section {
                MatAlarmIdentityHeader(
                    title: NativeAlarmPresentation.time(hour: model.state.alarmTime.hour, minute: model.state.alarmTime.minute),
                    subtitle: NativeEditorPresentation.weekdays(model.state),
                    eyebrow: NativeStrings.text("Alarm"),
                    emphasizesTime: true,
                    hour: Int(model.state.alarmTime.hour), minute: Int(model.state.alarmTime.minute))
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)
            }
            NativeEditorControls(model: model)
            if model.state.validation != .none {
                Section { Label(NativeStrings.validation(model.state.validation), systemImage: "exclamationmark.triangle") }
            }
            if model.state.isSaving {
                Section { ProgressView("Saving…").accessibilityIdentifier("editor-saving") }
            }
        }
        .matAlarmContent()
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
    /// Reuse Android's shared occurrence calculation, including recurrence,
    /// persisted one-time dates, skipped dates and snooze. Swift formats only.
    static func nextAlarmDate(_ alarms: [app.Alarm], now: Date) -> Date? {
        let clock = PresentationClock(now)
        let zone = Kotlinx_datetimeTimeZone.companion.currentSystemDefault()
        let next = alarms.filter { $0.isOn && $0.scheduleError == nil }.compactMap {
            AlarmUtilKt.calculateNextAlarmTime(alarm: $0, timeZone: zone, clock: clock)?.toEpochMilliseconds()
        }.min()
        return next.map { Date(timeIntervalSince1970: Double($0) / 1_000) }
    }

    static func nextAlarmSubtitle(_ alarms: [app.Alarm], now: Date) -> String {
        guard let next = nextAlarmDate(alarms, now: now) else { return "" }
        let day = DateFormatter()
        day.locale = .autoupdatingCurrent
        day.calendar = .autoupdatingCurrent
        day.timeZone = .autoupdatingCurrent
        day.dateStyle = .medium
        day.doesRelativeDateFormatting = true
        let components = Calendar.autoupdatingCurrent.dateComponents([.hour, .minute], from: next)
        let time = time(hour: Int32(components.hour ?? 0), minute: Int32(components.minute ?? 0))
        return NativeStrings.text("Next alarm:") + " " + day.string(from: next) + " · " + time
    }

    private final class PresentationClock: NSObject, KotlinClock {
        let date: Date
        init(_ date: Date) { self.date = date }
        func now() -> KotlinInstant {
            KotlinInstant.companion.fromEpochMilliseconds(epochMilliseconds: Int64(date.timeIntervalSince1970 * 1_000))
        }
    }

    struct ClockTime {
        let digits: String
        let period: String
        var formatted: String { "\(digits) \(period)" }
    }

    static func time(hour: Int32, minute: Int32) -> String {
        clockTime(hour: hour, minute: minute).formatted
    }

    /// Match Android's 12-hour display with localized day periods.
    /// A fixed Gregorian UTC date preserves the stored wall-clock components.
    static func clockTime(hour: Int32, minute: Int32, locale: Locale = .current) -> ClockTime {
        let zone = TimeZone.gmt
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = zone
        let date = calendar.date(from: DateComponents(year: 2001, month: 1, day: 1,
            hour: Int(hour), minute: Int(minute))) ?? Date(timeIntervalSinceReferenceDate: 0)
        let digits = date.formatted(.verbatim(
            "\(hour: .twoDigits(clock: .twelveHour, hourCycle: .oneBased)):\(minute: .twoDigits)",
            locale: locale, timeZone: zone, calendar: calendar))
        let period = date.formatted(.verbatim("\(dayPeriod: .standard(.abbreviated))",
            locale: locale, timeZone: zone, calendar: calendar)).uppercased(with: locale)
        return ClockTime(digits: digits, period: period)
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
