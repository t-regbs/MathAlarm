package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.AlarmRepositoryFake
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AddAlarmTest {
    private val dataSource = AlarmRepositoryFake()

    private val alarmRepository = AlarmRepository(dataSource)

    private val addAlarmUseCase = AddAlarm(alarmRepository)

    private val findAlarmUseCase = FindAlarm(alarmRepository)

    @Before
    fun setup() = runBlocking {
        alarmRepository.clear()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `test if alarm is correctly added`() = runTest {
        val alarm = Alarm(alarmId = 12, isOn = true, vibrate = true)
        addAlarmUseCase(alarm)

        val result = findAlarmUseCase(alarm.alarmId)

        require(result != null)
        assertEquals(alarm, result)
    }
}
