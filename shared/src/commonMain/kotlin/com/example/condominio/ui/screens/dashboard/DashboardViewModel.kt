package com.example.condominio.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.model.Payment
import com.example.condominio.data.model.PettyCashBalanceDto
import com.example.condominio.data.model.SolvencyStatus
import com.example.condominio.data.repository.AuthRepository
import com.example.condominio.data.repository.PettyCashRepository
import com.example.condominio.ui.utils.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val authRepository: AuthRepository,
    private val paymentRepository: com.example.condominio.data.repository.PaymentRepository,
    private val buildingRepository: com.example.condominio.data.repository.BuildingRepository,
    private val pettyCashRepository: PettyCashRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val userResult = authRepository.fetchCurrentUser()

            if (userResult.isSuccess) {
                val user = userResult.getOrNull()
                val currentUnit = user?.currentUnit

                _uiState.update { state ->
                    state.copy(
                        userName = user?.name ?: "",
                        userBuilding = currentUnit?.buildingName ?: "",
                        userApartment = currentUnit?.unitName ?: ""
                    )
                }

                val unitId = currentUnit?.unitId
                val buildingId = currentUnit?.buildingId

                if (!unitId.isNullOrEmpty()) {
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

                    launch {
                        try {
                            val payments = paymentRepository.getPayments(unitId)
                            _uiState.update {
                                it.copy(
                                    recentPayments = payments.take(5),
                                    isLoading = false
                                )
                            }
                        } catch (e: Exception) {
                            _uiState.update { it.copy(isLoading = false, error = e.message?.let { UiText.DynamicString(it) }) }
                        }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }

                if (!buildingId.isNullOrEmpty()) {
                    launch {
                        pettyCashRepository.getBalance(buildingId)
                            .onSuccess { balance ->
                                _uiState.update { it.copy(pettyCashBalance = balance) }
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
    val pettyCashBalance: PettyCashBalanceDto? = null,
    val error: UiText? = null
)
