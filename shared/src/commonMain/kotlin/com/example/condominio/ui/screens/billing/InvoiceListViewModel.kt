package com.example.condominio.ui.screens.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.model.Invoice
import com.example.condominio.data.repository.AuthRepository
import com.example.condominio.data.repository.PaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.condominio.ui.utils.UiText
import condominio.shared.generated.resources.*

class InvoiceListViewModel (
    private val authRepository: AuthRepository,
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvoiceListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadInvoices()
    }

    private fun loadInvoices() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val user = authRepository.fetchCurrentUser().getOrNull()
            val unitId = user?.currentUnit?.unitId ?: user?.units?.firstOrNull()?.unitId
            
            if (!unitId.isNullOrEmpty()) {
                val result = paymentRepository.getInvoices(unitId)
                result.onSuccess { invoices ->
                    _uiState.update { 
                        it.copy(
                            invoices = invoices,
                            isLoading = false
                        )
                    }
                }.onFailure { e ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = UiText.DynamicString(e.message ?: "")
                        )
                    }
                }
            } else {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = UiText.StringResource(Res.string.error_user_unit_not_found)
                    )
                }
            }
        }
    }

    fun refresh() {
        loadInvoices()
    }
}

data class InvoiceListUiState(
    val invoices: List<Invoice> = emptyList(),
    val isLoading: Boolean = false,
    val error: UiText? = null
)
