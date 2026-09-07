package com.timilehinaregbesola.mathalarm

import androidx.test.platform.app.InstrumentationRegistry
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.koin.core.context.GlobalContext

/** Uses a real AlarmManager delivery, queued behind an in-progress app command. */
class AlarmResumeRecoveryTest {
    @Test fun resumeRecoveryKeepsDelayedDelivery() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".debug"))
        val usecases = GlobalContext.get().get<Usecases>()
        val backend = GlobalContext.get().get<AlarmInteractor>()
        val id = 900002L
        try {
            usecases.command {
                val trigger = System.currentTimeMillis() + 2_000
                val alarm = Alarm(
                    alarmId = id,
                    isOn = true,
                    isSaved = true,
                    title = "Resume recovery regression",
                    scheduleInitialized = true,
                    scheduleTimeZone = TimeZone.currentSystemDefault().id,
                    pendingTimes = listOf(trigger)
                )
                addAlarm(alarm)
                backend.schedule(alarm, trigger)
                // Receiver waits for this lock, as it would during an app command.
                delay(3_000)
                rescheduleFutureAlarms.onAppResume()
            }
            val delivered = withTimeoutOrNull(10_000) {
                while (usecases.findAlarm(id)?.activeAt == null) delay(100)
                true
            }
            assertNotNull("Resume recovery must not discard the queued OS delivery", delivered)
        } finally {
            usecases.command {
                completeAlarm(id)
                deleteAlarm(id)
            }
        }
    }
}
