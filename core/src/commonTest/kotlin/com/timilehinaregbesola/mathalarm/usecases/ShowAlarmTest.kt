package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.AlarmInteractorFake
import com.timilehinaregbesola.mathalarm.fake.AlarmRepositoryFake
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

    private val notificationInteractor = NotificationInteractorFake()

    private val addAlarmUseCase = AddAlarm(alarmRepository)

    private val showAlarmUseCase = ShowAlarm(alarmRepository, notificationInteractor)

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
    fun `test notification is shown for repeating alarm`() = runTest {
        val alarm = Alarm(alarmId = 3, title = "is repeating", repeat = true, isOn = true)
        addAlarmUseCase(alarm)
        showAlarmUseCase(alarm.alarmId)

        // ShowAlarm should show notification but not schedule next alarm
        assertTrue(notificationInteractor.isNotificationShown(alarm.alarmId))
        assertFalse(alarmInteractor.isAlarmScheduled(alarm))
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
}
