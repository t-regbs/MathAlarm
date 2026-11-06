package com.timilehinaregbesola.mathalarm.utils

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import com.google.firebase.messaging.RemoteMessage
import com.timilehinaregbesola.mathalarm.BuildConfig
import com.timilehinaregbesola.mathalarm.notification.MathAlarmNotificationChannel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class)
class FirebaseMessagingServiceTest {
    private lateinit var service: FirebaseMessagingService
    private lateinit var notifications: NotificationManager

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        startKoin { modules(module { single { MathAlarmNotificationChannel(context) } }) }
        service = Robolectric.buildService(FirebaseMessagingService::class.java).create().get()
        notifications = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @After
    fun tearDown() {
        service.onDestroy()
        stopKoin()
    }

    @Test
    fun `missing update flag is ignored`() = assertIgnored(mapOf("title" to "Hello"))

    @Test
    fun `missing version is ignored`() = assertIgnored(updatePayload() - "versionCode")

    @Test
    fun `invalid update flag is ignored`() = assertIgnored(updatePayload() + ("updateApp" to "yes"))

    @Test
    fun `invalid version is ignored`() = assertIgnored(updatePayload() + ("versionCode" to "latest"))

    @Test
    fun `overflow version is ignored`() = assertIgnored(updatePayload() + ("versionCode" to "999999999999999999"))

    @Test
    fun `irrelevant and incomplete messages are ignored`() {
        assertIgnored(emptyMap())
        assertIgnored(updatePayload() + ("updateApp" to "0"))
        assertIgnored(updatePayload() - "title")
        assertIgnored(updatePayload() - "message")
    }

    @Test
    fun `current and older versions are ignored`() {
        assertIgnored(updatePayload() + ("versionCode" to BuildConfig.VERSION_CODE.toString()))
        assertIgnored(updatePayload() + ("versionCode" to (BuildConfig.VERSION_CODE - 1).toString()))
    }

    @Test
    fun `newer version posts update notification with title body and store action`() {
        receive(updatePayload())

        val notification = shadowOf(notifications).allNotifications.single()
        assertEquals("Update available", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("New version", notification.extras.getString(Notification.EXTRA_TEXT))
        assertEquals(MathAlarmNotificationChannel.UPDATE_CHANNEL_ID, notification.channelId)
        assertEquals("https://play.google.com/store/apps/details?id=${service.packageName}",
            shadowOf(notification.actions.single().actionIntent).savedIntent.dataString)
    }

    private fun assertIgnored(payload: Map<String, String>) {
        receive(payload)
        assertTrue(shadowOf(notifications).allNotifications.isEmpty())
    }

    private fun receive(payload: Map<String, String>) {
        service.onMessageReceived(RemoteMessage.Builder("test").setData(payload).build())
    }

    private fun updatePayload() = mapOf(
        "updateApp" to "1",
        "versionCode" to (BuildConfig.VERSION_CODE + 1).toString(),
        "title" to "Update available",
        "message" to "New version",
    )
}
