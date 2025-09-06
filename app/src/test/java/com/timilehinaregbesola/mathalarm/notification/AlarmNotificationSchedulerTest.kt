package com.timilehinaregbesola.mathalarm.notification

import android.content.Context
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Before
import java.util.*

class AlarmNotificationSchedulerTest {

    private val mockContext = mockk<Context>(relaxed = true)

    private val mockAlarm = mockk<Alarm>()

    private val scheduler = AlarmNotificationScheduler(mockContext)

    @Before
    fun setUp() {
        val cal = Calendar.getInstance()
        mockkStatic("com.timilehinaregbesola.mathalarm.utils.extension-context")
        every { mockAlarm.repeatDays } returns "FFFTFFF"
        every { mockAlarm.alarmId } returns 22L
        every { mockAlarm.repeat } returns false
//        every { mockAlarm.newCal } returns cal
        every { mockAlarm.hour } returns cal[Calendar.HOUR_OF_DAY]
        every { mockAlarm.minute } returns cal[Calendar.MINUTE]
    }

//    @Test
//    fun `check if alarm scheduled is valid`() {
//        scheduler.scheduleAlarm(mockAlarm, false)
//        verify { mockContext.setAlarm(mockAlarm.newCal.time.time, any()) }
//    }
//
//    @Test
//    fun `check if alarm canceled is valid`() {
//        scheduler.cancelAlarm(mockAlarm)
//        verify { mockContext.cancelAlarm(any()) }
//    }
}
