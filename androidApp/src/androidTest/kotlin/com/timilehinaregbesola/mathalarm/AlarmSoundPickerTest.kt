package com.timilehinaregbesola.mathalarm

import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import cafe.adriel.lyricist.LocalStrings
import com.timilehinaregbesola.mathalarm.navigation.NavGraph
import com.timilehinaregbesola.mathalarm.platform.getDefaultAlarmTone
import com.timilehinaregbesola.mathalarm.platform.getRingtoneTitle
import com.timilehinaregbesola.mathalarm.presentation.MainActivity
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import com.timilehinaregbesola.mathalarm.utils.strings.EnMathAlarmStrings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Opens and cancels the native picker; does not save or schedule any alarms. */
@OptIn(androidx.compose.animation.ExperimentalAnimationApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    kotlinx.coroutines.InternalCoroutinesApi::class)
class AlarmSoundPickerTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun androidOpensSystemPickerDirectlyAndCancellationKeepsDeviceDefault() {
        compose.runOnUiThread {
            val preferences = compose.activity.preferences
            listOf("math-challenges-v1", "skip-next-alarm-v1", "snooze-settings-v1")
                .forEach(preferences::markAnnouncementSeen)
            compose.activity.setContent {
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
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().readText()
                .lineSequence().last { it.contains('/') }.substringBefore('/').trim()
        }
        compose.onNodeWithContentDescription("Add alarm").performClick()
        compose.onNodeWithText(defaultTitle).assertIsDisplayed().performClick()
        val deadline = SystemClock.uptimeMillis() + 5_000
        while (automation.rootInActiveWindow?.packageName?.toString() != nativePackage && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(100)
        }
        assertEquals(nativePackage, automation.rootInActiveWindow?.packageName?.toString())
        automation.executeShellCommand("input keyevent 4").use { descriptor ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).readBytes()
        }
        compose.onNodeWithText(defaultTitle).assertIsDisplayed()
        compose.onNodeWithText(EnMathAlarmStrings.alarmSound).assertDoesNotExist()
        compose.onNodeWithText("Daybreak").assertDoesNotExist()
    }
}
