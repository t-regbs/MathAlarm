package com.timilehinaregbesola.mathalarm.sound

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AlarmSoundCatalogTest {
    @Test
    fun librarySelectionsResolveToTheirBundledFiles() {
        for (id in AlarmSoundCatalog.sounds.map { it.id }) {
            assertEquals(id, AlarmSoundCatalog.iosResourceName(id))
            assertEquals(id, AlarmSoundCatalog.iosResourceName("$id.caf"))
            assertEquals(id, AlarmSoundCatalog.iosResourceName("$id.wav"))
        }
        assertEquals(AlarmSoundCatalog.DEFAULT_SOUND, AlarmSoundCatalog.iosResourceName(""))
    }

    @Test
    fun deviceUrisAreNotMistakenForBundledSounds() {
        assertNull(AlarmSoundCatalog.find("content://media/internal/audio/media/42"))
        assertNull(AlarmSoundCatalog.find("content://settings/system/alarm_alert"))
        assertNull(AlarmSoundCatalog.find("/somewhere/alarm_orbit.wav"))
    }

    @Test
    fun unknownIosToneUsesAnAvailableLibraryFallback() {
        assertEquals("alarm_orbit", AlarmSoundCatalog.iosResourceName("missing-tone"))
    }
}
