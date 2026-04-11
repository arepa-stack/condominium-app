package com.example.condominio.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.model.BuildingRole
import com.example.condominio.data.model.UserUnit
import com.example.condominio.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

open class UnitSelectionViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnitSelectionUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadUnits()
    }

    private fun loadUnits() {
        viewModelScope.launch {
            authRepository.fetchCurrentUser().onSuccess { user ->
                val buildingGroups = user.units
                    .groupBy { it.buildingId }
                    .map { (buildingId, units) ->
                        val role = user.buildingRoles
                            .find { it.buildingId == buildingId }
                            ?.role ?: "resident"
                        val buildingName = units.first().buildingName
                        BuildingGroup(
                            buildingId = buildingId,
                            buildingName = buildingName,
                            role = role,
                            units = units
                        )
                    }

                _uiState.update {
                    it.copy(
                        buildings = buildingGroups,
                        isLoading = false,
                        userName = user.name
                    )
                }
            }.onFailure {
                _uiState.update { state -> state.copy(isLoading = false) }
            }
        }
    }

    fun onBuildingSelected(group: BuildingGroup) {
        if (group.units.size == 1) {
            authRepository.setCurrentUnit(group.units.first())
            _uiState.update { it.copy(unitSelected = true) }
        } else {
            _uiState.update { it.copy(expandedBuildingId = group.buildingId) }
        }
    }

    fun onUnitSelected(unit: UserUnit) {
        authRepository.setCurrentUnit(unit)
        _uiState.update { it.copy(unitSelected = true) }
    }
}

data class BuildingGroup(
    val buildingId: String,
    val buildingName: String,
    val role: String,
    val units: List<UserUnit>
)

data class UnitSelectionUiState(
    val buildings: List<BuildingGroup> = emptyList(),
    val isLoading: Boolean = true,
    val userName: String = "",
    val unitSelected: Boolean = false,
    val expandedBuildingId: String? = null
)
