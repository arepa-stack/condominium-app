package com.example.condominio.ui.screens.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.condominio.data.model.PaymentMethod
import com.example.condominio.ui.theme.AptoSurfaceContainerHigh
import condominio.shared.generated.resources.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// ponytail: hardcoded for Residencias Granada; move to a building endpoint when the API exposes it.
private object PayTo {
    const val BANK = "Bancaribe (0114)"
    const val HOLDER = "Edificio Residencias Granada"
    const val RIF = "J-315383128"
    const val ACCOUNT = "0114-0184-8618-4005-4353"
    const val PHONE = "0412-3870654"
    const val EMAIL = "condominio.granada2026@gmail.com"
}

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
    method: PaymentMethod,
    onMethodChange: (PaymentMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    val isTransfer = method == PaymentMethod.TRANSFER
    @Suppress("DEPRECATION") // see PayToRow
    val clipboard = LocalClipboardManager.current
    val rows = if (isTransfer) listOf(
        Res.string.pay_to_bank to PayTo.BANK,
        Res.string.pay_to_holder to PayTo.HOLDER,
        Res.string.pay_to_rif to PayTo.RIF,
        Res.string.pay_to_account to PayTo.ACCOUNT,
        Res.string.pay_to_email to PayTo.EMAIL
    ) else listOf(
        Res.string.pay_to_bank to PayTo.BANK,
        Res.string.pay_to_phone to PayTo.PHONE,
        Res.string.pay_to_rif to PayTo.RIF
    )
    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.pay_to_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = stringResource(Res.string.pay_to_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf(PaymentMethod.PAGO_MOVIL, PaymentMethod.TRANSFER).forEachIndexed { index, option ->
                SegmentedButton(
                    selected = (option == PaymentMethod.TRANSFER) == isTransfer,
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
        val allText = rows.map { (label, value) -> "${stringResource(label)}: $value" }.joinToString("\n")
        var copied by remember(method) { mutableStateOf(false) }
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
            Text(stringResource(if (copied) Res.string.pay_to_copied_all else Res.string.pay_to_copy_all))
        }

        if (!isTransfer) {
            Spacer(modifier = Modifier.height(16.dp))
            // ponytail: placeholder until the real QR image is added to composeResources/drawable.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(180.dp)
                    .background(AptoSurfaceContainerHigh, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = stringResource(Res.string.pay_to_qr_desc),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(150.dp)
                )
            }
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
