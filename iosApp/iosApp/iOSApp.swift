import SwiftUI
import app
import UserNotifications
import AVFoundation
import AlarmKit

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    @Environment(\.scenePhase) private var scenePhase
    
    init() {
        print("iOSApp.init: Initializing Koin early (before UI)")
        MainViewControllerKt.doInitKoin()
        
        // Prewarm database in background so it's ready when UI needs it
        MainViewControllerKt.prewarmDatabaseInBackground()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea()
                .onAppear {
                    // Request notification permissions after UI is shown
                    // This is deferred to not block startup
                    MainViewControllerKt.requestNotificationPermissionsDeferred()
                }
        }
        .onChange(of: scenePhase) { newPhase in
            if newPhase == .active {
                // Check for pending AlarmKit deeplinks when app becomes active
                AppDelegate.checkPendingAlarmKitDeeplink()
            }
        }
    }
}

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    
    /// The AlarmKit wrapper instance
    private let alarmKitWrapper = AlarmKitWrapperImpl.shared
    
    /// Check for pending AlarmKit deeplinks and process them
    /// Called when app becomes active (from scenePhase change)
    static func checkPendingAlarmKitDeeplink() {
        // First check for pending deeplink from StopAlarmIntent
        if let pendingJson = PendingDeeplinkStore.shared.consumePendingDeeplink() {
            print("AppDelegate: Found pending AlarmKit deeplink, setting it now")
            print("AppDelegate: JSON = \(pendingJson)")
            
            // Set the deeplink in Kotlin holder
            NotificationDeeplinkHolder.shared.setAlarmDeeplink(json: pendingJson)
            return
        }
        
        // If no pending deeplink, check if there's an alerting alarm
        // (User may have tapped the alert itself, not the stop button)
        if #available(iOS 26, *) {
            checkAlertingAlarms()
        }
    }
    
    /// Check for alerting alarms and navigate to MathScreen if found
    @available(iOS 26, *)
    private static func checkAlertingAlarms() {
        do {
            let manager = AlarmManager.shared
            let alarms = try manager.alarms
            
            // Find any alerting alarm
            for alarm in alarms where alarm.state == .alerting {
                print("AppDelegate: Found alerting alarm: \(alarm.id)")
                
                // Get alarm data from our store
                if let alarmData = AlarmDataStore.shared.retrieve(alarmUUID: alarm.id.uuidString) {
                    let deeplinkJson = createDeeplinkJsonFromData(alarmData)
                    
                    print("AppDelegate: Setting deeplink for alerting alarm")
                    NotificationDeeplinkHolder.shared.setAlarmDeeplink(json: deeplinkJson)
                    
                    // Stop the alarm since user is now in app
                    try manager.stop(id: alarm.id)
                    AlarmDataStore.shared.remove(alarmUUID: alarm.id.uuidString)
                    
                    return  // Handle one alerting alarm at a time
                }
            }
        } catch {
            print("AppDelegate: Error checking alerting alarms: \(error)")
        }
    }
    
    /// Create deeplink JSON from alarm data
    @available(iOS 26, *)
    private static func createDeeplinkJsonFromData(_ data: MathAlarmData) -> String {
        let vibrateStr = data.vibrate ? "true" : "false"
        return "{\"alarmId\":\(data.alarmId),\"hour\":\(data.hour),\"minute\":\(data.minute),\"repeat\":false,\"repeatDays\":\"FFFFFFF\",\"isOn\":true,\"difficulty\":\(data.difficulty),\"alarmTone\":\"\(data.alarmTone)\",\"vibrate\":\(vibrateStr),\"snooze\":\(data.snooze),\"title\":\"\(data.title)\",\"isSaved\":true}"
    }
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        // Register AlarmKit wrapper with Kotlin bridge FIRST
        // This allows Kotlin to use AlarmKit when available (iOS 26+)
        registerAlarmKitBridge()
        
        // Set notification delegate EARLY - before Compose UI loads
        UNUserNotificationCenter.current().delegate = self
        
        // Configure audio session for alarm playback
        configureAudioSession()
        
        // Check if app was launched from a notification
        if let notificationResponse = launchOptions?[.remoteNotification] as? [String: Any] {
            handleAlarmNotification(userInfo: notificationResponse)
        }
        
        // Log AlarmKit availability and request authorization
        let alarmKitAvailable = alarmKitWrapper.isAlarmKitAvailable()
        print("iOSApp: AlarmKit available = \(alarmKitAvailable)")
        
        if alarmKitAvailable {
            // Explicitly request AlarmKit authorization
            alarmKitWrapper.requestAuthorization()
            
            // Debug: Check current auth status and list alarms
            let authStatus = alarmKitWrapper.checkAuthorizationStatus()
            print("iOSApp: AlarmKit authorization status = \(authStatus)")
            
            // List any existing alarms
            alarmKitWrapper.debugListAlarms()
        }
        
        // Check for pending AlarmKit deeplink (in case app was launched from AlarmKit)
        // This runs after a short delay to ensure Kotlin/Compose is ready
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            AppDelegate.checkPendingAlarmKitDeeplink()
        }
        
        return true
    }
    
    /// Register the Swift AlarmKit wrapper with Kotlin's AlarmSchedulerBridge
    private func registerAlarmKitBridge() {
        print("iOSApp: Registering AlarmKit bridge...")
        
        // Create a Kotlin-compatible wrapper that bridges to our Swift implementation
        // This uses the current Objective-C interop
        // When Swift Export is stable, this can be simplified
        
        let kotlinBridge = AlarmKitKotlinBridge(wrapper: alarmKitWrapper)
        AlarmSchedulerBridge.shared.registerScheduler(scheduler: kotlinBridge)
        
        print("iOSApp: AlarmKit bridge registered")
    }
    
    // Configure audio session to play sound even in silent mode
    private func configureAudioSession() {
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .default, options: [])
            try session.setActive(true)
            print("iOSApp: Audio session configured")
        } catch {
            print("iOSApp: Failed to configure audio session: \(error)")
        }
    }
    
    // Called when user taps on a notification
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                didReceive response: UNNotificationResponse,
                                withCompletionHandler completionHandler: @escaping () -> Void) {
        let userInfo = response.notification.request.content.userInfo
        let actionIdentifier = response.actionIdentifier
        
        print("iOSApp: Notification tapped with action: \(actionIdentifier)")
        
        // Handle different actions
        switch actionIdentifier {
        case "SNOOZE_ACTION":
            // Snooze the alarm
            let snoozeMinutes = (userInfo["snooze"] as? NSNumber)?.intValue ?? 5
            AlarmAudioController.shared.snoozeAlarm(minutes: Int32(snoozeMinutes))
            completionHandler()
            return
            
        case "DISMISS_ACTION":
            // Stop the alarm completely
            AlarmAudioController.shared.stopAlarm()
            completionHandler()
            return
            
        default:
            // Default tap or "Solve Math" action - navigate to math screen
            break
        }
        
        handleAlarmNotification(userInfo: userInfo)
        completionHandler()
    }
    
    // Handle alarm notification - start audio and set deeplink
    private func handleAlarmNotification(userInfo: [AnyHashable: Any]) {
        print("iOSApp: 🔔 handleAlarmNotification called")
        print("iOSApp: userInfo keys = \(userInfo.keys)")
        
        // Extract alarm data
        let alarmId = (userInfo["alarmId"] as? NSNumber)?.int64Value ?? 0
        let hour = (userInfo["hour"] as? NSNumber)?.intValue ?? 0
        let minute = (userInfo["minute"] as? NSNumber)?.intValue ?? 0
        let difficulty = (userInfo["difficulty"] as? NSNumber)?.intValue ?? 0
        let snooze = (userInfo["snooze"] as? NSNumber)?.intValue ?? 5
        let vibrate = (userInfo["vibrate"] as? NSNumber)?.boolValue ?? false
        let title = (userInfo["title"] as? String) ?? ""
        let alarmTone = (userInfo["alarmTone"] as? String) ?? ""
        
        print("iOSApp: Extracted - alarmId=\(alarmId), tone='\(alarmTone)', vibrate=\(vibrate)")
        
        // Start alarm audio immediately when notification is tapped
        // Run on main thread to ensure audio session works correctly
        print("iOSApp: 🔊 Starting AlarmAudioController...")
        DispatchQueue.main.async {
            AlarmAudioController.shared.startAlarm(soundName: alarmTone, vibrate: vibrate)
        }
        
        // Create JSON string for the alarm - matching AlarmEntity format
        let vibrateStr = vibrate ? "true" : "false"
        let alarmJson = "{\"alarmId\":\(alarmId),\"hour\":\(hour),\"minute\":\(minute),\"repeat\":false,\"repeatDays\":\"FFFFFFF\",\"isOn\":true,\"difficulty\":\(difficulty),\"alarmTone\":\"\(alarmTone)\",\"vibrate\":\(vibrateStr),\"snooze\":\(snooze),\"title\":\"\(title)\",\"isSaved\":true}"
        
        print("iOSApp: Setting deeplink with JSON = \(alarmJson)")
        
        // Set deeplink in Kotlin holder to navigate to MathScreen
        NotificationDeeplinkHolder.shared.setAlarmDeeplink(json: alarmJson)
    }
    
    // Called when notification arrives while app is in foreground
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        let userInfo = notification.request.content.userInfo
        
        print("iOSApp: 🔔 Notification arrived in foreground!")
        print("iOSApp: userInfo = \(userInfo)")
        
        // Start alarm audio immediately when notification arrives in foreground
        let alarmTone = (userInfo["alarmTone"] as? String) ?? ""
        let vibrate = (userInfo["vibrate"] as? NSNumber)?.boolValue ?? false
        
        print("iOSApp: Starting AlarmAudioController - tone='\(alarmTone)', vibrate=\(vibrate)")
        
        // Run on main thread to ensure audio session works
        DispatchQueue.main.async {
            AlarmAudioController.shared.startAlarm(soundName: alarmTone, vibrate: vibrate)
        }
        
        // Also set the deeplink so the UI navigates to MathScreen
        handleAlarmNotification(userInfo: userInfo)
        
        // Show banner, badge, AND sound (sound as backup in case AlarmAudioController fails)
        // Our AlarmAudioController provides the full looping alarm, but notification sound
        // gives us at least something if that fails
        completionHandler([.banner, .badge, .sound])
    }
}
