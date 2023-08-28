package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.timilehinaregbesola.mathalarm.R
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AddAlarmFab.FAB_BACKGROUND_COLOR
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AddAlarmFab.FAB_IMAGE_SIZE

@ExperimentalMaterialApi
@Composable
fun AddAlarmFab(
    modifier: Modifier = Modifier,
    fabImage: Painter,
    onClick: () -> Unit,
) {
    FloatingActionButton(
        modifier = modifier,
        onClick = onClick,
        backgroundColor = Color(FAB_BACKGROUND_COLOR),
    ) {
        Image(
            modifier = Modifier
                .size(FAB_IMAGE_SIZE),
            painter = fabImage,
            contentDescription = null,
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@Preview
private fun AddAlarmFabPreview() {
    MaterialTheme {
        AddAlarmFab(fabImage = painterResource(id = R.drawable.fabb)) {}
    }
}

private object AddAlarmFab {
    const val FAB_BACKGROUND_COLOR = 0x482FF7
    val FAB_IMAGE_SIZE = 72.dp
}
