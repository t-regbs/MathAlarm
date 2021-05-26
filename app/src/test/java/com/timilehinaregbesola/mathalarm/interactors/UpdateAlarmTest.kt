package com.timilehinaregbesola.mathalarm.interactors

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.AlarmRepositoryFake
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Test

class UpdateAlarmTest {
    private val dataSource = AlarmRepositoryFake()

    private val alarmRepository = AlarmRepository(dataSource)

    private val updateAlarmUseCase = UpdateAlarm(alarmRepository)

    private val addAlarmUseCase = AddAlarm(alarmRepository)

    private val findAlarmUseCase = FindAlarm(alarmRepository)

    @ExperimentalCoroutinesApi
    @Test
    fun `test if alarm is updated`() = runBlockingTest {
        val alarm = Alarm(alarmId = 11, isOn = true, vibrate = true)
        addAlarmUseCase(alarm)

        val updatedAlarm = alarm.copy(isOn = false, snooze = 9)
        updateAlarmUseCase(updatedAlarm)

        val foundTask = findAlarmUseCase(alarm.alarmId)
        assertEquals(updatedAlarm, foundTask)
    }
}
