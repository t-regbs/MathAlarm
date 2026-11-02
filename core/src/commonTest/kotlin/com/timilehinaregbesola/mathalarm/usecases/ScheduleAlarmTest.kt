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

        assertEquals(true, result?.isOn)
        assertEquals(true, result?.scheduleInitialized)
        assertEquals(alarmTimeCalculator.calculateAlarmTimes(newAlarm).sorted(), result?.pendingTimes)
        assertEquals(null, result?.scheduleError)
    }
    @Test
    fun `failed replacement preserves unresolved occurrence and recovery until retry accepted`() = runTest {
        var reject = true
        var recoveryCancellations = 0
        val backend = object : com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor by alarmInteractor {
            override suspend fun schedule(alarm: Alarm, timeInMillis: Long) {
                if (reject) error("OS rejected replacement")
                alarmInteractor.schedule(alarm, timeInMillis)
            }
            override fun cancelRecovery(alarm: Alarm) { recoveryCancellations++ }
        }
        val old = Alarm(alarmId = 91, isOn = true, activeAt = 1000, snoozeCount = 2)
        addAlarmUseCase(old)
        val command = ScheduleAlarm(alarmRepository, backend, alarmTimeCalculator)
        kotlin.test.assertFailsWith<IllegalStateException> { command(old, true) }
        val failed = findAlarmUseCase(91)!!
        assertEquals(1000L, failed.activeAt)
        assertEquals(2, failed.snoozeCount)
        assertEquals(0, recoveryCancellations)
        kotlin.test.assertNotNull(failed.scheduleError)
        reject = false
        command(failed, true)
        val accepted = findAlarmUseCase(91)!!
        assertEquals(null, accepted.activeAt)
        assertEquals(null, accepted.scheduleError)
        assertEquals(1, recoveryCancellations)
        assertEquals(alarmTimeCalculator.calculateAlarmTimes(old).sorted(), accepted.pendingTimes)
        kotlin.test.assertTrue(alarmInteractor.isAlarmScheduled(old))
    }

}
