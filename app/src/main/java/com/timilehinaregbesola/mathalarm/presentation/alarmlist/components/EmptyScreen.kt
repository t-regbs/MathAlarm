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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.EmptyScreen.EMPTY_IMAGE_END_PADDING
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.EmptyScreen.EMPTY_IMAGE_HEIGHT
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.EmptyScreen.EMPTY_IMAGE_TOP_PADDING
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.EmptyScreen.EMPTY_IMAGE_WIDTH
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.EmptyScreen.EMPTY_TEXT_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.EmptyScreen.EMPTY_TEXT_TOP_PADDING
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing

@ExperimentalMaterial3Api
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
                    .padding(top = EMPTY_IMAGE_TOP_PADDING)
                    .align(CenterHorizontally),
                contentAlignment = TopEnd,
            ) {
                Image(
                    painter = emptyImage,
                    contentDescription = "Empty Alarm List",
                    colorFilter = if (darkTheme) ColorFilter.tint(color = White) else null,
                    modifier = Modifier
                        .width(EMPTY_IMAGE_WIDTH)
                        .height(EMPTY_IMAGE_HEIGHT),
                )
                Image(
                    painter = emptyImage,
                    contentDescription = "Empty Alarm List",
                    colorFilter = if (darkTheme) ColorFilter.tint(color = White) else null,
                    modifier = Modifier
                        .padding(
                            top = MaterialTheme.spacing.medium,
                            end = EMPTY_IMAGE_END_PADDING,
                        )
                        .width(EMPTY_IMAGE_WIDTH)
                        .height(EMPTY_IMAGE_HEIGHT),
                )
            }

            Text(
                modifier = Modifier
                    .padding(top = EMPTY_TEXT_TOP_PADDING)
                    .align(CenterHorizontally),
                text = "Nothing to see here",
                fontSize = EMPTY_TEXT_FONT_SIZE,
            )
        }
        val fabImage = painterResource(id = R.drawable.fab_icon)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
private fun EmptyScreenPreview() {
    MaterialTheme {
        Surface {
            AlarmEmptyScreen(
                darkTheme = false,
            ) {}
        }
    }
}

private object EmptyScreen {
    val EMPTY_TEXT_FONT_SIZE = 16.sp
    val EMPTY_TEXT_TOP_PADDING = 29.dp
    val EMPTY_IMAGE_HEIGHT = 228.dp
    val EMPTY_IMAGE_WIDTH = 167.dp
    val EMPTY_IMAGE_END_PADDING = 40.dp
    val EMPTY_IMAGE_TOP_PADDING = 143.dp
}
