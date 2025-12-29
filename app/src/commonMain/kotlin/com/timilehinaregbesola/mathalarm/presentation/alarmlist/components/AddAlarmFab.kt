package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.foundation.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AddAlarmFab.FAB_BACKGROUND_COLOR
import com.timilehinaregbesola.mathalarm.presentation.ui.fabShape
import mathalarm.app.generated.resources.Res
import mathalarm.app.generated.resources.fab_icon
import org.jetbrains.compose.resources.painterResource

@ExperimentalMaterial3Api
@Composable
fun AddAlarmFab(
    modifier: Modifier = Modifier,
    fabImage: Painter,
    onClick: () -> Unit,
) {
    FloatingActionButton(
        modifier = modifier,
        onClick = onClick,
        shape = fabShape,
        containerColor = Color(FAB_BACKGROUND_COLOR),
    ) {
        Image(
            painter = fabImage,
            contentDescription = null,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
private fun AddAlarmFabPreview() {
    MaterialTheme {
        AddAlarmFab(fabImage = painterResource(Res.drawable.fab_icon)) {}
    }
}

private object AddAlarmFab {
    const val FAB_BACKGROUND_COLOR = 0x482FF7
    val FAB_IMAGE_SIZE = 72.dp
}
