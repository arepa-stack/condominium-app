package com.example.condominio.ui.screens.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.condominio.data.model.Payment
import com.example.condominio.data.model.PaymentMethod
import com.example.condominio.data.model.PaymentStatus
import com.example.condominio.ui.components.ListItemCard
import com.example.condominio.ui.components.LoadingState
import com.example.condominio.ui.components.StatusBadge
import com.example.condominio.ui.components.TopBarWithBack
import com.example.condominio.ui.theme.*
import com.example.condominio.ui.utils.formatCurrency
import condominio.shared.generated.resources.*
import kotlinx.datetime.Instant
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
        containerColor = AptoBackground
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
                uiState.payments.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(192.dp)
                                    .background(AptoSurfaceContainerHigh, CircleShape)
                                    .padding(bottom = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = AptoOutlineVariant,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                            Text(
                                text = "No hay pagos registrados.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = AptoOnSurfaceVariant,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 24.dp)
                    ) {
                        item {
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

                        item {
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
    val formattedDate = try {
        val instant = Instant.fromEpochMilliseconds(payment.date)
        val date = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val months = listOf("", "Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
        val monthStr = if (date.monthNumber in 1..12) months[date.monthNumber] else date.monthNumber.toString()
        "${date.dayOfMonth} $monthStr, ${date.year}"
    } catch (e: Exception) {
        "Fecha Inválida"
    }

    val iconData = when (payment.method) {
        PaymentMethod.PAGO_MOVIL -> Pair(Icons.Default.Payments, AptoSecondary)
        PaymentMethod.TRANSFER -> Pair(Icons.Default.AccountBalanceWallet, AptoSecondary)
        PaymentMethod.CASH -> Pair(Icons.Default.HomeWork, AptoSecondary)
    }

    val statusData = when (payment.status) {
        PaymentStatus.APPROVED -> Pair("Aprobado", AptoStatusSuccess)
        PaymentStatus.REJECTED -> Pair("Rechazado", AptoStatusError)
        PaymentStatus.PENDING -> Pair("Pendiente", AptoStatusWarning)
    }

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
