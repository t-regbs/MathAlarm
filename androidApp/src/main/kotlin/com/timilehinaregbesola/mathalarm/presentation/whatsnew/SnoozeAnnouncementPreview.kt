package com.timilehinaregbesola.mathalarm.presentation.whatsnew

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cafe.adriel.lyricist.strings
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.TextWithCheckbox
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing

/** A self-contained demonstration; no real alarms are changed. */
@Composable
internal fun SnoozeAnnouncementPreview() {
    var enabled by rememberSaveable { mutableStateOf(true) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            TextWithCheckbox(
                text = strings.allowSnooze,
                initialState = enabled,
                onCheckChange = { enabled = it },
            )
            Text(
                text = if (enabled) strings.snoozeSettingsSummary(5, strings.snoozeMaximumSummary(3)) else strings.snoozeOff,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
