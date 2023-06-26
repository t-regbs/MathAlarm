package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Alignment.Companion.TopEnd
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timilehinaregbesola.mathalarm.R
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.EmptyScreen.EmptyImageEndPadding
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.EmptyScreen.EmptyImageHeight
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.EmptyScreen.EmptyImageTopPadding
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.EmptyScreen.EmptyImageWidth
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.EmptyScreen.EmptyTextFontSize
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.EmptyScreen.EmptyTextTopPadding
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing

@ExperimentalMaterialApi
@Composable
fun AlarmEmptyScreen(
    modifier: Modifier = Modifier,
    darkTheme: Boolean,
    onClickFab: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(top = MaterialTheme.spacing.medium),
        contentAlignment = Center,
    ) {
        val emptyImage = painterResource(id = R.drawable.search_icon)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .align(TopCenter),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = EmptyImageTopPadding)
                    .align(CenterHorizontally),
                contentAlignment = TopEnd,
            ) {
                Image(
                    painter = emptyImage,
                    contentDescription = "Empty Alarm List",
                    colorFilter = if (darkTheme) ColorFilter.tint(color = White) else null,
                    modifier = Modifier
                        .width(EmptyImageWidth)
                        .height(EmptyImageHeight),
                )
                Image(
                    painter = emptyImage,
                    contentDescription = "Empty Alarm List",
                    colorFilter = if (darkTheme) ColorFilter.tint(color = White) else null,
                    modifier = Modifier
                        .padding(
                            top = MaterialTheme.spacing.medium,
                            end = EmptyImageEndPadding,
                        )
                        .width(EmptyImageWidth)
                        .height(EmptyImageHeight),
                )
            }

            Text(
                modifier = Modifier
                    .padding(top = EmptyTextTopPadding)
                    .align(CenterHorizontally),
                text = "Nothing to see here",
                fontSize = EmptyTextFontSize,
            )
        }
        val fabImage = painterResource(id = R.drawable.fabb)
        AddAlarmFab(
            modifier = Modifier
                .padding(
                    bottom = MaterialTheme.spacing.medium,
                    end = MaterialTheme.spacing.medium,
                )
                .align(BottomEnd),
            fabImage = fabImage,
            onClick = onClickFab,
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@Preview
private fun EmptyScreenPreview() {
    MaterialTheme {
        AlarmEmptyScreen(
            darkTheme = true,
        ) {}
    }
}

private object EmptyScreen {
    val EmptyTextFontSize = 16.sp
    val EmptyTextTopPadding = 29.dp
    val EmptyImageHeight = 228.dp
    val EmptyImageWidth = 167.dp
    val EmptyImageEndPadding = 40.dp
    val EmptyImageTopPadding = 143.dp
}
