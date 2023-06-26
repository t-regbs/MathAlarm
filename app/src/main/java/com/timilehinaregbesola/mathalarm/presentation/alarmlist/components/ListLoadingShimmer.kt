package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode.Restart
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.DarkGray
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.ListLoadingShimmer.DoublePadding
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.ListLoadingShimmer.InfiniteTransitionLabel
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.ListLoadingShimmer.NinetyPercent
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.ListLoadingShimmer.ShimmerCardNumber
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.ListLoadingShimmer.ShimmerDelay
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.ListLoadingShimmer.ThirtyPercent
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.ListLoadingShimmer.TwentyPercent
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.ListLoadingShimmer.XShimmerDuration
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.ListLoadingShimmer.XShimmerLabel
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.ListLoadingShimmer.YShimmerDuration
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.ListLoadingShimmer.YShimmerLabel
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing

@Composable
fun ListLoadingShimmer(
    imageHeight: Dp,
    padding: Dp = MaterialTheme.spacing.medium,
    isDark: Boolean,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        val cardWidthPx = with(LocalDensity.current) { (maxWidth - (padding * DoublePadding)).toPx() }
        val cardHeightPx = with(LocalDensity.current) { (imageHeight - padding).toPx() }
        val gradientWidth: Float = (TwentyPercent * cardHeightPx)

        val infiniteTransition = rememberInfiniteTransition(label = InfiniteTransitionLabel)
        val xCardShimmer = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (cardWidthPx + gradientWidth),
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = XShimmerDuration,
                    easing = LinearEasing,
                    delayMillis = ShimmerDelay,
                ),
                repeatMode = Restart,
            ),
            label = XShimmerLabel,
        )
        val yCardShimmer = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (cardHeightPx + gradientWidth),
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = YShimmerDuration,
                    easing = LinearEasing,
                    delayMillis = ShimmerDelay,
                ),
                repeatMode = Restart,
            ),
            label = YShimmerLabel,
        )

        val lightColors = with(LightGray) {
            listOf(
                copy(alpha = NinetyPercent),
                copy(alpha = ThirtyPercent),
                copy(alpha = NinetyPercent),
            )
        }
        val darkColors = with(DarkGray) {
            listOf(
                copy(alpha = NinetyPercent),
                copy(alpha = ThirtyPercent),
                copy(alpha = NinetyPercent),
            )
        }

        LazyColumn {
            items(ShimmerCardNumber) {
                ShimmerCardItem(
                    colors = if (isDark) darkColors else lightColors,
                    xShimmer = xCardShimmer.value,
                    yShimmer = yCardShimmer.value,
                    cardHeight = imageHeight,
                    gradientWidth = gradientWidth,
                    padding = padding,
                )
            }
        }
    }
}

private object ListLoadingShimmer {
    const val InfiniteTransitionLabel = "InfiniteTransition"
    const val XShimmerLabel = "XShimmer"
    const val YShimmerLabel = "YShimmer"
    const val XShimmerDuration = 2000
    const val YShimmerDuration = 1300
    const val ShimmerDelay = 300
    const val NinetyPercent = .9f
    const val ThirtyPercent = .3f
    const val TwentyPercent = .2f
    const val DoublePadding = 2
    const val ShimmerCardNumber = 5
}
