package com.timilehinaregbesola.mathalarm.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class OverlaySceneStrategyTest {
    private val sheetStrategy = BottomSheetSceneStrategy<String>()
    private val previewStrategy = MathPreviewSceneStrategy<String>()
    private val list = NavEntry("list") {}
    private val settings = NavEntry("settings", metadata = BottomSheetSceneStrategy.bottomSheet()) {}
    private val preview = NavEntry("preview", metadata = MathPreviewSceneStrategy.metadata()) {}

    @Test
    fun closingSettingsReturnsToExactlyTheUnderlyingStack() {
        val stack = listOf(list, NavEntry("another page") {}, settings)
        val scene = assertIs<OverlayScene<String>>(sheetStrategy.scene(stack))
        assertEquals(stack.dropLast(1), scene.previousEntries)
        assertEquals(scene.previousEntries, scene.overlaidEntries)
    }

    @Test
    fun testAlarmRetainsSettingsAndOnlyPreviewStrategyClaimsIt() {
        val stack = listOf(list, settings, preview)
        assertNull(sheetStrategy.scene(stack))
        val scene = assertIs<OverlayScene<String>>(previewStrategy.scene(stack))
        assertEquals(listOf(list, settings), scene.overlaidEntries)
        assertEquals(scene.overlaidEntries, scene.previousEntries)
        assertIs<BottomSheetScene<String>>(sheetStrategy.scene(scene.overlaidEntries))
    }

    @Test
    fun overlaysRequireAnUnderlyingDestination() {
        assertNull(sheetStrategy.scene(listOf(settings)))
        assertNull(previewStrategy.scene(listOf(preview)))
        assertNull(previewStrategy.scene(listOf(list, settings)))
    }

    @Test
    fun refreshedNavigationCallbackDoesNotReplaceTheEditorScene() {
        fun scene(onBack: () -> Unit) = BottomSheetScene(
            key = settings.contentKey,
            previousEntries = listOf(list),
            overlaidEntries = listOf(list),
            bottomSheetEntry = settings,
            onBack = onBack,
        )
        val original = scene { error("Original callback") }
        val refreshed = scene { error("Refreshed callback") }
        assertEquals(original, refreshed)
        assertEquals(original.hashCode(), refreshed.hashCode())
    }

    private fun SceneStrategy<String>.scene(entries: List<NavEntry<String>>) =
        with(this) { SceneStrategyScope<String>().calculateScene(entries) }
}
