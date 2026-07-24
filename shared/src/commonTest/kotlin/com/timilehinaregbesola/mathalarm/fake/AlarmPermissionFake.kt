package com.timilehinaregbesola.mathalarm.fake

import com.timilehinaregbesola.mathalarm.framework.app.permission.AlarmPermission

class AlarmPermissionFake(
    private var hasPermission: Boolean = true
) : AlarmPermission {
    var exactAlarmPermissionScreenOpened = false
        private set
    var appSettingsOpened = false
        private set

    override fun hasExactAlarmPermission(): Boolean = hasPermission
    
    override fun openExactAlarmPermissionScreen() {
        exactAlarmPermissionScreenOpened = true
    }

    override fun openAppSettings() {
        appSettingsOpened = true
    }
    
    fun setPermission(granted: Boolean) {
        hasPermission = granted
    }

    fun reset() {
        exactAlarmPermissionScreenOpened = false
        appSettingsOpened = false
    }
}
