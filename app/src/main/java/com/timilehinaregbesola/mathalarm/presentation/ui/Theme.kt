package com.timilehinaregbesola.mathalarm.presentation.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColors(
    primary = darkPrimary,
    primaryVariant = darkPrimaryLight,
    secondary = secondaryColor,
    secondaryVariant = secondaryDarkColor,
    surface = darkPrimary,
    background = darkPrimary,
    onPrimary = Color.White,
    onSurface = Color.White,
    onBackground = Color.White,
    onSecondary = Color.White,
)

private val LightColorPalette = lightColors(
    primary = Color.White,
    primaryVariant = primaryDark,
    secondary = secondaryColor,
    secondaryVariant = secondaryLightColor,
    surface = Color.White,
    background = Color.White,
    onPrimary = Color.Black,
    onSurface = Color.Black,
    onBackground = Color.Black,
    onSecondary = Color.White

        /* Other default colors to override
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    */
)

@Composable
fun MathAlarmTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = typography,
        shapes = shapes,
        content = content
    )
}
