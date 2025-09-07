package com.timilehinaregbesola.mathalarm.notification

import android.content.Context
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.datetime.LocalDateTime
import org.junit.Before

class AlarmNotificationSchedulerTest {

    private val mockContext = mockk<Context>(relaxed = true)

    private val mockAlarm = mockk<Alarm>()

    private val scheduler = AlarmNotificationScheduler(mockContext)

    @Before
    fun setUp() {
        val now = LocalDateTime(2023, 1, 1, 10, 30) // Example LocalDateTime
        mockkStatic("com.timilehinaregbesola.mathalarm.utils.extension-context")
        every { mockAlarm.repeatDays } returns "FFFTFFF"
        every { mockAlarm.alarmId } returns 22L
        every { mockAlarm.repeat } returns false
        every { mockAlarm.newDateTime } returns now
        every { mockAlarm.hour } returns now.hour
        every { mockAlarm.minute } returns now.minute
    }

//    @Test
//    fun `check if alarm scheduled is valid`() {
//        scheduler.scheduleAlarm(mockAlarm, false)
//        verify { mockContext.setExactAlarm(mockAlarm.newDateTime.time.toNanosecondOfDay(), any()) }
//    }
//
//    @Test
//    fun `check if alarm canceled is valid`() {
//        scheduler.cancelAlarm(mockAlarm)
//        verify { mockContext.cancelAlarm(any()) }
//    }
}
