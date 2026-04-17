package com.example.condominio.ui.screens.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.model.Building
import com.example.condominio.data.model.UnitDto
import com.example.condominio.data.repository.AuthRepository
import com.example.condominio.data.repository.BuildingRepository
import com.example.condominio.ui.utils.UiText
import condominio.shared.generated.resources.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel (
    private val authRepository: AuthRepository,
    private val buildingRepository: BuildingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadBuildings()
    }

    private fun loadBuildings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingBuildings = true) }
            val result = buildingRepository.getBuildings()
            result.onSuccess { buildings ->
                _uiState.update { 
                    it.copy(
                        buildings = buildings,
                        isLoadingBuildings = false
                    ) 
                }
            }.onFailure {
                _uiState.update { 
                    it.copy(
                        isLoadingBuildings = false,
                        error = UiText.StringResource(Res.string.error_failed_load_buildings)
                    ) 
                }
            }
        }
    }

    private fun loadUnits(buildingId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingUnits = true, units = emptyList()) }
            val result = authRepository.getUnits(buildingId)
            result.onSuccess { units ->
                _uiState.update { 
                    it.copy(
                        units = units,
                        isLoadingUnits = false
                    ) 
                }
            }.onFailure {
                _uiState.update { 
                    it.copy(
                        isLoadingUnits = false,
                        error = UiText.StringResource(Res.string.error_failed_load_units)
                    ) 
                }
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    fun onUnitChange(unitId: String, unitName: String) {
        _uiState.update { it.copy(unit = unitName, selectedUnitId = unitId) }
    }

    fun onBuildingChange(buildingId: String) {
        _uiState.update { it.copy(selectedBuildingId = buildingId, unit = "", selectedUnitId = "") }
        if (buildingId.isNotBlank()) {
            loadUnits(buildingId)
        }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun onRegisterClick() {
        if (_uiState.value.email.isBlank() || _uiState.value.password.isBlank() || 
            _uiState.value.name.isBlank() || _uiState.value.unit.isBlank() ||
            _uiState.value.selectedBuildingId.isBlank()) {
            _uiState.update { it.copy(error = UiText.StringResource(Res.string.error_fill_all_fields)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.register(
                _uiState.value.name, 
                _uiState.value.email, 
                _uiState.value.selectedUnitId,
                _uiState.value.selectedBuildingId, // Send building ID
                _uiState.value.password
            )
            _uiState.update { it.copy(isLoading = false) }
            
            result.onSuccess {
                _uiState.update { it.copy(isSuccess = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(error = UiText.DynamicString(error.message ?: "")) }
            }
        }
    }
}

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val unit: String = "", // Display name
    val selectedUnitId: String = "", // UUID
    val selectedBuildingId: String = "",
    val buildings: List<Building> = emptyList(),
    val isLoadingBuildings: Boolean = false,
    val units: List<com.example.condominio.data.model.UnitDto> = emptyList(),
    val isLoadingUnits: Boolean = false,
    val password: String = "",
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val isSuccess: Boolean = false
)
