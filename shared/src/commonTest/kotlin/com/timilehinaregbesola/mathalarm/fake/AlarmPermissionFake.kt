package com.timilehinaregbesola.mathalarm.fake

import com.timilehinaregbesola.mathalarm.framework.app.permission.AlarmPermission

@OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
@kotlin.native.HiddenFromObjC
class AlarmPermissionFake(
    private var hasPermission: Boolean = true
) : AlarmPermission {
    override fun hasExactAlarmPermission(): Boolean = hasPermission
    
    fun setPermission(granted: Boolean) {
        hasPermission = granted
    }
}
