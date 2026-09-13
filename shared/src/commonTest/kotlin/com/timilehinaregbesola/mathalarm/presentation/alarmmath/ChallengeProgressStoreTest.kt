package com.timilehinaregbesola.mathalarm.presentation.alarmmath

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChallengeProgressStoreTest {
    @Test
    fun simultaneousAlarmsAndNewOccurrencesStayIndependent() {
        val store = ChallengeProgressStore(MapSettings())
        val progress = ChallengeProgressStore.Progress(1000, listOf(MathProblem()), 0)
        store.save(1, progress)
        store.save(2, progress.copy(activeAt = 2000))
        assertEquals(progress, store.load(1, 1000))
        assertNull(store.load(1, 2000))
        store.clear(2, 1000) // An old screen cannot clear a newer occurrence.
        assertEquals(2000L, store.load(2, 2000)?.activeAt)
        store.clear(1, 1000)
        assertNull(store.load(1, 1000))
        assertEquals(2000L, store.load(2, 2000)?.activeAt)
    }

    @Test
    fun corruptOrInvalidProgressIsIgnored() {
        val settings = MapSettings()
        val store = ChallengeProgressStore(settings)
        settings.putString("mathalarm_challenge_progress_1", "invalid json")
        assertNull(store.load(1, 1000))
        store.save(1, ChallengeProgressStore.Progress(1000, listOf(MathProblem()), 5))
        assertNull(store.load(1, 1000))
    }
}
