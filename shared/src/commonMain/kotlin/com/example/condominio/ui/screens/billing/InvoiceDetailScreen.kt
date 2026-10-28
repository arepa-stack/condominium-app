package com.example.condominio.ui.screens.billing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.condominio.data.model.Invoice
import com.example.condominio.data.model.InvoiceStatus
import com.example.condominio.data.model.InvoiceType
import com.example.condominio.data.model.withPayments
import com.example.condominio.data.utils.rememberHtmlPrinter
import com.example.condominio.ui.utils.invoiceTitle
import com.example.condominio.ui.screens.payment.invoiceReceiptHtml
import com.example.condominio.data.model.Payment
import com.example.condominio.data.model.PaymentMethod
import com.example.condominio.ui.components.AdaptiveColumns
import com.example.condominio.ui.components.LoadingState
import com.example.condominio.ui.components.TopBarWithBack
import com.example.condominio.ui.components.WebCard
import com.example.condominio.ui.components.WebEmptyFrame
import com.example.condominio.ui.components.isExpanded
import com.example.condominio.ui.components.pagePadding
import com.example.condominio.ui.components.screenBackground
import com.example.condominio.ui.theme.*
import com.example.condominio.ui.utils.formatCurrency
import com.example.condominio.ui.components.ErrorState
import com.example.condominio.ui.screens.payment.formatPaymentDate
import com.example.condominio.ui.screens.payment.invoiceMonthLabel
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    onBackClick: () -> Unit,
    onSeeAllPaymentsClick: () -> Unit,
    onPayRemainderClick: (String) -> Unit,
    onPaymentClick: (String) -> Unit,
    viewModel: InvoiceDetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val invoice = uiState.invoice?.withPayments(uiState.payments)
    // "Download invoice" prints the receipt; "Save as PDF" in the print dialog is the download
    val printHtml = rememberHtmlPrinter()
    val receipt = invoice?.let { invoiceReceiptHtml(it, uiState.payments) }
    val receiptTitle = invoice?.let { invoiceTitle(it.description) ?: it.period }.orEmpty()
    val onDownloadClick: () -> Unit = { receipt?.let { printHtml(receiptTitle, it) } }
    val hasPendingBalance = invoice != null &&
            invoice.remaining > 0 &&
            invoice.status != InvoiceStatus.CANCELLED &&
            invoice.status != InvoiceStatus.PAID
    val expanded = isExpanded()

    Scaffold(
        topBar = {
            TopBarWithBack(
                title = stringResource(Res.string.invoice_detail_title),
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            // Expanded: actions live in the right-hand summary column instead of a fixed bottom bar.
            if (invoice != null && !expanded) {
                Surface(
                    color = AptoSurfaceContainerLowest,
                    shadowElevation = 8.dp,
                    tonalElevation = 0.dp
                ) {
                    HorizontalDivider(color = AptoOutlineVariant)
                    val secondaryLabel = stringResource(Res.string.view_all_payments)
                    val primaryLabel = if (hasPendingBalance) {
                        stringResource(Res.string.pay_now).uppercase()
                    } else {
                        stringResource(Res.string.download_invoice)
                    }
                    val seeAll: @Composable (Modifier) -> Unit = { mod ->
                        OutlinedButton(
                            onClick = onSeeAllPaymentsClick,
                            modifier = mod,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, AptoSecondary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AptoSecondary)
                        ) {
                            BottomBarButtonLabel(secondaryLabel)
                        }
                    }
                    val primary: @Composable (Modifier) -> Unit = { mod ->
                        Button(
                            onClick = { if (hasPendingBalance) onPayRemainderClick(invoice.id) else onDownloadClick() },
                            modifier = mod,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasPendingBalance) AptoSecondaryContainer else AptoSecondary,
                                contentColor = AptoOnSecondary
                            )
                        ) {
                            BottomBarButtonLabel(primaryLabel)
                        }
                    }
                    // Side by side when both labels fit on one line; otherwise stacked at full width, so they
                    // never wrap to two lines (A-21).
                    BoxWithConstraints(Modifier.fillMaxWidth().padding(16.dp)) {
                        val measurer = rememberTextMeasurer()
                        val density = LocalDensity.current
                        val labelStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        val halfLabelWidth = (maxWidth - BottomBarSpacing) / 2 -
                            ButtonDefaults.ContentPadding.calculateLeftPadding(LayoutDirection.Ltr) -
                            ButtonDefaults.ContentPadding.calculateRightPadding(LayoutDirection.Ltr)
                        val fits = listOf(secondaryLabel, primaryLabel).all {
                            with(density) {
                                measurer.measure(it, labelStyle, maxLines = 1, softWrap = false).size.width.toDp()
                            } <= halfLabelWidth
                        }
                        if (fits) {
                            Row(horizontalArrangement = Arrangement.spacedBy(BottomBarSpacing)) {
                                seeAll(Modifier.weight(1f))
                                primary(Modifier.weight(1f))
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                seeAll(Modifier.fillMaxWidth())
                                primary(Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        },
        containerColor = screenBackground(AptoBackground)
    ) { paddingValues ->
        if (expanded) {
            InvoiceDetailWideContent(
                isLoading = uiState.isLoading,
                loadFailed = uiState.loadFailed,
                onRetry = viewModel::loadData,
                invoice = invoice,
                payments = uiState.payments,
                isLoadingPayments = uiState.isLoadingPayments,
                paymentsLoadFailed = uiState.paymentsLoadFailed,
                onRetryPayments = viewModel::loadPayments,
                hasPendingBalance = hasPendingBalance,
                onSeeAllPaymentsClick = onSeeAllPaymentsClick,
                onDownloadClick = onDownloadClick,
                onPayRemainderClick = onPayRemainderClick,
                onPaymentClick = onPaymentClick,
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            )
            return@Scaffold
        }
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

            // A failed load is not "no invoices found" (A-07)
            if (invoice == null && uiState.loadFailed) {
                item {
                    ErrorState(
                        title = stringResource(Res.string.error_loading_invoice),
                        onRetry = viewModel::loadData,
                        modifier = Modifier.fillParentMaxSize()
                    )
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
                    // pagePadding(): 16dp on phone (unchanged), 32dp on Medium to align with the page header.
                    modifier = Modifier.padding(horizontal = pagePadding())
                )
            }

            item {
                Text(
                    text = stringResource(Res.string.payment_history_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AptoPrimary,
                    modifier = Modifier.padding(start = pagePadding(), end = pagePadding(), top = 16.dp, bottom = 8.dp)
                )
            }

            if (uiState.isLoadingPayments && uiState.payments.isEmpty()) {
                item {
                    LoadingState(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        fullScreen = false
                    )
                }
            } else if (uiState.paymentsLoadFailed && uiState.payments.isEmpty()) {
                // A failed load is not "no payments" (A-07)
                item {
                    ErrorState(
                        title = stringResource(Res.string.error_loading_invoice_payments),
                        onRetry = viewModel::loadPayments
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
                items(uiState.payments, key = { it.id }) { payment ->
                    PaymentItem(
                        payment = payment,
                        invoiceId = invoice.id,
                        onClick = { onPaymentClick(payment.id) },
                        modifier = Modifier.padding(horizontal = pagePadding(), vertical = 4.dp)
                    )
                }
            }
        }
    }
}

private val BottomBarSpacing = 12.dp

@Composable
private fun BottomBarButtonLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        // Safety net (e.g. huge font scale): the bar stacks the buttons before this is needed.
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
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
            text = invoice.description ?: stringResource(Res.string.invoice_period_label, invoiceMonthLabel(invoice.period)),
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
        InvoiceStatus.PAID -> Triple(Icons.Default.CheckCircle, AptoStatusSuccess, stringResource(Res.string.status_paid).uppercase())
        InvoiceStatus.OVERDUE -> Triple(Icons.Default.Warning, AptoStatusError, stringResource(Res.string.status_overdue).uppercase())
        InvoiceStatus.CANCELLED -> Triple(Icons.Default.Warning, AptoStatusError, stringResource(Res.string.status_cancelled).uppercase())
        InvoiceStatus.PARTIAL -> Triple(Icons.Default.Warning, AptoSecondaryContainer, stringResource(Res.string.status_partial).uppercase())
        InvoiceStatus.PENDING -> Triple(Icons.Default.Warning, AptoSecondaryContainer, stringResource(Res.string.status_pending).uppercase())
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
            text = stringResource(Res.string.currency_amount, formatCurrency(amount)),
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
                    text = formatPaymentDate(payment.date),
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
                    text = stringResource(Res.string.applied_label, stringResource(Res.string.currency_amount, formatCurrency(displayAmount))),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AptoStatusSuccess,
                    textDecoration = TextDecoration.Underline
                )
                if (allocation != null && allocation.amount != payment.amount) {
                    Text(
                        text = stringResource(Res.string.total_amount_label, stringResource(Res.string.currency_amount, formatCurrency(payment.amount))),
                        style = MaterialTheme.typography.labelSmall,
                        color = AptoOnSurfaceVariant
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Expanded (web desktop) layout. Compact/Medium use the original LazyColumn + bottom bar above.
// ---------------------------------------------------------------------------------------------

@Composable
private fun InvoiceDetailWideContent(
    isLoading: Boolean,
    loadFailed: Boolean,
    onRetry: () -> Unit,
    invoice: Invoice?,
    payments: List<Payment>,
    isLoadingPayments: Boolean,
    paymentsLoadFailed: Boolean,
    onRetryPayments: () -> Unit,
    hasPendingBalance: Boolean,
    onSeeAllPaymentsClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onPayRemainderClick: (String) -> Unit,
    onPaymentClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading && invoice == null) {
        LoadingState(modifier = modifier)
        return
    }
    if (invoice == null && loadFailed) {
        // Error with retry instead of "no invoices found" (A-07)
        Box(modifier = modifier) {
            WebEmptyFrame {
                ErrorState(title = stringResource(Res.string.error_loading_invoice), onRetry = onRetry)
            }
        }
        return
    }
    if (invoice == null) {
        // Empty state as a card under the header (left edge = pagePadding), not floating mid-canvas.
        Box(modifier = modifier) {
            WebEmptyFrame {
                Text(
                    text = stringResource(Res.string.no_invoices_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AptoOnSurfaceVariant
                )
            }
        }
        return
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(start = pagePadding(), end = pagePadding(), top = 8.dp, bottom = 32.dp)
    ) {
        AdaptiveColumns(
            main = {
                WebCard {
                    InvoiceWideHeader(invoice = invoice)
                }
                WebCard(contentPadding = 0.dp) {
                    Text(
                        text = stringResource(Res.string.payment_history_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AptoPrimary,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp)
                    )
                    InvoicePaymentsTable(
                        isLoading = isLoadingPayments,
                        loadFailed = paymentsLoadFailed,
                        onRetry = onRetryPayments,
                        payments = payments,
                        invoiceId = invoice.id,
                        onPaymentClick = onPaymentClick
                    )
                }
            },
            side = {
                InvoiceSummaryCard(
                    invoice = invoice,
                    hasPendingBalance = hasPendingBalance,
                    onSeeAllPaymentsClick = onSeeAllPaymentsClick,
                    onDownloadClick = onDownloadClick,
                    onPayRemainderClick = onPayRemainderClick
                )
            }
        )
    }
}

@Composable
private fun InvoiceWideHeader(invoice: Invoice) {
    val (icon, iconColor, statusText) = resolveInvoiceStatusVisuals(invoice.status)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(iconColor.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(32.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = invoice.description ?: stringResource(Res.string.invoice_period_label, invoiceMonthLabel(invoice.period)),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AptoPrimary
            )
            Text(
                text = stringResource(Res.string.period_label, invoiceMonthLabel(invoice.period)),
                style = MaterialTheme.typography.bodyMedium,
                color = AptoOnSurfaceVariant
            )
            if (invoice.type == InvoiceType.PETTY_CASH_REPLENISHMENT) {
                Text(
                    text = stringResource(Res.string.petty_cash),
                    style = MaterialTheme.typography.labelMedium,
                    color = AptoOnSurfaceVariant
                )
            }
        }
        InvoiceStatusPill(text = statusText, color = iconColor)
    }
}

@Composable
private fun InvoiceStatusPill(text: String, color: Color) {
    Surface(
        shape = CircleShape,
        color = badgeBg(color.copy(alpha = 0.12f), color),
        border = badgeBorder(color)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = onStatusContainer(color),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun InvoicePaymentsTable(
    isLoading: Boolean,
    loadFailed: Boolean,
    onRetry: () -> Unit,
    payments: List<Payment>,
    invoiceId: String,
    onPaymentClick: (String) -> Unit
) {
    if (isLoading && payments.isEmpty()) {
        LoadingState(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            fullScreen = false
        )
        return
    }
    if (loadFailed && payments.isEmpty()) {
        // A failed load is not "no payments" (A-07)
        ErrorState(title = stringResource(Res.string.error_loading_invoice_payments), onRetry = onRetry)
        return
    }
    if (payments.isEmpty()) {
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
        return
    }

    // Header row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TableHeaderBg)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TableHeaderCell(stringResource(Res.string.date_label), Modifier.weight(1f))
        TableHeaderCell(stringResource(Res.string.method_label), Modifier.weight(1.2f))
        TableHeaderCell(stringResource(Res.string.reference_label), Modifier.weight(1.2f))
        TableHeaderCell(
            stringResource(Res.string.web_invoice_detail_applied),
            Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
    HorizontalDivider(color = AptoOutlineVariant)

    payments.forEachIndexed { index, payment ->
        val allocation = payment.allocations.find { it.invoiceId == invoiceId }
        val displayAmount = allocation?.amount ?: payment.amount
        val methodLabel = when (payment.method) {
            PaymentMethod.PAGO_MOVIL -> stringResource(Res.string.method_pago_movil)
            PaymentMethod.TRANSFER -> stringResource(Res.string.method_transfer)
            PaymentMethod.CASH -> stringResource(Res.string.method_cash)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable { onPaymentClick(payment.id) }
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatPaymentDate(payment.date),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = AptoOnSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = methodLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = AptoOnSurfaceVariant,
                modifier = Modifier.weight(1.2f)
            )
            Text(
                text = payment.reference?.takeIf { it.isNotBlank() } ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                color = AptoOnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.2f)
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = stringResource(Res.string.currency_amount, formatCurrency(displayAmount)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AptoStatusSuccess
                )
                if (allocation != null && allocation.amount != payment.amount) {
                    Text(
                        text = stringResource(Res.string.total_amount_label, stringResource(Res.string.currency_amount, formatCurrency(payment.amount))),
                        style = MaterialTheme.typography.labelSmall,
                        color = AptoOnSurfaceVariant
                    )
                }
            }
        }
        if (index < payments.lastIndex) {
            HorizontalDivider(color = AptoOutlineVariant)
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun TableHeaderCell(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = AptoOnSurfaceVariant,
        textAlign = textAlign,
        modifier = modifier
    )
}

@Composable
private fun InvoiceSummaryCard(
    invoice: Invoice,
    hasPendingBalance: Boolean,
    onSeeAllPaymentsClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onPayRemainderClick: (String) -> Unit
) {
    val (_, statusColor, statusText) = resolveInvoiceStatusVisuals(invoice.status)
    WebCard {
        Text(
            text = stringResource(Res.string.web_invoice_detail_summary),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AptoPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        // Same amount rows as the mobile InvoiceAmountCard.
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

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = AptoOutlineVariant
        )

        SummaryInfoRow(label = stringResource(Res.string.web_invoice_detail_status)) {
            InvoiceStatusPill(text = statusText, color = statusColor)
        }
        SummaryInfoRow(label = stringResource(Res.string.web_invoice_detail_period)) {
            Text(
                text = invoiceMonthLabel(invoice.period),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = AptoOnSurface
            )
        }
        invoice.dueDate?.let { due ->
            SummaryInfoRow(label = stringResource(Res.string.web_invoice_detail_due_date)) {
                Text(
                    text = formatPaymentDate(due),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (invoice.status == InvoiceStatus.OVERDUE) AptoStatusError else AptoOnSurface
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (hasPendingBalance) {
                Button(
                    onClick = { onPayRemainderClick(invoice.id) },
                    modifier = Modifier.fillMaxWidth().pointerHoverIcon(PointerIcon.Hand),
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
                OutlinedButton(
                    onClick = onDownloadClick,
                    modifier = Modifier.fillMaxWidth().pointerHoverIcon(PointerIcon.Hand),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, AptoSecondary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AptoSecondary)
                ) {
                    Text(
                        text = stringResource(Res.string.download_invoice),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = onDownloadClick,
                    modifier = Modifier.fillMaxWidth().pointerHoverIcon(PointerIcon.Hand),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AptoSecondary,
                        contentColor = AptoOnSecondary
                    )
                ) {
                    Text(
                        text = stringResource(Res.string.download_invoice),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            TextButton(
                onClick = onSeeAllPaymentsClick,
                modifier = Modifier.fillMaxWidth().pointerHoverIcon(PointerIcon.Hand),
                colors = ButtonDefaults.textButtonColors(contentColor = AptoSecondary)
            ) {
                Text(
                    text = stringResource(Res.string.view_all_payments),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SummaryInfoRow(
    label: String,
    value: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = AptoOnSurfaceVariant
        )
        value()
    }
}
