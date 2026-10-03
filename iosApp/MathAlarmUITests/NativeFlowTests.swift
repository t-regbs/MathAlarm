import XCTest
import UIKit

@MainActor
final class NativeFlowTests: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

    func testAll27ProductionGroupsAcrossFreshProcesses() {
        let app = XCUIApplication()
        var observed = Set<String>()
        for phase in ["--verify-m6-settings-fresh", "--verify-m5-restoration"] {
            app.launchArguments = ["--verify-shared-bridge", phase, "-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
            app.launchEnvironment["MATHALARM_HOSTED_TESTS"] = "0"
            app.launch()
            let completion = app.staticTexts["verification-complete"]
            XCTAssertTrue(completion.waitForExistence(timeout: 180), "Mounted production checks failed in \(phase)")
            let groups = (completion.value as? String ?? "").components(separatedBy: "\n")
            observed.formUnion(groups)
            let report = XCTAttachment(string: groups.joined(separator: "\n"))
            report.name = phase; report.lifetime = .keepAlways; add(report)
            app.terminate() // OS process death; no in-memory Kotlin coordinator survives.
        }
        for group in VerificationContract.groups {
            XCTContext.runActivity(named: group) { _ in XCTAssertTrue(observed.contains(group)) }
        }
        XCTAssertEqual(observed, Set(VerificationContract.groups))
    }

    func testAlarmListTimeAlignmentDeletionNoticeAndVisibleEditMode() {
        let app = XCUIApplication()
        app.launchEnvironment["MATHALARM_HOSTED_TESTS"] = "0"
        app.launchArguments = ["-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launch()
        for _ in 0..<3 {
            let announcement = app.buttons["announcementGotIt"]
            if announcement.waitForExistence(timeout: 2) { announcement.tap() }
        }
        let firstTitle = "UI list first alarm"
        let secondTitle = "UI list second alarm with a longer label"
        addDisabledAlarm(firstTitle, in: app)
        addDisabledAlarm(secondTitle, in: app)
        let first = alarmButton(firstTitle, in: app)
        let second = alarmButton(secondTitle, in: app)
        XCTAssertTrue(second.waitForExistence(timeout: 5))
        let alarmID = second.identifier.replacingOccurrences(of: "edit-alarm-", with: "")
        let toggle = app.switches["alarm-enabled-" + alarmID]
        XCTAssertTrue(toggle.exists)
        // Details can make cards tall; the switch must remain beside the time at the top.
        let time = app.otherElements["alarm-time-" + alarmID]
        XCTAssertTrue(time.exists)
        XCTAssertEqual(toggle.frame.midY, time.frame.midY, accuracy: 2)
        capture(app, name: "Alarm cards with time-aligned switches")

        swipeToDelete(first, in: app)
        let noticeUndo = app.buttons["undo-delete-notice"]
        XCTAssertTrue(noticeUndo.waitForExistence(timeout: 5))
        XCTAssertTrue(second.isHittable, "Temporary feedback must leave the remaining alarms visible")
        capture(app, name: "Temporary deletion notice")
        noticeUndo.tap()
        XCTAssertTrue(first.waitForExistence(timeout: 5))
        XCTAssertFalse(app.buttons["dismiss-alarm-notice"].exists)

        swipeToDelete(first, in: app)
        let dismiss = app.buttons["dismiss-alarm-notice"]
        XCTAssertTrue(dismiss.waitForExistence(timeout: 5))
        dismiss.tap()
        XCTAssertFalse(dismiss.exists)
        undoFromToolbar(in: app)
        XCTAssertTrue(first.waitForExistence(timeout: 5))

        swipeToDelete(first, in: app)
        XCTAssertTrue(dismiss.waitForExistence(timeout: 5))
        XCTAssertTrue(dismiss.waitForNonExistence(timeout: 15), "Deletion feedback should expire without user action")
        undoFromToolbar(in: app)
        XCTAssertTrue(first.waitForExistence(timeout: 5), "Undo must remain after the notice expires")

        let edit = app.buttons["edit-alarm-list"]
        edit.tap()
        XCTAssertEqual(edit.label, "Done")
        XCTAssertFalse(app.switches["alarm-enabled-" + alarmID].isEnabled)
        capture(app, name: "Native list Edit mode with delete controls")
        // iOS 27 exposes the native minus control's symbol rather than a named Button.
        let remove = app.images["minus.circle.fill"].firstMatch
        XCTAssertTrue(remove.waitForExistence(timeout: 5))
        remove.tap() // Native minus control reveals the destructive confirmation button.
        let confirmation = app.buttons["Delete"]
        XCTAssertTrue(confirmation.waitForExistence(timeout: 5))
        confirmation.tap()
        XCTAssertTrue(noticeUndo.waitForExistence(timeout: 5))
        XCTAssertEqual(app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@ AND identifier != %@", "edit-alarm-", "edit-alarm-list")).count, 1)
        noticeUndo.tap()
        edit.tap()
        XCTAssertTrue(first.waitForExistence(timeout: 5))
        XCTAssertTrue(second.exists)
        XCTAssertTrue(toggle.isEnabled)
        capture(app, name: "Restored alarms after native Edit deletion")
    }

    private func addDisabledAlarm(_ name: String, in app: XCUIApplication) {
        let add = app.buttons["add-alarm"]
        XCTAssertTrue(add.waitForExistence(timeout: 10)); add.tap()
        let title = app.textFields["editor-title"]
        XCTAssertTrue(title.waitForExistence(timeout: 5))
        title.tap(); title.typeText(name)
        app.buttons["editor-keyboard-done"].tap()
        let enabled = app.switches["editor-enabled"]
        XCTAssertTrue(enabled.exists)
        if enabled.value as? String == "1" {
            // Toggle accessibility frames include the label; target the trailing
            // switch itself independently of the editor column's width.
            enabled.coordinate(withNormalizedOffset: CGVector(dx: 1, dy: 0.5))
                .withOffset(CGVector(dx: -25, dy: 0)).tap()
        }
        XCTAssertEqual(enabled.value as? String, "0")
        app.buttons["save-alarm"].tap()
        XCTAssertTrue(alarmButton(name, in: app).waitForExistence(timeout: 10))
        XCTAssertTrue(app.buttons["save-alarm"].waitForNonExistence(timeout: 10))
    }

    private func alarmButton(_ name: String, in app: XCUIApplication) -> XCUIElement {
        app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@ AND label CONTAINS %@", "edit-alarm-", name)).firstMatch
    }

    private func swipeToDelete(_ alarm: XCUIElement, in app: XCUIApplication) {
        let start = alarm.coordinate(withNormalizedOffset: CGVector(dx: 0.9, dy: 0.5))
        let end = alarm.coordinate(withNormalizedOffset: CGVector(dx: 0.35, dy: 0.5))
        start.press(forDuration: 0.05, thenDragTo: end)
        let delete = app.buttons["Delete"]
        XCTAssertTrue(delete.waitForExistence(timeout: 5))
        delete.tap()
        XCTAssertTrue(alarm.waitForNonExistence(timeout: 5))
    }

    private func undoFromToolbar(in app: XCUIApplication) {
        // UIKit's generated toolbar overflow menu carries the title, not SwiftUI's identifier.
        let undo = app.buttons.matching(NSPredicate(format: "identifier == %@ OR label == %@", "undo-delete-menu", "Undo delete")).firstMatch
        if !undo.isHittable {
            // Duo can place native overflow in a vertical toolbar.
            let more = app.buttons["More"].firstMatch
            XCTAssertTrue(more.waitForExistence(timeout: 5), app.debugDescription)
            more.tap()
        }
        XCTAssertTrue(undo.waitForExistence(timeout: 5), app.debugDescription)
        undo.tap()
    }

    private func capture(_ app: XCUIApplication, name: String) {
        // App-element screenshots can retain portrait crop coordinates after an
        // iPad rotation. Capture the display so the evidence matches the screen.
        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = name; attachment.lifetime = .keepAlways; add(attachment)
    }

    func testCancelProtectsChangesAndNestedBackKeepsCurrentEdit() {
        let app = XCUIApplication()
        app.launchEnvironment["MATHALARM_HOSTED_TESTS"] = "0"
        app.launchArguments = ["-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launch()
        for _ in 0..<3 where app.buttons["announcementGotIt"].exists { app.buttons["announcementGotIt"].tap() }
        let addAlarm = app.buttons["add-alarm"]
        XCTAssertTrue(addAlarm.waitForExistence(timeout: 15)); addAlarm.tap()
        let title = app.textFields["editor-title"]
        XCTAssertTrue(title.waitForExistence(timeout: 5))
        title.tap(); title.typeText("M7 exact Back draft")
        app.buttons["editor-keyboard-done"].tap()
        let snooze = app.buttons["editor-snooze"]
        for _ in 0..<5 where !snooze.isHittable { app.swipeUp() }
        XCTAssertTrue(snooze.isHittable); snooze.tap()
        let back = app.buttons.matching(identifier: "BackButton").firstMatch
        XCTAssertTrue(back.waitForExistence(timeout: 5)); back.tap()
        XCTAssertTrue(app.buttons["save-alarm"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["editor-snooze"].exists)
        XCTAssertFalse(app.buttons["Discard changes"].exists)
        for _ in 0..<5 where !title.isHittable { app.swipeDown() }
        XCTAssertEqual(title.value as? String, "M7 exact Back draft")
        app.buttons["discard-draft"].tap()
        XCTAssertTrue(app.buttons["Discard changes"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Your changes haven’t been saved."].exists)
        capture(app, name: "Unsaved changes exit confirmation")
        app.buttons["Keep editing"].tap()
        XCTAssertTrue(app.buttons["save-alarm"].waitForExistence(timeout: 5))
        XCTAssertEqual(title.value as? String, "M7 exact Back draft")
        app.buttons["discard-draft"].tap()
        app.buttons["Discard changes"].tap()
        XCTAssertTrue(addAlarm.waitForExistence(timeout: 5))
        XCTAssertFalse(app.buttons["save-alarm"].exists)
        addAlarm.tap()
        XCTAssertTrue(title.waitForExistence(timeout: 5))
        XCTAssertNotEqual(title.value as? String, "M7 exact Back draft")
        app.buttons["discard-draft"].tap()
        XCTAssertTrue(addAlarm.waitForExistence(timeout: 5))
        XCTAssertFalse(app.buttons["Discard changes"].exists)
        let more = app.buttons["More"].firstMatch
        if more.exists { more.tap() }
        XCTAssertFalse(app.buttons["Retained drafts"].exists)
    }

    func testMainEditorUsesExplicitCancelToResolveChanges() {
        let app = launchEditingApp()
        let add = app.buttons["add-alarm"]
        XCTAssertTrue(add.waitForExistence(timeout: 15)); add.tap()
        let title = app.textFields["editor-title"]
        XCTAssertTrue(title.waitForExistence(timeout: 5))
        title.tap(); title.typeText("Back exit protection")
        app.buttons["editor-keyboard-done"].tap()
        XCTAssertFalse(app.buttons.matching(identifier: "BackButton").firstMatch.exists)
        let cancel = app.buttons["discard-draft"]
        cancel.tap()
        XCTAssertTrue(app.buttons["Discard changes"].waitForExistence(timeout: 5), app.debugDescription)
        app.buttons["Keep editing"].tap()
        XCTAssertTrue(title.waitForExistence(timeout: 5))
        XCTAssertEqual(title.value as? String, "Back exit protection")
        cancel.tap()
        XCTAssertTrue(app.buttons["Discard changes"].waitForExistence(timeout: 5))
        app.buttons["Discard changes"].tap()
        XCTAssertTrue(add.waitForExistence(timeout: 5))
        XCTAssertFalse(app.buttons["save-alarm"].exists)
        add.tap()
        XCTAssertTrue(title.waitForExistence(timeout: 5))
        cancel.tap() // Unchanged editors leave without a confirmation.
        XCTAssertTrue(add.waitForExistence(timeout: 5))
        XCTAssertFalse(app.buttons["save-alarm"].exists)
        XCTAssertFalse(app.buttons["Discard changes"].exists)
    }

    func testChangedSnoozeAndChallengeReturnWithoutDiscardingEditor() throws {
        let app = launchEditingApp()
        app.buttons["add-alarm"].tap()
        let title = app.textFields["editor-title"]
        XCTAssertTrue(title.waitForExistence(timeout: 5))
        title.tap(); title.typeText("Nested Back regression")
        capture(app, name: "Alarm editor with keyboard")
        app.buttons["editor-keyboard-done"].tap()
        capture(app, name: "Alarm editor configuration")

        for (linkID, switchID) in [("editor-snooze", "snooze-enabled"),
                                   ("editor-challenge", "challenge-mixing")] {
            let link = app.buttons[linkID]
            for _ in 0..<5 where !link.isHittable { app.swipeUp() }
            XCTAssertTrue(link.isHittable); link.tap()
            let toggle = app.switches[switchID]
            XCTAssertTrue(toggle.waitForExistence(timeout: 5))
            let original = try XCTUnwrap(toggle.value as? String)
            toggle.coordinate(withNormalizedOffset: CGVector(dx: 1, dy: 0.5))
                .withOffset(CGVector(dx: -25, dy: 0)).tap()
            let changed = original == "1" ? "0" : "1"
            XCTAssertEqual(toggle.value as? String, changed)
            capture(app, name: "Changed " + linkID + " settings")
            let back = app.buttons.matching(identifier: "BackButton").firstMatch
            XCTAssertTrue(back.waitForExistence(timeout: 5)); back.tap()
            XCTAssertTrue(app.navigationBars["New alarm"].waitForExistence(timeout: 5))
            XCTAssertFalse(app.buttons["Discard changes"].exists)
            XCTAssertTrue(app.buttons["save-alarm"].exists)
            XCTAssertEqual(title.value as? String, "Nested Back regression")

            // Reopen to prove that Back preserves the changed subpage value.
            for _ in 0..<5 where !link.isHittable { app.swipeUp() }
            link.tap()
            XCTAssertTrue(toggle.waitForExistence(timeout: 5))
            XCTAssertEqual(toggle.value as? String, changed)
            // Duo remains a phone on its inner display and can move Back to a
            // side toolbar. Use a window-edge gesture only when native Back
            // sits at that edge; otherwise exercise the native button again.
            if back.frame.minX < 60 {
                let start = app.coordinate(withNormalizedOffset: CGVector(dx: 0.01, dy: 0.5))
                let end = app.coordinate(withNormalizedOffset: CGVector(dx: 0.8, dy: 0.5))
                start.press(forDuration: 0.05, thenDragTo: end)
            } else { back.tap() }
            XCTAssertTrue(app.navigationBars["New alarm"].waitForExistence(timeout: 5))
            XCTAssertFalse(app.buttons["Discard changes"].exists)
            XCTAssertEqual(title.value as? String, "Nested Back regression")
        }
        capture(app, name: "Changed nested settings return to intact configuration")
        app.buttons["discard-draft"].tap()
        XCTAssertTrue(app.buttons["Discard changes"].waitForExistence(timeout: 5))
        app.buttons["Discard changes"].tap()
        XCTAssertTrue(app.buttons["add-alarm"].waitForExistence(timeout: 5))
        XCTAssertFalse(app.buttons["save-alarm"].exists)
    }

    func testIPadSwitchingAlarmsResolvesUnsavedChanges() throws {
        guard UIDevice.current.userInterfaceIdiom == .pad else { throw XCTSkip("iPad sidebar switching flow") }
        let orientation = XCUIDevice.shared.orientation
        XCUIDevice.shared.orientation = .landscapeLeft
        defer { XCUIDevice.shared.orientation = orientation }
        let app = launchEditingApp()
        let firstName = "Sidebar first alarm"
        let secondName = "Sidebar second alarm"
        addDisabledAlarm(firstName, in: app)
        addDisabledAlarm(secondName, in: app)
        let first = alarmButton(firstName, in: app)
        let second = alarmButton(secondName, in: app)
        first.tap()
        let title = app.textFields["editor-title"]
        XCTAssertTrue(title.waitForExistence(timeout: 5))
        title.tap(); title.typeText(" unfinished")
        app.buttons["editor-keyboard-done"].tap()
        let editedTitle = title.value as? String
        XCTAssertNotEqual(editedTitle, firstName)
        second.tap()
        XCTAssertTrue(app.buttons["Discard changes"].waitForExistence(timeout: 5))
        capture(app, name: "iPad resolves changes before switching alarms")
        app.buttons["Keep editing"].tap()
        XCTAssertEqual(title.value as? String, editedTitle)
        second.tap()
        app.buttons["Discard changes"].tap()
        XCTAssertTrue(title.waitForExistence(timeout: 5))
        XCTAssertEqual(title.value as? String, secondName)
        first.tap() // Unchanged second editor can switch directly.
        XCTAssertEqual(title.value as? String, firstName)
        XCTAssertFalse(app.buttons["Discard changes"].exists)
        app.buttons["discard-draft"].tap()
        XCTAssertFalse(app.buttons["save-alarm"].exists)
    }

    private func launchEditingApp() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchEnvironment["MATHALARM_HOSTED_TESTS"] = "0"
        app.launchArguments = ["-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launch()
        for _ in 0..<3 {
            let announcement = app.buttons["announcementGotIt"]
            if announcement.waitForExistence(timeout: 2) { announcement.tap() }
        }
        return app
    }

    func testEnabledAlarmCanBeEditedRepeatedlyWithRealAlarmKit() throws {
        // System Settings automation must start from a known orientation rather
        // than inheriting the preceding sidebar/rotation test's device state.
        XCUIDevice.shared.orientation = .portrait
        let app = launchEditingApp()
        app.buttons["add-alarm"].tap()
        let title = app.textFields["editor-title"]
        XCTAssertTrue(title.waitForExistence(timeout: 5))
        let name = "AlarmKit edit " + UUID().uuidString.prefix(8)
        title.tap(); title.typeText(name)
        app.buttons["editor-keyboard-done"].tap()
        app.buttons["save-alarm"].tap()
        let allow = XCUIApplication(bundleIdentifier: "com.apple.springboard").buttons["Allow"]
        if allow.waitForExistence(timeout: 5) { allow.tap() }
        let settingsLink = app.buttons["Go to Settings"]
        if settingsLink.exists {
            // iOS 27 iPad Simulator accepts this grant through Settings/Maestro,
            // but its XCTest switch actions do not change authorization. Keep
            // that setup limitation distinct from the live save assertions.
            guard UIDevice.current.userInterfaceIdiom != .pad else {
                throw XCTSkip("Grant Alarms in Settings before the iPad live AlarmKit save test; see docs/testing.md")
            }
            settingsLink.tap()
            let settings = XCUIApplication(bundleIdentifier: "com.apple.Preferences")
            let alarms = settings.switches["Alarms"]
            if !alarms.waitForExistence(timeout: 2) {
                if !settings.searchFields["Search Apps"].exists {
                    let apps = settings.buttons["Apps"]
                    for _ in 0..<8 where !apps.isHittable { settings.swipeUp() }
                    XCTAssertTrue(apps.isHittable, settings.debugDescription); apps.tap()
                }
                let search = settings.searchFields["Search Apps"]
                XCTAssertTrue(search.waitForExistence(timeout: 5)); search.tap()
                search.typeText("Math Alarm")
                let mathAlarm = settings.buttons["com.timilehinaregbesola.mathalarm"]
                XCTAssertTrue(mathAlarm.waitForExistence(timeout: 5), settings.debugDescription)
                mathAlarm.tap()
            }
            XCTAssertTrue(alarms.waitForExistence(timeout: 10), settings.debugDescription)
            if alarms.value as? String == "0" {
                alarms.coordinate(withNormalizedOffset: CGVector(dx: 0.95, dy: 0.5)).tap()
            }
            let granted = XCTNSPredicateExpectation(predicate: NSPredicate(format: "value == %@", "1"), object: alarms)
            XCTAssertEqual(XCTWaiter.wait(for: [granted], timeout: 5), .completed, settings.debugDescription)
            app.activate()
        }
        XCTAssertTrue(app.buttons["save-alarm"].waitForNonExistence(timeout: 15), app.debugDescription)
        for suffix in [" first", " second"] {
            let row = alarmButton(name, in: app)
            XCTAssertTrue(row.waitForExistence(timeout: 5)); row.tap()
            XCTAssertTrue(title.waitForExistence(timeout: 5))
            title.tap(); title.typeText(suffix)
            app.buttons["editor-keyboard-done"].tap()
            app.buttons["save-alarm"].tap()
            XCTAssertTrue(app.buttons["save-alarm"].waitForNonExistence(timeout: 15), app.debugDescription)
            XCTAssertTrue(alarmButton(name, in: app).exists)
        }
        // Snooze/challenge-only edits take the same metadata update path as labels.
        alarmButton(name, in: app).tap()
        let snooze = app.buttons["editor-snooze"]
        for _ in 0..<5 where !snooze.isHittable { app.swipeUp() }
        snooze.tap()
        let enabled = app.switches["snooze-enabled"]
        XCTAssertTrue(enabled.waitForExistence(timeout: 5))
        enabled.coordinate(withNormalizedOffset: CGVector(dx: 1, dy: 0.5))
            .withOffset(CGVector(dx: -25, dy: 0)).tap()
        app.buttons.matching(identifier: "BackButton").firstMatch.tap()
        app.buttons["save-alarm"].tap()
        XCTAssertTrue(app.buttons["save-alarm"].waitForNonExistence(timeout: 15), app.debugDescription)

        alarmButton(name, in: app).tap()
        let sound = app.buttons["editor-sound"]
        for _ in 0..<5 where !sound.isHittable { app.swipeUp() }
        sound.tap()
        app.buttons["tone-alarm_daybreak"].tap()
        app.buttons["sound-done"].tap()
        app.buttons["save-alarm"].tap()
        XCTAssertTrue(app.buttons["save-alarm"].waitForNonExistence(timeout: 15), app.debugDescription)
        capture(app, name: "Enabled alarm after repeated AlarmKit edits")
        swipeToDelete(alarmButton(name, in: app), in: app)
    }

    func testPreviewTransitionsPreserveDraftAndLiteralLabel() {
        let app = launchEditingApp()
        let name = "Coffee + maths " + UUID().uuidString.prefix(8)
        addDisabledAlarm(name, in: app)
        alarmButton(name, in: app).tap()
        let title = app.textFields["editor-title"]
        XCTAssertTrue(title.waitForExistence(timeout: 5))
        XCTAssertEqual(title.value as? String, name)
        for _ in 0..<2 {
            let preview = app.buttons["editor-test-alarm"]
            for _ in 0..<5 where !preview.isHittable { app.swipeUp() }
            preview.tap()
            let answer = app.textFields["challenge-answer"]
            XCTAssertTrue(answer.waitForExistence(timeout: 3))
            XCTAssertFalse(app.otherElements["challenge-initializing"].exists)
            answer.tap(); answer.typeText("-999999")
            app.buttons["challenge-keyboard-done"].tap()
            app.buttons["challenge-submit"].tap()
            XCTAssertTrue(app.staticTexts["challenge-feedback"].waitForExistence(timeout: 5))
            capture(app, name: "Instant preview with wrong-answer feedback")
            app.buttons["challenge-cancel-preview"].tap()
            XCTAssertTrue(app.buttons["save-alarm"].waitForExistence(timeout: 5))
        }
        for _ in 0..<5 where !title.isHittable { app.swipeDown() }
        XCTAssertEqual(title.value as? String, name)
        app.buttons["save-alarm"].tap()
        XCTAssertTrue(app.buttons["save-alarm"].waitForNonExistence(timeout: 10))
        swipeToDelete(alarmButton(name, in: app), in: app)
    }
}
