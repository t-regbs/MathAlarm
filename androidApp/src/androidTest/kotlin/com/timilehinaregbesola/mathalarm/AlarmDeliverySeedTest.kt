package com.timilehinaregbesola.mathalarm

import android.app.AlarmManager
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.koin.core.context.GlobalContext

/** External runner owns process death/reboot and observes delivery before restarting instrumentation. */
class AlarmDeliverySeedTest {
    private val arguments get() = InstrumentationRegistry.getArguments()
    private fun context(): Context {
        assumeTrue("Run through scripts/test_alarm_delivery.py", arguments.getString("alarmLifecycle") == "true")
        return InstrumentationRegistry.getInstrumentation().targetContext.also {
            check(it.packageName.endsWith(".debug"))
        }
    }

    @Test fun seedColdStartAlarm() = runBlocking {
        val context = context()
        val delaySeconds = arguments.getString("delaySeconds", "45").toLong()
        require(delaySeconds in 15..600)
        val trigger = System.currentTimeMillis() + delaySeconds * 1000
        val usecases = GlobalContext.get().get<Usecases>()
        usecases.command {
            findAlarm(TEST_ID)?.let { deleteAlarm(it) }
            context.getSharedPreferences("alarm_delivery_log", Context.MODE_PRIVATE).edit().clear().commit()
            val alarm = Alarm(alarmId = TEST_ID, hour = 7, minute = 0, isOn = true, isSaved = true,
                alarmTone = "content://missing/reliability-tone", title = "Reliability test", snooze = 1,
                scheduleInitialized = true, pendingTimes = listOf(trigger))
            addAlarm(alarm)
            GlobalContext.get().get<AlarmInteractor>().schedule(alarm, trigger)
        }
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        assertEquals("The OS must register this occurrence", trigger, manager.nextAlarmClock?.triggerTime)
    }

    @Test fun verifyCompleted() = runBlocking {
        context()
        val stored = GlobalContext.get().get<Usecases>().findAlarm(TEST_ID)!!
        assertFalse(stored.isOn)
        assertNull(stored.activeAt)
        assertNull(stored.snoozedUntil)
        assertTrue(stored.pendingTimes.isEmpty())
    }

    @Test fun cleanup(): Unit = runBlocking {
        context()
        val usecases = GlobalContext.get().get<Usecases>()
        usecases.command { findAlarm(TEST_ID)?.let { deleteAlarm(it) } }
    }

    companion object { const val TEST_ID = 900001L }
}
