package com.timilehinaregbesola.mathalarm.interactors

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.AlarmRepositoryFake
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DeleteAlarmWithIdTest {
    private val dataSource = AlarmRepositoryFake()

    private val alarmRepository = AlarmRepository(dataSource)

//    private val interactor = Interac

    private val deleteAlarmUseCase = DeleteAlarmWithId(alarmRepository)

    private val addAlarmUseCase = AddAlarm(alarmRepository)

    private val findAlarmUseCase = FindAlarm(alarmRepository)

    @Before
    fun setup() = runBlocking {
        alarmRepository.clear()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `test if alarm is deleted`() = runBlockingTest {
        val alarm = Alarm(alarmId = 11, isOn = true, vibrate = true)
        addAlarmUseCase(alarm)
        deleteAlarmUseCase(alarm.alarmId)

        val foundAlarm = findAlarmUseCase(alarm.alarmId)

        assertNull(foundAlarm)
    }
}
