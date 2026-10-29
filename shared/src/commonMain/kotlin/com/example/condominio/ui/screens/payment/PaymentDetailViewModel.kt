package com.example.condominio.ui.screens.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.model.Payment
import com.example.condominio.data.repository.PaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PaymentDetailViewModel(
    private val paymentRepository: PaymentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val paymentId: String? = savedStateHandle["paymentId"]

    init {
        loadPayment()
    }

    fun loadPayment() {
        if (paymentId == null) {
            _uiState.update { it.copy(loadFailed = true) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadFailed = false) }
            val result = paymentRepository.fetchPayment(paymentId)
            _uiState.update {
                it.copy(
                    payment = result.getOrNull() ?: it.payment,
                    isLoading = false,
                    // Error state with retry instead of a blank screen (A-07)
                    loadFailed = result.isFailure
                )
            }
        }
    }
}

data class PaymentDetailUiState(
    val payment: Payment? = null,
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false
)
