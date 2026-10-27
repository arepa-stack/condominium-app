package com.example.condominio.ui.screens.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.model.UnitDto
import com.example.condominio.data.repository.BuildingRepository
import com.example.condominio.data.repository.OnboardingRepository
import com.example.condominio.ui.utils.AuthAction
import com.example.condominio.ui.utils.UiText
import com.example.condominio.ui.utils.authErrorText
import condominio.shared.generated.resources.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val buildingRepository: BuildingRepository,
    private val onboardingRepository: OnboardingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    fun onBuildingScanned(buildingCode: String) {
        if (_uiState.value.buildingCode == buildingCode && _uiState.value.buildingName.isNotBlank()) return
        _uiState.update { it.copy(buildingCode = buildingCode, isLoadingBuilding = true, error = null) }
        viewModelScope.launch {
            buildingRepository.getBuildingByCode(buildingCode)
                .onSuccess { building ->
                    _uiState.update { it.copy(buildingName = building.name, isLoadingBuilding = false) }
                    loadUnitsByCode(buildingCode)
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoadingBuilding = false,
                            error = UiText.StringResource(Res.string.qr_invalid_code)
                        )
                    }
                }
        }
    }

    private fun loadUnitsByCode(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingUnits = true, units = emptyList()) }
            buildingRepository.getBuildingUnitsByCode(code)
                .onSuccess { units ->
                    _uiState.update { it.copy(units = units, isLoadingUnits = false) }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoadingUnits = false,
                            error = UiText.StringResource(Res.string.error_failed_load_units)
                        )
                    }
                }
        }
    }

    fun onFirstNameChange(value: String) = _uiState.update { it.withEdited(RegisterField.FirstName).copy(firstName = value) }
    fun onLastNameChange(value: String) = _uiState.update { it.withEdited(RegisterField.LastName).copy(lastName = value) }
    fun onEmailChange(value: String) = _uiState.update { it.withEdited(RegisterField.Email).copy(email = value) }
    fun onDocumentIdChange(value: String) = _uiState.update { it.withEdited(RegisterField.DocumentId).copy(documentId = value) }

    fun onUnitChange(unitId: String, unitName: String) {
        _uiState.update { it.withEdited(RegisterField.Unit).copy(selectedUnitId = unitId, unit = unitName) }
    }

    fun onRegisterClick() {
        val state = _uiState.value
        if (state.isLoading) return
        val missing = buildSet {
            if (state.firstName.isBlank()) add(RegisterField.FirstName)
            if (state.lastName.isBlank()) add(RegisterField.LastName)
            if (state.email.isBlank()) add(RegisterField.Email)
            if (state.documentId.isBlank()) add(RegisterField.DocumentId)
            if (state.selectedUnitId.isBlank()) add(RegisterField.Unit)
        }
        if (missing.isNotEmpty() || state.buildingCode.isBlank()) {
            _uiState.update {
                it.copy(missingFields = missing, error = UiText.StringResource(Res.string.error_fill_all_fields))
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            onboardingRepository.submitRegistrationRequest(
                buildingCode = state.buildingCode,
                unitId = state.selectedUnitId,
                email = state.email,
                firstName = state.firstName,
                lastName = state.lastName,
                documentId = state.documentId
            ).onSuccess {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = authErrorText(error, AuthAction.Register)
                    )
                }
            }
        }
    }
}

data class RegisterUiState(
    val buildingCode: String = "",
    val buildingName: String = "",
    val isLoadingBuilding: Boolean = false,
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val documentId: String = "",
    val unit: String = "",
    val selectedUnitId: String = "",
    val units: List<UnitDto> = emptyList(),
    val isLoadingUnits: Boolean = false,
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val isSuccess: Boolean = false,
    val missingFields: Set<RegisterField> = emptySet()
)

enum class RegisterField { FirstName, LastName, Email, DocumentId, Unit }

/** Editing a flagged field clears its flag; the general message goes away once nothing is flagged. */
private fun RegisterUiState.withEdited(field: RegisterField): RegisterUiState {
    val left = missingFields - field
    return copy(missingFields = left, error = if (left.isEmpty() && missingFields.isNotEmpty()) null else error)
}
