package com.timilehinaregbesola.mathalarm.navigation

import androidx.window.core.layout.WindowSizeClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsWindowLayoutTest {
    @Test
    fun compactWidthKeepsBottomSheetEvenWithAmpleHeight() {
        val layout = settingsWindowLayout(WindowSizeClass(599, 1000))
        assertFalse(layout.useCenteredDialog)
        assertFalse(layout.showDismissButton)
    }

    @Test
    fun shortWideWindowKeepsBottomSheetAndExplicitDismissButton() {
        val layout = settingsWindowLayout(WindowSizeClass(1000, 479))
        assertFalse(layout.useCenteredDialog)
        assertTrue(layout.showDismissButton)
    }

    @Test
    fun mediumWidthAndHeightAllowCenteredDialogAtTheBoundary() {
        assertEquals(SettingsWindowLayout(true, true), settingsWindowLayout(WindowSizeClass(600, 480)))
    }

    @Test
    fun spaciousWindowsUseSamePresentationRegardlessOfOrientation() {
        assertEquals(SettingsWindowLayout(true, true), settingsWindowLayout(WindowSizeClass(800, 1200)))
        assertEquals(SettingsWindowLayout(true, true), settingsWindowLayout(WindowSizeClass(1200, 800)))
    }
}
