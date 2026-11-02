package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Instant

/** Refresh time-dependent labels and expire skip actions while the screen stays open. */
@Composable
internal fun rememberAlarmNow(): State<Instant> = produceState(Clock.System.now()) {
    while (true) {
        value = Clock.System.now()
        delay(1_000)
    }
}
