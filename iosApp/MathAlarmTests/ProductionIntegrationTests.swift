import XCTest
import UIKit
import MessageUI
import AVKit
import app
import KMPObservableViewModelCore
@testable import MathAlarm

@MainActor
final class ProductionIntegrationTests: XCTestCase {
    private enum DeinitContext {
        @TaskLocal static var marker = "outside"
    }

    func testEditorSummariesTrackDraftChanges() {
        let id = "editor-summaries/\(UUID().uuidString)"
        let model = SharedFeatures.shared.doNewEditor(sessionId: id)
        defer { SharedFeatures.shared.closeEditor(sessionId: id) }
        XCTAssertEqual(NativeEditorPresentation.challengeSummary(model.state.challenge), "Easy · 1 question")
        model.setChallengeMixing(enabled: true)
        model.setMixedQuestionCount(difficulty: 1, count: 2)
        XCTAssertEqual(NativeEditorPresentation.challengeSummary(model.state.challenge), "Mixed difficulty · 3 questions")

        XCTAssertTrue(NativeEditorPresentation.snoozeSummary(model.state).hasSuffix("Up to 3 snoozes"))
        model.onEvent(event: AddEditAlarmEvent.ChangeMaxSnoozes(value: 1))
        XCTAssertTrue(NativeEditorPresentation.snoozeSummary(model.state).hasSuffix("Up to 1 snooze"))
        model.onEvent(event: AddEditAlarmEvent.ChangeMaxSnoozes(value: 0))
        XCTAssertTrue(NativeEditorPresentation.snoozeSummary(model.state).hasSuffix("Unlimited"))
        model.onEvent(event: AddEditAlarmEvent.ToggleSnooze(value: false))
        XCTAssertEqual(NativeEditorPresentation.snoozeSummary(model.state), "Off")

        model.onEvent(event: AddEditAlarmEvent.ToggleDayChooser(value: "FTFTFTF"))
        model.onEvent(event: AddEditAlarmEvent.ToggleRepeat(value: true))
        XCTAssertTrue(NativeEditorPresentation.repeatSummary(model.state).hasPrefix("Every week · "))
        model.onEvent(event: AddEditAlarmEvent.ToggleRepeat(value: false))
        XCTAssertTrue(NativeEditorPresentation.repeatSummary(model.state).hasPrefix("Once · "))
        model.onEvent(event: AddEditAlarmEvent.ToggleRepeat(value: true))
        model.onEvent(event: AddEditAlarmEvent.ToggleDayChooser(value: "TTTTTTT"))
        XCTAssertEqual(NativeEditorPresentation.repeatSummary(model.state), "Every day")
        model.onEvent(event: AddEditAlarmEvent.ToggleDayChooser(value: "FFFFFFF"))
        XCTAssertEqual(NativeEditorPresentation.repeatSummary(model.state), "Every week · No weekdays selected")
    }

    func testNextAlarmCountdownBoundaries() {
        let now = Date(timeIntervalSince1970: 1_893_972_600) // Fixed instant near midnight.
        let cases: [(TimeInterval, String)] = [
            (59, "Next alarm in less than a minute"),
            (60, "Next alarm in 1 minute"),
            (5 * 60, "Next alarm in 5 minutes"),
            (60 * 60 - 1, "Next alarm in 59 minutes"),
            (60 * 60, "Next alarm in 1 hour"),
            (61 * 60, "Next alarm in 1 hour 1 minute"),
            (4 * 60 * 60, "Next alarm in 4 hours"),
            (24 * 60 * 60, "Next alarm in 24 hours"),
        ]
        for (delay, expected) in cases {
            XCTAssertEqual(NativeAlarmPresentation.nextAlarmSubtitle(next: now.addingTimeInterval(delay), now: now), expected)
        }
        let next = now.addingTimeInterval(25 * 60 * 60)
        XCTAssertTrue(NativeAlarmPresentation.nextAlarmSubtitle(next: next, now: now).hasPrefix("Next alarm:"))
        XCTAssertEqual(NativeAlarmPresentation.nextAlarmSubtitle(next: next, now: now.addingTimeInterval(60 * 60)), "Next alarm in 24 hours")
        XCTAssertEqual(NativeAlarmPresentation.nextAlarmSubtitle(next: now.addingTimeInterval(24 * 60 * 60 + 1), now: now).hasPrefix("Next alarm:"), true)
        XCTAssertEqual(NativeAlarmPresentation.nextAlarmSubtitle(next: now, now: now), "")
        XCTAssertEqual(NativeAlarmPresentation.nextAlarmSubtitle(next: now.addingTimeInterval(-1), now: now), "")
        XCTAssertEqual(NativeAlarmPresentation.nextAlarmSubtitle([], now: now), "")
    }

    func testRecoveryReplacementKeepsFallbackUntilNativeAcceptance() async throws {
        let old = UUID(), next = UUID()
        var native: Set<UUID> = [old]
        var events: [String] = []
        try await NativeRecoveryDeadlineReplacement.perform(previousIDs: [old], replacementID: next,
            schedule: {
                XCTAssertEqual(native, [old])
                events.append("schedule")
                await Task.yield() // A process exit here still leaves the old OS alarm.
                XCTAssertEqual(native, [old])
                native.insert(next)
            }, isCurrent: { true }, cancel: { id in
                XCTAssertTrue(native.contains(next), "Never remove the fallback before native acceptance")
                events.append("cancel")
                native.remove(id)
            })
        XCTAssertEqual(events, ["schedule", "cancel"])
        XCTAssertEqual(native, [next])
    }

    func testScreenCancellationFinishesAcceptedRecoveryReplacement() async {
        let old = UUID(), next = UUID()
        var native: Set<UUID> = [old]
        var resume: CheckedContinuation<Void, Never>?
        var failure: Error?
        let accepted = expectation(description: "OS accepts replacement while screen is active")
        let screen = Task { @MainActor in
            await NativeRecoveryDeadlineReplacement.finishRefresh {
                do {
                    try await NativeRecoveryDeadlineReplacement.perform(previousIDs: [old], replacementID: next,
                        schedule: {
                            native.insert(next)
                            await withCheckedContinuation { continuation in
                                resume = continuation
                                accepted.fulfill()
                            }
                        }, isCurrent: {
                            // Kotlin suspend bridges reject cancelled Swift callers here.
                            try Task.checkCancellation()
                            return true
                        }, cancel: { native.remove($0) })
                } catch { failure = error }
            }
        }
        await fulfillment(of: [accepted], timeout: 3)
        screen.cancel() // Locking cancels SwiftUI's scene-scoped task.
        resume?.resume()
        await screen.value
        XCTAssertNil(failure, "A normal screen departure must not become a recovery failure")
        XCTAssertEqual(native, [next], "Accepted replacement must retire its predecessor")
    }

    func testScreenCancellationBeforeRecoveryCheckKeepsServiceWorkActive() async {
        var resume: CheckedContinuation<Void, Never>?
        var checked = false
        var failure: Error?
        let suspended = expectation(description: "Initial authoritative check is suspended")
        let screen = Task { @MainActor in
            await NativeRecoveryDeadlineReplacement.finishRefresh {
                await withCheckedContinuation { continuation in
                    resume = continuation
                    suspended.fulfill()
                }
                do {
                    try Task.checkCancellation()
                    checked = true
                } catch { failure = error }
            }
        }
        await fulfillment(of: [suspended], timeout: 3)
        screen.cancel()
        resume?.resume()
        await screen.value
        XCTAssertTrue(checked)
        XCTAssertNil(failure)
    }

    func testRejectedRecoveryReplacementPreservesPreviousFallback() async {
        let old = UUID(), next = UUID()
        var native: Set<UUID> = [old]
        do {
            try await NativeRecoveryDeadlineReplacement.perform(previousIDs: [old], replacementID: next,
                schedule: { throw NSError(domain: "OS rejection", code: 1) },
                isCurrent: { true }, cancel: { native.remove($0) })
            XCTFail("Expected the native rejection")
        } catch {}
        XCTAssertEqual(native, [old])
    }

    func testResolutionDuringRecoveryAcceptanceCancelsNewRegistration() async throws {
        let old = UUID(), next = UUID()
        var native: Set<UUID> = [old]
        var unresolved = true
        try await NativeRecoveryDeadlineReplacement.perform(previousIDs: [old], replacementID: next,
            schedule: {
                await Task.yield()
                // Completion/disable/delete persisted while the OS request suspended.
                unresolved = false
                native.remove(old)
                native.insert(next)
            }, isCurrent: { unresolved }, cancel: { native.remove($0) })
        XCTAssertTrue(native.isEmpty)
    }

    func testRecoveryReplacementAttemptsEveryOldCancellationOnFailure() async {
        let old = UUID(), stale = UUID(), next = UUID()
        var attempted: Set<UUID> = []
        do {
            try await NativeRecoveryDeadlineReplacement.perform(previousIDs: [old, stale], replacementID: next,
                schedule: {}, isCurrent: { true }, cancel: { id in
                    attempted.insert(id)
                    if id == old { throw NSError(domain: "cancel rejection", code: 1) }
                })
            XCTFail("Expected the cancellation error to remain actionable")
        } catch {}
        XCTAssertEqual(attempted, [old, stale])
    }

    func testWindowAndGuideReleaseInsideTaskLocalScope() async throws {
        // Older iOS Swift runtimes crash in isolated deinit's TaskLocal cleanup.
        // Release production owners synchronously inside an actual task-local scope.
        weak var released: NativeWindowSessions?
        let model = DeinitContext.$marker.withValue("M8 owner release") {
            var sessions: NativeWindowSessions? = NativeWindowSessions()
            released = sessions
            let editor = sessions!.openEditor(alarm: nil)
            editor.model.onEvent(event: AddEditAlarmEvent.EnteredTitle(value: "Uncommitted teardown"))
            sessions = nil
            XCTAssertNil(released)
            var guide: NativeAlarmPermissionGuidePlayer? = NativeAlarmPermissionGuidePlayer()
            weak var releasedGuide = guide
            guide = nil
            XCTAssertNil(releasedGuide)
            return editor.model
        }
        // The nonisolated deinit snapshots IDs and dispatches factory closure to Main.
        for _ in 0..<20 where !model.isClosed { await Task.yield() }
        XCTAssertTrue(model.isClosed)
    }

    func testPreviewIsReadyWhenNavigationCommits() throws {
        let sessions = NativeWindowSessions()
        defer { sessions.closeWindow() }
        let editor = sessions.openEditor(alarm: nil)
        let started = ContinuousClock.now
        sessions.setEditorPath([.preview], id: editor.id)
        let preview = try XCTUnwrap(sessions.previews[editor.id])
        XCTAssertEqual(preview.model.state.readiness, .ready,
                       "An in-memory draft must have its maths ready before the preview route renders")
        XCTAssertNotNil(preview.model.state.currentProblem)
        print("PREVIEW READY \(started.duration(to: .now))")
    }

    func testRecordMainThreadAudioSessionLatency() throws {
        // Capability/latency evidence only: neither session acceptance nor timing
        // proves audible output. Use the production owner and leave no lease behind.
        var samples: [[String: Any]] = []
        for index in 0..<8 {
            let owner = "M8 timing/\(UUID().uuidString)"
            let start = Date.timeIntervalSinceReferenceDate
            let accepted = IosApplication.shared.beginSettingsGuideAudio(ownerId: owner, onInterrupted: {})
            let activated = Date.timeIntervalSinceReferenceDate
            let released = accepted && IosApplication.shared.endSettingsGuideAudio(ownerId: owner)
            let end = Date.timeIntervalSinceReferenceDate
            samples.append(["sample": index, "accepted": accepted, "released": released,
                            "activationMs": (activated - start) * 1000,
                            "deactivationMs": (end - activated) * 1000])
            XCTAssertTrue(accepted && released)
        }
        let evidence: [String: Any] = ["os": UIDevice.current.systemVersion, "samples": samples,
                                     "physicalAudibility": "not measured", "thread": "main"]
        let data = try JSONSerialization.data(withJSONObject: evidence, options: [.prettyPrinted, .sortedKeys])
        let attachment = XCTAttachment(data: data, uniformTypeIdentifier: "public.json")
        attachment.name = "M8 synchronous main-thread audio session timings"; attachment.lifetime = .keepAlways
        add(attachment)
        print("M8 AUDIO TIMING " + String(decoding: data, as: UTF8.self))
    }

    func testMountedProductionFrameworkJourney() async {
        // The host runs production bootstrap but leaves its window to this fixture.
        // Every original behavioral assertion is forwarded with its source location.
        VerificationResults.shared.assertion = { passed, message, file, line in
            XCTAssertTrue(passed, message, file: file, line: line)
        }
        defer { VerificationResults.shared.assertion = nil }
        await SharedBridgeVerification.run()
        XCTAssertTrue(VerificationResults.shared.finished)
        let restartOnly = Set([VerificationContract.groups[22], VerificationContract.groups[26]])
        XCTAssertEqual(Set(VerificationResults.shared.groups), Set(VerificationContract.groups).subtracting(restartOnly))
        XCTAssertGreaterThan(VerificationResults.shared.assertionCount, 150)
        let report = XCTAttachment(string: VerificationResults.shared.groups.joined(separator: "\n"))
        report.name = "25 mounted production groups (controlled scheduling)"
        report.lifetime = .keepAlways
        add(report)
        // Resolve/remove this hosted fixture before another test process starts.
        // This is cleanup only; the UI target independently proves fresh-process restoration.
        await SharedBridgeVerification.run(restoration: true)
    }

    func testSessionRetainsObservationThroughOutgoingObserverReplacement() {
        let sessions = NativeWindowSessions()
        let editor = sessions.openEditor(alarm: nil)
        let previewID = "xctest-observation/\(UUID().uuidString)"
        let model = SharedFeatures.shared.challenge(sessionId: previewID)
        let retained = NativeChallengeSession(id: previewID, payload: "", model: model, editorID: editor.id)
        var outgoing: ObservableViewModel<AlarmMathViewModel>?
        do {
            let initial: ObservableViewModel<AlarmMathViewModel> = observableViewModel(for: model)
            outgoing = initial
        }
        XCTAssertTrue(outgoing?.viewModel === model)
        outgoing = nil
        // A borrowed observer disappearing cannot expire the retained session bridge.
        XCTAssertFalse(model.isClosed)
        SharedFeatures.shared.closeChallenge(sessionId: previewID)
        XCTAssertTrue(model.isClosed)
        let lateObserver = observableViewModel(for: retained.model)
        XCTAssertTrue(lateObserver.viewModel === model)
        XCTAssertTrue(lateObserver.viewModel.isClosed) // never resurrect closed commands
        sessions.closeWindow()
        withExtendedLifetime(retained) {}
    }

    func testRecordAvailableClientCapabilities() throws {
        let evidence: [String: Any] = [
            "runtime": UIDevice.current.systemVersion,
            "device": UIDevice.current.model,
            "configuredMailAvailable": MFMailComposeViewController.canSendMail(),
            "voiceOverRunning": UIAccessibility.isVoiceOverRunning,
            "pictureInPictureSupported": AVPictureInPictureController.isPictureInPictureSupported(),
            "physicalDeviceEvidence": false,
            "voiceOverTraversal": "not established by this capability probe",
            "interactiveWindowResizing": "requires separate native client evidence"
        ]
        let data = try JSONSerialization.data(withJSONObject: evidence, options: [.prettyPrinted, .sortedKeys])
        let attachment = XCTAttachment(data: data, uniformTypeIdentifier: "public.json")
        attachment.name = "Available client capabilities (not behavioral acceptance)"
        attachment.lifetime = .keepAlways; add(attachment)
        print("M7 CLIENT CAPABILITIES " + String(decoding: data, as: UTF8.self))
    }

    func testEditorExitDiscardsOnlyAfterExplicitChoiceAndAddStartsFresh() throws {
        let sessions = NativeWindowSessions()
        defer { sessions.closeWindow() }
        let editor = try XCTUnwrap(sessions.requestOpenEditor(alarm: nil))
        editor.model.onEvent(event: AddEditAlarmEvent.EnteredTitle(value: "Unfinished alarm"))
        XCTAssertTrue(editor.model.state.hasUnsavedChanges)
        sessions.setEditorPath([.snooze], id: editor.id)
        // Add means a new alarm, including when a new alarm is already being edited.
        XCTAssertNil(sessions.requestOpenEditor(alarm: nil))
        XCTAssertTrue(sessions.editorExitConfirmation)
        XCTAssertEqual(sessions.editors.count, 1)
        sessions.keepEditing()
        sessions.requestCloseEditor(id: editor.id)
        XCTAssertTrue(sessions.editorExitConfirmation)
        XCTAssertEqual(sessions.selectedEditorID, editor.id)
        XCTAssertFalse(editor.model.isClosed)
        sessions.keepEditing()
        XCTAssertFalse(sessions.editorExitConfirmation)
        XCTAssertEqual(editor.model.state.alarmTitle, "Unfinished alarm")
        XCTAssertEqual(sessions.editorPaths[editor.id], [.snooze])

        sessions.requestCloseEditor(id: editor.id)
        sessions.discardEditorChanges()
        XCTAssertTrue(editor.model.isClosed)
        XCTAssertTrue(sessions.editors.isEmpty)
        XCTAssertNil(sessions.selectedEditorID)
        let fresh = try XCTUnwrap(sessions.requestOpenEditor(alarm: nil))
        XCTAssertNotEqual(fresh.id, editor.id)
        XCTAssertFalse(fresh.model.state.hasUnsavedChanges)
        XCTAssertNotEqual(fresh.model.state.alarmTitle, "Unfinished alarm")
        sessions.requestCloseEditor(id: fresh.id)
        XCTAssertFalse(sessions.editorExitConfirmation)
        XCTAssertTrue(sessions.editors.isEmpty)
    }

    func testSwitchingAlarmsResolvesChangesAndNeverAccumulatesUserEditors() throws {
        let sessions = NativeWindowSessions()
        defer { sessions.closeWindow() }
        let first = try XCTUnwrap(sessions.requestOpenEditor(alarm: nil))
        first.model.onEvent(event: AddEditAlarmEvent.EnteredTitle(value: "Unsaved change"))
        first.model.onEvent(event: AddEditAlarmEvent.OnTestClick.shared)
        let target = try XCTUnwrap((first.model.state.results.last?.event as? AlarmSettingsViewModel.UiEventTestAlarm)?.alarm)
        XCTAssertNil(sessions.requestOpenEditor(alarm: target, initialPath: [.challenge]))
        XCTAssertTrue(sessions.editorExitConfirmation)
        XCTAssertEqual(sessions.editors.count, 1)
        sessions.keepEditing()
        XCTAssertEqual(sessions.selectedEditorID, first.id)
        XCTAssertNil(sessions.requestOpenEditor(alarm: target, initialPath: [.challenge]))
        sessions.discardEditorChanges()
        let second = try XCTUnwrap(sessions.selectedEditor)
        XCTAssertNotEqual(first.id, second.id)
        XCTAssertTrue(first.model.isClosed)
        XCTAssertEqual(sessions.editors.count, 1)
        XCTAssertEqual(sessions.editorPaths[second.id], [.challenge])
        XCTAssertEqual(sessions.requestOpenEditor(alarm: target)?.id, second.id)
        XCTAssertFalse(sessions.editorExitConfirmation)

        // Permission and delivery interruptions cannot dismiss or replace an edit.
        XCTAssertTrue(sessions.beginPermissionRequest(id: second.id))
        XCTAssertNil(sessions.requestOpenEditor(alarm: nil))
        sessions.requestCloseEditor(id: second.id)
        XCTAssertEqual(sessions.selectedEditorID, second.id)
        sessions.endPermissionRequest(id: second.id)
        sessions.deliveryPresented = true
        sessions.requestCloseEditor(id: second.id)
        XCTAssertNil(sessions.requestOpenEditor(alarm: nil))
        XCTAssertFalse(second.model.isClosed)
        sessions.deliveryPresented = false
        second.model.onEvent(event: AddEditAlarmEvent.EnteredTitle(value: "Interrupted edit"))
        sessions.requestCloseEditor(id: second.id)
        XCTAssertTrue(sessions.editorExitConfirmation)
        sessions.deliveryPresented = true
        XCTAssertFalse(sessions.editorExitConfirmation)
        sessions.discardEditorChanges() // An interrupted confirmation cannot close the edit.
        XCTAssertEqual(sessions.selectedEditorID, second.id)
        XCTAssertEqual(second.model.state.alarmTitle, "Interrupted edit")
        sessions.deliveryPresented = false
        sessions.requestCloseEditor(id: second.id)
        sessions.discardEditorChanges()
        let third = try XCTUnwrap(sessions.requestOpenEditor(alarm: nil))
        XCTAssertTrue(second.model.isClosed)
        XCTAssertEqual(sessions.editors.count, 1)
        XCTAssertEqual(sessions.selectedEditorID, third.id)
    }

    func testDeletionUndoReplacesOnlyItsOwnEntryAndSurvivesNoticeDismissal() {
        let manager = UndoManager()
        manager.groupsByEvent = false
        let deletion = NativeAlarmDeletionUndo()
        let unrelatedTarget = NSObject()
        var unrelatedUndone = false
        manager.beginUndoGrouping()
        manager.registerUndo(withTarget: unrelatedTarget) { _ in unrelatedUndone = true }
        manager.endUndoGrouping()
        var restored: [Int] = []
        manager.beginUndoGrouping()
        deletion.synchronize(manager: manager, available: true) { restored.append(1) }
        manager.endUndoGrouping()
        deletion.synchronize(manager: manager, available: true) { restored.append(2) }

        // Dismissing/expiring presentation must not detach the retained undo entry.
        manager.undo()
        XCTAssertEqual(restored, [2])
        manager.undo()
        XCTAssertTrue(unrelatedUndone)
        XCTAssertFalse(manager.canUndo)
    }

    func testDeletionUndoInvalidationAndMissingSystemManager() {
        let manager = UndoManager()
        manager.groupsByEvent = false
        let deletion = NativeAlarmDeletionUndo()
        var restores = 0
        manager.beginUndoGrouping()
        deletion.synchronize(manager: manager, available: true) { restores += 1 }
        manager.endUndoGrouping()
        deletion.synchronize(manager: manager, available: false) { restores += 1 }
        XCTAssertFalse(manager.canUndo)
        deletion.undo()
        XCTAssertEqual(restores, 0)

        deletion.synchronize(manager: nil, available: true) { restores += 1 }
        deletion.undo() // The explicit button works even in an environment without system undo.
        deletion.undo()
        XCTAssertEqual(restores, 1)
    }

    func testExactDurableQueueAcknowledgementAndLegacyCompatibility() {
        let suite = "MathAlarm.XCTest.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        let first = "{\"alarmId\":1}"
        let second = "{\"alarmId\":2,\"version\":2,\"deliveryId\":\"second\"}"
        let store = PendingDeeplinkStore(userDefaults: defaults)
        XCTAssertTrue(store.setPendingDeeplink(first))
        XCTAssertTrue(store.setPendingDeeplink(second))
        let reopened = PendingDeeplinkStore(userDefaults: defaults)
        XCTAssertFalse(reopened.acknowledgePendingDeeplink(second))
        XCTAssertEqual(reopened.peekPendingDeeplink(), first)
        XCTAssertTrue(reopened.acknowledgePendingDeeplink(first))
        XCTAssertEqual(reopened.peekPendingDeeplink(), second)
        XCTAssertTrue(reopened.acknowledgePendingDeeplink(second))
        XCTAssertFalse(reopened.hasPendingDeeplink())
    }
}
