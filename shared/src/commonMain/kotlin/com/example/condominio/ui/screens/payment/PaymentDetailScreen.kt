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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.example.condominio.data.model.Payment
import com.example.condominio.data.model.PaymentStatus
import com.example.condominio.ui.components.FullScreenImageDialog
import com.example.condominio.ui.components.LoadingState
import com.example.condominio.ui.components.PrimaryButton
import com.example.condominio.ui.components.SecondaryOutlinedButton
import com.example.condominio.ui.components.TopBarWithBack
import com.example.condominio.ui.components.AdaptiveColumns
import com.example.condominio.ui.components.WebCard
import com.example.condominio.ui.components.isExpanded
import com.example.condominio.ui.components.pagePadding
import com.example.condominio.ui.components.screenBackground
import com.example.condominio.ui.components.shimmerEffect
import com.example.condominio.ui.theme.*
import com.example.condominio.ui.utils.formatCurrency
import condominio.shared.generated.resources.Res
import condominio.shared.generated.resources.allocations_label
import condominio.shared.generated.resources.period_label_short
import kotlinx.datetime.*
import org.jetbrains.compose.resources.stringResource
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

    LaunchedEffect(uiState.pdfFile) {
        uiState.pdfFile?.let { file ->
            println("PDF ready to be opened: $file")
            viewModel.onPdfShown()
        }
    }

    if (isExpanded()) {
        PaymentDetailWeb(
            payment = payment,
            isLoading = uiState.isLoading,
            onBackClick = onBackClick,
            onDownloadClick = { viewModel.onDownloadReceiptClick() },
            onShowFullImage = { showFullImage = true }
        )
        if (showFullImage && payment?.proofUrl != null) {
            FullScreenImageDialog(
                imageUrl = payment.proofUrl,
                onDismiss = { showFullImage = false },
            )
        }
        return
    }

    Scaffold(
        topBar = {
            TopBarWithBack(
                title = "Detalle del Pago",
                onBackClick = onBackClick
            )
        },
        containerColor = screenBackground(AptoBackground),
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
                    // 16dp on phone (unchanged), 32dp on Medium to align with the page header.
                    .padding(horizontal = pagePadding()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success/Status Header
                Spacer(modifier = Modifier.height(32.dp))

                val statusConfig = paymentStatusConfig(payment.status)

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

                ReceiptImageBox(
                    proofUrl = payment.proofUrl,
                    onClick = { if (payment.proofUrl != null) showFullImage = true }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }

            if (showFullImage && payment.proofUrl != null) {
                FullScreenImageDialog(
                    imageUrl = payment.proofUrl,
                    onDismiss = { showFullImage = false },
                )
            }
        } else if (uiState.isLoading) {
            LoadingState()
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

private fun formatDate(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dateTime.dayOfMonth}/${dateTime.monthNumber}/${dateTime.year}"
}

private fun paymentStatusConfig(status: PaymentStatus): Triple<String, Color, ImageVector> = when (status) {
    PaymentStatus.APPROVED -> Triple("Pago Aprobado", AptoStatusSuccess, Icons.Default.CheckCircle)
    PaymentStatus.REJECTED -> Triple("Pago Rechazado", AptoStatusError, Icons.Default.Warning)
    PaymentStatus.PENDING -> Triple("Pago Pendiente", AptoStatusWarning, Icons.Default.Info)
}

@Composable
private fun ReceiptImageBox(
    proofUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .background(AptoSurfaceContainerLow, RoundedCornerShape(12.dp))
            .border(1.dp, AptoOutlineVariant, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (proofUrl != null) {
            SubcomposeAsyncImage(
                model = proofUrl,
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
}

@Composable
private fun WebCardTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = AptoOnSurface,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

/** Desktop web layout (Expanded only): payment data on the left, status + receipt on the right. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentDetailWeb(
    payment: Payment?,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onShowFullImage: () -> Unit
) {
    Scaffold(
        topBar = {
            TopBarWithBack(
                title = "Detalle del Pago",
                onBackClick = onBackClick,
                actions = {
                    if (payment != null) {
                        OutlinedButton(
                            onClick = { /* Contact Support */ },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Icon(Icons.Default.Mail, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Contactar Soporte")
                        }
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = onDownloadClick,
                            enabled = !isLoading,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("Descargar Recibo")
                        }
                    }
                }
            )
        },
        containerColor = screenBackground(AptoBackground)
    ) { paddingValues ->
        if (payment != null) {
            val statusConfig = paymentStatusConfig(payment.status)
            val hasProof = payment.proofUrl != null
            val openImage: () -> Unit = { if (hasProof) onShowFullImage() }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = pagePadding())
                    .padding(top = 8.dp, bottom = 32.dp)
            ) {
                AdaptiveColumns(
                    main = {
                        // Amount hero
                        WebCard {
                            Text(
                                text = payment.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AptoOnSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "$${formatCurrency(payment.amount)}",
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = AptoPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${formatDate(payment.date)} · ${payment.method.label}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AptoOutline
                            )
                        }

                        // Transaction details
                        WebCard {
                            WebCardTitle("Detalles de la Transacción")
                            DetailRow("ID de Transacción", "#TXN-${payment.id.take(8)}")
                            DetailRow("Fecha", formatDate(payment.date))
                            DetailRow("Método", payment.method.label)
                            payment.userName?.let { DetailRow("Pagado por", it) }
                            DetailRow("Banco", payment.bank ?: "—")
                            DetailRow("Referencia", payment.reference ?: "—")
                        }

                        // Invoice allocations
                        if (payment.allocations.isNotEmpty()) {
                            WebCard {
                                WebCardTitle(stringResource(Res.string.allocations_label))
                                payment.allocations.forEach { allocation ->
                                    val period = allocation.invoicePeriod
                                    DetailRow(
                                        label = if (period != null) {
                                            stringResource(Res.string.period_label_short, period)
                                        } else {
                                            "#${allocation.invoiceId.take(8)}"
                                        },
                                        value = "$${formatCurrency(allocation.amount)}"
                                    )
                                }
                            }
                        }
                    },
                    side = {
                        // Status
                        WebCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(statusConfig.second.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = statusConfig.third,
                                        contentDescription = null,
                                        tint = statusConfig.second,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = statusConfig.first,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = statusConfig.second
                                    )
                                    Text(
                                        text = formatDate(payment.processedAt ?: payment.date),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AptoOutline
                                    )
                                }
                            }
                        }

                        // Receipt
                        WebCard {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Comprobante de Pago",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = AptoOnSurface
                                )
                                if (hasProof) {
                                    Text(
                                        text = "Ver Completo",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = AptoSecondary,
                                        modifier = Modifier
                                            .pointerHoverIcon(PointerIcon.Hand)
                                            .clickable { openImage() }
                                    )
                                }
                            }
                            ReceiptImageBox(
                                proofUrl = payment.proofUrl,
                                onClick = openImage,
                                modifier = if (hasProof) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier
                            )
                        }
                    }
                )
            }
        } else if (isLoading) {
            LoadingState()
        }
    }
}
