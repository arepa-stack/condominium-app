package com.example.condominio.ui.screens.pettycash

import androidx.compose.animation.*
import com.example.condominio.ui.utils.formatCurrency
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import condominio.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.koin.compose.viewmodel.koinViewModel
import coil3.compose.AsyncImage
import com.example.condominio.data.model.*

import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PettyCashScreen(onBackClick: () -> Unit, viewModel: PettyCashViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var showIncomeSheet by remember { mutableStateOf(false) }
    var showExpenseSheet by remember { mutableStateOf(false) }

    @Composable
    fun PettyCashCategory.toLabel(): String = when (this) {
        PettyCashCategory.REPAIR -> stringResource(Res.string.cat_repair)
        PettyCashCategory.CLEANING -> stringResource(Res.string.cat_cleaning)
        PettyCashCategory.EMERGENCY -> stringResource(Res.string.cat_emergency)
        PettyCashCategory.OFFICE -> stringResource(Res.string.cat_office)
        PettyCashCategory.UTILITIES -> stringResource(Res.string.cat_utilities)
        PettyCashCategory.OTHER -> stringResource(Res.string.cat_other)
    }
    var selectedTransactionForEvidence by remember {
        mutableStateOf<PettyCashTransactionDto?>(null)
    }
    var successData by remember { mutableStateOf<SuccessFeedbackData?>(null) }

    if (successData != null) {
        LaunchedEffect(successData) {
            kotlinx.coroutines.delay(4000)
            successData = null
        }
    }

    // Lazy load next page
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }.collect { index
            ->
            if (index != null && index >= uiState.history.size - 2) {
                viewModel.loadNextPage()
            }
        }
    }

    Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                        title = { Text(stringResource(Res.string.petty_cash_title), fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                            }
                        },
                        colors =
                                TopAppBarDefaults.centerAlignedTopAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.background,
                                        titleContentColor = MaterialTheme.colorScheme.onBackground
                                )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Balance Section
            BalanceCard(
                    balance = uiState.balance,
                    canManage = uiState.canManage,
                    onIncomeClick = { showIncomeSheet = true },
                    onExpenseClick = { showExpenseSheet = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // History Section
            Text(
                    text = stringResource(Res.string.recent_activity),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            if (uiState.isRefreshing && uiState.history.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.history.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                            stringResource(Res.string.no_transactions),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                        state = scrollState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.history) { transaction ->
                        TransactionItem(
                                transaction = transaction,
                                label = transaction.category.toLabel(),
                                onEvidenceClick = { selectedTransactionForEvidence = transaction }
                        )
                    }
                    if (uiState.isLoading) {
                        item {
                            Box(
                                    Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
                        }
                    }
                }
            }
        }
    }

    // Sheets & Dialogs
    if (showIncomeSheet) {
        MovementSheet(
                title = stringResource(Res.string.register_income),
                isIncome = true,
                categoryToLabel = { it.toLabel() },
                onDismiss = { showIncomeSheet = false },
                onConfirm = { amount, desc, _, _ ->
                    viewModel.registerIncome(amount, desc) { showIncomeSheet = false }
                },
                isSubmitting = uiState.isSubmitting
        )
    }

    if (showExpenseSheet) {
        MovementSheet(
                title = stringResource(Res.string.register_expense),
                isIncome = false,
                categoryToLabel = { it.toLabel() },
                onDismiss = { showExpenseSheet = false },
                onConfirm = { amount, desc, category, evidence ->
                    val currentBalance = uiState.balance?.currentBalance ?: 0.0
                    viewModel.registerExpense(amount, desc, category!!, evidence) {
                        showExpenseSheet = false
                        // Trigger success feedback
                        val isPartial = amount > currentBalance
                        successData =
                                SuccessFeedbackData(
                                        amount = amount,
                                        covered = if (isPartial) currentBalance else amount,
                                        extra = if (isPartial) amount - currentBalance else 0.0
                                )
                    }
                },
                isSubmitting = uiState.isSubmitting,
                currentBalance = uiState.balance?.currentBalance ?: 0.0
        )
    }

    // Success Feedback Overlay
    AnimatedVisibility(
            visible = successData != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.zIndex(10f)
    ) { successData?.let { data -> SuccessFeedback(data) } }

    if (selectedTransactionForEvidence != null) {
        EvidenceDialog(
                transaction = selectedTransactionForEvidence!!,
                onDismiss = { selectedTransactionForEvidence = null }
        )
    }

    // Error Dialog
    if (uiState.error != null) {
        AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text(stringResource(Res.string.attention)) },
                text = { Text(uiState.error!!.asString()) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) { Text(stringResource(Res.string.understood)) }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun BalanceCard(
        balance: PettyCashBalanceDto?,
        canManage: Boolean,
        onIncomeClick: () -> Unit,
        onExpenseClick: () -> Unit
) {
    Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                    text = stringResource(Res.string.available_balance),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                    text =
                            "${balance?.currency ?: "$"} ${formatCurrency(balance?.currentBalance ?: 0.0)}",
                    style =
                            MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                            )
            )
            if (balance != null) {
                Text(
                        text = stringResource(Res.string.from_date_label, balance.updatedAt.take(10)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            if (canManage) {
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ActionBtn(
                            label = stringResource(Res.string.income_item_label),
                            icon = Icons.Default.Add,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f),
                            onClick = onIncomeClick
                    )
                    ActionBtn(
                            label = stringResource(Res.string.expense_item_label),
                            icon = Icons.Default.Remove,
                            color = Color(0xFFE53935),
                            modifier = Modifier.weight(1f),
                            onClick = onExpenseClick
                    )
                }
            }
        }
    }
}

@Composable
fun ActionBtn(
        label: String,
        icon: ImageVector,
        color: Color,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
) {
    Surface(
            onClick = onClick,
            modifier = modifier.height(56.dp),
            color = color.copy(alpha = 0.12f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
fun TransactionItem(transaction: PettyCashTransactionDto, label: String, onEvidenceClick: () -> Unit) {
    val isIncome = transaction.type == PettyCashTransactionType.INCOME
    val color = if (isIncome) Color(0xFF4CAF50) else Color(0xFFE53935)
    val icon =
            when (transaction.category) {
                PettyCashCategory.REPAIR -> Icons.Default.Build
                PettyCashCategory.CLEANING -> Icons.Default.CleaningServices
                PettyCashCategory.EMERGENCY -> Icons.Default.Warning
                PettyCashCategory.OFFICE -> Icons.Default.Work
                PettyCashCategory.UTILITIES -> Icons.Default.Power
                PettyCashCategory.OTHER -> Icons.AutoMirrored.Filled.List
            }

    Surface(
            modifier =
                    Modifier.fillMaxWidth().clickable {
                        if (transaction.evidenceUrl != null) onEvidenceClick()
                    },
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 1.dp
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                    modifier =
                            Modifier.size(52.dp).background(color.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                        text =
                                "$label · ${transaction.createdAt.take(10)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                        text =
                                "${if (isIncome) "+" else "-"} $${formatCurrency(transaction.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                )
                if (transaction.evidenceUrl != null) {
                    Icon(
                            Icons.Default.Receipt,
                            contentDescription = stringResource(Res.string.view_receipt),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementSheet(
        title: String,
        isIncome: Boolean,
        onDismiss: () -> Unit,
        onConfirm: (Double, String, PettyCashCategory?, String?) -> Unit,
        isSubmitting: Boolean,
        currentBalance: Double = 0.0,
        categoryToLabel: @Composable (PettyCashCategory) -> String = { it.name }
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember {
        mutableStateOf(if (isIncome) PettyCashCategory.OTHER else PettyCashCategory.REPAIR)
    }
    var evidencePath by remember { mutableStateOf<String?>(null) }
    var showCategoryMenu by remember { mutableStateOf(false) }

    val imagePicker: () -> Unit = {
        scope.launch {
            evidencePath = "mock_evidence_url"
        }
    }

    ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
                modifier =
                        Modifier.padding(horizontal = 24.dp)
                                .padding(bottom = 48.dp)
                                .navigationBarsPadding()
                                .imePadding()
        ) {
            Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        if (it.isEmpty() || it.toDoubleOrNull() != null) amount = it
                    },
                    label = { Text(stringResource(Res.string.amount_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp),
                    colors =
                            OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
            )

            if (!isIncome && amount.isNotEmpty()) {
                val amt = amount.toDoubleOrNull() ?: 0.0
                if (amt > currentBalance) {
                    Card(
                            colors =
                                    CardDefaults.cardColors(
                                            containerColor = Color(0xFFFF0040).copy(alpha = 0.1f)
                                    ),
                            border =
                                    androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            Color(0xFFFF0040).copy(alpha = 0.5f)
                                    ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                        Icons.Default.Warning,
                                        null,
                                        tint = Color(0xFFFF0040),
                                        modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                        stringResource(Res.string.insufficient_balance_title),
                                        color = Color(0xFFFF0040),
                                        fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(Res.string.insufficient_balance_desc, formatCurrency(currentBalance), formatCurrency(amt), formatCurrency(amt - currentBalance)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(Res.string.description_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
            )

            if (!isIncome) {
                Spacer(modifier = Modifier.height(16.dp))

                Box {
                    OutlinedTextField(
                            value = categoryToLabel(category),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(Res.string.category_label)) },
                            modifier =
                                    Modifier.fillMaxWidth().clickable { showCategoryMenu = true },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            shape = RoundedCornerShape(16.dp),
                            enabled = false,
                            colors =
                                    OutlinedTextFieldDefaults.colors(
                                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                                            disabledLabelColor =
                                                    MaterialTheme.colorScheme.onSurface.copy(
                                                            alpha = 0.6f
                                                    )
                                    )
                    )
                    DropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        PettyCashCategory.values().forEach { cat ->
                            DropdownMenuItem(
                                    text = { Text(categoryToLabel(cat)) },
                                    onClick = {
                                        category = cat
                                        showCategoryMenu = false
                                    }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                        onClick = { imagePicker() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors =
                                ButtonDefaults.buttonColors(
                                        containerColor =
                                                MaterialTheme.colorScheme.primary.copy(
                                                        alpha = 0.1f
                                                ),
                                        contentColor = MaterialTheme.colorScheme.primary
                                )
                ) {
                    Icon(
                            if (evidencePath != null) Icons.Default.Check
                            else Icons.Default.CameraAlt,
                            null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (evidencePath != null) stringResource(Res.string.evidence_ready) else stringResource(Res.string.upload_evidence))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                    onClick = {
                        val amt = amount.toDoubleOrNull() ?: 0.0
                        if (amt > 0 && description.isNotBlank()) {
                            onConfirm(amt, description, category, evidencePath)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled =
                            !isSubmitting &&
                                    amount.isNotEmpty() &&
                                    (isIncome || amount.toDoubleOrNull() != null),
                    shape = RoundedCornerShape(16.dp),
                    colors =
                            ButtonDefaults.buttonColors(
                                    containerColor =
                                            if (!isIncome &&
                                                            (amount.toDoubleOrNull()
                                                                    ?: 0.0) > currentBalance
                                            )
                                                    Color(0xFFFF0040)
                                            else MaterialTheme.colorScheme.primary
                            )
            ) {
                if (isSubmitting)
                        CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                        )
                else {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    val isOverage = !isIncome && amt > currentBalance
                    Text(
                            if (isOverage) stringResource(Res.string.confirm_overage) else stringResource(Res.string.register),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                    )
                }
            }
        }
    }
}

data class SuccessFeedbackData(val amount: Double, val covered: Double, val extra: Double)

@Composable
fun SuccessFeedback(data: SuccessFeedbackData) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Dim background
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))

        // Glow Card
        Card(
                modifier = Modifier.padding(32.dp).fillMaxWidth(),
                colors =
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                        modifier =
                                Modifier.size(80.dp)
                                        .background(
                                                Color(0xFF4CAF50).copy(alpha = 0.15f),
                                                CircleShape
                                        )
                                        .border(
                                                2.dp,
                                                Color(0xFF4CAF50).copy(alpha = 0.3f),
                                                CircleShape
                                        ),
                        contentAlignment = Alignment.Center
                ) {
                    Icon(
                            Icons.Default.Check,
                            null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                        stringResource(Res.string.expense_registered_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                )

                if (data.extra > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(Res.string.petty_cash_title), style = MaterialTheme.typography.bodyMedium)
                        Text(
                                "-$${formatCurrency(data.covered)}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                                stringResource(Res.string.excess_neighbors),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFFF0040)
                        )
                        Text(
                                "-$${formatCurrency(data.extra)}",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF0040)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EvidenceDialog(transaction: PettyCashTransactionDto, onDismiss: () -> Unit) {
    AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(Res.string.expense_evidence_title), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    AsyncImage(
                            model = transaction.evidenceUrl,
                            contentDescription = null,
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .height(350.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color.Black.copy(alpha = 0.05f))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(transaction.description, style = MaterialTheme.typography.bodyLarge)
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.close), fontWeight = FontWeight.Bold) }
            },
            shape = RoundedCornerShape(28.dp)
    )
}

