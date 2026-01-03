package com.timilehinaregbesola.mathalarm.fake

import com.timilehinaregbesola.mathalarm.framework.app.permission.PermissionChecker

/**
 * Fake implementation of PermissionChecker for testing.
 */
class PermissionCheckerFake(
    var canScheduleExactAlarms: Boolean = true
) : PermissionChecker {
    override fun canScheduleExactAlarms(): Boolean = canScheduleExactAlarms
}
