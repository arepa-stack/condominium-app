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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.condominio.data.utils.rememberExternalViewerLauncher
import com.example.condominio.ui.components.ErrorState
import com.example.condominio.ui.components.WebEmptyFrame
import com.example.condominio.ui.utils.formatCurrency
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource
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
    // PDF proofs (accepted by the upload since E-P3) can't be drawn as an image: open them externally.
    val externalViewer = rememberExternalViewerLauncher()
    val openProof: () -> Unit = {
        payment?.proofUrl?.let { url -> if (isPdfUrl(url)) externalViewer(url) else showFullImage = true }
    }

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
            loadFailed = uiState.loadFailed,
            onRetry = viewModel::loadPayment,
            onBackClick = onBackClick,
            onDownloadClick = { viewModel.onDownloadReceiptClick() },
            onShowFullImage = openProof
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
                title = stringResource(Res.string.payment_details_title),
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
                    text = stringResource(Res.string.download_receipt),
                    onClick = { viewModel.onDownloadReceiptClick() },
                    isLoading = uiState.isLoading,
                    icon = Icons.Default.Download
                )
                Spacer(modifier = Modifier.height(12.dp))
                SecondaryOutlinedButton(
                    text = stringResource(Res.string.contact_support),
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
                    text = stringResource(Res.string.currency_amount, formatCurrency(payment.amount)),
                    style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = (-0.5).sp),
                    color = AptoPrimary
                )

                Text(
                    text = stringResource(statusConfig.first),
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
                            text = stringResource(Res.string.transaction_details),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = AptoPrimary,
                            modifier = Modifier.semantics { heading() }
                        )
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        TransactionDetailRows(payment)
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
                        text = stringResource(Res.string.payment_proof),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = AptoPrimary,
                        modifier = Modifier.semantics { heading() }
                    )
                    // Only offered when there is a proof to open (E-P6)
                    if (payment.proofUrl != null) {
                        Text(
                            text = stringResource(Res.string.view_full),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = AptoSecondary,
                            modifier = Modifier.clickable(role = Role.Button, onClick = openProof)
                        )
                    }
                }

                ReceiptImageBox(
                    proofUrl = payment.proofUrl,
                    onClick = openProof
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
        } else if (uiState.loadFailed) {
            // Error with retry instead of a blank screen (A-07)
            ErrorState(
                title = stringResource(Res.string.error_loading_payment),
                onRetry = viewModel::loadPayment,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

/** Rows shared by the phone card and the web card. */
@Composable
private fun TransactionDetailRows(payment: Payment) {
    DetailRow(stringResource(Res.string.transaction_id), "#TXN-${payment.id.take(8)}")
    DetailRow(stringResource(Res.string.date_label), formatPaymentDate(payment.date))
    DetailRow(stringResource(Res.string.method_label), paymentMethodLabel(payment.method))
    payment.userName?.let { DetailRow(stringResource(Res.string.paid_by_label), it) }
    DetailRow(stringResource(Res.string.bank_label), payment.bank ?: "—")
    DetailRow(stringResource(Res.string.reference_label), payment.reference ?: "—")
}

/** Storage URLs keep the uploaded extension; query strings (signed URLs) are ignored. */
private fun isPdfUrl(url: String): Boolean = url.substringBefore('?').endsWith(".pdf", ignoreCase = true)

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

private fun paymentStatusConfig(status: PaymentStatus): Triple<StringResource, Color, ImageVector> = when (status) {
    PaymentStatus.APPROVED -> Triple(Res.string.payment_approved, AptoStatusSuccess, Icons.Default.CheckCircle)
    PaymentStatus.REJECTED -> Triple(Res.string.payment_rejected, AptoStatusError, Icons.Default.Warning)
    PaymentStatus.PENDING -> Triple(Res.string.payment_pending_approval, AptoStatusWarning, Icons.Default.Info)
}

@Composable
private fun ReceiptImageBox(
    proofUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val proofLabel = stringResource(Res.string.payment_proof_desc)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .background(AptoSurfaceContainerLow, RoundedCornerShape(12.dp))
            .border(1.dp, AptoOutlineVariant, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            // Clickable only when there is something to open (E-P6)
            .then(
                if (proofUrl != null) {
                    Modifier.clickable(onClickLabel = stringResource(Res.string.view_full), role = Role.Button, onClick = onClick)
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (proofUrl != null && isPdfUrl(proofUrl)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = proofLabel,
                    modifier = Modifier.size(64.dp),
                    tint = AptoSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.billboard_attachment_pdf),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = AptoSecondary
                )
            }
        } else if (proofUrl != null) {
            SubcomposeAsyncImage(
                model = proofUrl,
                contentDescription = proofLabel,
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
                    text = stringResource(Res.string.no_proof_uploaded),
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
    loadFailed: Boolean,
    onRetry: () -> Unit,
    onBackClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onShowFullImage: () -> Unit
) {
    Scaffold(
        topBar = {
            TopBarWithBack(
                title = stringResource(Res.string.payment_details_title),
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
                            Text(stringResource(Res.string.contact_support))
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
                            Text(stringResource(Res.string.download_receipt))
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
                                text = stringResource(Res.string.currency_amount, formatCurrency(payment.amount)),
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = AptoPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${formatPaymentDate(payment.date)} · ${paymentMethodLabel(payment.method)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AptoOutline
                            )
                        }

                        // Transaction details
                        WebCard {
                            WebCardTitle(stringResource(Res.string.transaction_details))
                            TransactionDetailRows(payment)
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
                                        value = stringResource(Res.string.currency_amount, formatCurrency(allocation.amount))
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
                                        text = stringResource(statusConfig.first),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = statusConfig.second
                                    )
                                    Text(
                                        text = formatPaymentDate(payment.processedAt ?: payment.date),
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
                                    text = stringResource(Res.string.payment_proof),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = AptoOnSurface
                                )
                                if (hasProof) {
                                    Text(
                                        text = stringResource(Res.string.view_full),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = AptoSecondary,
                                        modifier = Modifier
                                            .pointerHoverIcon(PointerIcon.Hand)
                                            .clickable(role = Role.Button) { openImage() }
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
        } else if (loadFailed) {
            // Error with retry instead of a blank page (A-07)
            Box(Modifier.padding(paddingValues)) {
                WebEmptyFrame {
                    ErrorState(title = stringResource(Res.string.error_loading_payment), onRetry = onRetry)
                }
            }
        }
    }
}
