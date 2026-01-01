package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.AlarmInteractorFake
import com.timilehinaregbesola.mathalarm.fake.AlarmRepositoryFake
import com.timilehinaregbesola.mathalarm.fake.AlarmTimeCalculatorFake
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class ScheduleAlarmTest {
    private val dataSource = AlarmRepositoryFake()

    private val alarmRepository = AlarmRepository(dataSource)

    private val alarmInteractor = AlarmInteractorFake()
    
    private val alarmTimeCalculator = AlarmTimeCalculatorFake()

    private val addAlarmUseCase = AddAlarm(alarmRepository)

    private val findAlarmUseCase = FindAlarm(alarmRepository)

    private val scheduleAlarmUseCase = ScheduleAlarm(alarmRepository, alarmInteractor, alarmTimeCalculator)

    @BeforeTest
    fun setup() = runTest {
        alarmRepository.clear()
        alarmInteractor.clear()
    }

    @Test
    fun `test if alarm is scheduled`() = runTest {
        val newAlarm = Alarm(alarmId = 2, vibrate = true)
        val reschedule = false
        addAlarmUseCase(newAlarm)

        scheduleAlarmUseCase(newAlarm, reschedule)
        val result = findAlarmUseCase(newAlarm.alarmId)
        val assertAlarm = newAlarm.copy(isOn = true)

        assertEquals(assertAlarm, result)
    }
}
