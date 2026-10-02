import SwiftUI
import UIKit
import Observation
import KMPObservableViewModelCore
import KMPObservableViewModelSwiftUI
import KMPNativeCoroutinesAsync
import app

// One conformance for every production shared feature model.
extension app.ViewModel: @retroactive KMPObservableViewModelCore.ViewModel { }
extension app.ViewModel: @retroactive Observable { }

#if DEBUG
/// Production-app integration checks. Only an explicit launch argument enables them.
/// Disabled fixture alarms exercise native save/list flows in disposable storage.
/// No alarms are scheduled, delivered, completed, or snoozed by this harness.
@MainActor
enum SharedBridgeVerification {
    static var enabled: Bool { ProcessInfo.processInfo.arguments.contains("--verify-shared-bridge") }

    private final class MountState: ObservableObject {
        @Published var visible = true
        @Published var alternateDetail = false
        @Published var largeText = false
        @Published var list: AlarmListViewModel?
        var renderedDetail = ""
        var observationInvalidated = false
    }

    private struct NativeScene: View {
        @ObservedObject var mount: MountState
        @ObservedObject var sessions: NativeWindowSessions
        var body: some View {
            ZStack {
                NativeSessionOwners(sessions: sessions)
                if let list = mount.list {
                    NativeListVerification(model: list, sessions: sessions)
                } else if mount.alternateDetail {
                    NavigationSplitView(preferredCompactColumn: .constant(.detail)) {
                        Text("Verification sidebar")
                    } detail: {
                        detail(expanded: true)
                    }.id("expanded")
                } else {
                    detail(expanded: false).id("compact")
                }
            }
        }

        private func detail(expanded: Bool) -> some View {
            NativeEditorStack(sessions: sessions, onDestinationAppeared: { id, observer, destination in
                // Outgoing native transitions can appear after a replacement detail.
                // Only the selected stack and its current route establish readiness.
                guard sessions.ownsNavigation(id: id, observer: observer),
                      sessions.editorPaths[id]?.last == destination else { return }
                print("BRIDGE TRACE destination \(id) \(destination?.rawValue ?? "editor") path=\(sessions.editorPaths[id] ?? [])")
                mount.renderedDetail = "\(expanded)/\(id)/\(destination?.rawValue ?? "editor")"
            })
        }
    }

    private struct Mount: View {
        @ObservedObject var mount: MountState
        let sessions: NativeWindowSessions?
        var body: some View {
            if mount.visible, let sessions {
                NativeScene(mount: mount, sessions: sessions)
                    .environment(\.dynamicTypeSize, mount.largeText ? .accessibility3 : .large)
            }
        }
    }

    private struct AutomaticOwnerProbe: View {
        @StateViewModel var model: AlarmSettingsViewModel
        var body: some View { Text(model.state.alarmTitle) }
    }

    private struct NativeListVerification: View {
        @StateViewModel var model: AlarmListViewModel
        @ObservedObject var sessions: NativeWindowSessions
        var body: some View {
            NavigationStack {
                NativeAlarmList(model: model, sessions: sessions,
                    openEditor: { sessions.openEditor(alarm: $0) }, openSettings: {})
            }
        }
    }

    static func run() async {
        setbuf(stdout, nil) // Preserve the failing group in simulator crash transcripts.
        let handoff = IosApplication.shared.createAlarmHandoffJson(
            alarmId: 7, deliveryId: "bridge-verification-token", activeAt: KotlinLong(value: 1000))
        let encoded = try! JSONSerialization.jsonObject(with: Data(handoff.utf8)) as! [String: Any]
        precondition(encoded["alarmId"] as? Int == 7)
        precondition(encoded["activeAt"] as? Int == 1000)
        precondition(encoded["version"] as? Int == 2)
        precondition(encoded["deliveryId"] as? String == "bridge-verification-token")
        let decoded = IosApplication.shared.decodeAlarmHandoffJson(payload: handoff)!
        precondition(decoded.alarmId == 7 && decoded.activeAt?.int64Value == 1000)
        precondition(decoded.deliveryId == "bridge-verification-token")
        precondition(IosApplication.shared.decodeAlarmHandoffJson(payload: "invalid payload") == nil)
        let missingSession = "bridge-verification/missing-occurrence/\(UUID().uuidString)"
        let missingChallenge = SharedFeatures.shared.challenge(sessionId: missingSession)
        let missingReady = try! await asyncFunction(for: missingChallenge.initializeOccurrence(
            alarmId: Int64.min, activeAt: KotlinLong(value: 1000)))
        precondition(!missingReady.boolValue && missingChallenge.state.readiness == .error)
        precondition(missingChallenge.state.occurrenceId == nil && missingChallenge.state.alarm == nil)
        SharedFeatures.shared.closeChallenge(sessionId: missingSession)
        precondition(missingChallenge.isClosed)
        print("BRIDGE PASS UI-free bootstrap, versioned codec and identity-only missing-occurrence result")
        let suite = "bridge-verification/\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        let pendingStore = PendingDeeplinkStore(userDefaults: defaults)
        var sessions: NativeWindowSessions? = NativeWindowSessions(pendingStore: pendingStore)
        let editor = sessions!.openEditor(alarm: nil)
        let session = editor.id
        let model = editor.model
        let mount = MountState()
        guard let window = UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene })
            .flatMap(\.windows).first(where: \.isKeyWindow), let parent = window.rootViewController else {
            fatalError("Bridge verification requires the running app's mounted window")
        }
        var host: UIHostingController<Mount>? = UIHostingController(rootView: Mount(mount: mount, sessions: sessions))
        parent.addChild(host!)
        parent.view.addSubview(host!.view)
        host!.view.frame = parent.view.bounds
        host!.didMove(toParent: parent)
        await wait { mount.renderedDetail == "false/\(session)/editor" && navigationSettled(in: host!) }

        withObservationTracking { _ = model.state.alarmTitle } onChange: {
            Task { @MainActor in mount.observationInvalidated = true }
        }
        model.onEvent(event: AddEditAlarmEvent.EnteredTitle(value: "Retained Swift draft"))
        await wait { mount.observationInvalidated }
        precondition(model.state.alarmTitle == "Retained Swift draft")
        precondition(model.state.hasUnsavedChanges)
        print("BRIDGE PASS typed edits and Swift Observation")
        capture(window, name: "editor")

        precondition(sessions!.beginPermissionRequest(id: session))
        precondition(!sessions!.beginPermissionRequest(id: session))
        mount.alternateDetail = true
        await wait { mount.renderedDetail == "true/\(session)/editor" && navigationSettled(in: host!) }
        precondition(SharedFeatures.shared.doNewEditor(sessionId: session) === model)
        precondition(model.state.alarmTitle == "Retained Swift draft")
        precondition(!model.isClosed)
        mount.alternateDetail = false
        await wait { mount.renderedDetail == "false/\(session)/editor" && navigationSettled(in: host!) }
        precondition(sessions!.openEditor(alarm: nil).model === model && !model.isClosed)
        precondition(sessions!.permissionRequests.contains(session))
        sessions!.endPermissionRequest(id: session)
        print("BRIDGE PASS production editor owner survives compact-expanded-compact layout")

        model.onEvent(event: AddEditAlarmEvent.OnTestClick.shared)
        let previewResult = model.state.results.last!
        let previewDraft = (previewResult.event as! AlarmSettingsViewModel.UiEventTestAlarm).alarm
        model.acknowledgeResult(id: previewResult.id)
        let replacement = sessions!.openEditor(alarm: previewDraft)
        await wait { mount.renderedDetail == "false/\(replacement.id)/editor" && navigationSettled(in: host!) }
        replacement.model.onEvent(event: AddEditAlarmEvent.EnteredTitle(value: "Second retained draft"))
        sessions!.setEditorPath([.sound], id: replacement.id)
        await wait { mount.renderedDetail == "false/\(replacement.id)/Sound library" && navigationSettled(in: host!) }
        sessions!.selectEditor(id: session)
        await wait { mount.renderedDetail == "false/\(session)/editor" && navigationSettled(in: host!) }
        precondition(model.state.alarmTitle == "Retained Swift draft" && !model.isClosed)
        precondition(replacement.model.state.alarmTitle == "Second retained draft" && !replacement.model.isClosed)
        precondition(sessions!.editorPaths[replacement.id] == [.sound])
        sessions!.setEditorPath([.challenge], id: session)
        await wait { mount.renderedDetail == "false/\(session)/Challenge settings" && navigationSettled(in: host!) }
        capture(window, name: "challenge")
        mount.alternateDetail = true
        await wait { mount.renderedDetail == "true/\(session)/Challenge settings" && navigationSettled(in: host!) }
        sessions!.selectEditor(id: replacement.id)
        await wait { mount.renderedDetail == "true/\(replacement.id)/Sound library" && navigationSettled(in: host!) }
        sessions!.selectEditor(id: session)
        await wait { mount.renderedDetail == "true/\(session)/Challenge settings" && navigationSettled(in: host!) }
        mount.alternateDetail = false
        await wait { mount.renderedDetail == "false/\(session)/Challenge settings" && navigationSettled(in: host!) }
        sessions!.setEditorPath([], id: session)
        // SwiftUI can keep the root mounted throughout a push/pop, so a second
        // root onAppear is not a rendering signal. Inspect the visible UIKit stack.
        await wait { visibleEditor(in: host!, title: model.state.alarmTitle) }
        precondition(sessions!.editorPaths[session] == [])
        sessions!.closeEditor(id: replacement.id)
        await wait { replacement.model.isClosed }
        precondition(!model.isClosed)
        print("BRIDGE PASS production detail and nested destination replacement retain drafts")

        model.onEvent(event: AddEditAlarmEvent.ChangeTime(value: TimeState(hour: 9, minute: 45)))
        model.onEvent(event: AddEditAlarmEvent.ToggleRepeat(value: true))
        model.onEvent(event: AddEditAlarmEvent.ToggleDayChooser(value: "FTFTFTF"))
        model.onEvent(event: AddEditAlarmEvent.OnChallengeChange(value: MathChallenge(
            difficulty: 1, questionCount: 3, operations: "+−×÷", additionRange: 0, factorRange: 0, difficultyMix: "")))
        model.setChallengeMixing(enabled: true)
        model.setMixedQuestionCount(difficulty: 2, count: 2)
        precondition(model.state.challenge.questionCount == 5 && model.state.challenge.difficultyMix == "11122")
        model.setChallengeMixing(enabled: false)
        model.onEvent(event: AddEditAlarmEvent.OnChallengeChange(value: MathChallenge(
            difficulty: 3, questionCount: 4, operations: "+×", additionRange: 2, factorRange: 1, difficultyMix: "")))
        model.onEvent(event: AddEditAlarmEvent.ChangeSnoozeDuration(minutes: 12))
        model.onEvent(event: AddEditAlarmEvent.ChangeMaxSnoozes(value: 0))
        for destination in [NativeEditorDestination.repeatSettings, .challenge, .snooze, .preview] {
            sessions!.setEditorPath([], id: session)
            await wait { visibleEditor(in: host!, title: model.state.alarmTitle) }
            sessions!.setEditorPath([destination], id: session)
            await wait { mount.renderedDetail == "false/\(session)/\(destination.rawValue)" && navigationSettled(in: host!) }
            precondition(model.state.alarmTime.hour == 9 && model.state.alarmTime.minute == 45)
            precondition(model.state.dayChooser == "FTFTFTF" && model.state.repeatWeekly)
            precondition(model.state.challenge.operations == "+×" && model.state.snoozeMinutes == 12)
            capture(window, name: destination == .preview ? "m5-preview-development" : destination.rawValue.lowercased())
        }
        sessions!.setEditorPath([], id: session)
        await wait { visibleEditor(in: host!, title: model.state.alarmTitle) }
        sessions!.setEditorPath([.sound], id: session)
        await wait { mount.renderedDetail == "false/\(session)/Sound library" && navigationSettled(in: host!) }
        capture(window, name: "sound")
        let sound = sessions!.soundSelection(sessionID: session)!
        let originalTone = model.state.tone
        sound.choose("alarm_daybreak")
        precondition(model.state.tone == originalTone && sound.hasChanges)
        mount.alternateDetail = true
        await wait { mount.renderedDetail == "true/\(session)/Sound library" && navigationSettled(in: host!) }
        precondition(sessions!.soundSelection(sessionID: session) === sound && sound.pendingTone == "alarm_daybreak")
        let oldPresentation = sound.beginPresentation()
        let newPresentation = sound.beginPresentation()
        sound.togglePreview("alarm_orbit")
        precondition(sound.previewingTone == "alarm_orbit" && sound.previewMessage == nil)
        sound.endPresentation(oldPresentation)
        precondition(sound.previewingTone == "alarm_orbit")
        sound.endPresentation(newPresentation)
        precondition(sound.previewingTone == nil)
        sound.finish() // Explicit Back/discard leaves Kotlin selection untouched.
        sound.begin(currentTone: model.state.tone)
        precondition(sound.pendingTone == originalTone && !sound.hasChanges)
        sound.choose("alarm_rally")
        model.onEvent(event: AddEditAlarmEvent.OnToneChange(value: sound.pendingTone)) // Same Done action.
        sound.finish()
        precondition(model.state.tone == "alarm_rally" && model.state.hasUnsavedChanges)
        mount.alternateDetail = false
        await wait { mount.renderedDetail == "false/\(session)/Sound library" && navigationSettled(in: host!) }
        sessions!.setEditorPath([], id: session)
        await wait { visibleEditor(in: host!, title: model.state.alarmTitle) }
        window.overrideUserInterfaceStyle = .dark
        mount.largeText = true
        await Task.yield()
        host!.view.layoutIfNeeded()
        capture(window, name: "editor-dark-dynamic-type")
        mount.largeText = false
        window.overrideUserInterfaceStyle = .light
        await wait { host!.traitCollection.userInterfaceStyle == .light }
        await exerciseKeyboard(in: host!.view, title: model.state.alarmTitle, window: window)
        print("BRIDGE PASS native editor subpages and staged sound survive presentation replacement")

        pendingStore.setPendingDeeplink(handoff)
        let laterHandoff = IosApplication.shared.createAlarmHandoffJson(
            alarmId: 8, deliveryId: "later-verification-token", activeAt: KotlinLong(value: 2000))
        pendingStore.setPendingDeeplink(laterHandoff)
        let queuedData = defaults.data(forKey: "MathAlarm.pendingAlarmHandoffs.v1")!
        sessions!.refreshPendingDelivery()
        let challenge = sessions!.challenge!
        await Task.yield()
        sessions!.deliveryPresented = false
        sessions!.refreshPendingDelivery()
        precondition(sessions!.challenge!.model === challenge.model)
        precondition(challenge.model.state.readiness == .idle && !challenge.model.isClosed)
        precondition(PendingDeeplinkStore(userDefaults: defaults).peekPendingDeeplink() == handoff)
        precondition(!pendingStore.acknowledgePendingDeeplink(laterHandoff))
        precondition(defaults.data(forKey: "MathAlarm.pendingAlarmHandoffs.v1") == queuedData)
        precondition(model.state.alarmTitle == "Retained Swift draft" && !model.isClosed)
        print("BRIDGE PASS production challenge placeholder preserves durable ordered deliveries")

        model.onEvent(event: AddEditAlarmEvent.ChangeTime(value: TimeState(hour: 25, minute: 0)))
        model.onEvent(event: AddEditAlarmEvent.OnSaveTodoClick.shared)
        precondition(model.state.validation == .invalidTime)
        precondition(!model.state.results.isEmpty)
        precondition(!model.state.isSaving)
        await wait { hasPresentedAlert(in: parent) }
        let retainedValidationIDs = model.state.results.map(\.id)
        sessions!.deliveryPresented = true
        await wait { !hasPresentedAlert(in: parent) }
        precondition(model.state.results.map(\.id) == retainedValidationIDs)
        sessions!.deliveryPresented = false
        await wait { hasPresentedAlert(in: parent) }
        precondition(defaults.data(forKey: "MathAlarm.pendingAlarmHandoffs.v1") == queuedData)
        print("BRIDGE PASS typed validation result without persistence")

        let cursor = model.state.results.last!.id
        var suspendStarted = false
        var suspendCancelled = false
        let resultWaiter = Task { @MainActor in
            suspendStarted = true
            do { _ = try await asyncFunction(for: model.awaitResult(afterId: cursor)) }
            catch { suspendCancelled = error is CancellationError }
        }
        await wait { suspendStarted }
        resultWaiter.cancel()
        await resultWaiter.value
        precondition(suspendCancelled)
        precondition(!model.isClosed)
        model.onEvent(event: AddEditAlarmEvent.OnSaveTodoClick.shared)
        precondition(model.state.results.last!.id > cursor)
        print("BRIDGE PASS native suspend cancellation leaves later authoritative result retained")

        var emissions = 0
        let observation = Task { @MainActor in
            do {
                for try await _ in asyncSequence(for: model.stateFlow) { emissions += 1 }
            } catch { }
        }
        await wait { emissions > 0 }
        observation.cancel()
        await observation.value
        let before = emissions
        model.onEvent(event: AddEditAlarmEvent.EnteredTitle(value: "After observer cancellation"))
        await Task.yield()
        precondition(emissions == before)
        precondition(!model.isClosed)
        print("BRIDGE PASS native Flow cancellation leaves owner and state alive")

        sessions!.closeEditor(id: session)
        await wait { model.isClosed }
        precondition(!challenge.model.isClosed && pendingStore.peekPendingDeeplink() == handoff)
        let savedEditor = sessions!.openEditor(alarm: nil)
        let savedModel = savedEditor.model
        await wait { mount.renderedDetail == "false/\(savedEditor.id)/editor" && navigationSettled(in: host!) }
        savedModel.onEvent(event: AddEditAlarmEvent.EnteredTitle(value: "M4 disabled native fixture"))
        savedModel.onEvent(event: AddEditAlarmEvent.ChangeTime(value: TimeState(hour: 24, minute: 0)))
        savedModel.onEvent(event: AddEditAlarmEvent.OnSaveTodoClick.shared)
        precondition(!savedModel.isClosed && savedModel.state.hasUnsavedChanges)
        let validationResult = savedModel.state.results.last!
        precondition(validationResult.event is AlarmSettingsViewModel.UiEventValidationFailed)
        savedModel.acknowledgeResult(id: validationResult.id)
        await wait { !hasPresentedAlert(in: parent) }
        savedModel.onEvent(event: AddEditAlarmEvent.ChangeTime(value: TimeState(hour: 8, minute: 20)))
        savedModel.onEvent(event: AddEditAlarmEvent.ToggleEnabled(value: false))
        savedModel.onEvent(event: AddEditAlarmEvent.OnSaveTodoClick.shared)
        savedModel.onEvent(event: AddEditAlarmEvent.OnSaveTodoClick.shared)
        precondition(savedModel.state.isSaving)
        sessions!.closeEditor(id: savedEditor.id) // Explicit discard is guarded during accepted work.
        precondition(sessions!.editors.contains { $0.id == savedEditor.id })
        await wait { savedModel.isClosed }
        precondition(savedModel.state.isSaved && savedModel.state.results.isEmpty)
        precondition(!sessions!.editors.contains { $0.id == savedEditor.id })
        precondition(sessions!.soundSelection(sessionID: savedEditor.id) == nil)
        let listModel = SharedFeatures.shared.list()
        await wait { !listModel.state.loading && listModel.state.alarms.contains { $0.title == "M4 disabled native fixture" } }
        mount.list = listModel
        await wait { !hasPresentedAlert(in: parent) && navigationSettled(in: host!) }
        await Task.yield()
        host!.view.layoutIfNeeded()
        capture(window, name: "list")
        let fixture = listModel.state.alarms.first { $0.title == "M4 disabled native fixture" }!
        precondition(!fixture.isOn && fixture.pendingTimes.isEmpty)
        precondition(listModel.state.alarms.filter { $0.title == fixture.title }.count == 1)
        listModel.onEvent(event: AlarmListEvent.OnDeleteAlarmClick(alarm: fixture))
        await wait { listModel.state.pendingOperations == 0 && !listModel.state.alarms.contains { $0.alarmId == fixture.alarmId } }
        precondition(listModel.state.canUndoDelete)
        capture(window, name: "list-undo")
        listModel.onEvent(event: AlarmListEvent.OnUndoDeleteClick.shared)
        await wait { listModel.state.pendingOperations == 0 && listModel.state.alarms.contains { $0.alarmId == fixture.alarmId } }
        precondition(!listModel.state.canUndoDelete)
        listModel.onEvent(event: AlarmListEvent.OnClearAlarmsClick.shared)
        await wait { listModel.state.pendingOperations == 0 && listModel.state.alarms.isEmpty }
        capture(window, name: "list-empty")
        mount.list = nil
        listModel.close()
        precondition(defaults.data(forKey: "MathAlarm.pendingAlarmHandoffs.v1") == queuedData)
        print("BRIDGE PASS native validation retry duplicate save result acknowledgement and list undo")

        let windowEditor = sessions!.openEditor(alarm: nil)
        windowEditor.model.onEvent(event: AddEditAlarmEvent.EnteredTitle(value: "Window-owned retained draft"))
        await wait { mount.renderedDetail == "false/\(windowEditor.id)/editor" && navigationSettled(in: host!) }
        let probeID = "bridge-verification/automatic-owner/\(UUID().uuidString)"
        let probeModel = SharedFeatures.shared.doNewEditor(sessionId: probeID)
        var probe: UIHostingController<AutomaticOwnerProbe>? = UIHostingController(
            rootView: AutomaticOwnerProbe(model: probeModel))
        parent.addChild(probe!)
        parent.view.addSubview(probe!.view)
        probe!.view.frame = CGRect(x: 0, y: 0, width: 100, height: 100)
        probe!.didMove(toParent: parent)
        await Task.yield()
        probe!.view.layoutIfNeeded()
        precondition(!probeModel.isClosed)
        probe!.willMove(toParent: nil)
        probe!.view.removeFromSuperview()
        probe!.removeFromParent()
        probe = nil
        await wait { probeModel.isClosed }
        SharedFeatures.shared.closeEditor(sessionId: probeID)
        precondition(defaults.data(forKey: "MathAlarm.pendingAlarmHandoffs.v1") == queuedData)
        print("BRIDGE PASS mounted StateViewModel cleanup leaves durable queue unchanged")

        mount.visible = false
        host!.rootView = Mount(mount: mount, sessions: nil)
        // Structural owner teardown explicitly ends factory sessions independently of caches.
        await wait { challenge.model.isClosed && windowEditor.model.isClosed && sessions!.editors.isEmpty }
        precondition(sessions!.challenges.isEmpty)
        sessions!.refreshPendingDelivery() // A late outgoing-window notification cannot recreate owners.
        precondition(sessions!.challenge == nil && sessions!.challenges.isEmpty)
        precondition(windowEditor.model.state.alarmTitle == "Window-owned retained draft")
        precondition(challenge.model.state.readiness == .idle)
        precondition(defaults.data(forKey: "MathAlarm.pendingAlarmHandoffs.v1") == queuedData)
        // A real window lifetime ends when its hosting controller is detached/released.
        // Native Form/alert transition caches may retain removed view values while a host lives.
        host!.willMove(toParent: nil)
        host!.view.removeFromSuperview()
        host!.removeFromParent()
        host = nil
        weak var releasedWindow = sessions
        sessions = nil
        await Task.yield()
        // UIKit may cache outgoing native Form/navigation values, especially with a
        // keyboard or alert. The gate is authoritative owner/factory cleanup, not
        // an undocumented deadline for presentation-cache object deallocation.
        if let cachedWindow = releasedWindow {
            precondition(cachedWindow.editors.isEmpty && cachedWindow.challenges.isEmpty)
            cachedWindow.refreshPendingDelivery()
            precondition(cachedWindow.challenge == nil)
            print("BRIDGE NOTE outgoing native presentation cache retains an inert registry; all factory owners ended")
        }
        precondition(challenge.model.state.readiness == .idle)
        precondition(PendingDeeplinkStore(userDefaults: defaults).peekPendingDeeplink() == handoff)
        precondition(defaults.data(forKey: "MathAlarm.pendingAlarmHandoffs.v1") == queuedData)
        precondition(model.isClosed)
        model.close()
        windowEditor.model.close()
        challenge.model.close()
        print("BRIDGE PASS production session-end and window cleanup preserve unresolved delivery")
        print("SHARED_BRIDGE_VERIFICATION_PASSED")
        fflush(stdout)
    }

    private static func hasPresentedAlert(in controller: UIViewController) -> Bool {
        if let presented = controller.presentedViewController {
            if presented is UIAlertController || hasPresentedAlert(in: presented) { return true }
        }
        return controller.children.contains { hasPresentedAlert(in: $0) }
    }

    private static func navigationSettled(in controller: UIViewController) -> Bool {
        if let navigation = controller as? UINavigationController,
           navigation.transitionCoordinator != nil { return false }
        return controller.children.allSatisfy { navigationSettled(in: $0) }
    }

    private static func capture(_ window: UIWindow, name: String) {
        window.layoutIfNeeded()
        let image = UIGraphicsImageRenderer(bounds: window.bounds).image { _ in
            window.drawHierarchy(in: window.bounds, afterScreenUpdates: true)
        }
        let directory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("native-ui-m4", isDirectory: true)
        try! FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        try! image.pngData()!.write(to: directory.appendingPathComponent("\(name).png"))
    }

    private static func exerciseKeyboard(in view: UIView, title: String, window: UIWindow) async {
        guard let field = editorField(in: view, title: title) else {
            preconditionFailure("Mounted native editor must expose its label field")
        }
        field.becomeFirstResponder()
        await wait { field.isFirstResponder }
        await Task.yield()
        capture(window, name: "editor-keyboard")
        field.resignFirstResponder()
    }

    private static func editorField(in view: UIView, title: String) -> UITextField? {
        if let field = view as? UITextField, field.text == title { return field }
        return view.subviews.lazy.compactMap { editorField(in: $0, title: title) }.first
    }

    private static func visibleEditor(in controller: UIViewController, title: String) -> Bool {
        if let navigation = controller as? UINavigationController {
            guard navigation.transitionCoordinator == nil,
                  let visible = navigation.visibleViewController,
                  visible === navigation.topViewController else { return false }
            return visibleEditor(in: visible, title: title)
        }
        if controller.viewIfLoaded?.window != nil,
           containsEditorControl(in: controller.view, title: title) { return true }
        return controller.children.contains { visibleEditor(in: $0, title: title) }
    }

    private static func containsEditorControl(in view: UIView, title: String) -> Bool {
        guard !view.isHidden, view.alpha > 0 else { return false }
        if let field = view as? UITextField, field.text == title {
            return true
        }
        // Navigation controllers keep outgoing/root views in their hierarchy during
        // transitions. Traverse their visible controller rather than those subviews.
        if view.next is UINavigationController { return false }
        return view.subviews.contains { containsEditorControl(in: $0, title: title) }
    }

    private static func wait(file: StaticString = #fileID, line: UInt = #line, _ condition: () -> Bool) async {
        let deadline = ContinuousClock.now + .seconds(5)
        while !condition() {
            precondition(ContinuousClock.now < deadline, "Bridge integration condition timed out", file: file, line: line)
            try? await Task.sleep(for: .milliseconds(20))
        }
    }
}
#endif
