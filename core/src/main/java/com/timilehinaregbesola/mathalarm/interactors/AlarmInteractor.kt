package com.timilehinaregbesola.mathalarm.interactors

import com.timilehinaregbesola.mathalarm.domain.model.Alarm

/**
 * Contract to interact with the Alarm layer.
 */
interface AlarmInteractor {

    /**
     * Schedules a new alarm.
     *
     * @param alarm the alarm
     * @param reschedule whether repeating
     */
    fun schedule(alarm: Alarm, reschedule: Boolean): Boolean

    /**
     * Cancels an alarm.
     *
     * @param alarm the alarm
     */
    fun cancel(alarm: Alarm)
}
