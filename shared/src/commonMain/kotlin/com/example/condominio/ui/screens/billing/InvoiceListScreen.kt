package com.example.condominio.ui.screens.billing

import com.example.condominio.ui.components.isWide
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.condominio.data.model.Invoice
import androidx.compose.foundation.layout.PaddingValues
import com.example.condominio.data.model.InvoiceStatus
import com.example.condominio.ui.components.EmptyState
import com.example.condominio.ui.components.ErrorState
import com.example.condominio.ui.components.LoadingState
import com.example.condominio.ui.components.TopBarWithBack
import com.example.condominio.ui.components.WebCard
import com.example.condominio.ui.components.WebEmptyFrame
import com.example.condominio.ui.components.isExpanded
import com.example.condominio.ui.components.pagePadding
import com.example.condominio.ui.components.screenBackground
import com.example.condominio.ui.screens.payment.formatPaymentDate
import com.example.condominio.ui.screens.payment.invoiceMonthLabel
import com.example.condominio.ui.theme.*
import com.example.condominio.ui.utils.formatCurrency
import com.example.condominio.ui.utils.invoiceTitle
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceListScreen(
    onBackClick: () -> Unit,
    onInvoiceClick: (Invoice) -> Unit,
    onPayNowClick: (Invoice) -> Unit,
    viewModel: InvoiceListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    // In the ViewModel so it survives going to the detail and back (E-F4)
    val selectedTab = uiState.selectedTab
    val tabs = listOf(
        stringResource(Res.string.tab_all),
        stringResource(Res.string.tab_pending),
        stringResource(Res.string.tab_paid)
    )

    val filteredInvoices = remember(uiState.invoices, selectedTab) {
        when (selectedTab) {
            1 -> uiState.invoices.filter {
                it.status != InvoiceStatus.PAID && it.status != InvoiceStatus.CANCELLED
            }
            2 -> uiState.invoices.filter { it.status == InvoiceStatus.PAID }
            else -> uiState.invoices
        }
    }

    Scaffold(
        topBar = {
            TopBarWithBack(
                title = stringResource(Res.string.invoices_title),
                onBackClick = onBackClick,
                actions = {
                    if (isWide()) {
                        // Same labelled header action as Cartelera / Votaciones on web.
                        OutlinedButton(
                            onClick = { viewModel.refresh() },
                            border = androidx.compose.foundation.BorderStroke(1.dp, AptoOutlineVariant),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AptoOnSurface),
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.refresh))
                        }
                    } else {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(Res.string.refresh),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        containerColor = screenBackground(AptoBackground)
    ) { paddingValues ->
        if (isExpanded()) {
            InvoiceWebContent(
                modifier = Modifier.padding(paddingValues),
                tabs = tabs,
                selectedTab = selectedTab,
                onTabSelected = viewModel::onTabSelected,
                isLoading = uiState.isLoading,
                error = uiState.error?.asString(),
                onRetry = viewModel::refresh,
                invoices = filteredInvoices,
                onInvoiceClick = onInvoiceClick,
                onPayNowClick = onPayNowClick
            )
            return@Scaffold
        }
        // Compact (phone) and Medium (tablet) share this path; on Medium the tabs, grid and
        // empty/error states align with the page header at pagePadding() (phone unchanged).
        val wide = isWide()
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = if (wide) Modifier.padding(horizontal = pagePadding()) else Modifier,
                containerColor = AptoSurface,
                contentColor = AptoSecondaryContainer,
                indicator = { tabPositions ->
                    Box(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .height(2.dp)
                            .background(AptoSecondaryContainer)
                    )
                },
                divider = { HorizontalDivider(color = AptoOutlineVariant) }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { viewModel.onTabSelected(index) },
                        selectedContentColor = AptoSecondaryContainer,
                        unselectedContentColor = AptoOnSurfaceVariant,
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    LoadingState()
                }
                // Error with retry instead of plain red text (A-07)
                uiState.error != null -> WebEmptyFrame {
                    ErrorState(title = uiState.error!!.asString(), onRetry = viewModel::refresh)
                }
                filteredInvoices.isEmpty() -> WebEmptyFrame { InvoiceEmptyState() }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(340.dp),
                        modifier = Modifier.fillMaxSize(),
                        // pagePadding() is 16dp on phone, so Compact stays PaddingValues(16.dp).
                        contentPadding = PaddingValues(horizontal = pagePadding(), vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredInvoices, key = { it.id }) { invoice ->
                            InvoiceItem(
                                invoice = invoice,
                                onClick = { onInvoiceClick(invoice) },
                                onPayNow = { onPayNowClick(invoice) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceItem(
    invoice: Invoice,
    onClick: () -> Unit,
    onPayNow: () -> Unit
) {
    val isActionable = invoice.remaining > 0 &&
            invoice.status != InvoiceStatus.PAID &&
            invoice.status != InvoiceStatus.CANCELLED
    val showPaidColumn = invoice.paid > 0 || invoice.status == InvoiceStatus.PAID

    // Same look as ListItemCard, but only the info area is clickable: the "Pay" button is its sibling,
    // not nested inside a clickable card (E-F1).
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClickLabel = stringResource(Res.string.invoice_detail),
                        role = Role.Button,
                        onClick = onClick
                    )
                    .pointerHoverIcon(PointerIcon.Hand)
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = if (isActionable) 16.dp else 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = invoiceTitle(invoice.description) ?: stringResource(Res.string.default_invoice_desc),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AptoOnSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(Res.string.period_label, invoiceMonthLabel(invoice.period)),
                            style = MaterialTheme.typography.bodySmall,
                            color = AptoOnSurfaceVariant
                        )
                    }
                    InvoiceStatusBadge(status = invoice.status)
                }

                HorizontalDivider(color = AptoOutlineVariant)

                if (showPaidColumn) {
                    // Each column as wide as its content and amounts on one line, so they never split (E-F3);
                    // "Paid" left-aligned like "Total" instead of centered.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            InvoiceAmountLabel(stringResource(Res.string.total_label))
                            InvoiceAmountText(
                                text = stringResource(Res.string.currency_amount, formatCurrency(invoice.amount)),
                                style = MaterialTheme.typography.bodyLarge,
                                color = AptoOnSurface
                            )
                        }
                        Column {
                            InvoiceAmountLabel(stringResource(Res.string.paid_label))
                            InvoiceAmountText(
                                text = stringResource(Res.string.currency_amount, formatCurrency(invoice.paid)),
                                style = MaterialTheme.typography.bodyLarge,
                                color = AptoStatusSuccess,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            InvoiceAmountLabel(stringResource(Res.string.remaining_label))
                            val remainingColor = if (invoice.remaining <= 0) AptoStatusSuccess else AptoSecondaryContainer
                            InvoiceAmountText(
                                text = stringResource(Res.string.currency_amount, formatCurrency(invoice.remaining)),
                                style = MaterialTheme.typography.bodyLarge,
                                color = remainingColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            InvoiceAmountLabel(stringResource(Res.string.total_label))
                            InvoiceAmountText(
                                text = stringResource(Res.string.currency_amount, formatCurrency(invoice.amount)),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = AptoOnSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            InvoiceAmountLabel(stringResource(Res.string.remaining_label))
                            InvoiceAmountText(
                                text = stringResource(Res.string.currency_amount, formatCurrency(invoice.remaining)),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AptoSecondaryContainer
                            )
                        }
                    }
                }
            }

            if (isActionable) {
                Button(
                    onClick = onPayNow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                        .pointerHoverIcon(PointerIcon.Hand),
                    shape = RoundedCornerShape(8.dp),
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
            }
        }
    }
}

@Composable
private fun InvoiceAmountText(
    text: String,
    style: TextStyle,
    color: androidx.compose.ui.graphics.Color,
    fontWeight: FontWeight? = null
) {
    Text(
        text = text,
        style = style,
        color = color,
        fontWeight = fontWeight,
        maxLines = 1,
        softWrap = false
    )
}

// ---------------------------------------------------------------------------
// Expanded (desktop web): filter chips + invoice table inside a WebCard.
// Compact/Medium keep the original TabRow + grid above.
// ---------------------------------------------------------------------------

private val ColumnSpacing = 16.dp
private val RowHorizontalPadding = 24.dp
private val StatusBadgeHorizontalPadding = 12.dp

@Composable
private fun InvoiceWebContent(
    modifier: Modifier,
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    invoices: List<Invoice>,
    onInvoiceClick: (Invoice) -> Unit,
    onPayNowClick: (Invoice) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = pagePadding())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                val selected = selectedTab == index
                FilterChip(
                    selected = selected,
                    onClick = { onTabSelected(index) },
                    label = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = AptoSurfaceContainerLowest,
                        labelColor = AptoOnSurfaceVariant,
                        selectedContainerColor = AptoSecondaryContainer,
                        selectedLabelColor = AptoOnSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = AptoOutlineVariant,
                        selectedBorderColor = AptoSecondaryContainer
                    ),
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                )
            }
        }

        when {
            isLoading -> LoadingState()
            error != null -> WebCard {
                Box(Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.Center) {
                    ErrorState(title = error, onRetry = onRetry)
                }
            }
            // Fixed height: EmptyState fills its parent, which would stretch the card to the whole page.
            invoices.isEmpty() -> WebCard { Box(Modifier.fillMaxWidth().height(320.dp)) { InvoiceEmptyState() } }
            else -> InvoiceTable(
                invoices = invoices,
                onInvoiceClick = onInvoiceClick,
                onPayNowClick = onPayNowClick,
                // Card as tall as its rows, up to the bottom of the page; rows scroll inside it.
                modifier = Modifier.weight(1f, fill = false).padding(bottom = 32.dp)
            )
        }
    }
}

/** Widths of the fixed columns, measured like the dashboard table so cells never wrap (E-F3). */
private data class InvoiceColumnWidths(val due: Dp, val total: Dp, val remaining: Dp, val status: Dp, val action: Dp)

@Composable
private fun InvoiceTable(
    invoices: List<Invoice>,
    onInvoiceClick: (Invoice) -> Unit,
    onPayNowClick: (Invoice) -> Unit,
    modifier: Modifier = Modifier
) {
    val dueHeader = stringResource(Res.string.web_invoices_col_due)
    val totalHeader = stringResource(Res.string.total_label)
    val remainingHeader = stringResource(Res.string.remaining_label)
    val statusHeader = stringResource(Res.string.web_invoices_col_status)
    val payNow = stringResource(Res.string.pay_now)
    val dueDates = invoices.map { invoiceDueText(it) }
    val totals = invoices.map { stringResource(Res.string.currency_amount, formatCurrency(it.amount)) }
    val remainings = invoices.map { stringResource(Res.string.currency_amount, formatCurrency(it.remaining)) }
    val statusLabels = InvoiceStatus.entries.map { invoiceStatusLabel(it) }

    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val headerStyle = invoiceHeaderStyle()
    val cellStyle = MaterialTheme.typography.bodyMedium
    val boldCellStyle = cellStyle.copy(fontWeight = FontWeight.Bold)
    val badgeStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
    val buttonStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
    fun widthOf(texts: List<String>, style: TextStyle, extra: Dp = 0.dp): Dp = with(density) {
        texts.maxOf { measurer.measure(it, style, maxLines = 1, softWrap = false).size.width }.toDp() + extra + 1.dp
    }
    fun headerWidth(text: String) = widthOf(listOf(text.uppercase()), headerStyle)
    val widths = InvoiceColumnWidths(
        due = maxOf(headerWidth(dueHeader), widthOf(dueDates + "00/00/0000", cellStyle)),
        total = maxOf(headerWidth(totalHeader), widthOf(totals, cellStyle)),
        remaining = maxOf(headerWidth(remainingHeader), widthOf(remainings, boldCellStyle)),
        // Badge: 12dp padding per side + 1dp border per side.
        status = maxOf(headerWidth(statusHeader), widthOf(statusLabels, badgeStyle, StatusBadgeHorizontalPadding * 2 + 2.dp)),
        // Button: 16dp content padding per side; never narrower than Material's minimum button width.
        action = maxOf(ButtonDefaults.MinWidth, widthOf(listOf(payNow), buttonStyle, 32.dp))
    )

    WebCard(modifier = modifier, contentPadding = 0.dp) {
        InvoiceTableHeader(widths)
        // Lazy: only the visible rows are composed (E-F2).
        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
            itemsIndexed(invoices, key = { _, invoice -> invoice.id }) { index, invoice ->
                HorizontalDivider(color = AptoOutlineVariant)
                InvoiceTableRow(
                    invoice = invoice,
                    dueText = dueDates[index],
                    totalText = totals[index],
                    remainingText = remainings[index],
                    widths = widths,
                    onClick = { onInvoiceClick(invoice) },
                    onPayNow = { onPayNowClick(invoice) }
                )
            }
        }
    }
}

private fun invoiceDueText(invoice: Invoice): String =
    // dd/MM/yyyy like Pagos and Dashboard (C-08)
    invoice.dueDate?.let { formatPaymentDate(it) } ?: "—"

@Composable
private fun invoiceHeaderStyle(): TextStyle =
    MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)

@Composable
private fun InvoiceTableHeader(widths: InvoiceColumnWidths) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TableHeaderBg)
            .padding(horizontal = RowHorizontalPadding, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(ColumnSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InvoiceHeaderCell(stringResource(Res.string.description_label), Modifier.weight(1f))
        InvoiceHeaderCell(stringResource(Res.string.web_invoices_col_due), Modifier.width(widths.due))
        InvoiceHeaderCell(stringResource(Res.string.total_label), Modifier.width(widths.total), TextAlign.End)
        InvoiceHeaderCell(stringResource(Res.string.remaining_label), Modifier.width(widths.remaining), TextAlign.End)
        InvoiceHeaderCell(stringResource(Res.string.web_invoices_col_status), Modifier.width(widths.status))
        Spacer(Modifier.width(widths.action))
    }
}

@Composable
private fun InvoiceHeaderCell(text: String, modifier: Modifier, textAlign: TextAlign = TextAlign.Start) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = invoiceHeaderStyle(),
        color = AptoOnSurfaceVariant,
        textAlign = textAlign,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun InvoiceTableRow(
    invoice: Invoice,
    dueText: String,
    totalText: String,
    remainingText: String,
    widths: InvoiceColumnWidths,
    onClick: () -> Unit,
    onPayNow: () -> Unit
) {
    val isActionable = invoice.remaining > 0 &&
            invoice.status != InvoiceStatus.PAID &&
            invoice.status != InvoiceStatus.CANCELLED
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (hovered) AptoSurfaceContainerLow else AptoSurfaceContainerLowest)
            .hoverable(interactionSource)
            .padding(end = RowHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Only the data cells open the detail; the "Pay" button is a sibling, not nested in the clickable row (E-F1).
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    onClickLabel = stringResource(Res.string.invoice_detail),
                    role = Role.Button,
                    onClick = onClick
                )
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(start = RowHorizontalPadding, end = ColumnSpacing, top = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(ColumnSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = invoiceTitle(invoice.description) ?: stringResource(Res.string.default_invoice_desc),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AptoOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(Res.string.period_label, invoiceMonthLabel(invoice.period)),
                    style = MaterialTheme.typography.bodySmall,
                    color = AptoOnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = dueText,
                modifier = Modifier.width(widths.due),
                style = MaterialTheme.typography.bodyMedium,
                color = if (invoice.status == InvoiceStatus.OVERDUE) AptoStatusError else AptoOnSurfaceVariant,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = totalText,
                modifier = Modifier.width(widths.total),
                style = MaterialTheme.typography.bodyMedium,
                color = AptoOnSurface,
                textAlign = TextAlign.End,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = remainingText,
                modifier = Modifier.width(widths.remaining),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (invoice.remaining <= 0) AptoStatusSuccess else AptoSecondaryContainer,
                textAlign = TextAlign.End,
                maxLines = 1,
                softWrap = false
            )
            Box(modifier = Modifier.width(widths.status)) {
                InvoiceStatusBadge(status = invoice.status)
            }
        }
        Box(modifier = Modifier.width(widths.action), contentAlignment = Alignment.CenterEnd) {
            if (isActionable) {
                Button(
                    onClick = onPayNow,
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AptoSecondaryContainer,
                        contentColor = AptoOnSecondary
                    )
                ) {
                    Text(
                        text = stringResource(Res.string.pay_now),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun InvoiceAmountLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = AptoOnSurfaceVariant,
        maxLines = 1,
        softWrap = false
    )
}

@Composable
private fun invoiceStatusLabel(status: InvoiceStatus): String = when (status) {
    InvoiceStatus.PAID -> stringResource(Res.string.status_paid)
    InvoiceStatus.OVERDUE -> stringResource(Res.string.status_overdue)
    InvoiceStatus.CANCELLED -> stringResource(Res.string.status_cancelled)
    InvoiceStatus.PENDING -> stringResource(Res.string.status_pending)
    InvoiceStatus.PARTIAL -> stringResource(Res.string.status_partial)
}.uppercase()

@Composable
fun InvoiceStatusBadge(status: InvoiceStatus) {
    val text = invoiceStatusLabel(status)
    val (containerColor, contentColor) = when (status) {
        InvoiceStatus.PAID -> AptoStatusSuccess.copy(alpha = 0.1f) to AptoStatusSuccess
        InvoiceStatus.OVERDUE -> AptoStatusError.copy(alpha = 0.1f) to AptoStatusError
        InvoiceStatus.CANCELLED -> AptoStatusError.copy(alpha = 0.1f) to AptoStatusError
        InvoiceStatus.PENDING -> AptoSecondaryFixed to AptoSecondary
        InvoiceStatus.PARTIAL -> AptoSecondaryFixed to AptoSecondary
    }

    Surface(
        color = badgeBg(containerColor, contentColor),
        contentColor = onStatusContainer(contentColor),
        shape = CircleShape,
        border = badgeBorder(contentColor)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = StatusBadgeHorizontalPadding, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun InvoiceEmptyState() {
    EmptyState(
        title = stringResource(Res.string.no_invoices_found),
        subtitle = stringResource(Res.string.no_invoices_for_filter),
        icon = Icons.Default.Receipt,
    )
}
