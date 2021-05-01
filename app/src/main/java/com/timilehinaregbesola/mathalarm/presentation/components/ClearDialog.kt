package com.timilehinaregbesola.mathalarm.presentation.components

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState

@Composable
fun ClearDialog(openDialog: MutableState<Boolean>) {
    AlertDialog(
        onDismissRequest = {
            openDialog.value = false
        },
        title = {
            Text("Clear Alarms")
        },
        text = {
            Text("Are you sure you want to clear the alarms?")
        },
        confirmButton = {
            Button(onClick = { openDialog.value = false }) {
                Text("Yes")
            }
        },
        dismissButton = {
            Button(onClick = { openDialog.value = false }) {
                Text("No")
            }
        }
    )
}

@ExperimentalMaterialApi
@Composable
fun TimeLeftSnack(state: SnackbarHostState) {
    SnackbarHost(
        hostState = state,
        snackbar = { data -> Snackbar(data) }
    )
}