package com.timilehinaregbesola.mathalarm

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.test.platform.app.InstrumentationRegistry
import cafe.adriel.lyricist.LocalStrings
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.navigation.NavGraph
import com.timilehinaregbesola.mathalarm.presentation.MainActivity
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import com.timilehinaregbesola.mathalarm.utils.strings.EnMathAlarmStrings
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
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
class AlarmSaveInteractionTest {
    @Test
    fun rapidSaveTapsWithIdleAndRealResumeWork() = runAndroidComposeUiTest<MainActivity> {
        val testActivity = requireNotNull(activity)
        val usecases = GlobalContext.get().get<Usecases>()
        val labels = EnMathAlarmStrings
        val prefix = "Save interaction regression "
        val results = mutableListOf<String>()
        var duplicates = false
        val output = File(testActivity.getExternalFilesDir(null), "save-interaction-results.txt")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            runOnUiThread {
                val preferences = testActivity.preferences
                listOf("math-challenges-v1", "skip-next-alarm-v1", "snooze-settings-v1")
                    .forEach(preferences::markAnnouncementSeen)
                testActivity.setContent {
                    CompositionLocalProvider(LocalStrings provides labels) {
                        MathAlarmTheme(darkTheme = false) { NavGraph(preferences, null) }
                    }
                }
            }
            for (busy in listOf(false, true)) {
                if (busy) {
                    runBlocking {
                        usecases.command {
                            repeat(10) { index ->
                                addAlarm(
                                    Alarm(
                                        alarmId = 910000L + index,
                                        title = prefix + "work $index",
                                        hour = 1,
                                        minute = 0,
                                        isSaved = true,
                                        isOn = true,
                                        repeat = true,
                                        repeatDays = "TTTTTTT",
                                    )
                                )
                            }
                        }
                    }
                }
                repeat(5) { iteration ->
                    val title = prefix + "$busy $iteration"
                    onNodeWithContentDescription("Add alarm").performClick()
                    waitUntil(timeoutMillis = 5_000) {
                        onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size == 1
                    }
                    onNode(hasSetTextAction()).performTextReplacement(title)
                    val bounds = onNodeWithText(labels.save.uppercase()).fetchSemanticsNode().boundsInWindow
                    val started = CompletableDeferred<Unit>()
                    var workMillis = 0L
                    val work = scope.launch {
                        if (busy) {
                            usecases.command {
                                val start = SystemClock.uptimeMillis()
                                started.complete(Unit)
                                rescheduleFutureAlarms.onAppResume()
                                workMillis = SystemClock.uptimeMillis() - start
                            }
                        } else {
                            started.complete(Unit)
                        }
                    }
                    runBlocking { started.await() }
                    // Inject without Compose's idle wait so real background work can overlap.
                    tap(bounds.center.x, bounds.center.y)
                    SystemClock.sleep(80)
                    tap(bounds.center.x, bounds.center.y)
                    runBlocking { work.join() }
                    waitUntil(timeoutMillis = 10_000) {
                        onAllNodesWithText(labels.save.uppercase()).fetchSemanticsNodes().isEmpty()
                    }
                    waitForIdle()
                    val saved = runBlocking { usecases.getSavedAlarms().first().filter { it.title == title } }
                    results += "busy=$busy iteration=$iteration resumeWorkMs=$workMillis saved=${saved.size} ids=${saved.map { it.alarmId }}"
                    output.writeText(results.joinToString("\n"))
                    if (saved.size != 1) duplicates = true
                    runBlocking { usecases.command { saved.forEach { deleteAlarm(it.alarmId) } } }
                }
            }
            assertTrue(results.joinToString("\n"), !duplicates)
        } finally {
            scope.cancel()
            runBlocking {
                usecases.command {
                    getSavedAlarms().first()
                        .filter { it.title.startsWith(prefix) }
                        .forEach { deleteAlarm(it.alarmId) }
                }
            }
        }
    }

    private fun tap(x: Float, y: Float) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val down = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, y, 0)
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            automation.injectInputEvent(event, true)
            event.recycle()
        }
    }
}
