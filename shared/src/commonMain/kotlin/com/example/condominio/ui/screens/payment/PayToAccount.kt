package com.example.condominio.ui.screens.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.condominio.data.model.PaymentMethod
import com.example.condominio.ui.components.isWide
import com.example.condominio.ui.utils.formatBs
import com.example.condominio.ui.utils.formatCurrency
import com.example.condominio.ui.utils.parseAmount
import coil3.compose.AsyncImage
import condominio.shared.generated.resources.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** "1. Datos de pago | 2. Registrar pago" header of the payment pager; tapping a tab scrolls to it. */
@Composable
internal fun PaymentPagerTabs(pagerState: PagerState) {
    val scope = rememberCoroutineScope()
    PrimaryTabRow(
        selectedTabIndex = pagerState.currentPage,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        listOf(Res.string.pay_to_tab_account, Res.string.pay_to_tab_register).forEachIndexed { index, label ->
            Tab(
                selected = pagerState.currentPage == index,
                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                text = { Text(stringResource(label), style = MaterialTheme.typography.labelLarge) },
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
            )
        }
    }
}

/** First pager page: where to pay (Pago Móvil or transfer). Choosing one also sets the form's method. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PayToAccountContent(
    uiState: CreatePaymentUiState,
    onMethodChange: (PaymentMethod) -> Unit,
    onRetryRate: () -> Unit,
    onRetryPaymentInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val method = uiState.method
    val totalUsd = parseAmount(uiState.amount) ?: 0.0
    // Fees are in USD but paid in Bs at the BCV rate. Bank apps take "9901,08" (no grouping) in the amount field.
    val totalBs = uiState.bcvRate?.let { formatBs(totalUsd * it) }
    val totalBsPlain = totalBs?.replace(".", "")
    @Suppress("DEPRECATION") // see PayToRow
    val clipboard = LocalClipboardManager.current
    // Set per building in the admin panel; fields the board left blank are not shown.
    val info = uiState.paymentInfo
    val bank = listOfNotNull(info?.bankName, info?.bankCode?.let { "($it)" }).joinToString(" ").ifBlank { null }
    // Cash (picked in the form's dropdown) has no account to pay to: no rows, copy or QR.
    val rows = when (method) {
        PaymentMethod.TRANSFER -> listOf(
            Res.string.pay_to_bank to bank,
            Res.string.pay_to_holder to info?.accountHolder,
            Res.string.pay_to_rif to info?.rif,
            Res.string.pay_to_account to info?.accountNumber,
            Res.string.pay_to_email to info?.email
        )
        PaymentMethod.PAGO_MOVIL -> listOf(
            Res.string.pay_to_bank to bank,
            Res.string.pay_to_phone to info?.pagoMovilPhone,
            Res.string.pay_to_rif to info?.rif
        )
        PaymentMethod.CASH -> emptyList()
    }.mapNotNull { (label, value) -> value?.takeIf { it.isNotBlank() }?.let { label to it } }
    val qrUrl = info?.qrUrl?.takeIf { method == PaymentMethod.PAGO_MOVIL }
    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.pay_to_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            // No mouse drag on the web pager: point to the tab instead of "swipe".
            text = stringResource(if (isWide()) Res.string.pay_to_hint_wide else Res.string.pay_to_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // Amount to pay (USD and Bs at today's BCV rate), in sight while paying in the bank app.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.total_to_pay).uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = stringResource(Res.string.currency_amount, formatCurrency(totalUsd)),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            when {
                uiState.isLoadingRate -> Text(
                    text = stringResource(Res.string.bcv_rate_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                uiState.bcvRate == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(Res.string.bcv_rate_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onRetryRate, modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)) {
                        Text(stringResource(Res.string.retry))
                    }
                }
                else -> {
                    Text(
                        text = stringResource(
                            Res.string.bcv_rate_value,
                            bcvDateLabel(uiState.bcvRateDate),
                            formatBs(uiState.bcvRate ?: 0.0)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(Res.string.amount_in_bs).uppercase(),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = stringResource(Res.string.bs_amount, totalBs.orEmpty()),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                        val amountLabel = stringResource(Res.string.amount_in_bs)
                        IconButton(
                            onClick = { clipboard.setText(AnnotatedString(totalBsPlain.orEmpty())) },
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = stringResource(Res.string.pay_to_copy, amountLabel),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf(PaymentMethod.PAGO_MOVIL, PaymentMethod.TRANSFER).forEachIndexed { index, option ->
                SegmentedButton(
                    selected = method == option,
                    onClick = { onMethodChange(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, 2),
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Text(
                        stringResource(
                            if (option == PaymentMethod.TRANSFER) Res.string.method_transfer_bank
                            else Res.string.method_pago_movil
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (method == PaymentMethod.CASH) {
            Text(
                text = stringResource(Res.string.cash_zelle_payment_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }
        when {
            uiState.isLoadingPaymentInfo -> {
                Text(
                    text = stringResource(Res.string.pay_to_info_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                return@Column
            }
            uiState.paymentInfoFailed -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(Res.string.pay_to_info_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onRetryPaymentInfo, modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)) {
                        Text(stringResource(Res.string.retry))
                    }
                }
                return@Column
            }
            rows.isEmpty() && qrUrl == null -> {
                // Board hasn't filled this method in the admin panel yet.
                Text(
                    text = stringResource(Res.string.pay_to_not_configured),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }
        }

        if (rows.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                rows.forEachIndexed { index, (label, value) ->
                    PayToRow(label, value, divider = index < rows.lastIndex)
                }
            }

            // "Copy all": one "Label: value" line per row of the selected method, ready to paste in the bank app.
            val amountLine = totalBsPlain?.takeIf { totalUsd > 0 }?.let { "${stringResource(Res.string.amount_in_bs)}: $it" }
            val allText = (rows.map { (label, value) -> "${stringResource(label)}: $value" } + listOfNotNull(amountLine))
                .joinToString("\n")
            var copied by remember(method) { mutableStateOf(false) }
            LaunchedEffect(copied) {
                if (copied) {
                    delay(2000)
                    copied = false
                }
            }
            OutlinedButton(
                onClick = {
                    clipboard.setText(AnnotatedString(allText))
                    copied = true
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(48.dp)
                    .pointerHoverIcon(PointerIcon.Hand)
            ) {
                Icon(
                    imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(if (copied) Res.string.pay_to_copied_all else Res.string.pay_to_copy_all),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
        }

        if (qrUrl != null) {
            Spacer(modifier = Modifier.height(16.dp))
            // The building's Pago Móvil QR (uploaded in the admin panel), white background so it scans in dark mode too.
            AsyncImage(
                model = qrUrl,
                contentDescription = stringResource(Res.string.pay_to_qr_desc),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .padding(8.dp)
            )
        }
    }
}

@Suppress("DEPRECATION") // LocalClipboard needs a platform ClipEntry per target; this one is common.
@Composable
private fun PayToRow(label: StringResource, value: String, divider: Boolean = true) {
    val clipboard = LocalClipboardManager.current
    val labelText = stringResource(label)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = labelText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(
            onClick = { clipboard.setText(AnnotatedString(value)) },
            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = stringResource(Res.string.pay_to_copy, labelText),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
    if (divider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/** "2026-10-27" -> "27/10/2026"; anything else as is. */
private fun bcvDateLabel(date: String?): String =
    date?.take(10)?.split("-")?.takeIf { it.size == 3 }?.reversed()?.joinToString("/") ?: date.orEmpty()
