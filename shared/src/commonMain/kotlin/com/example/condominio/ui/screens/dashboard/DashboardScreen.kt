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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.example.condominio.ui.components.ErrorState
import com.example.condominio.ui.components.LoadingState
import com.example.condominio.ui.components.WebCard
import com.example.condominio.ui.components.WebEmptyFrame
import com.example.condominio.ui.screens.payment.formatPaymentDate
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
import com.example.condominio.ui.theme.onStatusContainer
import com.example.condominio.ui.theme.TableHeaderBg
import com.example.condominio.ui.theme.statCardBg
import com.example.condominio.ui.theme.statCardBorder
import org.koin.compose.viewmodel.koinViewModel
import com.example.condominio.ui.utils.formatCurrency
import org.jetbrains.compose.resources.stringResource
import condominio.shared.generated.resources.*

@Suppress("NAME_SHADOWING")
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
    // Reload when coming back from a child screen or from background; opening and rotation don't (A-06, A-19)
    com.example.condominio.ui.utils.OnScreenReturn(viewModel::onScreenReturn, viewModel::onBackgroundReturn)
    // Every forward navigation tells the ViewModel first, so the next re-entry knows it is a real return.
    val onPayClick = { viewModel.onOpenChild(); onPayClick() }
    val onHistoryClick = { viewModel.onOpenChild(); onHistoryClick() }
    val onPaymentClick = { id: String -> viewModel.onOpenChild(); onPaymentClick(id) }
    val onProfileClick = { viewModel.onOpenChild(); onProfileClick() }
    val onUnitClick = { viewModel.onOpenChild(); onUnitClick() }
    val onSeeAllInvoicesClick = { viewModel.onOpenChild(); onSeeAllInvoicesClick() }
    val onDecisionsClick = { viewModel.onOpenChild(); onDecisionsClick() }
    val onBillboardClick = { viewModel.onOpenChild(); onBillboardClick() }

    Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = screenBackground(Color(0xFFF7F9FB)) // pro-surface
    ) { paddingValues ->
        if (uiState.isLoading && uiState.userName.isEmpty()) {
            LoadingState(modifier = Modifier.padding(paddingValues))
            return@Scaffold
        }
        // Nothing loaded and the user/unit request failed: error with retry, not a blank screen (A-07).
        val error = uiState.error
        if (error != null && uiState.userName.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(paddingValues)) {
                WebEmptyFrame { ErrorState(title = error.asString(), onRetry = viewModel::refresh) }
            }
            return@Scaffold
        }
        val balance = BalanceUi(
                totalDebt = uiState.totalDebt,
                loading = uiState.totalDebt == null && !uiState.balanceFailed && !uiState.unitId.isNullOrEmpty(),
                failed = uiState.totalDebt == null && uiState.balanceFailed,
                onRetry = viewModel::refresh
        )
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
                        balance = balance,
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

        val historyLabel = stringResource(Res.string.history)
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
                            modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clickable(onClickLabel = historyLabel, role = Role.Button) { onHistoryClick() }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(uiState.recentPayments, key = { it.id }) { payment ->
                TransactionItem(payment = payment, onClick = { onPaymentClick(payment.id) })
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (uiState.recentPayments.isEmpty() && !uiState.isLoading) {
                item {
                    if (uiState.paymentsFailed) {
                        ErrorState(title = stringResource(Res.string.error_loading_payments), onRetry = viewModel::refresh)
                    } else {
                        Text(
                                text = stringResource(Res.string.web_dashboard_no_transactions),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                        )
                    }
                }
            }
        }

        if (isExpanded()) {
            // Web dashboard: greeting, KPI cards, action cards and a transactions table in one scroll.
            DashboardWebLayout(
                    uiState = uiState,
                    balance = balance,
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

/** Balance as the cards show it: [totalDebt] is null while unknown (loading or failed), never "al día" (A-05). */
class BalanceUi(
        val totalDebt: Double?,
        val loading: Boolean,
        val failed: Boolean,
        val onRetry: () -> Unit
) {
    val isSolvent: Boolean get() = totalDebt != null && totalDebt <= 0
}

/** Amount placeholder while the balance is unknown, or its error with retry. */
@Composable
private fun BalanceUnknown(balance: BalanceUi) {
    if (balance.failed) {
        Column(Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
            Text(
                    text = stringResource(Res.string.error_loading_balance),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.error
            )
            TextButton(onClick = balance.onRetry, contentPadding = PaddingValues(0.dp)) {
                Text(stringResource(Res.string.retry))
            }
        }
    } else if (balance.loading) {
        LoadingState(fullScreen = false, size = 24.dp, strokeWidth = 3.dp)
    }
}

@Composable
fun BillingCard(
        balance: BalanceUi,
        pendingInvoices: List<com.example.condominio.data.model.Invoice>,
        onSeeAllClick: () -> Unit = {}
) {
    val totalDebt = balance.totalDebt
    val isSolvent = balance.isSolvent
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
                        text = stringResource(Res.string.see_invoices),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clickable(role = Role.Button) { onSeeAllClick() }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (totalDebt == null) {
                BalanceUnknown(balance)
                return@Column
            }
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
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(Res.string.pending_invoices_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                    text = invoice.description ?: invoice.period,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                                text = stringResource(Res.string.currency_amount, formatCurrency(invoice.remaining)),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else if (isSolvent) {
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
            modifier = Modifier.weight(1f).then(if (hasMultipleUnits) Modifier.clickable(onClick = onUnitClick) else Modifier)
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
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
            if (building.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                            text = stringResource(Res.string.apt_unit_label, building, apartmentUnit),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
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
        Spacer(modifier = Modifier.width(16.dp))
        // Avatar - Clickable to open profile; announced as "Perfil", not as the initial (E-D2)
        val profileLabel = stringResource(Res.string.profile)
        Surface(
                modifier = Modifier.size(48.dp).clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onProfileClick)
                        .clearAndSetSemantics { contentDescription = profileLabel },
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
                            contentDescription = null,
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
                label = stringResource(Res.string.billboard_title),
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
            modifier = modifier.clickable(role = Role.Button, onClick = onClick),
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
            // The label below already names the action: no contentDescription, or it is read twice (E-D2)
            ) { Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp)) }
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
                    text = formatPaymentDate(payment.date),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.outline
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                PaymentStatusBadge(payment.status)

                if (payment.status != PaymentStatus.PENDING && !payment.processorName.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                            text = stringResource(Res.string.processed_by_short, payment.processorName),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
        Text(
                text = stringResource(Res.string.currency_amount, formatCurrency(payment.amount)),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun paymentStatusLabel(status: PaymentStatus): String = when (status) {
    PaymentStatus.APPROVED -> stringResource(Res.string.status_approved_upper).uppercase()
    PaymentStatus.REJECTED -> stringResource(Res.string.status_rejected_upper).uppercase()
    PaymentStatus.PENDING -> stringResource(Res.string.status_pending).uppercase()
}

// labelSmall size (was a fixed 9sp, unreadable) (E-D4)
@Composable
private fun paymentStatusBadgeStyle(): TextStyle =
        MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)

private val BadgeHorizontalPadding = 6.dp

/** Status pill shared by the mobile transaction item and the web transactions table. */
@Composable
private fun PaymentStatusBadge(status: PaymentStatus) {
    val statusLabel = paymentStatusLabel(status)

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
            .padding(horizontal = BadgeHorizontalPadding, vertical = 2.dp)
    ) {
        Text(
                text = statusLabel,
                style = paymentStatusBadgeStyle(),
                color = onStatusContainer(statusColor),
                maxLines = 1,
                softWrap = false
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
        balance: BalanceUi,
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
                        balance = balance,
                        pendingInvoices = uiState.pendingInvoices,
                        pettyCash = uiState.pettyCashBalance?.let { it.currentBalance to it.currency },
                        onSeeAllInvoicesClick = onSeeAllInvoicesClick
                )
                WebTransactionsTable(
                        payments = uiState.recentPayments,
                        loading = uiState.isLoading,
                        failed = uiState.paymentsFailed,
                        onRetry = balance.onRetry,
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
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
        }

        if (building.isNotEmpty()) {
            // Unit selector: a pill that opens the unit picker when the user owns several units.
            val unitModifier = if (hasMultipleUnits) HandCursor.clickable(onClick = onUnitClick) else Modifier
            Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AptoSurfaceContainerLowest,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AptoOutlineVariant),
                    // Capped so a long building name can't squeeze the greeting out (E-D5).
                    modifier = Modifier.widthIn(max = 360.dp).clip(RoundedCornerShape(12.dp)).then(unitModifier)
            ) {
                Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                            text = stringResource(Res.string.apt_unit_label, building, apartmentUnit),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
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

        val profileLabel = stringResource(Res.string.profile)
        Surface(
                modifier = Modifier.size(44.dp).clip(CircleShape).then(HandCursor)
                        .clickable(role = Role.Button, onClick = onProfileClick)
                        // Announced as "Perfil", not as the initial (E-D2).
                        .clearAndSetSemantics { contentDescription = profileLabel },
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
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun WebKpiRow(
        balance: BalanceUi,
        pendingInvoices: List<com.example.condominio.data.model.Invoice>,
        pettyCash: Pair<Double, String>?,
        onSeeAllInvoicesClick: () -> Unit
) {
    val totalDebt = balance.totalDebt
    val isSolvent = balance.isSolvent
    // Both cards come from the balance: while it is unknown, a dash and the loading/error footer (A-05).
    val unknownValue = "—"
    val cards = mutableListOf<@Composable (Modifier) -> Unit>()
    cards += { cardModifier ->
        WebKpiCard(
                modifier = cardModifier,
                icon = Icons.Default.AccountBalanceWallet,
                iconTint = AptoSecondaryContainer,
                label = stringResource(Res.string.total_debt),
                value = totalDebt?.let { stringResource(Res.string.currency_amount, formatCurrency(it)) } ?: unknownValue,
                valueColor = if (isSolvent) AptoSuccess else MaterialTheme.colorScheme.onSurface,
                footer = {
                    if (totalDebt == null) {
                        BalanceUnknown(balance)
                    } else if (isSolvent) {
                        Text(
                                text = stringResource(Res.string.up_to_date).emojiSafe(),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = AptoSuccess
                        )
                    } else {
                        WebLink(stringResource(Res.string.see_invoices), onSeeAllInvoicesClick)
                    }
                }
        )
    }

    val invoicesKnown = totalDebt != null
    cards += { cardModifier ->
        WebKpiCard(
                modifier = cardModifier,
                icon = Icons.Default.Receipt,
                iconTint = when {
                    !invoicesKnown -> MaterialTheme.colorScheme.onSurfaceVariant
                    pendingInvoices.isEmpty() -> AptoSuccess
                    else -> MaterialTheme.colorScheme.error
                },
                label = stringResource(Res.string.pending_invoices_label).trimEnd(':', ' '),
                value = if (invoicesKnown) pendingInvoices.size.toString() else unknownValue,
                valueColor = if (invoicesKnown && pendingInvoices.isEmpty()) AptoSuccess else MaterialTheme.colorScheme.onSurface,
                footer = {
                    if (!invoicesKnown) {
                        // Loading/error is already shown in the total-debt card next to it.
                    } else if (pendingInvoices.isEmpty()) {
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
    }

    pettyCash?.let { (amount, currency) ->
        cards += { cardModifier ->
            WebKpiCard(
                    modifier = cardModifier,
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

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // Side by side each card needs ~200dp for its label and amount; narrower (e.g. an 800px window) they
        // stack instead of breaking words mid-way.
        if (maxWidth < KpiRowMinWidth) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                cards.forEach { it(Modifier.fillMaxWidth()) }
            }
        } else {
            Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                cards.forEach { it(Modifier.weight(1f).fillMaxHeight()) }
            }
        }
    }
}

private val KpiRowMinWidth = 640.dp

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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
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
private fun WebLink(text: String, onClick: () -> Unit, onClickLabel: String? = null) {
    Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = AptoSecondaryContainer,
            modifier = Modifier.minimumInteractiveComponentSize().then(HandCursor)
                    .clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
    )
}

@Composable
private fun WebTransactionsTable(
        payments: List<Payment>,
        loading: Boolean,
        failed: Boolean,
        onRetry: () -> Unit,
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
            WebLink(stringResource(Res.string.see_all), onSeeAllClick, onClickLabel = stringResource(Res.string.history))
        }

        // Date, amount and status columns are as wide as their widest cell (they never wrap, e.g. in an 800px
        // window); the description takes the rest and ellipsizes.
        val dateHeader = stringResource(Res.string.date_label)
        val amountHeader = stringResource(Res.string.web_dashboard_amount)
        val statusHeader = stringResource(Res.string.web_dashboard_status)
        val dates = payments.map { formatPaymentDate(it.date) }
        val amounts = payments.map { stringResource(Res.string.currency_amount, formatCurrency(it.amount)) }
        val statusLabels = PaymentStatus.entries.map { paymentStatusLabel(it) }
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val headerStyle = webTableHeaderStyle()
        val badgeStyle = paymentStatusBadgeStyle()
        fun widthOf(texts: List<String>, style: TextStyle, extra: Dp = 0.dp): Dp = with(density) {
            texts.maxOf { measurer.measure(it, style, maxLines = 1, softWrap = false).size.width }.toDp() + extra + 1.dp
        }
        val dateWidth = maxOf(widthOf(listOf(dateHeader.uppercase()), headerStyle), widthOf(dates + "00/00/0000", MaterialTheme.typography.bodyMedium))
        val amountWidth = maxOf(widthOf(listOf(amountHeader.uppercase()), headerStyle), widthOf(amounts.ifEmpty { listOf("") }, WebAmountStyle()))
        val statusWidth = maxOf(widthOf(listOf(statusHeader.uppercase()), headerStyle), widthOf(statusLabels, badgeStyle, BadgeHorizontalPadding * 2))

        // Column header
        Row(
                modifier = Modifier.fillMaxWidth().background(TableHeaderBg).padding(horizontal = 24.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            WebTableHeader(dateHeader, Modifier.width(dateWidth))
            WebTableHeader(stringResource(Res.string.description_label), Modifier.weight(1f))
            WebTableHeader(amountHeader, Modifier.width(amountWidth), TextAlign.End)
            WebTableHeader(statusHeader, Modifier.width(statusWidth))
        }

        if (payments.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                when {
                    loading -> LoadingState(fullScreen = false)
                    // A failed load is not "no transactions" (A-07)
                    failed -> ErrorState(title = stringResource(Res.string.error_loading_payments), onRetry = onRetry)
                    else -> Text(
                            text = stringResource(Res.string.web_dashboard_no_transactions),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        payments.forEachIndexed { index, payment ->
            if (index > 0) HorizontalDivider(color = AptoOutlineVariant)
            Row(
                    modifier = Modifier.fillMaxWidth()
                            .then(HandCursor)
                            .clickable { onPaymentClick(payment.id) }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = dates[index],
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.width(dateWidth)
                )
                Column(Modifier.weight(1f)) {
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
                        text = amounts[index],
                        style = WebAmountStyle(),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.width(amountWidth)
                )
                Box(Modifier.width(statusWidth)) { PaymentStatusBadge(payment.status) }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun webTableHeaderStyle(): TextStyle =
        MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)

@Composable
private fun WebAmountStyle(): TextStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold)

@Composable
private fun WebTableHeader(text: String, modifier: Modifier, textAlign: TextAlign = TextAlign.Start) {
    Text(
            text = text.uppercase(),
            style = webTableHeaderStyle(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = textAlign,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
    )
}
