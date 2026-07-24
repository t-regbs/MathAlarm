package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.foundation.layout.Arrangement.Center
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.lyricist.strings
import com.mohamedrejeb.calf.ui.button.AdaptiveIconButton
import com.mohamedrejeb.calf.ui.ExperimentalCalfUiApi
import com.mohamedrejeb.calf.ui.navigation.AdaptiveTopBar
import com.mohamedrejeb.calf.ui.navigation.UIKitUIBarButtonItem
import com.mohamedrejeb.calf.ui.uikit.UIKitImage
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.ListTopAppBar.APP_BAR_TITLE
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.ListTopAppBar.LIST_TITLE_FONT_SIZE
import androidx.compose.ui.tooling.preview.Preview
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.DeleteSweep
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.Settings

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCalfUiApi::class)
@Composable
fun ListTopAppBar(
    modifier: Modifier = Modifier,
    openDialog: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    AdaptiveTopBar(
        modifier = Modifier
            .shadow(elevation = APP_BAR_TITLE)
            .then(modifier),
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Center) {
                Text(text = strings.alarms, fontSize = LIST_TITLE_FONT_SIZE)
            }
        },
        actions = {
            AdaptiveIconButton(onClick = { openDialog() }) {
                Icon(imageVector = DeleteSweep, contentDescription = null)
            }
            AdaptiveIconButton(onClick = onSettingsClick) {
                Icon(imageVector = Settings, contentDescription = "Settings")
            }
        },
        iosTitle = strings.alarms,
        iosTrailingItems = listOf(
            UIKitUIBarButtonItem(
                image = UIKitImage.SystemName("xmark.bin"),
                onClick = { openDialog() },
            ),
            UIKitUIBarButtonItem(
                image = UIKitImage.SystemName("gearshape"),
                onClick = onSettingsClick,
            ),
        ),
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
