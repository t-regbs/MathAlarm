package com.timilehinaregbesola.mathalarm.navigation

import androidx.navigation3.runtime.NavKey
import com.timilehinaregbesola.mathalarm.utils.Destinations.AlarmList
import com.timilehinaregbesola.mathalarm.utils.Destinations.AppSettings
import com.timilehinaregbesola.mathalarm.utils.Destinations.SettingsSheet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AlarmEditorNavigationTest {
    @Test
    fun savingCurrentEditorReturnsToListOnEitherLayout() {
        val editor = SettingsSheet("first")
        val stack = mutableListOf<NavKey>(AlarmList, editor)
        assertTrue(closeCurrentAlarmEditor(stack, editor))
        assertEquals(listOf<NavKey>(AlarmList), stack)
        assertFalse(closeCurrentAlarmEditor(stack, editor))
    }

    @Test
    fun delayedSaveCannotCloseAnotherTabletEditor() {
        val outgoing = SettingsSheet("first")
        val selected = SettingsSheet("second")
        val stack = mutableListOf<NavKey>(AlarmList, selected)
        assertFalse(closeCurrentAlarmEditor(stack, outgoing))
        assertEquals(listOf<NavKey>(AlarmList, selected), stack)
    }

    @Test
    fun delayedSaveCannotCloseReopenedCopyOfSameAlarm() {
        val outgoing = SettingsSheet("same")
        val reopened = outgoing.copy()
        val stack = mutableListOf<NavKey>(AlarmList, reopened)
        assertFalse(closeCurrentAlarmEditor(stack, outgoing))
        assertEquals(2, stack.size)
    }

    @Test
    fun hiddenEditorCannotDismissSettingsOrOfferReview() {
        val editor = SettingsSheet("first")
        val stack = mutableListOf<NavKey>(AlarmList, editor, AppSettings)
        assertFalse(closeCurrentAlarmEditor(stack, editor))
        assertEquals(listOf<NavKey>(AlarmList, editor, AppSettings), stack)
    }
}
