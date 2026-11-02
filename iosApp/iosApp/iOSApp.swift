import SwiftUI
import app
import AlarmKit

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    @Environment(\.scenePhase) private var scenePhase
    
    init() {
        #if DEBUG
        NativeSettingsVerification.prepareIfRequested()
        #endif
        let bridge = AlarmKitKotlinBridge(wrapper: AlarmKitWrapperImpl.shared)
        IosApplication.shared.initialize(scheduler: bridge)
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
        .onChange(of: scenePhase) { newPhase in
            #if DEBUG
            if ProcessInfo.processInfo.environment["MATHALARM_HOSTED_TESTS"] == "1" || SharedBridgeVerification.enabled || NativePresentationVerification.enabled || NativeAlarmPermissionGuideVerification.enabled { return }
            #endif
            if newPhase == .active {
                // Check for pending AlarmKit deeplinks when app becomes active
                AppDelegate.checkPendingAlarmKitDeeplink(restoreUnresolved: true)
            }
        }
    }
}

/// Native lifecycle status only. Kotlin remains authoritative for occurrence state.
@MainActor
final class NativeDeliveryRestoration: ObservableObject {
    static let shared = NativeDeliveryRestoration()
    @Published private(set) var ready = false
    @Published private(set) var loading = false
    @Published private(set) var failure: AlarmErrorMessage?

    fileprivate func begin() {
        ready = false
        loading = true
        failure = nil
    }
    fileprivate func finish(error: AlarmErrorMessage? = nil) {
        failure = error
        loading = false
        ready = error == nil
        NotificationCenter.default.post(name: .mathAlarmPendingDelivery, object: nil)
    }
}

@MainActor
class AppDelegate: NSObject, UIApplicationDelegate {
    
    /// The AlarmKit wrapper instance
    private let alarmKitWrapper = AlarmKitWrapperImpl.shared
    
    private static var restorationStarted = false
    static var pendingDeliveryRestorationFinished: Bool { NativeDeliveryRestoration.shared.ready }

    /// Check for pending AlarmKit deeplinks and process them
    /// Called when app becomes active (from scenePhase change)
    static func checkPendingAlarmKitDeeplink(restoreUnresolved: Bool = false) {
        #if DEBUG
        if ProcessInfo.processInfo.environment["MATHALARM_HOSTED_TESTS"] == "1" || SharedBridgeVerification.enabled || NativePresentationVerification.enabled || NativeAlarmPermissionGuideVerification.enabled { return }
        #endif
        if restoreUnresolved || !pendingDeliveryRestorationFinished {
            guard !restorationStarted else { return }
            restorationStarted = true
            NativeDeliveryRestoration.shared.begin()
            IosApplication.shared.restoreUnresolvedHandoffs { payloads, succeeded in
                // The approved Kotlin facade invokes this callback with
                // withContext(Dispatchers.Main). Assert that contract at the bridge.
                MainActor.assumeIsolated {
                    guard succeeded.boolValue else {
                        // Preserve queue and drafts. The native failure remains visible
                        // until explicit retry or a later lifecycle refresh succeeds.
                        restorationStarted = false
                        NativeDeliveryRestoration.shared.finish(error: .initialization)
                        return
                    }
                    guard PendingDeeplinkStore.shared.restoreUnresolvedHandoffs(payloads) else {
                        restorationStarted = false
                        NativeDeliveryRestoration.shared.finish(error: .update)
                        return
                    }
                    Task { @MainActor in
                        await AlarmKitWrapperImpl.shared.restoreRecoveryForUnresolved(payloads: payloads)
                        await AlarmKitWrapperImpl.shared.collectAlertingDeliveries()
                        restorationStarted = false
                        NativeDeliveryRestoration.shared.finish()
                        checkPendingAlarmKitDeeplink()
                        IosApplication.shared.resumeAlarmSchedules()
                    }
                }
            }
            return
        }
        guard !restorationStarted else { return }
        Task { @MainActor in await AlarmKitWrapperImpl.shared.collectAlertingDeliveries() }
        // Keep the oldest handoff until its challenge has initialized.
        if let pendingJson = PendingDeeplinkStore.shared.peekPendingDeeplink() {
            print("AppDelegate: Found pending AlarmKit deeplink, setting it now")
            
            // Set the deeplink in Kotlin holder
            IosApplication.shared.deliverPendingHandoff(payload: pendingJson)
            NotificationCenter.default.post(name: .mathAlarmPendingDelivery, object: nil)
            return
        }
        
        // If no pending deeplink, check if there's an alerting alarm
        // (User may have tapped the alert itself, not the stop button)
        // collectAlertingDeliveries above publishes any newly retained head.
    }
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        #if DEBUG
        if ProcessInfo.processInfo.environment["MATHALARM_HOSTED_TESTS"] == "1" || SharedBridgeVerification.enabled || NativePresentationVerification.enabled || NativeAlarmPermissionGuideVerification.enabled { return true }
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
            AppDelegate.checkPendingAlarmKitDeeplink(restoreUnresolved: true)
        }
        
        return true
    }
    
}
