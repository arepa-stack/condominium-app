package com.example.condominio.ui.screens.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.condominio.data.model.Payment
import com.example.condominio.data.model.PaymentStatus
import org.koin.compose.viewmodel.koinViewModel
import com.example.condominio.ui.utils.formatCurrency
import com.example.condominio.ui.utils.formatDate
import org.jetbrains.compose.resources.stringResource
import condominio.shared.generated.resources.*

@Composable
fun DashboardScreen(
        onPayClick: () -> Unit,
        onHistoryClick: () -> Unit,
        onPaymentClick: (String) -> Unit,
        onProfileClick: () -> Unit,
        onUnitClick: () -> Unit,
        onSeeAllInvoicesClick: () -> Unit = {},
        onDecisionsClick: () -> Unit = {},
        onBillboardClick: () -> Unit = {},
        viewModel: DashboardViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color(0xFFF7F9FB), // pro-surface
            bottomBar = {
                BottomNavBar(onProfileClick = onProfileClick)
            }
    ) { paddingValues ->
        if (uiState.isLoading && uiState.userName.isEmpty()) {
            Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(
                modifier =
                        Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                HeaderSection(
                        userName = uiState.userName,
                        building = uiState.userBuilding,
                        apartmentUnit = uiState.userApartment,
                        hasMultipleUnits = uiState.hasMultipleUnits,
                        onProfileClick = onProfileClick,
                        onUnitClick = onUnitClick
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                BillingCard(
                        totalDebt = uiState.totalDebt,
                        pendingInvoices = uiState.pendingInvoices,
                        onSeeAllClick = onSeeAllInvoicesClick
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            uiState.pettyCashBalance?.let { balance ->
                item {
                    PettyCashBalanceCard(
                            amount = balance.currentBalance,
                            currency = balance.currency
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            item {
                QuickActions(
                        onPayClick = onPayClick,
                        onHistoryClick = onHistoryClick,
                        onDecisionsClick = onDecisionsClick,
                        onBillboardClick = onBillboardClick,
                        unreadAnnouncementsCount = uiState.unreadAnnouncementsCount
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                            text = stringResource(Res.string.recent_transactions),
                            style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp
                            ),
                            color = Color(0xFF09151A)
                    )
                    Text(
                            text = stringResource(Res.string.see_all),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFF6B00), // brand orange
                            modifier = Modifier.clickable { onHistoryClick() }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(uiState.recentPayments) { payment ->
                TransactionItem(payment = payment, onClick = { onPaymentClick(payment.id) })
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun BillingCard(
        totalDebt: Double,
        pendingInvoices: List<com.example.condominio.data.model.Invoice>,
        onSeeAllClick: () -> Unit = {}
) {
    val isSolvent = totalDebt <= 0
    val amountColor = if (isSolvent) Color(0xFF16A34A) else Color(0xFFDC2626) // pro-success or pro-danger

    Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(8.dp), // pro border radius
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF3F4F6)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = stringResource(Res.string.total_debt).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color(0xFF6B7280) // text-gray-500
                )
                Text(
                        text = stringResource(Res.string.see_all),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                        ),
                        color = Color(0xFF09151A), // dark text instead of blue
                        modifier = Modifier.clickable { onSeeAllClick() }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                    text = stringResource(Res.string.currency_amount, formatCurrency(totalDebt)),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp
                    ),
                    color = amountColor
            )
            
            if (totalDebt > 0) {
                 Text(
                        text = "¡Atención! Tienes saldo deudor pendiente.",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFDC2626), // pro-danger
                        modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            if (pendingInvoices.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFF3F4F6))
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                pendingInvoices.take(3).forEach { invoice ->
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = stringResource(Res.string.pending_invoices_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF6B7280) // text-gray-500
                            )
                            Text(
                                    text = invoice.description ?: invoice.period,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF09151A)
                            )
                        }
                        Text(
                                text = stringResource(Res.string.currency_amount, formatCurrency(invoice.remaining)),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFDC2626) // pro-danger
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                Text(
                        text = stringResource(Res.string.up_to_date),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = Color(0xFF16A34A) // pro-success
                )
            }
        }
    }
}

@Composable
fun HeaderSection(
        userName: String,
        building: String = "",
        apartmentUnit: String = "",
        hasMultipleUnits: Boolean = false,
        onProfileClick: () -> Unit = {},
        onUnitClick: () -> Unit = {}
) {
    Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = if (hasMultipleUnits) Modifier.clickable(onClick = onUnitClick) else Modifier
        ) {
            Text(
                    text = stringResource(Res.string.welcome_back),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFF6B7280) // gray-500
            )
            Text(
                    text = userName,
                    style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                    ),
                    color = Color(0xFF09151A)
            )
            if (building.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                            text = stringResource(Res.string.apt_unit_label, building, apartmentUnit),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFFFF6B00) // brand orange
                    )
                    if (hasMultipleUnits) {
                        Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = stringResource(Res.string.select_unit),
                                tint = Color(0xFFFF6B00),
                                modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        // Avatar - Clickable to open profile
        Surface(
                modifier = Modifier.size(48.dp).clickable(onClick = onProfileClick),
                shape = CircleShape,
                color = Color(0xFFFFF7ED), // orange-100
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFEDD5)) // orange-200
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (userName.isNotEmpty()) {
                    Text(
                            text = userName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFFEA580C) // orange-600
                    )
                } else {
                    Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = stringResource(Res.string.profile),
                            tint = Color(0xFFEA580C)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActions(
        onPayClick: () -> Unit,
        onHistoryClick: () -> Unit,
        onDecisionsClick: () -> Unit = {},
        onBillboardClick: () -> Unit = {},
        unreadAnnouncementsCount: Int = 0,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        QuickActionItem(
                icon = Icons.Default.Payment,
                label = stringResource(Res.string.pay_now),
                color = Color(0xFFFF6D00), // Orange
                onClick = onPayClick,
                modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        QuickActionItem(
                icon = Icons.Default.History,
                label = stringResource(Res.string.history),
                color = Color(0xFF0091EA), // Blue
                onClick = onHistoryClick,
                modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        QuickActionItem(
                icon = Icons.Default.AccountBalanceWallet,
                label = stringResource(Res.string.decisions_title),
                color = Color(0xFF0D47A1), // Deep Blue
                onClick = onDecisionsClick,
                modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        QuickActionItem(
                icon = Icons.Default.Campaign,
                label = "Cartelera",
                color = Color(0xFF00897B), // Teal
                onClick = onBillboardClick,
                modifier = Modifier.weight(1f),
                badgeCount = unreadAnnouncementsCount
        )
    }
}

@Composable
fun PettyCashBalanceCard(amount: Double, currency: String) {
    val isNegative = amount < 0
    val amountColor = if (isNegative) Color(0xFFDC2626) else Color(0xFF09151A)
    val iconTint = Color(0xFFEC4899) // pink-500
    val iconBg = Color(0xFFFDF2F8) // pink-50

    Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF3F4F6)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                    modifier = Modifier
                            .size(48.dp)
                            .background(iconBg, CircleShape),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = stringResource(Res.string.petty_cash),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF374151) // gray-700
                )
                Text(
                        text = stringResource(Res.string.available_balance),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9CA3AF) // gray-400
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                        text = stringResource(Res.string.currency_amount, formatCurrency(amount)),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = amountColor
                )
                Text(
                        text = currency,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF9CA3AF) // gray-400
                )
            }
        }
    }
}

@Composable
fun QuickActionItem(
        icon: ImageVector,
        label: String,
        color: Color,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        badgeCount: Int = 0,
) {
    Column(
            modifier = modifier.clickable(onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BadgedBox(
                badge = {
                    if (badgeCount > 0) {
                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                            Text(
                                    text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }
        ) {
            Box(
                    modifier =
                            Modifier.size(56.dp)
                                    .background(color.copy(alpha = 0.1f), CircleShape)
                                    .border(1.dp, color.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
            ) { Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp)) }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified),
                color = Color(0xFF4B5563), // gray-600
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun TransactionItem(payment: Payment, onClick: () -> Unit) {
    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .background(
                                    Color.White, // pro-container
                                    RoundedCornerShape(8.dp) // pro border radius
                            )
                            .border(1.dp, Color(0xFFF9FAFB), RoundedCornerShape(8.dp)) // border-gray-50
                            .clickable(onClick = onClick)
                            .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
                modifier =
                        Modifier.size(48.dp)
                                .background(
                                        Color(0xFFFFF7ED), // orange-50
                                        RoundedCornerShape(8.dp)
                                ),
                contentAlignment = Alignment.Center
        ) {
            Icon(
                    imageVector = Icons.Default.Payment,
                    contentDescription = null,
                    tint = Color(0xFFEA580C) // orange-600
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                    text = payment.description.ifBlank { stringResource(Res.string.payment_item_label) },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF09151A),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                    text = formatDate(payment.date),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFF9CA3AF) // gray-400
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                val statusLabel = when (payment.status) {
                    PaymentStatus.APPROVED -> stringResource(Res.string.status_approved_upper)
                    PaymentStatus.REJECTED -> stringResource(Res.string.status_rejected_upper)
                    PaymentStatus.PENDING -> stringResource(Res.string.status_pending)
                }
                
                val statusBg = when (payment.status) {
                    PaymentStatus.APPROVED -> Color(0xFFF0FDF4) // green-50
                    PaymentStatus.REJECTED -> Color(0xFFFEF2F2) // red-50
                    PaymentStatus.PENDING -> Color(0xFFFEFCE8) // yellow-50
                }
                
                val statusColor = when (payment.status) {
                    PaymentStatus.APPROVED -> Color(0xFF16A34A) // pro-success
                    PaymentStatus.REJECTED -> Color(0xFFDC2626) // pro-danger
                    PaymentStatus.PENDING -> Color(0xFFD97706) // yellow-600
                }

                Box(
                    modifier = Modifier.background(statusBg, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold, 
                                fontSize = 9.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = statusColor
                    )
                }
                
                if (payment.status != PaymentStatus.PENDING && !payment.processorName.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                            text = stringResource(Res.string.processed_by_short, payment.processorName),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 9.sp
                            ),
                            color = Color(0xFF9CA3AF),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
        Text(
                text = "$${formatCurrency(payment.amount)}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = Color(0xFF09151A)
        )
    }
}

@Composable
fun BottomNavBar(
    onProfileClick: () -> Unit = {},
) {
    val activeColor = Color(0xFFFF6B00) // brand orange
    val inactiveColor = Color(0xFF9CA3AF) // gray-400

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF3F4F6))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Inicio (active)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { }
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = "Inicio",
                    tint = activeColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Inicio",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = activeColor
                )
            }

            // Alertas
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Alertas",
                    tint = inactiveColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Alertas",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = inactiveColor
                )
            }

            // Perfil
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(onClick = onProfileClick)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Perfil",
                    tint = inactiveColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Perfil",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = inactiveColor
                )
            }

            // Más
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { }
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Más",
                    tint = inactiveColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Más",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = inactiveColor
                )
            }
        }
    }
}
