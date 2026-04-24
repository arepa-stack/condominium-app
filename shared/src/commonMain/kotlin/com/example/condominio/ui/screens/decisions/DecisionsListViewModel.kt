package com.example.condominio.ui.screens.decisions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.model.DecisionDto
import com.example.condominio.data.model.DecisionStatus
import com.example.condominio.data.repository.AuthRepository
import com.example.condominio.data.repository.DecisionsRepository
import com.example.condominio.ui.utils.UiText
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
                    _uiState.update { it.copy(buildingId = bId) }
                    if (bId != null) refresh()
                }
            }
        }
    }

    fun refresh() {
        val bId = _uiState.value.buildingId ?: return
        val status = _uiState.value.statusFilter
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = decisionsRepository.list(buildingId = bId, status = status)
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    decisions = result.getOrNull()?.items ?: emptyList(),
                    error = result.exceptionOrNull()?.message?.let { UiText.DynamicString(it) }
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
