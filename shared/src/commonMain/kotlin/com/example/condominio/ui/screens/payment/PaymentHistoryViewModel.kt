package com.example.condominio.ui.screens.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.model.Payment
import com.example.condominio.data.repository.PaymentRepository
import kotlinx.coroutines.Job
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

    // Declared before `init` so its loadPayments() call doesn't get these reset afterwards.
    /** Current load; a new one cancels it so two loads never overlap or land out of order (A-19). */
    private var loadJob: Job? = null

    /** The user went forward to a child screen; set by [onOpenChild], consumed by the next [onScreenReturn] (A-19). */
    private var childOpened = false

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

    /** The screen is about to navigate forward (payment, action, profile...): coming back must reload. */
    fun onOpenChild() {
        childOpened = true
    }

    /**
     * Screen entered composition again ([com.example.condominio.ui.utils.OnScreenReturn]). Reloads only coming
     * back from a child screen; opening and rotation/recreation (the ViewModel survives) don't, `init` loaded.
     */
    fun onScreenReturn() {
        if (!childOpened) return
        childOpened = false
        reloadIfIdle()
    }

    /** App back from background: reload. */
    fun onBackgroundReturn() {
        reloadIfIdle()
    }

    private fun reloadIfIdle() {
        // A load already running is as fresh as a new one: don't cancel and restart it on a duplicate signal.
        if (loadJob?.isActive == true) return
        loadPayments()
    }

    /** Explicit reload (retry/refresh): always loads, cancelling any load in flight. */
    fun loadPayments() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
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
