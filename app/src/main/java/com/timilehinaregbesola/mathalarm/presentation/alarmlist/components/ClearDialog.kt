package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import cafe.adriel.lyricist.strings

@Composable
fun ClearDialog(
    openDialog: Boolean,
    onClear: () -> Unit,
    onCloseDialog: () -> Unit,
) {
    val arguments = DialogArguments(
        title = strings.clearAlarmDialogTitle,
        text = strings.clearAlarmDialogText,
        confirmText = strings.clearAlarmDialogConfirm,
        dismissText = strings.clearAlarmDialogCancel,
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

@ExperimentalMaterial3Api
@Composable
fun AlarmSnack(state: SnackbarHostState) {
    SnackbarHost(
        hostState = state,
        snackbar = { data -> Snackbar(snackbarData = data) },
    )
}
