import XCTest

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

    func testBackCancelRetainsThenClosesExactDraft() {
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
        let back = app.navigationBars.buttons.matching(identifier: "BackButton").firstMatch
        XCTAssertTrue(back.waitForExistence(timeout: 5)); back.tap()
        XCTAssertTrue(app.buttons["save-alarm"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["editor-snooze"].exists)
        for _ in 0..<5 where !title.isHittable { app.swipeDown() }
        XCTAssertEqual(title.value as? String, "M7 exact Back draft")
        app.buttons["discard-draft"].tap()
        XCTAssertTrue(app.buttons["Discard changes"].waitForExistence(timeout: 5))
        app.buttons["Discard changes"].tap()
        XCTAssertTrue(addAlarm.waitForExistence(timeout: 5))
        XCTAssertFalse(app.buttons["save-alarm"].exists)
        let capture = XCTAttachment(screenshot: app.screenshot())
        capture.name = "Back retained editor; Cancel returned to list"; capture.lifetime = .keepAlways; add(capture)
    }
}
