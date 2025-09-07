package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.AlarmInteractorFake
import com.timilehinaregbesola.mathalarm.fake.AlarmRepositoryFake
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class RescheduleFutureAlarmsTest {
    private val dataSource = AlarmRepositoryFake()

    private val alarmRepository = AlarmRepository(dataSource)

    private val alarmInteractor = AlarmInteractorFake()

    private val addAlarmUseCase = AddAlarm(alarmRepository)

    private val rescheduleFutureAlarmUseCase = RescheduleFutureAlarms(alarmRepository, alarmInteractor)

    @Before
    fun setup() = runTest {
        alarmRepository.clear()
        alarmInteractor.clear()
    }

    @Test
    fun `test if saved alarms that are on are rescheduled`() = runTest {
        val alarm1 = Alarm(alarmId = 1, title = "alarm 1", isSaved = true, isOn = true)
        val alarm2 = Alarm(alarmId = 2, title = "alarm 2", isSaved = true, isOn = false)
        val alarm3 = Alarm(alarmId = 3, title = "alarm 3", isSaved = true, isOn = true)
        val alarm4 = Alarm(alarmId = 4, title = "alarm 4", isSaved = true, isOn = false)
        val alarm5 = Alarm(alarmId = 5, title = "alarm 5", isSaved = true, isOn = true)
        val repoList = listOf(alarm1, alarm2, alarm3, alarm4, alarm5)
        repoList.forEach { alarm -> addAlarmUseCase(alarm) }

        rescheduleFutureAlarmUseCase()

        Assert.assertTrue(alarmInteractor.isAlarmScheduled(alarm1))
        Assert.assertFalse(alarmInteractor.isAlarmScheduled(alarm2))
        Assert.assertTrue(alarmInteractor.isAlarmScheduled(alarm3))
        Assert.assertFalse(alarmInteractor.isAlarmScheduled(alarm4))
        Assert.assertTrue(alarmInteractor.isAlarmScheduled(alarm5))
    }

    @Test
    fun `test if unsaved alarms are not rescheduled`() = runTest {
        val alarm1 = Alarm(alarmId = 1, title = "alarm 1", isSaved = false, isOn = true)
        val alarm2 = Alarm(alarmId = 2, title = "alarm 2", isSaved = true, isOn = true)
        val alarm3 = Alarm(alarmId = 3, title = "alarm 3", isSaved = false, isOn = true)
        val alarm4 = Alarm(alarmId = 4, title = "alarm 4", isSaved = true, isOn = true)
        val alarm5 = Alarm(alarmId = 5, title = "alarm 5", isSaved = false, isOn = true)
        val repoList = listOf(alarm1, alarm2, alarm3, alarm4, alarm5)
        repoList.forEach { alarm -> addAlarmUseCase(alarm) }

        rescheduleFutureAlarmUseCase()

        Assert.assertFalse(alarmInteractor.isAlarmScheduled(alarm1))
        Assert.assertTrue(alarmInteractor.isAlarmScheduled(alarm2))
        Assert.assertFalse(alarmInteractor.isAlarmScheduled(alarm3))
        Assert.assertTrue(alarmInteractor.isAlarmScheduled(alarm4))
        Assert.assertFalse(alarmInteractor.isAlarmScheduled(alarm5))
    }
}
