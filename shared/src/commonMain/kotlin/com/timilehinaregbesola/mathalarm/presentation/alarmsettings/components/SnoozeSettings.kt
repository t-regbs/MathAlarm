package com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.strings
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.KeyboardArrowDown
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing

@Composable
internal fun SnoozeSettings(
    enabled: Boolean,
    maxSnoozes: Int,
    snoozeMinutes: Int,
    onEnabledChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        Row(
            Modifier.weight(1f).heightIn(min = 48.dp)
                .toggleable(value = enabled, role = Role.Checkbox, onValueChange = onEnabledChange),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        ) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Checkbox(checked = enabled, onCheckedChange = null)
            }
            Column(Modifier.weight(1f)) {
                Text(strings.snooze, style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (!enabled) strings.snoozeOff else strings.snoozeSettingsSummary(
                        snoozeMinutes,
                        if (maxSnoozes == 0) strings.unlimitedSnoozes else strings.snoozeMaximumSummary(maxSnoozes),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (enabled) {
            TextButton(
                onClick = onEdit,
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
            ) {
                Text(strings.edit, style = MaterialTheme.typography.titleMedium, textDecoration = TextDecoration.Underline)
            }
        }
    }
}

@Composable
internal fun SnoozeEditor(
    initialEnabled: Boolean,
    initialMinutes: Int,
    initialMaximum: Int,
    onDismiss: () -> Unit,
    onApply: (Boolean, Int, Int) -> Unit,
) {
    var enabled by rememberSaveable { mutableStateOf(initialEnabled) }
    var minutesText by rememberSaveable { mutableStateOf(initialMinutes.coerceIn(1, 30).toString()) }
    val minutes = minutesText.toIntOrNull()
    val validDuration = minutes != null && minutes in 1..30
    var maximum by rememberSaveable { mutableStateOf(initialMaximum) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 640.dp).fillMaxSize().padding(
                    horizontal = MaterialTheme.spacing.extraMedium,
                    vertical = MaterialTheme.spacing.medium,
                ),
            ) {
                Text(
                    strings.snooze,
                    modifier = Modifier.fillMaxWidth().padding(bottom = MaterialTheme.spacing.medium),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                ) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .toggleable(value = enabled, role = Role.Checkbox, onValueChange = { enabled = it }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = enabled, onCheckedChange = null)
                        Text(strings.allowSnooze, Modifier.padding(start = MaterialTheme.spacing.small))
                    }
                    if (enabled) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(strings.snoozeDuration, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                            CountStepper(
                                value = minutesText,
                                label = strings.snoozeDuration,
                                onValueChange = { value ->
                                    val number = value.toIntOrNull()
                                    if (value.isEmpty() || (value.length <= 2 && number != null && number in 0..30)) {
                                        minutesText = value
                                    }
                                },
                                readOnly = false,
                                isError = !validDuration,
                                canDecrease = minutes != null && minutes > 1,
                                canIncrease = minutes == null || minutes < 30,
                                onDecrease = { minutesText = ((minutes ?: 1) - 1).coerceAtLeast(1).toString() },
                                onIncrease = { minutesText = ((minutes ?: 0) + 1).coerceAtMost(30).toString() },
                            )
                        }
                        Text(
                            strings.snoozeDurationHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Box {
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { expanded = true },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                            ) {
                                Text(strings.maximumSnoozes, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                                Text(if (maximum == 0) strings.unlimitedSnoozes else maximum.toString())
                                Icon(KeyboardArrowDown, contentDescription = strings.chooseRange)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                listOf(1, 2, 3, 5, 0).forEach { count ->
                                    DropdownMenuItem(
                                        text = { Text(if (count == 0) strings.unlimitedSnoozes else count.toString()) },
                                        onClick = { maximum = count; expanded = false },
                                    )
                                }
                            }
                        }
                        if (maximum > 0 && validDuration) {
                            Text(
                                strings.snoozeLimitSummary(maximum, maximum * minutes!!),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                SheetFooter(
                    secondaryLabel = strings.cancel.uppercase(),
                    primaryLabel = strings.applySnooze.uppercase(),
                    onSecondaryClick = onDismiss,
                    onPrimaryClick = { onApply(enabled, minutes?.takeIf { validDuration } ?: initialMinutes.coerceIn(1, 30), maximum) },
                    primaryEnabled = !enabled || validDuration,
                )
            }
        }
    }
}
