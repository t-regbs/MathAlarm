package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.AlarmInteractorFake
import com.timilehinaregbesola.mathalarm.fake.AlarmRepositoryFake
import com.timilehinaregbesola.mathalarm.fake.NotificationInteractorFake
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class ShowAlarmTest {
    private val dataSource = AlarmRepositoryFake()

    private val alarmRepository = AlarmRepository(dataSource)

    private val alarmInteractor = AlarmInteractorFake()

    private val notificationInteractor = NotificationInteractorFake()

    private val addAlarmUseCase = AddAlarm(alarmRepository)

    private val findAlarmUseCase = FindAlarm(alarmRepository)

    private val scheduleNextAlarmUseCase = ScheduleNextAlarm(alarmInteractor)

    private val showAlarmUseCase = ShowAlarm(alarmRepository, notificationInteractor, scheduleNextAlarmUseCase)

    @Before
    fun setup() = runBlockingTest {
        alarmRepository.clear()
        alarmInteractor.clear()
        notificationInteractor.clear()
    }

    @Test
    fun `test if alarm is shown when alarm is on`() = runBlockingTest {
        val alarm = Alarm(alarmId = 1, title = "should show", isOn = true)
        addAlarmUseCase(alarm)
        showAlarmUseCase(alarm.alarmId)

        Assert.assertTrue(notificationInteractor.isNotificationShown(alarm.alarmId))
    }

    @Test
    fun `test if alarm is ignored when alarm is not on`() = runBlockingTest {
        val alarm = Alarm(alarmId = 2, title = "should not show")
        addAlarmUseCase(alarm)
        showAlarmUseCase(alarm.alarmId)

        Assert.assertFalse(notificationInteractor.isNotificationShown(alarm.alarmId))
    }

    @Test
    fun `test if next alarm is scheduled when alarm is repeating`() = runBlockingTest {
        val alarm = Alarm(alarmId = 3, title = "is repeating", repeat = true, isOn = true)
        addAlarmUseCase(alarm)
        showAlarmUseCase(alarm.alarmId)

        Assert.assertTrue(alarmInteractor.isAlarmScheduled(alarm))
    }

    @Test
    fun `test if next alarm is not scheduled when alarm is not repeating`() = runBlockingTest {
        val alarm = Alarm(alarmId = 4, title = "should no repeat", repeat = false, isOn = true)
        addAlarmUseCase(alarm)
        showAlarmUseCase(alarm.alarmId)

        Assert.assertFalse(alarmInteractor.isAlarmScheduled(alarm))
    }

    @Test
    fun `test if next alarm is not scheduled when alarm is not on`() = runBlockingTest {
        val alarm = Alarm(alarmId = 5, title = "alarm off", repeat = true, isOn = false)
        addAlarmUseCase(alarm)
        showAlarmUseCase(alarm.alarmId)

        Assert.assertFalse(alarmInteractor.isAlarmScheduled(alarm))
    }
}
