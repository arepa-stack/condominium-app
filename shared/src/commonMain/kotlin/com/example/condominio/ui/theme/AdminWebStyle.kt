package com.example.condominio.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.condominio.ui.utils.isWebPlatform

// =============================================================================
// Web POC — condominium-admin look (shadcn "Warm Architecture").
// Every helper returns the native value on Android/iOS, so mobile stays unchanged.
// Delete this file + its call sites to drop the POC.
// =============================================================================

// --- Badges: pill, accent/20 fill, accent/30 border, darkened accent text (AA) -----
fun badgeShape(native: Shape): Shape = if (isWebPlatform) CircleShape else native
fun badgeBg(native: Color, accent: Color): Color = if (isWebPlatform) accent.copy(alpha = 0.2f) else native
fun badgeText(native: Color, accent: Color): Color = if (isWebPlatform) onStatusContainer(accent) else native
fun badgeBorder(accent: Color): BorderStroke? = if (isWebPlatform) BorderStroke(1.dp, accent.copy(alpha = 0.3f)) else null

// --- Tables: transparent header row (admin) instead of a filled band ------------
val TableHeaderBg: Color get() = if (isWebPlatform) Color.Transparent else AptoSurfaceContainerLow

// --- Cards: stat cards tinted with the primary color --------------------------
// Opaque on purpose: primary/5 pre-blended over the #F3F1ED canvas (a translucent card shows its own shadow through it).
fun statCardBg(native: Color): Color = if (isWebPlatform) Color(0xFFF0E9E2) else native
fun statCardBorder(native: BorderStroke?): BorderStroke? =
    if (isWebPlatform) BorderStroke(1.dp, AptoSecondaryContainer.copy(alpha = 0.15f)) else native
val StatCardRadius: Dp get() = if (isWebPlatform) 16.dp else 12.dp

/** Admin logo: outlined building on a primary/10 tile. Web only; native keeps its PNG. */
@Composable
fun AdminLogoMark(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .background(AptoSecondaryContainer.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
            .border(1.dp, AptoSecondaryContainer.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.Apartment, contentDescription = null, tint = AptoSecondaryContainer, modifier = Modifier.size(size * 0.55f))
    }
}
