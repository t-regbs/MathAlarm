package com.timilehinaregbesola.mathalarm.presentation

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsControllerCompat
import cafe.adriel.lyricist.Lyricist
import cafe.adriel.lyricist.ProvideStrings
import cafe.adriel.lyricist.rememberStrings
import com.timilehinaregbesola.mathalarm.navigation.NavGraph
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferencesImpl
import com.timilehinaregbesola.mathalarm.presentation.appsettings.shouldUseDarkColors
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import com.timilehinaregbesola.mathalarm.presentation.ui.darkPrimary
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

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        deeplinkInfo = intent.extractAlarmJson()

        setContent {
            lyricist = rememberStrings()
            val isDarkTheme = preferences.shouldUseDarkColors()
            updateTheme(isDarkTheme)
            ProvideStrings(lyricist) {
                MathAlarmTheme(darkTheme = isDarkTheme) {
                    NavGraph(preferences)
                }
            }
        }
    }

    private fun updateTheme(darkTheme: Boolean) {
        window.apply {
            statusBarColor = if (darkTheme) darkPrimary.toArgb() else Color.WHITE
            navigationBarColor = if (darkTheme) darkPrimary.toArgb() else Color.WHITE
            WindowInsetsControllerCompat(this, this.decorView).isAppearanceLightStatusBars =
                !darkTheme
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
