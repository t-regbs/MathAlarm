package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.AlarmInteractorFake
import com.timilehinaregbesola.mathalarm.fake.AlarmRepositoryFake
import com.timilehinaregbesola.mathalarm.usecases.alarm.AddAlarm
import com.timilehinaregbesola.mathalarm.usecases.alarm.DeleteAlarm
import com.timilehinaregbesola.mathalarm.usecases.alarm.FindAlarm
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteAlarmTest {
    private val dataSource = AlarmRepositoryFake()

    private val alarmRepository = AlarmRepository(dataSource)

    private val alarmInteractor = AlarmInteractorFake()

    private val deleteAlarmUseCase = DeleteAlarm(alarmRepository, alarmInteractor)

    private val addAlarmUseCase = AddAlarm(alarmRepository)

    private val findAlarmUseCase = FindAlarm(alarmRepository)

    private val baseAlarm = Alarm(alarmId = 11, isOn = true, title = "Delete me!!")

    @Before
    fun setup() = runBlockingTest {
        alarmRepository.clear()
        alarmInteractor.clear()
        addAlarmUseCase(baseAlarm)
    }

    @Test
    fun `test if alarm is deleted`() = runBlockingTest {
        deleteAlarmUseCase(baseAlarm)

        val assertAlarm = findAlarmUseCase(baseAlarm.alarmId)

        assertNull(assertAlarm)
    }

    @Test
    fun `test if deleted alarm is cancelled`() = runBlockingTest {
        deleteAlarmUseCase(baseAlarm)

        assertFalse(alarmInteractor.isAlarmScheduled(baseAlarm))
    }

    @Test
    fun `test if alarm is deleted with id`() = runBlockingTest {
        addAlarmUseCase(baseAlarm)
        deleteAlarmUseCase(baseAlarm.alarmId)

        val assertAlarm = findAlarmUseCase(baseAlarm.alarmId)

        assertNull(assertAlarm)
    }

    @Test
    fun `test if deleted alarm by id is cancelled`() = runBlockingTest {
        deleteAlarmUseCase(baseAlarm.alarmId)

        assertFalse(alarmInteractor.isAlarmScheduled(baseAlarm))
    }
}
