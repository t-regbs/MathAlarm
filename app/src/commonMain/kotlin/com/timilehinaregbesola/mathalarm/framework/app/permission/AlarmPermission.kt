package com.timilehinaregbesola.mathalarm.framework.app.permission

/**
 * Platform-abstracted interface for checking alarm permissions.
 */
interface AlarmPermission {
    /**
     * Verifies if the permission to schedule exact alarms is granted.
     *
     * @return `true` if the permission is granted, `false` otherwise
     */
    fun hasExactAlarmPermission(): Boolean
}
