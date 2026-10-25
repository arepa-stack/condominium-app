package com.example.condominio.ui.screens.payment

import com.example.condominio.ui.components.WebEmptyFrame
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.condominio.data.model.Payment
import com.example.condominio.data.model.PaymentMethod
import com.example.condominio.data.model.PaymentStatus
import com.example.condominio.ui.components.EmptyState
import com.example.condominio.ui.components.ErrorState
import com.example.condominio.ui.components.ListItemCard
import com.example.condominio.ui.components.LoadingState
import com.example.condominio.ui.components.StatusBadge
import com.example.condominio.ui.components.TopBarWithBack
import com.example.condominio.ui.components.WebCard
import com.example.condominio.ui.components.isExpanded
import com.example.condominio.ui.components.isWide
import com.example.condominio.ui.components.pagePadding
import com.example.condominio.ui.components.screenBackground
import com.example.condominio.ui.theme.*
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.condominio.ui.utils.formatCurrency
import com.example.condominio.ui.utils.formatDate
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import com.example.condominio.ui.utils.OnScreenReturn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentHistoryScreen(
    onBackClick: () -> Unit,
    onPaymentClick: (String) -> Unit,
    viewModel: PaymentHistoryViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    // Reload when coming back from a payment detail or from background; opening and rotation don't (A-19)
    OnScreenReturn(viewModel::onScreenReturn, viewModel::onBackgroundReturn)
    val openPayment: (String) -> Unit = { id -> viewModel.onOpenChild(); onPaymentClick(id) }

    Scaffold(
        topBar = {
            TopBarWithBack(
                title = stringResource(Res.string.payment_history),
                onBackClick = onBackClick
            )
        },
        containerColor = screenBackground(AptoBackground)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.payments.isEmpty() -> {
                    LoadingState()
                }
                // A failed load is not "no payments" (A-07)
                uiState.loadFailed && uiState.payments.isEmpty() -> WebEmptyFrame {
                    ErrorState(
                        title = stringResource(Res.string.error_loading_payments),
                        onRetry = viewModel::loadPayments,
                    )
                }
                uiState.payments.isEmpty() -> WebEmptyFrame {
                    EmptyState(
                        title = stringResource(Res.string.payment_history_empty),
                        icon = Icons.Default.ReceiptLong,
                    )
                }
                isExpanded() -> {
                    PaymentHistoryTable(
                        unit = uiState.unit,
                        payments = uiState.payments,
                        onPaymentClick = openPayment
                    )
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(340.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = pagePadding()),
                        // Medium (tablet): start 8dp under the page header like the other pages;
                        // phone keeps its original 24dp.
                        contentPadding = if (isWide()) {
                            PaddingValues(top = 8.dp, bottom = 24.dp)
                        } else {
                            PaddingValues(vertical = 24.dp)
                        },
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                                if (uiState.unit.isNotEmpty()) {
                                    Text(
                                        text = stringResource(Res.string.unit, uiState.unit).uppercase(),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = AptoOnSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                                Text(
                                    text = stringResource(Res.string.recent_transactions),
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AptoPrimary,
                                    modifier = Modifier.semantics { heading() }
                                )
                            }
                        }

                        items(uiState.payments) { payment ->
                            PaymentHistoryCard(
                                payment = payment,
                                onClick = { openPayment(payment.id) }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                             Spacer(modifier = Modifier.height(80.dp)) // Safe padding for global bottom bar if any
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentHistoryCard(
    payment: Payment,
    onClick: () -> Unit
) {
    // Formatter logic local to component for KMP safety
    val formattedDate = formatPaymentDate(payment.date)

    val iconData = when (payment.method) {
        PaymentMethod.PAGO_MOVIL -> Pair(Icons.Default.Payments, AptoSecondary)
        PaymentMethod.TRANSFER -> Pair(Icons.Default.AccountBalanceWallet, AptoSecondary)
        PaymentMethod.CASH -> Pair(Icons.Default.HomeWork, AptoSecondary)
    }

    val statusData = paymentStatusData(payment.status)

    val iconBgColor = if (payment.status == PaymentStatus.REJECTED) AptoSurfaceContainerHigh else AptoSecondaryFixed

    ListItemCard(
        onClick = onClick,
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(iconBgColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (payment.status == PaymentStatus.REJECTED) Icons.Default.ReceiptLong else iconData.first,
                contentDescription = null,
                tint = if (payment.status == PaymentStatus.REJECTED) AptoOnSurfaceVariant else iconData.second,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = payment.description.ifBlank { stringResource(Res.string.payment_default_desc) },
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = AptoPrimary,
                        modifier = Modifier.padding(bottom = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AptoOnSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                Text(
                    text = stringResource(Res.string.currency_amount, formatCurrency(payment.amount)),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = AptoPrimary,
                    maxLines = 1
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                StatusBadge(
                    text = stringResource(statusData.first),
                    statusColor = statusData.second
                )
                Spacer(modifier = Modifier.width(8.dp))
                val extraText = when {
                    !payment.processorName.isNullOrBlank() ->
                        stringResource(Res.string.processed_by_short, payment.processorName).trimStart()
                    else -> "· ${paymentMethodLabel(payment.method)}"
                }
                Text(
                    text = extraText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = AptoOutline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Same numeric date in every payment screen and language (C-08), as in the invoice detail. */
internal fun formatPaymentDate(epochMillis: Long): String = formatDate(epochMillis, "dd/MM/yyyy")

/** Localized method name (the enum label is Spanish only). */
@Composable
internal fun paymentMethodLabel(method: PaymentMethod): String = stringResource(
    when (method) {
        PaymentMethod.PAGO_MOVIL -> Res.string.method_pago_movil
        PaymentMethod.TRANSFER -> Res.string.method_transfer
        PaymentMethod.CASH -> Res.string.method_cash
    }
)

private fun paymentStatusData(status: PaymentStatus) = when (status) {
    PaymentStatus.APPROVED -> Pair(Res.string.status_approved, AptoStatusSuccess)
    PaymentStatus.REJECTED -> Pair(Res.string.status_rejected, AptoStatusError)
    PaymentStatus.PENDING -> Pair(Res.string.status_pending, AptoStatusWarning)
}

// Column weights shared by the header and the rows of the web table.
private const val COL_DATE = 1.6f
private const val COL_METHOD = 1.3f
private const val COL_REFERENCE = 1.3f
private const val COL_AMOUNT = 1f
private const val COL_STATUS = 1.1f

/** Expanded (desktop web) layout: page subtitle + payments table inside a [WebCard]. */
@Composable
private fun PaymentHistoryTable(
    unit: String,
    payments: List<Payment>,
    onPaymentClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = pagePadding()),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column {
                if (unit.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.unit, unit).uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AptoOnSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Text(
                    text = stringResource(Res.string.recent_transactions),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = AptoOnSurface,
                    modifier = Modifier.semantics { heading() }
                )
            }
        }
        item {
            WebCard(contentPadding = 0.dp) {
                PaymentTableHeader()
                payments.forEachIndexed { index, payment ->
                    if (index > 0) HorizontalDivider(color = AptoOutlineVariant)
                    PaymentTableRow(payment = payment, onClick = { onPaymentClick(payment.id) })
                }
            }
        }
    }
}

@Composable
private fun PaymentTableHeader() {
    val headerStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TableHeaderBg)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(Res.string.date_label).uppercase(),
            style = headerStyle,
            color = AptoOnSurfaceVariant,
            modifier = Modifier.weight(COL_DATE)
        )
        Text(
            text = stringResource(Res.string.web_payment_history_method).uppercase(),
            style = headerStyle,
            color = AptoOnSurfaceVariant,
            modifier = Modifier.weight(COL_METHOD)
        )
        Text(
            text = stringResource(Res.string.reference_label).uppercase(),
            style = headerStyle,
            color = AptoOnSurfaceVariant,
            modifier = Modifier.weight(COL_REFERENCE)
        )
        Text(
            text = stringResource(Res.string.web_payment_history_amount).uppercase(),
            style = headerStyle,
            color = AptoOnSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(COL_AMOUNT).padding(end = 24.dp)
        )
        Text(
            text = stringResource(Res.string.web_payment_history_status).uppercase(),
            style = headerStyle,
            color = AptoOnSurfaceVariant,
            modifier = Modifier.weight(COL_STATUS)
        )
    }
    HorizontalDivider(color = AptoOutlineVariant)
}

@Composable
private fun PaymentTableRow(
    payment: Payment,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val statusData = paymentStatusData(payment.status)
    val methodIcon = when (payment.method) {
        PaymentMethod.PAGO_MOVIL -> Icons.Default.Payments
        PaymentMethod.TRANSFER -> Icons.Default.AccountBalanceWallet
        PaymentMethod.CASH -> Icons.Default.HomeWork
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (hovered) AptoSurfaceContainerLow else AptoSurfaceContainerLowest)
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(COL_DATE).padding(end = 16.dp)) {
            Text(
                text = formatPaymentDate(payment.date),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = AptoOnSurface
            )
            Text(
                text = payment.description.ifBlank { stringResource(Res.string.payment_default_desc) },
                style = MaterialTheme.typography.bodySmall,
                color = AptoOnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            modifier = Modifier.weight(COL_METHOD).padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(AptoSecondaryFixed, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = methodIcon,
                    contentDescription = null,
                    tint = AptoSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = paymentMethodLabel(payment.method),
                style = MaterialTheme.typography.bodyMedium,
                color = AptoOnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = payment.reference?.takeIf { it.isNotBlank() } ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            color = AptoOnSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(COL_REFERENCE).padding(end = 16.dp)
        )
        Text(
            text = stringResource(Res.string.currency_amount, formatCurrency(payment.amount)),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = AptoPrimary,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.weight(COL_AMOUNT).padding(end = 24.dp)
        )
        Box(modifier = Modifier.weight(COL_STATUS)) {
            StatusBadge(text = stringResource(statusData.first), statusColor = statusData.second)
        }
    }
}
