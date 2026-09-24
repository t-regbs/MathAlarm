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
import com.timilehinaregbesola.mathalarm.provider.skippedTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
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

/** Migrate older schedules once, then reconcile missing alarms on each activation. */
fun resumeAlarmSchedules() {
    CoroutineScope(Dispatchers.Main).launch {
        if (NotificationDeeplinkHolder.deeplinkInfo.value != null ||
            com.timilehinaregbesola.mathalarm.alarm.AlarmSchedulerBridge.hasPendingHandoff()) return@launch
        val settings = com.russhwolf.settings.Settings()
        try {
            val usecases = (object : KoinComponent {}).getKoin().get<com.timilehinaregbesola.mathalarm.framework.Usecases>()
            usecases.command {
                if (NotificationDeeplinkHolder.deeplinkInfo.value != null ||
                    com.timilehinaregbesola.mathalarm.alarm.AlarmSchedulerBridge.hasPendingHandoff()) return@command
                val needsLegacyMigration = !settings.getBoolean("alarm_occurrences_v5", false)
                val needsSkipRemoval = !settings.getBoolean("ios_skip_removed_v1", false)
                if (!needsLegacyMigration && !needsSkipRemoval) {
                    rescheduleFutureAlarms.onAppResume(skipWhenNativeCurrent = true)
                    return@command
                }
                if (needsLegacyMigration) {
                    com.timilehinaregbesola.mathalarm.alarm.AlarmSchedulerBridge.cancelAllAlarms()
                }
                if (needsSkipRemoval) {
                    val now = Clock.System.now().toEpochMilliseconds()
                    getSavedAlarms().first().filter { it.skippedDate != null }.forEach { alarm ->
                        val skippedTime = alarm.skippedTime(TimeZone.currentSystemDefault())
                            ?.takeIf { it > now }
                        val pendingTimes = if (!alarm.repeat && skippedTime != null) {
                            val savedZone = alarm.scheduleTimeZone?.let(TimeZone::of)
                                ?: TimeZone.currentSystemDefault()
                            (alarm.pendingTimes + listOfNotNull(alarm.skippedTime(savedZone)))
                                .distinct().sorted()
                        } else alarm.pendingTimes
                        updateAlarm(alarm.copy(
                            skippedDate = null,
                            pendingTimes = pendingTimes,
                            isOn = alarm.isOn || (!alarm.repeat && skippedTime != null),
                        ))
                    }
                }
                rescheduleFutureAlarms()
                val alarms = getSavedAlarms().first()
                if (alarms.none { it.isOn && it.scheduleError != null }) {
                    settings.putBoolean("alarm_occurrences_v5", true)
                    settings.putBoolean("ios_skip_removed_v1", true)
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            co.touchlab.kermit.Logger.e(e) { "Alarm recovery failed" }
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
        val usecases = rememberKoinInject<com.timilehinaregbesola.mathalarm.framework.Usecases>()
        val lyricist = rememberStrings()
        val isDarkTheme = preferences.shouldUseDarkColors()
        
        // Observe deeplink info from notification taps
        val deeplinkInfo by NotificationDeeplinkHolder.deeplinkInfo.collectAsState()
        
        // Debug log
        println("MainViewController: deeplinkInfo = $deeplinkInfo")
        
        ProvideStrings(lyricist) {
            MathAlarmTheme(darkTheme = isDarkTheme) {
                NavGraph(
                    preferences,
                    deeplinkInfo,
                    onDeeplinkConsumed = {
                        deeplinkInfo?.let(NotificationDeeplinkHolder::acknowledgeDeeplink)
                        resumeAlarmSchedules()
                    },
                    onAlarmReady = { payload ->
                        NotificationDeeplinkHolder.acknowledgeDeeplink(payload)
                        resumeAlarmSchedules()
                    },
                    validateAlarmHandoff = { id -> usecases.findAlarm(id)?.isOn == true },
                )
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
