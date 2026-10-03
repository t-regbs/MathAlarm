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
