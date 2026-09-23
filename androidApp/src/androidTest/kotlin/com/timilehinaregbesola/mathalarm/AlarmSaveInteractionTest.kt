package com.timilehinaregbesola.mathalarm

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import cafe.adriel.lyricist.LocalStrings
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.navigation.NavGraph
import com.timilehinaregbesola.mathalarm.presentation.MainActivity
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import com.timilehinaregbesola.mathalarm.utils.strings.EnMathAlarmStrings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext
import java.io.File

/** Real touch events, Room and AlarmManager; no delayed/fake save implementation. */
@OptIn(androidx.compose.animation.ExperimentalAnimationApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class, InternalCoroutinesApi::class)
class AlarmSaveInteractionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun rapidSaveTapsWithIdleAndRealResumeWork() {
        val usecases = GlobalContext.get().get<Usecases>()
        val labels = EnMathAlarmStrings
        val prefix = "Save interaction regression "
        val results = mutableListOf<String>()
        var duplicates = false
        val output = File(compose.activity.getExternalFilesDir(null), "save-interaction-results.txt")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            compose.runOnUiThread {
                val preferences = compose.activity.preferences
                listOf("math-challenges-v1", "skip-next-alarm-v1", "snooze-settings-v1")
                    .forEach(preferences::markAnnouncementSeen)
                compose.activity.setContent {
                    CompositionLocalProvider(LocalStrings provides labels) {
                        MathAlarmTheme(darkTheme = false) { NavGraph(preferences, null) }
                    }
                }
            }
            for (busy in listOf(false, true)) {
                if (busy) runBlocking { usecases.command {
                    repeat(10) { index ->
                        addAlarm(Alarm(alarmId = 910000L + index, title = prefix + "work $index",
                            hour = 1, minute = 0, isSaved = true, isOn = true,
                            repeat = true, repeatDays = "TTTTTTT"))
                    }
                } }
                repeat(5) { iteration ->
                    val title = prefix + "$busy $iteration"
                    compose.onNodeWithContentDescription("Add alarm").performClick()
                    compose.onNode(hasSetTextAction()).performTextReplacement(title)
                    val bounds = compose.onNodeWithText(labels.save.uppercase()).fetchSemanticsNode().boundsInWindow
                    val started = CompletableDeferred<Unit>()
                    var workMillis = 0L
                    val work = scope.launch {
                        if (busy) usecases.command {
                            val start = SystemClock.uptimeMillis()
                            started.complete(Unit)
                            rescheduleFutureAlarms.onAppResume()
                            workMillis = SystemClock.uptimeMillis() - start
                        } else started.complete(Unit)
                    }
                    runBlocking { started.await() }
                    // Inject without Compose's idle wait so real background work can overlap.
                    tap(bounds.center.x, bounds.center.y)
                    SystemClock.sleep(80)
                    tap(bounds.center.x, bounds.center.y)
                    runBlocking { work.join() }
                    compose.waitUntil(10_000) {
                        compose.onAllNodesWithText(labels.save.uppercase()).fetchSemanticsNodes().isEmpty()
                    }
                    compose.waitForIdle()
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
            runBlocking { usecases.command {
                getSavedAlarms().first().filter { it.title.startsWith(prefix) }.forEach { deleteAlarm(it.alarmId) }
            } }
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
