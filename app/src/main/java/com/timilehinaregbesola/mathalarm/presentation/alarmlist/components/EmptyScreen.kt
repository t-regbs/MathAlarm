package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timilehinaregbesola.mathalarm.R

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AlarmEmptyScreen(
    modifier: Modifier = Modifier,
    onClickFab: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        val emptyImage = painterResource(id = R.drawable.search_icon)
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
        val fabImage = painterResource(id = R.drawable.fabb)
        AddAlarmFab(
            modifier = Modifier
                .padding(bottom = 16.dp, end = 16.dp)
                .align(Alignment.BottomEnd),
            fabImage = fabImage,
            onClick = onClickFab
        )
    }
}
