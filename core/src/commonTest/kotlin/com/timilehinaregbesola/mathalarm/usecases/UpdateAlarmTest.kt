package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.AlarmRepositoryFake
import com.timilehinaregbesola.mathalarm.fake.AlarmInteractorFake
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertFailsWith

class UpdateAlarmTest {
    private val dataSource = AlarmRepositoryFake()

    private val alarmRepository = AlarmRepository(dataSource)

    private val updateAlarmUseCase = UpdateAlarm(alarmRepository)

    private val addAlarmUseCase = AddAlarm(alarmRepository)

    private val findAlarmUseCase = FindAlarm(alarmRepository)

    @Test
    fun disablingSnoozeTurnsOffTheFinalOneTimeOccurrence() = runTest {
        val native = AlarmInteractorFake()
        val alarm = Alarm(alarmId = 12, isOn = true, scheduleInitialized = true, snoozedUntil = 1000)
        addAlarmUseCase(alarm)
        native.scheduleSnooze(alarm, 1000)

        UpdateAlarm(alarmRepository, native)(alarm.copy(snooze = 0))

        val saved = findAlarmUseCase(12)!!
        assertFalse(saved.isOn)
        assertNull(saved.snoozedUntil)
        assertFalse(native.isAlarmScheduled(saved))
    }

    @Test
    fun failedSnoozeCancellationKeepsTheOccurrenceEnabledForRetry() = runTest {
        val native = AlarmInteractorFake()
        val alarm = Alarm(alarmId = 12, isOn = true, scheduleInitialized = true, snoozedUntil = 1000)
        addAlarmUseCase(alarm)
        native.scheduleSnooze(alarm, 1000)
        val rejecting = object : AlarmInteractor by native {
            override fun cancelSnooze(alarm: Alarm) { error("Cancellation failed") }
        }

        assertFailsWith<IllegalStateException> {
            UpdateAlarm(alarmRepository, rejecting)(alarm.copy(snooze = 0))
        }

        assertEquals(alarm, findAlarmUseCase(12))
        assertEquals(1000L, native.getAlarmTimeMillis(12))
        UpdateAlarm(alarmRepository, native)(alarm.copy(snooze = 0))
        assertFalse(findAlarmUseCase(12)!!.isOn)
    }

    @Test
    fun disablingSnoozePreservesOtherReasonsToRemainEnabled() = runTest {
        val snoozed = Alarm(alarmId = 12, isOn = true, scheduleInitialized = true, snoozedUntil = 1000)
        val alarms = listOf(
            snoozed.copy(repeat = true),
            snoozed.copy(pendingTimes = listOf(2000)),
            snoozed.copy(activeAt = 500),
            snoozed.copy(scheduleInitialized = false),
            snoozed.copy(snoozedUntil = null),
            snoozed.copy(isOn = false),
        )
        for (alarm in alarms) {
            addAlarmUseCase(alarm)
            updateAlarmUseCase(alarm.copy(snooze = 0))
            assertEquals(alarm.copy(snooze = 0, snoozedUntil = null), findAlarmUseCase(12))
        }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `test if alarm is updated`() = runTest {
        val alarm = Alarm(alarmId = 11, isOn = true, vibrate = true)
        addAlarmUseCase(alarm)

        val updatedAlarm = alarm.copy(isOn = false, snooze = 9)
        updateAlarmUseCase(updatedAlarm)

        val foundTask = findAlarmUseCase(alarm.alarmId)
        assertEquals(updatedAlarm, foundTask)
    }
}
