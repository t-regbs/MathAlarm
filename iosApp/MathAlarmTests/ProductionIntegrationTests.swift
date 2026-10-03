import XCTest
import UIKit
import MessageUI
import AVKit
import app
import KMPObservableViewModelCore
@testable import MathAlarm

@MainActor
final class ProductionIntegrationTests: XCTestCase {
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
