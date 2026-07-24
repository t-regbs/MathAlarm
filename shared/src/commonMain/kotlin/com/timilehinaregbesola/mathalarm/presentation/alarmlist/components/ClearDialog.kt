package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.lyricist.strings
import com.mohamedrejeb.calf.ui.dialog.uikit.AlertDialogIosActionStyle

@Composable
fun ClearDialog(
    modifier: Modifier = Modifier,
    openDialog: Boolean,
    onClear: () -> Unit,
    onCloseDialog: () -> Unit,
) {
    MathAlarmDialog(
        modifier = modifier,
        arguments = DialogArguments(
            title = strings.clearAlarmDialogTitle,
            text = strings.clearAlarmDialogText,
            confirmText = strings.clearAlarmDialogConfirm,
            dismissText = strings.clearAlarmDialogCancel,
            onConfirmAction = {
                onClear()
                onCloseDialog()
            },
            iosConfirmButtonStyle = AlertDialogIosActionStyle.Destructive,
        ),
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
