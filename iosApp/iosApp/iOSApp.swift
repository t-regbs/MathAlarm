import SwiftUI
import app
import UserNotifications
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
        }
        .onChange(of: scenePhase) { newPhase in
            if newPhase == .active {
                // Check for pending AlarmKit deeplinks when app becomes active
                AppDelegate.checkPendingAlarmKitDeeplink()
                MainViewControllerKt.resumeAlarmSchedules()
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
        // Keep the oldest handoff until its challenge has initialized.
        if let pendingJson = PendingDeeplinkStore.shared.peekPendingDeeplink() {
            print("AppDelegate: Found pending AlarmKit deeplink, setting it now")
            
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
                    let deeplinkJson = try createDeeplinkJson(from: alarmData)
                    PendingDeeplinkStore.shared.setPendingDeeplink(deeplinkJson)
                    
                    // Stop the alarm since user is now in app
                    try manager.stop(id: alarm.id)
                    NotificationDeeplinkHolder.shared.setAlarmDeeplink(json: deeplinkJson)
                    
                    return  // Handle one alerting alarm at a time
                }
            }
        } catch {
            print("AppDelegate: Error checking alerting alarms: \(error)")
        }
    }
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        // Register AlarmKit wrapper with Kotlin bridge FIRST
        // This allows Kotlin to use AlarmKit when available (iOS 26+)
        registerAlarmKitBridge()
        
        // Set notification delegate EARLY - before Compose UI loads
        UNUserNotificationCenter.current().delegate = self
        
        // Check if app was launched from a notification
        if let notificationResponse = launchOptions?[.remoteNotification] as? [String: Any] {
            handleAlarmNotification(userInfo: notificationResponse)
        }
        
        // Log AlarmKit availability; ask for authorization when saving an alarm.
        let alarmKitAvailable = alarmKitWrapper.isAlarmKitAvailable()
        print("iOSApp: AlarmKit available = \(alarmKitAvailable)")
        
        if alarmKitAvailable {
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
            let alarmId = (userInfo["alarmId"] as? NSNumber)?.int64Value ?? 0
            IosNotificationSnooze.shared.snooze(alarmId: alarmId) { failure in
                if failure == nil {
                    AlarmAudioController.shared.stopAlarm()
                } else {
                    self.handleAlarmNotification(userInfo: userInfo)
                }
                completionHandler()
            }
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
        
        guard alarmId > 0 else {
            print("iOSApp: Ignoring notification without a valid alarm ID")
            return
        }
        let alarmData = MathAlarmData(
            alarmId: alarmId,
            difficulty: Int32(difficulty),
            hour: Int32(hour),
            minute: Int32(minute),
            snooze: Int32(snooze),
            vibrate: vibrate,
            alarmTone: alarmTone,
            title: title
        )
        do {
            let alarmJson = try createDeeplinkJson(from: alarmData)
            PendingDeeplinkStore.shared.setPendingDeeplink(alarmJson)
            DispatchQueue.main.async {
                AlarmAudioController.shared.startAlarm(soundName: alarmTone, vibrate: vibrate)
            }
            print("iOSApp: Setting deeplink for alarm \(alarmId)")
            AppDelegate.checkPendingAlarmKitDeeplink()
        } catch {
            print("iOSApp: Unable to encode alarm deeplink: \(error)")
        }
    }
    
    // Called when notification arrives while app is in foreground
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        let userInfo = notification.request.content.userInfo
        
        print("iOSApp: 🔔 Notification arrived in foreground!")
        print("iOSApp: userInfo = \(userInfo)")
        
        // Also set the deeplink so the UI navigates to MathScreen
        handleAlarmNotification(userInfo: userInfo)
        
        // Show banner, badge, AND sound (sound as backup in case AlarmAudioController fails)
        // Our AlarmAudioController provides the full looping alarm, but notification sound
        // gives us at least something if that fails
        completionHandler([.banner, .badge, .sound])
    }
}
