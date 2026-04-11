package com.example.condominio.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
                val unitItems = user.units.map { SelectionItem.UnitItem(it) }
                val roleItems = user.buildingRoles.map { SelectionItem.RoleItem(it) }
                
                _uiState.update { 
                    it.copy(
                        items = unitItems + roleItems, 
                        isLoading = false,
                        userName = user.name
                    ) 
                }
            }.onFailure {
                _uiState.update { state -> state.copy(isLoading = false) }
            }
        }
    }

    fun onItemSelected(item: SelectionItem) {
        when (item) {
            is SelectionItem.UnitItem -> {
                authRepository.setCurrentUnit(item.unit)
            }
            is SelectionItem.RoleItem -> {
                val virtualUnit = UserUnit(
                    unitId = "ADMIN-${item.role.buildingId}",
                    buildingId = item.role.buildingId,
                    unitName = "Administración",
                    buildingName = "Edificio ${item.role.buildingId.take(4)}",
                    isPrimary = false
                )
                authRepository.setCurrentUnit(virtualUnit)
            }
        }
        _uiState.update { it.copy(unitSelected = true) }
    }
}

sealed class SelectionItem {
    data class UnitItem(val unit: UserUnit) : SelectionItem()
    data class RoleItem(val role: com.example.condominio.data.model.BuildingRole) : SelectionItem()
}

data class UnitSelectionUiState(
    val items: List<SelectionItem> = emptyList(),
    val isLoading: Boolean = true,
    val userName: String = "",
    val unitSelected: Boolean = false
)
