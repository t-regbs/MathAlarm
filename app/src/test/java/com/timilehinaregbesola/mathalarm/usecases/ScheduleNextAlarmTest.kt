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
class ScheduleNextAlarmTest {
    private val dataSource = AlarmRepositoryFake()

    private val alarmRepository = AlarmRepository(dataSource)

    private val alarmInteractor = AlarmInteractorFake()

    private val addAlarmUseCase = AddAlarm(alarmRepository)

    private val scheduleNextAlarmUseCase = ScheduleNextAlarm(alarmInteractor)

    private val baseAlarm = Alarm(alarmId = 2, title = "new alarm", isOn = true)

    @Before
    fun setup() = runTest {
        alarmRepository.clear()
        alarmInteractor.clear()
    }

    @Test
    fun `test if alarm is scheduled`() = runTest {
        val alarm = baseAlarm.copy(repeat = true)
        addAlarmUseCase(alarm)

        scheduleNextAlarmUseCase(alarm)

        Assert.assertTrue(alarmInteractor.isAlarmScheduled(alarm))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test if fails if not repeating`() = runTest {
        val alarm = baseAlarm.copy(repeat = false)
        scheduleNextAlarmUseCase(alarm)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test if fails if not on`() = runTest {
        val alarm = baseAlarm.copy(repeat = true, isOn = false)
        scheduleNextAlarmUseCase(alarm)
    }
}
