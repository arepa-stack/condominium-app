package com.example.condominio.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// =============================================================================
// Apto Type System — single source of truth
// =============================================================================
//
// Consumption rules:
// 1. Use `MaterialTheme.typography.X` in components — never hardcode fontSize.
// 2. Apto look = "heavy/firm": titles Bold/ExtraBold, body Medium (not Normal).
// 3. Letter spacing tightens for large titles, widens for small labels.
// =============================================================================

private val ApFamily = FontFamily.Default

val Typography = Typography(
    // --- Display: largest hero text (splash, oversized avatar) ----------------
    displayLarge = TextStyle(
        fontFamily = ApFamily,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-1).sp
    ),
    displayMedium = TextStyle(
        fontFamily = ApFamily,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-1).sp
    ),
    displaySmall = TextStyle(
        fontFamily = ApFamily,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    ),

    // --- Headline: page-level titles and hero amounts ------------------------
    headlineLarge = TextStyle(
        fontFamily = ApFamily,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-1).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = ApFamily,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = ApFamily,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.5).sp
    ),

    // --- Title: card titles and topbar -------------------------------------
    titleLarge = TextStyle(
        fontFamily = ApFamily,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = ApFamily,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp
    ),
    titleSmall = TextStyle(
        fontFamily = ApFamily,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    ),

    // --- Body: main content text -------------------------------------------
    bodyLarge = TextStyle(
        fontFamily = ApFamily,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = ApFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = ApFamily,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.3.sp
    ),

    // --- Label: buttons, badges, eyebrows ----------------------------------
    labelLarge = TextStyle(
        fontFamily = ApFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp
    ),
    labelMedium = TextStyle(
        fontFamily = ApFamily,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = ApFamily,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
    ),
)
