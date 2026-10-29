package com.example.condominio.ui.screens.decisions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.model.DecisionDto
import com.example.condominio.data.model.DecisionStatus
import com.example.condominio.data.repository.AuthRepository
import com.example.condominio.data.repository.DecisionsRepository
import com.example.condominio.data.remote.rethrowIfCancellation
import com.example.condominio.ui.utils.UiText
import condominio.shared.generated.resources.Res
import condominio.shared.generated.resources.error_loading_decisions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DecisionsListUiState(
    val decisions: List<DecisionDto> = emptyList(),
    val statusFilter: DecisionStatus? = null,
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val buildingId: String? = null
)

class DecisionsListViewModel(
    private val decisionsRepository: DecisionsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DecisionsListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                if (user != null) {
                    val bId = user.buildingId.takeIf { it.isNotEmpty() }
                    val isNewBuilding = bId != null && bId != _uiState.value.buildingId
                    _uiState.update { it.copy(buildingId = bId) }
                    if (isNewBuilding) refresh()
                } else {
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        if (_uiState.value.isLoading) return
        val status = _uiState.value.statusFilter
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val bId = _uiState.value.buildingId ?: run {
                val fetchResult = try {
                    authRepository.fetchCurrentUser()
                } catch (e: Throwable) {
                    e.rethrowIfCancellation()
                    Result.failure(e)
                }
                val user = fetchResult.getOrNull()
                if (user == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = UiText.StringResource(Res.string.error_loading_decisions)
                        )
                    }
                    return@launch
                }
                val fetchedBId = user.buildingId.takeIf { it.isNotEmpty() }
                _uiState.update { it.copy(buildingId = fetchedBId) }
                fetchedBId
            }

            if (bId == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            val result = decisionsRepository.list(buildingId = bId, status = status)
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    decisions = result.getOrNull()?.data ?: emptyList(),
                    error = result.exceptionOrNull()?.let { UiText.StringResource(Res.string.error_loading_decisions) }
                )
            }
        }
    }

    fun setStatusFilter(status: DecisionStatus?) {
        if (_uiState.value.statusFilter == status) return
        _uiState.update { it.copy(statusFilter = status) }
        refresh()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
