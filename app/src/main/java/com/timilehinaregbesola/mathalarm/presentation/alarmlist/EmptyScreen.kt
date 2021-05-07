package com.timilehinaregbesola.mathalarm.presentation.alarmlist

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timilehinaregbesola.mathalarm.R
import com.timilehinaregbesola.mathalarm.presentation.components.AddAlarmFab
import com.timilehinaregbesola.mathalarm.presentation.components.ClearDialog
import com.timilehinaregbesola.mathalarm.presentation.components.ListTopAppBar
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
@Composable
fun EmptyScreen(
    viewModel: AlarmListViewModel
) {
    val openDialog = remember { mutableStateOf(false) }
    val scaffoldState = rememberBottomSheetScaffoldState()
    val scope = rememberCoroutineScope()

    val fromAdd = viewModel.addClicked.observeAsState()
    viewModel.navigateToAlarmSettings.observeAsState().value.let { id ->
        if (id != null) {
            viewModel.getAlarm(id)
        }
    }
    val alarm = viewModel.alarm.value

    Surface(modifier = Modifier.fillMaxSize().padding(bottom = 24.dp)) {
        BottomSheetScaffold(
            sheetContent = {
                fromAdd.value?.let { AlarmBottomSheet(it, alarm, viewModel, scope, scaffoldState) }
            },
            sheetPeekHeight = 0.dp,
            scaffoldState = scaffoldState,
            topBar = {
                ListTopAppBar(openDialog)
            }
        ) {
            if (openDialog.value) ClearDialog(openDialog)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(color = Color.White)
            ) {
                val emptyImage = painterResource(id = R.drawable.search_icon)
                val fabImage = painterResource(id = R.drawable.fabb)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .align(Alignment.TopCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 143.dp)
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.TopEnd

                    ) {
                        Image(
                            painter = emptyImage,
                            contentDescription = "Empty Alarm List",
                            modifier = Modifier
                                .width(167.dp)
                                .height(228.dp)
                        )
                        Image(
                            painter = emptyImage,
                            contentDescription = "Empty Alarm List",
                            modifier = Modifier
                                .padding(top = 24.dp, end = 40.dp)
                                .width(167.dp)
                                .height(228.dp)
                        )
                    }

                    Text(
                        modifier = Modifier
                            .padding(top = 29.dp)
                            .align(Alignment.CenterHorizontally),
                        text = "Nothing to see here",
                        fontSize = 16.sp
                    )
                }
                AddAlarmFab(
                    modifier = Modifier
                        .padding(bottom = 24.dp, end = 16.dp)
                        .align(Alignment.BottomEnd),
                    fabImage = fabImage,
                    onClick = {
                        viewModel.onFabClicked()
                        scope.launch {
                            scaffoldState.bottomSheetState.expand()
                        }
                    }
                )
            }
        }
    }
}
