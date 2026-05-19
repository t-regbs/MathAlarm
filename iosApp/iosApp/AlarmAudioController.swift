import Foundation
import AVFoundation
import AudioToolbox

/// Controller for playing alarm sounds and haptic feedback
/// Used when notifications arrive (both foreground and when user taps notification)
class AlarmAudioController: NSObject, AVAudioPlayerDelegate {
    
    static let shared = AlarmAudioController()
    
    private var audioPlayer: AVAudioPlayer?
    private var vibrationTimer: Timer?
    private var soundTimer: Timer?
    private var isPlaying = false
    
    /// Default alarm sound name (bundled with app)
    static let defaultSoundName = "alarm_classic"
    
    // System sound IDs that are known to work
    // See: https://github.com/TUNER88/iOSSystemSoundsLibrary
    private let alarmSoundID: SystemSoundID = 1005  // SMS Alert - loud and noticeable
    private let alternateSoundID: SystemSoundID = 1007  // SMS Tri-tone
    
    private override init() {
        super.init()
        print("AlarmAudioController: Initialized")
    }
    
    // MARK: - Audio Session Configuration
    
    private func configureAudioSession() {
        do {
            let session = AVAudioSession.sharedInstance()
            // Use playback category to play even when device is silent/locked
            // Don't mix with others - alarm should be prominent
            try session.setCategory(.playback, mode: .default, options: [])
            try session.setActive(true, options: .notifyOthersOnDeactivation)
            print("AlarmAudioController: Audio session configured for playback")
        } catch {
            print("AlarmAudioController: Failed to configure audio session: \(error)")
        }
    }
    
    // MARK: - Public Interface
    
    /// Start playing alarm sound
    /// - Parameters:
    ///   - soundName: Name of the sound file (without extension), empty for default
    ///   - vibrate: Whether to enable vibration
    func startAlarm(soundName: String, vibrate: Bool) {
        // Allow restarting if called again
        if isPlaying {
            print("AlarmAudioController: Already playing, restarting...")
            stopAlarm()
        }
        
        print("AlarmAudioController: 🔔 Starting alarm - sound='\(soundName)', vibrate=\(vibrate)")
        isPlaying = true
        
        // Configure audio session for playback
        configureAudioSession()
        
        // Try to play custom/bundled sound first
        if !playBundledSound(named: soundName) {
            // Fall back to system sounds
            print("AlarmAudioController: No bundled sound, using system alert sounds")
            startSystemSoundLoop()
        }
        
        // Start vibration if enabled
        if vibrate {
            startVibration()
        }
    }
    
    /// Stop the alarm sound and vibration
    func stopAlarm() {
        print("AlarmAudioController: 🔕 Stopping alarm")
        isPlaying = false
        
        audioPlayer?.stop()
        audioPlayer = nil
        
        soundTimer?.invalidate()
        soundTimer = nil
        
        stopVibration()
        
        // Deactivate audio session
        do {
            try AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        } catch {
            print("AlarmAudioController: Failed to deactivate audio session: \(error)")
        }
    }
    
    /// Snooze the alarm - stops current sound, will be rescheduled
    func snoozeAlarm(minutes: Int32) {
        print("AlarmAudioController: Snoozing alarm for \(minutes) minutes")
        stopAlarm()
    }
    
    // MARK: - Bundled Sound Playback
    
    private func playBundledSound(named soundName: String) -> Bool {
        let effectiveName = soundName.isEmpty ? AlarmAudioController.defaultSoundName : soundName
        
        // Try to find the sound file in bundle with various extensions
        let extensions = ["caf", "aiff", "wav", "mp3", "m4a", "aac"]
        var soundURL: URL? = nil
        
        for ext in extensions {
            if let url = Bundle.main.url(forResource: effectiveName, withExtension: ext) {
                soundURL = url
                print("AlarmAudioController: Found bundled sound: \(effectiveName).\(ext)")
                break
            }
        }
        
        // Also try without extension (file might have extension in name)
        if soundURL == nil, let url = Bundle.main.url(forResource: effectiveName, withExtension: nil) {
            soundURL = url
            print("AlarmAudioController: Found bundled sound (no ext): \(effectiveName)")
        }
        
        guard let url = soundURL else {
            print("AlarmAudioController: No bundled sound found for '\(effectiveName)'")
            return false
        }
        
        do {
            audioPlayer = try AVAudioPlayer(contentsOf: url)
            audioPlayer?.delegate = self
            audioPlayer?.numberOfLoops = -1 // Loop indefinitely
            audioPlayer?.volume = 1.0
            audioPlayer?.prepareToPlay()
            
            if audioPlayer?.play() == true {
                print("AlarmAudioController: ▶️ Playing bundled sound: \(url.lastPathComponent)")
                return true
            } else {
                print("AlarmAudioController: Failed to start playback")
                return false
            }
        } catch {
            print("AlarmAudioController: Failed to play bundled sound: \(error)")
            return false
        }
    }
    
    // MARK: - System Sound Playback (Fallback)
    
    private func startSystemSoundLoop() {
        print("AlarmAudioController: Starting system sound loop")
        playSystemSound()
        
        // Repeat every 1.5 seconds for alarm effect
        soundTimer = Timer.scheduledTimer(withTimeInterval: 1.5, repeats: true) { [weak self] _ in
            guard let self = self, self.isPlaying else { return }
            self.playSystemSound()
        }
    }
    
    private func playSystemSound() {
        guard isPlaying else { return }
        
        // Play alert sound (respects ringer volume, plays through speaker)
        AudioServicesPlayAlertSound(alarmSoundID)
        print("AlarmAudioController: 🔊 Playing system sound \(alarmSoundID)")
    }
    
    // MARK: - Vibration
    
    private func startVibration() {
        print("AlarmAudioController: Starting vibration")
        
        // Vibrate immediately
        AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
        
        // Set up repeating vibration
        vibrationTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            guard self?.isPlaying == true else { return }
            AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
        }
    }
    
    private func stopVibration() {
        vibrationTimer?.invalidate()
        vibrationTimer = nil
    }
    
    // MARK: - AVAudioPlayerDelegate
    
    func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        print("AlarmAudioController: Audio player finished (success: \(flag))")
    }
    
    func audioPlayerDecodeErrorDidOccur(_ player: AVAudioPlayer, error: Error?) {
        print("AlarmAudioController: Audio decode error: \(error?.localizedDescription ?? "unknown")")
        // Fall back to system sounds
        if isPlaying {
            startSystemSoundLoop()
        }
    }
}
