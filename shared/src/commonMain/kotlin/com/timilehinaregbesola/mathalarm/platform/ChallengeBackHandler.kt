package com.timilehinaregbesola.mathalarm.platform

import androidx.compose.runtime.Composable

@Composable
expect fun ChallengeBackHandler(enabled: Boolean, onBack: () -> Unit)
