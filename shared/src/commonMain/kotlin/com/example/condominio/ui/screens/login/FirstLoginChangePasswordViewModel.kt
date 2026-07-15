package com.example.condominio.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.repository.AuthRepository
import com.example.condominio.ui.utils.UiText
import condominio.shared.generated.resources.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FirstLoginChangePasswordViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FirstLoginUiState())
    val uiState = _uiState.asStateFlow()

    fun onNewPasswordChange(value: String) {
        _uiState.update { it.copy(newPassword = value, error = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, error = null) }
    }

    fun toggleNewPasswordVisibility() {
        _uiState.update { it.copy(newPasswordVisible = !it.newPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(confirmPasswordVisible = !it.confirmPasswordVisible) }
    }

    fun onSubmitClick() {
        val state = _uiState.value
        val pw = state.newPassword

        // Backend policy: >= 8 chars, at least 1 uppercase, at least 1 digit.
        if (pw.length < 8 || pw.none { it.isUpperCase() } || pw.none { it.isDigit() }) {
            _uiState.update { it.copy(error = UiText.StringResource(Res.string.first_login_password_policy)) }
            return
        }
        if (pw != state.confirmPassword) {
            _uiState.update { it.copy(error = UiText.StringResource(Res.string.error_passwords_dont_match)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.changePasswordFirstLogin(pw)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isSuccess = result.isSuccess,
                    error = result.exceptionOrNull()?.message?.let { msg -> UiText.DynamicString(msg) }
                )
            }
        }
    }
}

data class FirstLoginUiState(
    val newPassword: String = "",
    val confirmPassword: String = "",
    val newPasswordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: UiText? = null
)
