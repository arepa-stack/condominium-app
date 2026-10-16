package com.example.condominio.ui.theme

// TODO: Re-enable when a real dark palette is designed for Apto.
// import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import com.example.condominio.ui.utils.isWebPlatform
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CondominioTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = platformTypography(),
    ) {
        // Web POC: hover/press state layers tinted with the brand (admin look) instead of Material's gray.
        if (isWebPlatform) {
            CompositionLocalProvider(LocalRippleConfiguration provides RippleConfiguration(color = AptoSecondaryContainer), content = content)
        } else {
            content()
        }
    }
}
