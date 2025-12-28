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
import com.timilehinaregbesola.mathalarm.navigation.NavGraph
import com.timilehinaregbesola.mathalarm.notification.IosNotificationSetup
import com.timilehinaregbesola.mathalarm.notification.NotificationDeeplinkHolder
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferencesImpl
import com.timilehinaregbesola.mathalarm.presentation.appsettings.shouldUseDarkColors
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import kotlinx.coroutines.InternalCoroutinesApi
import org.koin.core.component.KoinComponent
import platform.UIKit.UIViewController

/**
 * iOS Main View Controller - Entry point for the Compose Multiplatform UI
 * Note: Notification delegate is set up in Swift AppDelegate for proper timing
 */
@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
    InternalCoroutinesApi::class
)
fun MainViewController(): UIViewController {
    println("MainViewController: Initializing...")
    
    // Initialize Koin DI
    initKoin()
    
    // Setup notification categories for alarm actions
    IosNotificationSetup.setupNotificationCategories()
    
    // Request notification permissions
    IosNotificationSetup.requestPermissions { granted ->
        println("MainViewController: Notification permissions granted = $granted")
    }
    
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
