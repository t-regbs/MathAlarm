import SwiftUI
import app
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

class AppDelegate: NSObject, UIApplicationDelegate {
    
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
        checkAlertingAlarms()
    }
    
    /// Check for alerting alarms and navigate to MathScreen if found
    @available(iOS 26, *)
    private static func checkAlertingAlarms() {
        Task { @MainActor in
            do {
                let manager = AlarmManager.shared
                let alarms = try manager.alarms
            
                // Find any alerting alarm
                for alarm in alarms where alarm.state == .alerting {
                    print("AppDelegate: Found alerting alarm: \(alarm.id)")
                
                    // Get alarm data from our store
                    if let alarmData = AlarmDataStore.shared.retrieve(alarmUUID: alarm.id.uuidString) {
                        let deeplinkJson = createDeeplinkJson(from: alarmData)
                        PendingDeeplinkStore.shared.setPendingDeeplink(deeplinkJson)
                    
                        // Stop the alarm since user is now in app
                        try manager.stop(id: alarm.id)
                        // A recovery reuses its native ID; stop the delivered alert before
                        // replacing it, otherwise Stop would silence the new registration.
                        try await AlarmKitWrapperImpl.shared.armRecovery(for: alarmData)
                        NotificationDeeplinkHolder.shared.setAlarmDeeplink(json: deeplinkJson)
                    
                        return  // Handle one alerting alarm at a time
                    }
                }
            } catch {
                print("AppDelegate: Error checking alerting alarms: \(error)")
            }
        }
    }
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        // Register AlarmKit wrapper with Kotlin bridge FIRST
        // This allows Kotlin to use AlarmKit when available (iOS 26+)
        registerAlarmKitBridge()
        
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
}
