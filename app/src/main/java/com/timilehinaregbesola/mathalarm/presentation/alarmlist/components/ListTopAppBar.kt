package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.foundation.layout.Arrangement.Center
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.ListTopAppBar.ListTitleFontSize

@Composable
fun ListTopAppBar(
    openDialog: (Boolean) -> Unit,
    onSettingsClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Center) {
                Text(text = "Alarms", fontSize = ListTitleFontSize)
            }
        },
        actions = {
            IconButton(onClick = { openDialog(true) }) {
                Icon(imageVector = Icons.Outlined.DeleteSweep, contentDescription = null)
            }
            IconButton(onClick = onSettingsClick) {
                Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings")
            }
        },
    )
}

@Preview
@Composable
fun AppBarPreview() {
    ListTopAppBar(
        openDialog = {},
    ) {}
}

private object ListTopAppBar {
    val ListTitleFontSize = 16.sp
}
