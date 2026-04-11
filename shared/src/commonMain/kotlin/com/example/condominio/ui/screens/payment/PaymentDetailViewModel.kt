package com.example.condominio.ui.screens.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.model.Payment
import com.example.condominio.data.repository.PaymentRepository
import com.example.condominio.data.utils.PdfService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PaymentDetailViewModel(
    private val paymentRepository: PaymentRepository,
    private val pdfService: PdfService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val paymentId: String? = savedStateHandle["paymentId"]

    init {
        loadPayment()
    }

    private fun loadPayment() {
        if (paymentId == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val payment = paymentRepository.getPayment(paymentId)
            _uiState.update {
                it.copy(
                    payment = payment,
                    isLoading = false
                )
            }
        }
    }

    fun onDownloadReceiptClick() {
        val payment = _uiState.value.payment ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val file = pdfService.generateReceipt(payment)
            _uiState.update { it.copy(isLoading = false, pdfFile = file) }
        }
    }

    fun onPdfShown() {
        _uiState.update { it.copy(pdfFile = null) }
    }
}

data class PaymentDetailUiState(
    val payment: Payment? = null,
    val isLoading: Boolean = false,
    val pdfFile: String? = null
)
