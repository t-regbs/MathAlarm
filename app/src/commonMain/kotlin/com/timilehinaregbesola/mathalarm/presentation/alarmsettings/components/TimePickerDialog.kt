package com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import cafe.adriel.lyricist.strings
import com.mohamedrejeb.calf.ui.timepicker.AdaptiveTimePicker
import com.mohamedrejeb.calf.ui.timepicker.AdaptiveTimePickerState
import com.mohamedrejeb.calf.ui.timepicker.rememberAdaptiveTimePickerState
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import com.timilehinaregbesola.mathalarm.presentation.ui.darkPrimary
import kotlinx.datetime.LocalTime
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    timeState: AdaptiveTimePickerState,
    onCancel: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = false,
) {

    fun onConfirmClicked() {
        val currentTime = LocalTime(timeState.hour, timeState.minute)
        onConfirm(currentTime)
    }

    // TimePicker does not provide a default TimePickerDialog, so we use our own PickerDialog:
    // https://issuetracker.google.com/issues/288311426
    PickerDialog(
        modifier = modifier,
        onDismissRequest = onCancel,
        darkTheme = darkTheme,
        title = { Text(strings.selectHour) },
        buttons = {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onCancel) {
                Text(
                    text = strings.cancel,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            TextButton(onClick = ::onConfirmClicked) {
                Text(
                    text = strings.ok,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
    ) {
        val contentModifier = Modifier.padding(horizontal = 24.dp)
        AdaptiveTimePicker(
            modifier = contentModifier,
            state = timeState,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    darkTheme: Boolean,
    buttons: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    BasicAlertDialog(
        modifier = modifier
            .width(IntrinsicSize.Min)
            .height(IntrinsicSize.Min),
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = if (darkTheme) darkPrimary else Color.LightGray,
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Title
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                    ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(horizontal = 24.dp)
                                .padding(top = 16.dp, bottom = 20.dp),
                        ) {
                            title()
                        }
                    }
                }
                // Content
                CompositionLocalProvider(LocalContentColor provides AlertDialogDefaults.textContentColor) {
                    content()
                }
                // Buttons
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                    ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                        // TODO This should wrap on small screens, but we can't use AlertDialogFlowRow as it is no public
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp, end = 6.dp, start = 6.dp),
                            horizontalArrangement = spacedBy(8.dp, End),
                        ) {
                            buttons()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun TimePickerDialogPreview() {
    MathAlarmTheme(darkTheme = false) {
        TimePickerDialog(
            timeState = rememberAdaptiveTimePickerState(),
            onCancel = {},
            onConfirm = {}
        )
    }

}
