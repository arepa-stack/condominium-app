package com.example.condominio.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.model.User
import com.example.condominio.data.model.UserUnit
import com.example.condominio.data.remote.rethrowIfCancellation
import com.example.condominio.data.repository.AuthRepository
import com.example.condominio.ui.utils.UiText
import condominio.shared.generated.resources.Res
import condominio.shared.generated.resources.error_loading_units
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

open class UnitSelectionViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnitSelectionUiState())
    val uiState = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadUnits()
    }

    /** Retry after a failed load (A-07). Ignored while a load is already running. */
    fun retry() {
        if (loadJob?.isActive == true) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadUnits()
    }

    private fun loadUnits() {
        loadJob = viewModelScope.launch {
            val result = try {
                authRepository.fetchCurrentUser()
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                Result.failure(e)
            }
            result.onSuccess { user ->
                _uiState.update {
                    it.copy(
                        buildings = user.toBuildingGroups(),
                        isLoading = false,
                        error = null,
                        userName = user.name
                    )
                }
            }.onFailure {
                _uiState.update { state ->
                    state.copy(isLoading = false, error = UiText.StringResource(Res.string.error_loading_units))
                }
            }
        }
    }

    private fun User.toBuildingGroups(): List<BuildingGroup> = units
        .groupBy { it.buildingId }
        .map { (buildingId, units) ->
            BuildingGroup(
                buildingId = buildingId,
                buildingName = units.first().buildingName,
                role = buildingRoles.find { it.buildingId == buildingId }?.role ?: "resident",
                units = units
            )
        }

    fun onBuildingSelected(group: BuildingGroup) {
        if (group.units.size == 1) {
            authRepository.setCurrentUnit(group.units.first())
            _uiState.update { it.copy(unitSelected = true) }
        } else {
            // Tapping the expanded building again collapses it (E-U2).
            _uiState.update {
                it.copy(expandedBuildingId = if (it.expandedBuildingId == group.buildingId) null else group.buildingId)
            }
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
    val expandedBuildingId: String? = null,
    val error: UiText? = null
) {
    /** Loaded fine but the user has no units: a message, not a blank list (E-U1). */
    val isEmpty: Boolean get() = !isLoading && error == null && buildings.isEmpty()
}
