package com.timilehinaregbesola.mathalarm.navigation

import androidx.window.core.layout.WindowSizeClass

internal data class SettingsWindowLayout(
    val showDismissButton: Boolean,
    val useCenteredDialog: Boolean,
)

/** Use the app window for presentation decisions; container constraints size the content itself. */
internal fun settingsWindowLayout(windowSizeClass: WindowSizeClass): SettingsWindowLayout {
    val hasMediumWidth = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    val hasMediumHeight = windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
    return SettingsWindowLayout(
        showDismissButton = hasMediumWidth,
        useCenteredDialog = hasMediumWidth && hasMediumHeight,
    )
}
