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

class ForgotPasswordViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, error = null) }
    }

    fun onSubmitClick() {
        val email = _uiState.value.email.trim()
        if (email.isBlank() || !email.contains("@")) {
            _uiState.update { it.copy(error = UiText.StringResource(Res.string.error_invalid_email)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.resetPassword(email)
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

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: UiText? = null
)
