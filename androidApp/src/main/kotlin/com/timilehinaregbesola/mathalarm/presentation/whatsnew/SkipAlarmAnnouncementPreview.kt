package com.timilehinaregbesola.mathalarm.presentation.whatsnew

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cafe.adriel.lyricist.strings
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing
import com.timilehinaregbesola.mathalarm.utils.formatShortDate
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/** A self-contained demonstration; no real alarms are changed. */
@Composable
internal fun SkipAlarmAnnouncementPreview() {
    var skipped by rememberSaveable { mutableStateOf(false) }
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val nextDate = (today + DatePeriod(days = 1)).toString()
    val followingDate = (today + DatePeriod(days = 2)).toString()
    val labels = strings
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            Text(labels.skipAnnouncementExample, style = MaterialTheme.typography.labelMedium)
            Text(
                labels.nextOccurrenceOn(formatShortDate(if (skipped) followingDate else nextDate, labels.dateLocale)),
                style = MaterialTheme.typography.titleMedium,
            )
            if (skipped) {
                Text(
                    labels.skippedAlarmOn(formatShortDate(nextDate, labels.dateLocale)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { skipped = !skipped }) {
                Text(if (skipped) labels.undoSkip else labels.skipNext)
            }
        }
    }
}
