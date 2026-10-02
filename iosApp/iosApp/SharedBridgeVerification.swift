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
        var ownerAppeared = false
        var detailGeneration = 0
        var observationInvalidated = false
    }
    private struct Owner: View {
        @ObservedObject var mount: MountState
        @StateViewModel var model: AlarmSettingsViewModel
        var body: some View {
            Group {
                if mount.alternateDetail {
                    Detail(model: model, generation: 1, mount: mount).id("expanded")
                } else {
                    Detail(model: model, generation: 0, mount: mount).id("compact")
                }
            }.onAppear { mount.ownerAppeared = true }
        }
    }
    private struct Detail: View {
        @ObservedViewModel var model: AlarmSettingsViewModel
        let generation: Int
        @ObservedObject var mount: MountState
        var body: some View {
            Text(model.state.alarmTitle).onAppear { mount.detailGeneration = generation }
        }
    }
    private struct Mount: View {
        @ObservedObject var mount: MountState
        let model: AlarmSettingsViewModel
        var body: some View {
            if mount.visible { Owner(mount: mount, model: model) }
        }
    }
    static func run() async {
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
        let session = "bridge-verification/\(UUID().uuidString)"
        let model = SharedFeatures.shared.doNewEditor(sessionId: session)
        let mount = MountState()
        guard let window = UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene })
            .flatMap(\.windows).first(where: \.isKeyWindow), let parent = window.rootViewController else {
            fatalError("Bridge verification requires the running app's mounted window")
        }
        let host = UIHostingController(rootView: Mount(mount: mount, model: model))
        parent.addChild(host)
        parent.view.addSubview(host.view)
        host.view.frame = parent.view.bounds
        host.didMove(toParent: parent)
        await wait { mount.ownerAppeared }
        withObservationTracking { _ = model.state.alarmTitle } onChange: {
            Task { @MainActor in mount.observationInvalidated = true }
        }
        model.onEvent(event: AddEditAlarmEvent.EnteredTitle(value: "Retained Swift draft"))
        await wait { mount.observationInvalidated }
        precondition(model.state.alarmTitle == "Retained Swift draft")
        precondition(model.state.hasUnsavedChanges)
        print("BRIDGE PASS typed edits and Swift Observation")

        mount.alternateDetail = true
        await wait { mount.detailGeneration == 1 }
        precondition(SharedFeatures.shared.doNewEditor(sessionId: session) === model)
        precondition(model.state.alarmTitle == "Retained Swift draft" && !model.isClosed)
        print("BRIDGE PASS retained editor and observed child after detail replacement")
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

        mount.visible = false
        await wait { model.isClosed }
        SharedFeatures.shared.closeEditor(sessionId: session)
        host.willMove(toParent: nil)
        host.view.removeFromSuperview()
        host.removeFromParent()
        model.close()
        print("BRIDGE PASS mounted owner removal and idempotent explicit cleanup")
        print("SHARED_BRIDGE_VERIFICATION_PASSED")
        fflush(stdout)
    }
    private static func wait(_ condition: () -> Bool) async {
        let deadline = Date().addingTimeInterval(5)
        while !condition() {
            precondition(Date() < deadline, "Bridge integration condition timed out")
            try? await Task.sleep(nanoseconds: 20_000_000)
        }
    }
}
#endif
