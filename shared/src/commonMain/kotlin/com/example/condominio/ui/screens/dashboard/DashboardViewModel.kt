package com.example.condominio.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.model.Payment
import com.example.condominio.data.model.PettyCashBalanceDto
import com.example.condominio.data.model.SolvencyStatus
import com.example.condominio.data.repository.AuthRepository
import com.example.condominio.data.repository.PettyCashRepository
import com.example.condominio.domain.usecase.billboard.GetUnreadAnnouncementsCountUseCase
import com.example.condominio.ui.utils.UiText
import condominio.shared.generated.resources.Res
import condominio.shared.generated.resources.error_loading_dashboard
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val authRepository: AuthRepository,
    private val paymentRepository: com.example.condominio.data.repository.PaymentRepository,
    private val buildingRepository: com.example.condominio.data.repository.BuildingRepository,
    private val pettyCashRepository: PettyCashRepository,
    private val getUnreadAnnouncementsCount: GetUnreadAnnouncementsCountUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()

    /** Current load; a new one cancels it (and its child requests) so a stale response never lands (A-06). */
    private var loadJob: Job? = null

    /** `init` already loads: the first ON_RESUME (right after opening) must not load again (A-06). */
    private var skipNextResume = true

    init {
        loadData()
    }

    private fun loadData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val userResult = authRepository.fetchCurrentUser()

            if (userResult.isSuccess) {
                val user = userResult.getOrNull()
                val currentUnit = user?.currentUnit

                val distinctBuildings = user?.units?.map { it.buildingId }?.distinct() ?: emptyList()
                val multipleUnits = distinctBuildings.size > 1 || (user?.units?.size ?: 0) > 1

                val unitId = currentUnit?.unitId
                val buildingId = currentUnit?.buildingId

                _uiState.update { state ->
                    // Another unit: drop the previous unit's balance/payments instead of showing them meanwhile.
                    val base = if (state.unitId == unitId) state else DashboardUiState(isLoading = true)
                    base.copy(
                        unitId = unitId,
                        userName = user?.name ?: "",
                        userBuilding = currentUnit?.buildingName ?: "",
                        userApartment = currentUnit?.unitName ?: "",
                        hasMultipleUnits = multipleUnits,
                        balanceFailed = false,
                        paymentsFailed = false
                    )
                }

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
                        }.onFailure {
                            // Keep a balance already shown; with none, the card shows the error (A-05)
                            _uiState.update { it.copy(balanceFailed = true) }
                        }
                    }

                    launch {
                        paymentRepository.fetchPayments(unitId)
                            .onSuccess { payments ->
                                _uiState.update { it.copy(recentPayments = payments.take(5), isLoading = false) }
                            }
                            .onFailure {
                                _uiState.update { it.copy(isLoading = false, paymentsFailed = true) }
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
                    launch {
                        val count = getUnreadAnnouncementsCount(buildingId)
                        _uiState.update { it.copy(unreadAnnouncementsCount = count) }
                    }
                }
            } else {
                // Shown full screen only while there is nothing loaded yet (A-07).
                _uiState.update { it.copy(isLoading = false, error = UiText.StringResource(Res.string.error_loading_dashboard)) }
            }
        }
    }

    fun refresh() {
        loadData()
    }

    /** Reload when coming back to the screen (e.g. after a payment), except for the first resume. */
    fun onResume() {
        if (skipNextResume) {
            skipNextResume = false
            return
        }
        loadData()
    }
}

data class DashboardUiState(
    /** Unit the data below belongs to. */
    val unitId: String? = null,
    val userName: String = "",
    val userBuilding: String = "",
    val userApartment: String = "",
    val hasMultipleUnits: Boolean = false,
    val solvencyStatus: SolvencyStatus = SolvencyStatus.PENDING,
    val recentPayments: List<Payment> = emptyList(),
    val isLoading: Boolean = false,
    /** Null until the balance loads: unknown, never "al día" (A-05). */
    val totalDebt: Double? = null,
    val pendingInvoices: List<com.example.condominio.data.model.Invoice> = emptyList(),
    /** Last balance load failed (network/HTTP). */
    val balanceFailed: Boolean = false,
    /** Last payments load failed: not the same as "no transactions". */
    val paymentsFailed: Boolean = false,
    val pettyCashBalance: PettyCashBalanceDto? = null,
    val unreadAnnouncementsCount: Int = 0,
    /** The user/unit could not be loaded. */
    val error: UiText? = null
)
