package com.timilehinaregbesola.mathalarm.framework.app.permission

/**
 * Abstraction for navigating to system screens.
 * Extracted for better testability.
 */
interface ScreenNavigator {
    /**
     * Opens the exact alarm permission screen (Android S+).
     */
    fun openExactAlarmPermissionScreen()

    /**
     * Opens the app settings screen.
     */
    fun openAppSettings()
}
