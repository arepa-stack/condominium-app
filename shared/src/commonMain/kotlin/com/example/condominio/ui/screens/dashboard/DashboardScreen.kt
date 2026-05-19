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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.condominio.ui.components.LoadingState
import com.example.condominio.data.model.Payment
import com.example.condominio.data.model.PaymentStatus
import com.example.condominio.ui.theme.AptoCategoryBlue
import com.example.condominio.ui.theme.AptoCategoryGreen
import com.example.condominio.ui.theme.AptoCategoryLavender
import com.example.condominio.ui.theme.AptoCategoryOrange
import com.example.condominio.ui.theme.AptoPettyCashAccent
import com.example.condominio.ui.theme.AptoPettyCashAccentContainer
import com.example.condominio.ui.theme.AptoSuccess
import com.example.condominio.ui.theme.AptoSuccessContainer
import com.example.condominio.ui.theme.AptoWarning
import com.example.condominio.ui.theme.AptoWarningContainer
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
            containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (uiState.isLoading && uiState.userName.isEmpty()) {
            LoadingState(modifier = Modifier.padding(paddingValues))
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
                            color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                            text = stringResource(Res.string.see_all),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
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
    val amountColor = if (isSolvent) AptoSuccess else MaterialTheme.colorScheme.onSurface

    Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                        text = stringResource(Res.string.see_all),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
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

            Spacer(modifier = Modifier.height(24.dp))

            if (pendingInvoices.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                    text = invoice.description ?: invoice.period,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                                text = stringResource(Res.string.currency_amount, formatCurrency(invoice.remaining)),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                Text(
                        text = stringResource(Res.string.up_to_date),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = AptoSuccess
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                    text = userName,
                    style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
            )
            if (building.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                            text = stringResource(Res.string.apt_unit_label, building, apartmentUnit),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                    )
                    if (hasMultipleUnits) {
                        Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = stringResource(Res.string.select_unit),
                                tint = MaterialTheme.colorScheme.primary,
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
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (userName.isNotEmpty()) {
                    Text(
                            text = userName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = stringResource(Res.string.profile),
                            tint = MaterialTheme.colorScheme.primary
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
                color = AptoCategoryOrange,
                onClick = onPayClick,
                modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        QuickActionItem(
                icon = Icons.Default.History,
                label = stringResource(Res.string.history),
                color = AptoCategoryBlue,
                onClick = onHistoryClick,
                modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        QuickActionItem(
                icon = Icons.Default.AccountBalanceWallet,
                label = stringResource(Res.string.decisions_title),
                color = AptoCategoryLavender,
                onClick = onDecisionsClick,
                modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        QuickActionItem(
                icon = Icons.Default.Campaign,
                label = "Cartelera",
                color = AptoCategoryGreen,
                onClick = onBillboardClick,
                modifier = Modifier.weight(1f),
                badgeCount = unreadAnnouncementsCount
        )
    }
}

@Composable
fun PettyCashBalanceCard(amount: Double, currency: String) {
    val isNegative = amount < 0
    val amountColor = if (isNegative) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                            .background(AptoPettyCashAccentContainer, CircleShape),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = null,
                        tint = AptoPettyCashAccent,
                        modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = stringResource(Res.string.petty_cash),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                        text = stringResource(Res.string.available_balance),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
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
                        color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun QuickActionItem(
        icon: ImageVector,
        label: String,
        color: androidx.compose.ui.graphics.Color,
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(8.dp)
                            )
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .clickable(onClick = onClick)
                            .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
                modifier =
                        Modifier.size(48.dp)
                                .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        RoundedCornerShape(8.dp)
                                ),
                contentAlignment = Alignment.Center
        ) {
            Icon(
                    imageVector = Icons.Default.Payment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                    text = payment.description.ifBlank { stringResource(Res.string.payment_item_label) },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                    text = formatDate(payment.date),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.outline
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                val statusLabel = when (payment.status) {
                    PaymentStatus.APPROVED -> stringResource(Res.string.status_approved_upper)
                    PaymentStatus.REJECTED -> stringResource(Res.string.status_rejected_upper)
                    PaymentStatus.PENDING -> stringResource(Res.string.status_pending)
                }

                val statusBg = when (payment.status) {
                    PaymentStatus.APPROVED -> AptoSuccessContainer
                    PaymentStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
                    PaymentStatus.PENDING -> AptoWarningContainer
                }

                val statusColor = when (payment.status) {
                    PaymentStatus.APPROVED -> AptoSuccess
                    PaymentStatus.REJECTED -> MaterialTheme.colorScheme.error
                    PaymentStatus.PENDING -> AptoWarning
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
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
        Text(
                text = "$${formatCurrency(payment.amount)}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
        )
    }
}

