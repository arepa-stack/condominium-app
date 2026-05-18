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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.condominio.data.model.PaymentMethod
import com.example.condominio.data.utils.rememberImagePickerLauncher
import com.example.condominio.ui.utils.formatCurrency
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

// --- Custom Colors based on HTML ---
private val SurfaceColor = Color(0xFFF7F9FB)
private val OnSurfaceColor = Color(0xFF191C1E)
private val SurfaceContainerLowest = Color(0xFFFFFFFF)
private val SurfaceContainerHigh = Color(0xFFE6E8EA)
private val OutlineVariantColor = Color(0xFFC3C7CA)
private val OutlineColor = Color(0xFF73787A)
private val SecondaryColor = Color(0xFF9F4200)
private val SecondaryContainerColor = Color(0xFFFD6C00)
private val OnSecondaryColor = Color(0xFFFFFFFF)
private val StatusErrorColor = Color(0xFFEF4444)
private val StatusSuccessColor = Color(0xFF10B981)
private val OnSurfaceVariantColor = Color(0xFF43474A)

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
            TopAppBar(
                title = {
                    Text(
                        text = if (currentStep == 1) "Pagar Ahora" else "Detalles del Pago",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        color = OnSurfaceColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep == 2) {
                            currentStep = 1
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = OnSurfaceColor
                        )
                    }
                },
                actions = {
                    if (currentStep == 1) {
                        IconButton(onClick = { /* TODO: Help */ }) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "Ayuda",
                                tint = OutlineColor
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceColor
                )
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
        containerColor = SurfaceColor
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
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurfaceColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Elige los recibos pendientes que deseas liquidar hoy.",
            fontSize = 14.sp,
            color = OutlineColor,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (uiState.isLoadingInvoices) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SecondaryColor)
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
                fontSize = 16.sp,
                color = OutlineColor,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Info Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceContainerHigh, RoundedCornerShape(12.dp))
                .border(1.dp, OutlineVariantColor, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = SecondaryColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Al realizar el pago se generará un comprobante digital inmediato disponible en tu sección de Historial.",
                fontSize = 14.sp,
                color = OnSurfaceVariantColor,
                lineHeight = 20.sp
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
    val borderColor = if (isSelected) SecondaryColor else OutlineVariantColor
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Custom Checkbox
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(if (isSelected) SecondaryColor else Color.Transparent, RoundedCornerShape(6.dp))
                .border(
                    width = if (isSelected) 0.dp else 2.dp,
                    color = if (isSelected) Color.Transparent else OutlineColor,
                    shape = RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
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
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = SecondaryColor,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = invoice.description ?: "Mantenimiento Mensual",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceColor
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$${formatCurrency(invoice.remaining)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceColor
            )
            Text(
                text = "Pendiente",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = StatusErrorColor
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
        color = SurfaceContainerLowest,
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
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OutlineColor,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "$$amountToDisplay",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceColor
                    )
                }
                Text(
                    text = "$selectedCount recibo${if (selectedCount == 1) "" else "s"} seleccionado${if (selectedCount == 1) "" else "s"}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SecondaryColor
                )
            }

            Button(
                onClick = onContinue,
                enabled = isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecondaryContainerColor,
                    disabledContainerColor = OutlineVariantColor
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text(
                        text = "Continuar al Pago",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
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
                .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                .border(1.dp, OutlineVariantColor, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(SecondaryColor.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = SecondaryColor
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Total a Registrar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OutlineColor
                    )
                    Text(
                        text = "$${uiState.amount}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryColor
                    )
                }
            }
            Box(
                modifier = Modifier
                    .background(SecondaryColor.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Paso 2 de 2",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SecondaryColor
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Method Selector
        Text(
            text = "Método de Pago",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = OutlineColor,
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
                    unfocusedContainerColor = SurfaceContainerLowest,
                    focusedContainerColor = SurfaceContainerLowest,
                    unfocusedBorderColor = OutlineVariantColor,
                    focusedBorderColor = SecondaryColor
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
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
                    fontSize = 14.sp,
                    color = OnSurfaceVariantColor,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Upload Area
        Text(
            text = "Comprobante de Pago",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = OutlineColor,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                // Note: Dashed borders require custom drawing in Compose, using solid for simplicity or basic border
                .border(2.dp, if (isImageSelected) StatusSuccessColor else OutlineVariantColor, RoundedCornerShape(12.dp))
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
                            if (isImageSelected) StatusSuccessColor.copy(alpha = 0.2f) else SurfaceContainerHigh, 
                            RoundedCornerShape(32.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isImageSelected) Icons.Default.CheckCircle else Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = if (isImageSelected) StatusSuccessColor else OutlineColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isImageSelected) "Archivo Cargado" else "Subir Comprobante",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurfaceColor
                )
                Text(
                    text = if (isImageSelected) "Toca para cambiar archivo" else "JPG, PNG o PDF (Máx. 5MB)",
                    fontSize = 14.sp,
                    color = OutlineColor,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        if (uiState.error != null) {
            Text(
                text = uiState.error!!.asString(),
                color = StatusErrorColor,
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
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = OutlineColor,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = OutlineVariantColor) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = SurfaceContainerLowest,
                focusedContainerColor = SurfaceContainerLowest,
                unfocusedBorderColor = OutlineVariantColor,
                focusedBorderColor = SecondaryColor
            ),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
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
            Button(
                onClick = onSubmit,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecondaryContainerColor,
                    disabledContainerColor = OutlineVariantColor
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = "Registrar Pago",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    text = "CANCELAR OPERACIÓN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SecondaryColor,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Info Notice
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceContainerHigh.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = OutlineColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Tu pago será validado por la administración en un lapso de 24 a 48 horas hábiles.",
                    fontSize = 14.sp,
                    color = OnSurfaceVariantColor,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
