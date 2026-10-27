package com.example.condominio.ui.screens.profile

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

class ChangePasswordViewModel (
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState = _uiState.asStateFlow()

    fun onCurrentPasswordChange(password: String) {
        _uiState.update { it.copy(currentPassword = password, currentPasswordError = false, error = null) }
    }

    fun onNewPasswordChange(password: String) {
        _uiState.update { it.copy(newPassword = password, newPasswordError = false, error = null) }
    }

    fun onConfirmPasswordChange(password: String) {
        _uiState.update { it.copy(confirmPassword = password, confirmPasswordError = false, error = null) }
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

    /** The screen confirmed the change (one-shot flag). */
    fun onSuccessShown() {
        _uiState.update { it.copy(isSuccess = false) }
    }

    fun onChangePasswordClick() {
        val state = _uiState.value
        if (state.isLoading) return

        // Validation
        if (state.currentPassword.isBlank()) {
            _uiState.update { it.copy(currentPasswordError = true, error = UiText.StringResource(Res.string.error_password_required)) }
            return
        }

        if (state.newPassword.length < 6) {
            _uiState.update { it.copy(newPasswordError = true, error = UiText.StringResource(Res.string.password_min_length)) }
            return
        }

        if (state.newPassword != state.confirmPassword) {
            _uiState.update { it.copy(confirmPasswordError = true, error = UiText.StringResource(Res.string.error_passwords_dont_match)) }
            return
        }

        if (state.currentPassword == state.newPassword) {
            _uiState.update { it.copy(newPasswordError = true, error = UiText.StringResource(Res.string.error_new_password_same)) }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.changePassword(
                currentPassword = state.currentPassword,
                newPassword = state.newPassword
            )

            _uiState.update {
                if (result.isSuccess) {
                    // Nothing typed stays in memory once the password is changed.
                    it.copy(isLoading = false, isSuccess = true, currentPassword = "", newPassword = "", confirmPassword = "")
                } else {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.let { e -> authErrorText(e, AuthAction.ChangePassword) }
                    )
                }
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
    /** One-shot: the change just succeeded; the screen confirms it and calls `onSuccessShown`. */
    val isSuccess: Boolean = false,
    val currentPasswordError: Boolean = false,
    val newPasswordError: Boolean = false,
    val confirmPasswordError: Boolean = false,
    val error: UiText? = null
) {
    /** Something typed that leaving would lose. */
    val hasChanges: Boolean get() = currentPassword.isNotEmpty() || newPassword.isNotEmpty() || confirmPassword.isNotEmpty()
}
