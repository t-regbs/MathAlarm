package com.timilehinaregbesola.mathalarm.framework.app.permission

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.os.Build

/**
 * Android implementation of [PermissionChecker].
 */
class PermissionCheckerImpl(
    private val alarmManager: AlarmManager?
) : PermissionChecker {

    @SuppressLint("NewApi")
    override fun canScheduleExactAlarms(): Boolean {
        if (alarmManager == null) return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}
