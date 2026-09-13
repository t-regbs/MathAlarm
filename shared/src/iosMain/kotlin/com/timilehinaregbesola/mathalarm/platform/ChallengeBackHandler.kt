package com.timilehinaregbesola.mathalarm.platform

import androidx.compose.runtime.Composable

@Composable
actual fun ChallengeBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS has no system Back action; the sheet provides its own controls.
}
