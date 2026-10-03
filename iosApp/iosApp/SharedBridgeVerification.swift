import SwiftUI
import AVFoundation
import AVKit
import UIKit
import Observation
import KMPObservableViewModelCore
import KMPObservableViewModelSwiftUI
import KMPNativeCoroutinesAsync
import app

#if DEBUG
/// Production-app integration checks. Only an explicit launch argument enables them.
/// Disabled fixtures exercise native save/list flows in disposable storage.
/// Occurrence checks use a controlled scheduler and real shared progress logic;
/// no physical AlarmKit registration or acoustic behavior is established.
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

    private static var verificationTask: Task<Void, Never>?

    static func start() {
        // Full-screen fixtures can remove the launch view from presentation.
        // Its SwiftUI task must neither cancel nor restart the explicit verifier.
        guard verificationTask == nil else { return }
        verificationTask = Task { @MainActor in await run() }
    }

    static func run(restoration: Bool = false) async {
        VerificationResults.shared.reset()
        setbuf(stdout, nil) // Preserve the failing group in simulator crash transcripts.
        if ProcessInfo.processInfo.arguments.contains("--verify-m6-settings-only") {
            guard let window = UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene })
                .flatMap(\.windows).first(where: \.isKeyWindow), let parent = window.rootViewController else {
                preconditionFailure("Settings verification requires an attached app window")
            }
            await NativeSettingsVerification.run(window: window, parent: parent)
            VerificationResults.shared.finish()
            return
        }
        if restoration || ProcessInfo.processInfo.arguments.contains("--verify-m5-restoration") {
            await verifyProcessRestoration()
            NativeSettingsVerification.verifyFreshProcess()
            VerificationResults.shared.finish()
            return
        }
        let handoff = IosApplication.shared.createAlarmHandoffJson(
            alarmId: 7, deliveryId: "bridge-verification-token", activeAt: KotlinLong(value: 1000))
        let encoded = try! JSONSerialization.jsonObject(with: Data(handoff.utf8)) as! [String: Any]
        verificationCheck(encoded["alarmId"] as? Int == 7)
        verificationCheck(encoded["activeAt"] as? Int == 1000)
        verificationCheck(encoded["version"] as? Int == 2)
        verificationCheck(encoded["deliveryId"] as? String == "bridge-verification-token")
        let decoded = IosApplication.shared.decodeAlarmHandoffJson(payload: handoff)!
        verificationCheck(decoded.alarmId == 7 && decoded.activeAt?.int64Value == 1000)
        verificationCheck(decoded.deliveryId == "bridge-verification-token")
        verificationCheck(IosApplication.shared.decodeAlarmHandoffJson(payload: "invalid payload") == nil)
        let missingSession = "bridge-verification/missing-occurrence/\(UUID().uuidString)"
        let missingChallenge = SharedFeatures.shared.challenge(sessionId: missingSession)
        let missingReady = try! await asyncFunction(for: missingChallenge.initializeOccurrence(
            alarmId: Int64.min, activeAt: KotlinLong(value: 1000)))
        verificationCheck(!missingReady.boolValue && missingChallenge.state.readiness == .error)
        verificationCheck(missingChallenge.state.occurrenceId == nil && missingChallenge.state.alarm == nil)
        SharedFeatures.shared.closeChallenge(sessionId: missingSession)
        verificationCheck(missingChallenge.isClosed)
        VerificationResults.shared.pass("UI-free bootstrap, versioned codec and identity-only missing-occurrence result")
        let suite = "bridge-verification/\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        let pendingStore = PendingDeeplinkStore(userDefaults: defaults)
        var sessions: NativeWindowSessions? = NativeWindowSessions(pendingStore: pendingStore, inspectDelivery: { _ in .initialize_ }, restoreRecovery: { _ in })
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
        verificationCheck(model.state.alarmTitle == "Retained Swift draft")
        verificationCheck(model.state.hasUnsavedChanges)
        VerificationResults.shared.pass("typed edits and Swift Observation")
        capture(window, name: "editor")

        verificationCheck(sessions!.beginPermissionRequest(id: session))
        verificationCheck(!sessions!.beginPermissionRequest(id: session))
        mount.alternateDetail = true
        await wait { mount.renderedDetail == "true/\(session)/editor" && navigationSettled(in: host!) }
        verificationCheck(SharedFeatures.shared.doNewEditor(sessionId: session) === model)
        verificationCheck(model.state.alarmTitle == "Retained Swift draft")
        verificationCheck(!model.isClosed)
        mount.alternateDetail = false
        await wait { mount.renderedDetail == "false/\(session)/editor" && navigationSettled(in: host!) }
        verificationCheck(sessions!.openEditor(alarm: nil).model === model && !model.isClosed)
        verificationCheck(sessions!.permissionRequests.contains(session))
        sessions!.endPermissionRequest(id: session)
        VerificationResults.shared.pass("production editor owner survives compact-expanded-compact layout")

        model.onEvent(event: AddEditAlarmEvent.OnTestClick.shared)
        let previewResult = model.state.results.last!
        let previewDraft = (previewResult.event as! AlarmSettingsViewModel.UiEventTestAlarm).alarm
        model.acknowledgeResult(id: previewResult.id)
        let replacement = sessions!.openEditor(alarm: previewDraft)
        await wait { mount.renderedDetail == "false/\(replacement.id)/editor" && navigationSettled(in: host!) }
        replacement.model.onEvent(event: AddEditAlarmEvent.EnteredTitle(value: "Second retained draft"))
        sessions!.setEditorPath([.sound], id: replacement.id)
        await waitForDestination(.sound, editorID: replacement.id, sessions: sessions!, in: host!, window: window)
        sessions!.selectEditor(id: session)
        await wait { mount.renderedDetail == "false/\(session)/editor" && navigationSettled(in: host!) }
        verificationCheck(model.state.alarmTitle == "Retained Swift draft" && !model.isClosed)
        verificationCheck(replacement.model.state.alarmTitle == "Second retained draft" && !replacement.model.isClosed)
        verificationCheck(sessions!.editorPaths[replacement.id] == [.sound])
        sessions!.setEditorPath([.challenge], id: session)
        await waitForDestination(.challenge, editorID: session, sessions: sessions!, in: host!, window: window)
        capture(window, name: "challenge")
        mount.alternateDetail = true
        await waitForDestination(.challenge, editorID: session, sessions: sessions!, in: host!, window: window)
        sessions!.selectEditor(id: replacement.id)
        await waitForDestination(.sound, editorID: replacement.id, sessions: sessions!, in: host!, window: window)
        sessions!.selectEditor(id: session)
        await waitForDestination(.challenge, editorID: session, sessions: sessions!, in: host!, window: window)
        mount.alternateDetail = false
        await waitForDestination(.challenge, editorID: session, sessions: sessions!, in: host!, window: window)
        sessions!.setEditorPath([], id: session)
        // SwiftUI can keep the root mounted throughout a push/pop, so a second
        // root onAppear is not a rendering signal. Inspect the visible UIKit stack.
        await wait { visibleEditor(in: host!, title: model.state.alarmTitle) }
        verificationCheck(sessions!.editorPaths[session] == [])
        sessions!.closeEditor(id: replacement.id)
        await wait { replacement.model.isClosed }
        verificationCheck(!model.isClosed)
        VerificationResults.shared.pass("production detail and nested destination replacement retain drafts")

        model.onEvent(event: AddEditAlarmEvent.ChangeTime(value: TimeState(hour: 9, minute: 45)))
        model.onEvent(event: AddEditAlarmEvent.ToggleRepeat(value: true))
        model.onEvent(event: AddEditAlarmEvent.ToggleDayChooser(value: "FTFTFTF"))
        model.onEvent(event: AddEditAlarmEvent.OnChallengeChange(value: MathChallenge(
            difficulty: 1, questionCount: 3, operations: "+−×÷", additionRange: 0, factorRange: 0, difficultyMix: "")))
        model.setChallengeMixing(enabled: true)
        model.setMixedQuestionCount(difficulty: 2, count: 2)
        verificationCheck(model.state.challenge.questionCount == 5 && model.state.challenge.difficultyMix == "11122")
        model.setChallengeMixing(enabled: false)
        model.onEvent(event: AddEditAlarmEvent.OnChallengeChange(value: MathChallenge(
            difficulty: 3, questionCount: 4, operations: "+×", additionRange: 2, factorRange: 1, difficultyMix: "")))
        model.onEvent(event: AddEditAlarmEvent.ChangeSnoozeDuration(minutes: 12))
        model.onEvent(event: AddEditAlarmEvent.ChangeMaxSnoozes(value: 0))
        for destination in [NativeEditorDestination.repeatSettings, .challenge, .snooze, .preview] {
            sessions!.setEditorPath([], id: session)
            await wait { visibleEditor(in: host!, title: model.state.alarmTitle) }
            sessions!.setEditorPath([destination], id: session)
            await waitForDestination(destination, editorID: session, sessions: sessions!, in: host!, window: window)
            verificationCheck(model.state.alarmTime.hour == 9 && model.state.alarmTime.minute == 45)
            verificationCheck(model.state.dayChooser == "FTFTFTF" && model.state.repeatWeekly)
            verificationCheck(model.state.challenge.operations == "+×" && model.state.snoozeMinutes == 12)
            capture(window, name: destination == .preview ? "maths-preview-native" : destination.rawValue.lowercased())
        }
        sessions!.setEditorPath([], id: session)
        await wait { visibleEditor(in: host!, title: model.state.alarmTitle) }
        sessions!.setEditorPath([.sound], id: session)
        await waitForDestination(.sound, editorID: session, sessions: sessions!, in: host!, window: window)
        capture(window, name: "sound")
        let sound = sessions!.soundSelection(sessionID: session)!
        let originalTone = model.state.tone
        sound.choose("alarm_daybreak")
        verificationCheck(model.state.tone == originalTone && sound.hasChanges)
        mount.alternateDetail = true
        await waitForDestination(.sound, editorID: session, sessions: sessions!, in: host!, window: window)
        verificationCheck(sessions!.soundSelection(sessionID: session) === sound && sound.pendingTone == "alarm_daybreak")
        let oldPresentation = sound.beginPresentation()
        let newPresentation = sound.beginPresentation()
        sound.togglePreview("alarm_orbit")
        verificationCheck(sound.previewingTone == "alarm_orbit" && sound.previewMessage == nil)
        sound.endPresentation(oldPresentation)
        verificationCheck(sound.previewingTone == "alarm_orbit")
        sound.endPresentation(newPresentation)
        verificationCheck(sound.previewingTone == nil)
        sound.finish() // Explicit Back/discard leaves Kotlin selection untouched.
        sound.begin(currentTone: model.state.tone)
        verificationCheck(sound.pendingTone == originalTone && !sound.hasChanges)
        sound.choose("alarm_rally")
        model.onEvent(event: AddEditAlarmEvent.OnToneChange(value: sound.pendingTone)) // Same Done action.
        sound.finish()
        verificationCheck(model.state.tone == "alarm_rally" && model.state.hasUnsavedChanges)
        mount.alternateDetail = false
        await waitForDestination(.sound, editorID: session, sessions: sessions!, in: host!, window: window)
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
        VerificationResults.shared.pass("native editor subpages and staged sound survive presentation replacement")

        pendingStore.setPendingDeeplink(handoff)
        let laterHandoff = IosApplication.shared.createAlarmHandoffJson(
            alarmId: 8, deliveryId: "later-verification-token", activeAt: KotlinLong(value: 2000))
        pendingStore.setPendingDeeplink(laterHandoff)
        let queuedData = defaults.data(forKey: "MathAlarm.pendingAlarmHandoffs.v1")!
        sessions!.refreshPendingDelivery()
        await wait { sessions!.challenge?.model.state.readiness == .error }
        let challenge = sessions!.challenge!
        await Task.yield()
        sessions!.deliveryPresented = false
        sessions!.refreshPendingDelivery()
        verificationCheck(sessions!.challenge!.model === challenge.model)
        verificationCheck(challenge.model.state.readiness == .error && !challenge.model.isClosed)
        verificationCheck(PendingDeeplinkStore(userDefaults: defaults).peekPendingDeeplink() == handoff)
        verificationCheck(!pendingStore.acknowledgePendingDeeplink(laterHandoff))
        verificationCheck(defaults.data(forKey: "MathAlarm.pendingAlarmHandoffs.v1") == queuedData)
        verificationCheck(model.state.alarmTitle == "Retained Swift draft" && !model.isClosed)
        VerificationResults.shared.pass("failed readiness preserves durable ordered deliveries")
        // End only the error presentation before exercising the editor alert.
        // Replay above legitimately requests presentation again; it does not resolve it.
        await wait { !sessions!.deliveryReplayInFlight }
        sessions!.deliveryPresented = false
        await Task.yield()

        model.onEvent(event: AddEditAlarmEvent.ChangeTime(value: TimeState(hour: 25, minute: 0)))
        model.onEvent(event: AddEditAlarmEvent.OnSaveTodoClick.shared)
        verificationCheck(model.state.validation == .invalidTime)
        verificationCheck(!model.state.results.isEmpty)
        verificationCheck(!model.state.isSaving)
        print("BRIDGE TRACE editor validation deliveryPresented=\(sessions!.deliveryPresented) revision=\(sessions!.editorResultsRevision) results=\(model.state.results.map(\.id))")
        await wait { hasPresentedAlert(in: parent) }
        let retainedValidationIDs = model.state.results.map(\.id)
        sessions!.deliveryPresented = true
        await wait { !hasPresentedAlert(in: parent) }
        verificationCheck(model.state.results.map(\.id) == retainedValidationIDs)
        sessions!.deliveryPresented = false
        await wait { hasPresentedAlert(in: parent) }
        verificationCheck(defaults.data(forKey: "MathAlarm.pendingAlarmHandoffs.v1") == queuedData)
        VerificationResults.shared.pass("typed validation result without persistence")

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
        verificationCheck(suspendCancelled)
        verificationCheck(!model.isClosed)
        model.onEvent(event: AddEditAlarmEvent.OnSaveTodoClick.shared)
        verificationCheck(model.state.results.last!.id > cursor)
        VerificationResults.shared.pass("native suspend cancellation leaves later authoritative result retained")

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
        verificationCheck(emissions == before)
        verificationCheck(!model.isClosed)
        VerificationResults.shared.pass("native Flow cancellation leaves owner and state alive")

        sessions!.closeEditor(id: session)
        await wait { model.isClosed }
        verificationCheck(!challenge.model.isClosed && pendingStore.peekPendingDeeplink() == handoff)
        let savedEditor = sessions!.openEditor(alarm: nil)
        let savedModel = savedEditor.model
        await wait { mount.renderedDetail == "false/\(savedEditor.id)/editor" && navigationSettled(in: host!) }
        savedModel.onEvent(event: AddEditAlarmEvent.EnteredTitle(value: "M4 disabled native fixture"))
        savedModel.onEvent(event: AddEditAlarmEvent.ChangeTime(value: TimeState(hour: 24, minute: 0)))
        savedModel.onEvent(event: AddEditAlarmEvent.OnSaveTodoClick.shared)
        verificationCheck(!savedModel.isClosed && savedModel.state.hasUnsavedChanges)
        let validationResult = savedModel.state.results.last!
        verificationCheck(validationResult.event is AlarmSettingsViewModel.UiEventValidationFailed)
        savedModel.acknowledgeResult(id: validationResult.id)
        await wait { !hasPresentedAlert(in: parent) }
        savedModel.onEvent(event: AddEditAlarmEvent.ChangeTime(value: TimeState(hour: 8, minute: 20)))
        savedModel.onEvent(event: AddEditAlarmEvent.ToggleEnabled(value: false))
        savedModel.onEvent(event: AddEditAlarmEvent.OnSaveTodoClick.shared)
        savedModel.onEvent(event: AddEditAlarmEvent.OnSaveTodoClick.shared)
        verificationCheck(savedModel.state.isSaving)
        sessions!.closeEditor(id: savedEditor.id) // Explicit discard is guarded during accepted work.
        verificationCheck(sessions!.editors.contains { $0.id == savedEditor.id })
        await wait { savedModel.isClosed }
        verificationCheck(savedModel.state.isSaved && savedModel.state.results.isEmpty)
        verificationCheck(!sessions!.editors.contains { $0.id == savedEditor.id })
        verificationCheck(sessions!.soundSelection(sessionID: savedEditor.id) == nil)
        let listModel = SharedFeatures.shared.list()
        await wait { !listModel.state.loading && listModel.state.alarms.contains { $0.title == "M4 disabled native fixture" } }
        mount.list = listModel
        await wait { !hasPresentedAlert(in: parent) && navigationSettled(in: host!) }
        await Task.yield()
        host!.view.layoutIfNeeded()
        capture(window, name: "list")
        let fixture = listModel.state.alarms.first { $0.title == "M4 disabled native fixture" }!
        verificationCheck(!fixture.isOn && fixture.pendingTimes.isEmpty)
        verificationCheck(listModel.state.alarms.filter { $0.title == fixture.title }.count == 1)
        listModel.onEvent(event: AlarmListEvent.OnDeleteAlarmClick(alarm: fixture))
        await wait { listModel.state.pendingOperations == 0 && !listModel.state.alarms.contains { $0.alarmId == fixture.alarmId } }
        verificationCheck(listModel.state.canUndoDelete)
        capture(window, name: "list-undo")
        listModel.onEvent(event: AlarmListEvent.OnUndoDeleteClick.shared)
        await wait { listModel.state.pendingOperations == 0 && listModel.state.alarms.contains { $0.alarmId == fixture.alarmId } }
        verificationCheck(!listModel.state.canUndoDelete)
        listModel.onEvent(event: AlarmListEvent.OnClearAlarmsClick.shared)
        await wait { listModel.state.pendingOperations == 0 && listModel.state.alarms.isEmpty }
        capture(window, name: "list-empty")
        mount.list = nil
        listModel.close()
        verificationCheck(defaults.data(forKey: "MathAlarm.pendingAlarmHandoffs.v1") == queuedData)
        VerificationResults.shared.pass("native validation retry duplicate save result acknowledgement and list undo")

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
        verificationCheck(!probeModel.isClosed)
        probe!.willMove(toParent: nil)
        probe!.view.removeFromSuperview()
        probe!.removeFromParent()
        probe = nil
        await wait { probeModel.isClosed }
        SharedFeatures.shared.closeEditor(sessionId: probeID)
        verificationCheck(defaults.data(forKey: "MathAlarm.pendingAlarmHandoffs.v1") == queuedData)
        VerificationResults.shared.pass("mounted StateViewModel cleanup leaves durable queue unchanged")

        mount.visible = false
        host!.rootView = Mount(mount: mount, sessions: nil)
        // Structural owner teardown explicitly ends factory sessions independently of caches.
        await wait { challenge.model.isClosed && windowEditor.model.isClosed && sessions!.editors.isEmpty }
        verificationCheck(sessions!.challenges.isEmpty)
        sessions!.refreshPendingDelivery() // A late outgoing-window notification cannot recreate owners.
        verificationCheck(sessions!.challenge == nil && sessions!.challenges.isEmpty)
        verificationCheck(windowEditor.model.state.alarmTitle == "Window-owned retained draft")
        verificationCheck(challenge.model.state.readiness == .error)
        verificationCheck(defaults.data(forKey: "MathAlarm.pendingAlarmHandoffs.v1") == queuedData)
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
            verificationCheck(cachedWindow.editors.isEmpty && cachedWindow.challenges.isEmpty)
            cachedWindow.refreshPendingDelivery()
            verificationCheck(cachedWindow.challenge == nil)
            print("BRIDGE NOTE outgoing native presentation cache retains an inert registry; all factory owners ended")
        }
        verificationCheck(challenge.model.state.readiness == .error)
        verificationCheck(PendingDeeplinkStore(userDefaults: defaults).peekPendingDeeplink() == handoff)
        verificationCheck(defaults.data(forKey: "MathAlarm.pendingAlarmHandoffs.v1") == queuedData)
        verificationCheck(model.isClosed)
        model.close()
        windowEditor.model.close()
        challenge.model.close()
        VerificationResults.shared.pass("production session-end and window cleanup preserve unresolved delivery")
        await exercisePermissionSave(parent: parent)
        await exerciseMilestone5(window: window, parent: parent)
        await NativeSettingsVerification.run(window: window, parent: parent)
        VerificationResults.shared.finish()
        fflush(stdout)
    }

    /// Controlled native acceptance exercises production Room/usecases/session owners.
    /// It creates no AlarmKit registration and proves no acoustic/device behavior.
    final class VerificationScheduler: NSObject, NativeAlarmScheduler {
        var registrations: [String: AlarmScheduleRequest] = [:]
        var cancellations: [Int64] = []
        var rejectCancellation = false
        var authorization = "authorized"
        var authorizationResponse = true
        var deferAuthorization = false
        var pendingAuthorization: AlarmAuthorizationCompletion?
        var authorizationRequests = 0
        private func key(_ id: Int64, _ occurrence: String) -> String { "\(id)/\(occurrence)" }
        func isAlarmKitAvailable() -> Bool { true }
        func authorizationStatus() -> String { authorization }
        func requestAuthorization(completion: AlarmAuthorizationCompletion) {
            authorizationRequests += 1
            if deferAuthorization {
                pendingAuthorization = completion
            } else {
                authorization = authorizationResponse ? "authorized" : "denied"
                completion.complete(authorized: authorizationResponse)
            }
        }
        func scheduleAlarm(request: AlarmScheduleRequest, completion: AlarmScheduleCompletion) {
            registrations[key(request.alarmId, request.occurrenceKey)] = request
            completion.complete(success: true, error: nil)
        }
        func cancelAlarm(alarmId: Int64) -> String? {
            if rejectCancellation { return "Injected native cancellation rejection" }
            registrations = registrations.filter { $0.value.alarmId != alarmId }
            cancellations.append(alarmId)
            return nil
        }
        func cancelOccurrence(alarmId: Int64, occurrenceKey: String) -> String? {
            if rejectCancellation { return "Injected native cancellation rejection" }
            registrations.removeValue(forKey: key(alarmId, occurrenceKey))
            cancellations.append(alarmId)
            return nil
        }
        func hasPendingOccurrence(alarmId: Int64, occurrenceKey: String) -> Bool {
            registrations[key(alarmId, occurrenceKey)] != nil
        }
        func hasPendingHandoff() -> Bool { false }
        func acknowledgePendingHandoff(payload: String) { }
    }

    private final class PermissionMount: ObservableObject {
        @Published var showEditor = true
    }

    private struct PermissionScene: View {
        @ObservedObject var sessions: NativeWindowSessions
        @ObservedObject var mount: PermissionMount
        var body: some View {
            ZStack {
                NativeSessionOwners(sessions: sessions)
                if mount.showEditor { NativeEditorStack(sessions: sessions) }
            }
        }
    }

    private static func exercisePermissionSave(parent: UIViewController) async {
        let scheduler = VerificationScheduler()
        scheduler.authorization = "notDetermined"
        scheduler.authorizationResponse = false
        AlarmSchedulerBridge.shared.registerScheduler(scheduler: scheduler)
        defer { AlarmSchedulerBridge.shared.registerScheduler(scheduler: AlarmKitKotlinBridge(wrapper: AlarmKitWrapperImpl.shared)) }
        let sessions = NativeWindowSessions()
        let mount = PermissionMount()
        let host = UIHostingController(rootView: PermissionScene(sessions: sessions, mount: mount))
        parent.addChild(host); parent.view.addSubview(host.view)
        host.view.frame = parent.view.bounds; host.didMove(toParent: parent)
        let list = SharedFeatures.shared.list()
        await wait { !list.state.loading }
        let originalIDs = Set(list.state.alarms.map(\.alarmId))
        defer {
            sessions.closeWindow(); list.close()
            host.willMove(toParent: nil); host.view.removeFromSuperview(); host.removeFromParent()
        }

        let draft = sessions.openEditor(alarm: nil)
        scheduler.deferAuthorization = true
        draft.model.onEvent(event: AddEditAlarmEvent.EnteredTitle(value: "Permission retry fixture"))
        draft.model.onEvent(event: AddEditAlarmEvent.OnSaveTodoClick.shared)
        await wait { scheduler.pendingAuthorization != nil }
        let permission = draft.model.state.results.first!
        verificationCheck(sessions.permissionRequests == [draft.id] && scheduler.authorizationRequests == 1)
        verificationCheck(sessions.requestInitialAlarmPermission(id: draft.id, resultID: permission.id) == nil)
        scheduler.authorization = "denied"
        scheduler.pendingAuthorization!.complete(authorized: false)
        scheduler.pendingAuthorization = nil
        scheduler.deferAuthorization = false
        await wait { sessions.permissionRequests.isEmpty }
        verificationCheck(draft.model.state.results.map(\.id) == [permission.id])
        verificationCheck(sessions.requestInitialAlarmPermission(id: draft.id, resultID: permission.id) == nil)
        verificationCheck(scheduler.authorizationRequests == 1)
        await wait { host.presentedViewController != nil }
        if let window = parent.view.window { capture(window, name: "permission-settings-guidance") }
        draft.model.acknowledgeResult(id: permission.id)
        mount.showEditor = false
        await wait { host.presentedViewController == nil && editorField(in: host.view, title: "Permission retry fixture") == nil }
        verificationCheck(!draft.model.isClosed && !draft.model.state.isSaving && !draft.model.state.isSaved)
        verificationCheck(draft.model.state.alarmId?.int64Value == 0 && draft.model.state.results.isEmpty)
        verificationCheck(draft.model.state.alarmTitle == "Permission retry fixture")
        verificationCheck(sessions.selectedEditorID == draft.id && sessions.permissionRequests.isEmpty)
        verificationCheck(Set(list.state.alarms.map(\.alarmId)) == originalIDs && scheduler.registrations.isEmpty)

        // A later explicit save can request permission again and accept this exact draft.
        scheduler.authorization = "notDetermined"
        scheduler.authorizationResponse = true
        draft.model.onEvent(event: AddEditAlarmEvent.OnSaveTodoClick.shared)
        await wait { !draft.model.state.isSaving && !draft.model.state.results.isEmpty }
        draft.model.acknowledgeResult(id: draft.model.state.results.first!.id)
        await sessions.requestAlarmPermission(id: draft.id)!.value
        await wait { draft.model.isClosed && list.state.alarms.contains { $0.title == "Permission retry fixture" } }
        let saved = list.state.alarms.first { $0.title == "Permission retry fixture" }!
        verificationCheck(saved.isOn && saved.scheduleInitialized && saved.scheduleError == nil && !saved.pendingTimes.isEmpty)
        verificationCheck(scheduler.registrations.values.filter { $0.alarmId == saved.alarmId }.count == saved.pendingTimes.count)
        verificationCheck(sessions.selectedEditorID == nil && sessions.permissionRequests.isEmpty)
        list.onEvent(event: AlarmListEvent.OnDeleteAlarmClick(alarm: saved))
        await wait { list.state.pendingOperations == 0 && !list.state.alarms.contains { $0.alarmId == saved.alarmId } }
        verificationCheck(scheduler.registrations.values.allSatisfy { $0.alarmId != saved.alarmId })

        // An OS response arriving after discard cannot save the replacement draft.
        scheduler.authorization = "notDetermined"
        scheduler.deferAuthorization = true
        let discarded = sessions.openEditor(alarm: nil)
        let late = sessions.requestAlarmPermission(id: discarded.id)!
        await wait { scheduler.pendingAuthorization != nil }
        sessions.closeEditor(id: discarded.id)
        let replacement = sessions.openEditor(alarm: nil)
        replacement.model.onEvent(event: AddEditAlarmEvent.EnteredTitle(value: "Replacement permission draft"))
        scheduler.authorization = "authorized"
        scheduler.pendingAuthorization!.complete(authorized: true)
        scheduler.pendingAuthorization = nil
        await late.value
        verificationCheck(discarded.model.isClosed && !replacement.model.isClosed && !replacement.model.state.isSaved)
        verificationCheck(replacement.model.state.alarmId?.int64Value == 0 && replacement.model.state.results.isEmpty)
        verificationCheck(sessions.selectedEditorID == replacement.id && sessions.permissionRequests.isEmpty)
        verificationCheck(Set(list.state.alarms.map(\.alarmId)) == originalIDs && scheduler.registrations.isEmpty)
        VerificationResults.shared.pass("native permission denial keeps unsaved draft without retry; grant saves exact draft and occurrences; late response cannot save replacement")

        // Returning without a grant consumes the handoff without another Save/dialog.
        scheduler.authorization = "denied"
        verificationCheck(sessions.beginAlarmSettingsSave(id: replacement.id))
        sessions.alarmSettingsSceneChanged(.active) // No departure yet: do not save.
        sessions.alarmSettingsSceneChanged(.inactive)
        sessions.alarmSettingsSceneChanged(.active)
        verificationCheck(!replacement.model.state.isSaved && replacement.model.state.results.isEmpty)
        scheduler.authorization = "authorized"
        sessions.alarmSettingsSceneChanged(.active) // An unrelated later activation cannot save it.
        verificationCheck(!replacement.model.state.isSaved && replacement.model.state.alarmId?.int64Value == 0)

        // A failed Settings open and a discarded originating draft both cancel the intent.
        verificationCheck(sessions.beginAlarmSettingsSave(id: replacement.id))
        sessions.cancelAlarmSettingsSave(id: replacement.id)
        sessions.alarmSettingsSceneChanged(.inactive)
        sessions.alarmSettingsSceneChanged(.active)
        verificationCheck(!replacement.model.state.isSaved)
        verificationCheck(sessions.beginAlarmSettingsSave(id: replacement.id))
        sessions.alarmSettingsSceneChanged(.inactive)
        sessions.closeEditor(id: replacement.id)
        let settingsDraft = sessions.openEditor(alarm: nil)
        settingsDraft.model.onEvent(event: AddEditAlarmEvent.EnteredTitle(value: "Settings grant fixture"))
        sessions.alarmSettingsSceneChanged(.active)
        verificationCheck(!settingsDraft.model.state.isSaved && settingsDraft.model.state.results.isEmpty)

        // Grant saves this exact draft once, after any delivered challenge yields.
        scheduler.authorization = "denied"
        verificationCheck(sessions.beginAlarmSettingsSave(id: settingsDraft.id))
        sessions.alarmSettingsSceneChanged(.background)
        scheduler.authorization = "authorized"
        sessions.deliveryPresented = true
        sessions.alarmSettingsSceneChanged(.active)
        verificationCheck(!settingsDraft.model.state.isSaved && !settingsDraft.model.state.isSaving)
        sessions.deliveryPresented = false
        sessions.resumeAlarmSaveAfterSettings()
        sessions.resumeAlarmSaveAfterSettings() // Repeated activation must not duplicate the Save.
        await wait { settingsDraft.model.isClosed && list.state.alarms.contains { $0.title == "Settings grant fixture" } }
        let settingsSaved = list.state.alarms.first { $0.title == "Settings grant fixture" }!
        verificationCheck(settingsSaved.isOn && settingsSaved.scheduleInitialized && settingsSaved.scheduleError == nil)
        verificationCheck(!settingsSaved.pendingTimes.isEmpty)
        verificationCheck(scheduler.registrations.values.filter { $0.alarmId == settingsSaved.alarmId }.count == settingsSaved.pendingTimes.count)
        verificationCheck(list.state.alarms.filter { $0.title == "Settings grant fixture" }.count == 1)
        list.onEvent(event: AlarmListEvent.OnDeleteAlarmClick(alarm: settingsSaved))
        await wait { list.state.pendingOperations == 0 && Set(list.state.alarms.map(\.alarmId)) == originalIDs }
        verificationCheck(scheduler.registrations.isEmpty && scheduler.authorizationRequests == 3)
        VerificationResults.shared.pass("native first Save requests authorization once; Settings grant resumes exact draft once; denial, failed open and replacement never save; delivery defers resumption")

        for language in ["en", "es", "de", "ru", "pt", "hi", "pa", "bn", "zh"] {
            let url = Bundle.main.url(forResource: "alarm-settings-guide-\(language)", withExtension: "mp4")!
            let asset = AVURLAsset(url: url)
            let duration = try! await asset.load(.duration)
            let video = try! await asset.loadTracks(withMediaType: .video)
            let audio = try! await asset.loadTracks(withMediaType: .audio)
            verificationCheck(duration.seconds == 6 && video.count == 1 && audio.isEmpty)
            let size = try! await video[0].load(.naturalSize)
            verificationCheck(size == CGSize(width: 480, height: 640))
        }
        verificationCheck(IosApplication.shared.beginSettingsGuideAudio(ownerId: "verification-guide-a", onInterrupted: {}))
        verificationCheck(!IosApplication.shared.beginSettingsGuideAudio(ownerId: "verification-guide-b", onInterrupted: {}))
        verificationCheck(!IosApplication.shared.endSettingsGuideAudio(ownerId: "verification-guide-b"))
        verificationCheck(IosApplication.shared.endSettingsGuideAudio(ownerId: "verification-guide-a"))
        sessions.settingsGuide.prepare(autoplay: false)
        verificationCheck(sessions.settingsGuide.available && !sessions.settingsGuide.playing)
        if !AVPictureInPictureController.isPictureInPictureSupported() {
            var openedSettings = 0
            sessions.settingsGuide.openSettingsWithGuide { openedSettings += 1 }
            verificationCheck(openedSettings == 1 && !sessions.settingsGuide.starting && !sessions.settingsGuide.floating)
            print("BRIDGE NOTE native Picture in Picture unsupported on this simulator; direct Settings fallback verified")
        }
        verificationCheck(!IosApplication.shared.beginSettingsGuideAudio(ownerId: "verification-guide-b", onInterrupted: {}))
        sessions.deliveryPresented = true // The window registry yields the tutorial to delivery.
        verificationCheck(IosApplication.shared.beginSettingsGuideAudio(ownerId: "verification-guide-b", onInterrupted: {}))
        verificationCheck(IosApplication.shared.endSettingsGuideAudio(ownerId: "verification-guide-b"))
        sessions.deliveryPresented = false
        VerificationResults.shared.pass("native permission tutorial bundles nine silent loops; audio lease rejects competing owners and stale release; delivery stops guide")
    }

    private struct ChallengeScene: View {
        @ObservedObject var sessions: NativeWindowSessions
        @ObservedObject var mount: MountState
        var body: some View {
            ZStack {
                NativeSessionOwners(sessions: sessions)
                if mount.alternateDetail {
                    NavigationSplitView(preferredCompactColumn: .constant(.detail)) {
                        Text("Verification sidebar")
                    } detail: { NativeEditorStack(sessions: sessions) }
                } else { NativeEditorStack(sessions: sessions) }
            }
            .environment(\.dynamicTypeSize, mount.largeText ? .accessibility3 : .large)
            .fullScreenCover(isPresented: Binding(get: { sessions.deliveryPresented }, set: { sessions.deliveryPresented = $0 }),
                onDismiss: { sessions.presentationEnded() }) {
                NavigationStack { NativePendingDelivery(sessions: sessions) }
            }
        }
    }

    private static func exerciseMilestone5(window: UIWindow, parent: UIViewController) async {
        let scheduler = VerificationScheduler()
        AlarmSchedulerBridge.shared.registerScheduler(scheduler: scheduler)
        defer { AlarmSchedulerBridge.shared.registerScheduler(scheduler: AlarmKitKotlinBridge(wrapper: AlarmKitWrapperImpl.shared)) }
        let suite = "m5-production/\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        let queue = PendingDeeplinkStore(userDefaults: defaults)
        var recoveryRequests: [String] = []
        let sessions = NativeWindowSessions(pendingStore: queue, restoreRecovery: { recoveryRequests += $0 })
        let mount = MountState()
        var host: UIHostingController<ChallengeScene>? = UIHostingController(rootView: ChallengeScene(sessions: sessions, mount: mount))
        parent.addChild(host!)
        parent.view.addSubview(host!.view)
        host!.view.frame = parent.view.bounds
        host!.didMove(toParent: parent)
        let draft = sessions.openEditor(alarm: nil)
        draft.model.onEvent(event: AddEditAlarmEvent.EnteredTitle(value: "M5 retained draft \"quotes\""))
        draft.model.onEvent(event: AddEditAlarmEvent.OnChallengeChange(value: MathChallenge(difficulty: 3,
            questionCount: 2, operations: "+", additionRange: 0, factorRange: 0, difficultyMix: "")))
        sessions.setEditorPath([.challenge, .preview], id: draft.id)
        await wait { sessions.previews[draft.id]?.model.state.readiness == .ready }
        await waitForDestination(.preview, editorID: draft.id, sessions: sessions, in: host!, window: window)
        let preview = sessions.previews[draft.id]!
        verificationCheck(preview.model.state.preview && preview.model.state.occurrenceId == nil)
        let firstProblem = preview.model.state.currentProblem!
        await wait { answerField(in: host!.view) != nil }
        let nativeAnswer = answerField(in: host!.view)!
        verificationCheck(nativeAnswer.window === window && nativeAnswer.placeholder == NativeStrings.text("Answer"))
        verificationCheck(nativeAnswer.keyboardType == .numbersAndPunctuation && nativeAnswer.returnKeyType == .go)
        // SwiftUI owns the accessible container/label, rather than assigning it
        // to this inner UITextField. Client-level label checks belong to M7.
        await wait { nativeAnswer.isFirstResponder } // Native FocusState supplies initial answer focus.
        verificationCheck((nativeAnswer.text ?? "").isEmpty && preview.model.state.answerText.isEmpty)
        nativeAnswer.insertText("-999")
        await wait { nativeAnswer.text == "-999" && preview.model.state.answerText == "-999" }
        verificationCheck(nativeAnswer.isFirstResponder)
        capture(window, name: "maths-preview-native-answer-focus-keyboard")
        VerificationResults.shared.pass("native answer focus keyboard placeholder and UITextField insertion update shared raw answer")
        preview.model.submitAnswer(questionIndex: 0, problem: firstProblem)
        await wait { sessions.challengeFailures[preview.id] != nil }
        nativeAnswer.resignFirstResponder()
        preview.model.onEvent(event: MathScreenEvent.EnteredAnswer(value: String(firstProblem.answer)))
        preview.model.submitAnswer(questionIndex: 0, problem: firstProblem)
        await wait { preview.model.state.questionIndex == 1 }
        verificationCheck(!queue.hasPendingDeeplink() && scheduler.registrations.isEmpty)
        mount.largeText = true
        window.overrideUserInterfaceStyle = .dark
        await Task.yield()
        host!.view.layoutIfNeeded()
        capture(window, name: "maths-preview-dark-large-text")
        mount.largeText = false
        window.overrideUserInterfaceStyle = .light
        mount.alternateDetail = true
        await Task.yield()
        verificationCheck(sessions.previews[draft.id]?.model === preview.model)
        sessions.closePreview(editorID: draft.id, expectedSessionID: preview.id)
        verificationCheck(preview.model.isClosed && sessions.editorPaths[draft.id] == [.challenge])
        verificationCheck(draft.model.state.alarmTitle == "M5 retained draft \"quotes\"")
        sessions.setEditorPath([.challenge, .preview], id: draft.id)
        await wait { sessions.previews[draft.id]?.model.state.readiness == .ready }
        await waitForDestination(.preview, editorID: draft.id, sessions: sessions, in: host!, window: window)
        let secondPreview = sessions.previews[draft.id]!
        sessions.closePreview(editorID: draft.id, expectedSessionID: preview.id) // Outgoing Cancel cannot close replacement.
        verificationCheck(sessions.previews[draft.id]?.id == secondPreview.id)
        let draftBeforePreviewCompletion = draft.model.state
        for expectedIndex in 0..<Int(secondPreview.model.state.questionCount) {
            let problem = secondPreview.model.state.currentProblem!
            secondPreview.model.onEvent(event: MathScreenEvent.EnteredAnswer(value: String(problem.answer)))
            secondPreview.model.submitAnswer(questionIndex: Int32(expectedIndex), problem: problem)
            await wait { secondPreview.model.isClosed || secondPreview.model.state.questionIndex == Int32(expectedIndex + 1) }
        }
        await wait { secondPreview.model.isClosed && sessions.previews[draft.id] == nil }
        verificationCheck(secondPreview.model.state.readiness == .resolved && secondPreview.model.state.results.isEmpty)
        verificationCheck(sessions.selectedEditor?.model === draft.model && draft.model.state == draftBeforePreviewCompletion)
        verificationCheck(sessions.editorPaths[draft.id] == [.challenge] && !queue.hasPendingDeeplink() && scheduler.registrations.isEmpty)
        VerificationResults.shared.pass("native maths preview validation progress cancellation and retained route")
        VerificationResults.shared.pass("native preview accepted completion returns to identical unsaved draft and nested route")

        let list = SharedFeatures.shared.list()
        await wait { !list.state.loading }
        func saveFixture(_ title: String) async -> Alarm {
            let fixtureTitle = title + " / " + UUID().uuidString
            let id = "m5-save/\(UUID().uuidString)"
            let editor = SharedFeatures.shared.doNewEditor(sessionId: id)
            editor.onEvent(event: AddEditAlarmEvent.EnteredTitle(value: fixtureTitle))
            editor.onEvent(event: AddEditAlarmEvent.ChangeTime(value: TimeState(hour: 23, minute: 50)))
            editor.onEvent(event: AddEditAlarmEvent.OnChallengeChange(value: MathChallenge(difficulty: 3,
                questionCount: 2, operations: "+", additionRange: 0, factorRange: 0, difficultyMix: "")))
            editor.onEvent(event: AddEditAlarmEvent.OnSaveTodoClick.shared)
            await wait { !editor.state.isSaving && editor.state.results.contains { $0.event is AlarmSettingsViewModel.UiEventSaveAlarm } }
            // Room may emit the desired row before the accepted registration update.
            // Await the authoritative scheduled row, not merely its first insertion.
            await wait { list.state.alarms.contains { $0.title == fixtureTitle && $0.isOn && !$0.pendingTimes.isEmpty } }
            let alarm = list.state.alarms.first { $0.title == fixtureTitle }!
            verificationCheck(alarm.isOn && !alarm.pendingTimes.isEmpty)
            for result in editor.state.results { editor.acknowledgeResult(id: result.id) }
            SharedFeatures.shared.closeEditor(sessionId: id)
            return alarm
        }

        // Keep the original native window and draft alive throughout a real delivery.
        // The separate crash-window case below deliberately tears those owners down.
        let returningAlarm = await saveFixture("M5 same-owner draft return")
        sessions.setEditorPath([.challenge, .sound], id: draft.id)
        await waitForDestination(.sound, editorID: draft.id, sessions: sessions, in: host!, window: window)
        let stagedSound = sessions.soundSelection(sessionID: draft.id)!
        stagedSound.choose("alarm_daybreak")
        let draftBeforeDelivery = draft.model.state
        verificationCheck(sessions.beginPermissionRequest(id: draft.id))
        let returningPayload = IosApplication.shared.createAlarmHandoffJson(alarmId: returningAlarm.alarmId,
            deliveryId: "m5-same-owner-return", activeAt: returningAlarm.pendingTimes.first!)
        verificationCheck(queue.setPendingDeeplink(returningPayload))
        sessions.refreshPendingDelivery()
        await wait { sessions.challenge?.model.state.readiness == .ready && !queue.hasPendingDeeplink() }
        let interruption = sessions.challenge!
        await wait { visibleDeliveredChallenge(in: host!) }
        verificationCheck(interruption.model.state.alarm?.alarmId == returningAlarm.alarmId)
        verificationCheck(sessions.selectedEditor?.model === draft.model && sessions.editorPaths[draft.id] == [.challenge, .sound])
        verificationCheck(sessions.permissionRequests.contains(draft.id) && stagedSound.pendingTone == "alarm_daybreak")
        for expectedIndex in 0..<Int(interruption.model.state.questionCount) {
            let problem = interruption.model.state.currentProblem!
            interruption.model.onEvent(event: MathScreenEvent.EnteredAnswer(value: String(problem.answer)))
            interruption.model.submitAnswer(questionIndex: Int32(expectedIndex), problem: problem)
            await wait { interruption.model.isClosed || interruption.model.state.questionIndex == Int32(expectedIndex + 1) }
        }
        await wait { interruption.model.isClosed && sessions.challenge == nil && !sessions.deliveryPresented && !hasPresentation(in: host!) }
        await waitForDestination(.sound, editorID: draft.id, sessions: sessions, in: host!, window: window)
        verificationCheck(sessions.selectedEditor?.model === draft.model && !draft.model.isClosed && draft.model.state == draftBeforeDelivery)
        verificationCheck(sessions.editorPaths[draft.id] == [.challenge, .sound])
        verificationCheck(sessions.soundSelection(sessionID: draft.id) === stagedSound && stagedSound.pendingTone == "alarm_daybreak")
        verificationCheck(sessions.permissionRequests.contains(draft.id) && interruption.model.state.results.isEmpty)
        sessions.endPermissionRequest(id: draft.id)
        capture(window, name: "same-owner-draft-and-staged-sound-after-real-completion")
        VerificationResults.shared.pass("real accepted completion returns to identical retained draft nested route staged sound and permission guard")

        sessions.setEditorPath([.challenge, .preview], id: draft.id)
        await wait { sessions.previews[draft.id]?.model.state.readiness == .ready }
        await waitForDestination(.preview, editorID: draft.id, sessions: sessions, in: host!, window: window)
        let retainedPreview = sessions.previews[draft.id]!
        let first = await saveFixture("M5 first occurrence")
        let second = await saveFixture("M5 later occurrence")
        let firstAt = first.pendingTimes.first!
        let secondAt = second.pendingTimes.first!
        let firstPayload = IosApplication.shared.createAlarmHandoffJson(alarmId: first.alarmId,
            deliveryId: "m5-first", activeAt: firstAt)
        let secondPayload = IosApplication.shared.createAlarmHandoffJson(alarmId: second.alarmId,
            deliveryId: "m5-second", activeAt: secondAt)
        verificationCheck(queue.setPendingDeeplink(firstPayload) && queue.setPendingDeeplink(secondPayload))
        sessions.refreshPendingDelivery()
        await wait { sessions.challenge?.model.state.readiness == .ready && queue.peekPendingDeeplink() == secondPayload }
        let delivered = sessions.challenge!
        verificationCheck(delivered.model.state.alarm?.alarmId == first.alarmId)
        verificationCheck(delivered.model.state.alarm?.activeAt == firstAt)
        verificationCheck(sessions.previews[draft.id]?.model === retainedPreview.model)
        let problem = delivered.model.state.currentProblem!
        delivered.model.onEvent(event: MathScreenEvent.EnteredAnswer(value: String(problem.answer)))
        delivered.model.submitAnswer(questionIndex: 0, problem: problem)
        await wait { delivered.model.state.questionIndex == 1 }
        sessions.refreshPendingDelivery()
        verificationCheck(sessions.challenge?.id == delivered.id && queue.peekPendingDeeplink() == secondPayload)
        await Task.yield()
        capture(window, name: "delivered-challenge-progress")
        VerificationResults.shared.pass("native readiness before exact acknowledgement and ordered delivery interruption")

        // End the structural owner after acknowledgement; durable occurrence/progress must restore.
        let exactProblem = delivered.model.state.currentProblem!
        let startedAt = delivered.model.state.startedAt
        let incorrect = delivered.model.state.incorrectAnswers
        await wait { visibleDeliveredChallenge(in: host!) }
        sessions.closeWindow()
        verificationCheck(delivered.model.isClosed && queue.peekPendingDeeplink() == secondPayload)
        // This UIKit harness removes only a child host, unlike a real closed window.
        // Let its requested cover dismissal finish before installing another host.
        await wait { !hasPresentation(in: parent) }
        host!.willMove(toParent: nil); host!.view.removeFromSuperview(); host!.removeFromParent(); host = nil
        var restoredPayloads: [String]?
        IosApplication.shared.restoreUnresolvedHandoffs { payloads, succeeded in
            verificationCheck(succeeded.boolValue)
            restoredPayloads = payloads
        }
        await wait { restoredPayloads != nil }
        verificationCheck(restoredPayloads!.contains { IosApplication.shared.decodeAlarmHandoffJson(payload: $0)?.alarmId == first.alarmId })
        verificationCheck(queue.restoreUnresolvedHandoffs(restoredPayloads!))
        let replacement = NativeWindowSessions(pendingStore: queue, restoreRecovery: { recoveryRequests += $0 })
        host = UIHostingController(rootView: ChallengeScene(sessions: replacement, mount: mount))
        parent.addChild(host!); parent.view.addSubview(host!.view); host!.view.frame = parent.view.bounds; host!.didMove(toParent: parent)
        replacement.refreshPendingDelivery()
        await wait { replacement.challenge?.model.state.readiness == .ready && queue.peekPendingDeeplink() == secondPayload }
        let restored = replacement.challenge!
        verificationCheck(restored.model.state.questionIndex == 1 && restored.model.state.currentProblem == exactProblem)
        verificationCheck(restored.model.state.startedAt == startedAt && restored.model.state.incorrectAnswers == incorrect)
        // Exercise user commands only once the restored native cover is actually
        // mounted; readiness itself can precede SwiftUI's presentation transaction.
        await wait { visibleDeliveredChallenge(in: host!) }
        VerificationResults.shared.pass("acknowledged unresolved restoration preserves exact challenge progress")

        // Native cancellation failure must keep the answer/problem/identity; only accepted result navigates.
        scheduler.rejectCancellation = true
        restored.model.onEvent(event: MathScreenEvent.EnteredAnswer(value: String(exactProblem.answer)))
        restored.model.submitAnswer(questionIndex: 1, problem: exactProblem)
        restored.model.submitAnswer(questionIndex: 1, problem: exactProblem)
        await wait { replacement.challengeFailures[restored.id] != nil && !restored.model.state.finishing }
        verificationCheck(restored.model.state.readiness == .ready && restored.model.state.answerText == String(exactProblem.answer))
        verificationCheck(restored.model.state.occurrenceId == delivered.model.state.occurrenceId)
        let failed = replacement.challengeFailures[restored.id]!
        scheduler.rejectCancellation = false
        replacement.retryChallengeFailure(session: restored, resultID: failed.id)
        await wait { restored.model.isClosed && replacement.challenge?.model.state.alarm?.alarmId == second.alarmId }
        await wait { replacement.challenge?.model.state.readiness == .ready && !queue.hasPendingDeeplink() }
        let later = replacement.challenge!
        verificationCheck(restored.model.state.results.isEmpty)
        let cancellationCount = scheduler.cancellations.count
        replacement.retryChallengeFailure(session: restored, resultID: failed.id)
        verificationCheck(scheduler.cancellations.count == cancellationCount)
        later.model.onEvent(event: MathScreenEvent.OnSnoozeClick(alarm: second.alarmId, preview: false))
        later.model.onEvent(event: MathScreenEvent.OnSnoozeClick(alarm: second.alarmId, preview: false))
        await wait { later.model.isClosed && replacement.challenge == nil }
        await wait { list.state.alarms.first { $0.alarmId == second.alarmId }?.snoozeCount == 1 }
        verificationCheck(later.model.state.results.isEmpty && !queue.hasPendingDeeplink())
        verificationCheck(recoveryRequests.contains(firstPayload) && recoveryRequests.contains(secondPayload))
        VerificationResults.shared.pass("native accepted completion snooze retry duplicates and result acknowledgement")
        replacement.closeWindow()
        host!.willMove(toParent: nil); host!.view.removeFromSuperview(); host!.removeFromParent(); host = nil
        list.onEvent(event: AlarmListEvent.OnClearAlarmsClick.shared)
        await wait { list.state.pendingOperations == 0 && list.state.alarms.isEmpty }
        let restartAlarm = await saveFixture("M5 restart after acknowledgement")
        let restartDefaults = UserDefaults(suiteName: "MathAlarm.M5.restart-verification")!
        let restartQueue = PendingDeeplinkStore(userDefaults: restartDefaults)
        let restartPayload = IosApplication.shared.createAlarmHandoffJson(alarmId: restartAlarm.alarmId,
            deliveryId: "m5-crash-window", activeAt: restartAlarm.pendingTimes.first!)
        verificationCheck(restartQueue.setPendingDeeplink(restartPayload))
        let restartOwner = NativeWindowSessions(pendingStore: restartQueue, restoreRecovery: { _ in })
        host = UIHostingController(rootView: ChallengeScene(sessions: restartOwner, mount: mount))
        parent.addChild(host!); parent.view.addSubview(host!.view); host!.view.frame = parent.view.bounds; host!.didMove(toParent: parent)
        restartOwner.refreshPendingDelivery()
        await wait { restartOwner.challenge?.model.state.readiness == .ready && !restartQueue.hasPendingDeeplink() }
        let restarting = restartOwner.challenge!
        let firstRestartProblem = restarting.model.state.currentProblem!
        restarting.model.onEvent(event: MathScreenEvent.EnteredAnswer(value: "-999"))
        restarting.model.submitAnswer(questionIndex: 0, problem: firstRestartProblem)
        restarting.model.onEvent(event: MathScreenEvent.EnteredAnswer(value: String(firstRestartProblem.answer)))
        restarting.model.submitAnswer(questionIndex: 0, problem: firstRestartProblem)
        await wait { restarting.model.state.questionIndex == 1 }
        let state = restarting.model.state
        let evidence: [String: Any] = ["alarmId": restartAlarm.alarmId, "activeAt": state.alarm!.activeAt!.int64Value,
            "index": state.questionIndex, "startedAt": state.startedAt, "incorrect": state.incorrectAnswers,
            "problems": state.problems.map { ["first": $0.numOne, "second": $0.numTwo, "answer": $0.answer, "operation": $0.operator_.name] as [String: Any] }]
        let destination = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0].appendingPathComponent("m5-restart.json")
        try! JSONSerialization.data(withJSONObject: evidence).write(to: destination, options: .atomic)
        restartOwner.closeWindow()
        verificationCheck(restarting.model.isClosed && !restartQueue.hasPendingDeeplink())
        host!.willMove(toParent: nil); host!.view.removeFromSuperview(); host!.removeFromParent(); host = nil
        list.close()
        print("BRIDGE NOTE durable acknowledged occurrence retained for separate process restart")
    }

    private static func verifyProcessRestoration() async {
        let scheduler = VerificationScheduler()
        AlarmSchedulerBridge.shared.registerScheduler(scheduler: scheduler)
        let destination = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0].appendingPathComponent("m5-restart.json")
        let expected = try! JSONSerialization.jsonObject(with: Data(contentsOf: destination)) as! [String: Any]
        let id = (expected["alarmId"] as! NSNumber).int64Value
        let activeAt = (expected["activeAt"] as! NSNumber).int64Value
        let defaults = UserDefaults(suiteName: "MathAlarm.M5.restart-verification")!
        let queue = PendingDeeplinkStore(userDefaults: defaults)
        verificationCheck(!queue.hasPendingDeeplink()) // Payload was acknowledged in the terminated process.
        var payloads: [String]?
        IosApplication.shared.restoreUnresolvedHandoffs { restored, succeeded in
            verificationCheck(succeeded.boolValue)
            payloads = restored
        }
        await wait { payloads != nil }
        verificationCheck(payloads!.count == 1)
        let decoded = IosApplication.shared.decodeAlarmHandoffJson(payload: payloads!.first!)!
        verificationCheck(decoded.alarmId == id && decoded.activeAt?.int64Value == activeAt)
        verificationCheck(queue.restoreUnresolvedHandoffs(payloads!))
        let owner = NativeWindowSessions(pendingStore: queue, restoreRecovery: { _ in })
        let mount = MountState()
        let window = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.flatMap(\.windows).first(where: \.isKeyWindow)!
        let parent = window.rootViewController!
        let host = UIHostingController(rootView: ChallengeScene(sessions: owner, mount: mount))
        parent.addChild(host); parent.view.addSubview(host.view); host.view.frame = parent.view.bounds; host.didMove(toParent: parent)
        owner.refreshPendingDelivery()
        await wait { owner.challenge?.model.state.readiness == .ready && !queue.hasPendingDeeplink() }
        let session = owner.challenge!
        let state = session.model.state
        verificationCheck(state.alarm?.activeAt?.int64Value == activeAt && state.questionIndex == (expected["index"] as! NSNumber).int32Value)
        verificationCheck(state.startedAt == (expected["startedAt"] as! NSNumber).int64Value)
        verificationCheck(state.incorrectAnswers == (expected["incorrect"] as! NSNumber).int32Value)
        let problems = expected["problems"] as! [[String: Any]]
        verificationCheck(problems.count == state.problems.count)
        for (actual, expected) in zip(state.problems, problems) {
            verificationCheck(actual.numOne == (expected["first"] as! NSNumber).int32Value)
            verificationCheck(actual.numTwo == (expected["second"] as! NSNumber).int32Value)
            verificationCheck(actual.answer == (expected["answer"] as! NSNumber).int32Value)
            verificationCheck(actual.operator_.name == expected["operation"] as! String)
        }
        let problem = session.model.state.currentProblem!
        session.model.onEvent(event: MathScreenEvent.EnteredAnswer(value: String(problem.answer)))
        session.model.submitAnswer(questionIndex: state.questionIndex, problem: problem)
        await wait { session.model.isClosed && owner.challenge == nil }
        let list = SharedFeatures.shared.list()
        await wait { !list.state.loading }
        list.onEvent(event: AlarmListEvent.OnClearAlarmsClick.shared)
        await wait { list.state.pendingOperations == 0 && list.state.alarms.isEmpty }
        list.close(); owner.closeWindow()
        host.willMove(toParent: nil); host.view.removeFromSuperview(); host.removeFromParent()
        defaults.removePersistentDomain(forName: "MathAlarm.M5.restart-verification")
        try! FileManager.default.removeItem(at: destination)
        VerificationResults.shared.pass("process restart after acknowledgement restores exact durable progress")
    }

    private static func hasPresentedAlert(in controller: UIViewController) -> Bool {
        if let presented = controller.presentedViewController {
            if presented is UIAlertController || hasPresentedAlert(in: presented) { return true }
        }
        return controller.children.contains { hasPresentedAlert(in: $0) }
    }

    private struct NavigationStabilitySample {
        let signature: String
        let since: TimeInterval
    }
    private static var navigationStability: [ObjectIdentifier: NavigationStabilitySample] = [:]
    private static var reportedSettledNavigation: Set<String> = []

    /// SwiftUI may retain a coordinator after its transition. Establish settlement
    /// from the attached native top view and its rendered geometry over time.
    /// Inspect only navigation/container layers: a caret or progress spinner is
    /// allowed to animate inside an otherwise settled destination.
    private static func nativeNavigationSettled(_ navigation: UINavigationController) -> Bool {
        let identity = ObjectIdentifier(navigation)
        guard let window = navigation.viewIfLoaded?.window,
              let visible = navigation.visibleViewController,
              visible === navigation.topViewController,
              let view = visible.viewIfLoaded, view.window === window,
              !navigation.isBeingPresented, !navigation.isBeingDismissed,
              !visible.isBeingPresented, !visible.isBeingDismissed,
              !view.isHidden, view.alpha > 0.99 else {
            navigationStability.removeValue(forKey: identity)
            return false
        }
        let frame = view.convert(view.bounds, to: window)
        guard !frame.isEmpty, !frame.isNull, !frame.isInfinite,
              frame.intersects(window.bounds) else {
            navigationStability.removeValue(forKey: identity)
            return false
        }
        if let coordinator = navigation.transitionCoordinator,
           coordinator.isInteractive && coordinator.percentComplete < 1 {
            navigationStability.removeValue(forKey: identity)
            return false
        }
        let layers = navigationLayers(navigation, visible: visible)
        guard layers.allSatisfy({ !layerTransitionIsActive($0) }) else {
            navigationStability.removeValue(forKey: identity)
            return false
        }
        let geometry = layers.map { layer in
            "\(NSCoder.string(for: layer.frame))/\(NSCoder.string(for: layer.bounds))/\(layer.opacity)/\(String(describing: layer.transform))"
        }.joined(separator: "|")
        let signature = "\(ObjectIdentifier(visible))/\(visible.navigationItem.title ?? "")/\(NSCoder.string(for: frame))/\(geometry)"
        let now = ProcessInfo.processInfo.systemUptime
        guard let sample = navigationStability[identity], sample.signature == signature else {
            navigationStability[identity] = NavigationStabilitySample(signature: signature, since: now)
            return false
        }
        guard now - sample.since >= 0.12 else { return false }
        if let coordinator = navigation.transitionCoordinator,
           reportedSettledNavigation.insert(signature).inserted {
            print("BRIDGE TRACE native navigation settled title=\(visible.navigationItem.title ?? "") visible=\(ObjectIdentifier(visible)) topMatches=true windowFrame=\(NSCoder.string(for: frame)) stableGeometryAge=\(now - sample.since) activeTransitionAnimations=\(layers.flatMap { activeTransitionAnimations($0) }) presentationGeometryMatches=true coordinator(animated=\(coordinator.isAnimated),interactive=\(coordinator.isInteractive),initiallyInteractive=\(coordinator.initiallyInteractive),cancelled=\(coordinator.isCancelled),percentComplete=\(coordinator.percentComplete),duration=\(coordinator.transitionDuration))")
        }
        return true
    }

    private static func navigationLayers(_ navigation: UINavigationController,
                                         visible: UIViewController) -> [CALayer] {
        var layers: [CALayer] = []
        var view = visible.viewIfLoaded
        while let current = view {
            layers.append(current.layer)
            if current === navigation.viewIfLoaded { break }
            view = current.superview
        }
        layers.append(navigation.navigationBar.layer)
        return layers
    }

    private static func transitionKeyPaths(_ animation: CAAnimation) -> [String] {
        if let group = animation as? CAAnimationGroup {
            return (group.animations ?? []).flatMap { transitionKeyPaths($0) }
        }
        guard let property = animation as? CAPropertyAnimation,
              let key = property.keyPath else { return [] }
        return [key].filter { key in
            ["position", "bounds", "transform", "opacity"].contains { key.hasPrefix($0) }
        }
    }

    private static func activeTransitionAnimations(_ layer: CALayer) -> [String] {
        let now = layer.convertTime(CACurrentMediaTime(), from: nil)
        return (layer.animationKeys() ?? []).compactMap { key in
            guard let animation = layer.animation(forKey: key),
                  !transitionKeyPaths(animation).isEmpty,
                  animation.duration > 0 else { return nil }
            let duration = animation.duration * (animation.autoreverses ? 2 : 1)
            let repeated = animation.repeatDuration > 0 ? animation.repeatDuration :
                duration * Double(max(1, animation.repeatCount))
            let elapsed = (now - animation.beginTime) * Double(animation.speed) + animation.timeOffset
            // Removed-on-completion layers can retain an animation object. Its
            // elapsed timeline and the presentation geometry distinguish activity.
            guard animation.speed == 0 || elapsed < repeated else { return nil }
            return "\(key):paths=\(transitionKeyPaths(animation)),elapsed=\(elapsed),duration=\(repeated),speed=\(animation.speed)"
        }
    }

    private static func layerTransitionIsActive(_ layer: CALayer) -> Bool {
        if !activeTransitionAnimations(layer).isEmpty { return true }
        guard let presentation = layer.presentation() else { return false }
        let actual = presentation.frame, expected = layer.frame
        return abs(actual.origin.x - expected.origin.x) > 0.5 ||
            abs(actual.origin.y - expected.origin.y) > 0.5 ||
            abs(actual.width - expected.width) > 0.5 ||
            abs(actual.height - expected.height) > 0.5 ||
            abs(presentation.opacity - layer.opacity) > 0.01 ||
            !CATransform3DEqualToTransform(presentation.transform, layer.transform)
    }

    private static func navigationSettled(in controller: UIViewController) -> Bool {
        if let navigation = controller as? UINavigationController {
            guard nativeNavigationSettled(navigation),
                  let visible = navigation.visibleViewController else { return false }
            return navigationSettled(in: visible)
        }
        return controller.children.allSatisfy { navigationSettled(in: $0) }
    }

    private static func capture(_ window: UIWindow, name: String) {
        window.layoutIfNeeded()
        let image = UIGraphicsImageRenderer(bounds: window.bounds).image { _ in
            window.drawHierarchy(in: window.bounds, afterScreenUpdates: true)
        }
        let directory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("native-ui-m5", isDirectory: true)
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

    private static func answerField(in view: UIView) -> UITextField? {
        guard !view.isHidden, view.alpha > 0, view.window != nil else { return nil }
        if let field = view as? UITextField, field.placeholder == NativeStrings.text("Answer") { return field }
        return view.subviews.lazy.compactMap { answerField(in: $0) }.first
    }

    private static func visibleEditor(in controller: UIViewController, title: String) -> Bool {
        if let navigation = controller as? UINavigationController {
            guard nativeNavigationSettled(navigation),
                  let visible = navigation.visibleViewController,
                  visible === navigation.topViewController else { return false }
            return visibleEditor(in: visible, title: title)
        }
        if controller.viewIfLoaded?.window != nil,
           containsEditorControl(in: controller.view, title: title) { return true }
        return controller.children.contains { visibleEditor(in: $0, title: title) }
    }

    /// A retained native destination need not produce another onAppear when selected.
    /// Inspect only the visible UIKit navigation controller and its actual native content.
    private static func visibleDestination(in controller: UIViewController,
                                           destination: NativeEditorDestination) -> Bool {
        if let navigation = controller as? UINavigationController {
            guard nativeNavigationSettled(navigation),
                  navigation.viewIfLoaded?.window != nil,
                  let visible = navigation.visibleViewController,
                  visible === navigation.topViewController,
                  visible.navigationItem.title == NativeStrings.text(destination.rawValue) else { return false }
            let identifiers: Set<String>
            switch destination {
            case .sound: identifiers = ["tone-alarm_daybreak", "preview-alarm_daybreak"]
            case .challenge: identifiers = ["challenge-difficulty", "challenge-mix-0", "challenge-operation-addition"]
            case .repeatSettings: identifiers = ["repeat-weekly"]
            case .snooze: identifiers = ["snooze-enabled"]
            case .preview: identifiers = ["challenge-answer", "challenge-problem"]
            }
            // SwiftUI does not expose its automation containers to an in-process
            // host unless an accessibility client is active. The settled, attached
            // top controller/title plus retained route establishes this transition;
            // captures provide visible content evidence. XCTest coverage is M7.
            _ = accessibilityEvidence(in: visible.view, matching: identifiers)
            return true
        }
        return controller.children.contains { visibleDestination(in: $0, destination: destination) }
    }

    private static func visibleDeliveredChallenge(in controller: UIViewController) -> Bool {
        if let presented = controller.presentedViewController, visibleDeliveredChallenge(in: presented) { return true }
        if let navigation = controller as? UINavigationController {
            guard nativeNavigationSettled(navigation),
                  let visible = navigation.visibleViewController, visible === navigation.topViewController,
                  visible.viewIfLoaded?.window != nil,
                  visible.navigationItem.title == NativeStrings.text("Solve maths") else { return false }
            return true
        }
        return controller.children.contains { visibleDeliveredChallenge(in: $0) }
    }

    private static func hasPresentation(in controller: UIViewController) -> Bool {
        controller.presentedViewController != nil || controller.children.contains { hasPresentation(in: $0) }
    }

    /// Public UIKit automation/accessibility containers include SwiftUI controls that
    /// have no corresponding UIButton/UILabel in the rendered UIView hierarchy.
    private static func accessibilityEvidence(in root: NSObject, matching identifiers: Set<String>? = nil) -> [String] {
        var visited: Set<ObjectIdentifier> = []
        var evidence: [String] = []
        func visit(_ object: NSObject, depth: Int) {
            guard depth < 64, visited.count < 4096, visited.insert(ObjectIdentifier(object)).inserted else { return }
            if let view = object as? UIView, view.isHidden || view.alpha <= 0 || view.window == nil { return }
            if let element = object as? UIAccessibilityIdentification, let id = element.accessibilityIdentifier,
               identifiers == nil || identifiers!.contains(id) {
                evidence.append("\(String(describing: type(of: object))) id=\(id) label=\(object.accessibilityLabel ?? "")")
            }
            if let view = object as? UIView {
                // Outgoing/root navigation views cannot establish a visible destination.
                if view.next is UINavigationController { return }
                view.subviews.forEach { visit($0, depth: depth + 1) }
            }
            let elements = object.automationElements ?? object.accessibilityElements
            if let elements {
                for case let element as NSObject in elements { visit(element, depth: depth + 1) }
            } else {
                let count = object.accessibilityElementCount()
                if (1...512).contains(count) {
                    for index in 0..<count {
                        if let element = object.accessibilityElement(at: index) as? NSObject { visit(element, depth: depth + 1) }
                    }
                }
            }
        }
        visit(root, depth: 0)
        return evidence
    }

    private static func controllerEvidence(_ controller: UIViewController, indentation: String = "") -> [String] {
        let title = controller.navigationItem.title ?? ""
        let attached = controller.viewIfLoaded?.window != nil
        var result = ["\(indentation)\(String(describing: type(of: controller))) title=\(title) attached=\(attached)"]
        if let navigation = controller as? UINavigationController {
            let visible = navigation.visibleViewController
            let top = navigation.topViewController
            result.append("\(indentation) visible=\(String(describing: visible)) top=\(String(describing: top)) same=\(visible === top) settled=\(nativeNavigationSettled(navigation))")
            if let coordinator = navigation.transitionCoordinator {
                result.append("\(indentation) coordinator animated=\(coordinator.isAnimated) interactive=\(coordinator.isInteractive) initiallyInteractive=\(coordinator.initiallyInteractive) cancelled=\(coordinator.isCancelled) percentComplete=\(coordinator.percentComplete) duration=\(coordinator.transitionDuration)")
            } else { result.append("\(indentation) coordinator=nil") }
            if let visible, let view = visible.viewIfLoaded {
                let frame = view.window.map { NSCoder.string(for: view.convert(view.bounds, to: $0)) } ?? "detached"
                result.append("\(indentation) visibleWindowFrame=\(frame) hidden=\(view.isHidden) alpha=\(view.alpha) beingPresented=\(visible.isBeingPresented) beingDismissed=\(visible.isBeingDismissed)")
                for layer in navigationLayers(navigation, visible: visible) {
                    result.append("\(indentation) layer=\(type(of: layer)) frame=\(NSCoder.string(for: layer.frame)) presentationFrame=\(layer.presentation().map { NSCoder.string(for: $0.frame) } ?? "nil") animationKeys=\(layer.animationKeys() ?? []) activeTransitionAnimations=\(activeTransitionAnimations(layer)) geometryMoving=\(layerTransitionIsActive(layer))")
                }
                if let sample = navigationStability[ObjectIdentifier(navigation)] {
                    result.append("\(indentation) stableGeometryAge=\(ProcessInfo.processInfo.systemUptime - sample.since) signature=\(sample.signature)")
                }
            }
        }
        if let view = controller.viewIfLoaded {
            result += accessibilityEvidence(in: view).map { indentation + " " + $0 }
        }
        if let presented = controller.presentedViewController {
            result += controllerEvidence(presented, indentation: indentation + " presented ")
        }
        for child in controller.children { result += controllerEvidence(child, indentation: indentation + "  ") }
        return result
    }

    private static func waitForDestination(_ destination: NativeEditorDestination, editorID: String,
                                           sessions: NativeWindowSessions, in controller: UIViewController,
                                           window: UIWindow, file: StaticString = #fileID, line: UInt = #line) async {
        let deadline = ContinuousClock.now + .seconds(5)
        while !(sessions.selectedEditorID == editorID && sessions.selectedEditor?.id == editorID &&
                sessions.editorPaths[editorID]?.last == destination && navigationSettled(in: controller) &&
                visibleDestination(in: controller, destination: destination)) {
            if ContinuousClock.now >= deadline {
                let name = "navigation-timeout-" + destination.rawValue.lowercased().replacingOccurrences(of: " ", with: "-")
                capture(window, name: name)
                let lines = ["expectedTitle=\(NativeStrings.text(destination.rawValue))",
                             "expectedEditor=\(editorID) selectedEditor=\(sessions.selectedEditorID ?? "nil")",
                             "expectedDestination=\(destination.rawValue) retainedPath=\(sessions.editorPaths[editorID] ?? [])"] + controllerEvidence(controller)
                let evidence = lines.joined(separator: "\n")
                print("BRIDGE FAILURE native visible destination evidence\n\(evidence)")
                let directory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
                    .appendingPathComponent("native-ui-m5", isDirectory: true)
                try! evidence.write(to: directory.appendingPathComponent(name + ".txt"), atomically: true, encoding: .utf8)
                preconditionFailure("Native visible destination timed out; screenshot/hierarchy saved", file: file, line: line)
            }
            try? await Task.sleep(for: .milliseconds(20))
        }
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
        // XCTest accessibility snapshots and first-launch iPad layout can contend
        // with rendering on CI. Keep the exact condition and a bounded deadline;
        // a five-second wall-clock budget produced a diagnosed split-layout timeout.
        let deadline = ContinuousClock.now + .seconds(15)
        while !condition() {
            if ContinuousClock.now >= deadline,
               let window = UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene })
                .flatMap(\.windows).first(where: \.isKeyWindow) {
                capture(window, name: "failure-wait-\(line)")
            }
            verificationCheck(ContinuousClock.now < deadline, "Bridge integration condition timed out", file: file, line: line)
            try? await Task.sleep(for: .milliseconds(20))
        }
    }
}
#endif
