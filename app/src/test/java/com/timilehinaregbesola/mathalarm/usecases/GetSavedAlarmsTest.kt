package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.AlarmInteractorFake
import com.timilehinaregbesola.mathalarm.fake.AlarmRepositoryFake
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class GetSavedAlarmsTest {
    private val dataSource = AlarmRepositoryFake()

    private val alarmRepository = AlarmRepository(dataSource)

    private val alarmInteractor = AlarmInteractorFake()

    private val addAlarmUseCase = AddAlarm(alarmRepository)

    private val getSavedAlarmsUseCase = GetSavedAlarms(alarmRepository)

    @Before
    fun setup() = runBlockingTest {
        alarmRepository.clear()
        alarmInteractor.clear()
    }

    @OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
    @Test
    fun `test if saved alarms are retrieved`() = runBlockingTest {
        val alarm1 = Alarm(alarmId = 1, title = "alarm 1", isSaved = false, isOn = true)
        val alarm2 = Alarm(alarmId = 2, title = "alarm 2", isSaved = true, isOn = false)
        val alarm3 = Alarm(alarmId = 3, title = "alarm 3", isSaved = false, isOn = true)
        val alarm4 = Alarm(alarmId = 4, title = "alarm 4", isSaved = true, isOn = false)
        val alarm5 = Alarm(alarmId = 5, title = "alarm 5", isSaved = false, isOn = true)
        val repoList = listOf(alarm1, alarm2, alarm3, alarm4, alarm5)
        repoList.forEach { alarm -> addAlarmUseCase(alarm) }

        val alarms = getSavedAlarmsUseCase().flatMapConcat { it.asFlow() }.toList()

        Assert.assertEquals(listOf(alarm2, alarm4), alarms)
    }
}
