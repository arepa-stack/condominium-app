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
            _uiState.update { it.copy(isLoading = true) }
            
            val user = authRepository.fetchCurrentUser().getOrNull()
            val unitId = user?.currentUnit?.unitId ?: user?.units?.firstOrNull()?.unitId

            try {
                val payments = paymentRepository.getPayments(unitId)
                _uiState.update { 
                    it.copy(
                        payments = payments,
                        isLoading = false
                    ) 
                }
            } catch (e: Exception) {
                // Handle error
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}

data class PaymentHistoryUiState(
    val payments: List<Payment> = emptyList(),
    val unit: String = "",
    val isLoading: Boolean = false
)
