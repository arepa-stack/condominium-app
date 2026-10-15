package com.example.condominio.ui.screens.payment

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.condominio.data.model.PaymentMethod
import com.example.condominio.data.utils.rememberImagePickerLauncher
import com.example.condominio.data.model.Invoice
import com.example.condominio.ui.components.AdaptiveColumns
import com.example.condominio.ui.components.LabeledOutlinedField
import com.example.condominio.ui.components.ListItemCard
import com.example.condominio.ui.components.LoadingState
import com.example.condominio.ui.components.PrimaryButton
import com.example.condominio.ui.components.TopBarWithBack
import com.example.condominio.ui.components.WebCard
import com.example.condominio.ui.components.isExpanded
import com.example.condominio.ui.components.isWide
import com.example.condominio.ui.components.pagePadding
import com.example.condominio.ui.components.screenBackground
import com.example.condominio.ui.theme.AptoSuccess
import com.example.condominio.ui.theme.AptoSurfaceContainerHigh
import com.example.condominio.ui.utils.formatCurrency
import com.example.condominio.ui.utils.formatDate
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePaymentScreen(
    onBackClick: () -> Unit,
    onSubmitSuccess: () -> Unit,
    viewModel: CreatePaymentViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isImageSelected = uiState.proofUrl != null

    var currentStep by remember { mutableIntStateOf(1) }

    val imagePickerLauncher = rememberImagePickerLauncher { uri ->
        if (uri != null) {
            viewModel.onProofUrlChange(uri)
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onSubmitSuccess()
        }
    }

    if (isWide()) {
        // Web / tablet: single page, form on the left, invoices + summary + submit on the right.
        Scaffold(
            topBar = {
                TopBarWithBack(title = "Pagar Ahora", onBackClick = onBackClick)
            },
            containerColor = screenBackground(MaterialTheme.colorScheme.background)
        ) { paddingValues ->
            WidePaymentForm(
                uiState = uiState,
                isImageSelected = isImageSelected,
                viewModel = viewModel,
                onUploadClick = { imagePickerLauncher() },
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            )
        }
        return
    }

    Scaffold(
        topBar = {
            TopBarWithBack(
                title = if (currentStep == 1) "Pagar Ahora" else "Detalles del Pago",
                onBackClick = {
                    if (currentStep == 2) {
                        currentStep = 1
                    } else {
                        onBackClick()
                    }
                }
            )
        },
        bottomBar = {
            if (currentStep == 1) {
                Step1BottomBar(
                    totalAmount = uiState.amount,
                    selectedCount = uiState.selectedInvoiceIds.size,
                    onContinue = { currentStep = 2 }
                )
            } else {
                Step2BottomBar(
                    isLoading = uiState.isLoading,
                    onSubmit = viewModel::onSubmitClick,
                    onCancel = { currentStep = 1 }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally(animationSpec = tween(300)) { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally(animationSpec = tween(300)) { width -> -width } + fadeOut()
                } else {
                    slideInHorizontally(animationSpec = tween(300)) { width -> -width } + fadeIn() togetherWith
                            slideOutHorizontally(animationSpec = tween(300)) { width -> width } + fadeOut()
                }
            },
            label = "StepTransition",
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { step ->
            if (step == 1) {
                Step1Invoices(
                    uiState = uiState,
                    onToggleInvoice = viewModel::toggleInvoiceSelection
                )
            } else {
                Step2Details(
                    uiState = uiState,
                    isImageSelected = isImageSelected,
                    viewModel = viewModel,
                    onUploadClick = { imagePickerLauncher() }
                )
            }
        }
    }
}

@Composable
private fun Step1Invoices(
    uiState: CreatePaymentUiState,
    onToggleInvoice: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Text(
            text = "Seleccionar Recibos",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Elige los recibos pendientes que deseas liquidar hoy.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (uiState.isLoadingInvoices) {
            LoadingState(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                fullScreen = false
            )
        } else if (uiState.pendingInvoices.isNotEmpty()) {
            val invoices = uiState.pendingInvoices.sortedBy { it.period }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                invoices.forEach { invoice ->
                    val isSelected = uiState.selectedInvoiceIds.contains(invoice.id)
                    InvoiceCard(
                        invoice = invoice,
                        isSelected = isSelected,
                        onClick = { onToggleInvoice(invoice.id) }
                    )
                }
            }
        } else {
            Text(
                text = "No tienes recibos pendientes.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Info Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AptoSurfaceContainerHigh, RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Al realizar el pago se generará un comprobante digital inmediato disponible en tu sección de Historial.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Add extra spacer to avoid hiding behind bottom bar
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun InvoiceCard(
    invoice: com.example.condominio.data.model.Invoice,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    ListItemCard(
        onClick = onClick,
        modifier = modifier,
        borderColor = borderColor
    ) {
        // Custom Checkbox
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(6.dp))
                .border(
                    width = if (isSelected) 0.dp else 2.dp,
                    color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            val monthName = invoiceMonthLabel(invoice.period)

            Text(
                text = monthName.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = invoice.description ?: "Mantenimiento Mensual",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$${formatCurrency(invoice.remaining)}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Pendiente",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun Step1BottomBar(
    totalAmount: String,
    selectedCount: Int,
    onContinue: () -> Unit
) {
    val amountToDisplay = if (totalAmount.isBlank() || totalAmount == "0" || totalAmount == "0.0") "0.00" else totalAmount
    val isEnabled = selectedCount > 0

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TOTAL A PAGAR",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "$$amountToDisplay",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "$selectedCount recibo${if (selectedCount == 1) "" else "s"} seleccionado${if (selectedCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            PrimaryButton(
                text = "Continuar al Pago",
                onClick = onContinue,
                enabled = isEnabled,
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Step2Details(
    uiState: CreatePaymentUiState,
    isImageSelected: Boolean,
    viewModel: CreatePaymentViewModel,
    onUploadClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        // Summary Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Total a Registrar",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "$${uiState.amount}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Paso 2 de 2",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Method Selector
        Text(
            text = "Método de Pago",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        PaymentMethodDropdown(
            method = uiState.method,
            onMethodChange = viewModel::onMethodChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic Form Fields
        when (uiState.method) {
            PaymentMethod.PAGO_MOVIL -> {
                LabeledOutlinedField(
                    value = uiState.bank,
                    onValueChange = viewModel::onBankChange,
                    label = "Banco Emisor",
                    placeholder = "Seleccione el banco"
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LabeledOutlinedField(
                        value = uiState.phone,
                        onValueChange = viewModel::onPhoneChange,
                        label = "Teléfono",
                        placeholder = "0414-0000000",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f)
                    )
                    LabeledOutlinedField(
                        value = uiState.reference,
                        onValueChange = viewModel::onReferenceChange,
                        label = "Referencia",
                        placeholder = "Últimos 6 dígitos",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            PaymentMethod.TRANSFER -> {
                LabeledOutlinedField(
                    value = uiState.bank,
                    onValueChange = viewModel::onBankChange,
                    label = "Banco Emisor",
                    placeholder = "Seleccione el banco"
                )
                Spacer(modifier = Modifier.height(16.dp))
                LabeledOutlinedField(
                    value = uiState.reference,
                    onValueChange = viewModel::onReferenceChange,
                    label = "Referencia",
                    placeholder = "Últimos dígitos de confirmación",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            PaymentMethod.CASH -> {
                Text(
                    text = "Para pagos en efectivo o Zelle, acérquese a la administración o adjunte captura del Zelle.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Upload Area
        Text(
            text = "Comprobante de Pago",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        ProofUploadBox(
            isImageSelected = isImageSelected,
            onUploadClick = onUploadClick
        )

        if (uiState.error != null) {
            Text(
                text = uiState.error!!.asString(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(100.dp)) // padding for bottom bar
    }
}

@Composable
private fun Step2BottomBar(
    isLoading: Boolean,
    onSubmit: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            PrimaryButton(
                text = "Registrar Pago",
                onClick = onSubmit,
                enabled = !isLoading,
                isLoading = isLoading
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    text = "CANCELAR OPERACIÓN",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            ValidationNotice()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentMethodDropdown(
    method: PaymentMethod,
    onMethodChange: (PaymentMethod) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = when(method) {
                PaymentMethod.PAGO_MOVIL -> "Pago Móvil"
                PaymentMethod.TRANSFER -> "Transferencia Bancaria"
                PaymentMethod.CASH -> "Efectivo / Zelle"
            },
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            ),
            textStyle = MaterialTheme.typography.bodyLarge
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            PaymentMethod.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(when (option) {
                            PaymentMethod.PAGO_MOVIL -> "Pago Móvil"
                            PaymentMethod.TRANSFER -> "Transferencia Bancaria"
                            PaymentMethod.CASH -> "Efectivo / Zelle"
                        })
                    },
                    onClick = {
                        onMethodChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ProofUploadBox(
    isImageSelected: Boolean,
    onUploadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            // Note: Dashed borders require custom drawing in Compose, using solid for simplicity or basic border
            .border(2.dp, if (isImageSelected) AptoSuccess else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onUploadClick)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        if (isImageSelected) AptoSuccess.copy(alpha = 0.2f) else AptoSurfaceContainerHigh,
                        RoundedCornerShape(32.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isImageSelected) Icons.Default.CheckCircle else Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = if (isImageSelected) AptoSuccess else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isImageSelected) "Archivo Cargado" else "Subir Comprobante",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isImageSelected) "Toca para cambiar archivo" else "JPG, PNG o PDF (Máx. 5MB)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ValidationNotice() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AptoSurfaceContainerHigh.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Tu pago será validado por la administración en un lapso de 24 a 48 horas hábiles.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** "2025-03" -> "Marzo 2025" (falls back to the raw period). */
private fun invoiceMonthLabel(period: String): String {
    val parts = period.split("-")
    val monthNum = if (parts.size > 1) parts[1].toIntOrNull() ?: 1 else 1
    val months = listOf("", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
    return if (monthNum in 1..12) "${months[monthNum]} ${parts[0]}" else period
}

/** Preview of how [amount] is spread over the selected invoices (oldest first), same rule the ViewModel applies on submit. */
private fun previewAllocations(amount: Double, selected: List<Invoice>): List<Pair<Invoice, Double>> {
    var remaining = amount
    return selected.sortedBy { it.period }.map { invoice ->
        val toPay = if (remaining > 0) minOf(remaining, invoice.remaining) else 0.0
        remaining -= toPay
        invoice to toPay
    }
}

// ---------------------------------------------------------------------------------------------
// Wide (web / tablet) layout. Same state and handlers as the mobile steps, laid out on one page.
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidePaymentForm(
    uiState: CreatePaymentUiState,
    isImageSelected: Boolean,
    viewModel: CreatePaymentViewModel,
    onUploadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            // Full container width; left edge = pagePadding(), same as the page header. Never centered.
            .fillMaxWidth()
            .padding(start = pagePadding(), end = pagePadding(), top = 8.dp, bottom = 32.dp)
    ) {
        AdaptiveColumns(
            main = {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    // Method + method-specific bank data
                    WebCard {
                        WideSectionTitle(stringResource(Res.string.payment_method))
                        PaymentMethodDropdown(
                            method = uiState.method,
                            onMethodChange = viewModel::onMethodChange
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        WideMethodFields(uiState = uiState, viewModel = viewModel)
                    }

                    // Amount + date
                    WebCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            LabeledOutlinedField(
                                value = uiState.amount,
                                onValueChange = viewModel::onAmountChange,
                                label = stringResource(Res.string.payment_amount),
                                placeholder = "0.00",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            LabeledOutlinedField(
                                value = formatDate(uiState.date, "dd/MM/yyyy"),
                                onValueChange = {},
                                label = stringResource(Res.string.payment_date),
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(
                                        onClick = { showDatePicker = true },
                                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = stringResource(Res.string.select_date),
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Notes
                    WebCard {
                        LabeledOutlinedField(
                            value = uiState.description,
                            onValueChange = viewModel::onDescriptionChange,
                            label = stringResource(Res.string.payment_description_label),
                            placeholder = stringResource(Res.string.payment_description_placeholder),
                            singleLine = false
                        )
                    }

                    // Proof upload (wide drop zone)
                    WebCard {
                        WideSectionTitle(stringResource(Res.string.payment_proof))
                        ProofUploadBox(
                            isImageSelected = isImageSelected,
                            onUploadClick = onUploadClick,
                            modifier = Modifier
                                .heightIn(min = 220.dp)
                                .pointerHoverIcon(PointerIcon.Hand)
                        )
                    }
                }
            },
            side = {
                // AdaptiveColumns adds no gap when stacked (Medium): add it here.
                if (!isExpanded()) Spacer(modifier = Modifier.height(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    WideInvoicesCard(
                        uiState = uiState,
                        onToggleInvoice = viewModel::toggleInvoiceSelection
                    )
                    WideSummaryCard(
                        uiState = uiState,
                        onSubmit = viewModel::onSubmitClick
                    )
                }
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = localMillisToPickerUtc(uiState.date))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onDateChange(pickerUtcToLocalMillis(it)) }
                    showDatePicker = false
                }) {
                    Text(stringResource(Res.string.accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun WideSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 16.dp)
    )
}

@Composable
private fun WideMethodFields(
    uiState: CreatePaymentUiState,
    viewModel: CreatePaymentViewModel
) {
    when (uiState.method) {
        PaymentMethod.PAGO_MOVIL -> {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LabeledOutlinedField(
                    value = uiState.bank,
                    onValueChange = viewModel::onBankChange,
                    label = "Banco Emisor",
                    placeholder = "Seleccione el banco",
                    modifier = Modifier.weight(1f)
                )
                LabeledOutlinedField(
                    value = uiState.phone,
                    onValueChange = viewModel::onPhoneChange,
                    label = "Teléfono",
                    placeholder = "0414-0000000",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LabeledOutlinedField(
                    value = uiState.reference,
                    onValueChange = viewModel::onReferenceChange,
                    label = "Referencia",
                    placeholder = "Últimos 6 dígitos",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        PaymentMethod.TRANSFER -> {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LabeledOutlinedField(
                    value = uiState.bank,
                    onValueChange = viewModel::onBankChange,
                    label = "Banco Emisor",
                    placeholder = "Seleccione el banco",
                    modifier = Modifier.weight(1f)
                )
                LabeledOutlinedField(
                    value = uiState.reference,
                    onValueChange = viewModel::onReferenceChange,
                    label = "Referencia",
                    placeholder = "Últimos dígitos de confirmación",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        PaymentMethod.CASH -> {
            Text(
                text = "Para pagos en efectivo o Zelle, acérquese a la administración o adjunte captura del Zelle.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WideInvoicesCard(
    uiState: CreatePaymentUiState,
    onToggleInvoice: (String) -> Unit
) {
    WebCard {
        WideSectionTitle(stringResource(Res.string.select_invoices_label))
        when {
            uiState.isLoadingInvoices -> LoadingState(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                fullScreen = false
            )
            uiState.pendingInvoices.isNotEmpty() -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                uiState.pendingInvoices.sortedBy { it.period }.forEach { invoice ->
                    InvoiceCard(
                        invoice = invoice,
                        isSelected = uiState.selectedInvoiceIds.contains(invoice.id),
                        onClick = { onToggleInvoice(invoice.id) },
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                    )
                }
            }
            else -> Text(
                text = "No tienes recibos pendientes.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun WideSummaryCard(
    uiState: CreatePaymentUiState,
    onSubmit: () -> Unit
) {
    val amount = uiState.amount
    val amountToDisplay = if (amount.isBlank() || amount == "0" || amount == "0.0") "0.00" else amount
    val selectedCount = uiState.selectedInvoiceIds.size
    val allocations = previewAllocations(
        amount = amount.toDoubleOrNull() ?: 0.0,
        selected = uiState.pendingInvoices.filter { uiState.selectedInvoiceIds.contains(it.id) }
    )

    WebCard {
        Text(
            text = "$selectedCount recibo${if (selectedCount == 1) "" else "s"} seleccionado${if (selectedCount == 1) "" else "s"}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        allocations.forEach { (invoice, allocated) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = invoiceMonthLabel(invoice.period),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = invoice.description ?: "Mantenimiento Mensual",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Text(
                    text = "$${formatCurrency(allocated)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TOTAL A PAGAR",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = "$$amountToDisplay",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        uiState.error?.let { error ->
            Text(
                text = error.asString(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        PrimaryButton(
            text = "Registrar Pago",
            onClick = onSubmit,
            enabled = !uiState.isLoading,
            isLoading = uiState.isLoading,
            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
        )

        Spacer(modifier = Modifier.height(16.dp))

        ValidationNotice()
    }
}

// Material3 DatePicker works with UTC-midnight millis, while the app keeps dates in local time.
// Without converting, west-of-UTC users (e.g. UTC-4) get the previous day when picking a date.
private fun localMillisToPickerUtc(millis: Long): Long =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault()).date
        .atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

private fun pickerUtcToLocalMillis(millis: Long): Long =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
        .atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
