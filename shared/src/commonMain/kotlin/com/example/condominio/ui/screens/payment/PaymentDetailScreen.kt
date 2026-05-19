package com.example.condominio.ui.screens.payment

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.example.condominio.data.model.PaymentStatus
import com.example.condominio.ui.components.FullScreenImageDialog
import com.example.condominio.ui.components.PrimaryButton
import com.example.condominio.ui.components.SecondaryOutlinedButton
import com.example.condominio.ui.components.TopBarWithBack
import com.example.condominio.ui.components.shimmerEffect
import com.example.condominio.ui.theme.*
import com.example.condominio.ui.utils.formatCurrency
import kotlinx.datetime.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDetailScreen(
    onBackClick: () -> Unit,
    viewModel: PaymentDetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val payment = uiState.payment
    var showFullImage by remember { mutableStateOf(false) }

    fun formatDate(timestamp: Long): String {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${dateTime.dayOfMonth}/${dateTime.monthNumber}/${dateTime.year}"
    }

    LaunchedEffect(uiState.pdfFile) {
        uiState.pdfFile?.let { file ->
            println("PDF ready to be opened: $file")
            viewModel.onPdfShown()
        }
    }

    Scaffold(
        topBar = {
            TopBarWithBack(
                title = "Detalle del Pago",
                onBackClick = onBackClick
            )
        },
        containerColor = AptoBackground,
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(AptoSurfaceContainerLowest)
                    .padding(16.dp)
            ) {
                PrimaryButton(
                    text = "Descargar Recibo",
                    onClick = { viewModel.onDownloadReceiptClick() },
                    isLoading = uiState.isLoading,
                    icon = Icons.Default.Download
                )
                Spacer(modifier = Modifier.height(12.dp))
                SecondaryOutlinedButton(
                    text = "Contactar Soporte",
                    onClick = { /* Contact Support */ },
                    icon = Icons.Default.Mail
                )
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
                // Success/Status Header
                Spacer(modifier = Modifier.height(32.dp))

                val statusConfig = when (payment.status) {
                    PaymentStatus.APPROVED -> Triple("Pago Aprobado", AptoStatusSuccess, Icons.Default.CheckCircle)
                    PaymentStatus.REJECTED -> Triple("Pago Rechazado", AptoStatusError, Icons.Default.Warning)
                    PaymentStatus.PENDING -> Triple("Pago Pendiente", AptoStatusWarning, Icons.Default.Info)
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(statusConfig.second.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = statusConfig.third,
                        contentDescription = null,
                        tint = statusConfig.second,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "$${formatCurrency(payment.amount)}",
                    style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = (-0.5).sp),
                    color = AptoPrimary
                )

                Text(
                    text = statusConfig.first,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = AptoOnSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Transaction Details Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AptoSurfaceContainerLowest, RoundedCornerShape(12.dp))
                        .border(1.dp, AptoOutlineVariant, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AptoSurfaceContainerLow)
                            .border(1.dp, AptoOutlineVariant)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Detalles de la Transacción",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = AptoPrimary
                        )
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        DetailRow("ID de Transacción", "#TXN-${payment.id.take(8)}")
                        DetailRow("Fecha", formatDate(payment.date))
                        DetailRow("Método", payment.method.label)
                        payment.userName?.let { DetailRow("Pagado por", it) }
                        DetailRow("Banco", payment.bank ?: "—")
                        DetailRow("Referencia", payment.reference ?: "—")
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
                        text = "Comprobante de Pago",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = AptoPrimary
                    )
                    Text(
                        text = "Ver Completo",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = AptoSecondary,
                        modifier = Modifier.clickable {
                            if (payment.proofUrl != null) showFullImage = true
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.75f)
                        .background(AptoSurfaceContainerLow, RoundedCornerShape(12.dp))
                        .border(1.dp, AptoOutlineVariant, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { if (payment.proofUrl != null) showFullImage = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (payment.proofUrl != null) {
                        SubcomposeAsyncImage(
                            model = payment.proofUrl,
                            contentDescription = "Comprobante de Pago",
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop,
                            loading = {
                                Box(modifier = Modifier.fillMaxSize().shimmerEffect())
                            }
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = AptoOutlineVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Sin Comprobante",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = AptoOutline
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            if (showFullImage && payment.proofUrl != null) {
                FullScreenImageDialog(
                    imageUrl = payment.proofUrl,
                    onDismiss = { showFullImage = false },
                )
            }
        } else if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AptoSecondary)
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
            color = AptoOutline
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = AptoOnSurface
        )
    }
    HorizontalDivider(color = AptoOutlineVariant, thickness = 0.5.dp)
}
