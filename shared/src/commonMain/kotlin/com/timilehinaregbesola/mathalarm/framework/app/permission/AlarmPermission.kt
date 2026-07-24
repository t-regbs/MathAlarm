package com.timilehinaregbesola.mathalarm.framework.app.permission

/**
 * Platform-abstracted interface for checking and managing alarm permissions.
 */
interface AlarmPermission {
    /**
     * Verifies if the permission to schedule exact alarms is granted.
     *
     * @return `true` if the permission is granted, `false` otherwise
     */
    fun hasExactAlarmPermission(): Boolean

    /**
     * Opens the system settings screen for exact alarm permission.
     * On Android S+, this opens the SCHEDULE_EXACT_ALARM permission screen.
     */
    fun openExactAlarmPermissionScreen()

    /**
     * Opens the app settings screen.
     * Useful for directing users to manually enable permissions.
     */
    fun openAppSettings()
}
