package com.example.condominio.ui.screens.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.model.PaymentMethod
import com.example.condominio.data.repository.PaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

import com.example.condominio.data.repository.AuthRepository
import kotlinx.coroutines.flow.first
import com.example.condominio.ui.utils.UiText
import condominio.shared.generated.resources.*
import com.example.condominio.ui.utils.formatCurrency

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

    private fun loadPendingInvoices() {
        viewModelScope.launch {
            val user = authRepository.fetchCurrentUser().getOrNull()
            val unitId = user?.currentUnit?.unitId ?: user?.units?.firstOrNull()?.unitId
            
            if (unitId != null) {
                _uiState.update { it.copy(isLoadingInvoices = true) }
                paymentRepository.getBalance(unitId).onSuccess { balance ->
                    availableInvoices = balance.pendingInvoices
                    _uiState.update { 
                        it.copy(
                            pendingInvoices = balance.pendingInvoices,
                            isLoadingInvoices = false
                        )
                    }
                    
                    // Pre-select invoice if passed in navigation arguments
                    val invoiceIdArg = savedStateHandle.get<String>("invoiceId")
                    if (!invoiceIdArg.isNullOrEmpty()) {
                         toggleInvoiceSelection(invoiceIdArg)
                    }
                }.onFailure {
                    _uiState.update { it.copy(isLoadingInvoices = false) }
                }
            }
        }
    }

    fun onAmountChange(amount: String) {
        _uiState.update { it.copy(amount = amount) }
    }

    fun onDateChange(date: Long) {
        _uiState.update { it.copy(date = date) }
    }

    fun onDescriptionChange(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun onMethodChange(method: PaymentMethod) {
        _uiState.update { it.copy(method = method) }
    }

    fun onBankChange(bank: String) {
        _uiState.update { it.copy(bank = bank) }
    }

    fun onReferenceChange(reference: String) {
        _uiState.update { it.copy(reference = reference) }
    }

    fun onPhoneChange(phone: String) {
        _uiState.update { it.copy(phone = phone) }
    }

    fun onProofUrlChange(url: String) {
        _uiState.update { it.copy(proofUrl = url) }
    }

    fun toggleInvoiceSelection(invoiceId: String) {
        _uiState.update { state ->
            val newSelection = if (state.selectedInvoiceIds.contains(invoiceId)) {
                state.selectedInvoiceIds - invoiceId
            } else {
                state.selectedInvoiceIds + invoiceId
            }
            
            // Auto-update amount based on selection
            val selectedTotal = availableInvoices
                .filter { newSelection.contains(it.id) }
                .sumOf { it.remaining }
                
            state.copy(
                selectedInvoiceIds = newSelection,
                amount = formatCurrency(selectedTotal)
            )
        }
    }

    fun onSubmitClick() {
        val state = _uiState.value
        val amount = state.amount.toDoubleOrNull()
        
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(error = UiText.StringResource(Res.string.error_invalid_amount)) }
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
                _uiState.update { it.copy(error = UiText.DynamicString(error.message ?: "")) }
            }
        }
    }
}

data class CreatePaymentUiState(
    val amount: String = "",
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
    val error: UiText? = null,
    val isSuccess: Boolean = false
)
