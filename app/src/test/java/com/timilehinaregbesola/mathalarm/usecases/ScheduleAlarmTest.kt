package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.AlarmInteractorFake
import com.timilehinaregbesola.mathalarm.fake.AlarmRepositoryFake
import com.timilehinaregbesola.mathalarm.usecases.alarm.AddAlarm
import com.timilehinaregbesola.mathalarm.usecases.alarm.FindAlarm
import com.timilehinaregbesola.mathalarm.usecases.alarm.ScheduleAlarm
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class ScheduleAlarmTest {
    private val dataSource = AlarmRepositoryFake()

    private val alarmRepository = AlarmRepository(dataSource)

    private val alarmInteractor = AlarmInteractorFake()

    private val addAlarmUseCase = AddAlarm(alarmRepository)

    private val findAlarmUseCase = FindAlarm(alarmRepository)

    private val scheduleAlarmUseCase = ScheduleAlarm(alarmRepository, alarmInteractor)

    @Before
    fun setup() = runBlockingTest {
        alarmRepository.clear()
        alarmInteractor.clear()
    }

    @Test
    fun `test if alarm is scheduled`() = runBlockingTest {
        val newAlarm = Alarm(alarmId = 2, vibrate = true)
        val reschedule = false
        addAlarmUseCase(newAlarm)

        scheduleAlarmUseCase(newAlarm, reschedule)
        val result = findAlarmUseCase(newAlarm.alarmId)
        val assertAlarm = newAlarm.copy(isOn = true)

        Assert.assertEquals(assertAlarm, result)
    }
}
