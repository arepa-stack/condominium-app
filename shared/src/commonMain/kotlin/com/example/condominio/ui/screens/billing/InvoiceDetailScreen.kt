package com.example.condominio.ui.screens.billing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import com.example.condominio.data.model.Payment
import com.example.condominio.ui.utils.formatDate
import com.example.condominio.ui.utils.formatCurrency
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    onBackClick: () -> Unit,
    onSeeAllPaymentsClick: () -> Unit,
    onSeeAllInvoicesClick: () -> Unit,
    onPayRemainderClick: (String) -> Unit,
    onPaymentClick: (String) -> Unit,
    viewModel: InvoiceDetailViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Factura") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.invoice != null && uiState.invoice!!.remaining > 0 && uiState.invoice!!.status != com.example.condominio.data.model.InvoiceStatus.CANCELLED) {
                Button(
                    onClick = { onPayRemainderClick(uiState.invoice!!.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6D00))
                ) {
                    Text("Pagar Restante ($${formatCurrency(uiState.invoice!!.remaining)})")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Invoice Summary Card
            if (uiState.invoice != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = uiState.invoice!!.description ?: "Factura ${uiState.invoice!!.period}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Monto Total: $${formatCurrency(uiState.invoice!!.amount)}")
                            Text("Pagado: $${formatCurrency(uiState.invoice!!.paid)}")
                        }
                    }
                }
            }

            Text(
                text = "Historial de Pagos",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.payments.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("No hay pagos registrados para esta factura")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.payments) { payment ->
                        PaymentItem(
                            payment = payment, 
                            invoiceId = uiState.invoice?.id,
                            onClick = { onPaymentClick(payment.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                OutlinedButton(onClick = onSeeAllPaymentsClick) {
                    Text("Ver Todos los Pagos")
                }
                OutlinedButton(onClick = onSeeAllInvoicesClick) {
                    Text("Ver Facturas")
                }
            }
        }
    }
}

@Composable
fun PaymentItem(payment: Payment, invoiceId: String?, onClick: () -> Unit) {
    // Find allocation for this invoice
    val allocation = invoiceId?.let { id -> 
        payment.allocations.find { it.invoiceId == id } 
    }
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDate(payment.date, "dd/MM/yyyy"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Column(horizontalAlignment = Alignment.End) {
                    // Show allocated amount if available and different from total
                    if (allocation != null) {
                         Text(
                            text = "Aplicado: $${formatCurrency(allocation.amount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                        if (allocation.amount != payment.amount) {
                            Text(
                                text = "Total: $${formatCurrency(payment.amount)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                         Text(
                            text = "$${formatCurrency(payment.amount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
            Text(
                text = "Método: ${payment.method.label}",
                style = MaterialTheme.typography.bodySmall
            )
            if (!payment.reference.isNullOrEmpty()) {
                Text(
                    text = "Ref: ${payment.reference}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
