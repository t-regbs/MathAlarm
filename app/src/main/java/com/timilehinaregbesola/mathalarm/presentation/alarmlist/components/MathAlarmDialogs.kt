package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme

/**
 * Default dialog with confirm and dismiss button.
 *
 * @param arguments arguments to compose the dialog
 * @param isDialogOpen flag to indicate if the dialog should be open
 * @param onDismissRequest function to be called user requests to dismiss the dialog
 */
@Composable
fun MathAlarmDialog(
    arguments: DialogArguments,
    isDialogOpen: Boolean,
    onDismissRequest: () -> Unit,
) {
    if (isDialogOpen) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(text = arguments.title) },
            text = { Text(text = arguments.text) },
            confirmButton = {
                Button(onClick = arguments.onConfirmAction) {
                    Text(text = arguments.confirmText)
                }
            },
            dismissButton = {
                Button(onClick = onDismissRequest) {
                    Text(text = arguments.dismissText)
                }
            },
        )
    }
}

/**
* Arguments to be used with [MathAlarmDialog].
*
* @property title the dialog title
* @property text the dialog content text
* @property confirmText the text to be used in the confirm button
* @property dismissText the text to be used in the dismiss button
* @property onConfirmAction the action to be executed when the user confirms the dialog
*/
data class DialogArguments(
    val title: String,
    val text: String,
    val confirmText: String,
    val dismissText: String,
    val onConfirmAction: () -> Unit,
)

@Suppress("UndocumentedPublicFunction")
@Preview
@Composable
fun DialogPreview() {
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
