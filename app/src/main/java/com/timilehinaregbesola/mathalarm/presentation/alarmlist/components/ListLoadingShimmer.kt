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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.DarkGray
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.ListLoadingShimmer.DOUBLE_PADDING
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.ListLoadingShimmer.INFINITE_TRANSITION_LABEL
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.ListLoadingShimmer.NINETY_PERCENT
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.ListLoadingShimmer.SHIMMER_CARD_NUMBER
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.ListLoadingShimmer.SHIMMER_DELAY
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.ListLoadingShimmer.THIRTY_PERCENT
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.ListLoadingShimmer.TWENTY_PERCENT
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.ListLoadingShimmer.X_SHIMMER_DURATION
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.ListLoadingShimmer.X_SHIMMER_LABEL
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.ListLoadingShimmer.Y_SHIMMER_DURATION
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.ListLoadingShimmer.Y_SHIMMER_LABEL
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
        val cardWidthPx = with(LocalDensity.current) { (maxWidth - (padding * DOUBLE_PADDING)).toPx() }
        val cardHeightPx = with(LocalDensity.current) { (imageHeight - padding).toPx() }
        val gradientWidth: Float = (TWENTY_PERCENT * cardHeightPx)

        val infiniteTransition = rememberInfiniteTransition(label = INFINITE_TRANSITION_LABEL)
        val xCardShimmer = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (cardWidthPx + gradientWidth),
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = X_SHIMMER_DURATION,
                    easing = LinearEasing,
                    delayMillis = SHIMMER_DELAY,
                ),
                repeatMode = Restart,
            ),
            label = X_SHIMMER_LABEL,
        )
        val yCardShimmer = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (cardHeightPx + gradientWidth),
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = Y_SHIMMER_DURATION,
                    easing = LinearEasing,
                    delayMillis = SHIMMER_DELAY,
                ),
                repeatMode = Restart,
            ),
            label = Y_SHIMMER_LABEL,
        )

        val lightColors = with(LightGray) {
            listOf(
                copy(alpha = NINETY_PERCENT),
                copy(alpha = THIRTY_PERCENT),
                copy(alpha = NINETY_PERCENT),
            )
        }
        val darkColors = with(DarkGray) {
            listOf(
                copy(alpha = NINETY_PERCENT),
                copy(alpha = THIRTY_PERCENT),
                copy(alpha = NINETY_PERCENT),
            )
        }

        LazyColumn {
            items(SHIMMER_CARD_NUMBER) {
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
    const val INFINITE_TRANSITION_LABEL = "InfiniteTransition"
    const val X_SHIMMER_LABEL = "XShimmer"
    const val Y_SHIMMER_LABEL = "YShimmer"
    const val X_SHIMMER_DURATION = 2000
    const val Y_SHIMMER_DURATION = 1300
    const val SHIMMER_DELAY = 300
    const val NINETY_PERCENT = .9f
    const val THIRTY_PERCENT = .3f
    const val TWENTY_PERCENT = .2f
    const val DOUBLE_PADDING = 2
    const val SHIMMER_CARD_NUMBER = 5
}
