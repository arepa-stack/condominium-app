package com.example.condominio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            route.startsWith("dashboard") -> HOME
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

private val BottomTab.label: StringResource
    get() = when (this) {
        BottomTab.HOME -> Res.string.nav_home
        BottomTab.PAYMENTS -> Res.string.nav_payments
        BottomTab.BILLBOARD -> Res.string.nav_billboard
        BottomTab.DECISIONS -> Res.string.nav_decisions
        BottomTab.PROFILE -> Res.string.nav_profile
    }

private fun BottomTab.icon(active: Boolean): ImageVector = when (this) {
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
    val activeColor = Color(0xFFFF6B00) // brand orange
    val inactiveColor = Color(0xFF9CA3AF) // gray-400
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomTab.entries.forEach { tab ->
                val active = tab == activeTab
                val color = if (active) activeColor else inactiveColor
                val label = stringResource(tab.label)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { if (!active) onTabSelected(tab) }
                ) {
                    Icon(
                        imageVector = tab.icon(active),
                        contentDescription = label,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = color
                    )
                }
            }
        }
    }
}
