package com.timilehinaregbesola.mathalarm

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import cafe.adriel.lyricist.LocalStrings
import cafe.adriel.lyricist.Strings as Translations
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.presentation.MainActivity
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import java.io.File
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
class AlarmCardLayoutTest {
    @Test
    fun largeTextSkippedCardKeepsUndoAndExpansionIndependentOfEdit() = runAndroidComposeUiTest<MainActivity> {
        val labels = Translations.getValue("de")
        var edited = 0
        var undone = 0
        var enabled = 0
        runOnUiThread {
            requireNotNull(activity).setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalStrings provides labels,
                    LocalDensity provides Density(density.density, 2f),
                ) {
                    MathAlarmTheme(darkTheme = false) {
                        Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
                            AlarmItem(
                                alarm = Alarm(
                                    alarmId = 42,
                                    hour = 7,
                                    minute = 0,
                                    title = "Workdays",
                                    isOn = false,
                                    repeatDays = "FTFTFFF",
                                    skippedDate = "2030-01-07",
                                ),
                                onEditAlarm = { edited++ },
                                onDeleteAlarm = {},
                                onCancelAlarm = {},
                                onUndoSkip = { undone++ },
                                onScheduleAlarm = { _, _ -> enabled++ },
                                darkTheme = false,
                                selected = true,
                            )
                        }
                    }
                }
            }
        }
        onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            File(context.getExternalFilesDir(null), "alarm-card-large-text.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        onNodeWithText(labels.undo).performScrollTo().assertIsDisplayed().performClick()
        runOnIdle {
            assertEquals(1, undone)
            assertEquals(0, edited)
        }
        onNode(isToggleable()).performScrollTo().performClick()
        runOnIdle {
            assertEquals(1, enabled)
            assertEquals(1, undone)
        }
        onNodeWithText("Workdays", substring = true).performScrollTo().performClick()
        onNodeWithText(labels.edit).performScrollTo().assertIsDisplayed().performClick()
        runOnIdle { assertEquals(1, edited) }
        onNodeWithText("Workdays", substring = true).performScrollTo().performClick()
        runOnIdle { assertEquals(1, edited) }
        onNodeWithText(labels.edit).assertDoesNotExist()
        onNodeWithContentDescription(labels.expand).assertExists()
    }
}
