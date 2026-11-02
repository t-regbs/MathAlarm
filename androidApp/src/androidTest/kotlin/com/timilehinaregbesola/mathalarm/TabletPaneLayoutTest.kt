package com.timilehinaregbesola.mathalarm

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.AndroidComposeUiTest
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.compose.ui.test.v2.runEmptyComposeUiTest
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import cafe.adriel.lyricist.LocalStrings
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.navigation.NavGraph
import com.timilehinaregbesola.mathalarm.presentation.MainActivity
import com.timilehinaregbesola.mathalarm.presentation.appsettings.shouldUseDarkColors
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import com.timilehinaregbesola.mathalarm.utils.strings.EnMathAlarmStrings
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.koin.core.context.GlobalContext

@OptIn(
    ExperimentalTestApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
    InternalCoroutinesApi::class,
)
class TabletPaneLayoutTest {
    @Test
    fun editorAndSettingsShareTheWindowAndProtectDrafts() = runAndroidComposeUiTest<MainActivity> {
        val activity = checkNotNull(activity)
        assumeTrue(activity.resources.configuration.screenWidthDp >= 840)
        val labels = EnMathAlarmStrings
        val usecases = GlobalContext.get().get<Usecases>()
        val ids = listOf(900020L, 900021L)
        val reviewRequests = AtomicInteger()
        try {
            runBlocking {
                usecases.command {
                    addAlarm(
                        Alarm(
                            alarmId = ids[0],
                            hour = 7,
                            minute = 0,
                            title = "Morning commute",
                            isSaved = true,
                            isOn = false,
                            repeatDays = "TTTTTFF",
                        )
                    )
                    addAlarm(
                        Alarm(
                            alarmId = ids[1],
                            hour = 9,
                            minute = 30,
                            title = "Weekend",
                            isSaved = true,
                            isOn = false,
                            repeatDays = "FFFFFTT",
                        )
                    )
                }
            }
            runOnUiThread {
                val preferences = activity.preferences
                listOf("math-challenges-v1", "skip-next-alarm-v1", "snooze-settings-v1")
                    .forEach(preferences::markAnnouncementSeen)
                activity.setContent {
                    CompositionLocalProvider(LocalStrings provides labels) {
                        key("tablet-test") {
                            MathAlarmTheme(darkTheme = preferences.shouldUseDarkColors()) {
                                NavGraph(preferences, null, onRequestReview = { reviewRequests.incrementAndGet() })
                            }
                        }
                    }
                }
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Morning commute", substring = true).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText(labels.selectAlarmPrompt).assertIsDisplayed()
            onNodeWithText("Morning commute", substring = true).performClick()
            onNodeWithText(labels.selectAlarmPrompt).assertIsDisplayed()
            onNodeWithText(labels.delete).assertIsDisplayed()
            editAlarm("Morning commute")
            onNodeWithText(labels.save.uppercase()).assertIsDisplayed()
            onAllNodes(isDialog()).assertCountEquals(0)
            val list = onNodeWithText(labels.alarms).fetchSemanticsNode().boundsInRoot
            val save = onNodeWithText(labels.save.uppercase()).fetchSemanticsNode().boundsInRoot
            assertTrue("Editor should be beside the list", save.left > list.right)
            capture("tablet-alarm-editor.png")

            onAllNodesWithText("07:00 AM").onLast().performClick()
            onNodeWithText(labels.selectHour).assertIsDisplayed()
            onNodeWithText("Weekend", substring = true).assertIsDisplayed()
            onAllNodes(isDialog()).assertCountEquals(0)
            capture("tablet-time-picker.png")
            onNodeWithText(labels.cancel).performClick()
            onNodeWithText(labels.testAlarm.uppercase()).performClick()
            onNodeWithText(labels.cancel).assertIsDisplayed()
            onNodeWithText("Weekend", substring = true).assertIsDisplayed()
            onAllNodes(isDialog()).assertCountEquals(0)
            onNodeWithText(labels.cancel).performClick()

            onNode(hasSetTextAction()).performTextReplacement("Draft commute")
            onNodeWithText("Weekend", substring = true).performClick()
            onNodeWithText("Draft commute").assertExists()
            onNodeWithText(labels.closeAlarmEditor).assertDoesNotExist()
            editAlarm("Weekend")
            onNodeWithText(labels.closeAlarmEditor).assertIsDisplayed()
            onNodeWithText(labels.cancel).performClick()
            onNodeWithText("Draft commute").assertExists()

            onNodeWithContentDescription("Settings").performClick()
            onNodeWithText(labels.colorTheme).assertIsDisplayed()
            assertEquals("Visible list beside Settings must not trigger review", 0, reviewRequests.get())
            onNodeWithText("Weekend", substring = true).assertIsDisplayed()
            onAllNodes(isDialog()).assertCountEquals(0)
            capture("tablet-app-settings.png")
            onNodeWithContentDescription(labels.back).performClick()
            onNodeWithText("Draft commute").assertExists()

            onNodeWithText(labels.mathChallengeTitle).performScrollTo().performClick()
            onNodeWithText(labels.applyChallenge.uppercase()).assertIsDisplayed()
            onNodeWithText("Weekend", substring = true).assertIsDisplayed()
            onAllNodes(isDialog()).assertCountEquals(0)
            capture("tablet-challenge-editor.png")
            onNodeWithText(labels.cancel.uppercase()).performClick()

            editAlarm("Weekend")
            onNodeWithText(labels.discardChanges).performClick()
            onNode(hasSetTextAction()).assertTextContains("Weekend")
            onNode(hasSetTextAction()).performTextReplacement("Weekend updated")
            assertEquals("Editing and previewing must not trigger review", 0, reviewRequests.get())
            onNodeWithText(labels.save.uppercase()).performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText(labels.selectAlarmPrompt).fetchSemanticsNodes().isNotEmpty()
            }
            runBlocking {
                assertEquals("Weekend updated", usecases.findAlarm(ids[1])?.title)
                assertEquals("Morning commute", usecases.findAlarm(ids[0])?.title)
                assertTrue(usecases.findAlarm(ids[1])!!.pendingTimes.isEmpty())
            }
            waitForIdle()
            mainClock.advanceTimeBy(1_500)
            waitUntil(timeoutMillis = 5_000) { reviewRequests.get() == 1 }
            onNodeWithContentDescription("Settings").performClick()
            onNodeWithText(labels.colorTheme).assertIsDisplayed()
            onNodeWithContentDescription(labels.back).performClick()
            mainClock.advanceTimeBy(1_500)
            waitForIdle()
            assertEquals("Returning from another pane must not reuse an old save", 1, reviewRequests.get())
        } finally {
            runBlocking { usecases.command { ids.forEach { deleteAlarm(it) } } }
        }
    }

    @Test
    fun hiddenDraftRemainsProtectedAfterActivityRecreation() = runEmptyComposeUiTest {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var screenWidthDp = 0
            scenario.onActivity { activity ->
                screenWidthDp = activity.resources.configuration.screenWidthDp
            }
            assumeTrue(screenWidthDp >= 840)
            val labels = EnMathAlarmStrings
            val usecases = GlobalContext.get().get<Usecases>()
            val ids = listOf(900023L, 900024L)
            try {
                // Keep MainActivity's production composition so recreation restores the same tree.
                waitForIdle()
                repeat(3) {
                    if (onAllNodesWithText(labels.gotIt).fetchSemanticsNodes().isNotEmpty()) {
                        onNodeWithText(labels.gotIt).performClick()
                    }
                }
                runBlocking {
                    usecases.command {
                        addAlarm(Alarm(alarmId = ids[0], title = "Recreation source", isSaved = true, isOn = false))
                        addAlarm(Alarm(alarmId = ids[1], title = "Recreation target", isSaved = true, isOn = false))
                    }
                }
                waitUntil(timeoutMillis = 5_000) {
                    onAllNodesWithText("Recreation source").fetchSemanticsNodes().isNotEmpty()
                }
                onNodeWithText("Recreation source").performClick()
                editAlarm("Recreation source")
                onNode(hasSetTextAction()).performTextReplacement("Protected draft")
                onNodeWithContentDescription("Settings").performClick()
                onNodeWithText(labels.colorTheme).assertIsDisplayed()
                scenario.recreate()
                onNodeWithText(labels.colorTheme).assertIsDisplayed()
                onNodeWithText("Recreation target").performClick()
                editAlarm("Recreation target")
                onNodeWithText(labels.closeAlarmEditor).assertIsDisplayed()
                onNodeWithText(labels.cancel).performClick()
                onNodeWithContentDescription(labels.back).performClick()
                onNode(hasSetTextAction()).assertTextContains("Protected draft")
                runBlocking { assertEquals("Recreation source", usecases.findAlarm(ids[0])?.title) }
            } finally {
                runBlocking { usecases.command { ids.forEach { deleteAlarm(it) } } }
            }
        }
    }

    @Test
    fun compactWindowKeepsTheFamiliarSheet() = runAndroidComposeUiTest<MainActivity> {
        val activity = checkNotNull(activity)
        assumeTrue(activity.resources.configuration.screenWidthDp < 600)
        val labels = EnMathAlarmStrings
        val usecases = GlobalContext.get().get<Usecases>()
        val id = 900022L
        val reviewRequests = AtomicInteger()
        try {
            runBlocking {
                usecases.command {
                    addAlarm(
                        Alarm(
                            alarmId = id,
                            hour = 7,
                            minute = 0,
                            title = "Compact layout",
                            isSaved = true,
                            isOn = false,
                        )
                    )
                }
            }
            runOnUiThread {
                val preferences = activity.preferences
                listOf("math-challenges-v1", "skip-next-alarm-v1", "snooze-settings-v1")
                    .forEach(preferences::markAnnouncementSeen)
                activity.setContent {
                    key("compact-test") {
                        CompositionLocalProvider(LocalStrings provides labels) {
                            MathAlarmTheme(darkTheme = preferences.shouldUseDarkColors()) {
                                NavGraph(preferences, null, onRequestReview = { reviewRequests.incrementAndGet() })
                            }
                        }
                    }
                }
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Compact layout").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText(labels.selectAlarmPrompt).assertDoesNotExist()
            onNodeWithText("Compact layout").performClick()
            onNodeWithText(labels.edit).performClick()
            onNodeWithText(labels.save.uppercase()).assertIsDisplayed()
            capture("phone-alarm-editor.png")
            onNode(hasSetTextAction()).performTextReplacement("Phone draft")
            onNodeWithText(labels.testAlarm.uppercase()).performClick()
            onNodeWithText(labels.cancel).performClick()
            onNodeWithText("Phone draft").assertExists()
            assertEquals(0, reviewRequests.get())
            onNodeWithText(labels.save.uppercase()).performClick()
            waitForIdle()
            mainClock.advanceTimeBy(1_500)
            waitUntil(timeoutMillis = 5_000) { reviewRequests.get() == 1 }
            onNodeWithContentDescription("Settings").performClick()
            onNodeWithText(labels.colorTheme).assertIsDisplayed()
            onNodeWithContentDescription(labels.back).performClick()
            mainClock.advanceTimeBy(1_500)
            waitForIdle()
            assertEquals(1, reviewRequests.get())
        } finally {
            runBlocking { usecases.command { deleteAlarm(id) } }
        }
    }

    private fun ComposeUiTest.editAlarm(title: String) {
        onNode(hasText(EnMathAlarmStrings.edit) and hasAnyAncestor(hasText(title, substring = true)))
            .performScrollTo().performClick()
    }

    private fun AndroidComposeUiTest<MainActivity>.capture(name: String) {
        waitForIdle()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(checkNotNull(activity).getExternalFilesDir(null), name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
