package com.example.condominio.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.model.User
import com.example.condominio.data.repository.AuthRepository
import com.example.condominio.ui.utils.AuthAction
import com.example.condominio.ui.utils.UiText
import com.example.condominio.ui.utils.authErrorText
import condominio.shared.generated.resources.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditProfileViewModel (
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            val user = authRepository.currentUser.firstOrNull()
            user?.let {
                _uiState.update { state ->
                    state.copy(
                        userId = it.id,
                        name = it.name,
                        originalName = it.name,
                        apartmentUnit = it.apartmentUnit,
                        email = it.email,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, nameError = false, error = null) }
    }

    /** The screen confirmed the save (one-shot flag). */
    fun onSavedShown() {
        _uiState.update { it.copy(saved = false) }
    }

    fun onSaveClick() {
        val state = _uiState.value
        if (state.isLoading) return

        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = true, error = UiText.StringResource(Res.string.error_fill_all_fields)) }
            return
        }
        if (!state.hasChanges) return

        val newName = state.name.trim()
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            // The unit is read-only here: the API's PATCH users/me never takes it (A-10).
            val result = authRepository.updateUser(User(id = state.userId, name = newName, email = state.email))
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(isLoading = false, name = newName, originalName = newName, saved = true)
                } else {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.let { e -> authErrorText(e, AuthAction.UpdateProfile) }
                    )
                }
            }
        }
    }
}

data class EditProfileUiState(
    val userId: String = "",
    val name: String = "",
    /** The name the server has: what [hasChanges] compares against. */
    val originalName: String = "",
    val apartmentUnit: String = "",
    val email: String = "",
    val isLoading: Boolean = true,
    /** One-shot: the save just succeeded; the screen confirms it and calls `onSavedShown`. */
    val saved: Boolean = false,
    val nameError: Boolean = false,
    val error: UiText? = null
) {
    val hasChanges: Boolean get() = name.trim() != originalName.trim()
}
