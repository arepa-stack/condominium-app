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
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.condominio.data.model.Payment
import com.example.condominio.data.model.PaymentMethod
import com.example.condominio.data.model.PaymentStatus
import com.example.condominio.ui.components.EmptyState
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
import com.example.condominio.ui.utils.formatCurrency
import condominio.shared.generated.resources.*
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentHistoryScreen(
    onBackClick: () -> Unit,
    onPaymentClick: (String) -> Unit,
    viewModel: PaymentHistoryViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadPayments()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopBarWithBack(
                title = "Historial de Pagos",
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
                uiState.payments.isEmpty() -> WebEmptyFrame {
                    EmptyState(
                        title = "No hay pagos registrados.",
                        icon = Icons.Default.ReceiptLong,
                    )
                }
                isExpanded() -> {
                    PaymentHistoryTable(
                        unit = uiState.unit,
                        payments = uiState.payments,
                        onPaymentClick = onPaymentClick
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
                                        text = "UNIDAD ${uiState.unit}".uppercase(),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = AptoOnSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                                Text(
                                    text = "Transacciones Recientes",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AptoPrimary
                                )
                            }
                        }

                        items(uiState.payments) { payment ->
                            PaymentHistoryCard(
                                payment = payment,
                                onClick = { onPaymentClick(payment.id) }
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
                        text = payment.description.ifBlank { "Pago de Recibo" },
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = AptoPrimary,
                        modifier = Modifier.padding(bottom = 4.dp),
                        maxLines = 1
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AptoOnSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                Text(
                    text = "$${formatCurrency(payment.amount)}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = AptoPrimary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                StatusBadge(
                    text = statusData.first,
                    statusColor = statusData.second
                )
                Spacer(modifier = Modifier.width(8.dp))
                val extraText = when {
                    !payment.processorName.isNullOrBlank() -> "• Por: ${payment.processorName}"
                    else -> "• ${payment.method.label}"
                }
                Text(
                    text = extraText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = AptoOutline
                )
            }
        }
    }
}

private fun formatPaymentDate(epochMillis: Long): String = try {
    val instant = Instant.fromEpochMilliseconds(epochMillis)
    val date = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val months = listOf("", "Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
    val monthStr = if (date.monthNumber in 1..12) months[date.monthNumber] else date.monthNumber.toString()
    "${date.dayOfMonth} $monthStr, ${date.year}"
} catch (e: Exception) {
    "Fecha Inválida"
}

private fun paymentStatusData(status: PaymentStatus) = when (status) {
    PaymentStatus.APPROVED -> Pair("Aprobado", AptoStatusSuccess)
    PaymentStatus.REJECTED -> Pair("Rechazado", AptoStatusError)
    PaymentStatus.PENDING -> Pair("Pendiente", AptoStatusWarning)
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
                        text = "UNIDAD $unit".uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AptoOnSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Text(
                    text = "Transacciones Recientes",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = AptoOnSurface
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
                text = payment.description.ifBlank { "Pago de Recibo" },
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
                text = payment.method.label,
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
            text = "$${formatCurrency(payment.amount)}",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = AptoPrimary,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.weight(COL_AMOUNT).padding(end = 24.dp)
        )
        Box(modifier = Modifier.weight(COL_STATUS)) {
            StatusBadge(text = statusData.first, statusColor = statusData.second)
        }
    }
}
