package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.AlarmInteractorFake
import com.timilehinaregbesola.mathalarm.fake.AlarmRepositoryFake
import com.timilehinaregbesola.mathalarm.fake.DateTimeProviderFake
import com.timilehinaregbesola.mathalarm.fake.NotificationInteractorFake
import com.timilehinaregbesola.mathalarm.provider.AlarmTimeCalculatorImpl
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SkipSnoozeDeliveryTest {
    @Test
    fun `skip and undo leave future and recently due snoozes registered and deliverable`() = runTest {
        for (repeat in listOf(false, true)) {
            for (undo in listOf(false, true)) {
                for (ageMillis in listOf(-1L, 0L, 1_000L, 59_999L)) {
                    val repository = AlarmRepository(AlarmRepositoryFake())
                    val backend = AlarmInteractorFake()
                    val clock = DateTimeProviderFake().apply { setFixedDateTime(LocalDateTime(2025, 1, 6, 6, 0)) }
                    val calculator = AlarmTimeCalculatorImpl(clock) { TimeZone.UTC }
                    val restore = RescheduleFutureAlarms(repository, backend, calculator) { TimeZone.UTC }
                    val skip = SkipNextAlarm(repository, calculator, restore) { TimeZone.UTC }
                    val next = LocalDateTime(2025, 1, 6, 7, 0).toInstant(TimeZone.UTC).toEpochMilliseconds()
                    val alarm = Alarm(alarmId = 41, hour = 7, minute = 0, repeat = repeat,
                        repeatDays = "FTFTFFF", isOn = true, isSaved = true,
                        scheduleInitialized = true, scheduleTimeZone = "UTC", pendingTimes = listOf(next))
                    repository.addAlarm(alarm)
                    if (undo) skip(41)
                    val now = LocalDateTime(2025, 1, 6, 6, 5, 1)
                    val snooze = now.toInstant(TimeZone.UTC).toEpochMilliseconds() - ageMillis
                    val saved = repository.findAlarm(41)!!.copy(isOn = true, snoozedUntil = snooze)
                    repository.updateAlarm(saved)
                    backend.scheduleSnooze(saved, snooze)
                    val registration = backend.getScheduledAlarms().getValue(41).single { it.snoozed }
                    clock.setFixedDateTime(now)

                    if (undo) assertTrue(skip.undo(41)) else assertEquals("2025-01-06", skip(41))

                    val updated = repository.findAlarm(41)!!
                    assertEquals(snooze, updated.snoozedUntil)
                    assertTrue(updated.isOn) // Even when skipping the final finite occurrence.
                    assertEquals(undo, next in updated.pendingTimes)
                    assertEquals(registration, backend.getScheduledAlarms().getValue(41).single { it.snoozed })
                    assertEquals(updated.pendingTimes,
                        backend.getScheduledAlarms().getValue(41).filterNot { it.snoozed }.map { it.timeInMillis }.sorted())
                    if (ageMillis >= 0) {
                        val notifications = NotificationInteractorFake()
                        ShowAlarm(repository, notifications, ScheduleNextAlarm(backend, calculator))(41, snooze, snoozed = true)
                        assertTrue(notifications.isNotificationShown(41))
                        assertNull(repository.findAlarm(41)!!.snoozedUntil)
                        assertEquals(snooze, repository.findAlarm(41)!!.activeAt)
                    }
                }
            }
        }
    }

    @Test
    fun `skip expires snoozes at the end of the delivery grace period`() = runTest {
        val repository = AlarmRepository(AlarmRepositoryFake())
        val backend = AlarmInteractorFake()
        val now = LocalDateTime(2025, 1, 6, 6, 5, 1)
        val clock = DateTimeProviderFake().apply { setFixedDateTime(now) }
        val calculator = AlarmTimeCalculatorImpl(clock) { TimeZone.UTC }
        val restore = RescheduleFutureAlarms(repository, backend, calculator) { TimeZone.UTC }
        val stale = now.toInstant(TimeZone.UTC).toEpochMilliseconds() - 60_000
        val alarm = Alarm(alarmId = 41, hour = 7, minute = 0, repeat = true,
            repeatDays = "FTFFFFF", isOn = true, snoozedUntil = stale)
        repository.addAlarm(alarm)
        backend.scheduleSnooze(alarm, stale)
        SkipNextAlarm(repository, calculator, restore) { TimeZone.UTC }(41)
        assertNull(repository.findAlarm(41)!!.snoozedUntil)
        assertFalse(backend.getScheduledAlarms().getValue(41).any { it.snoozed })
    }
}
