package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.material.*
import androidx.compose.runtime.Composable

@Composable
fun ClearDialog(
    openDialog: Boolean,
    onClear: () -> Unit,
    onCloseDialog: () -> Unit,
) {
    val arguments = DialogArguments(
        title = "Clear Alarms",
        text = "Are you sure you want to clear the alarms?",
        confirmText = "Yes",
        dismissText = "No",
        onConfirmAction = {
            onClear()
            onCloseDialog()
        },
    )
    MathAlarmDialog(
        arguments = arguments,
        isDialogOpen = openDialog,
        onDismissRequest = onCloseDialog,
    )
}

@ExperimentalMaterialApi
@Composable
fun AlarmSnack(state: SnackbarHostState) {
    SnackbarHost(
        hostState = state,
        snackbar = { data -> Snackbar(snackbarData = data) },
    )
}
