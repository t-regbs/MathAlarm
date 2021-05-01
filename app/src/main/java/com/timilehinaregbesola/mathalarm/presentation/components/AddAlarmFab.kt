package com.timilehinaregbesola.mathalarm.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListViewModel
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
@Composable
fun AddAlarmFab(
    modifier: Modifier = Modifier,
    viewModel: AlarmListViewModel,
    fabImage: Painter,
    scaffoldState: BottomSheetScaffoldState
) {
    val scope = rememberCoroutineScope()
    FloatingActionButton(
        modifier = modifier,
        onClick = {
            viewModel.onAdd()
            scope.launch {
                scaffoldState.bottomSheetState.expand()
            }
        },
        backgroundColor = Color(0x482FF7),
    ) {
        Image(
            modifier = Modifier
                .width(72.dp)
                .height(75.dp),
            painter = fabImage,
            contentDescription = null
        )
    }
}