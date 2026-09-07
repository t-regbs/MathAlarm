package com.timilehinaregbesola.mathalarm

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.framework.database.AlarmEntity
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import com.timilehinaregbesola.mathalarm.notification.ActiveAlarmManager
import com.timilehinaregbesola.mathalarm.notification.AlarmService
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import org.koin.core.context.GlobalContext

@OptIn(
    androidx.compose.animation.ExperimentalAnimationApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    kotlinx.coroutines.InternalCoroutinesApi::class
)
class AlarmDeletionLifecycleTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val usecases get() = GlobalContext.get().get<Usecases>()
    private val firstId = 900003L
    private val secondId = 900004L

    @Test fun deleteRingingAlarmStopsPlayback() = runBlocking {
        withRingingAlarms(overlap = false) {
            usecases.command { deleteAlarm(firstId) }
            awaitStopped()
            assertNull(usecases.findAlarm(firstId))
            usecases.command { completeAlarm(firstId) }
            assertNull(ActiveAlarmManager.activeAlarmId)
        }
    }

    @Test fun deleteQueuedAlarmKeepsCurrentAlarmAndNeverPlaysDeletedAlarm() = runBlocking {
        withRingingAlarms(overlap = true) {
            usecases.command { deleteAlarm(secondId) }
            withTimeout(5_000) { while (secondId in playbackIds()) delay(100) }
            assertEquals(firstId, ActiveAlarmManager.activeAlarmId)
            usecases.command { completeAlarm(firstId) }
            awaitStopped()
        }
    }

    @Test fun clearOverlappingAlarmsStopsAllPlayback() = runBlocking {
        withRingingAlarms(overlap = true) {
            usecases.command { clearAlarms(listOfNotNull(findAlarm(firstId), findAlarm(secondId))) }
            awaitStopped()
            assertNull(usecases.findAlarm(firstId))
            assertNull(usecases.findAlarm(secondId))
        }
    }

    private suspend fun withRingingAlarms(overlap: Boolean, block: suspend () -> Unit) {
        check(context.packageName.endsWith(".debug"))
        val backend = GlobalContext.get().get<AlarmInteractor>()
        val ids = if (overlap) listOf(firstId, secondId) else listOf(firstId)
        try {
            for (id in ids) {
                val trigger = System.currentTimeMillis() + 1_500
                val alarm = Alarm(
                    alarmId = id, isOn = true, isSaved = true,
                    title = "Deletion lifecycle test", scheduleInitialized = true,
                    pendingTimes = listOf(trigger), scheduleTimeZone = TimeZone.currentSystemDefault().id
                )
                usecases.command { addAlarm(alarm); backend.schedule(alarm, trigger) }
                withTimeout(10_000) {
                    while (id !in playbackIds() || ActiveAlarmManager.activeAlarmId != firstId) delay(100)
                }
            }
            assertEquals(firstId, ActiveAlarmManager.activeAlarmId)
            block()
        } finally {
            for (id in ids) {
                AlarmService.stopAlarm(context, id)
                usecases.command { deleteAlarm(id) }
            }
            awaitStopped()
        }
    }

    private fun playbackIds(): List<Long> {
        val json = context.getSharedPreferences("active_alarm_playback", Context.MODE_PRIVATE)
            .getString("alarms", "[]") ?: "[]"
        return Json.decodeFromString<List<AlarmEntity>>(json).map { it.alarmId }
    }

    private suspend fun awaitStopped() {
        withTimeout(5_000) {
            while (ActiveAlarmManager.hasActiveAlarm() || playbackIds().isNotEmpty()) delay(100)
        }
    }
}
