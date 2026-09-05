package com.timilehinaregbesola.mathalarm.fake

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.provider.AlarmTimeCalculator
import kotlin.time.ExperimentalTime

/**
 * Fake implementation of AlarmTimeCalculator for testing.
 */
@OptIn(ExperimentalTime::class)
class AlarmTimeCalculatorFake : AlarmTimeCalculator {
    
    private var currentTimeMillis: Long = 1_893_909_600_000L // 2030-01-06T06:00:00Z
    
    /**
     * Set a custom current time for testing.
     */
    fun setCurrentTime(timeMillis: Long) {
        currentTimeMillis = timeMillis
    }

    override fun calculateAlarmTimes(alarm: Alarm): List<Long> {
        // Return a simple future time for testing (1 hour from "now")
        return listOf(currentTimeMillis + 3600_000L)
    }

    override fun calculateNextAlarmTime(alarm: Alarm): Long? {
        return currentTimeMillis + 3600_000L
    }

    override fun isInFuture(timeInMillis: Long): Boolean {
        return timeInMillis > currentTimeMillis
    }
}
