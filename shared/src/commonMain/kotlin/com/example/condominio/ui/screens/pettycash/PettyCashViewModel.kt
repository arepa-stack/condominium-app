package com.example.condominio.ui.screens.pettycash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.model.*
import com.example.condominio.data.repository.AuthRepository
import com.example.condominio.data.repository.PettyCashRepository
import com.example.condominio.ui.utils.UiText
import condominio.shared.generated.resources.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PettyCashUiState(
        val balance: PettyCashBalanceDto? = null,
        val history: List<PettyCashTransactionDto> = emptyList(),
        val isLoading: Boolean = false,
        val error: UiText? = null,
        val canManage: Boolean = false,
        val buildingId: String? = null,
        val isRefreshing: Boolean = false,
        val isSubmitting: Boolean = false
)

class PettyCashViewModel
(
        private val pettyCashRepository: PettyCashRepository,
        private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PettyCashUiState())
    val uiState = _uiState.asStateFlow()

    private var currentPage = 1
    private var isLastPage = false

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                if (user != null) {
                    // Fix: If buildingId (from currentUnit) is empty, try to get it from the units
                    // list
                    // This is common for admins who might not have a specific 'current unit' but
                    // belong to a building
                    var bId = user.buildingId
                    if (bId.isEmpty() && user.units.isNotEmpty()) {
                        bId = user.units.first().buildingId
                    }

                    val canManage = user.isAdmin || (bId.isNotEmpty() && user.hasBoardRoleIn(bId))
                    _uiState.update { it.copy(buildingId = bId, canManage = canManage) }

                    if (!bId.isNullOrEmpty()) {
                        refreshData()
                    }
                }
            }
        }
    }

    fun refreshData() {
        val bId = _uiState.value.buildingId ?: return
        if (bId.isEmpty()) return

        currentPage = 1
        isLastPage = false

        _uiState.update { it.copy(isRefreshing = true, error = null) }

        viewModelScope.launch {
            val balanceResult = pettyCashRepository.getBalance(bId)
            val historyResult = pettyCashRepository.getHistory(bId, page = 1)

            _uiState.update { state ->
                state.copy(
                        isRefreshing = false,
                        balance = balanceResult.getOrNull(),
                        history = historyResult.getOrDefault(emptyList()),
                        error =
                                if (balanceResult.isFailure)
                                        UiText.DynamicString(balanceResult.exceptionOrNull()?.message ?: "")
                                else null
                )
            }
        }
    }

    fun loadNextPage() {
        val bId = _uiState.value.buildingId ?: return
        if (bId.isEmpty() || _uiState.value.isLoading || isLastPage) return

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val nextPage = currentPage + 1
            val result = pettyCashRepository.getHistory(bId, page = nextPage)

            result
                    .onSuccess { newItems ->
                        if (newItems.isEmpty()) {
                            isLastPage = true
                        } else {
                            currentPage = nextPage
                            _uiState.update { state ->
                                state.copy(history = state.history + newItems)
                            }
                        }
                    }
                    .onFailure { e -> _uiState.update { it.copy(error = UiText.DynamicString(e.message ?: "")) } }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun registerIncome(amount: Double, description: String, onSuccess: () -> Unit) {
        val bId = _uiState.value.buildingId
        if (bId.isNullOrEmpty()) {
            _uiState.update { it.copy(error = UiText.StringResource(Res.string.error_no_building_found)) }
            return
        }

        _uiState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            println("Debug: `Registering income for building: $bId")
            val result = pettyCashRepository.registerIncome(bId, amount, description)
            if (result.isSuccess) {
                refreshData()
                onSuccess()
                _uiState.update { it.copy(isSubmitting = false) }
            } else {
                val errorText = result.exceptionOrNull()?.message ?: ""
                println("Error: `Register income failed: $errorText")
                _uiState.update { it.copy(isSubmitting = false, error = UiText.DynamicString(errorText)) }
            }
        }
    }

    fun registerExpense(
            amount: Double,
            description: String,
            category: PettyCashCategory,
            evidencePath: String?,
            onSuccess: () -> Unit
    ) {
        val bId = _uiState.value.buildingId
        if (bId.isNullOrEmpty()) {
            _uiState.update { it.copy(error = UiText.StringResource(Res.string.error_no_building_found)) }
            return
        }

        // Validation: amount <= balance (as requested)
        // Validation: amount > balance warning is handled in UI, here we allow it.
        val currentBalance = _uiState.value.balance?.currentBalance ?: 0.0
        // We no longer block if amount > currentBalance, relying on backend to handle the overage
        // logic

        _uiState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            println("Debug: `Registering expense for building: $bId")
            val result =
                    pettyCashRepository.registerExpense(
                            bId,
                            amount,
                            description,
                            category,
                            evidencePath
                    )
            if (result.isSuccess) {
                refreshData()
                onSuccess()
                _uiState.update { it.copy(isSubmitting = false) }
            } else {
                val errorText = result.exceptionOrNull()?.message ?: ""
                println("Error: `Register expense failed: $errorText")
                _uiState.update { it.copy(isSubmitting = false, error = UiText.DynamicString(errorText)) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
