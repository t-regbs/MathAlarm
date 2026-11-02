package com.timilehinaregbesola.mathalarm

import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import cafe.adriel.lyricist.LocalStrings
import cafe.adriel.lyricist.Strings as Translations
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.presentation.MainActivity
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Test

@OptIn(
    ExperimentalTestApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
    InternalCoroutinesApi::class
)
class AlarmErrorPresentationTest {
    @Test
    fun schedulingErrorUsesEveryLocaleAndHidesInternalState() = runAndroidComposeUiTest<MainActivity> {
        val diagnostic = "Internal OS error: /private/database/path"
        val alarm = mutableStateOf(Alarm(alarmId = 1, hour = 7, scheduleError = diagnostic))
        val translations = mutableStateOf(Translations.getValue("en"))
        runOnUiThread {
            requireNotNull(activity).setContent {
                CompositionLocalProvider(LocalStrings provides translations.value) {
                    MathAlarmTheme(darkTheme = false) {
                        AlarmItem(
                            alarm = alarm.value,
                            onEditAlarm = {},
                            onDeleteAlarm = {},
                            onCancelAlarm = {},
                            onScheduleAlarm = { _, _ -> },
                            darkTheme = false
                        )
                    }
                }
            }
        }
        for (strings in Translations.values) {
            runOnIdle { translations.value = strings }
            onNodeWithText(strings.alarmScheduleFailed).assertIsDisplayed()
            onNodeWithText(diagnostic).assertDoesNotExist()
        }
        runOnIdle { alarm.value = alarm.value.copy(scheduleError = Alarm.SCHEDULING_IN_PROGRESS) }
        onNodeWithText(translations.value.alarmScheduleFailed).assertDoesNotExist()
        onNodeWithText(Alarm.SCHEDULING_IN_PROGRESS).assertDoesNotExist()
    }
}
