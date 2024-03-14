package com.timilehinaregbesola.mathalarm.presentation.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColorScheme(
    primary = darkPrimary,
    primaryContainer = darkPrimaryLight,
    secondary = secondaryColor,
    secondaryContainer = secondaryDarkColor,
    surface = darkPrimary,
    background = darkPrimary,
    onPrimary = Color.White,
    onSurface = Color.White,
    onBackground = Color.White,
    onSecondary = Color.White,
)

private val LightColorPalette = lightColorScheme(
    primary = Color.White,
    primaryContainer = primaryDark,
    secondary = secondaryColor,
    secondaryContainer = secondaryLightColor,
    surface = Color.White,
    background = Color.White,
    onPrimary = Color.Black,
    onSurface = Color.Black,
    onBackground = Color.Black,
    onSecondary = Color.White
)

@Composable
fun MathAlarmTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    CompositionLocalProvider(LocalSpacing provides Spacing()) {
        MaterialTheme(
            colorScheme = colors,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}
