package com.timilehinaregbesola.mathalarm.framework.app.permission

/**
 * Abstraction for checking system permissions.
 * Extracted for better testability.
 */
interface PermissionChecker {
    /**
     * Checks if the app can schedule exact alarms.
     * @return true if exact alarms can be scheduled
     */
    fun canScheduleExactAlarms(): Boolean
}
