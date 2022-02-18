package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.AlarmInteractorFake
import com.timilehinaregbesola.mathalarm.fake.AlarmRepositoryFake
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert
import org.junit.Test

@ExperimentalCoroutinesApi
class ClearAlarmsTest {
    private val dataSource = AlarmRepositoryFake()

    private val alarmRepository = AlarmRepository(dataSource)

    private val alarmInteractor = AlarmInteractorFake()

    private val addAlarmUseCase = AddAlarm(alarmRepository)

    private val clearAlarmsUseCase = ClearAlarms(alarmRepository, alarmInteractor)

    @After
    fun tearDown() = runBlockingTest {
        alarmRepository.clear()
        alarmInteractor.clear()
    }

    @FlowPreview
    @Test
    fun `test if alarms are deleted`() = runBlockingTest {
        val alarm1 = Alarm(alarmId = 1, title = "alarm 1", isSaved = false, isOn = true)
        val alarm2 = Alarm(alarmId = 2, title = "alarm 2", isSaved = true, isOn = false)
        val alarm3 = Alarm(alarmId = 3, title = "alarm 3", isSaved = false, isOn = true)
        val alarm4 = Alarm(alarmId = 4, title = "alarm 4", isSaved = true, isOn = false)
        val alarm5 = Alarm(alarmId = 5, title = "alarm 5", isSaved = false, isOn = true)
        val repoList = listOf(alarm1, alarm2, alarm3, alarm4, alarm5)
        repoList.forEach { alarm -> addAlarmUseCase(alarm) }

        clearAlarmsUseCase(repoList)
        val alarms = alarmRepository.getAlarms().flatMapConcat { it.asFlow() }.toList()

        Assert.assertEquals(emptyList<Alarm>(), alarms)
    }

    @Test
    fun `test if cleared alarms are not scheduled`() = runBlockingTest {
        val alarm1 = Alarm(alarmId = 1, title = "alarm 1", isSaved = false, isOn = true)
        val alarm2 = Alarm(alarmId = 2, title = "alarm 2", isSaved = true, isOn = false)
        val alarm3 = Alarm(alarmId = 3, title = "alarm 3", isSaved = false, isOn = true)
        val alarm4 = Alarm(alarmId = 4, title = "alarm 4", isSaved = true, isOn = false)
        val alarm5 = Alarm(alarmId = 5, title = "alarm 5", isSaved = false, isOn = true)
        val repoList = listOf(alarm1, alarm2, alarm3, alarm4, alarm5)
        repoList.forEach { alarm -> addAlarmUseCase(alarm) }

        clearAlarmsUseCase(repoList)
        repoList.forEach {
            Assert.assertFalse(alarmInteractor.isAlarmScheduled(it))
        }
    }
}
