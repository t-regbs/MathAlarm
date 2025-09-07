package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.AlarmInteractorFake
import com.timilehinaregbesola.mathalarm.fake.AlarmRepositoryFake
import com.timilehinaregbesola.mathalarm.fake.NotificationInteractorFake
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class CompleteAlarmTest {
    private val dataSource = AlarmRepositoryFake()

    private val alarmRepository = AlarmRepository(dataSource)

    private val alarmInteractor = AlarmInteractorFake()

    private val notificationInteractor = NotificationInteractorFake()

    private val completeAlarmUseCase = CompleteAlarm(alarmRepository, alarmInteractor, notificationInteractor)

    private val addAlarmUseCase = AddAlarm(alarmRepository)

    private val findAlarmUseCase = FindAlarm(alarmRepository)

    private val baseAlarm = Alarm(alarmId = 6, title = "turn me off", isOn = true)

    @Before
    fun setup() = runTest {
        alarmRepository.clear()
        alarmInteractor.clear()
        addAlarmUseCase(baseAlarm)
    }

    @Test
    fun `test if an alarm is turned off after completion`() = runTest {
        completeAlarmUseCase(baseAlarm)

        val result = findAlarmUseCase(baseAlarm.alarmId)

        Assert.assertNotNull(result)
        Assert.assertTrue(result?.isOn == false)
    }

    @Test
    fun `test if an alarm is turned off after completion by id`() = runTest {
        completeAlarmUseCase(baseAlarm.alarmId)

        val result = findAlarmUseCase(baseAlarm.alarmId)

        Assert.assertNotNull(result)
        Assert.assertTrue(result?.isOn == false)
    }

    @Test
    fun `test if an alarm is cancelled after completion`() = runTest {
        completeAlarmUseCase(baseAlarm)

        Assert.assertFalse(alarmInteractor.isAlarmScheduled(baseAlarm))
    }

    @Test
    fun `test if an alarm is cancelled after completion by id`() = runTest {
        completeAlarmUseCase(baseAlarm.alarmId)

        Assert.assertFalse(alarmInteractor.isAlarmScheduled(baseAlarm))
    }

    @Test
    fun `test if notification is dismissed after alarm is completed`() = runTest {
        completeAlarmUseCase(baseAlarm)

        Assert.assertFalse(notificationInteractor.isNotificationShown(baseAlarm.alarmId))
    }

    @Test
    fun `test if notification is dismissed after alarm is completed by id`() = runTest {
        completeAlarmUseCase(baseAlarm.alarmId)

        Assert.assertFalse(notificationInteractor.isNotificationShown(baseAlarm.alarmId))
    }
}
