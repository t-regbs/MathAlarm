package com.timilehinaregbesola.mathalarm.presentation

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListViewModel
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.AlarmSettingsViewModel
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.AlarmMathViewModel
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AppSettingsViewModel
import org.koin.mp.KoinPlatform

/** Main-thread feature factories. Native owners keep IDs outside view/layout recreation. */
object SharedFeatures {
    private val editors = mutableMapOf<String, AlarmSettingsViewModel>()
    private val challenges = mutableMapOf<String, AlarmMathViewModel>()
    fun list(): AlarmListViewModel = KoinPlatform.getKoin().get()
    fun settings(): AppSettingsViewModel = KoinPlatform.getKoin().get()
    fun newEditor(sessionId: String): AlarmSettingsViewModel = editor(sessionId, Alarm())
    fun editor(sessionId: String, alarm: Alarm): AlarmSettingsViewModel {
        require(sessionId.isNotBlank())
        return editors[sessionId]?.takeUnless { it.isClosed } ?: KoinPlatform.getKoin().get<AlarmSettingsViewModel>().also {
            it.setAlarm(alarm)
            editors[sessionId] = it
        }
    }
    fun closeEditor(sessionId: String) { editors.remove(sessionId)?.close() }
    fun challenge(sessionId: String): AlarmMathViewModel {
        require(sessionId.isNotBlank())
        return challenges[sessionId]?.takeUnless { it.isClosed } ?: KoinPlatform.getKoin().get<AlarmMathViewModel>().also { challenges[sessionId] = it }
    }
    fun closeChallenge(sessionId: String) { challenges.remove(sessionId)?.close() }
}
