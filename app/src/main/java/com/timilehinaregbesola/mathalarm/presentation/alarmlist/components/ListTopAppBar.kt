package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.foundation.layout.Arrangement.Center
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.ListTopAppBar.APP_BAR_TITLE
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.ListTopAppBar.LIST_TITLE_FONT_SIZE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListTopAppBar(
    openDialog: (Boolean) -> Unit,
    onSettingsClick: () -> Unit,
) {
    TopAppBar(
        modifier = Modifier.shadow(elevation = APP_BAR_TITLE),
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Center) {
                Text(text = "Alarms", fontSize = LIST_TITLE_FONT_SIZE)
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
private fun AppBarPreview() {
    ListTopAppBar(
        openDialog = {},
    ) {}
}

private object ListTopAppBar {
    val LIST_TITLE_FONT_SIZE = 16.sp
    val APP_BAR_TITLE = 4.dp
}
