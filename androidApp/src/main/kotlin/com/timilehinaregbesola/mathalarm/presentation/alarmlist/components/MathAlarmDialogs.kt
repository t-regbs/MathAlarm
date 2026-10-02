package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import androidx.compose.ui.tooling.preview.Preview
import com.mohamedrejeb.calf.ui.button.AdaptiveButton as Button
import com.mohamedrejeb.calf.ui.ExperimentalCalfUiApi
import com.mohamedrejeb.calf.ui.dialog.AdaptiveBasicAlertDialog
import com.mohamedrejeb.calf.ui.dialog.uikit.AlertDialogIosAction
import com.mohamedrejeb.calf.ui.dialog.uikit.AlertDialogIosActionStyle
import com.mohamedrejeb.calf.ui.dialog.uikit.rememberAlertDialogIosProperties

/**
 * Default dialog with confirm and dismiss button.
 *
 * @param arguments arguments to compose the dialog
 * @param isDialogOpen flag to indicate if the dialog should be open
 * @param onDismissRequest function to be called user requests to dismiss the dialog
 */
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
            AdaptiveBasicAlertDialog(
                modifier = modifier,
                onDismissRequest = onDismissRequest,
                iosProperties = rememberAlertDialogIosProperties(
                    title = title.orEmpty(),
                    text = text,
                    actions = buildList {
                        add(
                            AlertDialogIosAction(
                                title = confirmText,
                                style = iosConfirmButtonStyle,
                                onClick = onConfirmAction,
                            )
                        )
                        dismissText?.let {
                            add(
                                AlertDialogIosAction(
                                    title = it,
                                    style = AlertDialogIosActionStyle.Cancel,
                                    onClick = onDismissRequest,
                                )
                            )
                        }
                    },
                ),
                materialContent = {
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
            )
        }
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
    val title: String?,
    val text: String,
    val confirmText: String,
    val dismissText: String?,
    val onConfirmAction: () -> Unit,
    val iosConfirmButtonStyle: AlertDialogIosActionStyle = AlertDialogIosActionStyle.Default,
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
