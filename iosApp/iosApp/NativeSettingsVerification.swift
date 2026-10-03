#if DEBUG
import SwiftUI
import UIKit
import KMPObservableViewModelSwiftUI
import app

/// Additional production-app checks, run only in a disposable simulator app.
/// The URL opener is injected: verification never contacts feedback recipients.
@MainActor
enum NativeSettingsVerification {
    private static let marker = "MathAlarm.M6.settingsVerificationReady"
    private static var failureCapture: (() -> Void)?

    /// Invoke before shared bootstrap only for the explicit fresh fixture launch.
    static func prepareIfRequested() {
        guard ProcessInfo.processInfo.arguments.contains("--verify-m6-settings-fresh") else { return }
        let defaults = UserDefaults.standard
        for id in NativeAnnouncementPresentation.supportedIDs {
            defaults.removeObject(forKey: "mathalarm_seen_announcement_" + id)
        }
        for key in ["mathalarm_announcement_catalog", "mathalarm_announcement_batch", marker] {
            defaults.removeObject(forKey: key)
        }
    }

    private final class Presentation: ObservableObject {
        @Published var presented = false
    }

    private struct SettingsOwner: View {
        @StateViewModel var model: AppSettingsViewModel
        @ObservedObject var external: NativeExternalPresentation
        @ObservedObject var sessions: NativeWindowSessions
        @ObservedObject var presentation: Presentation
        var body: some View {
            ZStack {
                NativeSessionOwners(sessions: sessions)
                Color.clear
            }
            .background(NativeWindowAppearance(theme: model.state.theme).frame(width: 0, height: 0))
            .sheet(isPresented: $presentation.presented) {
                NavigationStack {
                    NativeAppSettings(model: model, openWhatsNew: {}, onDone: {}, external: external)
                }
            }
        }
    }

    static func run(window: UIWindow, parent: UIViewController) async {
        let model = SharedFeatures.shared.settings()
        let defaults = UserDefaults.standard
        let sessions = NativeWindowSessions()
        let first = sessions.openEditor(alarm: nil)
        first.model.onEvent(event: AddEditAlarmEvent.EnteredTitle(value: "M6 retained first draft"))
        first.model.onEvent(event: AddEditAlarmEvent.OnTestClick.shared)
        let seedResult = first.model.state.results.first { $0.event is AlarmSettingsViewModel.UiEventTestAlarm }!
        let seed = (seedResult.event as! AlarmSettingsViewModel.UiEventTestAlarm).alarm
        first.model.acknowledgeResult(id: seedResult.id)
        let secondAlarm = seed.doCopy(alarmId: -6001, newDateTime: seed.newDateTime,
            newHour: seed.newHour, newMinute: seed.newMinute, hour: seed.hour, minute: seed.minute,
            repeat: seed.repeat, repeatDays: seed.repeatDays, isOn: seed.isOn, difficulty: seed.difficulty,
            questionCount: seed.questionCount, challengeOperations: seed.challengeOperations,
            additionRange: seed.additionRange, factorRange: seed.factorRange, difficultyMix: seed.difficultyMix,
            alarmTone: seed.alarmTone, vibrate: seed.vibrate, snooze: seed.snooze,
            maxSnoozes: seed.maxSnoozes, snoozeCount: seed.snoozeCount, title: seed.title,
            isSaved: seed.isSaved, pendingTimes: seed.pendingTimes, scheduleInitialized: seed.scheduleInitialized,
            snoozedUntil: seed.snoozedUntil, activeAt: seed.activeAt, skippedDate: seed.skippedDate,
            scheduleError: seed.scheduleError, scheduleTimeZone: seed.scheduleTimeZone)
        let second = sessions.openEditor(alarm: secondAlarm)
        second.model.onEvent(event: AddEditAlarmEvent.EnteredTitle(value: "M6 retained second draft"))
        sessions.setEditorPath([.sound], id: second.id)
        let sound = sessions.soundSelection(sessionID: second.id)!
        sound.begin(currentTone: second.model.state.tone)
        sound.choose("alarm_rally")
        verificationCheck(sessions.beginPermissionRequest(id: second.id))
        let originalFirst = first.model
        let originalSecond = second.model

        var requestedURLs: [URL] = []
        let external = NativeExternalPresentation(openURL: { url, completion in
            requestedURLs.append(url)
            completion(false)
        }, canSendMail: { false })
        let presentation = Presentation()
        let host = UIHostingController(rootView: SettingsOwner(model: model, external: external, sessions: sessions, presentation: presentation))
        let originalWindowStyle = window.overrideUserInterfaceStyle
        // The preceding M5 fixture forces light mode for its own captures.
        // Production windows inherit system appearance; match that boundary.
        window.overrideUserInterfaceStyle = .unspecified
        defer { window.overrideUserInterfaceStyle = originalWindowStyle }
        await Task.yield()
        let inheritedSystemStyle = window.traitCollection.userInterfaceStyle
        // Match the production SwiftUI sheet and retained window owner.
        await wait("preceding presentation=\(String(describing: parent.presentedViewController))") {
            parent.presentedViewController == nil
        }
        parent.addChild(host)
        parent.view.addSubview(host.view)
        host.view.frame = parent.view.bounds
        host.didMove(toParent: parent)
        presentation.presented = true
        await wait("settings anchor attached=\(external.anchor?.view.window != nil) scene=\(String(describing: external.anchor?.view.window?.windowScene?.activationState)) presenter=\(String(describing: parent.presentedViewController))") {
            external.anchor?.view.window === window
        }
        var settingsController = external.anchor!
        while let containing = settingsController.parent { settingsController = containing }
        failureCapture = { captureAppearance(window: window, host: settingsController, name: "settings-presentation-timeout") }
        defer { failureCapture = nil }

        for theme in [AlarmPreferencesTheme.light, .dark, .system, .dark] {
            model.selectTheme(theme: theme)
            await wait("shared theme expected=\(theme.name) actual=\(model.state.theme.name)") { model.state.theme == theme }
            let expectedStyle: UIUserInterfaceStyle = theme == .dark ? .dark :
                (theme == .light ? .light : inheritedSystemStyle)
            await waitForNativeFormAppearance(expectedStyle, theme: theme.name, host: settingsController, window: window)
            await Task.yield()
            verificationCheck(defaults.integer(forKey: "mathalarm_theme_option") == Int(theme.ordinal))
            verificationCheck(!model.isClosed)
            verificationCheck(!originalFirst.isClosed && !originalSecond.isClosed)
            verificationCheck(sessions.editors.first(where: { $0.id == first.id })?.model === originalFirst)
            verificationCheck(sessions.selectedEditor?.model === originalSecond)
            verificationCheck(originalFirst.state.alarmTitle == "M6 retained first draft")
            verificationCheck(originalSecond.state.alarmTitle == "M6 retained second draft")
            verificationCheck(sessions.editorPaths[second.id] == [.sound] && sound.pendingTone == "alarm_rally")
            verificationCheck(sessions.permissionRequests.contains(second.id))
        }
        for sort in [AlarmPreferencesAlarmSortOrder.creation, .time] {
            model.selectSortOrder(sortOrder: sort)
            await wait("shared sort expected=\(sort.name) actual=\(model.state.sortOrder.name)") { model.state.sortOrder == sort }
            verificationCheck(defaults.integer(forKey: "mathalarm_alarm_sort_order") == Int(sort.ordinal))
        }
        let replacement = SharedFeatures.shared.settings()
        verificationCheck(replacement.state.theme == .dark && replacement.state.sortOrder == .time)
        replacement.close()
        VerificationResults.shared.pass("native settings all themes/sorts use persisted keys while two drafts, nested route, staged sound and permission guard retain owners")

        let announcements = NativeAnnouncementPresentation()
        let seenBefore = model.state.seenAnnouncementIds
        announcements.offerAutomatically(model: model)
        if announcements.presented {
            let originalID = announcements.currentID
            announcements.interrupt()
            verificationCheck(model.state.seenAnnouncementIds == seenBefore)
            announcements.offerAutomatically(model: model)
            verificationCheck(announcements.presented && announcements.currentID == originalID)
        }
        announcements.reopen(model: model)
        verificationCheck(announcements.ids == model.state.announcementIds)
        verificationCheck(!announcements.ids.contains("skip-next-alarm-v1"))
        let reopenedID = announcements.currentID
        announcements.snoozeExampleEnabled = false
        let seenBeforeDemonstration = model.state.seenAnnouncementIds
        announcements.interrupt()
        announcements.interrupt()
        verificationCheck(sessions.selectedEditor?.id == second.id)
        verificationCheck(announcements.resumeIfInterrupted())
        verificationCheck(announcements.presented && announcements.currentID == reopenedID)
        verificationCheck(!announcements.snoozeExampleEnabled && model.state.seenAnnouncementIds == seenBeforeDemonstration)
        verificationCheck(!announcements.resumeIfInterrupted())
        if announcements.ids.count > 1 {
            let id = announcements.currentID!
            announcements.move(to: 1, model: model)
            verificationCheck(model.state.seenAnnouncementIds.contains(id))
            verificationCheck(defaults.bool(forKey: "mathalarm_seen_announcement_" + id))
        }
        let lastID = announcements.currentID!
        verificationCheck(announcements.finish(model: model))
        verificationCheck(defaults.bool(forKey: "mathalarm_seen_announcement_" + lastID))
        verificationCheck(!announcements.presented)
        let acknowledged = model.state.seenAnnouncementIds
        announcements.offerAutomatically(model: model)
        verificationCheck(!announcements.presented)
        announcements.reopen(model: model)
        verificationCheck(announcements.presented && announcements.ids == model.state.announcementIds)
        verificationCheck(model.state.seenAnnouncementIds == acknowledged)
        let observer = SharedFeatures.shared.settings()
        verificationCheck(observer.state.seenAnnouncementIds == acknowledged)
        observer.close()
        VerificationResults.shared.pass("native announcement IDs/page survive interruption without acknowledgement; explicit browse/Got it persist and latest batch reopens")

        let anchor = external.anchor!
        let inspection = external.makeShareController(anchor: anchor)
        if anchor.traitCollection.userInterfaceIdiom == .pad {
            verificationCheck(inspection.modalPresentationStyle == .popover)
            verificationCheck(inspection.popoverPresentationController?.sourceView === anchor.view)
            verificationCheck(inspection.popoverPresentationController?.sourceRect == anchor.view.bounds)
        }
        verificationCheck(anchor.view.window?.windowScene === window.windowScene)
        verificationCheck(NativeExternalPresentation.shareText == "MathAlarm Clock\nSolve math problems to wake up! https://github.com/t-regbs/MathAlarm")
        external.presentShare()
        await wait("share controller=\(String(describing: external.presentedController)) anchor presentation=\(String(describing: anchor.presentedViewController)) scene=\(String(describing: anchor.view.window?.windowScene?.activationState))") {
            guard let share = external.presentedController as? UIActivityViewController else { return false }
            return anchor.presentedViewController === share && share.viewIfLoaded?.window === window &&
                !share.isBeingPresented && share.transitionCoordinator == nil
        }
        let share = external.presentedController as! UIActivityViewController
        // Exercise the actual UIKit cancellation callback without choosing a recipient.
        share.completionWithItemsHandler?(nil, false, nil, nil)
        share.dismiss(animated: false)
        await wait("share cancellation dismissed: \(presentationEvidence(external: external, anchor: anchor, settings: settingsController))") {
            external.presentedController == nil && anchor.presentedViewController == nil
        }
        verificationCheck(external.failure == nil && requestedURLs.isEmpty)
        external.presentShare()
        await wait("second share presentation settled: \(presentationEvidence(external: external, anchor: anchor, settings: settingsController))") {
            guard let share = external.presentedController as? UIActivityViewController else { return false }
            return anchor.presentedViewController === share && share.viewIfLoaded?.window === window &&
                !share.isBeingPresented && share.transitionCoordinator == nil
        }
        let failedShare = external.presentedController as! UIActivityViewController
        failedShare.completionWithItemsHandler?(nil, false, nil, NSError(domain: "M6.fixture", code: 1))
        failedShare.dismiss(animated: false)
        await wait("share error callback retained: \(presentationEvidence(external: external, anchor: anchor, settings: settingsController))") {
            external.failure != nil && external.presentedController == nil &&
                failedShare.presentingViewController == nil && failedShare.viewIfLoaded?.window == nil
        }
        await wait("share error alert visible: \(presentationEvidence(external: external, anchor: anchor, settings: settingsController))") {
            visiblePresentedAlert(in: settingsController)?.message == NativeStrings.text("Couldn’t open the requested action. Try again.")
        }
        external.failure = nil
        await wait("share error alert dismissed: \(presentationEvidence(external: external, anchor: anchor, settings: settingsController))") {
            settingsController.presentedViewController == nil && anchor.presentedViewController == nil
        }
        external.presentFeedback()
        await wait("unavailable feedback callback URLs=\(requestedURLs.count): \(presentationEvidence(external: external, anchor: anchor, settings: settingsController))") {
            external.failure != nil
        }
        verificationCheck(requestedURLs == [NativeExternalPresentation.feedbackURL])
        verificationCheck(external.presentedController == nil)
        await wait("unavailable feedback alert visible: \(presentationEvidence(external: external, anchor: anchor, settings: settingsController))") {
            visiblePresentedAlert(in: settingsController)?.message == NativeStrings.text("No email app is available. Email aregbestimi@gmail.com to send feedback.")
        }
        external.failure = nil
        await wait("unavailable feedback alert dismissed: \(presentationEvidence(external: external, anchor: anchor, settings: settingsController))") {
            settingsController.presentedViewController == nil && anchor.presentedViewController == nil
        }
        VerificationResults.shared.pass("production settings share presents in the tapped active scene with iPad anchor, cancellation/error callbacks; unavailable feedback handler reports failure using injected mailto opener")

        defaults.set(true, forKey: marker)
        sessions.closeWindow()
        presentation.presented = false
        await wait("cleanup presenter=\(String(describing: settingsController.presentingViewController)) anchor attached=\(external.anchor?.viewIfLoaded?.window != nil) parent presented=\(String(describing: parent.presentedViewController))") {
            external.anchor?.viewIfLoaded?.window == nil && settingsController.presentingViewController == nil
        }
        host.willMove(toParent: nil)
        host.view.removeFromSuperview()
        host.removeFromParent()
        model.close()
    }

    static func verifyFreshProcess() {
        guard UserDefaults.standard.bool(forKey: marker) else {
            preconditionFailure("M6 restart verification requires the preceding settings fixture")
        }
        let model = SharedFeatures.shared.settings()
        verificationCheck(model.state.theme == .dark && model.state.sortOrder == .time)
        verificationCheck(NativeAnnouncementPresentation.supportedIDs.allSatisfy { model.state.seenAnnouncementIds.contains($0) })
        let announcements = NativeAnnouncementPresentation()
        announcements.offerAutomatically(model: model)
        verificationCheck(!announcements.presented)
        announcements.reopen(model: model)
        verificationCheck(announcements.ids == model.state.announcementIds && announcements.presented)
        model.close()
        VerificationResults.shared.pass("fresh process restores theme/sort and announcement acknowledgement while latest batch remains reopenable")
    }

    private static func wait(_ context: @autoclosure () -> String = "condition", _ condition: () -> Bool) async {
        let deadline = ContinuousClock.now + .seconds(8)
        while !condition() {
            if ContinuousClock.now >= deadline {
                failureCapture?()
                print("BRIDGE FAILURE M6 settings wait: \(context())")
                preconditionFailure("M6 production presentation timed out: \(context())")
            }
            try? await Task.sleep(for: .milliseconds(20))
        }
    }

    private static func presentationEvidence(external: NativeExternalPresentation, anchor: UIViewController,
                                              settings: UIViewController) -> String {
        let active = external.presentedController
        return "external=\(String(describing: active)) attached=\(active?.viewIfLoaded?.window != nil) beingPresented=\(active?.isBeingPresented ?? false) beingDismissed=\(active?.isBeingDismissed ?? false) transition=\(String(describing: active?.transitionCoordinator)) anchorPresented=\(String(describing: anchor.presentedViewController)) settingsPresented=\(String(describing: settings.presentedViewController)) failure=\(external.failure ?? "nil") scene=\(String(describing: anchor.view.window?.windowScene?.activationState))"
    }

    private static func visiblePresentedAlert(in controller: UIViewController) -> UIAlertController? {
        if let alert = controller as? UIAlertController, alert.viewIfLoaded?.window != nil,
           !alert.isBeingPresented, alert.transitionCoordinator == nil { return alert }
        if let presented = controller.presentedViewController,
           let alert = visiblePresentedAlert(in: presented) { return alert }
        return controller.children.compactMap { visiblePresentedAlert(in: $0) }.first
    }

    /// Inspect visible native controls, since a UIHostingController boundary can
    /// retain inherited traits while SwiftUI assigns appearance to its controls.
    static func nativeControlAppearanceEvidence(in root: UIView) -> [String] {
        visibleNativeControls(in: root).map {
            "\(String(describing: type(of: $0))) style=\($0.traitCollection.userInterfaceStyle.rawValue) frame=\(NSCoder.string(for: $0.convert($0.bounds, to: $0.window)))"
        }
    }

    private static func visibleNativeControls(in root: UIView) -> [UIView] {
        guard !root.isHidden, root.alpha > 0.01, root.window != nil else { return [] }
        var controls: [UIView] = []
        if root is UITableView || root is UICollectionView || root is UITextField || root is UINavigationBar {
            controls.append(root)
        }
        return controls + root.subviews.flatMap { visibleNativeControls(in: $0) }
    }

    private static func waitForNativeFormAppearance(_ expected: UIUserInterfaceStyle, theme: String,
                                                    host: UIViewController, window: UIWindow) async {
        let deadline = ContinuousClock.now + .seconds(8)
        while true {
            let forms = visibleNativeControls(in: host.view).filter { $0 is UITableView || $0 is UICollectionView }
            if !forms.isEmpty, forms.allSatisfy({ $0.traitCollection.userInterfaceStyle == expected }) {
                captureAppearance(window: window, host: host, name: "settings-theme-" + theme.lowercased())
                print("BRIDGE TRACE M6 rendered native settings theme=\(theme) expected=\(expected.rawValue) controls=\(nativeControlAppearanceEvidence(in: host.view))")
                return
            }
            if ContinuousClock.now >= deadline {
                captureAppearance(window: window, host: host, name: "settings-theme-timeout-" + theme.lowercased())
                let evidence = nativeControlAppearanceEvidence(in: host.view).joined(separator: "\n")
                print("BRIDGE FAILURE M6 settings theme=\(theme) expected=\(expected.rawValue) host=\(host.traitCollection.userInterfaceStyle.rawValue) window=\(window.traitCollection.userInterfaceStyle.rawValue) override=\(window.overrideUserInterfaceStyle.rawValue)\n\(evidence)")
                preconditionFailure("M6 native Form appearance timed out; rendered screenshot and UIKit control evidence captured")
            }
            try? await Task.sleep(for: .milliseconds(20))
        }
    }

    private static func captureAppearance(window: UIWindow, host: UIViewController, name: String) {
        window.layoutIfNeeded()
        let screenshot = UIGraphicsImageRenderer(bounds: window.bounds).image { _ in
            window.drawHierarchy(in: window.bounds, afterScreenUpdates: true)
        }
        let directory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("native-ui-m6-settings", isDirectory: true)
        try! FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        try! screenshot.pngData()!.write(to: directory.appendingPathComponent(name + ".png"))
        let evidence = nativeControlAppearanceEvidence(in: host.view).joined(separator: "\n")
        try! evidence.write(to: directory.appendingPathComponent(name + ".txt"), atomically: true, encoding: .utf8)
    }
}
#endif
