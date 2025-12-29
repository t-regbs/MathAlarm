package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.lyricist.strings

@Composable
fun ClearDialog(
    modifier: Modifier = Modifier,
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
        modifier = modifier,
        arguments = arguments,
        isDialogOpen = openDialog,
        onDismissRequest = onCloseDialog,
    )
}

@ExperimentalMaterial3Api
@Composable
fun AlarmSnack(modifier: Modifier = Modifier, state: SnackbarHostState) {
    SnackbarHost(
        modifier = modifier,
        hostState = state,
        snackbar = { data -> Snackbar(snackbarData = data) },
    )
}
