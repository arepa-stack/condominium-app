package com.example.condominio.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import condominio.shared.generated.resources.Res
import condominio.shared.generated.resources.dm_serif_display
import condominio.shared.generated.resources.outfit
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font

// Web POC: condominium-admin fonts. Outfit is a variable font (one file, every weight);
// DM Serif Display (single weight) only on headlineMedium = page titles (admin h1); amounts stay Outfit.
@Composable
internal actual fun platformTypography(): Typography {
    val outfit = FontFamily(
        listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold)
            .map { Font(Res.font.outfit, it) }
    )
    val serif = FontFamily(Font(Res.font.dm_serif_display))
    fun TextStyle.sans() = copy(fontFamily = outfit)
    fun TextStyle.title() = copy(fontFamily = serif, letterSpacing = (-0.5).sp)
    return with(Typography) {
        Typography(
            displayLarge = displayLarge.sans(), displayMedium = displayMedium.sans(), displaySmall = displaySmall.sans(),
            headlineLarge = headlineLarge.sans(), headlineMedium = headlineMedium.title(), headlineSmall = headlineSmall.sans(),
            titleLarge = titleLarge.sans(), titleMedium = titleMedium.sans(), titleSmall = titleSmall.sans(),
            bodyLarge = bodyLarge.sans(), bodyMedium = bodyMedium.sans(), bodySmall = bodySmall.sans(),
            labelLarge = labelLarge.sans(), labelMedium = labelMedium.sans(), labelSmall = labelSmall.sans(),
        )
    }
}

