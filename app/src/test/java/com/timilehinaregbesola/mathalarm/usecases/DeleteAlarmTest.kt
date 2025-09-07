package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.AlarmInteractorFake
import com.timilehinaregbesola.mathalarm.fake.AlarmRepositoryFake
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun setup() = runTest {
        alarmRepository.clear()
        alarmInteractor.clear()
        addAlarmUseCase(baseAlarm)
    }

    @Test
    fun `test if alarm is deleted`() = runTest {
        deleteAlarmUseCase(baseAlarm)

        val assertAlarm = findAlarmUseCase(baseAlarm.alarmId)

        assertNull(assertAlarm)
    }

    @Test
    fun `test if deleted alarm is cancelled`() = runTest {
        deleteAlarmUseCase(baseAlarm)

        assertFalse(alarmInteractor.isAlarmScheduled(baseAlarm))
    }

    @Test
    fun `test if alarm is deleted with id`() = runTest {
        addAlarmUseCase(baseAlarm)
        deleteAlarmUseCase(baseAlarm.alarmId)

        val assertAlarm = findAlarmUseCase(baseAlarm.alarmId)

        assertNull(assertAlarm)
    }

    @Test
    fun `test if deleted alarm by id is cancelled`() = runTest {
        deleteAlarmUseCase(baseAlarm.alarmId)

        assertFalse(alarmInteractor.isAlarmScheduled(baseAlarm))
    }
}
