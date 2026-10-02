#if DEBUG
import SwiftUI
import UIKit
import KMPObservableViewModelSwiftUI
import KMPNativeCoroutinesAsync
import app

/// Opt-in rendered presentation evidence from the production native views.
/// Uses unsaved drafts, preview sessions and uniquely named disabled list fixtures.
/// Production Room/list behavior uses the existing controlled DEBUG scheduler;
/// this proves no real AlarmKit save/cancellation behavior. Cleanup deletes only fixture IDs.
@MainActor
enum NativePresentationVerification {
    static var enabled: Bool { ProcessInfo.processInfo.arguments.contains("--verify-m6-presentation") }

    private enum Screen { case editor, list, settings, announcements }
    // UIKit presentation hides ContentView, cancelling its SwiftUI .task.
    // The explicit DEBUG verification run has an independent process lifetime.
    private static var verificationTask: Task<Void, Never>?
    static func start(window: UIWindow, parent: UIViewController) {
        guard verificationTask == nil else {
            print("M6 PRESENTATION start ignored: once-only process run already exists")
            return
        }
        verificationTask = Task { @MainActor in await run(window: window, parent: parent) }
    }
    private static var failureCapture: (() async -> Void)?
    private static var failureContext: (() -> String)?

    private final class Fixture: ObservableObject, Identifiable {
        let id: String
        let model: AlarmSettingsViewModel
        @Published var ownerAttached = false
        init(id: String, model: AlarmSettingsViewModel) {
            self.id = id
            self.model = model
        }
    }

    private struct FixtureOwner: View {
        @ObservedObject var fixture: Fixture
        @StateViewModel var model: AlarmSettingsViewModel
        init(fixture: Fixture) {
            self.fixture = fixture
            _model = StateViewModel(wrappedValue: fixture.model)
        }
        var body: some View {
            Text(model.state.alarmTitle)
                .hidden().frame(width: 0, height: 0).accessibilityHidden(true).allowsHitTesting(false)
                .onAppear { fixture.ownerAttached = true }
                .onDisappear { fixture.ownerAttached = false }
        }
    }

    private final class Mount: ObservableObject {
        @Published var screen = Screen.editor
        @Published var largeText = false
        @Published var forcedRTL = false
        @Published var scenePresented = true
        var expectedDark = false
    }

    private struct Scene: View {
        @ObservedObject var mount: Mount
        @ObservedObject var sessions: NativeWindowSessions
        @ObservedObject var announcements: NativeAnnouncementPresentation
        @ObservedViewModel var settings: AppSettingsViewModel
        @ObservedViewModel var list: AlarmListViewModel
        let external: NativeExternalPresentation

        var body: some View {
            ZStack {
                switch mount.screen {
                case .editor:
                    NativeEditorStack(sessions: sessions)
                case .list:
                    NavigationStack {
                        NativeAlarmList(model: list, sessions: sessions,
                            openEditor: { _ in }, openSettings: {})
                    }
                case .settings:
                    NavigationStack {
                        NativeAppSettings(model: settings, openWhatsNew: {}, onDone: {},
                            external: external)
                    }
                case .announcements:
                    NavigationStack {
                        NativeWhatsNew(model: settings, presentation: announcements, onTryFeature: { _ in })
                    }
                }
            }
            .environment(\.dynamicTypeSize, mount.largeText ? .accessibility3 : .large)
            .environment(\.layoutDirection, mount.forcedRTL ? .rightToLeft : .leftToRight)
        }
    }

    /// Mount content at the production SwiftUI native presentation boundary.
    /// NativeWindowAppearance applies the retained preference to this own window.
    private struct Presenter: View {
        @ObservedObject var mount: Mount
        @ObservedObject var sessions: NativeWindowSessions
        @ObservedObject var announcements: NativeAnnouncementPresentation
        @StateViewModel var settings: AppSettingsViewModel
        @StateViewModel var list: AlarmListViewModel
        let external: NativeExternalPresentation
        let fixtures: [Fixture]

        var body: some View {
            ZStack {
                // Native owners outlive the presentation, exactly as in the
                // production window root. Presented screens only borrow them.
                NativeSessionOwners(sessions: sessions)
                Text("\(list.state.alarms.count)")
                    .hidden().frame(width: 0, height: 0).accessibilityHidden(true).allowsHitTesting(false)
                ForEach(fixtures) { fixture in
                    FixtureOwner(fixture: fixture).id(fixture.id)
                }
                Color.clear
            }
            .background(NativeWindowAppearance(theme: settings.state.theme)
                .frame(width: 0, height: 0).accessibilityHidden(true))
            .fullScreenCover(isPresented: $mount.scenePresented) {
                Scene(mount: mount, sessions: sessions, announcements: announcements,
                    settings: settings, list: list, external: external)
            }
        }
    }

    private struct DeliveredScene: View {
        @ObservedObject var mount: Mount
        @ObservedObject var sessions: NativeWindowSessions
        @StateViewModel var settings: AppSettingsViewModel
        @StateViewModel var list: AlarmListViewModel
        var body: some View {
            ZStack {
                NativeSessionOwners(sessions: sessions)
                Text("\(list.state.alarms.count)")
                    .hidden().frame(width: 0, height: 0).accessibilityHidden(true).allowsHitTesting(false)
                NativeEditorStack(sessions: sessions)
            }
            .background(NativeWindowAppearance(theme: settings.state.theme)
                .frame(width: 0, height: 0).accessibilityHidden(true))
            .environment(\.dynamicTypeSize, mount.largeText ? .accessibility3 : .large)
            .fullScreenCover(isPresented: Binding(get: { sessions.deliveryPresented },
                set: { sessions.deliveryPresented = $0 }), onDismiss: { sessions.presentationEnded() }) {
                NavigationStack { NativePendingDelivery(sessions: sessions) }
                    .environment(\.dynamicTypeSize, mount.largeText ? .accessibility3 : .large)
            }
        }
    }

    static func run(window: UIWindow, parent: UIViewController) async {
        setbuf(stdout, nil) // Preserve rendered evidence and timeout context on fixture failures.
        let scheduler = SharedBridgeVerification.VerificationScheduler()
        AlarmSchedulerBridge.shared.registerScheduler(scheduler: scheduler)
        defer {
            AlarmSchedulerBridge.shared.registerScheduler(scheduler: AlarmKitKotlinBridge(wrapper: AlarmKitWrapperImpl.shared))
        }
        print("M6 PRESENTATION independent DEBUG task started cancelled=\(Task.isCancelled)")
        precondition(!Task.isCancelled)
        if ProcessInfo.processInfo.arguments.contains("--verify-m6-delivered-presentation") {
            await runDelivered(window: window, parent: parent, scheduler: scheduler)
            return
        }
        let settings = SharedFeatures.shared.settings()
        let originalTheme = settings.state.theme
        let list = SharedFeatures.shared.list()
        let sessions = NativeWindowSessions()
        let announcements = NativeAnnouncementPresentation()
        let editor = sessions.openEditor(alarm: nil)
        let model = editor.model
        let fixtures = (0..<2).map { index in
            let id = "native-presentation-fixture/\(UUID().uuidString)"
            let fixture = SharedFeatures.shared.doNewEditor(sessionId: id)
            fixture.onEvent(event: AddEditAlarmEvent.ToggleEnabled(value: false))
            fixture.onEvent(event: AddEditAlarmEvent.EnteredTitle(value:
                "M6 \(index == 0 ? "Morning" : "Evening") \(UUID().uuidString.prefix(8))"))
            fixture.onEvent(event: AddEditAlarmEvent.ChangeTime(value: TimeState(hour: index == 0 ? 7 : 19, minute: 35)))
            fixture.onEvent(event: AddEditAlarmEvent.ToggleRepeat(value: index == 0))
            fixture.onEvent(event: AddEditAlarmEvent.ToggleDayChooser(value: index == 0 ? "FTFTFTF" : "FFFFFFF"))
            return Fixture(id: id, model: fixture)
        }
        model.onEvent(event: AddEditAlarmEvent.ToggleEnabled(value: false))
        model.onEvent(event: AddEditAlarmEvent.EnteredTitle(value: "M6 native presentation"))
        model.onEvent(event: AddEditAlarmEvent.ChangeTime(value: TimeState(hour: 9, minute: 45)))
        model.onEvent(event: AddEditAlarmEvent.ToggleRepeat(value: true))
        model.onEvent(event: AddEditAlarmEvent.ToggleDayChooser(value: "FTFTFTF"))
        model.onEvent(event: AddEditAlarmEvent.ChangeSnoozeDuration(minutes: 12))
        model.onEvent(event: AddEditAlarmEvent.ChangeMaxSnoozes(value: 0))
        defer {
            settings.selectTheme(theme: originalTheme)
            sessions.closeWindow()
            fixtures.forEach { SharedFeatures.shared.closeEditor(sessionId: $0.id) }
        }
        await wait("initial list readiness loading=\(list.state.loading) alarms=\(list.state.alarms.map(\.alarmId))") {
            !list.state.loading
        }
        // Prepare disabled persisted data before SwiftUI installs StateViewModel
        // owners, matching the production M5 fixture-save verification path.
        // Rendering below retains these same factory models as native owners.
        for fixture in fixtures {
            print("M6 FIXTURE_INITIAL id=\(fixture.id) isOn=\(fixture.model.state.isOn) snooze=\(fixture.model.state.snoozeMinutes) closed=\(fixture.model.isClosed) validation=\(fixture.model.state.validation) ownerAttached=\(fixture.ownerAttached)")
            precondition(!fixture.model.state.isOn && !fixture.model.isClosed)
            fixture.model.onEvent(event: AddEditAlarmEvent.OnSaveTodoClick.shared)
            await wait("disabled fixture save id=\(fixture.id) ownerAttached=\(fixture.ownerAttached) closed=\(fixture.model.isClosed) isOn=\(fixture.model.state.isOn) snooze=\(fixture.model.state.snoozeMinutes) validation=\(fixture.model.state.validation) saving=\(fixture.model.state.isSaving) savedID=\(String(describing: fixture.model.state.alarmId)) results=\(fixture.model.state.results.map { String(describing: type(of: $0.event)) })") {
                !fixture.model.state.isSaving && fixture.model.state.results.contains {
                    $0.event is AlarmSettingsViewModel.UiEventSaveAlarm
                }
            }
            for result in fixture.model.state.results { fixture.model.acknowledgeResult(id: result.id) }
        }
        let fixtureIDs = Set(fixtures.compactMap { $0.model.state.alarmId?.int64Value })
        precondition(fixtureIDs.count == 2)
        await wait("fixture list loading=\(list.state.loading) expected=\(fixtureIDs.sorted()) actual=\(list.state.alarms.map(\.alarmId))") {
            fixtureIDs.isSubset(of: Set(list.state.alarms.map(\.alarmId)))
        }
        precondition(list.state.alarms.filter { fixtureIDs.contains($0.alarmId) }
            .allSatisfy { !$0.isOn && $0.pendingTimes.isEmpty })
        precondition(scheduler.registrations.isEmpty)
        print("M6 PRESENTATION disabled fixture saves schedulerRegistrations=0 cancellations=\(scheduler.cancellations) realAlarmKitEvidence=false")

        let mount = Mount()
        let originalWindowStyle = window.overrideUserInterfaceStyle
        window.overrideUserInterfaceStyle = .unspecified
        defer { window.overrideUserInterfaceStyle = originalWindowStyle }
        await inspectLaunchStoryboard(window: window, parent: parent, mount: mount)
        let external = NativeExternalPresentation(openURL: { _, completion in completion(false) },
            canSendMail: { false })
        let host = UIHostingController(rootView: Presenter(mount: mount, sessions: sessions,
            announcements: announcements, settings: settings, list: list,
            external: external, fixtures: fixtures))
        failureCapture = {
            await capture(window, host: host, mount: mount, name: "presentation-timeout", includeBottom: false)
        }
        failureContext = {
            "selectedID=\(sessions.selectedEditorID ?? "none") selectedClosed=\(String(describing: sessions.selectedEditor?.model.isClosed)) retainedModelClosed=\(model.isClosed) initializationTasks=\(initializationTaskIDs(in: sessions)) editorCount=\(sessions.editors.count) editors=\(sessions.editors.map { $0.id }) applicationState=\(UIApplication.shared.applicationState.rawValue) sceneState=\(window.windowScene?.activationState.rawValue ?? -1) keyWindow=\(window.isKeyWindow) schedulerCancellations=\(scheduler.cancellations) hierarchy=\(controllerHierarchy(in: parent))"
        }
        defer { failureCapture = nil; failureContext = nil }
        host.modalPresentationStyle = .fullScreen
        await wait("outer presenter availability current=\(String(describing: parent.presentedViewController))") {
            parent.presentedViewController == nil
        }
        parent.present(host, animated: false)
        await wait("SwiftUI cover attachment controller=\(String(describing: host.presentedViewController)) window=\(String(describing: host.presentedViewController?.viewIfLoaded?.window))") {
            host.presentedViewController?.viewIfLoaded?.window === window
        }
        for fixture in fixtures {
            await wait("prepared fixture owner attachment id=\(fixture.id) attached=\(fixture.ownerAttached) closed=\(fixture.model.isClosed)") {
                fixture.ownerAttached && !fixture.model.isClosed
            }
            precondition(fixtureIDs.contains(fixture.model.state.alarmId!.int64Value))
        }

        for dark in [false, true] {
            mount.largeText = dark
            mount.expectedDark = dark
            settings.selectTheme(theme: dark ? .dark : .light)
            let suffix = dark ? "dark-accessibility3" : "light"
            mount.screen = .editor
            sessions.setEditorPath([], id: editor.id)
            await waitForTitle(NativeStrings.text("New alarm"), in: host)
            await wait("editor Form theme expected=\(dark ? "dark" : "light") windowOverride=\(window.overrideUserInterfaceStyle.rawValue) controls=\(NativeSettingsVerification.nativeControlAppearanceEvidence(in: activeRootView(in: host)))") {
                let forms = visibleNativeForms(in: activeRootView(in: host))
                return !forms.isEmpty && forms.allSatisfy { $0.traitCollection.userInterfaceStyle == (dark ? .dark : .light) }
            }
            await capture(window, host: host, mount: mount, name: "editor-\(suffix)")
            await destination(.repeatSettings, sessions: sessions, editorID: editor.id, host: host)
            await capture(window, host: host, mount: mount, name: "repeat-\(suffix)")

            setChallenge(model, difficulty: 1)
            await destination(.challenge, sessions: sessions, editorID: editor.id, host: host)
            await capture(window, host: host, mount: mount, name: "challenge-preset-\(suffix)")
            model.setChallengeMixing(enabled: true)
            model.setMixedQuestionCount(difficulty: 2, count: 2)
            await capture(window, host: host, mount: mount, name: "challenge-mixing-\(suffix)")
            model.setChallengeMixing(enabled: false)
            setChallenge(model, difficulty: 3)
            await capture(window, host: host, mount: mount, name: "challenge-custom-\(suffix)")
            await destination(.snooze, sessions: sessions, editorID: editor.id, host: host)
            await capture(window, host: host, mount: mount, name: "snooze-\(suffix)")
            await destination(.sound, sessions: sessions, editorID: editor.id, host: host)
            await capture(window, host: host, mount: mount, name: "sound-\(suffix)")
            let originalTone = model.state.tone
            sessions.soundSelection(sessionID: editor.id)?.finish()
            model.onEvent(event: AddEditAlarmEvent.OnToneChange(value: "native-presentation-missing-tone"))
            await destination(.sound, sessions: sessions, editorID: editor.id, host: host)
            await capture(window, host: host, mount: mount, name: "sound-missing-tone-fallback-\(suffix)")
            sessions.soundSelection(sessionID: editor.id)?.finish()
            model.onEvent(event: AddEditAlarmEvent.OnToneChange(value: originalTone))
            await destination(.preview, sessions: sessions, editorID: editor.id, host: host)
            await wait("preview readiness=\(String(describing: sessions.previews[editor.id]?.model.state.readiness)) error=\(String(describing: sessions.previews[editor.id]?.model.state.error)) route=\(sessions.editorPaths[editor.id] ?? [])") {
                sessions.previews[editor.id]?.model.state.readiness == .ready
            }
            // Readiness is not the completion of the native async observation.
            // Inspect the actual DEBUG-only Swift task dictionary and wait for
            // its initialization defer, rather than closing a ready in-flight task.
            await wait("preview native initialization completion tasks=\(initializationTaskIDs(in: sessions)) readiness=\(String(describing: sessions.previews[editor.id]?.model.state.readiness))") {
                initializationTaskIDs(in: sessions).isEmpty
            }
            print("M6 PRESENTATION preview native initialization completed tasks=0 previewID=\(sessions.previews[editor.id]!.id)")
            // These captures intentionally retain native input focus; external UI
            // tooling separately establishes software keyboard and Done behavior.
            await capture(window, host: host, mount: mount, name: "preview-\(suffix)")
            sessions.closePreview(editorID: editor.id)
            sessions.setEditorPath([], id: editor.id)
            await waitForTitle(NativeStrings.text("New alarm"), in: host)

            mount.screen = .settings
            await waitForTitle(NativeStrings.text("App Settings"), in: host)
            await capture(window, host: host, mount: mount, name: "settings-\(suffix)")
            announcements.reopen(model: settings)
            precondition(announcements.currentID != nil)
            mount.screen = .announcements
            await waitForTitle(NativeStrings.text("What’s new"), in: host)
            await capture(window, host: host, mount: mount, name: "whats-new-maths-\(suffix)")
            if announcements.ids.count > 1 {
                // An explicit browse action follows production acknowledgement
                // semantics. Mounting/capturing alone never acknowledges a page.
                announcements.move(to: 1, model: settings)
                await capture(window, host: host, mount: mount, name: "whats-new-snooze-\(suffix)")
            }
            announcements.interrupt()
            mount.screen = .list
            await wait("populated list loading=\(list.state.loading) alarms=\(list.state.alarms.map(\.alarmId))") { !list.state.loading }
            await waitForTitle(NativeStrings.text("Math Alarm"), in: host)
            await capture(window, host: host, mount: mount, name: "list-populated-\(suffix)")
            precondition(sessions.selectedEditor?.model === model && !model.isClosed)
            precondition(model.state.alarmTitle == "M6 native presentation" && model.state.hasUnsavedChanges)
        }

        // The same mounted native list owner performs typed commands and retains
        // results. Its independent DEBUG task survives native cover presentation.
        for fixtureID in fixtureIDs.sorted() {
            guard let fixture = list.state.alarms.first(where: { $0.alarmId == fixtureID }),
                  let owned = fixtures.first(where: { $0.model.state.alarmId?.int64Value == fixtureID }) else {
                preconditionFailure("Saved presentation fixture must remain in the authoritative list")
            }
            precondition(fixture.title == owned.model.state.alarmTitle && !fixture.isOn && fixture.pendingTimes.isEmpty)
            print("M6 PRESENTATION mounted cleanup exactID=\(fixtureID) cancelled=\(Task.isCancelled) schedulerCancellationsBefore=\(scheduler.cancellations)")
            list.onEvent(event: AlarmListEvent.OnDeleteAlarmClick(alarm: fixture))
            await wait("mounted fixture exact-ID deletion id=\(fixtureID) closed=\(list.isClosed) pending=\(list.state.pendingOperations) schedulerCancellations=\(scheduler.cancellations) actual=\(list.state.alarms.map(\.alarmId)) results=\(list.state.results.map { String(describing: type(of: $0.event)) })") {
                list.state.pendingOperations == 0 && !list.state.alarms.contains { $0.alarmId == fixtureID }
            }
            precondition(!list.state.results.contains { $0.event is UiEvent.ShowError })
            for result in list.state.results { list.acknowledgeResult(id: result.id) }
        }
        precondition(scheduler.registrations.isEmpty && fixtureIDs.isSubset(of: Set(scheduler.cancellations)))
        precondition(sessions.selectedEditor?.model === model && !model.isClosed)
        precondition(model.state.alarmTitle == "M6 native presentation" && model.state.hasUnsavedChanges)
        print("M6 PRESENTATION disabled fixtures removed exactIDs=\(fixtureIDs.sorted()) remaining=\(list.state.alarms.count) sameProcess=true retainedDraft=true")
        mount.screen = .list
        await waitForTitle(NativeStrings.text("Math Alarm"), in: host)
        await capture(window, host: host, mount: mount,
            name: list.state.alarms.isEmpty ? "list-empty-after-fixture-removal" : "list-after-fixture-removal")
        mount.screen = .settings
        mount.forcedRTL = true
        await waitForTitle(NativeStrings.text("App Settings"), in: host)
        await capture(window, host: host, mount: mount, name: "settings-synthetic-rtl")
        settings.selectTheme(theme: originalTheme)
        // Screenshot fixture teardown is deterministic and deliberately does
        // not establish production animation or Reduce Motion client behavior.
        let presentedCover = host.presentedViewController
        presentedCover?.dismiss(animated: false)
        var teardown = Transaction()
        teardown.disablesAnimations = true
        withTransaction(teardown) { mount.scenePresented = false }
        host.dismiss(animated: false)
        await wait("nonanimated fixture detach parent=\(String(describing: parent.presentedViewController)) hostAttached=\(host.viewIfLoaded?.window != nil) coverAttached=\(presentedCover?.viewIfLoaded?.window != nil)") {
            parent.presentedViewController == nil
                && host.viewIfLoaded?.window == nil
                && presentedCover?.viewIfLoaded?.window == nil
        }
        sessions.closeWindow()
        precondition(sessions.editors.isEmpty && model.isClosed)
        fixtures.forEach { SharedFeatures.shared.closeEditor(sessionId: $0.id) }
        precondition(fixtures.allSatisfy { $0.model.isClosed })
        print("M6 PRESENTATION native render owners closed after detached fixture window editors=0 fixtureModelsClosed=true cancelled=\(Task.isCancelled)")
        print("M6_PRESENTATION_VERIFICATION_PASSED locale=\(Locale.current.identifier) sameProcessTypedCleanup=true")
    }

    /// A separate opt-in phase reuses the M5 controlled occurrence path. It
    /// schedules only into VerificationScheduler, with private queue storage and
    /// a no-op native recovery hook; it proves no real AlarmKit registration.
    private static func runDelivered(window: UIWindow, parent: UIViewController,
                                     scheduler: SharedBridgeVerification.VerificationScheduler) async {
        let settings = SharedFeatures.shared.settings()
        let originalTheme = settings.state.theme
        let list = SharedFeatures.shared.list()
        await wait("delivered fixture initial list loading=\(list.state.loading)") { !list.state.loading }
        let fixtureID = "native-delivered-presentation/\(UUID().uuidString)"
        let fixtureTitle = "M6 Delivered / \(UUID().uuidString)"
        let fixture = SharedFeatures.shared.doNewEditor(sessionId: fixtureID)
        fixture.onEvent(event: AddEditAlarmEvent.EnteredTitle(value: fixtureTitle))
        fixture.onEvent(event: AddEditAlarmEvent.ChangeTime(value: TimeState(hour: 23, minute: 50)))
        fixture.onEvent(event: AddEditAlarmEvent.OnChallengeChange(value: MathChallenge(difficulty: 3,
            questionCount: 3, operations: "+", additionRange: 0, factorRange: 0, difficultyMix: "")))
        fixture.onEvent(event: AddEditAlarmEvent.ChangeSnoozeDuration(minutes: 5))
        fixture.onEvent(event: AddEditAlarmEvent.ChangeMaxSnoozes(value: 3))
        precondition(fixture.state.isOn)
        fixture.onEvent(event: AddEditAlarmEvent.OnSaveTodoClick.shared)
        await wait("controlled delivered save saving=\(fixture.state.isSaving) results=\(fixture.state.results.count)") {
            !fixture.state.isSaving && fixture.state.results.contains { $0.event is AlarmSettingsViewModel.UiEventSaveAlarm }
        }
        await wait("controlled delivered fixture list title=\(fixtureTitle)") {
            list.state.alarms.contains { $0.title == fixtureTitle }
        }
        let saved = list.state.alarms.first { $0.title == fixtureTitle }!
        precondition(saved.isOn && !saved.repeat && !saved.pendingTimes.isEmpty)
        print("M6 DELIVERED_FIXTURE id=\(saved.alarmId) title=\(fixtureTitle) repeat=false controlledSchedulerOnly=true")
        precondition(scheduler.registrations.values.contains { $0.alarmId == saved.alarmId })
        for result in fixture.state.results { fixture.acknowledgeResult(id: result.id) }
        SharedFeatures.shared.closeEditor(sessionId: fixtureID)
        precondition(fixture.isClosed)

        let suite = "MathAlarm.M6.DeliveredPresentation.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        let queue = PendingDeeplinkStore(userDefaults: defaults)
        let sessions = NativeWindowSessions(pendingStore: queue, restoreRecovery: { _ in })
        let draft = sessions.openEditor(alarm: nil)
        draft.model.onEvent(event: AddEditAlarmEvent.EnteredTitle(value: "M6 retained delivered draft"))
        sessions.setEditorPath([.challenge, .sound], id: draft.id)
        let staged = sessions.soundSelection(sessionID: draft.id)!
        staged.begin(currentTone: draft.model.state.tone)
        staged.choose("alarm_daybreak")
        precondition(sessions.beginPermissionRequest(id: draft.id))
        let draftSnapshot = draft.model.state
        let mount = Mount()
        let originalStyle = window.overrideUserInterfaceStyle
        window.overrideUserInterfaceStyle = .unspecified
        defer {
            settings.selectTheme(theme: originalTheme)
            window.overrideUserInterfaceStyle = originalStyle
            sessions.closeWindow()
            defaults.removePersistentDomain(forName: suite)
            SharedFeatures.shared.closeEditor(sessionId: fixtureID)
        }
        let host = UIHostingController(rootView: DeliveredScene(mount: mount, sessions: sessions,
            settings: settings, list: list))
        host.modalPresentationStyle = .fullScreen
        failureCapture = { await capture(window, host: host, mount: mount, name: "delivered-timeout", includeBottom: false) }
        failureContext = {
            "deliveredPhase=true cancelled=\(Task.isCancelled) editors=\(sessions.editors.count) draftClosed=\(draft.model.isClosed) actualInitializationTasks=\(initializationTaskIDs(in: sessions)) queue=\(String(describing: queue.peekPendingDeeplink())) schedulerCancellations=\(scheduler.cancellations) hierarchy=\(controllerHierarchy(in: parent))"
        }
        defer { failureCapture = nil; failureContext = nil }
        await wait("delivered host parent availability") { parent.presentedViewController == nil }
        parent.present(host, animated: false)
        await waitForTitle(NativeStrings.text("Alarm sound"), in: host)
        let activeAt = saved.pendingTimes.first!
        let payload = IosApplication.shared.createAlarmHandoffJson(alarmId: saved.alarmId,
            deliveryId: "m6-native-delivered-\(UUID().uuidString)", activeAt: activeAt)
        precondition(queue.setPendingDeeplink(payload))
        sessions.refreshPendingDelivery()
        await wait("delivered readiness and exact-head acknowledgement state=\(String(describing: sessions.challenge?.model.state.readiness)) queue=\(String(describing: queue.peekPendingDeeplink()))") {
            sessions.challenge?.model.state.readiness == .ready && !queue.hasPendingDeeplink()
                && initializationTaskIDs(in: sessions).isEmpty && !sessions.deliveryReplayInFlight
        }
        let delivered = sessions.challenge!
        precondition(!delivered.isPreview && delivered.model.state.occurrenceId != nil)
        precondition(delivered.model.state.alarm?.alarmId == saved.alarmId && delivered.model.state.alarm?.activeAt == activeAt)
        // Association/coalescing is intentionally repeating-only. The generic
        // unresolved-occurrence predicate covers this one-time presentation.
        let unresolved = try! await asyncFunction(for: IosApplication.shared.isUnresolvedOccurrence(
            alarmId: saved.alarmId, activeAt: activeAt.int64Value))
        precondition(unresolved.boolValue)
        let first = delivered.model.state.currentProblem!
        delivered.model.onEvent(event: MathScreenEvent.EnteredAnswer(value: String(first.answer)))
        delivered.model.submitAnswer(questionIndex: 0, problem: first)
        await wait("delivered persisted progress index=\(delivered.model.state.questionIndex)") {
            delivered.model.state.questionIndex == 1 && !delivered.model.state.finishing
        }
        precondition(delivered.model.state.alarm!.canSnooze)
        for dark in [false, true] {
            mount.largeText = dark
            mount.expectedDark = dark
            settings.selectTheme(theme: dark ? .dark : .light)
            await waitForTitle(NativeStrings.text("Solve maths"), in: host)
            await wait("delivered actual Form appearance expected=\(dark)") {
                let forms = visibleNativeForms(in: activeRootView(in: host))
                return !forms.isEmpty && forms.allSatisfy { $0.traitCollection.userInterfaceStyle == (dark ? .dark : .light) }
            }
            delivered.model.onEvent(event: MathScreenEvent.OnClearClick.shared)
            let suffix = dark ? "dark-accessibility3" : "light"
            await capture(window, host: host, mount: mount, name: "delivered-progress-\(suffix)")
            let problem = delivered.model.state.currentProblem!
            precondition(problem.answer != 999999)
            delivered.model.onEvent(event: MathScreenEvent.EnteredAnswer(value: "999999"))
            delivered.model.submitAnswer(questionIndex: 1, problem: problem)
            await wait("delivered normal feedback retained result") {
                (sessions.challengeFailures[delivered.id]?.outcome as? ChallengeOutcome.Failure)?.error == .incorrectAnswer
            }
            await capture(window, host: host, mount: mount, name: "delivered-feedback-\(suffix)")
            let failure = sessions.challengeFailures[delivered.id]!
            sessions.dismissChallengeFailure(id: delivered.id, resultID: failure.id)
            precondition(delivered.model.state.questionIndex == 1)
            precondition(sessions.selectedEditor?.model === draft.model && draft.model.state == draftSnapshot)
            precondition(sessions.editorPaths[draft.id] == [.challenge, .sound]
                && sessions.permissionRequests.contains(draft.id) && staged.pendingTone == "alarm_daybreak")
        }
        for index in 1..<Int(delivered.model.state.questionCount) {
            let problem = delivered.model.state.currentProblem!
            delivered.model.onEvent(event: MathScreenEvent.EnteredAnswer(value: String(problem.answer)))
            delivered.model.submitAnswer(questionIndex: Int32(index), problem: problem)
            await wait("delivered accepted answer index=\(index)") {
                delivered.model.isClosed || delivered.model.state.questionIndex == Int32(index + 1)
            }
        }
        await wait("delivered authoritative completion and native dismissal") {
            delivered.model.isClosed && sessions.challenge == nil && !sessions.deliveryPresented
                && host.presentedViewController == nil
        }
        await waitForTitle(NativeStrings.text("Alarm sound"), in: host)
        precondition(delivered.model.state.results.isEmpty && !queue.hasPendingDeeplink())
        precondition(sessions.selectedEditor?.model === draft.model && !draft.model.isClosed && draft.model.state == draftSnapshot)
        precondition(sessions.soundSelection(sessionID: draft.id) === staged && staged.pendingTone == "alarm_daybreak")
        precondition(sessions.editorPaths[draft.id] == [.challenge, .sound] && sessions.permissionRequests.contains(draft.id))
        await capture(window, host: host, mount: mount, name: "delivered-returned-draft")
        sessions.endPermissionRequest(id: draft.id)
        await wait("completed delivered fixture disabled in authoritative list") {
            list.state.alarms.contains { $0.alarmId == saved.alarmId && !$0.isOn && $0.activeAt == nil }
        }
        let completed = list.state.alarms.first { $0.alarmId == saved.alarmId }!
        precondition(completed.title == fixtureTitle)
        list.onEvent(event: AlarmListEvent.OnDeleteAlarmClick(alarm: completed))
        await wait("delivered fixture exact-ID typed deletion pending=\(list.state.pendingOperations)") {
            list.state.pendingOperations == 0 && !list.state.alarms.contains { $0.alarmId == saved.alarmId }
        }
        precondition(!list.state.results.contains { $0.event is UiEvent.ShowError })
        for result in list.state.results { list.acknowledgeResult(id: result.id) }
        precondition(!scheduler.registrations.values.contains { $0.alarmId == saved.alarmId })
        settings.selectTheme(theme: originalTheme)
        host.dismiss(animated: false)
        await wait("delivered fixture native host detachment") {
            parent.presentedViewController == nil && host.viewIfLoaded?.window == nil
        }
        sessions.closeWindow()
        precondition(draft.model.isClosed && sessions.editors.isEmpty)
        print("M6_DELIVERED_PRESENTATION_PASSED locale=\(Locale.current.identifier) exactID=\(saved.alarmId) readyBeforeExactACK=true resultACK=true sameDraft=true stagedRoute=true controlledSchedulerOnly=true noNativeRecovery=true")
    }

    private static func setChallenge(_ model: AlarmSettingsViewModel, difficulty: Int32) {
        model.onEvent(event: AddEditAlarmEvent.OnChallengeChange(value: MathChallenge(
            difficulty: difficulty, questionCount: 3, operations: "+−×÷", additionRange: 2,
            factorRange: 1, difficultyMix: "")))
    }

    /// This renders the packaged launch storyboard at runtime. It establishes
    /// native asset/color/layout evidence, not actual cold-launch timing.
    private static func inspectLaunchStoryboard(window: UIWindow, parent: UIViewController, mount: Mount) async {
        guard let launch = UIStoryboard(name: "LaunchScreen", bundle: .main).instantiateInitialViewController() else {
            preconditionFailure("Packaged native launch storyboard must have an initial controller")
        }
        let originalAppearance = window.overrideUserInterfaceStyle
        parent.addChild(launch)
        parent.view.addSubview(launch.view)
        launch.view.frame = parent.view.bounds
        launch.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        launch.didMove(toParent: parent)
        defer {
            window.overrideUserInterfaceStyle = originalAppearance
            launch.willMove(toParent: nil)
            launch.view.removeFromSuperview()
            launch.removeFromParent()
        }
        func imageView(in view: UIView) -> UIImageView? {
            if let image = view as? UIImageView { return image }
            return view.subviews.compactMap { imageView(in: $0) }.first
        }
        for dark in [false, true] {
            window.overrideUserInterfaceStyle = dark ? .dark : .light
            launch.overrideUserInterfaceStyle = dark ? .dark : .light
            launch.view.setNeedsLayout()
            launch.view.layoutIfNeeded()
            await wait("rendered launch style expected=\(dark ? "dark" : "light") actual=\(launch.traitCollection.userInterfaceStyle.rawValue)") {
                launch.traitCollection.userInterfaceStyle == (dark ? .dark : .light)
            }
            guard let artwork = imageView(in: launch.view), let image = artwork.image?.cgImage,
                  let background = launch.view.backgroundColor?.resolvedColor(with: launch.traitCollection) else {
                preconditionFailure("Rendered launch storyboard must expose its native artwork and background")
            }
            precondition(artwork.window === window && !artwork.isHidden && artwork.alpha > 0)
            precondition(artwork.bounds.width > 0 && artwork.bounds.height > 0)
            var pixels = [UInt8](repeating: 0, count: image.width * image.height * 4)
            let transparentCorners = pixels.withUnsafeMutableBytes { buffer -> Bool in
                guard let context = CGContext(data: buffer.baseAddress, width: image.width, height: image.height,
                    bitsPerComponent: 8, bytesPerRow: image.width * 4, space: CGColorSpaceCreateDeviceRGB(),
                    bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue | CGBitmapInfo.byteOrder32Big.rawValue) else {
                    preconditionFailure("Native launch artwork inspection requires an RGBA context")
                }
                context.draw(image, in: CGRect(x: 0, y: 0, width: CGFloat(image.width), height: CGFloat(image.height)))
                let bytes = buffer.bindMemory(to: UInt8.self)
                return [0, image.width - 1, (image.height - 1) * image.width, image.width * image.height - 1]
                    .allSatisfy { bytes[$0 * 4 + 3] == 0 }
            }
            precondition(transparentCorners, "Native launch artwork must retain transparent corners")
            var red: CGFloat = 0, green: CGFloat = 0, blue: CGFloat = 0, alpha: CGFloat = 0
            precondition(background.getRed(&red, green: &green, blue: &blue, alpha: &alpha))
            precondition(alpha > 0.99 && (dark ? max(red, max(green, blue)) < 0.1 : min(red, min(green, blue)) > 0.9))
            await capture(window, host: launch, mount: mount,
                name: "launch-storyboard-rendered-\(dark ? "dark" : "light")")
            print("M6 LAUNCH_STORYBOARD_RENDERED appearance=\(dark ? "dark" : "light") artwork=\(image.width)x\(image.height) transparentCorners=\(transparentCorners) background=\(red),\(green),\(blue),\(alpha) artworkFrame=\(NSCoder.string(for: artwork.convert(artwork.bounds, to: window))) coldLaunchTiming=false")
        }
    }

    private static func destination(_ destination: NativeEditorDestination, sessions: NativeWindowSessions,
                                    editorID: String, host: UIViewController) async {
        sessions.setEditorPath([], id: editorID)
        await waitForTitle(NativeStrings.text("New alarm"), in: host)
        sessions.setEditorPath([destination], id: editorID)
        await waitForTitle(NativeStrings.text(destination.rawValue), in: host)
    }

    private static func waitForTitle(_ title: String, in host: UIViewController) async {
        await wait("native destination title expected=\(title) actual=\(visibleNavigation(in: host)?.navigationItem.title ?? "none") activeRoot=\(String(describing: type(of: activeRootView(in: host))))") {
            visibleNavigation(in: host)?.navigationItem.title == title
        }
    }

    private static func visibleNavigation(in controller: UIViewController) -> UIViewController? {
        if let presented = controller.presentedViewController, !presented.isBeingDismissed,
           let visible = visibleNavigation(in: presented) { return visible }
        if let navigation = controller as? UINavigationController,
           // SwiftUI can retain a coordinator after its visible destination settles.
           // Attached actual top/visible native controller establishes readiness.
           navigation.viewIfLoaded?.window != nil,
           let visible = navigation.visibleViewController, visible === navigation.topViewController,
           visible.viewIfLoaded?.window != nil { return visible }
        return controller.children.compactMap { visibleNavigation(in: $0) }.first
    }

    private static func activeRootView(in controller: UIViewController) -> UIView {
        if let presented = controller.presentedViewController, !presented.isBeingDismissed,
           presented.viewIfLoaded?.window != nil { return activeRootView(in: presented) }
        return controller.view
    }

    private static func wait(_ context: @autoclosure () -> String, _ predicate: () -> Bool) async {
        let started = ProcessInfo.processInfo.systemUptime
        while ProcessInfo.processInfo.systemUptime - started < 12 {
            if Task.isCancelled {
                print("M6 PRESENTATION_TASK_CANCELLED \(context())")
                preconditionFailure("Independent DEBUG verification task was cancelled; this is not a presentation timeout")
            }
            if predicate() { return }
            do { try await Task.sleep(for: .milliseconds(100)) }
            catch {
                print("M6 PRESENTATION_TASK_SLEEP_CANCELLED \(context()) error=\(error)")
                preconditionFailure("Independent DEBUG verification task sleep was cancelled; this is not a presentation timeout")
            }
        }
        let diagnostic = context() + " " + (failureContext?() ?? "")
            + " cancelled=\(Task.isCancelled) elapsedSeconds=\(ProcessInfo.processInfo.systemUptime - started)"
        print("M6 PRESENTATION_WAIT_FAILURE \(diagnostic)")
        await failureCapture?()
        preconditionFailure("Native presentation did not settle within 12 seconds: \(diagnostic)")
    }

    /// DEBUG reflection observes the existing Swift task storage only. It does
    /// not expose a production API or substitute readiness state for task return.
    private static func initializationTaskIDs(in sessions: NativeWindowSessions) -> [String] {
        guard let tasks = Mirror(reflecting: sessions).children
            .first(where: { $0.label == "initializationTasks" })?.value as? [String: Task<Void, Never>] else {
            preconditionFailure("Native presentation verification must inspect the actual initialization task storage")
        }
        return tasks.keys.sorted()
    }

    private static func controllerHierarchy(in root: UIViewController, depth: Int = 0) -> String {
        let attached = root.viewIfLoaded?.window != nil
        let current = "\(depth):\(String(describing: type(of: root))) title=\(root.navigationItem.title ?? "none") attached=\(attached) dismissing=\(root.isBeingDismissed)"
        let children = root.children.map { controllerHierarchy(in: $0, depth: depth + 1) }
        let presented = root.presentedViewController.map {
            "presented=" + controllerHierarchy(in: $0, depth: depth + 1)
        }
        return ([current] + children + [presented].compactMap { $0 }).joined(separator: " | ")
    }

    private static func visibleNativeForms(in root: UIView) -> [UIView] {
        guard !root.isHidden, root.alpha > 0.01, root.window != nil else { return [] }
        let current: [UIView] = root is UITableView || root is UICollectionView ? [root] : []
        return current + root.subviews.flatMap { visibleNativeForms(in: $0) }
    }

    private static func capture(_ window: UIWindow, host: UIViewController, mount: Mount, name: String,
                                includeBottom: Bool = true) async {
        precondition(!Task.isCancelled, "Rendered evidence requires an active independent DEBUG task")
        // Wait for native Form and navigation transitions before drawing the
        // pane. A 300ms snapshot can still show the outgoing editor at the edge.
        let settlingMilliseconds = 900
        do { try await Task.sleep(for: .milliseconds(settlingMilliseconds)) }
        catch { preconditionFailure("Rendered evidence settling sleep was cancelled: \(error)") }
        window.layoutIfNeeded()
        let image = UIGraphicsImageRenderer(bounds: window.bounds).image { _ in
            window.drawHierarchy(in: window.bounds, afterScreenUpdates: true)
        }
        let directory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("native-ui-m6-presentation", isDirectory: true)
            .appendingPathComponent(Locale.current.identifier, isDirectory: true)
        try! FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        try! image.pngData()!.write(to: directory.appendingPathComponent(name + ".png"))
        let visible = visibleNavigation(in: host)
        let renderedRoot = activeRootView(in: host)
        let metadata: [String: Any] = [
            "locale": Locale.current.identifier, "preferredLanguages": Locale.preferredLanguages,
            "windowFrame": NSCoder.string(for: window.frame), "safeArea": String(describing: window.safeAreaInsets),
            "visibleTitle": visible?.navigationItem.title ?? "", "contentFrame": NSCoder.string(for: visible?.view.frame ?? .zero),
            "expectedTheme": name.hasPrefix("launch-storyboard-rendered-")
                ? (name.hasSuffix("dark") ? "dark" : "light") : (mount.expectedDark ? "dark" : "light"),
            "hostingStyle": host.traitCollection.userInterfaceStyle.rawValue,
            "nativeControlAppearance": NativeSettingsVerification.nativeControlAppearanceEvidence(in: renderedRoot),
            "renderedFormStyles": visibleNativeForms(in: renderedRoot).map { $0.traitCollection.userInterfaceStyle.rawValue },
            "nativeScrollViews": visibleScrollViews(in: renderedRoot).map {
                "\(String(describing: type(of: $0))) viewport=\(NSCoder.string(for: $0.bounds)) contentSize=\(NSCoder.string(for: $0.contentSize)) offset=\(NSCoder.string(for: $0.contentOffset))"
            },
            "accessibility3": mount.largeText,
            "forcedRTLSynthetic": mount.forcedRTL,
            "verificationTaskCancelled": Task.isCancelled,
            "applicationState": UIApplication.shared.applicationState.rawValue,
            "sceneActivationState": window.windowScene?.activationState.rawValue ?? -1,
            "keyWindow": window.isKeyWindow,
            "reduceMotionActual": UIAccessibility.isReduceMotionEnabled,
            "reduceTransparencyActual": UIAccessibility.isReduceTransparencyEnabled,
            "renderedLaunchStoryboard": name.hasPrefix("launch-storyboard-rendered-"),
            "screenScale": window.screen.scale,
            "controllerHierarchy": controllerHierarchy(in: host),
            "retainedSessionDiagnostics": failureContext?() ?? ""
        ]
        try! JSONSerialization.data(withJSONObject: metadata, options: [.prettyPrinted, .sortedKeys])
            .write(to: directory.appendingPathComponent(name + ".json"))
        print("M6 CAPTURE \(Locale.current.identifier)/\(name) \(metadata["windowFrame"]!) title=\(metadata["visibleTitle"]!)")
        // Inspect the real lower viewport of tall native content. Scrolling is
        // presentation-only and the original offset is restored before navigation.
        if includeBottom, !name.hasPrefix("launch-storyboard-"),
           let scroll = visibleScrollViews(in: renderedRoot)
            .filter({ $0.isScrollEnabled && $0.bounds.height > 150 })
            .max(by: { $0.bounds.width * $0.bounds.height < $1.bounds.width * $1.bounds.height }) {
            let originalOffset = scroll.contentOffset
            let lowerOffset = max(-scroll.adjustedContentInset.top,
                scroll.contentSize.height - scroll.bounds.height + scroll.adjustedContentInset.bottom)
            if lowerOffset > originalOffset.y + 12 {
                scroll.setContentOffset(CGPoint(x: originalOffset.x, y: lowerOffset), animated: false)
                scroll.layoutIfNeeded()
                await capture(window, host: host, mount: mount, name: name + "-bottom", includeBottom: false)
                scroll.setContentOffset(originalOffset, animated: false)
                scroll.layoutIfNeeded()
            }
        }
    }

    private static func visibleScrollViews(in root: UIView) -> [UIScrollView] {
        guard !root.isHidden, root.alpha > 0.01, root.window != nil else { return [] }
        let current = (root as? UIScrollView).map { [$0] } ?? []
        return current + root.subviews.flatMap { visibleScrollViews(in: $0) }
    }
}
#endif
