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
    var recoverySession: UUID? = nil
    var recoveryAttempt: Int? = nil
    // Optional fields preserve decoding of existing persisted registration metadata.
    var occurrenceKey: String? = nil
    var scheduledAtMilliseconds: Int64? = nil
    var deliveryId: String? = nil
    var activeAtMilliseconds: Int64? = nil
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

// MARK: - App Intents for AlarmKit

/// Intent to stop/dismiss an alarm and open the Math Screen
@available(iOS 26, *)
struct StopAlarmIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Stop Alarm"
    static var description: IntentDescription? = IntentDescription("Stops the alarm and opens the math puzzle")
    
    // Arm recovery before foreground opening can block on device authentication.
    static var supportedModes: IntentModes { .background }
    static var authenticationPolicy: IntentAuthenticationPolicy { .alwaysAllowed }
    
    @Parameter(title: "Alarm ID")
    var alarmUUID: String
    
    init() {
        self.alarmUUID = ""
    }
    
    init(alarmUUID: UUID) {
        self.alarmUUID = alarmUUID.uuidString
    }
    
    @MainActor
    func perform() async throws -> some IntentResult & OpensIntent {
        print("StopAlarmIntent: Performing for alarm \(alarmUUID)")
        
        guard UUID(uuidString: alarmUUID) != nil else {
            print("StopAlarmIntent: Invalid UUID")
            throw AlarmIntentError.invalidAlarmId
        }
        
        // Persist navigation before requesting foreground opening.
        if let alarmData = AlarmDataStore.shared.retrieve(alarmUUID: alarmUUID) {
            if let sessionId = alarmData.recoverySession,
               !AlarmRecoveryStore.shared.accepts(alarmId: alarmData.alarmId,
                                                  sessionId: sessionId, attempt: alarmData.recoveryAttempt) {
                throw AlarmIntentError.invalidAlarmId
            }
            let delivery = identifiedDelivery(from: alarmData, registrationID: alarmUUID)
            let deeplinkJson = createDeeplinkJson(from: delivery)
            PendingDeeplinkStore.shared.setPendingDeeplink(deeplinkJson)
            print("StopAlarmIntent: Stored pending deeplink for MathScreen")
            try await AlarmKitWrapperImpl.shared.armRecovery(for: delivery)
        } else {
            print("StopAlarmIntent: No alarm data found for UUID \(alarmUUID)")
            throw AlarmIntentError.invalidAlarmId
        }
        
        // AlarmKit already performs Stop. Opening is a separate foreground intent.
        return .result(opensIntent: OpenMathChallengeIntent())
    }
}

@available(iOS 26, *)
struct OpenMathChallengeIntent: AppIntent {
    static var title: LocalizedStringResource = "Solve Math"
    static var isDiscoverable: Bool { false }
    static var supportedModes: IntentModes { .foreground }
    @MainActor
    func perform() async throws -> some IntentResult {
        AppDelegate.checkPendingAlarmKitDeeplink()
        return .result()
    }
}

/// AlarmKit metadata stays native; navigation uses the shared identity-only contract.
@available(iOS 26, *)
func createDeeplinkJson(from data: MathAlarmData) -> String {
    precondition(data.deliveryId != nil, "Identify a delivery before publishing its handoff")
    return IosApplication.shared.createAlarmHandoffJson(
        alarmId: data.alarmId, deliveryId: data.deliveryId!,
        activeAt: data.activeAtMilliseconds.map { KotlinLong(value: $0) })
}

/// Recovery copies this token, so re-ringing/replaying restores the same occurrence session.
@available(iOS 26, *)
func identifiedDelivery(from data: MathAlarmData, registrationID: String) -> MathAlarmData {
    var delivery = data
    if delivery.deliveryId == nil {
        let deliveredNow = Date()
        // Older persisted metadata has no occurrence date/key; retain legacy authoritative lookup.
        if data.occurrenceKey != nil || data.scheduledAtMilliseconds != nil {
            delivery.activeAtMilliseconds = NativeAlarmDeliveryIdentity.occurrenceMilliseconds(
                scheduledAtMilliseconds: data.scheduledAtMilliseconds, occurrenceKey: data.occurrenceKey,
                hour: Int(data.hour), minute: Int(data.minute), now: deliveredNow)
        }
        delivery.deliveryId = data.recoverySession?.uuidString ?? NativeAlarmDeliveryIdentity.identifier(
            registrationID: registrationID, scheduledAtMilliseconds: data.scheduledAtMilliseconds,
            occurrenceKey: data.occurrenceKey, hour: Int(data.hour), minute: Int(data.minute), now: deliveredNow
        )
    }
    return delivery
}

enum AlarmIntentError: Error, LocalizedError {
    case invalidAlarmId
    
    var errorDescription: String? {
        switch self {
        case .invalidAlarmId: return "Invalid alarm ID"
        }
    }
}

/// Swift implementation of the NativeAlarmScheduler interface from Kotlin
/// AlarmKit delivery for the iOS/iPadOS 26+ app.
class AlarmKitWrapperImpl: NSObject {
    
    /// Shared singleton instance
    static let shared = AlarmKitWrapperImpl()
    
    private override init() {
        super.init()
        observeAlarmUpdates()
    }

    @available(iOS 26, *)
    private func hasAlarmKitAuthorization() -> Bool {
        AlarmManager.shared.authorizationState == .authorized
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
    
    /// The app's deployment target is iOS/iPadOS 26.
    func isAlarmKitAvailable() -> Bool { true }

    /// Check the exact native registration; a snooze must not mask a missing weekday alarm.
    func hasPendingOccurrence(alarmId: Int64, occurrenceKey: String) -> Bool {

        do {
            let manager = AlarmManager.shared
            let id = occurrenceUUID(alarmId: alarmId, key: occurrenceKey)
            return try manager.alarms.contains { $0.id == id }
        } catch {
            print("AlarmKitWrapper: Failed to check pending occurrence for alarm \(alarmId): \(error)")
            return false
        }
    }
    
    /// Request authorization when someone saves or enables an alarm.
    func requestAuthorization(completion: AlarmAuthorizationCompletion) {
        Task { @MainActor in
            let manager = AlarmManager.shared
            switch manager.authorizationState {
            case .authorized:
                completion.complete(authorized: true)
            case .denied:
                completion.complete(authorized: false)
            case .notDetermined:
                do {
                    let state = try await manager.requestAuthorization()
                    completion.complete(authorized: state == .authorized)
                } catch {
                    print("AlarmKitWrapper: Authorization request failed: \(error)")
                    completion.complete(authorized: false)
                }
            @unknown default:
                completion.complete(authorized: false)
            }
        }
    }
    
    /// Check current authorization status
    func checkAuthorizationStatus() -> String {
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
        scheduleWithAlarmKit(request: request, completion: completion)
    }

    private func occurrenceUUID(alarmId: Int64, key: String) -> UUID {
        let bytes = Array(SHA256.hash(data: Data("mathalarm/\(alarmId)/\(key)".utf8)))
        return UUID(uuid: (bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7],
                           bytes[8], bytes[9], bytes[10], bytes[11], bytes[12], bytes[13], bytes[14], bytes[15]))
    }

    func cancelOccurrence(alarmId: Int64, occurrenceKey: String) -> String? {
        if occurrenceKey == "recovery" { AlarmRecoveryStore.shared.cancel(alarmId: alarmId) }
        let id = occurrenceUUID(alarmId: alarmId, key: occurrenceKey)
        do {
            let manager = AlarmManager.shared
            if try manager.alarms.contains(where: { $0.id == id }) {
                try manager.cancel(id: id)
            }
            AlarmDataStore.shared.remove(alarmUUID: id.uuidString)
            return nil
        } catch {
            print("Failed to cancel occurrence: \(error)")
            return error.localizedDescription
        }
    }

    /// Cancel an alarm scheduled with AlarmKit
    func cancelAlarm(alarmId: Int64) -> String? {
        return cancelAlarmKitAlarm(alarmId: alarmId)
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
                let time = AlarmKit.Alarm.Schedule.Relative.Time(hour: Int(hour), minute: Int(minute))
                print("AlarmKitWrapper: Created time - hour: \(hour), minute: \(minute)")
                
                let schedule: AlarmKit.Alarm.Schedule
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
                    stopButton: solveButton
                )
                let presentation = AlarmPresentation(alert: alertContent)
                print("AlarmKitWrapper: Created presentation with title: \(alertTitle)")
                
                // Create metadata with all alarm info (needed by intents)
                let metadata = MathAlarmData(
                    alarmId: alarmId,
                    difficulty: difficulty,
                    hour: hour,
                    minute: minute,
                    snooze: 0,
                    vibrate: vibrate,
                    alarmTone: soundName,
                    title: alertTitle,
                    occurrenceKey: request.occurrenceKey,
                    scheduledAtMilliseconds: repeats ? nil : request.timeInMillis
                )
                
                // Store alarm data so intents can access it when alarm fires
                AlarmDataStore.shared.store(alarmUUID: alarmUUID, data: metadata)
                
                let attributes = AlarmAttributes(
                    presentation: presentation,
                    metadata: metadata,
                    tintColor: .mathAlarmGreen
                )
                
                // Shared snooze policy schedules a concrete replacement after solving.
                // Native alerts always open the challenge and have no countdown action.
                let configuration = MathAlarmConfiguration.alarm(
                    schedule: schedule,
                    attributes: attributes,
                    stopIntent: StopAlarmIntent(alarmUUID: alarmUUID),
                    sound: .named(self.alertSoundName(for: soundName))
                )
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
        let fallbackName = AlarmSoundCatalog.shared.DEFAULT_SOUND
        let baseName = AlarmSoundCatalog.shared.iosResourceName(tone: soundName)
        let cafName = "\(baseName).caf"

        if Bundle.main.url(forResource: baseName, withExtension: "caf") != nil {
            return cafName
        }

        print("AlarmKitWrapper: Missing bundled CAF for \(baseName), falling back to \(fallbackName).caf")
        return "\(fallbackName).caf"
    }

    @available(iOS 26, *)
    @MainActor
    func armRecovery(for data: MathAlarmData) async throws {
        guard let session = AlarmRecoveryStore.shared.reserve(
            alarmId: data.alarmId, sourceSession: data.recoverySession,
            sourceAttempt: data.recoveryAttempt
        ) else { return }
        let id = occurrenceUUID(alarmId: data.alarmId, key: "recovery")
        var recovery = data
        recovery.recoverySession = session.id
        recovery.recoveryAttempt = session.attempt
        AlarmDataStore.shared.store(alarmUUID: id, data: recovery)
        let solve = AlarmButton(text: "Solve Math", textColor: .mathAlarmGreen, systemImageName: "function")
        let alert = AlarmPresentation.Alert(
            title: LocalizedStringResource(stringLiteral: data.title.isEmpty ? "Solve Math" : data.title),
            stopButton: solve
        )
        let configuration = MathAlarmConfiguration.alarm(
            schedule: .fixed(Date().addingTimeInterval(60)),
            attributes: AlarmAttributes(presentation: AlarmPresentation(alert: alert),
                                        metadata: recovery, tintColor: .mathAlarmGreen),
            stopIntent: StopAlarmIntent(alarmUUID: id),
            sound: .named(alertSoundName(for: data.alarmTone))
        )
        do {
            _ = try await AlarmManager.shared.schedule(id: id, configuration: configuration)
            if !AlarmRecoveryStore.shared.isCurrent(alarmId: data.alarmId, session: session) {
                // Completion can race OS acceptance while schedule is suspended.
                let stored = AlarmDataStore.shared.retrieve(alarmUUID: id.uuidString)
                if stored == nil || (stored?.recoverySession == session.id && stored?.recoveryAttempt == session.attempt) {
                    try AlarmManager.shared.cancel(id: id)
                    AlarmDataStore.shared.remove(alarmUUID: id.uuidString)
                }
            }
            if AlarmRecoveryStore.shared.isCurrent(alarmId: data.alarmId, session: session) {
                IosApplication.shared.clearRecoveryFailure(alarmId: data.alarmId)
            }
            print("Alarm recovery accepted: alarm=\(data.alarmId), attempt=\(session.attempt)")
        } catch {
            if AlarmRecoveryStore.shared.rollbackReservation(
                alarmId: data.alarmId, reservation: session,
                sourceSession: data.recoverySession, sourceAttempt: data.recoveryAttempt) {
                if data.recoverySession != nil {
                    AlarmDataStore.shared.store(alarmUUID: id, data: data)
                } else {
                    AlarmDataStore.shared.remove(alarmUUID: id.uuidString)
                }
                IosApplication.shared.reportRecoveryFailure(alarmId: data.alarmId)
            }
            print("Alarm recovery failed: \(error)")
            throw error
        }
    }

    @available(iOS 26, *)
    private func cancelAlarmKitAlarm(alarmId: Int64) -> String? {
        AlarmRecoveryStore.shared.cancel(alarmId: alarmId)
        do {
            let manager = AlarmManager.shared
            // Query OS alarms and persisted metadata so cancellation also works after relaunch.
            let keys = (0...6).map { "day_\($0)" } + ["snooze", "recovery"]
            let ids = Set(keys.map { occurrenceUUID(alarmId: alarmId, key: $0) })
            var failures: [String] = []
            for alarm in try manager.alarms {
                if ids.contains(alarm.id) || AlarmDataStore.shared.retrieve(alarmUUID: alarm.id.uuidString)?.alarmId == alarmId {
                    do {
                        try manager.cancel(id: alarm.id)
                        AlarmDataStore.shared.remove(alarmUUID: alarm.id.uuidString)
                    } catch {
                        failures.append("\(alarm.id): \(error.localizedDescription)")
                    }
                }
            }
            return failures.isEmpty ? nil : failures.joined(separator: "; ")
        } catch {
            print("Failed to list alarms for cancellation: \(error)")
            return error.localizedDescription
        }
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

    func hasPendingOccurrence(alarmId: Int64, occurrenceKey: String) -> Bool {
        return wrapper.hasPendingOccurrence(alarmId: alarmId, occurrenceKey: occurrenceKey)
    }

    func authorizationStatus() -> String {
        wrapper.checkAuthorizationStatus()
    }

    func requestAuthorization(completion: AlarmAuthorizationCompletion) {
        wrapper.requestAuthorization(completion: completion)
    }
    
    func scheduleAlarm(request: AlarmScheduleRequest, completion: AlarmScheduleCompletion) {
        wrapper.scheduleAlarm(request: request, completion: completion)
    }

    func cancelOccurrence(alarmId: Int64, occurrenceKey: String) -> String? {
        wrapper.cancelOccurrence(alarmId: alarmId, occurrenceKey: occurrenceKey)
    }

    func cancelAlarm(alarmId: Int64) -> String? {
        wrapper.cancelAlarm(alarmId: alarmId)
    }
    
    func acknowledgePendingHandoff(payload: String) {
        if PendingDeeplinkStore.shared.acknowledgePendingDeeplink(payload) {
            DispatchQueue.main.async {
                AppDelegate.checkPendingAlarmKitDeeplink()
            }
        }
    }

    func hasPendingHandoff() -> Bool {
        PendingDeeplinkStore.shared.hasPendingDeeplink()
    }
}
