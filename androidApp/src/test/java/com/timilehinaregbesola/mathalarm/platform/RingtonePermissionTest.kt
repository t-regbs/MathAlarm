package com.timilehinaregbesola.mathalarm.platform

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Looper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.util.DataSource

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class, shadows = [UnavailableRingtoneTitle::class])
class RingtonePermissionTest {
    private lateinit var activity: Activity
    private val players = mutableListOf<ShadowMediaPlayer>()
    private val tone = "content://media/external/audio/media/404"

    @Before
    fun setUp() {
        UnavailableRingtoneTitle.ringtone = null
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)
        ShadowMediaPlayer.setCreateListener { _, player -> players += player }
    }

    @After
    fun tearDown() {
        activity.finish()
        stopKoin()
    }

    @Test
    fun `inaccessible tone prompts from application context wiring and requests permission on confirmation`() {
        configureContext(RuntimeEnvironment.getApplication())
        rejectTone()

        checkTone()

        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        assertNotNull("The live activity must receive the permission dialog", dialog)
        assertTrue(shadowOf(dialog).message.toString().contains(tone))
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertArrayEquals(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            shadowOf(activity).lastRequestedPermission.requestedPermissions)
        assertEquals(3, shadowOf(activity).lastRequestedPermission.requestCode)
    }

    @Test
    fun `readable tone releases probe without prompting`() {
        configureContext(activity)
        ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(activity, Uri.parse(tone)), ShadowMediaPlayer.MediaInfo())

        checkTone()

        assertEquals(1, players.size)
        assertEquals(ShadowMediaPlayer.State.END, players.single().state)
        assertNull(ShadowAlertDialog.getLatestAlertDialog())
    }

    @Test
    fun `failed probe is released and title failure retains tone in dialog`() {
        configureContext(activity)
        rejectTone()

        checkTone()

        assertEquals(1, players.size)
        assertEquals(ShadowMediaPlayer.State.END, players.single().state)
        assertTrue(shadowOf(ShadowAlertDialog.getLatestAlertDialog()).message.toString().contains(tone))
    }

    @Test
    fun `permission already granted does not probe or prompt`() {
        configureContext(activity)
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)

        checkTone()

        assertTrue(players.isEmpty())
        assertNull(ShadowAlertDialog.getLatestAlertDialog())
    }

    @Test
    @Config(sdk = [35])
    fun `Android 13 and newer do not prompt for obsolete storage permission`() {
        configureContext(activity)
        rejectTone()

        checkTone()

        assertTrue(players.isEmpty())
        assertNull(ShadowAlertDialog.getLatestAlertDialog())
        assertNull(shadowOf(activity).lastRequestedPermission)
    }

    @Test
    fun `cancel leaves permission unrequested`() {
        configureContext(activity)
        rejectTone()
        checkTone()

        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()

        assertFalse(dialog.isShowing)
        assertNull(shadowOf(activity).lastRequestedPermission)
    }

    @Test
    fun `unplayable tone title lookup releases its ringtone`() {
        configureContext(activity)
        rejectTone()
        val ringtone = mockk<Ringtone>(relaxed = true)
        every { ringtone.getTitle(activity) } returns "Selected tone"
        UnavailableRingtoneTitle.ringtone = ringtone

        checkTone()

        assertTrue(shadowOf(ShadowAlertDialog.getLatestAlertDialog()).message.toString().contains("Selected tone"))
        verify(exactly = 1) { ringtone.stop() }
    }

    @Test
    fun `failed title lookup releases its ringtone and retains URI`() {
        configureContext(activity)
        rejectTone()
        val ringtone = mockk<Ringtone>(relaxed = true)
        every { ringtone.getTitle(activity) } throws SecurityException("Denied")
        UnavailableRingtoneTitle.ringtone = ringtone

        checkTone()

        assertTrue(shadowOf(ShadowAlertDialog.getLatestAlertDialog()).message.toString().contains(tone))
        verify(exactly = 1) { ringtone.stop() }
    }

    @Test
    fun `display title lookup releases its ringtone`() {
        configureContext(activity)
        val ringtone = mockk<Ringtone>(relaxed = true)
        every { ringtone.getTitle(activity) } returns "Selected tone"
        UnavailableRingtoneTitle.ringtone = ringtone

        assertEquals("Selected tone", getRingtoneTitle(tone))

        verify(exactly = 1) { ringtone.stop() }
    }

    @Test
    fun `failed display title lookup releases its ringtone`() {
        configureContext(activity)
        val ringtone = mockk<Ringtone>(relaxed = true)
        every { ringtone.getTitle(activity) } throws SecurityException("Denied")
        UnavailableRingtoneTitle.ringtone = ringtone

        assertEquals("", getRingtoneTitle(tone))

        verify(exactly = 1) { ringtone.stop() }
    }

    private fun checkTone() {
        checkRingtonePermissions(activity, listOf(tone), "Ringtone") { "Cannot play $it" }
    }

    private fun configureContext(context: Context) {
        startKoin { modules(module { single<Context> { context } }) }
    }

    private fun rejectTone() {
        ShadowMediaPlayer.addException(DataSource.toDataSource(activity, Uri.parse(tone)), SecurityException("Denied"))
    }
}

@Implements(RingtoneManager::class)
class UnavailableRingtoneTitle {
    companion object {
        var ringtone: Ringtone? = null

        @JvmStatic
        @Implementation
        fun getRingtone(context: Context, uri: Uri): Ringtone? =
            ringtone ?: throw SecurityException("Title inaccessible")
    }
}
