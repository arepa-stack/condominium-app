package com.example.condominio.ui.screens.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.model.User
import com.example.condominio.data.repository.AuthRepository
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
                        apartmentUnit = it.apartmentUnit,
                        email = it.email,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, error = null) }
    }

    fun onApartmentUnitChange(unit: String) {
        _uiState.update { it.copy(apartmentUnit = unit, error = null) }
    }

    fun onSaveClick() {
        val state = _uiState.value

        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Name is required") }
            return
        }

        if (state.apartmentUnit.isBlank()) {
            _uiState.update { it.copy(error = "Apartment unit is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val updatedUser = User(
                id = state.userId,
                name = state.name,
                email = state.email
            )

            val result = authRepository.updateUser(updatedUser)

            _uiState.update { 
                it.copy(
                    isLoading = false,
                    isSuccess = result.isSuccess,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }
}

data class EditProfileUiState(
    val userId: String = "",
    val name: String = "",
    val apartmentUnit: String = "",
    val email: String = "",
    val isLoading: Boolean = true,
    val isSuccess: Boolean = false,
    val error: String? = null
)
