package com.timilehinaregbesola.mathalarm.presentation

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.notification.ActiveAlarmManager
import com.timilehinaregbesola.mathalarm.notification.AlarmService
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsControllerCompat
import cafe.adriel.lyricist.Lyricist
import cafe.adriel.lyricist.ProvideStrings
import cafe.adriel.lyricist.rememberStrings
import com.timilehinaregbesola.mathalarm.navigation.NavGraph
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferencesImpl
import com.timilehinaregbesola.mathalarm.presentation.appsettings.shouldUseDarkColors
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import com.timilehinaregbesola.mathalarm.utils.strings.Strings
import kotlinx.coroutines.InternalCoroutinesApi
import org.koin.android.ext.android.inject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@ExperimentalComposeUiApi
@InternalCoroutinesApi
@ExperimentalAnimationApi
class MainActivity : AppCompatActivity() {
    val preferences: AlarmPreferencesImpl by inject()
    private lateinit var lyricist: Lyricist<Strings>
    private val logger = Logger.withTag("MainActivity")

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setupLockScreenFlags()

        deeplinkInfo = intent.extractAlarmJson()

        setContent {
            val isDarkTheme = preferences.shouldUseDarkColors()
            enableEdgeToEdge()
            updateStatusBarColor(isDarkTheme)
            lyricist = rememberStrings()
            ProvideStrings(lyricist) {
                MathAlarmTheme(darkTheme = isDarkTheme) {
                    NavGraph(preferences, deeplinkInfo)
                }
            }
        }
    }

    private fun setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }

    private fun updateStatusBarColor(darkTheme: Boolean) {
        window.apply {
            WindowInsetsControllerCompat(this, this.decorView).isAppearanceLightStatusBars =
                !darkTheme
        }
    }

    override fun onStop() {
        super.onStop()
        // If user closes the app while alarm is active, refresh the notification
        // This will trigger the full-screen intent to pop up again
        if (ActiveAlarmManager.hasActiveAlarm()) {
            logger.d("App stopped with active alarm - refreshing notification")
            AlarmService.refreshNotification(this)
        }
    }

    private fun Intent.extractAlarmJson(): String? {
        return data?.lastPathSegment
            ?.takeIf { it.startsWith("$PARAM=") }
            ?.substringAfter("$PARAM=")
            ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
    }

    companion object {
        private const val PARAM = "alarmId"
        var deeplinkInfo by mutableStateOf<String?>(null)
    }
}
