import SwiftUI
import app
import AlarmKit

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    @Environment(\.scenePhase) private var scenePhase
    
    init() {
        let bridge = AlarmKitKotlinBridge(wrapper: AlarmKitWrapperImpl.shared)
        IosApplication.shared.initialize(scheduler: bridge)
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView().ignoresSafeArea()
        }
        .onChange(of: scenePhase) { newPhase in
            #if DEBUG
            if SharedBridgeVerification.enabled { return }
            #endif
            if newPhase == .active {
                // Check for pending AlarmKit deeplinks when app becomes active
                AppDelegate.checkPendingAlarmKitDeeplink()
                IosApplication.shared.resumeAlarmSchedules()
            }
        }
    }
}

class AppDelegate: NSObject, UIApplicationDelegate {
    
    /// The AlarmKit wrapper instance
    private let alarmKitWrapper = AlarmKitWrapperImpl.shared
    
    private static var restorationStarted = false
    private static var restorationFinished = false

    /// Check for pending AlarmKit deeplinks and process them
    /// Called when app becomes active (from scenePhase change)
    static func checkPendingAlarmKitDeeplink() {
        #if DEBUG
        if SharedBridgeVerification.enabled { return }
        #endif
        if !restorationFinished {
            guard !restorationStarted else { return }
            restorationStarted = true
            IosApplication.shared.restoreUnresolvedHandoffs { payloads, succeeded in
                guard succeeded.boolValue else {
                    // Leave all deliveries unacknowledged; the next activation retries storage.
                    restorationStarted = false
                    return
                }
                PendingDeeplinkStore.shared.restoreUnresolvedHandoffs(payloads)
                restorationFinished = true
                checkPendingAlarmKitDeeplink()
            }
            return
        }
        // Keep the oldest handoff until its challenge has initialized.
        if let pendingJson = PendingDeeplinkStore.shared.peekPendingDeeplink() {
            print("AppDelegate: Found pending AlarmKit deeplink, setting it now")
            
            // Set the deeplink in Kotlin holder
            IosApplication.shared.deliverPendingHandoff(payload: pendingJson)
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
                        let delivery = identifiedDelivery(from: alarmData, registrationID: alarm.id.uuidString)
                        let deeplinkJson = createDeeplinkJson(from: delivery)
                        PendingDeeplinkStore.shared.setPendingDeeplink(deeplinkJson)
                    
                        // Stop the alarm since user is now in app
                        try manager.stop(id: alarm.id)
                        // A recovery reuses its native ID; stop the delivered alert before
                        // replacing it, otherwise Stop would silence the new registration.
                        try await AlarmKitWrapperImpl.shared.armRecovery(for: delivery)
                        IosApplication.shared.deliverPendingHandoff(payload: deeplinkJson)
                    
                        return  // Handle one alerting alarm at a time
                    }
                }
            } catch {
                print("AppDelegate: Error checking alerting alarms: \(error)")
            }
        }
    }
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        #if DEBUG
        if SharedBridgeVerification.enabled { return true }
        #endif
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
        // Shared services were initialized before constructing any view.
        DispatchQueue.main.async {
            AppDelegate.checkPendingAlarmKitDeeplink()
        }
        
        return true
    }
    
}
