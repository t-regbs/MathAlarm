package com.timilehinaregbesola.mathalarm.fake

import com.timilehinaregbesola.mathalarm.framework.app.permission.ScreenNavigator

/**
 * Fake implementation of ScreenNavigator for testing.
 */
class ScreenNavigatorFake : ScreenNavigator {
    var exactAlarmPermissionScreenOpened = false
        private set
    var appSettingsOpened = false
        private set

    override fun openExactAlarmPermissionScreen() {
        exactAlarmPermissionScreenOpened = true
    }

    override fun openAppSettings() {
        appSettingsOpened = true
    }

    fun reset() {
        exactAlarmPermissionScreenOpened = false
        appSettingsOpened = false
    }
}
