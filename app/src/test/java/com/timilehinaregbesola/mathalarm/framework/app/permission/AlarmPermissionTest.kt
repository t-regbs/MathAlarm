package com.timilehinaregbesola.mathalarm.framework.app.permission

import android.app.AlarmManager
import android.os.Build
import com.timilehinaregbesola.mathalarm.fake.AndroidVersionFake
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test

class AlarmPermissionTest {
    private val alarmManager = mockk<AlarmManager>(relaxed = true)

    private val androidVersion = AndroidVersionFake()

    private val alarmPermission = AlarmPermission(alarmManager, androidVersion)

    @Test
    fun `test if when permission is granted returns true`() {
        every { alarmManager.canScheduleExactAlarms() } returns true
        androidVersion.version = Build.VERSION_CODES.S

        val result = alarmPermission.hasExactAlarmPermission()

        Assert.assertTrue(result)
    }

    @Test
    fun `test if when permission is not granted returns false`() {
        every { alarmManager.canScheduleExactAlarms() } returns false
        androidVersion.version = Build.VERSION_CODES.S

        val result = alarmPermission.hasExactAlarmPermission()

        Assert.assertFalse(result)
    }

    @Test
    fun `test if Android below S returns true`() {
        androidVersion.version = Build.VERSION_CODES.M

        val result = alarmPermission.hasExactAlarmPermission()

        Assert.assertTrue(result)
    }
}
