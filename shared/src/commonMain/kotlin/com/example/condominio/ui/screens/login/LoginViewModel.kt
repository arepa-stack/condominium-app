package com.example.condominio.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

open class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun onLoginClick() {
        if (_uiState.value.email.isBlank() || _uiState.value.password.isBlank()) {
            _uiState.update { it.copy(error = "Please fill in all fields") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.login(_uiState.value.email, _uiState.value.password)
            _uiState.update { it.copy(isLoading = false) }
            
            result.onSuccess { user ->
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
                // Check if it's a pending exception (we'll need to move UserPendingException to common too)
                _uiState.update { it.copy(error = error.message) }
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
    val error: String? = null,
    val isSuccess: Boolean = false,
    val isPending: Boolean = false,
    val isAdminBlocked: Boolean = false,
    val databaseCleared: Boolean = false,
    val hasMultipleUnits: Boolean = false
)
