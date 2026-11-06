package com.timilehinaregbesola.mathalarm.framework.app.permission

import android.os.Build
import com.timilehinaregbesola.mathalarm.fake.AndroidVersionFake
import com.timilehinaregbesola.mathalarm.fake.PermissionCheckerFake
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class AlarmPermissionTest {
    private val permissionChecker = PermissionCheckerFake()
    private val androidVersion = AndroidVersionFake()

    private lateinit var alarmPermission: AlarmPermissionImpl

    @Before
    fun setup() {
        alarmPermission = AlarmPermissionImpl(permissionChecker, androidVersion)
    }

    @Test
    fun `test if when permission is granted returns true`() {
        permissionChecker.canScheduleExactAlarms = true
        androidVersion.version = Build.VERSION_CODES.S

        val result = alarmPermission.hasExactAlarmPermission()

        Assert.assertTrue(result)
    }

    @Test
    fun `test if when permission is not granted returns false`() {
        permissionChecker.canScheduleExactAlarms = false
        androidVersion.version = Build.VERSION_CODES.S

        val result = alarmPermission.hasExactAlarmPermission()

        Assert.assertFalse(result)
    }

    @Test
    fun `test if Android below S returns true`() {
        androidVersion.version = Build.VERSION_CODES.O
        permissionChecker.canScheduleExactAlarms = false // Should be ignored

        val result = alarmPermission.hasExactAlarmPermission()

        Assert.assertTrue(result)
    }
}
