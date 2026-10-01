package com.timilehinaregbesola.mathalarm

import android.media.RingtoneManager
import android.app.Activity
import android.content.Intent
import androidx.core.net.toUri
import androidx.test.platform.app.InstrumentationRegistry
import com.timilehinaregbesola.mathalarm.utils.PickRingtone
import org.junit.Assert.*
import org.junit.Test

/** Verify the native picker contract without changing alarms. */
class NativeAlarmSoundPickerContractTest {
    @Test
    fun nativePickerReceivesTheExistingDeviceUriAndOffersDeviceDefault() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val existing = "content://media/internal/audio/media/42"
        val intent = PickRingtone().createIntent(context, existing)
        assertEquals(existing.toUri(), intent.getParcelableExtra<android.net.Uri>(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI))
        assertTrue(intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false))
        assertEquals(RingtoneManager.TYPE_ALARM, intent.getIntExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, 0))
        val result = Intent().putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, existing.toUri())
        assertEquals(existing.toUri(), PickRingtone().parseResult(Activity.RESULT_OK, result))
        assertNull(PickRingtone().parseResult(Activity.RESULT_CANCELED, result))
    }
}
