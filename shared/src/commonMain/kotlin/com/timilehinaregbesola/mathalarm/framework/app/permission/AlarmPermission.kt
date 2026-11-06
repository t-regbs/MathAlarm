package com.timilehinaregbesola.mathalarm.framework.app.permission

/**
 * Platform-abstracted interface for checking alarm permissions.
 */
@OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
@kotlin.native.HiddenFromObjC
interface AlarmPermission {
    /**
     * Verifies if the permission to schedule exact alarms is granted.
     *
     * @return `true` if the permission is granted, `false` otherwise
     */
    fun hasExactAlarmPermission(): Boolean
}
