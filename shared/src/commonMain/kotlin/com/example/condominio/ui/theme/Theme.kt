package com.example.condominio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryWarm,
    secondary = SecondaryWarm,
    tertiary = TertiaryWarm,
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = OnBackgroundDark,
    onSurface = OnBackgroundDark,
    primaryContainer = PrimaryWarm.copy(alpha = 0.2f),
    onPrimaryContainer = OnBackgroundDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryWarm,
    secondary = SecondaryWarm,
    tertiary = TertiaryWarm,
    background = BackgroundLight,
    surface = SurfaceLight,
    onBackground = OnBackgroundLight,
    onSurface = OnBackgroundLight,
    primaryContainer = PrimaryWarm.copy(alpha = 0.1f),
    onPrimaryContainer = PrimaryWarm
)

@Composable
fun CondominioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
