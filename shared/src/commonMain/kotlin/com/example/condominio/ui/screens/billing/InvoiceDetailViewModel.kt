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
import com.example.condominio.ui.utils.UiText
import condominio.shared.generated.resources.*

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

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Fetch Invoice Details
            launch {
                paymentRepository.getInvoice(invoiceId)
                    .onSuccess { invoice ->
                        _uiState.update { it.copy(invoice = invoice) }
                    }
                    .onFailure { e ->
                        println("Error: `Error loading invoice $invoiceId: ${e.message}")
                        _uiState.update { it.copy(error = UiText.DynamicString(e.message ?: "")) }
                    }
            }

            // Fetch Payments
            launch {
                paymentRepository.getInvoicePayments(invoiceId)
                    .onSuccess { payments ->
                        _uiState.update { 
                            it.copy(
                                payments = payments,
                                isLoading = false
                            )
                        }
                    }
                    .onFailure {
                        _uiState.update { it.copy(isLoading = false, error = UiText.StringResource(Res.string.error_unknown)) }
                    }
            }
        }
    }
}

data class InvoiceDetailUiState(
    val invoice: com.example.condominio.data.model.Invoice? = null,
    val payments: List<Payment> = emptyList(),
    val isLoading: Boolean = false,
    val error: UiText? = null
)
