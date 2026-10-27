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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.example.condominio.data.model.PaymentMethod
import com.example.condominio.data.utils.PlatformFileReader
import com.example.condominio.data.utils.rememberDocumentPickerLauncher
import com.example.condominio.data.model.Invoice
import com.example.condominio.ui.components.AdaptiveColumns
import com.example.condominio.ui.components.ErrorState
import com.example.condominio.ui.components.LabeledOutlinedField
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
import com.example.condominio.ui.utils.UiText
import com.example.condominio.ui.utils.formatCurrency
import com.example.condominio.ui.utils.formatDate
import com.example.condominio.ui.utils.isWebPlatform
import com.example.condominio.ui.utils.parseAmount
import condominio.shared.generated.resources.*
import kotlin.time.Clock
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.coroutines.launch

/** Same limit as the decisions quote upload and the "máx. 5 MB" hint. */
private const val MAX_PROOF_BYTES = 5 * 1024 * 1024

/** Text fields that can receive focus when validation fails (E-P1). */
private class PaymentFieldFocus {
    val bank = FocusRequester()
    val reference = FocusRequester()
    val amount = FocusRequester()
}

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

    // Images or PDF, as the upload hint promises; Android used to offer images only (E-P3).
    // Same checks as the decisions quote upload: readable and at most 5 MB.
    val fileReader = koinInject<PlatformFileReader>()
    val imagePickerLauncher = rememberDocumentPickerLauncher { uri, _ ->
        if (uri == null) return@rememberDocumentPickerLauncher
        val bytes = fileReader.readBytes(uri)
        when {
            bytes == null -> viewModel.onProofRejected(UiText.StringResource(Res.string.decisions_upload_file_read_failed))
            bytes.size > MAX_PROOF_BYTES -> viewModel.onProofRejected(UiText.StringResource(Res.string.decisions_upload_file_too_large))
            else -> viewModel.onProofUrlChange(uri)
        }
    }

    val wide = isWide()
    val fieldFocus = remember { PaymentFieldFocus() }
    // Page 0: account to pay to; page 1: the payment form. Both stay composed so focus requests reach the form.
    val pagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()
    val onSubmit: () -> Unit = {
        viewModel.onSubmitClick()
        // Validation is synchronous: move focus to the first invalid field, in on-screen order (E-P1).
        // The amount field only exists on the wide layout; on the phone it comes from the invoices.
        val state = viewModel.uiState.value
        val target = when {
            state.bankError != null -> fieldFocus.bank
            state.referenceError != null -> fieldFocus.reference
            state.amountError != null && wide -> fieldFocus.amount
            else -> null
        }
        if (target != null || state.amountError != null || state.proofError != null) {
            // Errors live on the form page: show it first, then focus once the pager has settled.
            scope.launch {
                pagerState.animateScrollToPage(1)
                target?.let { runCatching { it.requestFocus() } }
            }
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onSubmitSuccess()
        }
    }

    if (wide) {
        // Web / tablet: single page, form on the left, invoices + summary + submit on the right.
        Scaffold(
            topBar = {
                TopBarWithBack(title = stringResource(Res.string.pay_now), onBackClick = onBackClick)
            },
            containerColor = screenBackground(MaterialTheme.colorScheme.background)
        ) { paddingValues ->
            WidePaymentForm(
                uiState = uiState,
                isImageSelected = isImageSelected,
                viewModel = viewModel,
                fieldFocus = fieldFocus,
                pagerState = pagerState,
                onSubmit = onSubmit,
                onUploadClick = { imagePickerLauncher() },
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            )
        }
        return
    }

    Scaffold(
        topBar = {
            TopBarWithBack(
                title = stringResource(if (currentStep == 1) Res.string.pay_now else Res.string.payment_details_title),
                onBackClick = {
                    if (currentStep == 2 && pagerState.currentPage == 1) {
                        scope.launch { pagerState.animateScrollToPage(0) }
                    } else if (currentStep == 2) {
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
            } else if (pagerState.currentPage == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    PrimaryButton(
                        text = stringResource(Res.string.pay_to_done),
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        trailingIcon = Icons.AutoMirrored.Filled.ArrowForward
                    )
                }
            } else {
                Step2BottomBar(
                    isLoading = uiState.isLoading,
                    onSubmit = onSubmit,
                    // Cancels the whole operation; going back to step 1 is the top bar's back arrow (E-P5)
                    onCancel = onBackClick
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
                    onToggleInvoice = viewModel::toggleInvoiceSelection,
                    onRetry = viewModel::loadPendingInvoices
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    PaymentPagerTabs(pagerState)
                    HorizontalPager(
                        state = pagerState,
                        beyondViewportPageCount = 1,
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.weight(1f)
                    ) { page ->
                        if (page == 0) {
                            PayToAccountContent(
                                uiState = uiState,
                                onMethodChange = viewModel::onMethodChange,
                                onRetryRate = viewModel::loadBcvRate,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 24.dp)
                            )
                        } else {
                            Step2Details(
                                uiState = uiState,
                                isImageSelected = isImageSelected,
                                viewModel = viewModel,
                                fieldFocus = fieldFocus,
                                onUploadClick = { imagePickerLauncher() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Step1Invoices(
    uiState: CreatePaymentUiState,
    onToggleInvoice: (String) -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Text(
            text = stringResource(Res.string.select_invoices_label),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp).semantics { heading() }
        )
        Text(
            text = stringResource(Res.string.create_payment_select_invoices_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (uiState.isLoadingInvoices) {
            LoadingState(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                fullScreen = false
            )
        } else if (uiState.invoicesError != null) {
            // Load failure with retry, not "no pending invoices" (A-07)
            ErrorState(
                title = uiState.invoicesError.asString(),
                onRetry = onRetry,
                modifier = Modifier.heightIn(max = 320.dp)
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
                text = stringResource(Res.string.no_pending_invoices),
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
                text = stringResource(Res.string.create_payment_history_hint),
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

    // Exposed as a checkbox (role + checked state) instead of a plain button (E-P2)
    Surface(
        checked = isSelected,
        onCheckedChange = { onClick() },
        modifier = modifier.fillMaxWidth().semantics { role = Role.Checkbox },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
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
                    text = invoice.description ?: stringResource(Res.string.default_invoice_desc),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(Res.string.currency_amount, formatCurrency(invoice.remaining)),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = stringResource(Res.string.status_pending),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun Step1BottomBar(
    totalAmount: String,
    selectedCount: Int,
    onContinue: () -> Unit
) {
    val amountToDisplay = amountDisplay(totalAmount)
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
                        text = stringResource(Res.string.total_to_pay).uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = amountToDisplay,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = pluralStringResource(Res.plurals.invoices_selected_count, selectedCount, selectedCount),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            PrimaryButton(
                text = stringResource(Res.string.continue_to_payment),
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
    fieldFocus: PaymentFieldFocus,
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
                        text = stringResource(Res.string.total_to_register),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = amountDisplay(uiState.amount),
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
                    text = stringResource(Res.string.step_of, 2, 2),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Method Selector
        Text(
            text = stringResource(Res.string.payment_method),
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
                BankField(uiState, viewModel, fieldFocus)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PhoneField(uiState, viewModel, Modifier.weight(1f))
                    ReferenceField(
                        uiState, viewModel, fieldFocus,
                        placeholder = stringResource(Res.string.reference_placeholder_last6),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            PaymentMethod.TRANSFER -> {
                BankField(uiState, viewModel, fieldFocus)
                Spacer(modifier = Modifier.height(16.dp))
                ReferenceField(
                    uiState, viewModel, fieldFocus,
                    placeholder = stringResource(Res.string.reference_placeholder_confirmation)
                )
            }
            PaymentMethod.CASH -> {
                Text(
                    text = stringResource(Res.string.cash_zelle_payment_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Upload Area
        Text(
            text = stringResource(Res.string.payment_proof),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        ProofUploadBox(
            isImageSelected = isImageSelected,
            isError = uiState.proofError != null,
            onUploadClick = onUploadClick
        )
        uiState.proofError?.let { FieldErrorText(it) }

        // No amount field on the phone (it comes from the invoices): its error is shown here
        (uiState.amountError ?: uiState.error)?.let { error ->
            FormErrorText(error, Modifier.padding(top = 16.dp))
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
                text = stringResource(Res.string.register_payment),
                onClick = onSubmit,
                enabled = !isLoading,
                isLoading = isLoading
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onCancel,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    text = stringResource(Res.string.cancel_payment).uppercase(),
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
            value = methodOptionLabel(method),
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
                    text = { Text(methodOptionLabel(option)) },
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
private fun methodOptionLabel(method: PaymentMethod): String = stringResource(
    when (method) {
        PaymentMethod.PAGO_MOVIL -> Res.string.method_pago_movil
        PaymentMethod.TRANSFER -> Res.string.method_transfer_bank
        PaymentMethod.CASH -> Res.string.method_cash_zelle
    }
)

@Composable
private fun BankField(
    uiState: CreatePaymentUiState,
    viewModel: CreatePaymentViewModel,
    fieldFocus: PaymentFieldFocus,
    modifier: Modifier = Modifier
) {
    LabeledOutlinedField(
        value = uiState.bank,
        onValueChange = viewModel::onBankChange,
        label = stringResource(Res.string.bank_issuing),
        placeholder = stringResource(Res.string.bank_placeholder),
        isError = uiState.bankError != null,
        supportingText = uiState.bankError?.asString(),
        modifier = modifier.focusRequester(fieldFocus.bank)
    )
}

@Composable
private fun PhoneField(
    uiState: CreatePaymentUiState,
    viewModel: CreatePaymentViewModel,
    modifier: Modifier = Modifier
) {
    LabeledOutlinedField(
        value = uiState.phone,
        onValueChange = viewModel::onPhoneChange,
        label = stringResource(Res.string.phone_label),
        placeholder = "0414-0000000",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        contentType = ContentType.PhoneNumber,
        modifier = modifier
    )
}

@Composable
private fun ReferenceField(
    uiState: CreatePaymentUiState,
    viewModel: CreatePaymentViewModel,
    fieldFocus: PaymentFieldFocus,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    LabeledOutlinedField(
        value = uiState.reference,
        onValueChange = viewModel::onReferenceChange,
        label = stringResource(Res.string.reference_label),
        placeholder = placeholder,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = uiState.referenceError != null,
        supportingText = uiState.referenceError?.asString(),
        modifier = modifier.focusRequester(fieldFocus.reference)
    )
}

/** Inline error under a non-text field (the proof box). */
@Composable
private fun FieldErrorText(error: UiText) {
    Text(
        text = error.asString(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier
            .padding(start = 16.dp, top = 4.dp)
            .semantics { liveRegion = LiveRegionMode.Polite }
    )
}

/** Form-level error (submit failure, invoice selection), announced by screen readers. */
@Composable
private fun FormErrorText(error: UiText, modifier: Modifier = Modifier) {
    Text(
        text = error.asString(),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite }
    )
}

/** "$1500.00" for the typed or auto-filled amount; input that can't be parsed shows $0.00. */
@Composable
private fun amountDisplay(amount: String): String =
    stringResource(Res.string.currency_amount, formatCurrency(parseAmount(amount) ?: 0.0))

@Composable
private fun ProofUploadBox(
    isImageSelected: Boolean,
    onUploadClick: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        isImageSelected -> AptoSuccess
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            // Note: Dashed borders require custom drawing in Compose, using solid for simplicity or basic border
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onUploadClick)
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
                text = stringResource(if (isImageSelected) Res.string.proof_uploaded else Res.string.proof_upload),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(if (isImageSelected) Res.string.proof_change_hint else Res.string.proof_formats_hint),
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
            text = stringResource(Res.string.payment_validation_notice),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val monthNames = listOf(
    Res.string.month_january, Res.string.month_february, Res.string.month_march, Res.string.month_april,
    Res.string.month_may, Res.string.month_june, Res.string.month_july, Res.string.month_august,
    Res.string.month_september, Res.string.month_october, Res.string.month_november, Res.string.month_december
)

/** "2025-03" -> "Marzo 2025" / "March 2025" in the app language (falls back to the raw period). Also used by billing. */
@Composable
internal fun invoiceMonthLabel(period: String): String {
    val parts = period.split("-")
    // Not "YYYY-MM" (the backend doesn't enforce it): show it as is instead of "Enero <period>".
    val monthNum = if (parts.size == 2) parts[1].toIntOrNull() ?: 0 else 0
    return if (monthNum in 1..12) "${stringResource(monthNames[monthNum - 1])} ${parts[0]}" else period
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
    fieldFocus: PaymentFieldFocus,
    pagerState: PagerState,
    onSubmit: () -> Unit,
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
                PaymentPagerTabs(pagerState)
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    verticalAlignment = Alignment.Top,
                    pageSpacing = 24.dp
                ) { page ->
                    if (page == 0) {
                        WebCard {
                            PayToAccountContent(
                                uiState = uiState,
                                onMethodChange = viewModel::onMethodChange,
                                onRetryRate = viewModel::loadBcvRate
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                            // Method + method-specific bank data
                            WebCard {
                                WideSectionTitle(stringResource(Res.string.payment_method))
                                PaymentMethodDropdown(
                                    method = uiState.method,
                                    onMethodChange = viewModel::onMethodChange
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                WideMethodFields(uiState = uiState, viewModel = viewModel, fieldFocus = fieldFocus)
                            }

                            // Amount + date
                            WebCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Comma or dot decimals ("150,50", "1.500,25"): parsed with parseAmount (A-01)
                                    LabeledOutlinedField(
                                        value = uiState.amount,
                                        onValueChange = viewModel::onAmountChange,
                                        label = stringResource(Res.string.payment_amount),
                                        placeholder = "0.00",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        isError = uiState.amountError != null,
                                        supportingText = uiState.amountError?.asString(),
                                        modifier = Modifier.weight(1f).focusRequester(fieldFocus.amount)
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
                                    isError = uiState.proofError != null,
                                    onUploadClick = onUploadClick,
                                    modifier = Modifier
                                        .heightIn(min = 220.dp)
                                        .pointerHoverIcon(PointerIcon.Hand)
                                )
                                uiState.proofError?.let { FieldErrorText(it) }
                            }
                        }
                    }
                }
            },
            side = {
                // AdaptiveColumns adds no gap when stacked (Medium): add it here.
                if (!isExpanded()) Spacer(modifier = Modifier.height(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    WideInvoicesCard(
                        uiState = uiState,
                        onToggleInvoice = viewModel::toggleInvoiceSelection,
                        onRetry = viewModel::loadPendingInvoices
                    )
                    WideSummaryCard(
                        uiState = uiState,
                        onSubmit = onSubmit
                    )
                }
            }
        )
    }

    if (showDatePicker) {
        // A payment can't be reported for a future day (E-P4); the picker works in UTC-midnight millis.
        val todayPickerUtc = remember { localMillisToPickerUtc(Clock.System.now().toEpochMilliseconds()) }
        val currentYear = remember {
            Instant.fromEpochMilliseconds(todayPickerUtc).toLocalDateTime(TimeZone.UTC).year
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = localMillisToPickerUtc(uiState.date),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayPickerUtc
                override fun isSelectableYear(year: Int): Boolean = year <= currentYear
            }
        )
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
            DatePicker(
                state = datePickerState,
                // Web only: Material3's wasm formatter renders the UTC-midnight millis in local time (A-16)
                dateFormatter = if (isWebPlatform) rememberUtcDateFormatter() else remember { DatePickerDefaults.dateFormatter() }
            )
        }
    }
}

@Composable
private fun WideSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 16.dp).semantics { heading() }
    )
}

@Composable
private fun WideMethodFields(
    uiState: CreatePaymentUiState,
    viewModel: CreatePaymentViewModel,
    fieldFocus: PaymentFieldFocus
) {
    when (uiState.method) {
        PaymentMethod.PAGO_MOVIL -> {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BankField(uiState, viewModel, fieldFocus, Modifier.weight(1f))
                PhoneField(uiState, viewModel, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ReferenceField(
                    uiState, viewModel, fieldFocus,
                    placeholder = stringResource(Res.string.reference_placeholder_last6),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        PaymentMethod.TRANSFER -> {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BankField(uiState, viewModel, fieldFocus, Modifier.weight(1f))
                ReferenceField(
                    uiState, viewModel, fieldFocus,
                    placeholder = stringResource(Res.string.reference_placeholder_confirmation),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        PaymentMethod.CASH -> {
            Text(
                text = stringResource(Res.string.cash_zelle_payment_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WideInvoicesCard(
    uiState: CreatePaymentUiState,
    onToggleInvoice: (String) -> Unit,
    onRetry: () -> Unit
) {
    WebCard {
        WideSectionTitle(stringResource(Res.string.select_invoices_label))
        when {
            uiState.isLoadingInvoices -> LoadingState(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                fullScreen = false
            )
            // Load failure with retry, not "no pending invoices" (A-07)
            uiState.invoicesError != null -> ErrorState(
                title = uiState.invoicesError.asString(),
                onRetry = onRetry,
                modifier = Modifier.heightIn(max = 320.dp)
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
                text = stringResource(Res.string.no_pending_invoices),
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
    val selectedCount = uiState.selectedInvoiceIds.size
    val allocations = previewAllocations(
        amount = parseAmount(amount) ?: 0.0,
        selected = uiState.pendingInvoices.filter { uiState.selectedInvoiceIds.contains(it.id) }
    )

    WebCard {
        Text(
            text = pluralStringResource(Res.plurals.invoices_selected_count, selectedCount, selectedCount),
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
                        text = invoice.description ?: stringResource(Res.string.default_invoice_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Text(
                    text = stringResource(Res.string.currency_amount, formatCurrency(allocated)),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
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
                text = stringResource(Res.string.total_to_pay).uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = amountDisplay(amount),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }

        uiState.error?.let { error ->
            FormErrorText(error, Modifier.padding(bottom = 16.dp))
        }

        PrimaryButton(
            text = stringResource(Res.string.register_payment),
            onClick = onSubmit,
            enabled = !uiState.isLoading,
            isLoading = uiState.isLoading,
            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
        )

        Spacer(modifier = Modifier.height(16.dp))

        ValidationNotice()
    }
}

/**
 * Headline ("21/10/2026"), month label ("Octubre 2026") and day descriptions formatted in UTC with kotlinx-datetime.
 * Material3 1.9 on Kotlin/Wasm formats skeletons with a JS `Date` in the local zone, so west of UTC
 * the picker's UTC midnights showed the previous day and month (A-16).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberUtcDateFormatter(): DatePickerFormatter {
    val months = monthNames.map { stringResource(it) }
    return remember(months) {
        object : DatePickerFormatter {
            private fun utcDate(millis: Long) = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date

            override fun formatMonthYear(monthMillis: Long?, locale: CalendarLocale): String? =
                monthMillis?.let { utcDate(it) }?.let { "${months[it.monthNumber - 1]} ${it.year}" }

            // Headline: numeric like the date field (a long month name cut off the year at headlineLarge);
            // screen readers keep the long form.
            override fun formatDate(dateMillis: Long?, locale: CalendarLocale, forContentDescription: Boolean): String? =
                dateMillis?.let { utcDate(it) }?.let {
                    if (forContentDescription) "${it.dayOfMonth} ${months[it.monthNumber - 1]} ${it.year}"
                    else "${it.dayOfMonth.toString().padStart(2, '0')}/${it.monthNumber.toString().padStart(2, '0')}/${it.year}"
                }
        }
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
