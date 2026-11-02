import SwiftUI
import KMPObservableViewModelSwiftUI
import app

/// Preview state belongs to the retained editor session, above detail replacement.
/// Kotlin owns the selected tone in the editor draft and the audio player.
@MainActor
final class NativeSoundSelection: ObservableObject {
    let ownerID: String
    @Published private(set) var previewingTone: String?
    @Published private(set) var previewMessage: String?
    private var playbackGeneration = 0
    private var presentationGeneration = 0
    private var visiblePresentation: Int?

    init(sessionID: String) {
        ownerID = "tone-preview/\(sessionID)"
    }

    func beginPresentation() -> Int {
        presentationGeneration += 1
        visiblePresentation = presentationGeneration
        previewMessage = nil
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

    /// Observation/layout disappearance stops only this preview.
    func stop() {
        playbackGeneration += 1
        AlarmTonePreview.shared.stop(ownerId: ownerID)
        previewingTone = nil
    }

    /// Called when the editor owner is closed.
    func finish() {
        stop()
        visiblePresentation = nil
        previewMessage = nil
    }
}

enum NativeSoundPresentation {
    static func selectionID(_ tone: String) -> String {
        AlarmSoundCatalog.shared.find(tone: tone)?.id ?? (tone.isEmpty ? AlarmSoundCatalog.shared.DEFAULT_SOUND : tone)
    }

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
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @State private var presentation: Int?

    var body: some View {
        let selectedTone = NativeSoundPresentation.selectionID(model.state.tone)
        List {
            if AlarmSoundCatalog.shared.find(tone: selectedTone) == nil {
                Section("Current sound") {
                    Text("This sound is unavailable. Orbit will play instead.")
                        .foregroundStyle(.secondary)
                }
            }
            Section {
                ForEach(AlarmSoundCatalog.shared.sounds, id: \.id) { sound in
                    let layout = dynamicTypeSize.isAccessibilitySize
                        ? AnyLayout(VStackLayout(alignment: .leading, spacing: 12))
                        : AnyLayout(HStackLayout(alignment: .center, spacing: 16))
                    layout {
                        Button {
                            model.onEvent(event: AddEditAlarmEvent.OnToneChange(value: sound.id))
                        } label: {
                            HStack(spacing: 12) {
                                if !dynamicTypeSize.isAccessibilitySize {
                                    MatAlarmFeatureIcon(symbol: "waveform")
                                }
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(sound.displayName).font(.headline).foregroundStyle(.primary)
                                    Text(NativeStrings.text(NativeSoundPresentation.description(sound.id)))
                                        .font(.footnote).foregroundStyle(.secondary)
                                }
                                Spacer(minLength: 0)
                            }
                            .frame(minHeight: 44)
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                        .accessibilityIdentifier("tone-\(sound.id)")
                        .accessibilityAddTraits(selectedTone == sound.id ? [.isSelected] : [])
                        Button { selection.togglePreview(sound.id) } label: {
                            Image(systemName: selection.previewingTone == sound.id ? "stop.fill" : "play.fill")
                                .frame(minWidth: 44, minHeight: 44)
                        }
                        .buttonStyle(.glass)
                        .buttonBorderShape(.circle)
                        .accessibilityLabel(Text(NativeStrings.text(selection.previewingTone == sound.id ? "Stop preview" : "Preview")) + Text(verbatim: " ") + Text(sound.displayName))
                        .accessibilityHint(selection.previewingTone == sound.id
                            ? Text("Stop preview") : Text("Preview this tone without changing your selection."))
                        .accessibilityIdentifier("preview-\(sound.id)")
                    }
                    .padding(.vertical, 6)
                    .listRowBackground(selectedTone == sound.id ? MatAlarmPalette.wash : MatAlarmPalette.surface)
                }
            }
            if let message = selection.previewMessage {
                Section { Text(NativeStrings.text(message)).foregroundStyle(.secondary) }
                    .accessibilityIdentifier("sound-preview-message")
            }
        }
        .listStyle(.insetGrouped)
        .matAlarmContent()
        .navigationTitle("Sound library")
        .disabled(model.state.isSaving)
        .onAppear {
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

}
