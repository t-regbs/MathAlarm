package com.timilehinaregbesola.mathalarm.framework.app.permission

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

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

    /**
     * On iOS, opens the app settings screen.
     * iOS doesn't have a separate exact alarm permission screen.
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
