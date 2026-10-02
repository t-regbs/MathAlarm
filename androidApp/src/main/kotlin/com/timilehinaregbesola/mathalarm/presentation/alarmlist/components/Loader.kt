package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.alexzhirkevich.compottie.Compottie.IterateForever
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter

@Composable
fun Loader(modifier: Modifier = Modifier) {
    val assets = LocalContext.current.assets
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            assets.open("files/loading.json").bufferedReader().use { it.readText() }
        )
    }
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = IterateForever,
    )
    Image(
        modifier = modifier,
        painter = rememberLottiePainter(
            composition = composition,
            progress = { progress },
        ),
        contentDescription = "Loading animation"
    )
}
