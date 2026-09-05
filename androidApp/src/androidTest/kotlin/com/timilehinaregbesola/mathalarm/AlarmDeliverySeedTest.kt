package com.timilehinaregbesola.mathalarm

import androidx.test.platform.app.InstrumentationRegistry
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.assertNotNull
import org.koin.core.context.GlobalContext

/** Seeds a real alarm, then exits so adb can kill the process and put the emulator in Doze.
 * Run only on a disposable debug emulator; see the review's validation instructions.
 */
class AlarmDeliverySeedTest {
    @Test fun seedColdStartAlarm() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".debug"))
        val trigger = System.currentTimeMillis() + 45_000
        val usecases = GlobalContext.get().get<Usecases>()
        usecases.command {
            val alarm = Alarm(alarmId = 900001, hour = 7, minute = 0, isOn = true, isSaved = true,
                alarmTone = "content://missing/reliability-tone", title = "Reliability test",
                scheduleInitialized = true, pendingTimes = listOf(trigger))
            addAlarm(alarm)
            GlobalContext.get().get<AlarmInteractor>().schedule(alarm, trigger)
        }
        val manager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
        assertNotNull(manager.nextAlarmClock)
    }
}
