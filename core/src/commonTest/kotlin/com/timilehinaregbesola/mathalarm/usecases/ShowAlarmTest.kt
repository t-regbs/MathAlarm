package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.AlarmInteractorFake
import com.timilehinaregbesola.mathalarm.fake.AlarmRepositoryFake
import com.timilehinaregbesola.mathalarm.fake.AlarmTimeCalculatorFake
import com.timilehinaregbesola.mathalarm.fake.NotificationInteractorFake
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class ShowAlarmTest {
    private val dataSource = AlarmRepositoryFake()

    private val alarmRepository = AlarmRepository(dataSource)

    private val alarmInteractor = AlarmInteractorFake()

    private val alarmTimeCalculator = AlarmTimeCalculatorFake()

    private val notificationInteractor = NotificationInteractorFake()

    private val addAlarmUseCase = AddAlarm(alarmRepository)

    private val scheduleNextAlarmUseCase = ScheduleNextAlarm(alarmInteractor, alarmTimeCalculator)

    private val showAlarmUseCase = ShowAlarm(alarmRepository, notificationInteractor, scheduleNextAlarmUseCase)

    @BeforeTest
    fun setup() = runTest {
        alarmRepository.clear()
        alarmInteractor.clear()
        notificationInteractor.clear()
    }

    @Test
    fun `test if alarm is shown when alarm is on`() = runTest {
        val alarm = Alarm(alarmId = 1, title = "should show", isOn = true)
        addAlarmUseCase(alarm)
        showAlarmUseCase(alarm.alarmId)

        assertTrue(notificationInteractor.isNotificationShown(alarm.alarmId))
    }

    @Test
    fun `test if alarm is ignored when alarm is not on`() = runTest {
        val alarm = Alarm(alarmId = 2, title = "should not show")
        addAlarmUseCase(alarm)
        showAlarmUseCase(alarm.alarmId)

        assertFalse(notificationInteractor.isNotificationShown(alarm.alarmId))
    }

    @Test
    fun `repeating alarm schedules next occurrence when shown`() = runTest {
        val alarm = Alarm(alarmId = 3, title = "is repeating", repeat = true, isOn = true)
        addAlarmUseCase(alarm)
        showAlarmUseCase(alarm.alarmId)

        assertTrue(notificationInteractor.isNotificationShown(alarm.alarmId))
        assertTrue(alarmInteractor.isAlarmScheduled(alarm))
    }

    @Test
    fun `test notification is shown for non-repeating alarm`() = runTest {
        val alarm = Alarm(alarmId = 4, title = "should no repeat", repeat = false, isOn = true)
        addAlarmUseCase(alarm)
        showAlarmUseCase(alarm.alarmId)

        assertTrue(notificationInteractor.isNotificationShown(alarm.alarmId))
        assertFalse(alarmInteractor.isAlarmScheduled(alarm))
    }

    @Test
    fun `test notification not shown when alarm is off`() = runTest {
        val alarm = Alarm(alarmId = 5, title = "alarm off", repeat = true, isOn = false)
        addAlarmUseCase(alarm)
        showAlarmUseCase(alarm.alarmId)

        assertFalse(notificationInteractor.isNotificationShown(alarm.alarmId))
        assertFalse(alarmInteractor.isAlarmScheduled(alarm))
    }

    @Test
    fun `snooze delivery and duplicate broadcasts preserve count but a new occurrence resets it`() = runTest {
        addAlarmUseCase(Alarm(alarmId = 77, isOn = true, scheduleInitialized = true,
            snoozeCount = 2, snoozedUntil = 1000, pendingTimes = listOf(2000)))
        showAlarmUseCase(77, 1000, snoozed = true)
        kotlin.test.assertEquals(2, alarmRepository.findAlarm(77)!!.snoozeCount)
        showAlarmUseCase(77, 1000, snoozed = true)
        kotlin.test.assertEquals(2, alarmRepository.findAlarm(77)!!.snoozeCount)
        showAlarmUseCase(77, 2000)
        kotlin.test.assertEquals(0, alarmRepository.findAlarm(77)!!.snoozeCount)
    }

}
