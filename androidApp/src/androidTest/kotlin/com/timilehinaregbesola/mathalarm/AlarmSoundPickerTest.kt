package com.timilehinaregbesola.mathalarm

import android.os.ParcelFileDescriptor.AutoCloseInputStream
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.test.platform.app.InstrumentationRegistry
import cafe.adriel.lyricist.LocalStrings
import com.timilehinaregbesola.mathalarm.navigation.NavGraph
import com.timilehinaregbesola.mathalarm.platform.getDefaultAlarmTone
import com.timilehinaregbesola.mathalarm.platform.getRingtoneTitle
import com.timilehinaregbesola.mathalarm.presentation.MainActivity
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import com.timilehinaregbesola.mathalarm.utils.strings.EnMathAlarmStrings
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(
    ExperimentalTestApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
    InternalCoroutinesApi::class,
)
class AlarmSoundPickerTest {
    @Test
    fun androidOpensSystemPickerDirectlyAndCancellationKeepsDeviceDefault() = runAndroidComposeUiTest<MainActivity> {
        val host = requireNotNull(activity)
        runOnUiThread {
            val preferences = host.preferences
            listOf("math-challenges-v1", "skip-next-alarm-v1", "snooze-settings-v1")
                .forEach(preferences::markAnnouncementSeen)
            host.setContent {
                CompositionLocalProvider(LocalStrings provides EnMathAlarmStrings) {
                    MathAlarmTheme(darkTheme = false) { NavGraph(preferences, null) }
                }
            }
        }
        val defaultTitle = getRingtoneTitle(getDefaultAlarmTone())
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        // Shell resolution avoids Android's package-visibility filtering of app queries.
        val nativePackage = automation.executeShellCommand(
            "cmd package resolve-activity --brief -a android.intent.action.RINGTONE_PICKER"
        ).use { descriptor ->
            AutoCloseInputStream(descriptor).bufferedReader().readText()
                .lineSequence().last { it.contains('/') }.substringBefore('/').trim()
        }
        onNodeWithContentDescription("Add alarm").performClick()
        onNodeWithText(defaultTitle).assertIsDisplayed().performClick()
        waitUntil(timeoutMillis = 5_000) {
            automation.rootInActiveWindow?.packageName?.toString() == nativePackage
        }
        assertEquals(nativePackage, automation.rootInActiveWindow?.packageName?.toString())
        automation.executeShellCommand("input keyevent 4").use { descriptor ->
            AutoCloseInputStream(descriptor).readBytes()
        }
        onNodeWithText(defaultTitle).assertIsDisplayed()
        onNodeWithText(EnMathAlarmStrings.alarmSound).assertDoesNotExist()
        onNodeWithText("Daybreak").assertDoesNotExist()
    }
}
