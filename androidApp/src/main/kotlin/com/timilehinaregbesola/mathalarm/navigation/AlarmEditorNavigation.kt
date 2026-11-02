package com.timilehinaregbesola.mathalarm.navigation

import androidx.navigation3.runtime.NavKey
import com.timilehinaregbesola.mathalarm.utils.Destinations.SettingsSheet

/** An outgoing editor's asynchronous save must not dismiss a newly selected tablet pane. */
internal fun closeCurrentAlarmEditor(backStack: MutableList<NavKey>, editor: SettingsSheet): Boolean {
    if (backStack.lastOrNull() !== editor) return false
    backStack.removeAt(backStack.lastIndex)
    return true
}
