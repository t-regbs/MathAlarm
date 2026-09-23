package com.timilehinaregbesola.mathalarm

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import cafe.adriel.lyricist.LocalStrings
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.navigation.NavGraph
import com.timilehinaregbesola.mathalarm.presentation.appsettings.shouldUseDarkColors
import com.timilehinaregbesola.mathalarm.presentation.MainActivity
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import com.timilehinaregbesola.mathalarm.utils.strings.EnMathAlarmStrings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext

/** Run on a window at least 840dp wide; fixtures never schedule or ring alarms. */
@OptIn(
    androidx.compose.animation.ExperimentalAnimationApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    kotlinx.coroutines.InternalCoroutinesApi::class,
)
class TabletPaneLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun editorAndSettingsShareTheWindowAndProtectDrafts() {
        assumeTrue(compose.activity.resources.configuration.screenWidthDp >= 840)
        val labels = EnMathAlarmStrings
        val usecases = GlobalContext.get().get<Usecases>()
        val ids = listOf(900020L, 900021L)
        val reviewRequests = java.util.concurrent.atomic.AtomicInteger()
        try {
            runBlocking {
                usecases.command {
                    addAlarm(Alarm(alarmId = ids[0], hour = 7, minute = 0, title = "Morning commute",
                        isSaved = true, isOn = false, repeatDays = "TTTTTFF"))
                    addAlarm(Alarm(alarmId = ids[1], hour = 9, minute = 30, title = "Weekend",
                        isSaved = true, isOn = false, repeatDays = "FFFFFTT"))
                }
            }
            compose.runOnUiThread {
                val preferences = compose.activity.preferences
                listOf("math-challenges-v1", "skip-next-alarm-v1", "snooze-settings-v1")
                    .forEach(preferences::markAnnouncementSeen)
                compose.activity.setContent {
                    CompositionLocalProvider(LocalStrings provides labels) {
                        androidx.compose.runtime.key("tablet-test") {
                            MathAlarmTheme(darkTheme = preferences.shouldUseDarkColors()) {
                                NavGraph(preferences, null, onRequestReview = { reviewRequests.incrementAndGet() })
                            }
                        }
                    }
                }
            }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithText("Morning commute", substring = true).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(labels.selectAlarmPrompt).assertIsDisplayed()
            compose.onNodeWithText("Morning commute", substring = true).performClick()
            compose.onNodeWithText(labels.selectAlarmPrompt).assertIsDisplayed()
            compose.onNodeWithText(labels.delete).assertIsDisplayed()
            editAlarm("Morning commute")
            compose.onNodeWithText(labels.save.uppercase()).assertIsDisplayed()
            compose.onAllNodes(isDialog()).assertCountEquals(0)
            val list = compose.onNodeWithText(labels.alarms).fetchSemanticsNode().boundsInRoot
            val save = compose.onNodeWithText(labels.save.uppercase()).fetchSemanticsNode().boundsInRoot
            assertTrue("Editor should be beside the list", save.left > list.right)
            capture("tablet-alarm-editor.png")

            compose.onAllNodesWithText("07:00 AM").onLast().performClick()
            compose.onNodeWithText(labels.selectHour).assertIsDisplayed()
            compose.onNodeWithText("Weekend", substring = true).assertIsDisplayed()
            compose.onAllNodes(isDialog()).assertCountEquals(0)
            capture("tablet-time-picker.png")
            compose.onNodeWithText(labels.cancel).performClick()
            compose.onNodeWithText(labels.testAlarm.uppercase()).performClick()
            compose.onNodeWithText(labels.cancel).assertIsDisplayed()
            compose.onNodeWithText("Weekend", substring = true).assertIsDisplayed()
            compose.onAllNodes(isDialog()).assertCountEquals(0)
            compose.onNodeWithText(labels.cancel).performClick()

            compose.onNode(hasSetTextAction()).performTextReplacement("Draft commute")
            compose.onNodeWithText("Weekend", substring = true).performClick()
            // Expanding another card must not navigate or interrupt the current draft.
            compose.onNodeWithText("Draft commute").assertExists()
            compose.onNodeWithText(labels.closeAlarmEditor).assertDoesNotExist()
            editAlarm("Weekend")
            compose.onNodeWithText(labels.closeAlarmEditor).assertIsDisplayed()
            compose.onNodeWithText(labels.cancel).performClick()
            compose.onNodeWithText("Draft commute").assertExists()

            compose.onNodeWithContentDescription("Settings").performClick()
            compose.onNodeWithText(labels.colorTheme).assertIsDisplayed()
            assertEquals("Visible list beside Settings must not trigger review", 0, reviewRequests.get())
            compose.onNodeWithText("Weekend", substring = true).assertIsDisplayed()
            compose.onAllNodes(isDialog()).assertCountEquals(0)
            capture("tablet-app-settings.png")
            compose.onNodeWithContentDescription(labels.back).performClick()
            compose.onNodeWithText("Draft commute").assertExists()

            compose.onNodeWithText(labels.mathChallengeTitle).performScrollTo().performClick()
            compose.onNodeWithText(labels.applyChallenge.uppercase()).assertIsDisplayed()
            compose.onNodeWithText("Weekend", substring = true).assertIsDisplayed()
            compose.onAllNodes(isDialog()).assertCountEquals(0)
            capture("tablet-challenge-editor.png")
            compose.onNodeWithText(labels.cancel.uppercase()).performClick()

            editAlarm("Weekend")
            compose.onNodeWithText(labels.discardChanges).performClick()
            compose.onNode(hasSetTextAction()).assertTextContains("Weekend")
            compose.onNode(hasSetTextAction()).performTextReplacement("Weekend updated")
            assertEquals("Editing and previewing must not trigger review", 0, reviewRequests.get())
            compose.onNodeWithText(labels.save.uppercase()).performClick()
            compose.waitUntil(5_000) {
                compose.onAllNodesWithText(labels.selectAlarmPrompt).fetchSemanticsNodes().isNotEmpty()
            }
            runBlocking {
                assertEquals("Weekend updated", usecases.findAlarm(ids[1])?.title)
                assertEquals("Morning commute", usecases.findAlarm(ids[0])?.title)
                assertTrue(usecases.findAlarm(ids[1])!!.pendingTimes.isEmpty())
            }
            compose.waitUntil(5_000) { reviewRequests.get() == 1 }
            compose.onNodeWithContentDescription("Settings").performClick()
            compose.onNodeWithText(labels.colorTheme).assertIsDisplayed()
            compose.onNodeWithContentDescription(labels.back).performClick()
            compose.mainClock.advanceTimeBy(1_500)
            compose.waitForIdle()
            assertEquals("Returning from another pane must not reuse an old save", 1, reviewRequests.get())
        } finally {
            runBlocking { usecases.command { ids.forEach { deleteAlarm(it) } } }
        }
    }

    @Test fun hiddenDraftRemainsProtectedAfterActivityRecreation() {
        assumeTrue(compose.activity.resources.configuration.screenWidthDp >= 840)
        val labels = EnMathAlarmStrings
        val usecases = GlobalContext.get().get<Usecases>()
        val ids = listOf(900023L, 900024L)
        try {
            // Keep MainActivity's production composition so recreation restores the same tree.
            compose.waitForIdle()
            repeat(3) {
                if (compose.onAllNodesWithText(labels.gotIt).fetchSemanticsNodes().isNotEmpty()) {
                    compose.onNodeWithText(labels.gotIt).performClick()
                }
            }
            runBlocking { usecases.command {
                addAlarm(Alarm(alarmId = ids[0], title = "Recreation source", isSaved = true, isOn = false))
                addAlarm(Alarm(alarmId = ids[1], title = "Recreation target", isSaved = true, isOn = false))
            } }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithText("Recreation source").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Recreation source").performClick()
            editAlarm("Recreation source")
            compose.onNode(hasSetTextAction()).performTextReplacement("Protected draft")
            compose.onNodeWithContentDescription("Settings").performClick()
            compose.onNodeWithText(labels.colorTheme).assertIsDisplayed()
            compose.activityRule.scenario.recreate()
            compose.onNodeWithText(labels.colorTheme).assertIsDisplayed()
            compose.onNodeWithText("Recreation target").performClick()
            editAlarm("Recreation target")
            compose.onNodeWithText(labels.closeAlarmEditor).assertIsDisplayed()
            compose.onNodeWithText(labels.cancel).performClick()
            compose.onNodeWithContentDescription(labels.back).performClick()
            compose.onNode(hasSetTextAction()).assertTextContains("Protected draft")
            runBlocking { assertEquals("Recreation source", usecases.findAlarm(ids[0])?.title) }
        } finally {
            runBlocking { usecases.command { ids.forEach { deleteAlarm(it) } } }
        }
    }

    @Test fun compactWindowKeepsTheFamiliarSheet() {
        assumeTrue(compose.activity.resources.configuration.screenWidthDp < 600)
        val labels = EnMathAlarmStrings
        val usecases = GlobalContext.get().get<Usecases>()
        val id = 900022L
        val reviewRequests = java.util.concurrent.atomic.AtomicInteger()
        try {
            runBlocking { usecases.command {
                addAlarm(Alarm(alarmId = id, hour = 7, minute = 0, title = "Compact layout",
                    isSaved = true, isOn = false))
            } }
            compose.runOnUiThread {
                val preferences = compose.activity.preferences
                listOf("math-challenges-v1", "skip-next-alarm-v1", "snooze-settings-v1")
                    .forEach(preferences::markAnnouncementSeen)
                compose.activity.setContent {
                    androidx.compose.runtime.key("compact-test") {
                        CompositionLocalProvider(LocalStrings provides labels) {
                            MathAlarmTheme(darkTheme = preferences.shouldUseDarkColors()) {
                                NavGraph(preferences, null, onRequestReview = { reviewRequests.incrementAndGet() })
                            }
                        }
                    }
                }
            }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithText("Compact layout").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(labels.selectAlarmPrompt).assertDoesNotExist()
            compose.onNodeWithText("Compact layout").performClick()
            compose.onNodeWithText(labels.edit).performClick()
            compose.onNodeWithText(labels.save.uppercase()).assertIsDisplayed()
            capture("phone-alarm-editor.png")
            compose.onNode(hasSetTextAction()).performTextReplacement("Phone draft")
            compose.onNodeWithText(labels.testAlarm.uppercase()).performClick()
            compose.onNodeWithText(labels.cancel).performClick()
            compose.onNodeWithText("Phone draft").assertExists()
            assertEquals(0, reviewRequests.get())
            compose.onNodeWithText(labels.save.uppercase()).performClick()
            compose.waitUntil(5_000) { reviewRequests.get() == 1 }
            compose.onNodeWithContentDescription("Settings").performClick()
            compose.onNodeWithText(labels.colorTheme).assertIsDisplayed()
            compose.onNodeWithContentDescription(labels.back).performClick()
            compose.mainClock.advanceTimeBy(1_500)
            compose.waitForIdle()
            assertEquals(1, reviewRequests.get())
        } finally {
            runBlocking { usecases.command { deleteAlarm(id) } }
        }
    }

    private fun editAlarm(title: String) {
        compose.onNode(hasText(EnMathAlarmStrings.edit) and hasAnyAncestor(hasText(title, substring = true)))
            .performScrollTo().performClick()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        java.io.File(compose.activity.getExternalFilesDir(null), name).outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
