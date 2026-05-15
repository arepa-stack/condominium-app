package com.example.condominio.ui.screens.billboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.model.AnnouncementCategory
import com.example.condominio.data.model.AnnouncementDto
import com.example.condominio.data.repository.AnnouncementsRepository
import com.example.condominio.data.repository.AuthRepository
import com.example.condominio.ui.utils.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BillboardListUiState(
    val announcements: List<AnnouncementDto> = emptyList(),
    val categoryFilter: AnnouncementCategory? = null,
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val buildingId: String? = null,
    val hasNextPage: Boolean = false,
    val currentPage: Int = 1,
)

class BillboardListViewModel(
    private val announcementsRepository: AnnouncementsRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BillboardListUiState())
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
        val category = _uiState.value.categoryFilter
        _uiState.update { it.copy(isLoading = true, error = null, currentPage = 1) }
        viewModelScope.launch {
            val result = announcementsRepository.list(
                buildingId = bId,
                category = category,
                page = 1,
            )
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    announcements = result.getOrNull()?.data ?: emptyList(),
                    hasNextPage = result.getOrNull()?.metadata?.hasNextPage ?: false,
                    error = result.exceptionOrNull()?.message?.let { UiText.DynamicString(it) },
                )
            }
        }
    }

    fun setCategoryFilter(category: AnnouncementCategory?) {
        if (_uiState.value.categoryFilter == category) return
        _uiState.update { it.copy(categoryFilter = category) }
        refresh()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
