package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing

@Composable
fun ListLoadingShimmer(
    imageHeight: Dp,
    padding: Dp = MaterialTheme.spacing.medium,
    theme: Boolean
) {
//    val isDark = when (theme) {
//        AppThemeOptions.DARK -> true
//        AppThemeOptions.LIGHT -> false
//        else -> isSystemInDarkTheme()
//    }
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val cardWidthPx = with(LocalDensity.current) { (maxWidth - (padding * 2)).toPx() }
        val cardHeightPx = with(LocalDensity.current) { (imageHeight - padding).toPx() }
        val gradientWidth: Float = (0.2f * cardHeightPx)

        val infiniteTransition = rememberInfiniteTransition()
        val xCardShimmer = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (cardWidthPx + gradientWidth),
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 2000,
                    easing = LinearEasing,
                    delayMillis = 300
                ),
                repeatMode = RepeatMode.Restart
            )
        )
        val yCardShimmer = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (cardHeightPx + gradientWidth),
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1300,
                    easing = LinearEasing,
                    delayMillis = 300
                ),
                repeatMode = RepeatMode.Restart
            )
        )

        val lightColors = listOf(
            Color.LightGray.copy(alpha = .9f),
            Color.LightGray.copy(alpha = .3f),
            Color.LightGray.copy(alpha = .9f),
        )
        val darkColors = listOf(
            Color.DarkGray.copy(alpha = .9f),
            Color.DarkGray.copy(alpha = .3f),
            Color.DarkGray.copy(alpha = .9f),
        )

        LazyColumn {
            items(5) {
                ShimmerCardItem(
                    colors = if (theme) darkColors else lightColors,
                    xShimmer = xCardShimmer.value,
                    yShimmer = yCardShimmer.value,
                    cardHeight = imageHeight,
                    gradientWidth = gradientWidth,
                    padding = padding
                )
            }
        }
    }
}
