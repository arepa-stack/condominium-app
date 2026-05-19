package com.example.condominio.ui.screens.payment

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
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.condominio.data.model.PaymentMethod
import com.example.condominio.data.utils.rememberImagePickerLauncher
import com.example.condominio.ui.components.ListItemCard
import com.example.condominio.ui.components.PrimaryButton
import com.example.condominio.ui.components.TopBarWithBack
import com.example.condominio.ui.theme.AptoSuccess
import com.example.condominio.ui.theme.AptoSurfaceContainerHigh
import com.example.condominio.ui.utils.formatCurrency
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
                },
                actions = {
                    if (currentStep == 1) {
                        IconButton(onClick = { /* TODO: Help */ }) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "Ayuda",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
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
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
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
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    ListItemCard(
        onClick = onClick,
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
            // Month parser logic (basic)
            val parts = invoice.period.split("-")
            val monthNum = if (parts.size > 1) parts[1].toIntOrNull() ?: 1 else 1
            val months = listOf("", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
            val monthName = if (monthNum in 1..12) "${months[monthNum]} ${parts[0]}" else invoice.period

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
    var expanded by remember { mutableStateOf(false) }

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
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = when(uiState.method) {
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
                PaymentMethod.entries.forEach { method ->
                    DropdownMenuItem(
                        text = {
                            Text(when (method) {
                                PaymentMethod.PAGO_MOVIL -> "Pago Móvil"
                                PaymentMethod.TRANSFER -> "Transferencia Bancaria"
                                PaymentMethod.CASH -> "Efectivo / Zelle"
                            })
                        },
                        onClick = {
                            viewModel.onMethodChange(method)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic Form Fields
        when (uiState.method) {
            PaymentMethod.PAGO_MOVIL -> {
                CustomTextField(
                    label = "Banco Emisor",
                    value = uiState.bank,
                    onValueChange = viewModel::onBankChange,
                    placeholder = "Seleccione el banco"
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CustomTextField(
                        label = "Teléfono",
                        value = uiState.phone,
                        onValueChange = viewModel::onPhoneChange,
                        placeholder = "0414-0000000",
                        keyboardType = KeyboardType.Phone,
                        modifier = Modifier.weight(1f)
                    )
                    CustomTextField(
                        label = "Referencia",
                        value = uiState.reference,
                        onValueChange = viewModel::onReferenceChange,
                        placeholder = "Últimos 6 dígitos",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            PaymentMethod.TRANSFER -> {
                CustomTextField(
                    label = "Banco Emisor",
                    value = uiState.bank,
                    onValueChange = viewModel::onBankChange,
                    placeholder = "Seleccione el banco"
                )
                Spacer(modifier = Modifier.height(16.dp))
                CustomTextField(
                    label = "Referencia",
                    value = uiState.reference,
                    onValueChange = viewModel::onReferenceChange,
                    placeholder = "Últimos dígitos de confirmación",
                    keyboardType = KeyboardType.Number
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

        Box(
            modifier = Modifier
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
private fun CustomTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.outlineVariant) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            ),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge
        )
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

            // Info Notice
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
    }
}
