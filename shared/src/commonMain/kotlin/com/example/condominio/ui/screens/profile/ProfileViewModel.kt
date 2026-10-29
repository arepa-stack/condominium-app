package com.example.condominio.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.model.User
import com.example.condominio.data.repository.AuthRepository
import com.example.condominio.data.remote.rethrowIfCancellation
import com.example.condominio.ui.utils.AuthAction
import com.example.condominio.ui.utils.UiText
import com.example.condominio.ui.utils.authErrorText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel (
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadUserData()
    }

    private var loadJob: Job? = null

    private fun loadUserData() {
        loadJob = viewModelScope.launch {
            // Fetch latest user data to trigger enrichment
            val fetchFailed = try {
                authRepository.fetchCurrentUser().isFailure
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                true
            }
            authRepository.currentUser.collect { user ->
                _uiState.update {
                    // A cached user stays on screen even if the refresh failed; the error only replaces "nothing".
                    val shouldMarkFailed = fetchFailed && user == null && !it.isLoggingOut && !it.isDeleting && !it.isLoggedOut
                    it.copy(user = user, isLoading = false, loadFailed = shouldMarkFailed)
                }
            }
        }
    }

    /** Ignored while a logout is already running: a double tap must not send it twice (E-R1). */
    fun onLogoutClick() {
        if (_uiState.value.isLoggingOut) return
        _uiState.update { it.copy(isLoggingOut = true) }
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { it.copy(isLoggedOut = true) }
        }
    }

    fun onDeleteAccountConfirm(reason: String) {
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(isDeleting = true, deleteError = null) }
        viewModelScope.launch {
            val result = authRepository.deleteAccount(reason.ifBlank { null })
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(isDeleting = false, isLoggedOut = true)
                } else {
                    it.copy(
                        isDeleting = false,
                        deleteError = authErrorText(result.exceptionOrNull() ?: Exception(), AuthAction.DeleteAccount)
                    )
                }
            }
        }
    }

    /** Retry from the load error screen; ignored while loading. */
    fun retry() {
        if (_uiState.value.isLoading) return
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        loadUserData()
    }
}

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = true,
    val isLoggingOut: Boolean = false,
    val isLoggedOut: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteError: UiText? = null,
    /** No user to show and loading it failed: the screen shows ErrorState with Retry. */
    val loadFailed: Boolean = false
)
