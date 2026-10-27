package com.example.condominio.ui.screens.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.model.PaymentMethod
import com.example.condominio.data.repository.PaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

import com.example.condominio.data.repository.AuthRepository
import kotlinx.coroutines.flow.first
import com.example.condominio.ui.utils.UiText
import condominio.shared.generated.resources.*
import com.example.condominio.ui.utils.formatAmountInput
import com.example.condominio.ui.utils.parseAmount

class CreatePaymentViewModel (
    private val paymentRepository: PaymentRepository,
    private val authRepository: AuthRepository,
    private val savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePaymentUiState())
    val uiState = _uiState.asStateFlow()

    // Cache invoices to lookup details
    private var availableInvoices: List<com.example.condominio.data.model.Invoice> = emptyList()

    init {
        loadPendingInvoices()
    }

    fun loadPendingInvoices() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingInvoices = true, invoicesError = null) }
            val user = authRepository.fetchCurrentUser().getOrNull()
            val unitId = user?.currentUnit?.unitId ?: user?.units?.firstOrNull()?.unitId

            if (unitId == null) {
                _uiState.update {
                    it.copy(isLoadingInvoices = false, invoicesError = UiText.StringResource(Res.string.error_loading_invoices))
                }
            } else {
                paymentRepository.getBalance(unitId).onSuccess { balance ->
                    availableInvoices = balance.pendingInvoices
                    _uiState.update {
                        it.copy(
                            pendingInvoices = balance.pendingInvoices,
                            isLoadingInvoices = false
                        )
                    }

                    // Pre-select invoice if passed in navigation arguments (once: a retry must not unselect it)
                    val invoiceIdArg = savedStateHandle.get<String>("invoiceId")
                    if (!invoiceIdArg.isNullOrEmpty() && invoiceIdArg !in _uiState.value.selectedInvoiceIds) {
                         toggleInvoiceSelection(invoiceIdArg)
                    }
                }.onFailure {
                    // Error state with retry instead of "no pending invoices" (A-07)
                    _uiState.update {
                        it.copy(isLoadingInvoices = false, invoicesError = UiText.StringResource(Res.string.error_loading_invoices))
                    }
                }
            }
        }
    }

    fun onAmountChange(amount: String) {
        // Once typed, (un)selecting invoices stops overwriting it; clearing the field re-enables auto-fill (A-03)
        _uiState.update { it.copy(amount = amount, amountEdited = amount.isNotBlank(), amountError = null) }
    }

    fun onDateChange(date: Long) {
        _uiState.update { it.copy(date = date) }
    }

    fun onDescriptionChange(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun onMethodChange(method: PaymentMethod) {
        _uiState.update { it.copy(method = method, bankError = null, referenceError = null) }
    }

    fun onBankChange(bank: String) {
        _uiState.update { it.copy(bank = bank, bankError = null) }
    }

    fun onReferenceChange(reference: String) {
        _uiState.update { it.copy(reference = reference, referenceError = null) }
    }

    fun onPhoneChange(phone: String) {
        _uiState.update { it.copy(phone = phone) }
    }

    fun onProofUrlChange(url: String) {
        _uiState.update { it.copy(proofUrl = url, proofError = null) }
    }

    /** The picked file was rejected (unreadable or over 5 MB): no file kept, reason shown under the upload box. */
    fun onProofRejected(reason: UiText) {
        _uiState.update { it.copy(proofUrl = null, proofError = reason) }
    }

    fun toggleInvoiceSelection(invoiceId: String) {
        _uiState.update { state ->
            val newSelection = if (state.selectedInvoiceIds.contains(invoiceId)) {
                state.selectedInvoiceIds - invoiceId
            } else {
                state.selectedInvoiceIds + invoiceId
            }

            // Auto-fill the amount from the selection only while the user hasn't typed one (A-03).
            // Input format ("1500.00"), never the display one: iOS formatCurrency used to add grouping (A-02).
            val selectedTotal = availableInvoices
                .filter { newSelection.contains(it.id) }
                .sumOf { it.remaining }

            state.copy(
                selectedInvoiceIds = newSelection,
                amount = if (state.amountEdited) state.amount else formatAmountInput(selectedTotal),
                amountError = if (state.amountEdited) state.amountError else null,
                error = null
            )
        }
    }

    fun onSubmitClick() {
        val state = _uiState.value
        if (state.isLoading) return
        val amount = parseAmount(state.amount)

        // Validate every field at once so each shows its own inline error (E-P1)
        val requiresBankInfo = state.method == PaymentMethod.TRANSFER ||
                state.method == PaymentMethod.PAGO_MOVIL
        val errors = state.copy(
            amountError = if (amount == null || amount <= 0) UiText.StringResource(Res.string.error_invalid_amount) else null,
            proofError = if (state.proofUrl.isNullOrEmpty()) {
                state.proofError ?: UiText.StringResource(Res.string.error_proof_required)
            } else null,
            bankError = if (requiresBankInfo && state.bank.isBlank()) UiText.StringResource(Res.string.error_bank_required) else null,
            referenceError = if (requiresBankInfo && state.reference.isBlank()) UiText.StringResource(Res.string.error_reference_required) else null,
            error = null
        )
        if (errors.hasFieldErrors || amount == null) {
            _uiState.value = errors
            return
        }

        // Without the invoice list nothing can be allocated: don't register an unallocated payment
        if (state.invoicesError != null) {
            _uiState.update { it.copy(error = state.invoicesError) }
            return
        }

        /*
        // Validation: Must select at least one invoice OR provide description?
        // Allocation logic:
        // 1. If invoices selected, allocate amount to them order by date (oldest first).
        // 2. If no invoices selected, treat as "Unallocated" or "General Payment"?
        //    For now, enforcing invoice selection if pending invoices exist.
        */

        if (state.selectedInvoiceIds.isEmpty() && state.pendingInvoices.isNotEmpty()) {
             // Optional: Allow proceeding without selection if valid reason?
             // Enforcing selection for better data quality
             _uiState.update { it.copy(error = UiText.StringResource(Res.string.error_select_invoices)) }
             return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Get current user and unit
            val user = authRepository.fetchCurrentUser().getOrNull()
            val unitId = user?.currentUnit?.unitId ?: user?.units?.firstOrNull()?.unitId

            if (unitId.isNullOrEmpty()) {
                _uiState.update { it.copy(isLoading = false, error = UiText.StringResource(Res.string.error_user_unit_not_found)) }
                return@launch
            }

            // Calculate Allocations
            val allocations = mutableListOf<com.example.condominio.data.model.PaymentAllocation>()
            var remainingAmount = amount

            // Sort selected invoices by period/date (Assuming period YYYY-MM sortable string)
            // Sort selected invoices by period/date
            val selectedInvoices = availableInvoices
                .filter { state.selectedInvoiceIds.contains(it.id) }
                .sortedBy { it.period }

            for (invoice in selectedInvoices) {
                if (remainingAmount <= 0) break

                val toPay = minOf(remainingAmount, invoice.remaining)
                allocations.add(com.example.condominio.data.model.PaymentAllocation(
                    invoiceId = invoice.id,
                    amount = toPay
                ))
                remainingAmount -= toPay
            }

            val result = paymentRepository.createPayment(
                amount = amount,
                date = state.date,
                description = state.description,
                method = state.method,
                unitId = unitId,
                allocations = allocations,
                reference = state.reference.ifBlank { null },
                bank = state.bank.ifBlank { null },
                phone = state.phone.ifBlank { null },
                proofUrl = state.proofUrl,
                buildingId = user?.currentUnit?.buildingId
            )
            _uiState.update { it.copy(isLoading = false) }

            result.onSuccess {
                _uiState.update { it.copy(isSuccess = true) }
            }.onFailure { error ->
                // Never a silent failure: blank messages fall back to a generic error
                val message = error.message?.takeIf { it.isNotBlank() }
                _uiState.update {
                    it.copy(error = message?.let { msg -> UiText.DynamicString(msg) } ?: UiText.StringResource(Res.string.error_unknown))
                }
            }
        }
    }
}

data class CreatePaymentUiState(
    val amount: String = "",
    /** True once the user typed an amount: selecting invoices no longer overwrites it. */
    val amountEdited: Boolean = false,
    val date: Long = Clock.System.now().toEpochMilliseconds(),
    val description: String = "",
    val method: PaymentMethod = PaymentMethod.PAGO_MOVIL,
    val bank: String = "",
    val reference: String = "",
    val phone: String = "",
    val proofUrl: String? = null,
    val pendingInvoices: List<com.example.condominio.data.model.Invoice> = emptyList(),
    val selectedInvoiceIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isLoadingInvoices: Boolean = false,
    /** Pending invoices failed to load: shown with a retry, not as "no pending invoices". */
    val invoicesError: UiText? = null,
    // Inline, per-field validation errors (cleared when the field is edited)
    val amountError: UiText? = null,
    val bankError: UiText? = null,
    val referenceError: UiText? = null,
    val proofError: UiText? = null,
    /** Form-level error: invoice selection, invoices not loaded, submit failure. */
    val error: UiText? = null,
    val isSuccess: Boolean = false
) {
    val hasFieldErrors: Boolean
        get() = amountError != null || bankError != null || referenceError != null || proofError != null
}
