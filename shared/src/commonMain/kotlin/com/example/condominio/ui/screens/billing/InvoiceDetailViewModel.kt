package com.example.condominio.ui.screens.billing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.model.Payment
import com.example.condominio.data.repository.PaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InvoiceDetailViewModel (
    private val paymentRepository: PaymentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val invoiceId: String = checkNotNull(savedStateHandle["invoiceId"])

    private val _uiState = MutableStateFlow(InvoiceDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    /** Loads the invoice and its payments; also the retry action of both error states (A-07). */
    fun loadData() {
        loadInvoice()
        loadPayments()
    }

    private fun loadInvoice() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadFailed = false) }
            val result = paymentRepository.getInvoice(invoiceId)
            _uiState.update {
                it.copy(
                    invoice = result.getOrNull() ?: it.invoice,
                    isLoading = false,
                    // A failed load is not "invoice not found" (A-07)
                    loadFailed = result.isFailure
                )
            }
        }
    }

    fun loadPayments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPayments = true, paymentsLoadFailed = false) }
            val result = paymentRepository.getInvoicePayments(invoiceId)
            _uiState.update {
                it.copy(
                    payments = result.getOrNull() ?: it.payments,
                    isLoadingPayments = false,
                    // A failed load is not "no payments" (A-07)
                    paymentsLoadFailed = result.isFailure
                )
            }
        }
    }
}

data class InvoiceDetailUiState(
    val invoice: com.example.condominio.data.model.Invoice? = null,
    val payments: List<Payment> = emptyList(),
    /** Invoice request in flight (the payments list has its own flag). */
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
    val isLoadingPayments: Boolean = false,
    val paymentsLoadFailed: Boolean = false
)
