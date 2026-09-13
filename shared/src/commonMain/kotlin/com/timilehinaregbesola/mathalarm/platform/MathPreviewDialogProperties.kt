package com.timilehinaregbesola.mathalarm.platform

import androidx.compose.ui.window.DialogProperties

expect fun mathPreviewDialogProperties(): DialogProperties

@androidx.compose.runtime.Composable
expect fun ConfigureMathPreviewWindow()
