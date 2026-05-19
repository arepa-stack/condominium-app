package com.example.condominio.ui.theme

import androidx.compose.ui.graphics.Color

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

// --- Material 3 colorScheme tokens ---------------------------------------------
val AptoPrimary                = Color(0xFF000000) // text/icon primary on light surfaces
val AptoPrimaryFixed           = Color(0xFFD8E4EC)

val AptoSecondary              = Color(0xFF9F4200) // brand dark variant
val AptoOnSecondary            = Color(0xFFFFFFFF)
val AptoSecondaryContainer     = Color(0xFFDB5A07) // brand orange Apto (terracota cálido)
val AptoSecondaryFixed         = Color(0xFFFFDBCB)

val AptoBackground             = Color(0xFFFFFFFF) // Apto: blanco puro de pantalla
val AptoSurface                = Color(0xFFFFFFFF)
val AptoSurfaceContainerLowest = Color(0xFFFFFFFF) // cards
val AptoSurfaceContainerLow    = Color(0xFFF2F4F6)
val AptoSurfaceContainerHigh   = Color(0xFFE6E8EA)

val AptoOnSurface              = Color(0xFF09151A) // Apto: azul profundo / negro
val AptoOnSurfaceVariant       = Color(0xFF43474A)
val AptoOutline                = Color(0xFF73787A)
val AptoOutlineVariant         = Color(0xFFE6E8EA) // Apto: borde suave de cards

val AptoError                  = Color(0xFFB3261E) // Apto: rojo crítico
val AptoErrorContainer         = Color(0xFFFFDAD6)

// --- Semantic status tokens (no M3 colorScheme equivalent) ---------------------
val AptoSuccess                = Color(0xFF146C2E) // Apto: verde bandera (aprobado)
val AptoSuccessContainer       = Color(0xFFD1FAE5)
val AptoWarning                = Color(0xFFF59E0B)
val AptoWarningContainer       = Color(0xFFFEF3C7)
val AptoStatusSuccess          = AptoSuccess  // alias for legacy call sites
val AptoStatusWarning          = AptoWarning  // alias for legacy call sites
val AptoStatusError            = AptoError    // alias — Apto unifica crítico = error

// --- Category decorative tokens (multi-color UI surfaces) ----------------------
val AptoCategoryOrange         = Color(0xFFFF6D00)
val AptoCategoryBlue           = Color(0xFF0091EA)
val AptoCategoryLavender       = Color(0xFF8B5CF6) // Apto: quick action Decisiones
val AptoCategoryGreen          = Color(0xFF16A34A) // Apto: quick action Cartelera

// Petty cash: Apto spec = icono terracota sobre fondo rosa suave
val AptoPettyCashAccent          = Color(0xFFC2410C) // terracota
val AptoPettyCashAccentContainer = Color(0xFFFDF2F8) // rosa suave

// Hero gradients (decorative headers)
val AptoGradientGold             = Color(0xFFFF9E00) // golden orange — top of brand gradient

// Decisions medal tokens (decorative — podium top 3 + neutral)
val AptoMedalGold     = Color(0xFFFFC107)
val AptoMedalGoldDark = Color(0xFFB8860B) // dark goldenrod for contrast on gold backgrounds
val AptoMedalSilver   = Color(0xFFB0BEC5)
val AptoMedalBronze   = Color(0xFFCD7F32)
val AptoMedalNeutral  = Color(0xFF90A4AE) // gris azulado — barras "fuera del top 3"

