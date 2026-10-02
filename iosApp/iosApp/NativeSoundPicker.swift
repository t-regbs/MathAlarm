import SwiftUI
import KMPObservableViewModelSwiftUI
import app

/// Native picker state belongs to the retained editor session, above detail replacement.
/// It holds presentation-only selection; Kotlin still owns the editor draft and audio.
@MainActor
final class NativeSoundSelection: ObservableObject {
    let ownerID: String
    @Published private(set) var pendingTone: String
    @Published private(set) var previewingTone: String?
    @Published private(set) var previewMessage: String?
    private(set) var originalTone: String
    private(set) var active = false
    private var playbackGeneration = 0
    private var presentationGeneration = 0
    private var visiblePresentation: Int?

    init(sessionID: String, currentTone: String) {
        ownerID = "tone-preview/\(sessionID)"
        let selectionID = Self.selectionID(currentTone)
        originalTone = selectionID
        pendingTone = selectionID
    }

    var hasChanges: Bool { pendingTone != originalTone }

    func begin(currentTone: String) {
        guard !active else { return }
        active = true
        originalTone = Self.selectionID(currentTone)
        pendingTone = originalTone
        previewMessage = nil
    }

    private static func selectionID(_ tone: String) -> String {
        AlarmSoundCatalog.shared.find(tone: tone)?.id ?? (tone.isEmpty ? AlarmSoundCatalog.shared.DEFAULT_SOUND : tone)
    }

    func choose(_ tone: String) { pendingTone = tone }

    func beginPresentation() -> Int {
        presentationGeneration += 1
        visiblePresentation = presentationGeneration
        return presentationGeneration
    }

    func endPresentation(_ generation: Int) {
        guard visiblePresentation == generation else { return }
        stop()
        visiblePresentation = nil
    }

    func stopPresentation(_ generation: Int) {
        guard visiblePresentation == generation else { return }
        stop()
    }

    func togglePreview(_ tone: String) {
        if previewingTone == tone { stop(); return }
        stop()
        playbackGeneration += 1
        let generation = playbackGeneration
        previewingTone = tone
        previewMessage = nil
        AlarmTonePreview.shared.play(ownerId: ownerID, tone: tone) { [weak self] result in
            // Platform playback and completion run on the main thread.
            MainActor.assumeIsolated {
                guard let self, self.playbackGeneration == generation else { return }
                self.previewingTone = nil
                switch result {
                case .blockedByAlarm:
                    self.previewMessage = "A delivered alarm has priority over tone previews."
                case .interruptedByAlarm:
                    self.previewMessage = "Tone preview was interrupted by an alarm."
                case .unavailable:
                    self.previewMessage = "Unable to play this tone preview."
                default: break
                }
            }
        }
    }

    /// Observation/layout disappearance stops only this preview; staged selection survives.
    func stop() {
        playbackGeneration += 1
        AlarmTonePreview.shared.stop(ownerId: ownerID)
        previewingTone = nil
    }

    /// Called only on explicit Back/discard/Done or when its editor is closed.
    func finish() {
        stop()
        active = false
        visiblePresentation = nil
        previewMessage = nil
    }
}

enum NativeSoundPresentation {
    static func displayName(_ tone: String) -> String {
        AlarmSoundCatalog.shared.find(tone: tone)?.displayName ?? NativeStrings.text("Default alarm tone")
    }

    static func description(_ id: String) -> String {
        switch id {
        case "alarm_daybreak": "Soft mallets · gentle"
        case "alarm_orbit": "Flowing synth · bright"
        case "alarm_rally": "Electronic rhythm · insistent"
        case "alarm_glass_garden": "Shimmering bells · clear"
        case "alarm_stepping_stones": "Wooden mallets · rhythmic"
        default: "Paired tones · distinct"
        }
    }
}

@MainActor
struct NativeSoundPicker: View {
    @ObservedViewModel var model: AlarmSettingsViewModel
    @ObservedObject var selection: NativeSoundSelection
    let onClose: () -> Void
    @Environment(\.scenePhase) private var scenePhase
    @State private var discardConfirmation = false
    @State private var presentation: Int?

    var body: some View {
        List {
            Section {
                Text("Choose a tone, then tap Done to apply it to this draft.")
                    .foregroundStyle(.secondary)
            }
            if AlarmSoundCatalog.shared.find(tone: selection.pendingTone) == nil {
                Section("Current sound") {
                    Text("This sound is unavailable. Orbit will play instead.")
                        .foregroundStyle(.secondary)
                }
            }
            Section {
                ForEach(AlarmSoundCatalog.shared.sounds, id: \.id) { sound in
                    HStack(alignment: .center, spacing: 16) {
                        Button { selection.choose(sound.id) } label: {
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(sound.displayName).foregroundStyle(.primary)
                                    Text(NativeStrings.text(NativeSoundPresentation.description(sound.id)))
                                        .font(.footnote).foregroundStyle(.secondary)
                                }
                                Spacer(minLength: 0)
                                if selection.pendingTone == sound.id {
                                    Image(systemName: "checkmark").accessibilityHidden(true)
                                }
                            }
                            .frame(minHeight: 44)
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                        .accessibilityIdentifier("tone-\(sound.id)")
                        .accessibilityAddTraits(selection.pendingTone == sound.id ? [.isSelected] : [])
                        Button { selection.togglePreview(sound.id) } label: {
                            Image(systemName: selection.previewingTone == sound.id ? "stop.fill" : "play.fill")
                                .frame(minWidth: 44, minHeight: 44)
                        }
                        .buttonStyle(.borderless)
                        .accessibilityLabel(Text(NativeStrings.text(selection.previewingTone == sound.id ? "Stop preview" : "Preview")) + Text(" ") + Text(sound.displayName))
                        .accessibilityHint(selection.previewingTone == sound.id
                            ? Text("Stop preview") : Text("Preview this tone without changing your selection."))
                        .accessibilityIdentifier("preview-\(sound.id)")
                    }
                }
            }
            if let message = selection.previewMessage {
                Section { Text(NativeStrings.text(message)).foregroundStyle(.secondary) }
                    .accessibilityIdentifier("sound-preview-message")
            }
        }
        .navigationTitle("Sound library")
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Back") {
                    if selection.hasChanges { discardConfirmation = true }
                    else { close() }
                }.accessibilityIdentifier("sound-back")
            }
            ToolbarItem(placement: .confirmationAction) {
                Button("Done") {
                    guard !model.state.isSaving else { return }
                    model.onEvent(event: AddEditAlarmEvent.OnToneChange(value: selection.pendingTone))
                    close()
                }
                .disabled(model.state.isSaving)
                .accessibilityIdentifier("sound-done")
            }
        }
        .confirmationDialog("Discard sound selection?", isPresented: $discardConfirmation, titleVisibility: .visible) {
            Button("Discard selection", role: .destructive) { close() }
            Button("Keep choosing", role: .cancel) { }
        }
        .onAppear {
            selection.begin(currentTone: model.state.tone)
            presentation = selection.beginPresentation()
        }
        .onDisappear { if let presentation { selection.endPresentation(presentation) } }
        .onChange(of: scenePhase) { _, value in
            if value != .active, let presentation { selection.stopPresentation(presentation) }
        }
        .onChange(of: selection.previewMessage) { _, value in
            if let value { NativeAccessibility.announce(NativeStrings.text(value)) }
        }
    }

    private func close() {
        selection.finish()
        onClose()
    }
}
