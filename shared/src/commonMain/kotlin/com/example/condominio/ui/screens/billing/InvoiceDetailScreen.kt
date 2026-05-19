package com.example.condominio.ui.screens.billing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.condominio.data.model.Invoice
import com.example.condominio.data.model.InvoiceStatus
import com.example.condominio.data.model.Payment
import com.example.condominio.data.model.PaymentMethod
import com.example.condominio.ui.components.LoadingState
import com.example.condominio.ui.components.TopBarWithBack
import com.example.condominio.ui.theme.*
import com.example.condominio.ui.utils.formatCurrency
import com.example.condominio.ui.utils.formatDate
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    onBackClick: () -> Unit,
    onSeeAllPaymentsClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onPayRemainderClick: (String) -> Unit,
    onPaymentClick: (String) -> Unit,
    viewModel: InvoiceDetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val invoice = uiState.invoice
    val hasPendingBalance = invoice != null &&
            invoice.remaining > 0 &&
            invoice.status != InvoiceStatus.CANCELLED &&
            invoice.status != InvoiceStatus.PAID

    Scaffold(
        topBar = {
            TopBarWithBack(
                title = stringResource(Res.string.invoice_detail_title),
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            if (invoice != null) {
                Surface(
                    color = AptoSurfaceContainerLowest,
                    shadowElevation = 8.dp,
                    tonalElevation = 0.dp
                ) {
                    HorizontalDivider(color = AptoOutlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSeeAllPaymentsClick,
                            modifier = Modifier.weight(1f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, AptoSecondary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AptoSecondary)
                        ) {
                            Text(
                                text = stringResource(Res.string.view_all_payments),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (hasPendingBalance) {
                            Button(
                                onClick = { onPayRemainderClick(invoice.id) },
                                modifier = Modifier.weight(1f),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AptoSecondaryContainer,
                                    contentColor = AptoOnSecondary
                                )
                            ) {
                                Text(
                                    text = stringResource(Res.string.pay_now).uppercase(),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Button(
                                onClick = onDownloadClick,
                                modifier = Modifier.weight(1f),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AptoSecondary,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = stringResource(Res.string.download_invoice),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = AptoBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (uiState.isLoading && invoice == null) {
                item {
                    LoadingState(modifier = Modifier.fillParentMaxSize())
                }
                return@LazyColumn
            }

            if (invoice == null) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.no_invoices_found),
                            style = MaterialTheme.typography.bodyLarge,
                            color = AptoOnSurfaceVariant
                        )
                    }
                }
                return@LazyColumn
            }

            item {
                InvoiceStatusHeader(invoice = invoice)
            }

            item {
                InvoiceAmountCard(
                    invoice = invoice,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                Text(
                    text = stringResource(Res.string.payment_history_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AptoPrimary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            if (uiState.isLoading) {
                item {
                    LoadingState(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        fullScreen = false
                    )
                }
            } else if (uiState.payments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.no_payments_found),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AptoOnSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(uiState.payments) { payment ->
                    PaymentItem(
                        payment = payment,
                        invoiceId = invoice.id,
                        onClick = { onPaymentClick(payment.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InvoiceStatusHeader(invoice: Invoice) {
    val (icon, iconColor, statusText) = resolveInvoiceStatusVisuals(invoice.status)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(iconColor.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(40.dp)
            )
        }
        Text(
            text = invoice.description ?: stringResource(Res.string.invoice_period_label, invoice.period),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = AptoPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = iconColor
        )
    }
}

@Composable
private fun resolveInvoiceStatusVisuals(status: InvoiceStatus): Triple<ImageVector, Color, String> {
    return when (status) {
        InvoiceStatus.PAID -> Triple(Icons.Default.CheckCircle, AptoStatusSuccess, stringResource(Res.string.status_paid))
        InvoiceStatus.OVERDUE -> Triple(Icons.Default.Warning, AptoStatusError, stringResource(Res.string.status_overdue))
        InvoiceStatus.CANCELLED -> Triple(Icons.Default.Warning, AptoStatusError, stringResource(Res.string.status_cancelled))
        InvoiceStatus.PARTIAL -> Triple(Icons.Default.Warning, AptoSecondaryContainer, stringResource(Res.string.status_partial))
        InvoiceStatus.PENDING -> Triple(Icons.Default.Warning, AptoSecondaryContainer, stringResource(Res.string.status_pending))
    }
}

@Composable
private fun InvoiceAmountCard(
    invoice: Invoice,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = AptoSurfaceContainerLowest),
        border = BorderStroke(1.dp, AptoOutlineVariant)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            AmountRow(
                label = stringResource(Res.string.total_label),
                amount = invoice.amount,
                amountColor = AptoSecondary
            )
            AmountRow(
                label = stringResource(Res.string.paid_label),
                amount = invoice.paid,
                amountColor = AptoSecondary
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = AptoOutlineVariant
            )
            AmountRow(
                label = stringResource(Res.string.remaining_label),
                amount = invoice.remaining,
                amountColor = if (invoice.remaining <= 0) AptoStatusSuccess else AptoStatusError,
                labelColor = AptoOutline
            )
        }
    }
}

@Composable
private fun AmountRow(
    label: String,
    amount: Double,
    amountColor: Color,
    labelColor: Color = AptoSecondary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = labelColor
        )
        Text(
            text = "\$${formatCurrency(amount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}

@Composable
fun PaymentItem(
    payment: Payment,
    invoiceId: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allocation = invoiceId?.let { id ->
        payment.allocations.find { it.invoiceId == id }
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AptoSurfaceContainerLow),
        border = BorderStroke(1.dp, AptoOutlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = formatDate(payment.date, "dd/MM/yyyy"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AptoOnSurface
                )
                val methodLabel = when (payment.method) {
                    PaymentMethod.PAGO_MOVIL -> stringResource(Res.string.method_pago_movil)
                    PaymentMethod.TRANSFER -> stringResource(Res.string.method_transfer)
                    PaymentMethod.CASH -> stringResource(Res.string.method_cash)
                }
                Text(
                    text = stringResource(Res.string.payment_method_label, methodLabel),
                    style = MaterialTheme.typography.labelMedium,
                    color = AptoOnSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val displayAmount = allocation?.amount ?: payment.amount
                Text(
                    text = stringResource(Res.string.applied_label, formatCurrency(displayAmount)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AptoStatusSuccess,
                    textDecoration = TextDecoration.Underline
                )
                if (allocation != null && allocation.amount != payment.amount) {
                    Text(
                        text = stringResource(Res.string.total_amount_label, formatCurrency(payment.amount)),
                        style = MaterialTheme.typography.labelSmall,
                        color = AptoOnSurfaceVariant
                    )
                }
            }
        }
    }
}
