package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.semantics.selected
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.lyricist.strings
import com.mohamedrejeb.calf.ui.toggle.AdaptiveSwitch
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing
import com.timilehinaregbesola.mathalarm.platform.formatAlarmWeekday
import com.timilehinaregbesola.mathalarm.presentation.ui.darkPrimaryLight
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.KeyboardArrowDown
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.KeyboardArrowUp
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.Edit
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.Delete
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.Schedule
import com.timilehinaregbesola.mathalarm.utils.calculateNextAlarmTime
import com.timilehinaregbesola.mathalarm.utils.formatShortDate
import com.timilehinaregbesola.mathalarm.utils.getFormatTime
import com.timilehinaregbesola.mathalarm.utils.shouldShowNextOccurrence
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlarmItem(
    modifier: Modifier = Modifier,
    alarm: Alarm,
    onEditAlarm: () -> Unit,
    onDeleteAlarm: (Alarm) -> Unit,
    onCancelAlarm: (Alarm) -> Unit,
    onSkipNext: (Alarm) -> Unit = {},
    onUndoSkip: (Alarm) -> Unit = {},
    onScheduleAlarm: (Alarm, Boolean) -> Unit,
    darkTheme: Boolean,
    selected: Boolean = false,
) {
    var expanded by rememberSaveable(alarm.alarmId) { mutableStateOf(false) }
    val labels = strings
    val now by rememberAlarmNow()
    val clock = object : kotlin.time.Clock { override fun now() = now }
    val zone = TimeZone.currentSystemDefault()
    val nextInstant = if (alarm.isOn) calculateNextAlarmTime(alarm, zone, clock) else null
    val next = nextInstant?.toLocalDateTime(zone)
    Card(
        onClick = { expanded = !expanded },
        modifier = modifier.fillMaxWidth().padding(vertical = MaterialTheme.spacing.small)
            .semantics { this.selected = selected },
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = AlarmItemDimensions.ELEVATION),
        shape = MaterialTheme.shapes.medium.copy(CornerSize(AlarmItemDimensions.CORNER_RADIUS)),
        colors = CardDefaults.cardColors(
            containerColor = if (darkTheme) darkPrimaryLight else Color.White,
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(MaterialTheme.spacing.medium), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
            // Flow at large font sizes rather than squeezing the clock or switch.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
            ) {
                Text(
                    text = buildAnnotatedString {
                        val time = alarm.getFormatTime()
                        append(time.substringBefore(' '))
                        withStyle(SpanStyle(
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )) {
                            append(" " + time.substringAfter(' '))
                        }
                    },
                    fontSize = AlarmItemDimensions.TIME_FONT_SIZE,
                    fontWeight = if (alarm.isOn) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AdaptiveSwitch(
                        checked = alarm.isOn,
                        onCheckedChange = { enabled ->
                            if (enabled) onScheduleAlarm(alarm.copy(isOn = true), true)
                            else onCancelAlarm(alarm.copy(isOn = false))
                        },
                        modifier = Modifier.semantics { contentDescription = labels.alarms + AlarmItemDimensions.SEPARATOR + alarm.getFormatTime() },
                    )
                    IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(AlarmItemDimensions.TOUCH_TARGET)) {
                        Icon(
                            if (expanded) KeyboardArrowUp else KeyboardArrowDown,
                            contentDescription = if (expanded) labels.collapse else labels.expand,
                        )
                    }
                }
            }
            if (alarm.title.isNotBlank()) Text(alarm.title, style = MaterialTheme.typography.titleMedium)
            val dayIndices = alarm.repeatDays.mapIndexedNotNull { index, day ->
                index.takeIf { day == 'T' }
            }
            val selectedDays = dayIndices.joinToString(AlarmItemDimensions.SEPARATOR) { index ->
                formatAlarmWeekday(
                    index, labels.dateLocale, abbreviated = dayIndices.size > 1,
                )
            }
            if (selectedDays.isNotEmpty()) Text(
                text = selectedDays,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (alarm.scheduleError != null && alarm.scheduleError != Alarm.SCHEDULING_IN_PROGRESS) {
                Text(labels.alarmScheduleFailed, color = MaterialTheme.colorScheme.error)
            }
            // Reserve one typography-sized status line, even without a message.
            // Longer messages can wrap naturally as font size or language changes.
            Text(
                text = if (next != null && alarm.shouldShowNextOccurrence(zone, clock)) buildString {
                    append(labels.nextOccurrenceOn(formatShortDate(next.date.toString(), labels.dateLocale)))
                    // Only a snooze can differ from the time already displayed on this card.
                    if (nextInstant.toEpochMilliseconds() == alarm.snoozedUntil) {
                        append(AlarmItemDimensions.SEPARATOR)
                        append(next.time.toString().take(5))
                    }
                } else "",
                minLines = 1,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            alarm.skippedDate?.let { date ->
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
                ) {
                    Text(
                        labels.skippedAlarmOn(formatShortDate(date, labels.dateLocale)),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                    TextButton(onClick = { onUndoSkip(alarm) }, modifier = Modifier.heightIn(min = AlarmItemDimensions.TOUCH_TARGET),
                        contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.small)) {
                        Text(labels.undo)
                    }
                }
            }
            if (expanded) {
                val canSkipNext = alarm.canSkipNext
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                ) {
                    AlarmActionButton(
                        label = labels.edit,
                        icon = Edit,
                        onClick = onEditAlarm,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    if (canSkipNext) {
                        AlarmActionButton(
                            label = labels.skipNext,
                            icon = Schedule,
                            onClick = { onSkipNext(alarm) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                    AlarmActionButton(
                        label = labels.delete,
                        icon = Delete,
                        onClick = { onDeleteAlarm(alarm) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    // Keep the familiar three-action width even when only Edit/Delete apply.
                    // The unused space stays on the right; two actions never become wide panels.
                    if (!canSkipNext) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** One labeled button per action; the icon is decorative for screen readers. */
@Composable
private fun AlarmActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = AlarmItemDimensions.ACTION_HEIGHT),
        shape = MaterialTheme.shapes.small,
        contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.small, vertical = AlarmItemDimensions.ACTION_PADDING),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AlarmItemDimensions.ICON_LABEL_SPACING),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(AlarmItemDimensions.ICON_SIZE))
            Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    }
}

private object AlarmItemDimensions {
    const val SEPARATOR = " · "
    val TOUCH_TARGET = 48.dp
    val ACTION_HEIGHT = 72.dp
    val ICON_SIZE = 24.dp
    val ACTION_PADDING = 12.dp
    val ICON_LABEL_SPACING = 6.dp
    val ELEVATION = 4.dp
    val CORNER_RADIUS = 8.dp
    val TIME_FONT_SIZE = 40.sp
}
