package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush.Companion.linearGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing

@Composable
fun ShimmerCardItem(
    modifier: Modifier = Modifier,
    colors: List<Color>,
    xShimmer: Float,
    yShimmer: Float,
    cardHeight: Dp,
    gradientWidth: Float
) {
    val brush = linearGradient(
        colors,
        start = Offset(xShimmer - gradientWidth, yShimmer - gradientWidth),
        end = Offset(xShimmer, yShimmer),
    )
    with(MaterialTheme) {
        Column(modifier = modifier) {
            Surface(
                shape = shapes.small,
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cardHeight)
                        .background(brush = brush),
                )
            }
            Spacer(modifier = Modifier.height(spacing.small))
            Surface(
                shape = shapes.small,
                modifier = Modifier
                    .padding(vertical = spacing.small),
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cardHeight / 10)
                        .background(brush = brush),
                )
            }
        }
    }
}
