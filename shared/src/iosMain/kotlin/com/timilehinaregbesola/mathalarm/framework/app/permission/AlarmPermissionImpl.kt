package com.timilehinaregbesola.mathalarm.framework.app.permission

import com.timilehinaregbesola.mathalarm.alarm.AlarmSchedulerBridge

/** AlarmKit authorization is required for the iOS/iPadOS 26+ release. */
@OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
@kotlin.native.HiddenFromObjC
class AlarmPermissionImpl : AlarmPermission {
    override fun hasExactAlarmPermission(): Boolean =
        AlarmSchedulerBridge.authorizationStatus() == "authorized"
}
