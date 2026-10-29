package com.example.condominio.ui.screens.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.model.Invoice
import com.example.condominio.data.repository.applyInvoicePayments
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

    private var entered = false

    /** Every re-entry reloads (back from the detail, a payment or another tab); the first one `init` loaded. */
    fun onScreenReturn() {
        if (entered) refresh() else entered = true
    }

    private fun loadInvoices() {
        viewModelScope.launch {
            // Clear the previous error so a successful refresh doesn't keep showing it (A-08)
            _uiState.update { it.copy(isLoading = true, error = null) }

            val user = authRepository.fetchCurrentUser().getOrNull()
            val unitId = user?.currentUnit?.unitId ?: user?.units?.firstOrNull()?.unitId

            if (!unitId.isNullOrEmpty()) {
                val result = paymentRepository.getInvoices(unitId)
                result.onSuccess { invoices ->
                    val withPayments = paymentRepository.applyInvoicePayments(invoices)
                    _uiState.update {
                        it.copy(
                            invoices = withPayments,
                            isLoading = false
                        )
                    }
                }.onFailure {
                    // Error state with retry instead of the raw exception message (A-07)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = UiText.StringResource(Res.string.error_loading_invoice_list)
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

    /** Kept here (not in `remember`) so the filter survives going to the detail and back (E-F4). */
    fun onTabSelected(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }
}

data class InvoiceListUiState(
    val invoices: List<Invoice> = emptyList(),
    val isLoading: Boolean = false,
    val error: UiText? = null,
    /** 0 = all, 1 = pending, 2 = paid. */
    val selectedTab: Int = 0
)
