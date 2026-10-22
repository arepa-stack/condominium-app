package com.example.condominio.ui.screens.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.model.Payment
import com.example.condominio.data.repository.PaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.example.condominio.data.repository.AuthRepository
import kotlinx.coroutines.flow.collectLatest

class PaymentHistoryViewModel (
    private val paymentRepository: PaymentRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentHistoryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadPayments()
        observeUser()
    }
    
    private fun observeUser() {
        viewModelScope.launch {
            authRepository.currentUser.collectLatest { user ->
                if (user != null) {
                    _uiState.update { it.copy(unit = user.apartmentUnit) }
                }
            }
        }
    }

    fun loadPayments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadFailed = false) }

            val user = authRepository.fetchCurrentUser().getOrNull()
            val unitId = user?.currentUnit?.unitId ?: user?.units?.firstOrNull()?.unitId

            paymentRepository.fetchPayments(unitId)
                .onSuccess { payments ->
                    _uiState.update {
                        it.copy(
                            payments = payments,
                            isLoading = false
                        )
                    }
                }
                .onFailure {
                    // Keep what was already shown; the screen shows the error only when there is nothing to show (A-07)
                    _uiState.update { it.copy(isLoading = false, loadFailed = true) }
                }
        }
    }
}

data class PaymentHistoryUiState(
    val payments: List<Payment> = emptyList(),
    val unit: String = "",
    val isLoading: Boolean = false,
    /** Last load failed (network/HTTP): not the same as "no payments". */
    val loadFailed: Boolean = false
)
