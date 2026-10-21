package com.example.condominio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ripple
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.condominio.ui.theme.AptoNavInactive
import com.example.condominio.ui.theme.brand
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import condominio.shared.generated.resources.Res
import condominio.shared.generated.resources.nav_billboard
import condominio.shared.generated.resources.nav_decisions
import condominio.shared.generated.resources.nav_home
import condominio.shared.generated.resources.nav_payments
import condominio.shared.generated.resources.nav_profile
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Pestañas principales de la app. Cada una navega a una ruta real del NavGraph.
 */
enum class BottomTab(val route: String) {
    HOME("dashboard"),
    PAYMENTS("invoice_list"),
    BILLBOARD("billboard_list"),
    DECISIONS("decisions_list"),
    PROFILE("profile");

    companion object {
        /** Resuelve la pestaña activa a partir de la ruta actual (patrón del NavDestination). */
        fun fromRoute(route: String?): BottomTab? = when {
            route == null -> null
            // unit_selection opened from the dashboard (wide layouts keep the sidebar there).
            route.startsWith("dashboard") || route == "unit_selection" -> HOME
            route.startsWith("invoice") ||
                route.startsWith("payment") ||
                route.startsWith("create_payment") -> PAYMENTS
            route.startsWith("billboard") -> BILLBOARD
            route.startsWith("decision") -> DECISIONS
            route.startsWith("profile") ||
                route == "edit_profile" ||
                route == "notification_settings" ||
                route == "change_password" -> PROFILE
            else -> null
        }
    }
}

internal val BottomTab.label: StringResource
    get() = when (this) {
        BottomTab.HOME -> Res.string.nav_home
        BottomTab.PAYMENTS -> Res.string.nav_payments
        BottomTab.BILLBOARD -> Res.string.nav_billboard
        BottomTab.DECISIONS -> Res.string.nav_decisions
        BottomTab.PROFILE -> Res.string.nav_profile
    }

internal fun BottomTab.icon(active: Boolean): ImageVector = when (this) {
    BottomTab.HOME -> if (active) Icons.Filled.Home else Icons.Outlined.Home
    BottomTab.PAYMENTS -> if (active) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong
    BottomTab.BILLBOARD -> if (active) Icons.Filled.Campaign else Icons.Outlined.Campaign
    BottomTab.DECISIONS -> if (active) Icons.Filled.HowToVote else Icons.Outlined.HowToVote
    BottomTab.PROFILE -> if (active) Icons.Filled.Person else Icons.Outlined.Person
}

/**
 * Barra de navegación inferior única para toda la app.
 *
 * @param currentRoute ruta actual del NavController (patrón), usada para marcar la pestaña activa.
 * @param onTabSelected callback con la pestaña pulsada; la navegación la resuelve el NavGraph.
 */
@Composable
fun AppBottomBar(
    currentRoute: String?,
    onTabSelected: (BottomTab) -> Unit,
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = AptoNavInactive
    val activeTab = BottomTab.fromRoute(currentRoute)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, Color(0xFFF3F4F6))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Edge-to-edge: keep the tabs above the gesture/3-button bar (the white surface still fills behind it).
                .windowInsetsPadding(WindowInsets.navigationBars)
                // 8dp + 48dp tabs + 8dp: same 64dp bar as before, with a 48dp touch target per tab.
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomTab.entries.forEach { tab ->
                val active = tab == activeTab
                val color = if (active) activeColor else inactiveColor
                val label = stringResource(tab.label)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    // Unbounded round ripple (like Material's NavigationBar) instead of the default square one.
                    modifier = Modifier
                        .selectable(
                            selected = active,
                            interactionSource = null,
                            indication = ripple(bounded = false, radius = 32.dp),
                            role = Role.Tab
                        ) { if (!active) onTabSelected(tab) }
                        .heightIn(min = 48.dp)
                        .widthIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = tab.icon(active),
                        contentDescription = null, // the label below names the tab
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = color
                    )
                }
            }
        }
    }
}

/**
 * Variante lateral de [AppBottomBar] para pantallas anchas (tablet, navegador de escritorio).
 * Mismas pestañas y colores; el NavGraph decide cuál mostrar según el ancho disponible.
 */
@Composable
fun AppNavRail(
    currentRoute: String?,
    onTabSelected: (BottomTab) -> Unit,
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = AptoNavInactive
    val activeTab = BottomTab.fromRoute(currentRoute)

    NavigationRail(
        containerColor = brand(0xFFFFFFFF, 0xFFF9F8F6), // web POC: condominium-admin --sidebar
        modifier = Modifier.border(BorderStroke(1.dp, Color(0xFFF3F4F6)))
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        BottomTab.entries.forEach { tab ->
            val active = tab == activeTab
            val label = stringResource(tab.label)
            NavigationRailItem(
                selected = active,
                onClick = { if (!active || currentRoute == "unit_selection") onTabSelected(tab) },
                // NavigationRailItem already exposes Role.Tab + selected; the label names it, so the icon is decorative.
                icon = { Icon(imageVector = tab.icon(active), contentDescription = null) },
                label = { Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = activeColor,
                    selectedTextColor = activeColor,
                    indicatorColor = activeColor.copy(alpha = 0.12f),
                    unselectedIconColor = inactiveColor,
                    unselectedTextColor = inactiveColor
                ),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}
