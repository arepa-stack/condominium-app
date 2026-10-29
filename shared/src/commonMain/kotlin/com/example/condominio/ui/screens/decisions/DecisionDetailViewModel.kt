package com.example.condominio.ui.screens.decisions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.model.DecisionDetailDto
import com.example.condominio.data.repository.AuthRepository
import com.example.condominio.data.repository.DecisionsRepository
import com.example.condominio.ui.utils.UiText
import condominio.shared.generated.resources.Res
import condominio.shared.generated.resources.decisions_no_active_unit
import condominio.shared.generated.resources.error_loading_decision
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DecisionDetailUiState(
    val detail: DecisionDetailDto? = null,
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val showUploadSheet: Boolean = false,
    val showVoteSheet: Boolean = false,
    val isSubmitting: Boolean = false,
    val submissionError: UiText? = null,
    val currentUnitId: String? = null,
    val currentUserId: String? = null
)

class DecisionDetailViewModel(
    private val decisionsRepository: DecisionsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DecisionDetailUiState())
    val uiState = _uiState.asStateFlow()

    private var decisionId: String? = null

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                if (user != null) {
                    _uiState.update {
                        it.copy(
                            currentUnitId = user.currentUnit?.unitId,
                            currentUserId = user.id
                        )
                    }
                }
            }
        }
    }

    fun loadDetail(id: String) {
        decisionId = id
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = decisionsRepository.detail(id)
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    detail = result.getOrNull() ?: state.detail,
                    error = result.exceptionOrNull()?.let { UiText.StringResource(Res.string.error_loading_decision) }
                )
            }
        }
    }

    fun refresh() {
        decisionId?.let { loadDetail(it) }
    }

    fun openUploadSheet() {
        _uiState.update { it.copy(showUploadSheet = true, submissionError = null) }
    }

    fun closeUploadSheet() {
        if (_uiState.value.isSubmitting) return
        _uiState.update { it.copy(showUploadSheet = false, submissionError = null) }
    }

    fun submitQuote(
        providerName: String,
        amount: Double,
        notes: String?,
        fileUri: String,
        mimeType: String
    ) {
        val id = decisionId ?: return
        if (_uiState.value.isSubmitting) return
        val unitId = _uiState.value.currentUnitId ?: run {
            _uiState.update { it.copy(submissionError = UiText.StringResource(Res.string.decisions_no_active_unit)) }
            return
        }
        _uiState.update { it.copy(isSubmitting = true, submissionError = null) }
        viewModelScope.launch {
            val result = decisionsRepository.uploadQuote(
                decisionId = id,
                unitId = unitId,
                providerName = providerName,
                amount = amount,
                notes = notes,
                fileUri = fileUri,
                mimeType = mimeType
            )
            if (result.isSuccess) {
                _uiState.update { it.copy(isSubmitting = false, showUploadSheet = false) }
                loadDetail(id)
            } else {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        submissionError = UiText.DynamicString(result.exceptionOrNull()?.message ?: "")
                    )
                }
            }
        }
    }

    fun openVoteSheet() {
        _uiState.update { it.copy(showVoteSheet = true, submissionError = null) }
    }

    fun closeVoteSheet() {
        if (_uiState.value.isSubmitting) return
        _uiState.update { it.copy(showVoteSheet = false, submissionError = null) }
    }

    fun submitVote(quoteId: String) {
        val id = decisionId ?: return
        if (_uiState.value.isSubmitting) return
        val unitId = _uiState.value.currentUnitId ?: run {
            _uiState.update { it.copy(submissionError = UiText.StringResource(Res.string.decisions_no_active_unit)) }
            return
        }
        _uiState.update { it.copy(isSubmitting = true, submissionError = null) }
        viewModelScope.launch {
            val result = decisionsRepository.vote(
                decisionId = id,
                unitId = unitId,
                quoteId = quoteId
            )
            if (result.isSuccess) {
                _uiState.update { it.copy(isSubmitting = false, showVoteSheet = false) }
                loadDetail(id)
            } else {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        submissionError = UiText.DynamicString(result.exceptionOrNull()?.message ?: "")
                    )
                }
            }
        }
    }

    fun deleteOwnQuote(quoteId: String) {
        val id = decisionId ?: return
        _uiState.update { it.copy(isSubmitting = true, submissionError = null) }
        viewModelScope.launch {
            val result = decisionsRepository.deleteQuote(id, quoteId)
            if (result.isSuccess) {
                _uiState.update { it.copy(isSubmitting = false) }
                loadDetail(id)
            } else {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        submissionError = UiText.DynamicString(result.exceptionOrNull()?.message ?: "")
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null, submissionError = null) }
    }
}
