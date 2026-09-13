package com.timilehinaregbesola.mathalarm.platform

import androidx.compose.ui.window.DialogProperties

actual fun mathPreviewDialogProperties() = DialogProperties(
    usePlatformDefaultWidth = false,
    dismissOnBackPress = false,
    dismissOnClickOutside = false,
)

@androidx.compose.runtime.Composable
actual fun ConfigureMathPreviewWindow() = Unit
