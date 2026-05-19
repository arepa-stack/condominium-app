package com.example.condominio.ui.theme

// TODO: Re-enable when a real dark palette is designed for Apto.
// import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = AptoSecondaryContainer,
    onPrimary = AptoOnSecondary,
    primaryContainer = AptoSecondaryFixed,
    onPrimaryContainer = AptoSecondary,

    secondary = AptoSecondary,
    onSecondary = AptoOnSecondary,
    secondaryContainer = AptoSecondaryFixed,
    onSecondaryContainer = AptoSecondary,

    tertiary = AptoCategoryBlue,
    onTertiary = Color.White,

    background = AptoBackground,
    onBackground = AptoOnSurface,

    surface = AptoSurfaceContainerLowest,
    onSurface = AptoOnSurface,

    surfaceVariant = AptoSurfaceContainerLow,
    onSurfaceVariant = AptoOnSurfaceVariant,

    outline = AptoOutline,
    outlineVariant = AptoOutlineVariant,

    error = AptoError,
    onError = Color.White,
    errorContainer = AptoErrorContainer,
    onErrorContainer = AptoError,
)

// Dark scheme is a placeholder — Apto has no canonical dark palette yet.
// Kept so the API stays stable; toggle to true only when a real dark theme lands.
private val DarkColorScheme = darkColorScheme(
    primary = AptoSecondaryContainer,
    onPrimary = AptoOnSecondary,
    secondary = AptoSecondary,
    onSecondary = AptoOnSecondary,
    error = AptoError,
    onError = Color.White,
)

@Composable
fun CondominioTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
