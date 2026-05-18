package com.example.condominio.ui.screens.billing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.condominio.data.model.Invoice
import com.example.condominio.data.model.InvoiceStatus
import com.example.condominio.ui.theme.*
import com.example.condominio.ui.utils.formatCurrency
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
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.invoices_title),
                        fontWeight = FontWeight.Bold,
                        color = AptoPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                            tint = AptoPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(Res.string.refresh),
                            tint = AptoPrimary
                        )
                    }
                    IconButton(onClick = { }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(Res.string.settings),
                            tint = AptoPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AptoSurface)
            )
        },
        containerColor = AptoBackground
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(
                selectedTabIndex = selectedTab,
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
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AptoSecondaryContainer)
                    }
                }
                uiState.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = uiState.error!!.asString(), color = AptoStatusError)
                    }
                }
                filteredInvoices.isEmpty() -> InvoiceEmptyState()
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
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

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AptoSurfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, AptoOutlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
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
        color = containerColor,
        contentColor = contentColor,
        shape = CircleShape
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.size(96.dp),
                shape = CircleShape,
                color = AptoSurfaceContainerHigh
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = AptoOutline,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.no_invoices_found),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AptoOnSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(Res.string.no_invoices_for_filter),
                style = MaterialTheme.typography.bodyMedium,
                color = AptoOnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
