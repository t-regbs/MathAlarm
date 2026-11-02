package com.timilehinaregbesola.mathalarm.notification

import com.timilehinaregbesola.mathalarm.alarm.AlarmSchedulerBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton to hold the deeplink information when a notification is tapped
 * Accessible from both Kotlin and Swift
 */
object NotificationDeeplinkHolder {
    private val _deeplinkInfo = MutableStateFlow<String?>(null)
    val deeplinkInfo: StateFlow<String?> = _deeplinkInfo.asStateFlow()

    // Alternative function name for easier Swift access
    fun setAlarmDeeplink(json: String) {
        println("NotificationDeeplinkHolder: setAlarmDeeplink called with json = $json")
        _deeplinkInfo.value = json
    }

    fun acknowledgeDeeplink(json: String) {
        if (_deeplinkInfo.value == json) _deeplinkInfo.value = null
        AlarmSchedulerBridge.acknowledgePendingHandoff(json)
    }

    // For Swift access - the shared instance
    val shared: NotificationDeeplinkHolder get() = this
}
