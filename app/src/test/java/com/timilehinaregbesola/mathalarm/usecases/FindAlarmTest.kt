package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.AlarmRepositoryFake
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class FindAlarmTest {
    private val dataSource = AlarmRepositoryFake()

    private val alarmRepository = AlarmRepository(dataSource)

    private val addAlarmUseCase = AddAlarm(alarmRepository)

    private val findAlarmUseCase = FindAlarm(alarmRepository)

    @Before
    fun setup() = runTest {
        alarmRepository.clear()
    }

    @Test
    fun `test if alarm is found`() = runTest {
        val alarm = Alarm(alarmId = 22, title = "Find me now")
        addAlarmUseCase(alarm)

        val foundAlarm = findAlarmUseCase(alarm.alarmId)
        Assert.assertEquals(alarm, foundAlarm)
    }
}
