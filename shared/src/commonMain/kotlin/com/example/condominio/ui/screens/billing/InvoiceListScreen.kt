package com.example.condominio.ui.screens.billing

import com.example.condominio.ui.components.isWide
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.condominio.data.model.Invoice
import androidx.compose.foundation.layout.PaddingValues
import com.example.condominio.data.model.InvoiceStatus
import com.example.condominio.ui.components.EmptyState
import com.example.condominio.ui.components.ListItemCard
import com.example.condominio.ui.components.LoadingState
import com.example.condominio.ui.components.TopBarWithBack
import com.example.condominio.ui.components.WebCard
import com.example.condominio.ui.components.WebEmptyFrame
import com.example.condominio.ui.components.isExpanded
import com.example.condominio.ui.components.pagePadding
import com.example.condominio.ui.components.screenBackground
import com.example.condominio.ui.theme.*
import com.example.condominio.ui.utils.formatCurrency
import com.example.condominio.ui.utils.formatDate
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
    var selectedTab by remember { mutableStateOf(0) }
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
                onTabSelected = { selectedTab = it },
                isLoading = uiState.isLoading,
                error = uiState.error?.asString(),
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
                        onClick = { selectedTab = index },
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
                uiState.error != null -> WebEmptyFrame {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = uiState.error!!.asString(), color = AptoStatusError)
                    }
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
                        items(filteredInvoices) { invoice ->
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

    ListItemCard(
        onClick = onClick,
        padding = PaddingValues(24.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = invoice.description ?: stringResource(Res.string.default_invoice_desc),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AptoOnSurface
                    )
                    Text(
                        text = stringResource(Res.string.period_label, invoice.period),
                        style = MaterialTheme.typography.bodySmall,
                        color = AptoOnSurfaceVariant
                    )
                }
                InvoiceStatusBadge(status = invoice.status)
            }

            HorizontalDivider(color = AptoOutlineVariant)

            if (showPaidColumn) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        InvoiceAmountLabel(stringResource(Res.string.total_label))
                        Text(
                            text = stringResource(Res.string.currency_amount, formatCurrency(invoice.amount)),
                            style = MaterialTheme.typography.bodyLarge,
                            color = AptoOnSurface
                        )
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        InvoiceAmountLabel(stringResource(Res.string.paid_label))
                        Text(
                            text = stringResource(Res.string.currency_amount, formatCurrency(invoice.paid)),
                            style = MaterialTheme.typography.bodyLarge,
                            color = AptoStatusSuccess,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        InvoiceAmountLabel(stringResource(Res.string.remaining_label))
                        val remainingColor = if (invoice.remaining <= 0) AptoStatusSuccess else AptoSecondaryContainer
                        Text(
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
                        Text(
                            text = stringResource(Res.string.currency_amount, formatCurrency(invoice.amount)),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = AptoOnSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        InvoiceAmountLabel(stringResource(Res.string.remaining_label))
                        Text(
                            text = stringResource(Res.string.currency_amount, formatCurrency(invoice.remaining)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AptoSecondaryContainer
                        )
                    }
                }
            }

            if (isActionable) {
                Button(
                    onClick = onPayNow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
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

// ---------------------------------------------------------------------------
// Expanded (desktop web): filter chips + invoice table inside a WebCard.
// Compact/Medium keep the original TabRow + grid above.
// ---------------------------------------------------------------------------

private const val COL_CONCEPT = 2.4f
private const val COL_DUE = 1.2f
private const val COL_AMOUNT = 1.1f
private const val COL_REMAINING = 1.1f
private const val COL_STATUS = 1.1f
private const val COL_ACTION = 1.3f

@Composable
private fun InvoiceWebContent(
    modifier: Modifier,
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    isLoading: Boolean,
    error: String?,
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
                    Text(text = error, color = AptoStatusError)
                }
            }
            // Fixed height: EmptyState fills its parent, which would stretch the card to the whole page.
            invoices.isEmpty() -> WebCard { Box(Modifier.fillMaxWidth().height(320.dp)) { InvoiceEmptyState() } }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 32.dp)
                ) {
                    WebCard(contentPadding = 0.dp) {
                        InvoiceTableHeader()
                        invoices.forEach { invoice ->
                            HorizontalDivider(color = AptoOutlineVariant)
                            InvoiceTableRow(
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
private fun InvoiceTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TableHeaderBg)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InvoiceHeaderCell(stringResource(Res.string.description_label), Modifier.weight(COL_CONCEPT))
        InvoiceHeaderCell(stringResource(Res.string.web_invoices_col_due), Modifier.weight(COL_DUE))
        InvoiceHeaderCell(stringResource(Res.string.total_label), Modifier.weight(COL_AMOUNT), TextAlign.End)
        InvoiceHeaderCell(stringResource(Res.string.remaining_label), Modifier.weight(COL_REMAINING), TextAlign.End)
        Spacer(Modifier.width(24.dp))
        InvoiceHeaderCell(stringResource(Res.string.web_invoices_col_status), Modifier.weight(COL_STATUS))
        Spacer(Modifier.weight(COL_ACTION))
    }
}

@Composable
private fun InvoiceHeaderCell(text: String, modifier: Modifier, textAlign: TextAlign = TextAlign.Start) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = AptoOnSurfaceVariant,
        textAlign = textAlign
    )
}

@Composable
private fun InvoiceTableRow(
    invoice: Invoice,
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
            .clickable(onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(COL_CONCEPT).padding(end = 16.dp)) {
            Text(
                text = invoice.description ?: stringResource(Res.string.default_invoice_desc),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = AptoOnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(Res.string.period_label, invoice.period),
                style = MaterialTheme.typography.bodySmall,
                color = AptoOnSurfaceVariant
            )
        }
        Text(
            text = invoice.dueDate?.let { formatDate(it) } ?: "—",
            modifier = Modifier.weight(COL_DUE),
            style = MaterialTheme.typography.bodyMedium,
            color = if (invoice.status == InvoiceStatus.OVERDUE) AptoStatusError else AptoOnSurfaceVariant
        )
        Text(
            text = stringResource(Res.string.currency_amount, formatCurrency(invoice.amount)),
            modifier = Modifier.weight(COL_AMOUNT),
            style = MaterialTheme.typography.bodyMedium,
            color = AptoOnSurface,
            textAlign = TextAlign.End
        )
        Text(
            text = stringResource(Res.string.currency_amount, formatCurrency(invoice.remaining)),
            modifier = Modifier.weight(COL_REMAINING),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (invoice.remaining <= 0) AptoStatusSuccess else AptoSecondaryContainer,
            textAlign = TextAlign.End
        )
        Spacer(Modifier.width(24.dp))
        Box(modifier = Modifier.weight(COL_STATUS)) {
            InvoiceStatusBadge(status = invoice.status)
        }
        Box(modifier = Modifier.weight(COL_ACTION), contentAlignment = Alignment.CenterEnd) {
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
                        maxLines = 1
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
        color = AptoOnSurfaceVariant
    )
}

@Composable
fun InvoiceStatusBadge(status: InvoiceStatus) {
    val (text, containerColor, contentColor) = when (status) {
        InvoiceStatus.PAID -> Triple(
            stringResource(Res.string.status_paid),
            AptoStatusSuccess.copy(alpha = 0.1f),
            AptoStatusSuccess
        )
        InvoiceStatus.OVERDUE -> Triple(
            stringResource(Res.string.status_overdue),
            AptoStatusError.copy(alpha = 0.1f),
            AptoStatusError
        )
        InvoiceStatus.CANCELLED -> Triple(
            stringResource(Res.string.status_cancelled),
            AptoStatusError.copy(alpha = 0.1f),
            AptoStatusError
        )
        InvoiceStatus.PENDING -> Triple(
            stringResource(Res.string.status_pending),
            AptoSecondaryFixed,
            AptoSecondary
        )
        InvoiceStatus.PARTIAL -> Triple(
            stringResource(Res.string.status_partial),
            AptoSecondaryFixed,
            AptoSecondary
        )
    }

    Surface(
        color = badgeBg(containerColor, contentColor),
        contentColor = contentColor,
        shape = CircleShape,
        border = badgeBorder(contentColor)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
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
