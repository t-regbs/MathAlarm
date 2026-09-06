# iOS Fixes Implementation Plan

## Fix #1: `areNotificationsEnabled()` race condition
**File:** `app/src/iosMain/kotlin/.../platform/PlatformApis.ios.kt:76-82`

Replace the function with a cached approach. The async completion handler updates a cache, and the synchronous function returns the cached value.

### Changes to `PlatformApis.ios.kt`:
Replace lines 76-82:
```kotlin
actual fun areNotificationsEnabled(): Boolean {
    // Return the cached value and trigger an async refresh for next call.
    // getNotificationSettingsWithCompletionHandler is async -- the completion runs after
    // this function returns, so we can't use it inline. Instead we keep a cached value
    // that's primed at startup via initNotificationStatusCache().
    val cachedResult = IosNotificationStatusCache.isEnabled

    // Trigger async update so the next call gets the latest value
    UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
        IosNotificationStatusCache.isEnabled =
            settings?.authorizationStatus == UNAuthorizationStatusAuthorized
    }

    return cachedResult
}

/**
 * Cache for notification authorization status.
 * Updated asynchronously; initial value is false until first async check completes.
 * Call [initNotificationStatusCache] early in app startup to prime this value.
 */
private object IosNotificationStatusCache {
    @Volatile
    var isEnabled: Boolean = false
}

/**
 * Prime the notification status cache at app startup.
 * Call this early (e.g., from doInitKoin()) so the cached value is accurate
 * by the time the UI first checks it.
 */
fun initNotificationStatusCache() {
    UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
        IosNotificationStatusCache.isEnabled =
            settings?.authorizationStatus == UNAuthorizationStatusAuthorized
    }
}
```

### Changes to `MainViewController.kt`:
In `doInitKoin()`, add after `initKoin()`:
```kotlin
fun doInitKoin() {
    initKoin()
    initNotificationStatusCache()  // Prime the notification status cache early
}
```
Add import: `import com.timilehinaregbesola.mathalarm.platform.initNotificationStatusCache`

---

## Fix #2: `scheduleWithAlarmKit` async result tracking
**File:** `iosApp/iosApp/AlarmKitWrapper.swift:464-644`

Add error tracking and document the limitation.

### Changes to `AlarmKitWrapper.swift`:
1. Add a property to track scheduling errors:
```swift
/// Last scheduling error (for diagnostics)
private(set) var lastSchedulingError: Error? = nil
```

2. In the `scheduleWithAlarmKit` method, at the start of `Task { @MainActor in`:
```swift
self.lastSchedulingError = nil
```

3. In each catch block, set: `self.lastSchedulingError = error`

4. Add a comment above the `return true` on line 643:
```swift
// Note: Returns true to indicate scheduling was initiated (async).
// The actual AlarmKit schedule() call runs in a Task and may fail.
// Check lastSchedulingError after a delay if you need to verify success.
// This is a limitation of the synchronous Kotlin<->Swift bridge protocol.
return true
```

---

## Fix #3: Widget Extension for AlarmKit countdown/snooze
**New files needed in Xcode**

### Step 1: Create widget source file
Create `iosApp/MathAlarmWidget/MathAlarmWidget.swift`:
```swift
import WidgetKit
import SwiftUI
import AlarmKit

@available(iOS 26, *)
struct MathAlarmWidget: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: AlarmAttributes<MathAlarmData>.self) { context in
            // Lock screen Live Activity view
            let state = context.state
            let attributes = context.attributes
            
            VStack(spacing: 8) {
                HStack {
                    Image(systemName: "alarm.fill")
                        .foregroundStyle(attributes.tintColor ?? .blue)
                    Text(attributes.presentation.alert.title)
                        .font(.headline)
                    Spacer()
                }
                
                switch state.mode {
                case .countdown(let countdown):
                    ProgressView(
                        timerInterval: countdown.startDate...countdown.startDate.addingTimeInterval(
                            countdown.totalCountdownDuration - countdown.previouslyElapsedDuration
                        ),
                        countsDown: true
                    ) {
                        Text("Alarm in")
                    }
                    .tint(attributes.tintColor ?? .blue)
                    
                case .paused(let paused):
                    let remaining = paused.totalCountdownDuration - paused.previouslyElapsedDuration
                    ProgressView(value: remaining, total: paused.totalCountdownDuration) {
                        Text("Paused")
                    }
                    .tint(.orange)
                    
                case .alert:
                    Text("Wake Up!")
                        .font(.title2)
                        .foregroundStyle(.red)
                    
                @unknown default:
                    EmptyView()
                }
            }
            .padding()
            
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Image(systemName: "alarm.fill")
                        .foregroundStyle(context.attributes.tintColor ?? .blue)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    switch context.state.mode {
                    case .countdown(let countdown):
                        Text(countdown.fireDate, style: .timer)
                    default:
                        Text("Math Alarm")
                    }
                }
                DynamicIslandExpandedRegion(.bottom) {
                    Text(context.attributes.presentation.alert.title)
                }
            } compactLeading: {
                Image(systemName: "alarm.fill")
                    .foregroundStyle(context.attributes.tintColor ?? .blue)
            } compactTrailing: {
                switch context.state.mode {
                case .countdown(let countdown):
                    Text(countdown.fireDate, style: .timer)
                default:
                    EmptyView()
                }
            } minimal: {
                Image(systemName: "alarm.fill")
                    .foregroundStyle(context.attributes.tintColor ?? .blue)
            }
        }
    }
}

@available(iOS 26, *)
struct MathAlarmWidgetBundle: WidgetBundle {
    var body: some Widget {
        MathAlarmWidget()
    }
}
```

### Step 2: Xcode manual setup (instructions for user)
1. In Xcode, File > New > Target > Widget Extension
2. Name it "MathAlarmWidget"
3. Set deployment target to iOS 26.0
4. Add `MathAlarmData` struct to the widget target's membership (or move it to a shared framework)
5. Enable App Groups capability on both the main app target and widget target
6. Add `import AlarmKit` to the widget

---

## Fix #4: Remove duplicate notification delegates
**File:** `app/src/iosMain/kotlin/.../notification/IosNotificationHandler.kt`

### Changes:
Remove the `IosNotificationDelegate` class (lines 92-167) and the `setupNotificationDelegate()` function (lines 173-177). Keep `NotificationDeeplinkHolder` and `AlarmAudioController` objects.

The resulting file should contain:
- `NotificationDeeplinkHolder` object (lines 20-43) - KEEP
- `AlarmAudioController` object (lines 49-82) - KEEP (will be renamed in Fix #5)
- Private action constants (lines 84-87) - REMOVE (only used by deleted class)
- `IosNotificationDelegate` class (lines 92-167) - REMOVE
- `setupNotificationDelegate()` function (lines 173-177) - REMOVE

---

## Fix #5: Consolidate audio controllers
**Files:** `IosNotificationHandler.kt`, `PlatformApis.ios.kt`

### Changes to `IosNotificationHandler.kt`:
Rename the Kotlin `AlarmAudioController` object to `AlarmAudioBridge` to avoid naming collision with the Swift `AlarmAudioController`:

```kotlin
/**
 * Bridge to manage alarm audio from Swift.
 * Delegates to the Swift AlarmAudioController for actual playback.
 * Named "Bridge" to avoid collision with the Swift AlarmAudioController class.
 */
object AlarmAudioBridge {
    val shared: AlarmAudioBridge get() = this

    fun startAlarm(soundName: String, vibrate: Boolean) {
        println("AlarmAudioBridge: Starting alarm - sound=$soundName, vibrate=$vibrate")
        IosAlarmAudioManager.startAlarm(soundName, vibrate, 1.0f)
    }

    fun stopAlarm() {
        println("AlarmAudioBridge: Stopping alarm")
        IosAlarmAudioManager.stopAlarm()
    }

    fun isPlaying(): Boolean = IosAlarmAudioManager.isPlaying()

    fun snoozeAlarm(minutes: Int) {
        println("AlarmAudioBridge: Snoozing alarm for $minutes minutes")
        IosAlarmAudioManager.snooze(minutes)
    }
}
```

### Changes to `iOSApp.swift`:
Update Swift references from `AlarmAudioController` (Kotlin) to use the Swift one directly. The Swift AppDelegate already uses the Swift `AlarmAudioController.shared` so no changes needed there.

### Changes to `PlatformApis.ios.kt`:
`stopPlatformAlarmAudio()` already calls `IosAlarmAudioManager.stopAlarm()` which is correct.

---

## Fix #6: PlatformVibrator continuous vibration
**File:** `app/src/iosMain/kotlin/.../platform/PlatformApis.ios.kt:22-39`

### Changes:
Add imports at top:
```kotlin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
```

Replace the `PlatformVibrator` class:
```kotlin
@OptIn(ExperimentalForeignApi::class)
actual class PlatformVibrator actual constructor() {
    private val feedbackGenerator = UIImpactFeedbackGenerator(
        style = UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy
    )
    private var isVibrating = false
    private var vibrationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    actual fun startWaveform(pattern: LongArray, repeat: Int) {
        if (isVibrating) return
        isVibrating = true
        feedbackGenerator.prepare()

        vibrationJob = scope.launch {
            while (isVibrating) {
                AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
                feedbackGenerator.impactOccurred()
                delay(800) // Vibrate every 800ms for alarm-like pattern
            }
        }
    }

    actual fun cancel() {
        isVibrating = false
        vibrationJob?.cancel()
        vibrationJob = null
    }
}
```

---

## Fix #7: Persist `scheduledAlarms` to UserDefaults
**File:** `iosApp/iosApp/AlarmKitWrapper.swift`

### Changes:
Replace the `scheduledAlarms` property and add persistence methods:

```swift
/// Store alarm IDs for cancellation - persisted to UserDefaults
private var scheduledAlarms: [Int64: UUID] = [:] {
    didSet {
        saveScheduledAlarms()
    }
}

private let scheduledAlarmsKey = "MathAlarm.scheduledAlarmMapping"

private func saveScheduledAlarms() {
    // Convert [Int64: UUID] to [String: String] for serialization
    let stringDict = scheduledAlarms.reduce(into: [String: String]()) { result, pair in
        result[String(pair.key)] = pair.value.uuidString
    }
    UserDefaults.standard.set(stringDict, forKey: scheduledAlarmsKey)
}

private func loadScheduledAlarms() {
    guard let stringDict = UserDefaults.standard.dictionary(forKey: scheduledAlarmsKey) as? [String: String] else {
        return
    }
    scheduledAlarms = stringDict.reduce(into: [Int64: UUID]()) { result, pair in
        if let key = Int64(pair.key), let uuid = UUID(uuidString: pair.value) {
            result[key] = uuid
        }
    }
    print("AlarmKitWrapper: Loaded \(scheduledAlarms.count) alarm mappings from UserDefaults")
}
```

In `init()`, add `loadScheduledAlarms()` before the `Task`:
```swift
private override init() {
    super.init()
    loadScheduledAlarms()
    if #available(iOS 26, *) {
        Task {
            await self.requestAuthorizationIfNeeded()
            self.observeAlarmUpdates()
        }
    }
}
```

---

## Fix #8: AlarmKit `stop()` for one-time alarms
**File:** `iosApp/iosApp/AlarmKitWrapper.swift` (StopAlarmIntent)

### Changes to `StopAlarmIntent.perform()`:
After `try manager.stop(id: uuid)`, add:
```swift
// AlarmKit bug: stop() on one-time alarms sets state back to .scheduled
// instead of deleting. Use cancel() for one-time alarms to fully remove.
if let alarmData = AlarmDataStore.shared.retrieve(alarmUUID: alarmUUID) {
    // Check if this was NOT a repeating alarm
    let isRepeating = alarmData.repeatDays?.contains("T") ?? false  // Needs repeatDays in MathAlarmData
    if !isRepeating {
        try? manager.cancel(id: uuid)
        print("StopAlarmIntent: Cancelled one-time alarm to fully remove")
    }
}
```

Also add `repeatDays` to `MathAlarmData`:
```swift
var repeatDays: String = "FFFFFFF"
```

And set it when creating metadata in `scheduleWithAlarmKit`:
```swift
let metadata = MathAlarmData(
    alarmId: alarmId,
    difficulty: difficulty,
    hour: hour,
    minute: minute,
    snooze: snoozeMinutes,
    vibrate: vibrate,
    alarmTone: soundName,
    title: alertTitle,
    repeatDays: repeatDays  // Add this
)
```

---

## Fix #9: Document `.named("")` sound workaround
**File:** `iosApp/iosApp/AlarmKitWrapper.swift`

### Changes:
The existing comments at lines 561-564 already document this. Enhance them:
```swift
// IMPORTANT: AlarmKit sound behavior (iOS 26.0, may change in future releases):
// - .default does NOT play any sound (known AlarmKit bug as of iOS 26 beta)
// - .named("") plays the default system alarm sound (workaround)
// - .named("customSound") plays a custom sound from the app bundle
// See: https://levelup.gitconnected.com/swiftui-alarm-app-copycat-with-alarmkit-wwdc-2025-part-2-5c3cb2194c54
// TODO: Revisit when Apple fixes .default sound in a future iOS 26 update
```

---

## Fix #10: RingtonePickerLauncher - implement bundled sound list
**File:** `app/src/iosMain/kotlin/.../platform/PlatformApis.ios.kt:114-119`

### Changes:
```kotlin
actual class RingtonePickerLauncher(private val onResult: (String?) -> Unit) {
    actual fun launch(currentTone: String?) {
        // Present a UIAlertController with available bundled sounds
        val sounds = IosAlarmAudioManager.availableSounds
        val alertController = platform.UIKit.UIAlertController.alertControllerWithTitle(
            title = "Select Alarm Sound",
            message = null,
            preferredStyle = platform.UIKit.UIAlertControllerStyleActionSheet
        )

        // Default option
        alertController.addAction(
            platform.UIKit.UIAlertAction.actionWithTitle(
                title = "Default",
                style = platform.UIKit.UIAlertActionStyleDefault
            ) { _ -> onResult("") }
        )

        // Bundled sounds
        for (sound in sounds) {
            alertController.addAction(
                platform.UIKit.UIAlertAction.actionWithTitle(
                    title = sound.displayName,
                    style = platform.UIKit.UIAlertActionStyleDefault
                ) { _ -> onResult(sound.filename) }
            )
        }

        // Cancel
        alertController.addAction(
            platform.UIKit.UIAlertAction.actionWithTitle(
                title = "Cancel",
                style = platform.UIKit.UIAlertActionStyleCancel
            ) { _ -> }
        )

        platform.UIKit.UIApplication.sharedApplication.keyWindow
            ?.rootViewController
            ?.presentViewController(alertController, animated = true, completion = null)
    }
}

@Composable
actual fun rememberRingtonePickerLauncher(onResult: (String?) -> Unit): RingtonePickerLauncher {
    return remember { RingtonePickerLauncher(onResult) }
}
```

---

## Fix #11: App Group for shared UserDefaults
**Files:** `iosApp/iosApp/AlarmKitWrapper.swift`

### Changes:
1. Add a constant at the top of `AlarmKitWrapper.swift`:
```swift
/// App Group identifier for sharing data between main app and widget extension
let kMathAlarmAppGroupId = "group.com.timilehinaregbesola.mathalarm"
```

2. In `AlarmDataStore`, replace `UserDefaults.standard` with:
```swift
private let userDefaults = UserDefaults(suiteName: kMathAlarmAppGroupId) ?? UserDefaults.standard
```

3. In `PendingDeeplinkStore`, replace `UserDefaults.standard` with:
```swift
private let userDefaults = UserDefaults(suiteName: kMathAlarmAppGroupId) ?? UserDefaults.standard
```

4. In the new `saveScheduledAlarms()`/`loadScheduledAlarms()` (Fix #7), use the same App Group UserDefaults.

### Xcode manual steps:
1. Select the main app target > Signing & Capabilities > + Capability > App Groups
2. Add `group.com.timilehinaregbesola.mathalarm`
3. Do the same for the widget extension target (from Fix #3)

---

## Fix #12: Guard AlarmKit import
**File:** `iosApp/iosApp/iOSApp.swift`

### Changes:
Replace line 5:
```swift
#if canImport(AlarmKit)
import AlarmKit
#endif
```

And wrap AlarmKit-specific code in `checkAlertingAlarms()` and related methods with `#if canImport(AlarmKit)` guards. The `@available(iOS 26, *)` annotations already provide runtime guards, but `#if canImport` ensures compile-time safety when building with older SDKs.

---

## Fix #13: Remove `iosX64` from `:core` module
**File:** `core/build.gradle.kts:36-40`

### Changes:
Remove lines 36-40:
```kotlin
iosX64 {
    binaries.framework {
        baseName = xcfName
    }
}
```

This aligns `:core` with `:app` which only declares `iosArm64` and `iosSimulatorArm64`.
