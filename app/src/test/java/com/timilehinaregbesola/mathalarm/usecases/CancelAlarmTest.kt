package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.AlarmInteractorFake
import com.timilehinaregbesola.mathalarm.fake.AlarmRepositoryFake
import com.timilehinaregbesola.mathalarm.fake.NotificationInteractorFake
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class CancelAlarmTest {
    private val dataSource = AlarmRepositoryFake()

    private val alarmRepository = AlarmRepository(dataSource)

    private val alarmInteractor = AlarmInteractorFake()

    private val notificationInteractor = NotificationInteractorFake()

    private val addAlarmUseCase = AddAlarm(alarmRepository)

    private val cancelAlarmUseCase = CancelAlarm(alarmInteractor)

    @Before
    fun setup() = runBlockingTest {
        alarmRepository.clear()
        alarmInteractor.clear()
        notificationInteractor.clear()
    }

    @Test
    fun `test if the alarm is canceled`() = runBlockingTest {
        val alarm = Alarm(alarmId = 11, title = "letsss gooo")
        addAlarmUseCase(alarm)
        cancelAlarmUseCase(alarm)

        assertFalse(alarmInteractor.isAlarmScheduled(alarm))
    }
}
