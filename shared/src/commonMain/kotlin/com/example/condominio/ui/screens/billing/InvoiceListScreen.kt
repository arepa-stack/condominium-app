package com.example.condominio.ui.screens.billing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.condominio.ui.utils.formatCurrency
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.material.icons.filled.Refresh
import com.example.condominio.data.model.Invoice
import com.example.condominio.data.model.InvoiceStatus
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceListScreen(
    onBackClick: () -> Unit,
    onInvoiceClick: (Invoice) -> Unit,
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
            1 -> uiState.invoices.filter { it.status != InvoiceStatus.PAID }
            2 -> uiState.invoices.filter { it.status == InvoiceStatus.PAID }
            else -> uiState.invoices
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.invoices_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.refresh))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.error!!.asString(), color = MaterialTheme.colorScheme.error)
                }
            } else if (filteredInvoices.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(Res.string.no_invoices_found))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredInvoices) { invoice ->
                        InvoiceItem(
                            invoice = invoice,
                            onClick = { onInvoiceClick(invoice) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceItem(
    invoice: Invoice,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (invoice.type == com.example.condominio.data.model.InvoiceType.PETTY_CASH_REPLENISHMENT) {
                            Icon(
                                imageVector = Icons.Default.Build, // Using Build as a proxy for maintenance/petty cash
                                contentDescription = stringResource(Res.string.petty_cash_title),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp).padding(end = 4.dp)
                            )
                        }
                        Text(
                            text = invoice.description ?: stringResource(Res.string.default_invoice_desc),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = stringResource(Res.string.period_label, invoice.period),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                InvoiceStatusBadge(status = invoice.status)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val progress = if (invoice.amount > 0) (invoice.paid / invoice.amount).toFloat() else 0f
            
            if (invoice.paid > 0 && invoice.status != InvoiceStatus.PAID) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.extraSmall),
                    color = Color(0xFF2E7D32),
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = stringResource(Res.string.total_label), style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = "$${formatCurrency(invoice.amount)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (invoice.paid > 0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = stringResource(Res.string.paid_label), style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "$${formatCurrency(invoice.paid)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = stringResource(Res.string.remaining_label), style = MaterialTheme.typography.labelSmall)
                    val remainingColor = if (invoice.remaining > 0) {
                        if (invoice.status == InvoiceStatus.OVERDUE) MaterialTheme.colorScheme.error else Color(0xFFE65100)
                    } else {
                        Color(0xFF2E7D32)
                    }
                    Text(
                        text = "$${formatCurrency(invoice.remaining)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = remainingColor
                    )
                }
            }
        }
    }
}

@Composable
fun InvoiceStatusBadge(status: InvoiceStatus) {
    val (text, containerColor, contentColor) = when (status) {
        InvoiceStatus.PAID -> Triple(stringResource(Res.string.status_paid), Color(0xFFE8F5E9), Color(0xFF2E7D32))
        InvoiceStatus.OVERDUE -> Triple(stringResource(Res.string.status_overdue), Color(0xFFFFEBEE), Color(0xFFC62828))
        InvoiceStatus.CANCELLED -> Triple(stringResource(Res.string.status_cancelled), Color(0xFFFFEBEE), Color(0xFFC62828))
        InvoiceStatus.PENDING -> Triple(stringResource(Res.string.status_pending), Color(0xFFF5F5F5), Color(0xFF616161))
        InvoiceStatus.PARTIAL -> Triple("PARCIAL", Color(0xFFFFF3E0), Color(0xFFE65100))
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

