package com.timilehinaregbesola.mathalarm.framework.app.permission

import com.timilehinaregbesola.mathalarm.alarm.AlarmSchedulerBridge
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

/** AlarmKit authorization is required for the iOS/iPadOS 26+ release. */
class AlarmPermissionImpl : AlarmPermission {
    override fun hasExactAlarmPermission(): Boolean =
        AlarmSchedulerBridge.authorizationStatus() == "authorized"

    /**
     * A denied AlarmKit permission can be changed in the app's Settings page.
     */
    override fun openExactAlarmPermissionScreen() {
        openAppSettings()
    }

    /**
     * Opens the iOS Settings app to this app's settings page.
     */
    override fun openAppSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
        if (url != null) {
            UIApplication.sharedApplication.openURL(url)
        }
    }
}
