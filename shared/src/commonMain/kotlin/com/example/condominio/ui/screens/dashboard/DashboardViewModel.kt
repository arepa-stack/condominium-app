package com.example.condominio.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.model.Payment
import com.example.condominio.data.model.SolvencyStatus
import com.example.condominio.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.example.condominio.ui.utils.UiText

class DashboardViewModel (
    private val authRepository: AuthRepository,
    private val paymentRepository: com.example.condominio.data.repository.PaymentRepository,
    private val buildingRepository: com.example.condominio.data.repository.BuildingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // Fetch currentUser to get selected unit
            val userResult = authRepository.fetchCurrentUser()
            
            if (userResult.isSuccess) {
                val user = userResult.getOrNull()
                val currentUnit = user?.currentUnit
                
                _uiState.update { state ->
                    state.copy(
                        userName = user?.name ?: "",
                        userBuilding = currentUnit?.buildingName ?: user?.building ?: "",
                        userApartment = currentUnit?.unitName ?: user?.apartmentUnit ?: ""
                    )
                }

                // If we have a unit ID, fetch balance
                // Fallback to legacy getPaymentSummary if no unit ID (shouldn't happen in Pro)
                val unitId = currentUnit?.unitId ?: user?.units?.firstOrNull()?.unitId
                
                if (!unitId.isNullOrEmpty()) {
                    // Fetch Balance
                    launch {
                        val result = paymentRepository.getBalance(unitId)
                        result.onSuccess { balance ->
                            val solvency = if (balance.totalDebt > 0) SolvencyStatus.PENDING else SolvencyStatus.SOLVENT
                            _uiState.update {
                                it.copy(
                                    solvencyStatus = solvency,
                                    totalDebt = balance.totalDebt,
                                    pendingInvoices = balance.pendingInvoices,
                                )
                            }
                        }.onFailure { error ->
                             _uiState.update { it.copy(error = error.message?.let { UiText.DynamicString(it) }) }
                        }
                    }

                    // Fetch Recent Transactions
                    launch {
                        try {
                            val payments = paymentRepository.getPayments(unitId)
                            _uiState.update { 
                                it.copy(
                                    recentPayments = payments.take(5), // Show last 5
                                    isLoading = false 
                                )
                            }
                        } catch (e: Exception) {
                            _uiState.update { it.copy(isLoading = false, error = e.message?.let { UiText.DynamicString(it) }) }
                        }
                    }
                } else {
                     // Legacy fallback
                     launch {
                        paymentRepository.getPaymentSummary().onSuccess { summary ->
                            _uiState.update {
                                it.copy(
                                    solvencyStatus = summary.solvencyStatus,
                                    recentPayments = summary.recentTransactions,
                                    isLoading = false
                                )
                            }
                        }.onFailure { error ->
                            _uiState.update { it.copy(isLoading = false, error = error.message?.let { UiText.DynamicString(it) }) }
                        }
                     }
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = userResult.exceptionOrNull()?.message?.let { UiText.DynamicString(it) }) }
            }
        }
    }
    
    fun refresh() {
        loadData()
    }
}

data class DashboardUiState(
    val userName: String = "",
    val userBuilding: String = "",
    val userApartment: String = "",
    val solvencyStatus: SolvencyStatus = SolvencyStatus.PENDING,
    val recentPayments: List<Payment> = emptyList(),
    val isLoading: Boolean = false,
    val totalDebt: Double = 0.0,
    val pendingInvoices: List<com.example.condominio.data.model.Invoice> = emptyList(),
    val error: UiText? = null
)
