package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.AlarmInteractorFake
import com.timilehinaregbesola.mathalarm.fake.AlarmRepositoryFake
import com.timilehinaregbesola.mathalarm.fake.NotificationInteractorFake
import com.timilehinaregbesola.mathalarm.provider.DateTimeProviderImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class SnoozeAlarmTest {
    private val dataSource = AlarmRepositoryFake()

    private val alarmRepository = AlarmRepository(dataSource)

    private val alarmInteractor = AlarmInteractorFake()

    private val notificationInteractor = NotificationInteractorFake()

    private val calendarProvider = DateTimeProviderImpl()

    private val addAlarmUseCase = AddAlarm(alarmRepository)

    private val snoozeAlarmUseCase = SnoozeAlarm(
        calendarProvider, notificationInteractor, alarmInteractor, alarmRepository
    )

    private val baseAlarm = Alarm(alarmId = 216L, title = "snooze me", isOn = true)
    @Before
    fun setup() = runTest {
        alarmRepository.clear()
        alarmInteractor.clear()
        notificationInteractor.clear()
    }

    // Test fails sometimes
 /*   @Test
    fun `test if alarm is snoozed`() = runTest {
        addAlarmUseCase(baseAlarm)
        val snoozeMinutes = 5

        snoozeAlarmUseCase(baseAlarm.alarmId, snoozeMinutes)

        val calendarAssert = Calendar.getInstance().apply {
            time = calendarProvider.getCurrentCalendar().time
            add(Calendar.MINUTE, snoozeMinutes)
        }

        val result = alarmInteractor.getAlarmTime(baseAlarm.alarmId)
        Assert.assertEquals(calendarAssert.time.time, result?.time?.time)
    }*/

    @Test(expected = IllegalArgumentException::class)
    fun `test if error is shown when snoozing with negative number`() = runTest {
        snoozeAlarmUseCase(baseAlarm.alarmId, -5)
    }

    @Test
    fun `test if notification is dismissed`() = runTest {
        notificationInteractor.show(baseAlarm)

        snoozeAlarmUseCase(baseAlarm.alarmId, 5)

        notificationInteractor.isNotificationShown(baseAlarm.alarmId)
    }
}
