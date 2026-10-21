package com.example.condominio.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.condominio.ui.utils.isWebPlatform

// =============================================================================
// Apto Design System — canonical palette (single source of truth)
// =============================================================================
//
// Consumption rules:
// 1. Prefer `MaterialTheme.colorScheme.*` over direct token references.
// 2. Use semantic status tokens (AptoSuccess/Warning) for success/warning UI —
//    Material 3 colorScheme has no equivalent.
// 3. Use category tokens (AptoCategory*) only for decorative multi-color
//    elements like quick-action chips, where each item needs its own hue.
// =============================================================================

// POC: on web the brand tokens take the condominium-admin "Warm Architecture" palette (app/globals.css);
// Android/iOS keep the Apto values. ponytail: one switch at the token level, no second theme to maintain.
internal fun brand(mobile: Long, web: Long) = Color(if (isWebPlatform) web else mobile)

// --- Material 3 colorScheme tokens ---------------------------------------------
val AptoPrimary                = Color(0xFF000000) // text/icon primary on light surfaces
val AptoPrimaryFixed           = brand(0xFFD8E4EC, 0xFFE9E4DB)

val AptoSecondary              = brand(0xFF9F4200, 0xFF92400E) // brand dark variant
val AptoOnSecondary            = Color(0xFFFFFFFF)
val AptoSecondaryContainer     = brand(0xFFC45006, 0xFFB45309) // brand orange Apto (terracota cálido); white on it ≥ 4.5:1
val AptoSecondaryFixed         = brand(0xFFFFDBCB, 0xFFF6EBE0)

val AptoBackground             = brand(0xFFFFFFFF, 0xFFF3F1ED) // Apto: blanco puro de pantalla
val AptoSurface                = Color(0xFFFFFFFF)
val AptoSurfaceContainerLowest = Color(0xFFFFFFFF) // cards
val AptoSurfaceContainerLow    = brand(0xFFF2F4F6, 0xFFEBE8E2)
val AptoSurfaceContainerHigh   = brand(0xFFE6E8EA, 0xFFE8E5DD)

val AptoOnSurface              = brand(0xFF09151A, 0xFF1C1917) // Apto: azul profundo / negro
val AptoOnSurfaceVariant       = brand(0xFF43474A, 0xFF6A6662)
val AptoOutline                = brand(0xFF73787A, 0xFF8A847C)
val AptoOutlineVariant         = brand(0xFFE6E8EA, 0xFFD6D2C9) // Apto: borde suave de cards

val AptoError                  = brand(0xFFB3261E, 0xFFDC2626) // Apto: rojo crítico
val AptoErrorContainer         = brand(0xFFFFDAD6, 0xFFF9E1DC)

// --- Semantic status tokens (no M3 colorScheme equivalent) ---------------------
val AptoSuccess                = brand(0xFF146C2E, 0xFF15803D) // Apto: verde bandera (aprobado)
val AptoSuccessContainer       = brand(0xFFD1FAE5, 0xFFE4EFE6)
val AptoWarning                = brand(0xFFF59E0B, 0xFFD97706)
val AptoWarningContainer       = brand(0xFFFEF3C7, 0xFFFBEFD9)
val AptoOnWarningContainer     = brand(0xFF8C5A06, 0xFF8D4D04) // warning text on a warning tint (AptoWarning itself is ~2:1)
val AptoStatusSuccess          = AptoSuccess  // alias for legacy call sites
val AptoStatusWarning          = AptoWarning  // alias for legacy call sites
val AptoStatusError            = AptoError    // alias — Apto unifica crítico = error

/**
 * Readable text color for a badge tinted with [status] (status color at 10–20% alpha): WCAG AA 4.5:1.
 * Warning and brand orange get darker tokens; on web every other accent is darkened (accent on accent/20 is 2.3–4.4:1).
 */
fun onStatusContainer(status: Color): Color = when {
    status == AptoWarning -> AptoOnWarningContainer
    status == AptoSecondaryContainer -> AptoSecondary // brand orange on its own tint is ~4:1
    isWebPlatform -> Color(status.red * 0.65f, status.green * 0.65f, status.blue * 0.65f, status.alpha)
    else -> status
}

// --- Navigation -----------------------------------------------------------------
val AptoNavInactive            = Color(0xFF6B7280) // gray-500: inactive tab label/icon (4.5:1 on white and sidebar)

// --- Category decorative tokens (multi-color UI surfaces) ----------------------
val AptoCategoryOrange         = Color(0xFFFF6D00)
val AptoCategoryBlue           = brand(0xFF0091EA, 0xFF0369A1)
val AptoCategoryLavender       = brand(0xFF8B5CF6, 0xFF6B21A8) // Apto: quick action Decisiones
val AptoCategoryGreen          = brand(0xFF16A34A, 0xFF15803D) // Apto: quick action Cartelera

// Petty cash: Apto spec = icono terracota sobre fondo rosa suave
val AptoPettyCashAccent          = brand(0xFFC2410C, 0xFFB45309) // terracota
val AptoPettyCashAccentContainer = brand(0xFFFDF2F8, 0xFFF6EBE0) // rosa suave

// Hero gradients (decorative headers)
val AptoGradientGold             = brand(0xFFFF9E00, 0xFFD97706) // golden orange — top of brand gradient

// Decisions medal tokens (decorative — podium top 3 + neutral)
val AptoMedalGold     = Color(0xFFFFC107)
val AptoMedalGoldDark = Color(0xFFB8860B) // dark goldenrod for contrast on gold backgrounds
val AptoMedalSilver   = Color(0xFFB0BEC5)
val AptoMedalBronze   = Color(0xFFCD7F32)
val AptoMedalNeutral  = Color(0xFF90A4AE) // gris azulado — barras "fuera del top 3"

