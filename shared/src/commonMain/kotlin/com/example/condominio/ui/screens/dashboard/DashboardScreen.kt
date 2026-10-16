package com.example.condominio.ui.screens.dashboard

import com.example.condominio.ui.utils.emojiSafe
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import com.example.condominio.ui.components.WebCard
import com.example.condominio.ui.components.isExpanded
import com.example.condominio.ui.components.pagePadding
import com.example.condominio.ui.components.screenBackground
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.condominio.ui.theme.AptoOutlineVariant
import com.example.condominio.ui.theme.AptoSecondaryContainer
import com.example.condominio.ui.theme.AptoSurfaceContainerLow
import com.example.condominio.ui.theme.AptoSurfaceContainerLowest
import com.example.condominio.data.model.Payment
import com.example.condominio.data.model.PaymentStatus
import androidx.compose.ui.graphics.Color
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
import androidx.compose.foundation.BorderStroke
import com.example.condominio.ui.utils.isWebPlatform
import com.example.condominio.ui.theme.badgeBg
import com.example.condominio.ui.theme.badgeShape
import com.example.condominio.ui.theme.badgeBorder
import com.example.condominio.ui.theme.TableHeaderBg
import com.example.condominio.ui.theme.statCardBg
import com.example.condominio.ui.theme.statCardBorder
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
            containerColor = screenBackground(Color(0xFFF7F9FB)) // pro-surface
    ) { paddingValues ->
        if (uiState.isLoading && uiState.userName.isEmpty()) {
            LoadingState(modifier = Modifier.padding(paddingValues))
            return@Scaffold
        }
        val summary: LazyListScope.() -> Unit = {
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
        }

        val transactions: LazyListScope.() -> Unit = {
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

        if (isExpanded()) {
            // Web dashboard: greeting, KPI cards, action cards and a transactions table in one scroll.
            DashboardWebLayout(
                    uiState = uiState,
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    onPayClick = onPayClick,
                    onHistoryClick = onHistoryClick,
                    onPaymentClick = onPaymentClick,
                    onProfileClick = onProfileClick,
                    onUnitClick = onUnitClick,
                    onSeeAllInvoicesClick = onSeeAllInvoicesClick,
                    onDecisionsClick = onDecisionsClick,
                    onBillboardClick = onBillboardClick
            )
        } else {
            LazyColumn(
                    Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = pagePadding()),
                    contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                summary()
                transactions()
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
                        text = stringResource(Res.string.up_to_date).emojiSafe(),
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
                modifier = Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onProfileClick),
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
                PaymentStatusBadge(payment.status)

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

/** Status pill shared by the mobile transaction item and the web transactions table. */
@Composable
private fun PaymentStatusBadge(status: PaymentStatus) {
    val statusLabel = when (status) {
        PaymentStatus.APPROVED -> stringResource(Res.string.status_approved_upper)
        PaymentStatus.REJECTED -> stringResource(Res.string.status_rejected_upper)
        PaymentStatus.PENDING -> stringResource(Res.string.status_pending)
    }

    val statusBg = when (status) {
        PaymentStatus.APPROVED -> AptoSuccessContainer
        PaymentStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
        PaymentStatus.PENDING -> AptoWarningContainer
    }

    val statusColor = when (status) {
        PaymentStatus.APPROVED -> AptoSuccess
        PaymentStatus.REJECTED -> MaterialTheme.colorScheme.error
        PaymentStatus.PENDING -> AptoWarning
    }

    Box(
        modifier = Modifier
            .background(badgeBg(statusBg, statusColor), badgeShape(RoundedCornerShape(4.dp)))
            .then(badgeBorder(statusColor)?.let { Modifier.border(it, CircleShape) } ?: Modifier)
            .padding(horizontal = 6.dp, vertical = 2.dp)
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
}

// ---------------------------------------------------------------------------------------------
// Expanded (desktop web) layout. Phone/tablet keep the LazyColumn above untouched.
// ---------------------------------------------------------------------------------------------

private val HandCursor = Modifier.pointerHoverIcon(PointerIcon.Hand)

@Composable
private fun DashboardWebLayout(
        uiState: DashboardUiState,
        modifier: Modifier,
        onPayClick: () -> Unit,
        onHistoryClick: () -> Unit,
        onPaymentClick: (String) -> Unit,
        onProfileClick: () -> Unit,
        onUnitClick: () -> Unit,
        onSeeAllInvoicesClick: () -> Unit,
        onDecisionsClick: () -> Unit,
        onBillboardClick: () -> Unit
) {
    Column(
            modifier = modifier
                    .verticalScroll(rememberScrollState())
                    // Greeting doubles as the page header: same left edge and top (24dp) as TopBarWithBack.
                    .padding(start = pagePadding(), end = pagePadding(), top = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        WebGreetingRow(
                userName = uiState.userName,
                building = uiState.userBuilding,
                apartmentUnit = uiState.userApartment,
                hasMultipleUnits = uiState.hasMultipleUnits,
                onProfileClick = onProfileClick,
                onUnitClick = onUnitClick
        )

        // Main area (KPIs + activity) and a narrower side rail of actions: fills wide screens.
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(2.4f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                WebKpiRow(
                        totalDebt = uiState.totalDebt,
                        pendingInvoices = uiState.pendingInvoices,
                        pettyCash = uiState.pettyCashBalance?.let { it.currentBalance to it.currency },
                        onSeeAllInvoicesClick = onSeeAllInvoicesClick
                )
                WebTransactionsTable(
                        payments = uiState.recentPayments,
                        onPaymentClick = onPaymentClick,
                        onSeeAllClick = onHistoryClick
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                        text = stringResource(Res.string.quick_actions_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
                        color = MaterialTheme.colorScheme.onSurface
                )
                WebActionCard(Icons.Default.Payment, stringResource(Res.string.pay_now), AptoCategoryOrange, onPayClick, Modifier.fillMaxWidth())
                WebActionCard(Icons.Default.History, stringResource(Res.string.history), AptoCategoryBlue, onHistoryClick, Modifier.fillMaxWidth())
                WebActionCard(Icons.Default.AccountBalanceWallet, stringResource(Res.string.decisions_title), AptoCategoryLavender, onDecisionsClick, Modifier.fillMaxWidth())
                WebActionCard(
                        Icons.Default.Campaign,
                        stringResource(Res.string.billboard_title),
                        AptoCategoryGreen,
                        onBillboardClick,
                        Modifier.fillMaxWidth(),
                        badgeCount = uiState.unreadAnnouncementsCount
                )
            }
        }
    }
}

@Composable
private fun WebGreetingRow(
        userName: String,
        building: String,
        apartmentUnit: String,
        hasMultipleUnits: Boolean,
        onProfileClick: () -> Unit,
        onUnitClick: () -> Unit
) {
    Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                    text = stringResource(Res.string.welcome_back),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                    text = userName,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
                    color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (building.isNotEmpty()) {
            // Unit selector: a pill that opens the unit picker when the user owns several units.
            val unitModifier = if (hasMultipleUnits) HandCursor.clickable(onClick = onUnitClick) else Modifier
            Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AptoSurfaceContainerLowest,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AptoOutlineVariant),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).then(unitModifier)
            ) {
                Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                            text = stringResource(Res.string.apt_unit_label, building, apartmentUnit),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                    )
                    if (hasMultipleUnits) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = stringResource(Res.string.select_unit),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Surface(
                modifier = Modifier.size(44.dp).clip(CircleShape).then(HandCursor).clickable(onClick = onProfileClick),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (userName.isNotEmpty()) {
                    Text(
                            text = userName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = stringResource(Res.string.profile), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun WebKpiRow(
        totalDebt: Double,
        pendingInvoices: List<com.example.condominio.data.model.Invoice>,
        pettyCash: Pair<Double, String>?,
        onSeeAllInvoicesClick: () -> Unit
) {
    val isSolvent = totalDebt <= 0
    Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        WebKpiCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Icons.Default.AccountBalanceWallet,
                iconTint = AptoSecondaryContainer,
                label = stringResource(Res.string.total_debt),
                value = stringResource(Res.string.currency_amount, formatCurrency(totalDebt)),
                valueColor = if (isSolvent) AptoSuccess else MaterialTheme.colorScheme.onSurface,
                footer = {
                    if (isSolvent) {
                        Text(
                                text = stringResource(Res.string.up_to_date).emojiSafe(),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = AptoSuccess
                        )
                    } else {
                        WebLink(stringResource(Res.string.see_all), onSeeAllInvoicesClick)
                    }
                }
        )

        WebKpiCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Icons.Default.Receipt,
                iconTint = if (pendingInvoices.isEmpty()) AptoSuccess else MaterialTheme.colorScheme.error,
                label = stringResource(Res.string.pending_invoices_label).trimEnd(':', ' '),
                value = pendingInvoices.size.toString(),
                valueColor = if (pendingInvoices.isEmpty()) AptoSuccess else MaterialTheme.colorScheme.onSurface,
                footer = {
                    if (pendingInvoices.isEmpty()) {
                        Text(
                                text = stringResource(Res.string.up_to_date).emojiSafe(),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = AptoSuccess
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            pendingInvoices.take(3).forEach { invoice ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                            text = invoice.description ?: invoice.period,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                            text = stringResource(Res.string.currency_amount, formatCurrency(invoice.remaining)),
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
        )

        pettyCash?.let { (amount, currency) ->
            WebKpiCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    icon = Icons.Default.Savings,
                    iconTint = AptoPettyCashAccent,
                    iconBackground = AptoPettyCashAccentContainer,
                    label = stringResource(Res.string.petty_cash),
                    value = stringResource(Res.string.currency_amount, formatCurrency(amount)),
                    valueColor = if (amount < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    footer = {
                        Text(
                                text = stringResource(Res.string.available_balance) + " · " + currency,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.outline
                        )
                    }
            )
        }
    }
}

@Composable
private fun WebKpiCard(
        modifier: Modifier,
        icon: ImageVector,
        iconTint: Color,
        label: String,
        value: String,
        valueColor: Color,
        iconBackground: Color = iconTint.copy(alpha = 0.1f),
        footer: @Composable () -> Unit
) {
    WebCard(
        modifier = modifier,
        color = statCardBg(AptoSurfaceContainerLowest),
        border = statCardBorder(BorderStroke(1.dp, AptoOutlineVariant)),
        shadowElevation = if (isWebPlatform) 2.dp else 0.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                    modifier = Modifier.size(36.dp).background(iconBackground, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(12.dp))
            Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp),
                color = valueColor
        )
        Spacer(Modifier.height(12.dp))
        footer()
    }
}

@Composable
private fun WebActionCard(
        icon: ImageVector,
        label: String,
        color: Color,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        badgeCount: Int = 0
) {
    Surface(
            modifier = modifier.clip(RoundedCornerShape(16.dp)).then(HandCursor).clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            color = AptoSurfaceContainerLowest,
            border = androidx.compose.foundation.BorderStroke(1.dp, AptoOutlineVariant)
    ) {
        Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                    modifier = Modifier.size(44.dp)
                            .background(color.copy(alpha = 0.1f), CircleShape)
                            .border(1.dp, color.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(14.dp))
            Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
            )
            if (badgeCount > 0) {
                Spacer(Modifier.width(8.dp))
                Badge(containerColor = MaterialTheme.colorScheme.error) {
                    Text(
                            text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }
    }
}

@Composable
private fun WebLink(text: String, onClick: () -> Unit) {
    Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = AptoSecondaryContainer,
            modifier = HandCursor.clickable(onClick = onClick)
    )
}

@Composable
private fun WebTransactionsTable(
        payments: List<Payment>,
        onPaymentClick: (String) -> Unit,
        onSeeAllClick: () -> Unit
) {
    WebCard(contentPadding = 0.dp) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                    text = stringResource(Res.string.recent_transactions),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
                    color = MaterialTheme.colorScheme.onSurface
            )
            WebLink(stringResource(Res.string.see_all), onSeeAllClick)
        }

        // Column header
        Row(
                modifier = Modifier.fillMaxWidth().background(TableHeaderBg).padding(horizontal = 24.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            WebTableHeader(stringResource(Res.string.date_label), Modifier.weight(1.2f))
            WebTableHeader(stringResource(Res.string.description_label), Modifier.weight(3f))
            WebTableHeader(stringResource(Res.string.web_dashboard_amount), Modifier.weight(1.3f), TextAlign.End)
            Spacer(Modifier.width(24.dp))
            WebTableHeader(stringResource(Res.string.web_dashboard_status), Modifier.weight(1.3f))
        }

        if (payments.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                Text(
                        text = stringResource(Res.string.web_dashboard_no_transactions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        payments.forEachIndexed { index, payment ->
            if (index > 0) HorizontalDivider(color = AptoOutlineVariant)
            Row(
                    modifier = Modifier.fillMaxWidth()
                            .then(HandCursor)
                            .clickable { onPaymentClick(payment.id) }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = formatDate(payment.date),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1.2f)
                )
                Column(Modifier.weight(3f)) {
                    Text(
                            text = payment.description.ifBlank { stringResource(Res.string.payment_item_label) },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                    )
                    if (payment.status != PaymentStatus.PENDING && !payment.processorName.isNullOrEmpty()) {
                        Text(
                                text = stringResource(Res.string.processed_by_short, payment.processorName),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                        text = stringResource(Res.string.currency_amount, formatCurrency(payment.amount)),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.3f)
                )
                Spacer(Modifier.width(24.dp))
                Box(Modifier.weight(1.3f)) { PaymentStatusBadge(payment.status) }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun WebTableHeader(text: String, modifier: Modifier, textAlign: TextAlign = TextAlign.Start) {
    Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = textAlign,
            modifier = modifier
    )
}
