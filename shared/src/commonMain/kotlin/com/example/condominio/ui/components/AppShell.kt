package com.example.condominio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.condominio.ui.theme.AptoGradientGold
import com.example.condominio.ui.theme.AptoOnSurface
import com.example.condominio.ui.theme.AptoOnSurfaceVariant
import com.example.condominio.ui.theme.AptoOutlineVariant
import com.example.condominio.ui.theme.AptoSecondaryContainer
import com.example.condominio.ui.theme.brand
import com.example.condominio.ui.theme.AdminLogoMark
import com.example.condominio.ui.utils.isWebPlatform
import condominio.shared.generated.resources.Res
import condominio.shared.generated.resources.login_subtitle
import condominio.shared.generated.resources.logo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Permanent left sidebar for Expanded (desktop web) layouts: brand + labeled sections,
 * like a typical web dashboard. Medium layouts use [AppNavRail], phones [AppBottomBar].
 */
@Composable
fun AppSidebar(
    currentRoute: String?,
    onTabSelected: (BottomTab) -> Unit,
) {
    val activeTab = BottomTab.fromRoute(currentRoute)
    Column(
        modifier = Modifier
            .width(248.dp)
            .fillMaxHeight()
            .background(brand(0xFFFFFFFF, 0xFFF9F8F6)) // web POC: condominium-admin --sidebar
            .border(BorderStroke(1.dp, brand(0xFFE6E8EA, 0xFFE8E5DD)))
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, bottom = 32.dp)) {
            if (isWebPlatform) AdminLogoMark(36.dp)
            else Image(painterResource(Res.drawable.logo), contentDescription = null, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Apto", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = AptoOnSurface)
                Text("VIVIENDA ELEVADA", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontSize = 9.sp), color = AptoOnSurfaceVariant)
            }
        }
        BottomTab.entries.forEach { tab ->
            val active = tab == activeTab
            val label = stringResource(tab.label)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .clip(RoundedCornerShape(if (isWebPlatform) 12.dp else 10.dp))
                    .background(if (active) AptoSecondaryContainer.copy(alpha = 0.10f) else Color.Transparent)
                    .then(if (active && isWebPlatform) Modifier.border(1.dp, AptoSecondaryContainer.copy(alpha = 0.15f), RoundedCornerShape(12.dp)) else Modifier)
                    .clickable(interactionSource = null, indication = ripple(color = if (isWebPlatform) AptoSecondaryContainer else Color.Unspecified)) { if (!active || currentRoute == "unit_selection") onTabSelected(tab) }
                    .pointerHoverIcon(PointerIcon.Hand)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Icon(tab.icon(active), contentDescription = null, tint = if (active) AptoSecondaryContainer else AptoOnSurfaceVariant, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(14.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (active && !isWebPlatform) FontWeight.Bold else FontWeight.Medium),
                    color = if (active) AptoSecondaryContainer else AptoOnSurface
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Text("Apto by Nibs", style = MaterialTheme.typography.labelSmall, color = AptoOnSurfaceVariant, modifier = Modifier.padding(start = 12.dp))
    }
}

/** Brand panel shown left of the auth/onboarding screens on Expanded (desktop) layouts. */
@Composable
fun AuthBrandPanel(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Brush.linearGradient(listOf(AptoGradientGold, AptoSecondaryContainer)))
            .padding(56.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(Res.drawable.logo), contentDescription = null, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.width(12.dp))
            Text("Apto", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
        }
        Column(Modifier.align(Alignment.CenterStart).widthIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "Tu condominio, en orden.",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                stringResource(Res.string.login_subtitle),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
        Text(
            "VIVIENDA ELEVADA",
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 3.sp, fontWeight = FontWeight.SemiBold),
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}
