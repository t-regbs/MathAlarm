import CryptoKit
import Foundation
import SwiftUI
import app  // Kotlin framework (current interop)
import AlarmKit
import AppIntents

@available(iOS 26, *)
private extension Color {
    static let mathAlarmGreen = Color(red: 0.11, green: 0.98, blue: 0.37)
}

// MARK: - Alarm Metadata

/// Simple metadata for MathAlarm alarms
/// Conforms to AlarmMetadata (for AlarmKit) and Codable (for persistence)
@available(iOS 26, *)
struct MathAlarmData: AlarmMetadata, Codable {
    var alarmId: Int64
    var difficulty: Int32
    var hour: Int32 = 0
    var minute: Int32 = 0
    var snooze: Int32 = 5
    var vibrate: Bool = false
    var alarmTone: String = ""
    var title: String = ""
    var createdAt: Date = Date()
}

// MARK: - Alarm Data Store (for intents to access)

/// Stores alarm data so App Intents can access it when the alarm fires
/// Uses UserDefaults for persistence in case app is killed
@available(iOS 26, *)
class AlarmDataStore {
    static let shared = AlarmDataStore()
    
    private let userDefaults = UserDefaults.standard
    private let storageKey = "MathAlarm.alarmDataStore"
    
    private var alarmData: [String: MathAlarmData] = [:] {
        didSet {
            saveToUserDefaults()
        }
    }
    
    private init() {
        loadFromUserDefaults()
    }
    
    func store(alarmUUID: UUID, data: MathAlarmData) {
        alarmData[alarmUUID.uuidString] = data
        print("AlarmDataStore: Stored data for alarm \(alarmUUID)")
    }
    
    func retrieve(alarmUUID: String) -> MathAlarmData? {
        // Reload in case another process updated it
        loadFromUserDefaults()
        return alarmData[alarmUUID]
    }
    
    func remove(alarmUUID: String) {
        alarmData.removeValue(forKey: alarmUUID)
    }
    
    private func saveToUserDefaults() {
        do {
            let data = try JSONEncoder().encode(alarmData)
            userDefaults.set(data, forKey: storageKey)
            print("AlarmDataStore: Saved \(alarmData.count) alarms to UserDefaults")
        } catch {
            print("AlarmDataStore: Failed to save: \(error)")
        }
    }
    
    private func loadFromUserDefaults() {
        guard let data = userDefaults.data(forKey: storageKey) else { return }
        do {
            alarmData = try JSONDecoder().decode([String: MathAlarmData].self, from: data)
            print("AlarmDataStore: Loaded \(alarmData.count) alarms from UserDefaults")
        } catch {
            print("AlarmDataStore: Failed to load: \(error)")
        }
    }
}

// MARK: - Pending Deeplink Storage

/// Stores pending deeplink for AlarmKit alarms
/// This is checked when the app becomes active since intents run AFTER app opens
class PendingDeeplinkStore {
    static let shared = PendingDeeplinkStore()
    
    private let userDefaults = UserDefaults.standard
    private let pendingDeeplinkKey = "MathAlarm.pendingAlarmKitDeeplink"
    
    /// Store a pending deeplink (called from intent BEFORE app opens)
    func setPendingDeeplink(_ json: String) {
        userDefaults.set(json, forKey: pendingDeeplinkKey)
        userDefaults.synchronize()  // Force immediate write
        print("PendingDeeplinkStore: Stored pending deeplink")
    }
    
    /// Get and clear the pending deeplink (called when app activates)
    func consumePendingDeeplink() -> String? {
        guard let json = userDefaults.string(forKey: pendingDeeplinkKey) else {
            return nil
        }
        userDefaults.removeObject(forKey: pendingDeeplinkKey)
        userDefaults.synchronize()
        print("PendingDeeplinkStore: Consumed pending deeplink")
        return json
    }
    
    /// Check if there's a pending deeplink without consuming it
    func hasPendingDeeplink() -> Bool {
        return userDefaults.string(forKey: pendingDeeplinkKey) != nil
    }
}

// MARK: - App Intents for AlarmKit

/// Intent to stop/dismiss an alarm and open the Math Screen
@available(iOS 26, *)
struct StopAlarmIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Stop Alarm"
    static var description: IntentDescription? = IntentDescription("Stops the alarm and opens the math puzzle")
    
    // This makes the intent open the app when performed
    static var openAppWhenRun: Bool { true }
    
    @Parameter(title: "Alarm ID")
    var alarmUUID: String
    
    init() {
        self.alarmUUID = ""
    }
    
    init(alarmUUID: UUID) {
        self.alarmUUID = alarmUUID.uuidString
    }
    
    func perform() async throws -> some IntentResult {
        print("StopAlarmIntent: Performing for alarm \(alarmUUID)")
        
        guard let uuid = UUID(uuidString: alarmUUID) else {
            print("StopAlarmIntent: Invalid UUID")
            throw AlarmIntentError.invalidAlarmId
        }
        
        // IMPORTANT: Store the deeplink FIRST, before stopping the alarm
        // This ensures the deeplink is available when the app activates
        if let alarmData = AlarmDataStore.shared.retrieve(alarmUUID: alarmUUID) {
            let deeplinkJson = createDeeplinkJson(from: alarmData)
            PendingDeeplinkStore.shared.setPendingDeeplink(deeplinkJson)
            AlarmDataStore.shared.remove(alarmUUID: alarmUUID)
            print("StopAlarmIntent: Stored pending deeplink for MathScreen")
        } else {
            print("StopAlarmIntent: No alarm data found for UUID \(alarmUUID)")
        }
        
        // Stop the alarm in AlarmKit
        let manager = AlarmManager.shared
        try manager.stop(id: uuid)
        print("StopAlarmIntent: Alarm stopped")
        
        return .result()
    }
}

/// Create deeplink JSON from alarm data
@available(iOS 26, *)
private func createDeeplinkJson(from data: MathAlarmData) -> String {
    let payload: [String: Any] = [
        "alarmId": data.alarmId,
        "hour": data.hour,
        "minute": data.minute,
        "repeat": false,
        "repeatDays": "FFFFFFF",
        "isOn": true,
        "difficulty": data.difficulty,
        "alarmTone": data.alarmTone,
        "vibrate": data.vibrate,
        "snooze": data.snooze,
        "title": data.title,
        "isSaved": true
    ]

    do {
        let jsonData = try JSONSerialization.data(withJSONObject: payload)
        return String(data: jsonData, encoding: .utf8) ?? "{}"
    } catch {
        print("StopAlarmIntent: Failed to encode deeplink JSON: \(error)")
        return "{}"
    }
}

/// Intent to snooze an alarm
@available(iOS 26, *)
struct SnoozeAlarmIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Snooze Alarm"
    static var description: IntentDescription? = IntentDescription("Snoozes the alarm for a few minutes")
    
    @Parameter(title: "Alarm ID")
    var alarmUUID: String
    
    init() {
        self.alarmUUID = ""
    }
    
    init(alarmUUID: UUID) {
        self.alarmUUID = alarmUUID.uuidString
    }
    
    func perform() async throws -> some IntentResult {
        print("SnoozeAlarmIntent: Performing for alarm \(alarmUUID)")
        
        guard let uuid = UUID(uuidString: alarmUUID) else {
            print("SnoozeAlarmIntent: Invalid UUID")
            throw AlarmIntentError.invalidAlarmId
        }
        
        // Countdown (snooze) the alarm - this starts the postAlert countdown
        let manager = AlarmManager.shared
        try manager.countdown(id: uuid)
        print("SnoozeAlarmIntent: Alarm snoozed (countdown started)")
        
        return .result()
    }
}

enum AlarmIntentError: Error, LocalizedError {
    case invalidAlarmId
    case alarmNotFound
    
    var errorDescription: String? {
        switch self {
        case .invalidAlarmId: return "Invalid alarm ID"
        case .alarmNotFound: return "Alarm not found"
        }
    }
}

// MARK: - AlarmButton Extensions

@available(iOS 26, *)
extension AlarmButton {
    static let stopButton = AlarmButton(
        text: LocalizedStringResource("Stop"),
        textColor: .red,
        systemImageName: "stop.fill"
    )
    
    static let repeatButton = AlarmButton(
        text: LocalizedStringResource("Snooze"),
        textColor: .mathAlarmGreen,
        systemImageName: "zzz"
    )
    
    static let pauseButton = AlarmButton(
        text: LocalizedStringResource("Pause"),
        textColor: .orange,
        systemImageName: "pause.fill"
    )
    
    static let resumeButton = AlarmButton(
        text: LocalizedStringResource("Resume"),
        textColor: .green,
        systemImageName: "play.fill"
    )
}

/// Swift implementation of the NativeAlarmScheduler interface from Kotlin
/// This provides AlarmKit functionality on iOS 26+
/// and gracefully falls back on older iOS versions
class AlarmKitWrapperImpl: NSObject {
    
    /// Shared singleton instance
    static let shared = AlarmKitWrapperImpl()
    
    /// Store alarm IDs for cancellation
    
    /// Track authorization status
    private var isAuthorized: Bool = false
    
    private override init() {
        super.init()
        // Request authorization on init
        if #available(iOS 26, *) {
            Task {
                await self.requestAuthorizationIfNeeded()
                self.observeAlarmUpdates()
            }
        }
    }
    
    // MARK: - Authorization
    
    @available(iOS 26, *)
    private func requestAuthorizationIfNeeded() async {
        let manager = AlarmManager.shared
        
        switch manager.authorizationState {
        case .notDetermined:
            print("AlarmKitWrapper: Authorization not determined, requesting...")
            do {
                let state = try await manager.requestAuthorization()
                isAuthorized = (state == .authorized)
                print("AlarmKitWrapper: Authorization result: \(state), isAuthorized: \(isAuthorized)")
            } catch {
                print("AlarmKitWrapper: Authorization request failed: \(error)")
                isAuthorized = false
            }
        case .authorized:
            print("AlarmKitWrapper: Already authorized")
            isAuthorized = true
        case .denied:
            print("AlarmKitWrapper: Authorization denied")
            isAuthorized = false
        @unknown default:
            print("AlarmKitWrapper: Unknown authorization state")
            isAuthorized = false
        }
    }
    
    @available(iOS 26, *)
    private func ensureAuthorized() async throws {
        let manager = AlarmManager.shared
        
        switch manager.authorizationState {
        case .notDetermined:
            let state = try await manager.requestAuthorization()
            if state != .authorized {
                throw AlarmKitError.notAuthorized
            }
            isAuthorized = true
        case .denied:
            throw AlarmKitError.notAuthorized
        case .authorized:
            isAuthorized = true
        @unknown default:
            throw AlarmKitError.unknownAuthState
        }
    }

    @available(iOS 26, *)
    private func hasAlarmKitAuthorization() -> Bool {
        let manager = AlarmManager.shared

        switch manager.authorizationState {
        case .authorized:
            isAuthorized = true
            return true
        case .notDetermined:
            print("AlarmKitWrapper: Authorization not determined; using notification fallback")
            isAuthorized = false
            return false
        case .denied:
            print("AlarmKitWrapper: Authorization denied; using notification fallback")
            isAuthorized = false
            return false
        @unknown default:
            print("AlarmKitWrapper: Unknown authorization state; using notification fallback")
            isAuthorized = false
            return false
        }
    }
    
    // MARK: - Alarm Observation
    
    @available(iOS 26, *)
    private func observeAlarmUpdates() {
        Task {
            let manager = AlarmManager.shared
            for await alarms in manager.alarmUpdates {
                print("AlarmKitWrapper: Alarm updates received - \(alarms.count) alarms")
                for alarm in alarms {
                    print("  - Alarm \(alarm.id): state=\(alarm.state), schedule=\(String(describing: alarm.schedule))")
                }
            }
        }
    }
    
    enum AlarmKitError: Error {
        case notAuthorized
        case unknownAuthState
        case schedulingFailed(String)
    }
    
    // MARK: - NativeAlarmScheduler Protocol Implementation
    
    /// Check if AlarmKit is available on this device
    func isAlarmKitAvailable() -> Bool {
        if #available(iOS 26, *) {
            print("AlarmKitWrapper: AlarmKit IS available (iOS 26+)")
            return true
        }
        print("AlarmKitWrapper: AlarmKit NOT available (iOS < 26)")
        return false
    }

    /// Check whether AlarmKit still has a future occurrence pending for this app alarm id.
    func hasPendingOccurrence(alarmId: Int64) -> Bool {
        guard #available(iOS 26, *) else {
            return false
        }

        do {
            let manager = AlarmManager.shared
            let alarms = try manager.alarms
            return alarms.contains { alarm in
                guard let metadata = AlarmDataStore.shared.retrieve(alarmUUID: alarm.id.uuidString) else {
                    return false
                }
                return metadata.alarmId == alarmId
            }
        } catch {
            print("AlarmKitWrapper: Failed to check pending occurrence for alarm \(alarmId): \(error)")
            return false
        }
    }
    
    /// Request alarm authorization - call this early in app lifecycle
    func requestAuthorization() {
        guard #available(iOS 26, *) else { return }
        Task {
            await requestAuthorizationIfNeeded()
        }
    }
    
    /// Check current authorization status
    func checkAuthorizationStatus() -> String {
        guard #available(iOS 26, *) else { return "unavailable" }
        let manager = AlarmManager.shared
        switch manager.authorizationState {
        case .notDetermined: return "notDetermined"
        case .authorized: return "authorized"
        case .denied: return "denied"
        @unknown default: return "unknown"
        }
    }
    
    /// Debug: List all currently scheduled alarms
    func debugListAlarms() {
        guard #available(iOS 26, *) else {
            print("AlarmKitWrapper: AlarmKit not available")
            return
        }
        do {
            let manager = AlarmManager.shared
            let alarms = try manager.alarms
            print("AlarmKitWrapper: === DEBUG: Current Alarms ===")
            print("  Authorization: \(manager.authorizationState)")
            print("  Total alarms: \(alarms.count)")
            for alarm in alarms {
                print("  - ID: \(alarm.id)")
                print("    State: \(alarm.state)")
                print("    Schedule: \(String(describing: alarm.schedule))")
                print("    Countdown: \(String(describing: alarm.countdownDuration))")
            }
            print("AlarmKitWrapper: === END DEBUG ===")
        } catch {
            print("AlarmKitWrapper: Failed to list alarms: \(error)")
        }
    }
    
    /// Schedule an alarm using AlarmKit
    /// - Returns: true if successfully scheduled, false otherwise
    func scheduleAlarm(request: AlarmScheduleRequest, completion: AlarmScheduleCompletion) {
        guard #available(iOS 26, *) else {
            completion.complete(success: false, error: "AlarmKit is unavailable")
            return
        }
        scheduleWithAlarmKit(request: request, completion: completion)
    }

    private func occurrenceUUID(alarmId: Int64, key: String) -> UUID {
        let bytes = Array(SHA256.hash(data: Data("mathalarm/\(alarmId)/\(key)".utf8)))
        return UUID(uuid: (bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7],
                           bytes[8], bytes[9], bytes[10], bytes[11], bytes[12], bytes[13], bytes[14], bytes[15]))
    }

    func cancelOccurrence(alarmId: Int64, occurrenceKey: String) {
        guard #available(iOS 26, *) else { return }
        let id = occurrenceUUID(alarmId: alarmId, key: occurrenceKey)
        do {
            let manager = AlarmManager.shared
            if try manager.alarms.contains(where: { $0.id == id }) { try manager.cancel(id: id) }
        } catch { print("Failed to cancel occurrence: \(error)") }
    }

    /// Cancel an alarm scheduled with AlarmKit
    func cancelAlarm(alarmId: Int64) {
        guard #available(iOS 26, *) else {
            print("AlarmKitWrapper: Cannot cancel - AlarmKit not available")
            return
        }
        cancelAlarmKitAlarm(alarmId: alarmId)
    }
    
    /// Cancel all AlarmKit alarms
    func cancelAllAlarms() {
        guard #available(iOS 26, *) else {
            print("AlarmKitWrapper: Cannot cancel all - AlarmKit not available")
            return
        }
        cancelAllAlarmKitAlarms()
    }
    
    /// Snooze an active alarm
    func snoozeAlarm(alarmId: Int64, minutes: Int32) {
        guard #available(iOS 26, *) else {
            print("AlarmKitWrapper: Cannot snooze - AlarmKit not available")
            return
        }
        snoozeAlarmKitAlarm(alarmId: alarmId, minutes: minutes)
    }
    
    // MARK: - AlarmKit Implementation (iOS 26+)
    
    // Typealias for AlarmConfiguration with our metadata type
    @available(iOS 26, *)
    typealias MathAlarmConfiguration = AlarmManager.AlarmConfiguration<MathAlarmData>
    
    @available(iOS 26, *)
    private func scheduleWithAlarmKit(request: AlarmScheduleRequest, completion: AlarmScheduleCompletion) {
        let alarmId = request.alarmId
        let hour = request.hour
        let minute = request.minute
        let title = request.title
        let soundName = request.soundName
        let repeatDays = request.repeatDays
        let snoozeMinutes = request.snoozeMinutes
        let vibrate = request.vibrate
        let difficulty = request.difficulty
        let repeats = request.repeats
        // Parse repeat days to weekdays (only used if repeats == true)
        let weekdays = parseRepeatDays(repeatDays)
        
        // Schedule alarm asynchronously
        Task { @MainActor in
            do {
                let manager = AlarmManager.shared
                
                guard self.hasAlarmKitAuthorization() else {
                    completion.complete(success: false, error: "Allow Math Alarm to schedule alarms in Settings")
                    return
                }
                print("AlarmKitWrapper: Authorization confirmed")
                
                let alarmUUID = self.occurrenceUUID(alarmId: alarmId, key: request.occurrenceKey)
                
                // Create the time for the schedule
                let time = Alarm.Schedule.Relative.Time(hour: Int(hour), minute: Int(minute))
                print("AlarmKitWrapper: Created time - hour: \(hour), minute: \(minute)")
                
                let schedule: Alarm.Schedule
                if repeats && !weekdays.isEmpty {
                    schedule = .relative(.init(time: time, repeats: .weekly(Array(weekdays))))
                } else {
                    schedule = .fixed(Date(timeIntervalSince1970: Double(request.timeInMillis) / 1000.0))
                }

                // Create alarm presentation with "Solve Math" as the stop button
                let alertTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
                let solveButton = AlarmButton(
                    text: LocalizedStringResource("Solve Math"),
                    textColor: .mathAlarmGreen,
                    systemImageName: "function"
                )
                let alertContent = AlarmPresentation.Alert(
                    title: LocalizedStringResource(stringLiteral: alertTitle),
                    stopButton: solveButton,
                    secondaryButton: snoozeMinutes > 0 ? .repeatButton : nil,
                    secondaryButtonBehavior: snoozeMinutes > 0 ? .countdown : nil
                )
                let presentation = AlarmPresentation(alert: alertContent)
                print("AlarmKitWrapper: Created presentation with title: \(alertTitle)")
                
                // Create metadata with all alarm info (needed by intents)
                let metadata = MathAlarmData(
                    alarmId: alarmId,
                    difficulty: difficulty,
                    hour: hour,
                    minute: minute,
                    snooze: snoozeMinutes,
                    vibrate: vibrate,
                    alarmTone: soundName,
                    title: alertTitle
                )
                
                // Store alarm data so intents can access it when alarm fires
                AlarmDataStore.shared.store(alarmUUID: alarmUUID, data: metadata)
                
                let attributes = AlarmAttributes(
                    presentation: presentation,
                    metadata: metadata,
                    tintColor: .mathAlarmGreen
                )
                
                // Create countdown duration for snooze (postAlert is snooze time)
                let countdownDuration: Alarm.CountdownDuration? = snoozeMinutes > 0
                    ? Alarm.CountdownDuration(preAlert: nil, postAlert: TimeInterval(snoozeMinutes) * 60)
                    : nil
                
                // Create intents for alarm buttons
                let stopIntent = StopAlarmIntent(alarmUUID: alarmUUID)
                let snoozeIntent: SnoozeAlarmIntent? = snoozeMinutes > 0 ? SnoozeAlarmIntent(alarmUUID: alarmUUID) : nil
                
                // AlarmKit uses ActivityKit AlertSound. Use the bundled CAF files
                // for system-level alarm playback and keep the source tone id in
                // metadata for in-app looping playback.
                let alertSoundName = self.alertSoundName(for: soundName)
                let configuration: MathAlarmConfiguration
                if countdownDuration == nil {
                    // Traditional alarm without countdown - use .alarm() factory
                    print("AlarmKitWrapper: Creating traditional alarm configuration")
                    print("AlarmKitWrapper: Using custom alarm sound: \(alertSoundName)")
                    configuration = MathAlarmConfiguration.alarm(
                        schedule: schedule,
                        attributes: attributes,
                        stopIntent: stopIntent,
                        secondaryIntent: snoozeIntent,
                        sound: .named(alertSoundName)
                    )
                } else {
                    // Alarm with countdown/snooze - use generic initializer
                    print("AlarmKitWrapper: Creating alarm with countdown configuration")
                    print("AlarmKitWrapper: Using custom alarm sound: \(alertSoundName)")
                    configuration = MathAlarmConfiguration(
                        countdownDuration: countdownDuration,
                        schedule: schedule,
                        attributes: attributes,
                        stopIntent: stopIntent,
                        secondaryIntent: snoozeIntent,
                        sound: .named(alertSoundName)
                    )
                }
                print("AlarmKitWrapper: Created configuration with intents, about to schedule...")
                
                // Schedule the alarm with id and configuration
                let alarm = try await manager.schedule(id: alarmUUID, configuration: configuration)
                
                completion.complete(success: true, error: nil)
                print("AlarmKitWrapper: ✅ Alarm scheduled successfully!")
                print("  - AlarmKit ID: \(alarm.id)")
                print("  - App Alarm ID: \(alarmId)")
                print("  - State: \(alarm.state)")
                print("  - Schedule: \(String(describing: alarm.schedule))")
                
                // Verify by listing all alarms

                
            } catch AlarmKitError.notAuthorized {
                completion.complete(success: false, error: "Not authorized for alarms")
            } catch AlarmKitError.unknownAuthState {
                completion.complete(success: false, error: "Unknown alarm authorization state")
            } catch {
                completion.complete(success: false, error: error.localizedDescription)
                print("AlarmKitWrapper: Error details - \(String(describing: error))")
            }
        }
        
    }

    private func alertSoundName(for soundName: String) -> String {
        let fallbackName = "alarm_classic"
        let trimmedName = soundName.trimmingCharacters(in: .whitespacesAndNewlines)
        let baseName = trimmedName.isEmpty
            ? fallbackName
            : (trimmedName as NSString).deletingPathExtension
        let cafName = "\(baseName).caf"

        if Bundle.main.url(forResource: baseName, withExtension: "caf") != nil {
            return cafName
        }

        print("AlarmKitWrapper: Missing bundled CAF for \(baseName), falling back to \(fallbackName).caf")
        return "\(fallbackName).caf"
    }

    @available(iOS 26, *)
    private func cancelAlarmKitAlarm(alarmId: Int64) {
        do {
            let manager = AlarmManager.shared
            // Query OS alarms and persisted metadata so cancellation also works after relaunch.
            let keys = (0...6).map { "day_\($0)" } + ["snooze"]
            let ids = Set(keys.map { occurrenceUUID(alarmId: alarmId, key: $0) })
            for alarm in try manager.alarms {
                if ids.contains(alarm.id) || AlarmDataStore.shared.retrieve(alarmUUID: alarm.id.uuidString)?.alarmId == alarmId {
                    try manager.cancel(id: alarm.id)
                }
            }
        } catch { print("Failed to cancel alarm: \(error)") }
    }

    @available(iOS 26, *)
    private func cancelAllAlarmKitAlarms() {
        do {
            let manager = AlarmManager.shared
            // Get current alarms synchronously
            let alarms = try manager.alarms
            for alarm in alarms {
                try manager.cancel(id: alarm.id)
            }
            
            print("AlarmKitWrapper: Cancelled all alarms")
        } catch {
            print("AlarmKitWrapper: Failed to cancel all alarms - \(error.localizedDescription)")
        }
    }
    
    @available(iOS 26, *)
    private func snoozeAlarmKitAlarm(alarmId: Int64, minutes: Int32) {
        // AlarmKit handles snooze through the system alarm UI automatically
        // The secondaryButtonBehavior: .countdown enables the Repeat/Snooze button
        // which uses the postAlert duration from CountdownDuration
        print("AlarmKitWrapper: Snooze requested for alarm \(alarmId) - \(minutes) minutes")
        print("AlarmKitWrapper: Note - AlarmKit snooze is handled by system alarm UI")
    }
    
    /// Convert repeat days string to array of Locale.Weekday
    /// Input: "TFFFTFF" where T=true, F=false, index 0=Sunday
    /// Output: Set<Locale.Weekday>
    @available(iOS 26, *)
    private func parseRepeatDays(_ repeatDays: String) -> Set<Locale.Weekday> {
        var days: Set<Locale.Weekday> = []
        let mapping: [Locale.Weekday] = [.sunday, .monday, .tuesday, .wednesday, .thursday, .friday, .saturday]
        
        for (index, char) in repeatDays.enumerated() {
            if char == "T" && index < mapping.count {
                days.insert(mapping[index])
            }
        }
        
        return days
    }
}

// MARK: - Kotlin Bridge Adapter

/// Bridges the Swift AlarmKitWrapperImpl to Kotlin's NativeAlarmScheduler interface
/// This class implements the Kotlin protocol and delegates to the Swift wrapper
class AlarmKitKotlinBridge: NSObject, NativeAlarmScheduler {
    
    private let wrapper: AlarmKitWrapperImpl
    
    init(wrapper: AlarmKitWrapperImpl) {
        self.wrapper = wrapper
        super.init()
    }
    
    // MARK: - NativeAlarmScheduler Protocol
    
    func isAlarmKitAvailable() -> Bool {
        return wrapper.isAlarmKitAvailable()
    }

    func hasPendingOccurrence(alarmId: Int64) -> Bool {
        return wrapper.hasPendingOccurrence(alarmId: alarmId)
    }
    
    func scheduleAlarm(request: AlarmScheduleRequest, completion: AlarmScheduleCompletion) {
        wrapper.scheduleAlarm(request: request, completion: completion)
    }

    func cancelOccurrence(alarmId: Int64, occurrenceKey: String) {
        wrapper.cancelOccurrence(alarmId: alarmId, occurrenceKey: occurrenceKey)
    }

    func cancelAlarm(alarmId: Int64) {
        wrapper.cancelAlarm(alarmId: alarmId)
    }
    
    func cancelAllAlarms() {
        wrapper.cancelAllAlarms()
    }
    
    func snoozeAlarm(alarmId: Int64, minutes: Int32) {
        wrapper.snoozeAlarm(alarmId: alarmId, minutes: minutes)
    }
}
