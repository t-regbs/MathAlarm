package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mohamedrejeb.calf.ui.ExperimentalCalfUiApi
import com.mohamedrejeb.calf.ui.button.AdaptiveButton as Button
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme

@OptIn(ExperimentalCalfUiApi::class)
@Composable
fun MathAlarmDialog(
    modifier: Modifier = Modifier,
    arguments: DialogArguments,
    isDialogOpen: Boolean,
    onDismissRequest: () -> Unit,
) {
    if (isDialogOpen) {
        with(arguments) {
            AlertDialog(
                modifier = modifier,
                onDismissRequest = onDismissRequest,
                title = title?.let { { Text(text = it) } },
                text = { Text(text = text) },
                confirmButton = {
                    Button(onClick = onConfirmAction) {
                        Text(text = confirmText)
                    }
                },
                dismissButton = dismissText?.let {
                    {
                        Button(onClick = onDismissRequest) {
                            Text(text = it)
                        }
                    }
                },
            )
        }
    }
}

data class DialogArguments(
    val title: String?,
    val text: String,
    val confirmText: String,
    val dismissText: String?,
    val onConfirmAction: () -> Unit,
)

@Preview
@Composable
private fun DialogPreview() {
    MathAlarmTheme {
        val arguments = DialogArguments(
            title = "Something just happened",
            text = "Are you sure that you want to let something happen?",
            confirmText = "Alright",
            dismissText = "Cancel",
            onConfirmAction = {},
        )

        MathAlarmDialog(arguments = arguments, isDialogOpen = true, onDismissRequest = {})
    }
}
