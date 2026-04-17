package com.example.condominio.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.example.condominio.ui.utils.UiText
import condominio.shared.generated.resources.*

class ChangePasswordViewModel (
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState = _uiState.asStateFlow()

    fun onCurrentPasswordChange(password: String) {
        _uiState.update { it.copy(currentPassword = password, error = null) }
    }

    fun onNewPasswordChange(password: String) {
        _uiState.update { it.copy(newPassword = password, error = null) }
    }

    fun onConfirmPasswordChange(password: String) {
        _uiState.update { it.copy(confirmPassword = password, error = null) }
    }

    fun toggleCurrentPasswordVisibility() {
        _uiState.update { it.copy(currentPasswordVisible = !it.currentPasswordVisible) }
    }

    fun toggleNewPasswordVisibility() {
        _uiState.update { it.copy(newPasswordVisible = !it.newPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(confirmPasswordVisible = !it.confirmPasswordVisible) }
    }

    fun onChangePasswordClick() {
        val state = _uiState.value

        // Validation
        if (state.currentPassword.isBlank()) {
            _uiState.update { it.copy(error = UiText.StringResource(Res.string.error_password_required)) }
            return
        }

        if (state.newPassword.length < 6) {
            _uiState.update { it.copy(error = UiText.StringResource(Res.string.password_min_length)) }
            return
        }

        if (state.newPassword != state.confirmPassword) {
            _uiState.update { it.copy(error = UiText.StringResource(Res.string.error_passwords_dont_match)) }
            return
        }

        if (state.currentPassword == state.newPassword) {
            _uiState.update { it.copy(error = UiText.StringResource(Res.string.error_new_password_same)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = authRepository.changePassword(
                currentPassword = state.currentPassword,
                newPassword = state.newPassword
            )

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

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val currentPasswordVisible: Boolean = false,
    val newPasswordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: UiText? = null
)
