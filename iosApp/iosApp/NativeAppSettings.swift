import SwiftUI
import UIKit
import MessageUI
import KMPObservableViewModelSwiftUI
import app

/// Presentation state lives above sheets so a delivery or layout change cannot
/// acknowledge an announcement or advance its page as a side effect.
@MainActor
final class NativeAnnouncementPresentation: ObservableObject {
    static let supportedIDs = ["math-challenges-v1", "snooze-settings-v1"]
    @Published private(set) var ids: [String] = []
    @Published private(set) var page = 0
    @Published var presented = false
    @Published var snoozeExampleEnabled = true
    private(set) var automaticOffered = false
    private var resumeAfterInterruption = false

    var currentID: String? { ids.indices.contains(page) ? ids[page] : nil }

    func offerAutomatically(model: AppSettingsViewModel) {
        // A real alarm may interrupt a frozen session; resume its exact page.
        if resumeIfInterrupted() { return }
        guard !automaticOffered,
              !model.state.failures.contains(where: { $0.operation == .announcement }) else { return }
        automaticOffered = true
        begin(ids: model.state.unseenAnnouncementIds)
    }

    func reopen(model: AppSettingsViewModel) {
        model.refreshAnnouncements()
        guard !model.state.failures.contains(where: { $0.operation == .announcement }) else { return }
        begin(ids: model.state.announcementIds)
    }

    private func begin(ids: [String]) {
        self.ids = ids.filter { Self.supportedIDs.contains($0) }
        page = 0
        snoozeExampleEnabled = true
        resumeAfterInterruption = false
        presented = !self.ids.isEmpty
    }

    func move(to page: Int, model: AppSettingsViewModel) {
        guard ids.indices.contains(page), page != self.page,
              acknowledgeCurrent(model: model) else { return }
        self.page = page
    }

    /// Only explicit browsing/feature actions persist acknowledgement. Failed
    /// persistence keeps the page open and the shared failure available for retry.
    @discardableResult
    func finish(model: AppSettingsViewModel) -> Bool {
        guard acknowledgeCurrent(model: model) else { return false }
        presented = false
        ids = []
        page = 0
        return true
    }

    private func acknowledgeCurrent(model: AppSettingsViewModel) -> Bool {
        guard let id = currentID else { return false }
        model.acknowledgeAnnouncement(id: id)
        return model.state.seenAnnouncementIds.contains(id)
    }

    func interrupt() {
        guard presented else { return }
        resumeAfterInterruption = !ids.isEmpty
        presented = false
    }

    @discardableResult
    func resumeIfInterrupted() -> Bool {
        guard resumeAfterInterruption, !ids.isEmpty else { return false }
        resumeAfterInterruption = false
        presented = true
        return true
    }
}

/// App appearance belongs to the retained native window. Updating the window's
/// inherited trait avoids SwiftUI presentation preferences retaining Dark after
/// a switch to System. Unspecified follows later system appearance changes.
@MainActor
struct NativeWindowAppearance: UIViewRepresentable {
    let theme: AlarmPreferencesTheme

    final class Anchor: UIView {
        var theme: AlarmPreferencesTheme = .system

        override func didMoveToWindow() {
            super.didMoveToWindow()
            applyAppearance()
        }

        func applyAppearance() {
            let style: UIUserInterfaceStyle = theme == .light ? .light :
                (theme == .dark ? .dark : .unspecified)
            if let window, window.overrideUserInterfaceStyle != style {
                window.overrideUserInterfaceStyle = style
            }
        }
    }

    func makeUIView(context: Context) -> Anchor {
        let anchor = Anchor()
        anchor.theme = theme
        anchor.isUserInteractionEnabled = false
        anchor.isAccessibilityElement = false
        return anchor
    }

    func updateUIView(_ anchor: Anchor, context: Context) {
        anchor.theme = theme
        anchor.applyAppearance()
    }
}

@MainActor
struct NativeAppSettings: View {
    @ObservedViewModel var model: AppSettingsViewModel
    let openWhatsNew: () -> Void
    let onDone: () -> Void
    @StateObject private var external: NativeExternalPresentation

    init(model: AppSettingsViewModel, openWhatsNew: @escaping () -> Void, onDone: @escaping () -> Void,
         external: NativeExternalPresentation? = nil) {
        _model = ObservedViewModel(wrappedValue: model)
        self.openWhatsNew = openWhatsNew
        self.onDone = onDone
        _external = StateObject(wrappedValue: external ?? NativeExternalPresentation())
    }

    var body: some View {
        Form {
            Section("Color Theme") {
                Picker("Color Theme", selection: Binding(get: { model.state.theme }, set: { model.selectTheme(theme: $0) })) {
                    Text("Light").tag(AlarmPreferencesTheme.light)
                    Text("Dark").tag(AlarmPreferencesTheme.dark)
                    Text("System").tag(AlarmPreferencesTheme.system)
                }
                .accessibilityIdentifier("appThemePicker")
            }
            Section {
                Picker("Alarm Order", selection: Binding(get: { model.state.sortOrder }, set: { model.selectSortOrder(sortOrder: $0) })) {
                    Text("Creation Order").tag(AlarmPreferencesAlarmSortOrder.creation)
                    Text("Time").tag(AlarmPreferencesAlarmSortOrder.time)
                }
                .accessibilityIdentifier("alarmSortPicker")
            } footer: {
                Text("Choose whether alarms appear by when they were created or by the time they go off.")
            }
            Section {
                Button(action: openWhatsNew) {
                    settingsLabel("What’s new", detail: "Explore the latest features", symbol: "sparkles")
                }
                .accessibilityIdentifier("whatsNewButton")
            }
            Section("Help") {
                Button { external.presentFeedback() } label: {
                    settingsLabel("Send Feedback", detail: "Send feedback to the developer", symbol: "envelope")
                }
                .accessibilityIdentifier("feedbackButton")
                Button { external.presentShare() } label: {
                    settingsLabel("Share", detail: "Check out this cool alarm app", symbol: "square.and.arrow.up")
                }
                .accessibilityIdentifier("shareButton")
                .background(NativeExternalPresentationAnchor(presentation: external).allowsHitTesting(false))
            }
        }
        .navigationTitle("App Settings")
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Done", action: onDone).accessibilityIdentifier("appSettingsDone")
            }
        }
        .alert("Help", isPresented: Binding(get: { external.failure != nil }, set: { if !$0 { external.failure = nil } })) {
            Button("OK", role: .cancel) { external.failure = nil }
        } message: { Text(NativeStrings.text(external.failure ?? "")) }
    }

    private func settingsLabel(_ title: String, detail: String, symbol: String) -> some View {
        Label {
            VStack(alignment: .leading, spacing: 4) {
                Text(NativeStrings.text(title)).foregroundStyle(Color.primary)
                Text(NativeStrings.text(detail)).font(.subheadline).foregroundStyle(Color.secondary)
            }
            .fixedSize(horizontal: false, vertical: true)
        } icon: { Image(systemName: symbol).accessibilityHidden(true) }
        .accessibilityElement(children: .combine)
    }
}

@MainActor
struct NativeWhatsNew: View {
    @ObservedViewModel var model: AppSettingsViewModel
    @ObservedObject var presentation: NativeAnnouncementPresentation
    let onTryFeature: (String) -> Void

    var body: some View {
        ScrollView {
            if let id = presentation.currentID {
                VStack(alignment: .leading, spacing: 24) {
                    Text(NativeStrings.text(id == "math-challenges-v1" ? "More ways to wake up" : "Snooze on your terms"))
                        .font(.title).bold().accessibilityAddTraits(.isHeader)
                    announcementPreview(id: id)
                    if id == "math-challenges-v1" {
                        Text(String(format: NativeStrings.text("Solve up to %@ questions. Mix difficulties or build a custom challenge that works for you."), NativeStrings.number(MathChallenge.companion.MAX_QUESTIONS)))
                        Text(String(format: NativeStrings.text("Add or edit an alarm, then tap %@."), NativeStrings.text("Challenge settings")))
                            .foregroundStyle(.secondary)
                    } else {
                        Text("Set a snooze limit, choose a duration from 1–30 minutes, or turn snooze off. New alarms start with 3 snoozes of 5 minutes. Existing alarms keep their snooze limits.")
                        Text(String(format: NativeStrings.text("Open %@ to turn snooze on or off, or change its limit and duration."), NativeStrings.text("Snooze")))
                            .foregroundStyle(.secondary)
                    }
                    if presentation.ids.count > 1 {
                        HStack {
                            Button("Back") { presentation.move(to: presentation.page - 1, model: model) }
                                .disabled(presentation.page == 0)
                                .accessibilityIdentifier("announcementPrevious")
                            Spacer()
                            Text(String(format: NativeStrings.text("Feature %@ of %@"), NativeStrings.number(presentation.page + 1), NativeStrings.number(presentation.ids.count)))
                                .font(.caption).foregroundStyle(.secondary)
                                .accessibilityIdentifier("announcementPage")
                            Spacer()
                            Button("Next") { presentation.move(to: presentation.page + 1, model: model) }
                                .disabled(presentation.page == presentation.ids.count - 1)
                                .accessibilityIdentifier("announcementNext")
                        }
                    }
                    Button("Try it") {
                        if presentation.finish(model: model) { onTryFeature(id) }
                    }
                    .buttonStyle(.borderedProminent)
                    .accessibilityIdentifier("announcementTry")
                }
                .frame(maxWidth: 560, alignment: .leading)
                .padding()
                .frame(maxWidth: .infinity)
            }
        }
        .navigationTitle("What’s new")
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Got it") { presentation.finish(model: model) }
                    .accessibilityIdentifier("announcementGotIt")
            }
        }
        // No swipe-dismiss acknowledgement; the user chooses a semantic action.
        .interactiveDismissDisabled()
    }

    private func announcementPreview(id: String) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            if id == "math-challenges-v1" {
                Label(NativeStrings.questions(3), systemImage: "function").font(.headline)
                Text(String(format: NativeStrings.text("Easy: %@ · Medium: %@"), NativeStrings.number(2), NativeStrings.number(1)))
                    .font(.subheadline).foregroundStyle(Color.secondary)
                announcementEquation(index: 1, left: 14, right: 28, division: false, difficulty: "Easy")
                announcementEquation(index: 2, left: 36, right: 4, division: true, difficulty: "Easy")
                announcementEquation(index: 3, left: 128, right: 246, division: false, difficulty: "Medium")
                Text("Example challenge").font(.caption).foregroundStyle(Color.secondary)
            } else {
                // A self-contained native demonstration. It changes no alarm,
                // shared preferences or announcement acknowledgement.
                Toggle("Allow snooze", isOn: $presentation.snoozeExampleEnabled)
                    .accessibilityIdentifier("announcementSnoozeExample")
                if presentation.snoozeExampleEnabled {
                    Text(verbatim: "\(NativeStrings.minutes(count: 5)) · \(String(format: NativeStrings.text("Maximum %@"), NativeStrings.number(3)))")
                        .font(.title3)
                } else {
                    Text("Off").font(.title3)
                }
            }
        }
        .padding().frame(maxWidth: .infinity, alignment: .leading)
        .background(.background.secondary, in: RoundedRectangle(cornerRadius: 16))
    }

    private func announcementEquation(index: Int, left: Int, right: Int, division: Bool, difficulty: String) -> some View {
        let equation = "\(NativeStrings.number(left)) \(division ? "÷" : "+") \(NativeStrings.number(right))"
        let spoken = String(format: NativeStrings.text(division ? "%@ divided by %@" : "%@ plus %@"),
                            NativeStrings.number(left), NativeStrings.number(right))
        return ViewThatFits(in: .horizontal) {
            HStack(spacing: 12) {
                Text(NativeStrings.number(index)).font(.caption)
                Text(equation).font(.headline.monospacedDigit()).accessibilityLabel(spoken)
                Text(NativeStrings.text(difficulty)).font(.caption)
            }
            .fixedSize(horizontal: true, vertical: true)
            VStack(alignment: .leading, spacing: 8) {
                Text(NativeStrings.number(index)).font(.caption)
                Text(equation).font(.headline.monospacedDigit()).accessibilityLabel(spoken)
                Text(NativeStrings.text(difficulty)).font(.caption)
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.background, in: RoundedRectangle(cornerRadius: 12))
        .accessibilityElement(children: .combine)
    }
}

/// The UIKit anchor belongs to the tapped settings row in its active window.
/// It never searches all connected scenes for an arbitrary first/key window.
@MainActor
// MessageUI's Objective-C delegate protocol lacks actor annotations. Its UI
// callback is delivered on the main thread; retain the native owner's isolation.
final class NativeExternalPresentation: NSObject, ObservableObject, @preconcurrency MFMailComposeViewControllerDelegate {
    typealias URLCompletion = @MainActor @Sendable (Bool) -> Void
    @Published var failure: String?
    private(set) weak var anchor: UIViewController?
    private(set) weak var presentedController: UIViewController?
    private var generation = 0
    private let openURL: @MainActor (URL, @escaping URLCompletion) -> Void
    private let canSendMail: @MainActor () -> Bool
    static let supportEmail = "aregbestimi@gmail.com"
    static var shareText: String { "MathAlarm Clock\nSolve math problems to wake up! " + PlatformServices_iosKt.getAppShareUrl() }
    static var feedbackURL: URL { URL(string: "mailto:\(supportEmail)")! }

    init(openURL: (@MainActor (URL, @escaping URLCompletion) -> Void)? = nil,
         canSendMail: (@MainActor () -> Bool)? = nil) {
        self.openURL = openURL ?? { url, completion in
            UIApplication.shared.open(url) { accepted in
                Task { @MainActor in completion(accepted) }
            }
        }
        self.canSendMail = canSendMail ?? { MFMailComposeViewController.canSendMail() }
        super.init()
    }

    func attach(_ controller: UIViewController) { anchor = controller }

    func detach(_ controller: UIViewController) {
        guard anchor === controller else { return }
        cancelPresentation()
        anchor = nil
    }

    func cancelPresentation() {
        generation += 1
        presentedController?.dismiss(animated: false)
        presentedController = nil
    }

    private func readyAnchor() -> UIViewController? {
        guard let anchor, let scene = anchor.view.window?.windowScene,
              scene.activationState == .foregroundActive, anchor.presentedViewController == nil,
              presentedController == nil else {
            failure = "Couldn’t open the requested action. Try again."
            return nil
        }
        return anchor
    }

    func makeShareController(anchor: UIViewController) -> UIActivityViewController {
        let share = UIActivityViewController(activityItems: [Self.shareText], applicationActivities: nil)
        share.title = NativeStrings.text("Share Math Alarm")
        if anchor.traitCollection.userInterfaceIdiom == .pad { share.modalPresentationStyle = .popover }
        if let popover = share.popoverPresentationController {
            popover.sourceView = anchor.view
            popover.sourceRect = anchor.view.bounds
            popover.permittedArrowDirections = [.up, .down]
        }
        return share
    }

    func presentShare() {
        guard let anchor = readyAnchor() else { return }
        let share = makeShareController(anchor: anchor)
        generation += 1
        let token = generation
        share.completionWithItemsHandler = { [weak self] _, _, _, error in
            Task { @MainActor in self?.finishExternal(generation: token, failed: error != nil) }
        }
        presentedController = share
        anchor.present(share, animated: true)
    }

    func presentFeedback() {
        guard let anchor = readyAnchor() else { return }
        if canSendMail() {
            generation += 1
            let mail = MFMailComposeViewController()
            mail.setToRecipients([Self.supportEmail])
            mail.mailComposeDelegate = self
            presentedController = mail
            anchor.present(mail, animated: true)
        } else {
            // Keep the established mailto destination available to third-party
            // handlers, including devices with no configured Apple Mail account.
            generation += 1
            let token = generation
            openURL(Self.feedbackURL) { [weak self] accepted in
                Task { @MainActor in
                    guard let self, self.generation == token else { return }
                    if !accepted { self.failure = "No email app is available. Email aregbestimi@gmail.com to send feedback." }
                }
            }
        }
    }

    func finishExternal(generation: Int, failed: Bool) {
        guard self.generation == generation else { return }
        presentedController = nil
        if failed { failure = "Couldn’t open the requested action. Try again." }
    }

    func mailComposeController(_ controller: MFMailComposeViewController, didFinishWith result: MFMailComposeResult, error: (any Error)?) {
        guard presentedController === controller else { return }
        let token = generation
        let failed = result == .failed || error != nil
        // Keep the native presenter occupied until Mail has finished dismissing.
        // SwiftUI can then present an error without competing with its transition.
        controller.dismiss(animated: true) { [weak self] in
            guard let self, self.generation == token,
                  self.presentedController === controller else { return }
            self.presentedController = nil
            if failed { self.failure = "Couldn’t send feedback. Try again." }
        }
    }
}

private struct NativeExternalPresentationAnchor: UIViewControllerRepresentable {
    let presentation: NativeExternalPresentation
    func makeUIViewController(context: Context) -> UIViewController {
        let controller = UIViewController()
        controller.view.backgroundColor = .clear
        presentation.attach(controller)
        return controller
    }
    func updateUIViewController(_ controller: UIViewController, context: Context) { presentation.attach(controller) }
    static func dismantleUIViewController(_ controller: UIViewController, coordinator: NativeExternalPresentation) { coordinator.detach(controller) }
    func makeCoordinator() -> NativeExternalPresentation { presentation }
}
