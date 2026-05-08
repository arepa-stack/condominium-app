package com.example.condominio.ui.screens.billboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.model.AnnouncementDto
import com.example.condominio.data.repository.AnnouncementsRepository
import com.example.condominio.ui.utils.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BillboardDetailUiState(
    val announcement: AnnouncementDto? = null,
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val reacted: Boolean = false,
    val reactionsCount: Int = 0,
    val isTogglingReaction: Boolean = false,
)

class BillboardDetailViewModel(
    private val announcementsRepository: AnnouncementsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BillboardDetailUiState())
    val uiState = _uiState.asStateFlow()

    private var announcementId: String? = null

    fun loadAnnouncement(id: String) {
        announcementId = id
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = announcementsRepository.detail(id)
            _uiState.update { state ->
                val announcement = result.getOrNull()
                state.copy(
                    isLoading = false,
                    announcement = announcement ?: state.announcement,
                    reacted = announcement?.reactedByCurrentUser ?: state.reacted,
                    reactionsCount = announcement?.metrics?.reactionsCount ?: state.reactionsCount,
                    error = result.exceptionOrNull()?.message?.let { UiText.DynamicString(it) },
                )
            }
            // Fire-and-forget: mark as read after loading detail
            if (result.isSuccess) {
                announcementsRepository.markRead(id)
            }
        }
    }

    fun toggleReaction() {
        val id = announcementId ?: return
        if (_uiState.value.isTogglingReaction) return

        // Optimistic update
        val currentReacted = _uiState.value.reacted
        val currentCount = _uiState.value.reactionsCount
        _uiState.update {
            it.copy(
                isTogglingReaction = true,
                reacted = !currentReacted,
                reactionsCount = if (currentReacted) currentCount - 1 else currentCount + 1,
            )
        }

        viewModelScope.launch {
            val result = announcementsRepository.toggleReaction(id)
            if (result.isSuccess) {
                val response = result.getOrNull()
                _uiState.update { state ->
                    state.copy(
                        isTogglingReaction = false,
                        reacted = response?.reacted ?: state.reacted,
                    )
                }
            } else {
                // Rollback on failure
                _uiState.update {
                    it.copy(
                        isTogglingReaction = false,
                        reacted = currentReacted,
                        reactionsCount = currentCount,
                        error = UiText.DynamicString(
                            result.exceptionOrNull()?.message ?: "Error al registrar reacción"
                        ),
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
