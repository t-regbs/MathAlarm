package com.timilehinaregbesola.mathalarm.platform

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
fun ChallengeBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
