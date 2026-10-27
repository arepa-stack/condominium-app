package com.example.condominio.ui.screens.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condominio.data.remote.NetworkFailureException
import com.example.condominio.data.repository.BuildingRepository
import com.example.condominio.ui.utils.UiText
import condominio.shared.generated.resources.Res
import condominio.shared.generated.resources.error_check_connection
import condominio.shared.generated.resources.qr_invalid_code
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Validates a building code (scanned or typed) before leaving the scanner: an invalid code shows its
 * error here instead of opening a registration form that cannot work (A-25).
 */
class QrScannerViewModel(
    private val buildingRepository: BuildingRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(QrScannerUiState())
    val uiState = _uiState.asStateFlow()

    /** The last code the building lookup rejected: the camera keeps reading it every frame, so skip repeats. */
    private var lastRejected: String? = null

    /** From the camera. */
    fun onScanned(raw: String) {
        if (extractBuildingCode(raw) == lastRejected) return
        validate(raw)
    }

    /** Typed by hand: always validated, so the user can retry the same code. */
    fun onManualCode(raw: String) = validate(raw)

    fun onErrorDismissed() {
        _uiState.update { it.copy(error = null) }
    }

    private fun validate(raw: String) {
        val code = extractBuildingCode(raw)
        val state = _uiState.value
        if (code.isBlank() || state.isValidating || state.validCode != null) return

        _uiState.update { it.copy(isValidating = true, error = null) }
        viewModelScope.launch {
            buildingRepository.getBuildingByCode(code)
                .onSuccess {
                    lastRejected = null
                    _uiState.update { it.copy(isValidating = false, validCode = code) }
                }
                .onFailure { e ->
                    // Remembered also for a network failure, or the camera would retry every frame; the user
                    // retries by typing the code or by pointing the camera at another one.
                    lastRejected = code
                    val text = if (e is NetworkFailureException) Res.string.error_check_connection else Res.string.qr_invalid_code
                    _uiState.update { it.copy(isValidating = false, error = UiText.StringResource(text)) }
                }
        }
    }
}

data class QrScannerUiState(
    val isValidating: Boolean = false,
    val error: UiText? = null,
    /** Set once the building exists: the screen navigates to the form with this code. */
    val validCode: String? = null
)
