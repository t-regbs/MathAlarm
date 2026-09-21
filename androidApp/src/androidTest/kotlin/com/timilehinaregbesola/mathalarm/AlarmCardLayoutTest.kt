package com.timilehinaregbesola.mathalarm

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import cafe.adriel.lyricist.Strings as Translations
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.presentation.MainActivity
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(
    androidx.compose.animation.ExperimentalAnimationApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    kotlinx.coroutines.InternalCoroutinesApi::class,
)
class AlarmCardLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun largeTextSkippedCardKeepsUndoAndExpansionIndependentOfEdit() {
        val labels = Translations.getValue("de")
        var edited = 0
        var undone = 0
        var enabled = 0
        compose.runOnUiThread {
            compose.activity.setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalStrings provides labels,
                    LocalDensity provides Density(density.density, 2f),
                ) {
                    MathAlarmTheme(darkTheme = false) {
                        Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
                            AlarmItem(
                                alarm = Alarm(alarmId = 42, hour = 7, minute = 0, title = "Workdays",
                                    isOn = false, repeatDays = "FTFTFFF", skippedDate = "2030-01-07"),
                                onEditAlarm = { edited++ },
                                onDeleteAlarm = {}, onCancelAlarm = {},
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
        compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
            java.io.File(context.getExternalFilesDir(null), "alarm-card-large-text.png").outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        compose.onNodeWithText(labels.undo).performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, undone); assertEquals(0, edited) }
        compose.onNode(isToggleable()).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, enabled); assertEquals(1, undone) }
        compose.onNodeWithText("Workdays").performScrollTo().performClick()
        compose.onNodeWithText(labels.edit).performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, edited) }
        compose.onNodeWithText("Workdays").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, edited) }
        compose.onNodeWithText(labels.edit).assertDoesNotExist()
        compose.onNodeWithContentDescription(labels.expand).assertExists()
    }
}
