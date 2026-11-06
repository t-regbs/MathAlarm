package com.timilehinaregbesola.mathalarm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.timilehinaregbesola.mathalarm.platform.checkRingtonePermissions
import com.timilehinaregbesola.mathalarm.presentation.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

@OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    kotlinx.coroutines.InternalCoroutinesApi::class,
    androidx.compose.animation.ExperimentalAnimationApi::class,
)
class RingtonePermissionDeviceTest {
    @Test
    fun inaccessibleToneCanRequestAndReceiveStoragePermission() {
        assumeTrue(Build.VERSION.SDK_INT in 26..32)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assertEquals(PackageManager.PERMISSION_DENIED,
            context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE))

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                checkRingtonePermissions(
                    activity,
                    listOf("content://com.mathalarm.missing/audio/404"),
                    "Ringtone access check",
                ) { "Cannot open $it" }
            }
            onView(withText("Ringtone access check")).check(matches(isDisplayed()))
            onView(withText(android.R.string.ok)).perform(click())

            val automation = instrumentation.uiAutomation
            val deadline = SystemClock.uptimeMillis() + 10_000
            var allow: AccessibilityNodeInfo? = null
            while (allow == null && SystemClock.uptimeMillis() < deadline) {
                allow = automation.rootInActiveWindow?.findAccessibilityNodeInfosByViewId(
                    "com.android.permissioncontroller:id/permission_allow_button"
                )?.firstOrNull()
                if (allow == null) SystemClock.sleep(100)
            }
            assertNotNull("Android must display the storage permission prompt", allow)
            assertTrue(allow!!.performAction(AccessibilityNodeInfo.ACTION_CLICK))

            while (context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED && SystemClock.uptimeMillis() < deadline) {
                SystemClock.sleep(100)
            }
            assertEquals(PackageManager.PERMISSION_GRANTED,
                context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }
}
