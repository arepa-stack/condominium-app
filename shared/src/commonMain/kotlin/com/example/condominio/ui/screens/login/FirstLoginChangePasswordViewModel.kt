package com.example.condominio.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.repository.AuthRepository
import com.example.condominio.ui.utils.AuthAction
import com.example.condominio.ui.utils.UiText
import com.example.condominio.ui.utils.authErrorText
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
        _uiState.update { it.copy(newPassword = value, newPasswordError = false, error = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, confirmPasswordError = false, error = null) }
    }

    fun toggleNewPasswordVisibility() {
        _uiState.update { it.copy(newPasswordVisible = !it.newPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(confirmPasswordVisible = !it.confirmPasswordVisible) }
    }

    /** The screen has no back stack to leave through: let the user drop the temporary session (A-14). */
    fun onLogoutClick() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { it.copy(loggedOut = true) }
        }
    }

    fun onSubmitClick() {
        val state = _uiState.value
        if (state.isLoading) return
        val pw = state.newPassword

        // Backend policy: >= 8 chars, at least 1 uppercase, at least 1 digit.
        if (pw.length < 8 || pw.none { it.isUpperCase() } || pw.none { it.isDigit() }) {
            _uiState.update {
                it.copy(newPasswordError = true, error = UiText.StringResource(Res.string.first_login_password_policy))
            }
            return
        }
        if (pw != state.confirmPassword) {
            _uiState.update {
                it.copy(confirmPasswordError = true, error = UiText.StringResource(Res.string.error_passwords_dont_match))
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.changePasswordFirstLogin(pw)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isSuccess = result.isSuccess,
                    error = result.exceptionOrNull()?.let { e -> authErrorText(e, AuthAction.ChangePassword) }
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
    val newPasswordError: Boolean = false,
    val confirmPasswordError: Boolean = false,
    val loggedOut: Boolean = false,
    val error: UiText? = null
)
