package com.example.condominio.ui.screens.payment

import com.example.condominio.ui.utils.formatDate
import com.example.condominio.ui.utils.formatCurrency
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import condominio.shared.generated.resources.*
import com.example.condominio.data.model.PaymentMethod
import com.example.condominio.data.utils.rememberImagePickerLauncher
import androidx.compose.ui.text.input.KeyboardType
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TextButton
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreatePaymentScreen(
    onBackClick: () -> Unit,
    onSubmitSuccess: () -> Unit,
    viewModel: CreatePaymentViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isImageSelected = uiState.proofUrl != null

    val imagePickerLauncher = rememberImagePickerLauncher { uri ->
        if (uri != null) {
            viewModel.onProofUrlChange(uri)
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val notFutureSelectableDates = remember {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis <= Clock.System.now().toEpochMilliseconds()

            override fun isSelectableYear(year: Int): Boolean =
                year <= Clock.System.todayIn(TimeZone.UTC).year
        }
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = uiState.date,
        selectableDates = notFutureSelectableDates
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { 
                    datePickerState.selectedDateMillis?.let { viewModel.onDateChange(it) }
                    showDatePicker = false 
                }) { Text(stringResource(Res.string.accept)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(Res.string.cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onSubmitSuccess()
        }
    }

    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.create_payment_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = stringResource(Res.string.payment_amount_label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            OutlinedTextField(
                value = uiState.amount,
                onValueChange = viewModel::onAmountChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                textStyle = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                prefix = { Text(stringResource(Res.string.currency_symbol), style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                )
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            
            Spacer(modifier = Modifier.height(24.dp))

            // Payment Method
            Text(
                text = stringResource(Res.string.payment_method_field),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = when(uiState.method) {
                        PaymentMethod.PAGO_MOVIL -> stringResource(Res.string.method_pago_movil)
                        PaymentMethod.TRANSFER -> stringResource(Res.string.method_transfer)
                        PaymentMethod.CASH -> stringResource(Res.string.method_cash)
                    },
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    PaymentMethod.entries.forEach { method ->
                        DropdownMenuItem(
                            text = {
                                Text(when (method) {
                                    PaymentMethod.PAGO_MOVIL -> stringResource(Res.string.method_pago_movil)
                                    PaymentMethod.TRANSFER -> stringResource(Res.string.method_transfer)
                                    PaymentMethod.CASH -> stringResource(Res.string.method_cash)
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

            // Conditional Fields based on method
            when (uiState.method) {
                PaymentMethod.PAGO_MOVIL -> {
                    OutlinedTextField(
                        value = uiState.bank,
                        onValueChange = viewModel::onBankChange,
                        label = { Text(stringResource(Res.string.issuing_bank_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = uiState.phone,
                        onValueChange = viewModel::onPhoneChange,
                        label = { Text(stringResource(Res.string.phone_number_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = uiState.reference,
                        onValueChange = viewModel::onReferenceChange,
                        label = { Text(stringResource(Res.string.reference_number_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                PaymentMethod.TRANSFER -> {
                    OutlinedTextField(
                        value = uiState.bank,
                        onValueChange = viewModel::onBankChange,
                        label = { Text(stringResource(Res.string.issuing_bank_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = uiState.reference,
                        onValueChange = viewModel::onReferenceChange,
                        label = { Text(stringResource(Res.string.reference_number_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                PaymentMethod.CASH -> {
                     Text(
                        text = stringResource(Res.string.cash_payment_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 8.dp)
                     )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Invoice Selector
            Text(
                text = stringResource(Res.string.select_invoices_label),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (uiState.isLoadingInvoices) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.pendingInvoices.isNotEmpty()) {
                val invoices = uiState.pendingInvoices.sortedBy { it.period }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    invoices.forEach { invoice ->
                        val isSelected = uiState.selectedInvoiceIds.contains(invoice.id)
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleInvoiceSelection(invoice.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { viewModel.toggleInvoiceSelection(invoice.id) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    val parts = invoice.period.split("-")
                                    val monthNum = if (parts.size > 1) parts[1].toIntOrNull() ?: 1 else 1
                                    val months = listOf(
                                        "", 
                                        stringResource(Res.string.month_january),
                                        stringResource(Res.string.month_february),
                                        stringResource(Res.string.month_march),
                                        stringResource(Res.string.month_april),
                                        stringResource(Res.string.month_may),
                                        stringResource(Res.string.month_june),
                                        stringResource(Res.string.month_july),
                                        stringResource(Res.string.month_august),
                                        stringResource(Res.string.month_september),
                                        stringResource(Res.string.month_october),
                                        stringResource(Res.string.month_november),
                                        stringResource(Res.string.month_december)
                                    )
                                    val monthName = if (monthNum in 1..12) months[monthNum] else invoice.period
                                    Text(
                                        text = invoice.description ?: monthName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = stringResource(Res.string.remaining_label) + ": $${formatCurrency(invoice.remaining)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = invoice.period,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                
                if (uiState.selectedInvoiceIds.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.invoices_selected_count, uiState.selectedInvoiceIds.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                 Text(
                    text = stringResource(Res.string.no_pending_invoices),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = stringResource(Res.string.payment_description_label),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.payment_description_placeholder)) },
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(Res.string.payment_date_label),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = formatDate(uiState.date, "dd/MM/yyyy"),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = stringResource(Res.string.select_date))
                    }
                },
                shape = RoundedCornerShape(12.dp),
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = stringResource(Res.string.proof_of_payment_required),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.error
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        if (isImageSelected) MaterialTheme.colorScheme.primaryContainer else Color.Gray.copy(alpha = 0.05f),
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        if (!isImageSelected) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { imagePickerLauncher() },
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (isImageSelected) Icons.Default.CheckCircle else Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isImageSelected) stringResource(Res.string.image_selected) else stringResource(Res.string.tap_to_upload),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (uiState.error != null) {
                Text(
                    text = uiState.error!!.asString(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Button(
                onClick = viewModel::onSubmitClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    Text(stringResource(Res.string.create_payment_title), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

