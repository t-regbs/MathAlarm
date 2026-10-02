package com.timilehinaregbesola.mathalarm.application

import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.russhwolf.settings.Settings
import com.timilehinaregbesola.mathalarm.utils.AlarmErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AlarmServiceFailure(val alarmId: Long, val error: AlarmErrorMessage)
data class AlarmApplicationState(val failures: List<AlarmServiceFailure> = emptyList())

/** Durable service status, independent of any feature observer. Native views localize its codes. */
object AlarmApplicationStatus {
    private const val KEY = "mathalarm_recovery_failures_v1"
    private val storage by lazy { Settings() }
    private var restored = false
    private val mutable = MutableStateFlow(AlarmApplicationState())
    @NativeCoroutinesState
    val state: kotlinx.coroutines.flow.StateFlow<AlarmApplicationState> = mutable.asStateFlow()
    internal fun restore() {
        val ids = storage.getString(KEY, "").split(',').mapNotNull(String::toLongOrNull)
        mutable.value = AlarmApplicationState(ids.map { AlarmServiceFailure(it, AlarmErrorMessage.RECOVERY) })
        restored = true
    }
    fun reportRecoveryFailure(alarmId: Long) {
        if (!restored) restore()
        mutable.value = AlarmApplicationState(mutable.value.failures.filterNot { it.alarmId == alarmId } + AlarmServiceFailure(alarmId, AlarmErrorMessage.RECOVERY))
        persist()
    }
    fun clearRecoveryFailure(alarmId: Long) {
        if (!restored) restore()
        mutable.value = AlarmApplicationState(mutable.value.failures.filterNot { it.alarmId == alarmId })
        persist()
    }
    private fun persist() {
        storage.putString(KEY, mutable.value.failures.joinToString(",") { it.alarmId.toString() })
    }
}
