package com.timilehinaregbesola.mathalarm.navigation

import androidx.window.core.layout.WindowSizeClass
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsWindowLayoutTest {
    @Test fun compactAndMediumWindowsKeepOnePane() {
        for (width in listOf(360, 599, 600, 839)) {
            assertFalse(settingsWindowLayout(WindowSizeClass(width, 1200)).useTwoPanes)
        }
    }

    @Test fun expandedWidthUsesTwoPanesEvenInAShortWindow() {
        for (height in listOf(400, 800, 1200)) {
            assertTrue(settingsWindowLayout(WindowSizeClass(840, height)).useTwoPanes)
        }
    }

    @Test fun dismissActionRemainsAvailableOutsideCompactPhoneLayout() {
        assertFalse(settingsWindowLayout(WindowSizeClass(599, 1000)).showDismissButton)
        assertTrue(settingsWindowLayout(WindowSizeClass(600, 480)).showDismissButton)
        assertTrue(settingsWindowLayout(WindowSizeClass(1200, 800)).showDismissButton)
    }
}
