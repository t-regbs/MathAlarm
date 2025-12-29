package com.timilehinaregbesola.mathalarm.framework.app.permission

/**
 * iOS implementation of AlarmPermission.
 * iOS doesn't require explicit exact alarm permissions - local notifications
 * handle alarm scheduling via UNUserNotificationCenter.
 */
class AlarmPermissionImpl : AlarmPermission {
    /**
     * On iOS, exact alarm permission is not required.
     * Local notifications handle the scheduling.
     *
     * @return Always returns true on iOS
     */
    override fun hasExactAlarmPermission(): Boolean = true
}
