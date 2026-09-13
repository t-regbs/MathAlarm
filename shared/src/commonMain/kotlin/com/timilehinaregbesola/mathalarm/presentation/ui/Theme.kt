package com.timilehinaregbesola.mathalarm.presentation.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColorScheme(
    primary = Color(0xFFCEC0FF),
    onPrimary = Color(0xFF29105F),
    primaryContainer = Color(0xFF44316F),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCEC0FF),
    onSecondary = Color(0xFF29105F),
    secondaryContainer = Color(0xFF44316F),
    onSecondaryContainer = Color(0xFFEADDFF),
    surface = Color(0xFF211F24),
    onSurface = Color(0xFFE8E1E9),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    background = Color(0xFF211F24),
    onBackground = Color(0xFFE8E1E9),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
)

private val LightColorPalette = lightColorScheme(
    primary = Color(0xFF482FF6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF29105F),
    secondary = Color(0xFF482FF6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEADDFF),
    onSecondaryContainer = Color(0xFF29105F),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF1D1B20),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
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
