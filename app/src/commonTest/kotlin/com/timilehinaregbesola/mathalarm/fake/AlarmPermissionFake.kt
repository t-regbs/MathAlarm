package com.timilehinaregbesola.mathalarm.fake

import com.timilehinaregbesola.mathalarm.framework.app.permission.AlarmPermission

class AlarmPermissionFake(
    private var hasPermission: Boolean = true
) : AlarmPermission {
    override fun hasExactAlarmPermission(): Boolean = hasPermission
    
    fun setPermission(granted: Boolean) {
        hasPermission = granted
    }
}
