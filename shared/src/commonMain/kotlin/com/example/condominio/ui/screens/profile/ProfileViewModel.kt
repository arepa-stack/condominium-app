package com.example.condominio.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.model.User
import com.example.condominio.data.repository.AuthRepository
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

    private fun loadUserData() {
        viewModelScope.launch {
            // Fetch latest user data to trigger enrichment
            authRepository.fetchCurrentUser()
            
            authRepository.currentUser.collect { user ->
                _uiState.update { 
                    it.copy(
                        user = user,
                        isLoading = false
                    ) 
                }
            }
        }
    }

    fun onLogoutClick() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { it.copy(isLoggedOut = true) }
        }
    }

    fun onDeleteAccountConfirm(reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, deleteError = null) }
            val result = authRepository.deleteAccount(reason.ifBlank { null })
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(isDeleting = false, isLoggedOut = true)
                } else {
                    it.copy(isDeleting = false, deleteError = result.exceptionOrNull()?.message ?: "Error")
                }
            }
        }
    }
}

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = true,
    val isLoggedOut: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteError: String? = null
)
