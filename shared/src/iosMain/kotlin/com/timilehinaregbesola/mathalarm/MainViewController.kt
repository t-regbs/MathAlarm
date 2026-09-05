package com.timilehinaregbesola.mathalarm

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeUIViewController
import cafe.adriel.lyricist.ProvideStrings
import cafe.adriel.lyricist.rememberStrings
import com.timilehinaregbesola.mathalarm.di.initKoin
import com.timilehinaregbesola.mathalarm.di.prewarmDatabase
import com.timilehinaregbesola.mathalarm.navigation.NavGraph
import com.timilehinaregbesola.mathalarm.notification.NotificationDeeplinkHolder
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferencesImpl
import com.timilehinaregbesola.mathalarm.presentation.appsettings.shouldUseDarkColors
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import platform.UIKit.UIViewController

/**
 * Initialize Koin early - called from Swift App init() before UI loads.
 */
fun doInitKoin() {
    initKoin()
}

/**
 * Prewarm the database in background - called after Koin init
 * This initializes Room in background so it's ready when UI needs it
 */
fun prewarmDatabaseInBackground() {
    prewarmDatabase()
}

/** Replace legacy weekday mappings and random native IDs after upgrading. */
fun migrateAlarmSchedules() {
    CoroutineScope(Dispatchers.Main).launch {
        val settings = com.russhwolf.settings.Settings()
        if (settings.getBoolean("alarm_occurrences_v5", false)) return@launch
        try {
            val usecases = (object : KoinComponent {}).getKoin().get<com.timilehinaregbesola.mathalarm.framework.Usecases>()
            usecases.command {
                if (settings.getBoolean("alarm_occurrences_v5", false)) return@command
                com.timilehinaregbesola.mathalarm.alarm.AlarmSchedulerBridge.cancelAllAlarms()
                rescheduleFutureAlarms()
                val alarms = getSavedAlarms().first()
                if (alarms.none { it.isOn && it.scheduleError != null }) {
                    settings.putBoolean("alarm_occurrences_v5", true)
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            co.touchlab.kermit.Logger.e(e) { "Alarm migration failed" }
        }
    }
}

/**
 * Request notification permissions in a non-blocking way.
 * Call this after the UI is shown to avoid blocking startup.
 * 
 * The permission request is deferred to not block app launch,
 * following best practices for permission timing.
 */
fun requestNotificationPermissionsDeferred() {
    CoroutineScope(Dispatchers.Main).launch {
        // Small delay to ensure UI is fully rendered first
        delay(500)
        
        try {
            val koinComponent = object : KoinComponent {}
            val scheduler: com.timilehinaregbesola.mathalarm.notification.IosAlarmScheduler = 
                koinComponent.getKoin().get()
            
            scheduler.requestPermissions { granted ->
                println("requestNotificationPermissionsDeferred: granted = $granted")
            }
        } catch (e: Exception) {
            println("requestNotificationPermissionsDeferred: error = ${e.message}")
        }
    }
}

/**
 * iOS Main View Controller - Entry point for the Compose Multiplatform UI
 * 
 * Note: Koin is initialized earlier via doInitKoin() from Swift's App init().
 * Notification categories are registered via IosAlarmScheduler on first access.
 * Notification delegate is set up in Swift AppDelegate for proper timing.
 */
@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
    InternalCoroutinesApi::class
)
fun MainViewController(): UIViewController {
    println("MainViewController: Creating Compose UI...")
    
    return ComposeUIViewController(configure = { enforceStrictPlistSanityCheck = false }) {
        val preferences = rememberKoinInject<AlarmPreferencesImpl>()
        val lyricist = rememberStrings()
        val isDarkTheme = preferences.shouldUseDarkColors()
        
        // Observe deeplink info from notification taps
        val deeplinkInfo by NotificationDeeplinkHolder.deeplinkInfo.collectAsState()
        
        // Debug log
        println("MainViewController: deeplinkInfo = $deeplinkInfo")
        
        // Clear the deeplink after it's been consumed to prevent re-navigation
        androidx.compose.runtime.LaunchedEffect(deeplinkInfo) {
            if (deeplinkInfo != null) {
                println("MainViewController: LaunchedEffect triggered with deeplinkInfo")
                // Small delay to ensure navigation happens first
                kotlinx.coroutines.delay(500)
                NotificationDeeplinkHolder.clearDeeplink()
            }
        }
        
        ProvideStrings(lyricist) {
            MathAlarmTheme(darkTheme = isDarkTheme) {
                NavGraph(preferences, deeplinkInfo)
            }
        }
    }
}

/**
 * Helper composable to inject Koin dependencies
 */
@androidx.compose.runtime.Composable
inline fun <reified T : Any> rememberKoinInject(): T {
    val koinComponent = object : KoinComponent {}
    return androidx.compose.runtime.remember { koinComponent.getKoin().get<T>() }
}
