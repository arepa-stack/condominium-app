package com.example.condominio.ui.screens.dashboard

import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Support
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.condominio.data.model.Payment
import com.example.condominio.data.model.PaymentStatus
import org.koin.compose.viewmodel.koinViewModel
import com.example.condominio.ui.utils.formatCurrency
import com.example.condominio.ui.utils.formatDate
import org.jetbrains.compose.resources.stringResource
import condominio.shared.generated.resources.*

@Composable
fun DashboardScreen(
        onPayClick: () -> Unit,
        onHistoryClick: () -> Unit,
        onPaymentClick: (String) -> Unit,
        onProfileClick: () -> Unit,
        onUnitClick: () -> Unit,
        onSeeAllInvoicesClick: () -> Unit = {},
        onPettyCashClick: () -> Unit = {},
        viewModel: DashboardViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
                modifier =
                        Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                HeaderSection(
                        userName = uiState.userName,
                        building = uiState.userBuilding,
                        apartmentUnit = uiState.userApartment,
                        onProfileClick = onProfileClick,
                        onUnitClick = onUnitClick
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                BillingCard(
                        totalDebt = uiState.totalDebt,
                        pendingInvoices = uiState.pendingInvoices,
                        onSeeAllClick = onSeeAllInvoicesClick
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                QuickActions(
                        onPayClick = onPayClick,
                        onHistoryClick = onHistoryClick,
                        onPettyCashClick = onPettyCashClick
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                Text(
                        text = stringResource(Res.string.recent_transactions),
                        style =
                                MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                ),
                        modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            items(uiState.recentPayments) { payment ->
                TransactionItem(payment = payment, onClick = { onPaymentClick(payment.id) })
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun BillingCard(
        totalDebt: Double,
        pendingInvoices: List<com.example.condominio.data.model.Invoice>,
        onSeeAllClick: () -> Unit = {}
) {
    val isSolvent = totalDebt <= 0
    val cardColor = if (isSolvent) Color(0xFF4CAF50) else Color(0xFFD32F2F) // Green or Red

    Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = stringResource(Res.string.total_debt),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f)
                )
                TextButton(
                        onClick = onSeeAllClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) { Text(stringResource(Res.string.see_all), style = MaterialTheme.typography.labelLarge) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                    text = "$${formatCurrency(totalDebt)}",
                    style =
                            MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Bold
                            ),
                    color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (pendingInvoices.isNotEmpty()) {
                Text(
                        text = stringResource(Res.string.pending_invoices_label),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                pendingInvoices.take(3).forEach { invoice ->
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                                text = invoice.description ?: invoice.period,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                                text = "$${formatCurrency(invoice.remaining)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            } else {
                Text(
                        text = stringResource(Res.string.up_to_date),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun HeaderSection(
        userName: String,
        building: String = "",
        apartmentUnit: String = "",
        onProfileClick: () -> Unit = {},
        onUnitClick: () -> Unit = {}
) {
    Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.clickable(onClick = onUnitClick)) {
            Text(
                    text = stringResource(Res.string.welcome_back),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                    text = userName,
                    style =
                            MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                            )
            )
            if (building.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                            text = stringResource(Res.string.apt_unit_label, building, apartmentUnit),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                    )
                    Icon(
                            imageVector =
                                    Icons.Default.ArrowDropDown,
                            contentDescription = stringResource(Res.string.select_unit),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        // Avatar - Clickable to open profile
        Surface(
                modifier = Modifier.size(52.dp).clickable(onClick = onProfileClick),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 2.dp,
                border =
                        androidx.compose.foundation.BorderStroke(
                                2.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (userName.isNotEmpty()) {
                    Text(
                            text = userName.take(1).uppercase(),
                            style =
                                    MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold
                                    ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = stringResource(Res.string.profile),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActions(
        onPayClick: () -> Unit,
        onHistoryClick: () -> Unit,
        onPettyCashClick: () -> Unit = {}
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        QuickActionItem(
                icon = Icons.Default.Payment,
                label = stringResource(Res.string.pay_now),
                color = Color(0xFFFF6D00), // Orange
                onClick = onPayClick,
                modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        QuickActionItem(
                icon = Icons.Default.History,
                label = stringResource(Res.string.history),
                color = Color(0xFF0091EA), // Blue
                onClick = onHistoryClick,
                modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        QuickActionItem(
                icon = Icons.Default.AccountBalanceWallet,
                label = stringResource(Res.string.petty_cash),
                color = Color(0xFF00FF80), // Neon Green
                onClick = onPettyCashClick,
                modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        QuickActionItem(
                icon = Icons.Default.Support,
                label = stringResource(Res.string.support),
                color = Color(0xFF43A047), // Green
                onClick = {},
                modifier = Modifier.weight(1f)
        )
        // Profile removed as requested
    }
}

@Composable
fun QuickActionItem(
        icon: ImageVector,
        label: String,
        color: Color,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
) {
    Column(
            modifier = modifier.clickable(onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
                modifier =
                        Modifier.size(56.dp)
                                .background(color.copy(alpha = 0.1f), CircleShape)
                                .border(1.dp, color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
        ) { Icon(imageVector = icon, contentDescription = label, tint = color) }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun TransactionItem(payment: Payment, onClick: () -> Unit) {


    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(16.dp)
                            )
                            .clickable(onClick = onClick)
                            .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
                modifier =
                        Modifier.size(48.dp)
                                .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                ),
                contentAlignment = Alignment.Center
        ) {
            Icon(
                    imageVector = Icons.Default.Payment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                    text = payment.description.ifBlank { stringResource(Res.string.payment_item_label) },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                    text = formatDate(payment.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            val statusColor =
                    when (payment.status) {
                        PaymentStatus.APPROVED -> Color(0xFF4CAF50)
                        PaymentStatus.PENDING -> Color(0xFFFFC107)
                        PaymentStatus.REJECTED -> Color(0xFFF44336)
                    }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                        text = payment.status.name,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor
                )
                if (payment.status != PaymentStatus.PENDING && !payment.processorName.isNullOrEmpty()) {
                    Text(
                            text = stringResource(Res.string.processed_by_short, payment.processorName),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
        Text(
                text = "$${formatCurrency(payment.amount)}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
    }
}

