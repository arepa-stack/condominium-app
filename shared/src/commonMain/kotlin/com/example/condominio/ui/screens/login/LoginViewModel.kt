package com.example.condominio.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.model.SessionExpiredException
import com.example.condominio.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.condominio.ui.utils.AuthAction
import com.example.condominio.ui.utils.UiText
import com.example.condominio.ui.utils.authErrorText
import condominio.shared.generated.resources.*

open class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    init {
        checkExistingSession()
    }

    private fun checkExistingSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingSession = true, sessionCheckFailed = false) }
            val hasSession = authRepository.hasValidSession()
            if (hasSession) {
                // Fetch user data to populate currentUser flow
                val result = authRepository.fetchCurrentUser()
                if (result.isFailure) {
                    // Only a 401 (the repository already dropped the token) means "log in again".
                    // Any other failure (offline, server down) keeps the session: offer to retry (A-17).
                    val expired = result.exceptionOrNull() is SessionExpiredException
                    _uiState.update { it.copy(isCheckingSession = false, sessionCheckFailed = !expired) }
                    return@launch
                }
                val mustChange = result.getOrNull()?.mustChangePassword == true
                _uiState.update {
                    it.copy(
                        isCheckingSession = false,
                        mustChangePassword = mustChange,
                        isSuccess = !mustChange
                    )
                }
            } else {
                _uiState.update { it.copy(isCheckingSession = false) }
            }
        }
    }

    fun onRetrySessionCheck() {
        if (_uiState.value.isCheckingSession) return
        checkExistingSession()
    }

    /** From the offline screen: give up the saved session and show the login form. */
    fun onUseAnotherAccount() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { it.copy(isCheckingSession = false, sessionCheckFailed = false) }
        }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, error = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }

    fun onLoginClick() {
        // Enter held down / double tap: one request at a time (E-L2).
        if (_uiState.value.isLoading) return
        val email = _uiState.value.email.trim()
        if (email.isBlank() || _uiState.value.password.isBlank()) {
            _uiState.update { it.copy(error = UiText.StringResource(Res.string.error_missing_fields)) }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.login(email, _uiState.value.password)
            _uiState.update { it.copy(isLoading = false) }
            
            result.onSuccess { user ->
                if (user.mustChangePassword) {
                    _uiState.update { it.copy(mustChangePassword = true) }
                    return@onSuccess
                }

                if (user.isAdmin) {
                    authRepository.logout()
                    _uiState.update { it.copy(isAdminBlocked = true) }
                    return@onSuccess
                }

                val distinctBuildings = user.units.map { it.buildingId }.distinct()
                val needsSelection = distinctBuildings.size > 1 || user.units.size > 1
                if (!needsSelection && user.units.isNotEmpty()) {
                    authRepository.setCurrentUnit(user.units.first())
                }

                _uiState.update { it.copy(isSuccess = true, hasMultipleUnits = needsSelection) }
            }.onFailure { error ->
                // Never the repository's own (English, technical) message (A-24).
                _uiState.update { it.copy(error = authErrorText(error, AuthAction.Login)) }
            }
        }
    }
    
    fun onClearDatabaseClick() {
        viewModelScope.launch {
            // database.clearAllData() // Database needs to be moved to common too
            _uiState.update { it.copy(databaseCleared = true) }
            delay(2000)
            _uiState.update { it.copy(databaseCleared = false) }
        }
    }
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isCheckingSession: Boolean = true,
    val sessionCheckFailed: Boolean = false,
    val error: UiText? = null,
    val isSuccess: Boolean = false,
    val isPending: Boolean = false,
    val isAdminBlocked: Boolean = false,
    val databaseCleared: Boolean = false,
    val hasMultipleUnits: Boolean = false,
    val mustChangePassword: Boolean = false
)
