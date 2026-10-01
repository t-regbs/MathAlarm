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
import com.timilehinaregbesola.mathalarm.navigation.AlarmHandoff
import com.timilehinaregbesola.mathalarm.navigation.decodeAlarmHandoff
import com.timilehinaregbesola.mathalarm.framework.database.AlarmMapper
import com.timilehinaregbesola.mathalarm.notification.NotificationDeeplinkHolder
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferencesImpl
import com.timilehinaregbesola.mathalarm.presentation.appsettings.shouldUseDarkColors
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import platform.UIKit.UIViewController

/**
 * Initialize Koin early - called from Swift App init() before UI loads.
 */
fun doInitKoin() {
    initKoin()
}

/** Swift uses the shared handoff codec instead of mirroring the database schema. */
fun createAlarmHandoffJson(alarmId: Long): String =
    com.timilehinaregbesola.mathalarm.navigation.encodeAlarmHandoff(AlarmHandoff(alarmId))

/**
 * Prewarm the database in background - called after Koin init
 * This initializes Room in background so it's ready when UI needs it
 */
fun prewarmDatabaseInBackground() {
    prewarmDatabase()
}

/** Reconcile missing or changed registrations on each activation. */
fun resumeAlarmSchedules() {
    CoroutineScope(Dispatchers.Main).launch {
        if (NotificationDeeplinkHolder.deeplinkInfo.value != null ||
            com.timilehinaregbesola.mathalarm.alarm.AlarmSchedulerBridge.hasPendingHandoff()) return@launch
        try {
            val usecases = (object : KoinComponent {}).getKoin().get<com.timilehinaregbesola.mathalarm.framework.Usecases>()
            usecases.command {
                if (NotificationDeeplinkHolder.deeplinkInfo.value != null ||
                    com.timilehinaregbesola.mathalarm.alarm.AlarmSchedulerBridge.hasPendingHandoff()) return@command
                rescheduleFutureAlarms.onAppResume(skipWhenNativeCurrent = true)
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
 * The Swift AppDelegate registers the AlarmKit bridge before alarm delivery.
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
                    resolveAlarmHandoff = { payload ->
                        decodeAlarmHandoff(payload)?.alarmId?.let { usecases.findAlarm(it) }
                            ?.takeIf { it.isOn }?.let { AlarmMapper().mapFromDomainModel(it) }
                    },
                    acknowledgeHandoffWhenReady = true,
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
