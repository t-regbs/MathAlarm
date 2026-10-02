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
/// No alarms are saved, scheduled, delivered, completed, or snoozed by this harness.
@MainActor
enum SharedBridgeVerification {
    static var enabled: Bool { ProcessInfo.processInfo.arguments.contains("--verify-shared-bridge") }

    private final class MountState: ObservableObject {
        @Published var visible = true
        @Published var alternateDetail = false
        var renderedDetail = ""
        var observationInvalidated = false
    }

    private struct NativeScene: View {
        @ObservedObject var mount: MountState
        @ObservedObject var sessions: NativeWindowSessions
        var body: some View {
            ZStack {
                NativeSessionOwners(sessions: sessions)
                if mount.alternateDetail {
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
            if mount.visible, let sessions { NativeScene(mount: mount, sessions: sessions) }
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
        let host = UIHostingController(rootView: Mount(mount: mount, sessions: sessions))
        parent.addChild(host)
        parent.view.addSubview(host.view)
        host.view.frame = parent.view.bounds
        host.didMove(toParent: parent)
        await wait { mount.renderedDetail == "false/\(session)/editor" }

        withObservationTracking { _ = model.state.alarmTitle } onChange: {
            Task { @MainActor in mount.observationInvalidated = true }
        }
        model.onEvent(event: AddEditAlarmEvent.EnteredTitle(value: "Retained Swift draft"))
        await wait { mount.observationInvalidated }
        precondition(model.state.alarmTitle == "Retained Swift draft")
        precondition(model.state.hasUnsavedChanges)
        print("BRIDGE PASS typed edits and Swift Observation")

        mount.alternateDetail = true
        await wait { mount.renderedDetail == "true/\(session)/editor" }
        precondition(SharedFeatures.shared.doNewEditor(sessionId: session) === model)
        precondition(model.state.alarmTitle == "Retained Swift draft")
        precondition(!model.isClosed)
        mount.alternateDetail = false
        await wait { mount.renderedDetail == "false/\(session)/editor" }
        precondition(sessions!.openEditor(alarm: nil).model === model && !model.isClosed)
        print("BRIDGE PASS production editor owner survives compact-expanded-compact layout")

        model.onEvent(event: AddEditAlarmEvent.OnTestClick.shared)
        let previewResult = model.state.results.last!
        let previewDraft = (previewResult.event as! AlarmSettingsViewModel.UiEventTestAlarm).alarm
        model.acknowledgeResult(id: previewResult.id)
        let replacement = sessions!.openEditor(alarm: previewDraft)
        await wait { mount.renderedDetail == "false/\(replacement.id)/editor" }
        replacement.model.onEvent(event: AddEditAlarmEvent.EnteredTitle(value: "Second retained draft"))
        sessions!.setEditorPath([.sound], id: replacement.id)
        await wait { mount.renderedDetail == "false/\(replacement.id)/Sound library" }
        sessions!.selectEditor(id: session)
        await wait { mount.renderedDetail == "false/\(session)/editor" }
        precondition(model.state.alarmTitle == "Retained Swift draft" && !model.isClosed)
        precondition(replacement.model.state.alarmTitle == "Second retained draft" && !replacement.model.isClosed)
        precondition(sessions!.editorPaths[replacement.id] == [.sound])
        sessions!.setEditorPath([.challenge], id: session)
        await wait { mount.renderedDetail == "false/\(session)/Challenge settings" }
        mount.alternateDetail = true
        await wait { mount.renderedDetail == "true/\(session)/Challenge settings" }
        sessions!.selectEditor(id: replacement.id)
        await wait { mount.renderedDetail == "true/\(replacement.id)/Sound library" }
        sessions!.selectEditor(id: session)
        await wait { mount.renderedDetail == "true/\(session)/Challenge settings" }
        mount.alternateDetail = false
        await wait { mount.renderedDetail == "false/\(session)/Challenge settings" }
        sessions!.setEditorPath([], id: session)
        // SwiftUI can keep the root mounted throughout a push/pop, so a second
        // root onAppear is not a rendering signal. Inspect the visible UIKit stack.
        await wait { visibleEditor(in: host, title: model.state.alarmTitle) }
        precondition(sessions!.editorPaths[session] == [])
        sessions!.closeEditor(id: replacement.id)
        await wait { replacement.model.isClosed }
        precondition(!model.isClosed)
        print("BRIDGE PASS production detail and nested destination replacement retain drafts")

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
        let windowEditor = sessions!.openEditor(alarm: nil)
        windowEditor.model.onEvent(event: AddEditAlarmEvent.EnteredTitle(value: "Window-owned retained draft"))
        await wait { mount.renderedDetail == "false/\(windowEditor.id)/editor" }
        mount.visible = false
        host.rootView = Mount(mount: mount, sessions: nil)
        // Retain the factory registry while testing automatic mounted wrapper cleanup.
        // This is the actual window view ending, never a layout/detail transition.
        await wait { challenge.model.isClosed && windowEditor.model.isClosed }
        precondition(sessions!.editors.contains { $0.id == windowEditor.id })
        precondition(windowEditor.model.state.alarmTitle == "Window-owned retained draft")
        precondition(challenge.model.state.readiness == .idle)
        precondition(defaults.data(forKey: "MathAlarm.pendingAlarmHandoffs.v1") == queuedData)
        print("BRIDGE PASS mounted StateViewModel cleanup leaves durable queue unchanged")
        weak var releasedWindow = sessions
        sessions = nil
        await wait { releasedWindow == nil }
        await Task.yield() // Let window-end factory cleanup run on Main after wrapper cleanup.
        precondition(challenge.model.state.readiness == .idle)
        precondition(PendingDeeplinkStore(userDefaults: defaults).peekPendingDeeplink() == handoff)
        precondition(defaults.data(forKey: "MathAlarm.pendingAlarmHandoffs.v1") == queuedData)
        host.willMove(toParent: nil)
        host.view.removeFromSuperview()
        host.removeFromParent()
        precondition(model.isClosed)
        model.close()
        windowEditor.model.close()
        challenge.model.close()
        print("BRIDGE PASS production session-end and window cleanup preserve unresolved delivery")
        print("SHARED_BRIDGE_VERIFICATION_PASSED")
        fflush(stdout)
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
