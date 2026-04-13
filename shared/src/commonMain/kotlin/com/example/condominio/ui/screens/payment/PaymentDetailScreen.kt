package com.example.condominio.ui.screens.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import com.example.condominio.ui.utils.formatCurrency
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.condominio.data.model.PaymentStatus
import kotlinx.datetime.*
import org.koin.compose.viewmodel.koinViewModel
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDetailScreen(
    onBackClick: () -> Unit,
    viewModel: PaymentDetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val payment = uiState.payment

    // Simple KMP-friendly date formatter replacement
    fun formatDate(timestamp: Long): String {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${dateTime.dayOfMonth}/${dateTime.monthNumber}/${dateTime.year}"
    }

    fun formatTimestamp(timestamp: Long): String {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${dateTime.dayOfMonth}/${dateTime.monthNumber}/${dateTime.year} ${dateTime.hour}:${dateTime.minute}"
    }

    LaunchedEffect(uiState.pdfFile) {
        uiState.pdfFile?.let { file ->
            // TODO: Move platform specific PDF opening to expect/actual or a platform service
            println("PDF ready to be opened: ${file}")
            viewModel.onPdfShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Box(modifier = Modifier.fillMaxWidth().padding(end = 48.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(Res.string.payment_details_title), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share */ }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = stringResource(Res.string.attention))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { viewModel.onDownloadReceiptClick() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(Res.string.download_receipt), fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { /* Contact */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Mail, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.contact_support), fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        if (payment != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                // Status Indicator
                val statusColor = when (payment.status) {
                    PaymentStatus.APPROVED -> Color(0xFF4CAF50) // Green
                    PaymentStatus.PENDING -> Color(0xFFFFC107) // Amber
                    PaymentStatus.REJECTED -> Color(0xFFF44336) // Red
                }
                
                val statusBgColor = statusColor.copy(alpha = 0.1f)
                
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(statusBgColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "$${formatCurrency(payment.amount)}",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold)
                )
                
                Text(
                    text = when (payment.status) {
                        PaymentStatus.APPROVED -> stringResource(Res.string.payment_approved)
                        PaymentStatus.PENDING -> stringResource(Res.string.payment_pending_approval)
                        PaymentStatus.REJECTED -> stringResource(Res.string.payment_rejected)
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Transaction Details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(Res.string.transaction_details),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        DetailRow(stringResource(Res.string.transaction_id), "#TXN-${payment.id.take(8)}")
                        DetailRow(stringResource(Res.string.date_label), formatDate(payment.date))
                        payment.createdAt?.let { 
                            DetailRow(stringResource(Res.string.reported_on), formatTimestamp(it))
                        }
                        DetailRow(stringResource(Res.string.method_label), payment.method.label)
                        payment.userName?.let { DetailRow(stringResource(Res.string.paid_by_label), it) }
                        
                        // New Details
                        payment.bank?.let { DetailRow(stringResource(Res.string.bank_label), it) }
                        payment.reference?.let { DetailRow(stringResource(Res.string.reference_label), it) }
                        payment.phone?.let { DetailRow(stringResource(Res.string.mobile_label), it) }
                        
                        if (payment.status != PaymentStatus.PENDING && !payment.processorName.isNullOrEmpty()) {
                            DetailRow(stringResource(Res.string.processed_by_label), payment.processorName)
                        }

                        if (payment.allocations.isNotEmpty()) {
                            Text(
                                text = stringResource(Res.string.allocations_label),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(top=12.dp, bottom=8.dp)
                            )
                            payment.allocations.forEach { allocation ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (allocation.invoicePeriod != null) stringResource(Res.string.period_label_short, allocation.invoicePeriod) else stringResource(Res.string.invoice_id_short, allocation.invoiceId.takeLast(6)), 
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "$${formatCurrency(allocation.amount)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(top=8.dp))
                        }

                        DetailRow(stringResource(Res.string.description_label), payment.description)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Receipt Preview
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = stringResource(Res.string.payment_receipt_label),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = stringResource(Res.string.view_full),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.75f)
                        .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (payment.proofUrl != null) {
                        AsyncImage(
                            model = payment.proofUrl,
                            contentDescription = stringResource(Res.string.payment_proof_desc),
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(Res.string.no_receipt_uploaded),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        } else if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
}
