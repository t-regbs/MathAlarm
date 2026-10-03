import SwiftUI
import AVKit
import app

/// A window retains the silent tutorial across the handoff to Settings.
/// Audio-session changes stay with the existing Kotlin platform audio owner.
@MainActor
final class NativeAlarmPermissionGuidePlayer: NSObject, ObservableObject, AVPictureInPictureControllerDelegate {
    let player = AVQueuePlayer()
    let layer = AVPlayerLayer()
    @Published private(set) var possible = false
    @Published private(set) var starting = false
    @Published private(set) var floating = false
    @Published private(set) var playing = false
    @Published private(set) var available = false
    @Published private(set) var ready = false
    @Published private(set) var poster: UIImage?
    private var looper: AVPlayerLooper?
    private var pip: AVPictureInPictureController?
    private var possibilityObservation: NSKeyValueObservation?
    private var readinessObservation: NSKeyValueObservation?
    private var startTimeout: Task<Void, Never>?
    private var onStarted: (() -> Void)?
    private let audioOwner = "settings-guide/\(UUID().uuidString)"
    private var ownsAudio = false

    override init() {
        super.init()
        player.isMuted = true
        layer.player = player
        layer.videoGravity = .resizeAspect
        readinessObservation = layer.observe(\.isReadyForDisplay, options: [.initial, .new]) { [weak self] layer, _ in
            let value = layer.isReadyForDisplay
            Task { @MainActor in self?.ready = value }
        }
        pip = AVPictureInPictureController(playerLayer: layer)
        pip?.delegate = self
        pip?.canStartPictureInPictureAutomaticallyFromInline = false
        pip?.requiresLinearPlayback = true
        possibilityObservation = pip?.observe(\.isPictureInPicturePossible, options: [.initial, .new]) { [weak self] controller, _ in
            let value = controller.isPictureInPicturePossible
            Task { @MainActor in self?.possible = value }
        }
    }

    func prepare(autoplay: Bool) {
        if looper == nil {
            let language = Bundle.main.preferredLocalizations.first?.components(separatedBy: "-").first ?? "en"
            let url = Bundle.main.url(forResource: "alarm-settings-guide-\(language)", withExtension: "mp4")
                ?? Bundle.main.url(forResource: "alarm-settings-guide-en", withExtension: "mp4")
            guard let url else { return }
            poster = UIImage(contentsOfFile: url.deletingPathExtension().appendingPathExtension("png").path)
            looper = AVPlayerLooper(player: player, templateItem: AVPlayerItem(url: url))
            available = true
        }
        acquireAudio()
        if autoplay { play() }
    }

    private func acquireAudio() {
        guard !ownsAudio else { return }
        ownsAudio = IosApplication.shared.beginSettingsGuideAudio(ownerId: audioOwner) { [weak self] in
            MainActor.assumeIsolated { self?.stop() }
        }
    }

    func togglePlayback() { playing ? pause() : play() }
    private func play() { player.play(); playing = true }
    private func pause() { player.pause(); playing = false }

    /// User-initiated PiP only. Open Settings after AVKit confirms the floating window.
    func openSettingsWithGuide(onStarted: @escaping () -> Void) {
        guard !starting else { return }
        guard available, AVPictureInPictureController.isPictureInPictureSupported(), let pip else {
            onStarted()
            return
        }
        acquireAudio()
        guard ownsAudio else { onStarted(); return }
        self.onStarted = onStarted
        starting = true
        play()
        let waitForFrame = !pip.isPictureInPicturePossible
        if !waitForFrame { pip.startPictureInPicture() }
        startTimeout = Task { @MainActor [weak self] in
            guard let self else { return }
            // A quick tap can precede the first decoded frame. Wait briefly for AVKit.
            if waitForFrame {
                for _ in 0..<40 {
                    try? await Task.sleep(for: .milliseconds(50))
                    guard !Task.isCancelled, self.starting else { return }
                    if pip.isPictureInPicturePossible { pip.startPictureInPicture(); break }
                }
                if !pip.isPictureInPicturePossible { self.openWithoutPictureInPicture(); return }
            }
            try? await Task.sleep(for: .seconds(4))
            guard !Task.isCancelled, self.starting else { return }
            self.openWithoutPictureInPicture()
        }
    }

    private func openWithoutPictureInPicture() {
        let action = onStarted
        stop()
        action?()
    }

    func stop() {
        onStarted = nil
        startTimeout?.cancel(); startTimeout = nil
        starting = false
        if pip?.isPictureInPictureActive == true { pip?.stopPictureInPicture() }
        floating = false
        pause()
        if ownsAudio {
            ownsAudio = false
            _ = IosApplication.shared.endSettingsGuideAudio(ownerId: audioOwner)
        }
    }

    func pictureInPictureControllerDidStartPictureInPicture(_ pictureInPictureController: AVPictureInPictureController) {
        startTimeout?.cancel(); startTimeout = nil
        guard starting, let action = onStarted else { stop(); return }
        starting = false
        floating = true
        onStarted = nil
        action()
    }

    func pictureInPictureController(_ pictureInPictureController: AVPictureInPictureController,
                                   failedToStartPictureInPictureWithError error: Error) { openWithoutPictureInPicture() }

    func pictureInPictureControllerDidStopPictureInPicture(_ pictureInPictureController: AVPictureInPictureController) { stop() }

    func pictureInPictureController(_ pictureInPictureController: AVPictureInPictureController,
        restoreUserInterfaceForPictureInPictureStopWithCompletionHandler completionHandler: @escaping (Bool) -> Void) {
        completionHandler(true) // AVKit returns to this scene; its retained editor is already present.
    }

    isolated deinit {
        startTimeout?.cancel()
        pip?.delegate = nil
        if pip?.isPictureInPictureActive == true { pip?.stopPictureInPicture() }
        player.pause()
        if ownsAudio { _ = IosApplication.shared.endSettingsGuideAudio(ownerId: audioOwner) }
    }
}

private final class AlarmGuideVideoView: UIView {
    let videoLayer: AVPlayerLayer
    init(layer: AVPlayerLayer) {
        videoLayer = layer
        super.init(frame: .zero)
        self.layer.addSublayer(layer)
        backgroundColor = .secondarySystemBackground
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }
    override func layoutSubviews() { super.layoutSubviews(); videoLayer.frame = bounds }
}

private struct AlarmGuideVideo: UIViewRepresentable {
    let player: NativeAlarmPermissionGuidePlayer
    func makeUIView(context: Context) -> UIView { AlarmGuideVideoView(layer: player.layer) }
    func updateUIView(_ uiView: UIView, context: Context) { }
}

@MainActor
struct NativeAlarmPermissionGuide: View {
    @ObservedObject var player: NativeAlarmPermissionGuidePlayer
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.dynamicTypeSize) private var typeSize
    let openSettings: () -> Void
    let keepEditing: () -> Void

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    if player.available {
                        AlarmGuideVideo(player: player)
                            .aspectRatio(3.0 / 4.0, contentMode: .fit)
                            .frame(maxWidth: 210)
                            .overlay {
                                if !player.ready, let poster = player.poster {
                                    Image(uiImage: poster).resizable().scaledToFit()
                                }
                            }
                            .clipShape(RoundedRectangle(cornerRadius: 20))
                            .accessibilityHidden(true)
                            .overlay(alignment: .bottomTrailing) {
                                Button { player.togglePlayback() } label: {
                                    Image(systemName: player.playing ? "pause.fill" : "play.fill")
                                        .frame(width: 28, height: 28)
                                }
                                .buttonStyle(.glass)
                                .accessibilityLabel(NativeStrings.text(player.playing ? "Pause guide" : "Play guide"))
                                .padding(8)
                            }
                    }
                    VStack(alignment: .leading, spacing: 16) {
                        Text("Turn on Alarms for Math Alarm in Settings, then return to finish saving.")
                        if typeSize.isAccessibilitySize {
                            VStack(alignment: .leading, spacing: 12) {
                                stepRow("1", title: NativeStrings.text("Apps"))
                                stepRow("2", title: "Math Alarm")
                                stepRow("3", title: NativeStrings.text("Alarms"))
                            }
                            .font(.subheadline)
                        } else {
                            HStack(alignment: .top, spacing: 12) {
                                step("1", title: NativeStrings.text("Apps"))
                                Image(systemName: "chevron.right").accessibilityHidden(true)
                                step("2", title: "Math Alarm")
                                Image(systemName: "chevron.right").accessibilityHidden(true)
                                step("3", title: NativeStrings.text("Alarms"))
                            }
                            .font(.subheadline)
                        }
                    }
                    VStack(spacing: 12) {
                        Button {
                            player.openSettingsWithGuide(onStarted: openSettings)
                        } label: {
                            Text("Go to Settings").multilineTextAlignment(.center)
                                .frame(maxWidth: .infinity, minHeight: 32)
                        }
                            .buttonStyle(.glassProminent)
                            .tint(MatAlarmPalette.action)
                            .disabled(player.starting)
                            .accessibilityIdentifier("alarm-permission-settings")
                    }
                }
                .padding(24)
                .frame(maxWidth: 480)
                .frame(maxWidth: .infinity)
            }
            .matAlarmContent()
            .navigationTitle("Enable alarms in Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(action: keepEditing) { Image(systemName: "xmark") }
                        .accessibilityLabel(NativeStrings.text("Keep editing"))
                        .accessibilityIdentifier("alarm-permission-guide-close")
                        .disabled(player.starting)
                }
            }
        }
        .tint(MatAlarmPalette.accent)
        .interactiveDismissDisabled(player.starting)
        .onAppear { player.prepare(autoplay: !reduceMotion) }
    }

    private func step(_ number: String, title: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(verbatim: number).foregroundStyle(MatAlarmPalette.accent)
            Text(verbatim: title).fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
    }

    private func stepRow(_ number: String, title: String) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 16) {
            Text(verbatim: number).foregroundStyle(MatAlarmPalette.accent).fixedSize()
            Text(verbatim: title).fixedSize(horizontal: false, vertical: true)
        }
        .accessibilityElement(children: .combine)
    }
}

#if DEBUG
/// Isolated native client entry point: no drafts, registrations or permission grants.
@MainActor
struct NativeAlarmPermissionGuideVerification: View {
    static var enabled: Bool { ProcessInfo.processInfo.arguments.contains("--verify-alarm-settings-guide") }
    @StateObject private var player = NativeAlarmPermissionGuidePlayer()
    @Environment(\.scenePhase) private var scenePhase
    @State private var leftApp = false

    var body: some View {
        NativeAlarmPermissionGuide(player: player, openSettings: {
            if let url = URL(string: UIApplication.openSettingsURLString) {
                UIApplication.shared.open(url)
            }
        }, keepEditing: { player.stop() })
        .onChange(of: scenePhase) { phase in
            if phase != .active { leftApp = true }
            else if leftApp { player.stop(); leftApp = false }
        }
    }
}
#endif
