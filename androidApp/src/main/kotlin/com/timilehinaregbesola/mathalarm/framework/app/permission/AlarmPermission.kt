package com.timilehinaregbesola.mathalarm.framework.app.permission

import android.os.Build

/**
 * Android implementation of [AlarmPermission] using abstracted dependencies.
 */
class AlarmPermissionImpl(
    private val screenNavigator: ScreenNavigator,
    private val permissionChecker: PermissionChecker,
    private val androidVersion: AndroidVersion
) : AlarmPermission {

    /**
     * Verifies if the permission [android.Manifest.permission.SCHEDULE_EXACT_ALARM] is granted.
     *
     * @return `true` if the permission is granted, `false` otherwise
     */
    override fun hasExactAlarmPermission(): Boolean {
        return if (androidVersion.currentVersion >= Build.VERSION_CODES.S) {
            permissionChecker.canScheduleExactAlarms()
        } else {
            true
        }
    }

    override fun openExactAlarmPermissionScreen() {
        screenNavigator.openExactAlarmPermissionScreen()
    }

    override fun openAppSettings() {
        screenNavigator.openAppSettings()
    }
}
