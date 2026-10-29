package com.example.condominio.ui.screens.register

import com.example.condominio.testutil.*
import com.example.condominio.ui.utils.UiText
import condominio.shared.generated.resources.Res
import condominio.shared.generated.resources.error_fill_all_fields
import condominio.shared.generated.resources.error_unknown
import condominio.shared.generated.resources.error_check_connection
import condominio.shared.generated.resources.error_registration_conflict
import condominio.shared.generated.resources.error_failed_load_units
import condominio.shared.generated.resources.qr_invalid_code
import com.example.condominio.data.model.Building
import com.example.condominio.data.model.UnitDto
import com.example.condominio.data.remote.ApiFailureException
import com.example.condominio.data.remote.NetworkFailureException
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class RegisterViewModelTest {
    private val scope = TestScope()
    private val buildings = FakeBuildingRepository()
    private val onboarding = FakeOnboardingRepository()

    @BeforeTest fun setUp() = installMain(scope)
    @AfterTest fun tearDown() = resetTestMain()

    private fun vm() = RegisterViewModel(buildings, onboarding)

    private fun RegisterViewModel.fillAll() {
        onFirstNameChange("Ana")
        onLastNameChange("Pérez")
        onEmailChange("ana@mail.com")
        onDocumentIdChange("V123")
        onUnitChange("u1", "1A")
    }

    @Test fun `missing fields are flagged one by one`() = scope.runTest {
        val vm = vm()
        vm.onFirstNameChange("Ana")
        vm.onRegisterClick()
        val state = vm.uiState.value
        assertEquals(
            setOf(RegisterField.LastName, RegisterField.Email, RegisterField.DocumentId, RegisterField.Unit),
            state.missingFields
        )
        val error = assertIs<UiText.StringResource>(state.error)
        assertEquals(Res.string.error_fill_all_fields, error.res)
        assertEquals(0, onboarding.submitCount)
    }

    @Test fun `editing a flagged field clears only its flag`() = scope.runTest {
        val vm = vm()
        vm.onRegisterClick()
        vm.onEmailChange("a@b.c")
        assertFalse(RegisterField.Email in vm.uiState.value.missingFields)
        assertTrue(RegisterField.LastName in vm.uiState.value.missingFields)
    }

    private suspend fun TestScope.submitFailing(error: Throwable): UiText.StringResource {
        onboarding.result = Result.failure(error)
        val vm = vm()
        vm.onBuildingScanned("COND-1")
        vm.fillAll()
        vm.onRegisterClick()
        advanceUntilIdle()
        return assertIs<UiText.StringResource>(vm.uiState.value.error)
    }

    @Test fun `an existing request shows the conflict text`() = scope.runTest {
        assertEquals(Res.string.error_registration_conflict, submitFailing(ApiFailureException(409)).res)
    }

    @Test fun `no network on register shows the connection text`() = scope.runTest {
        assertEquals(Res.string.error_check_connection, submitFailing(NetworkFailureException("down")).res)
    }

    @Test fun `a technical message on register is never shown`() = scope.runTest {
        assertEquals(Res.string.error_unknown, submitFailing(Exception("Error del servidor (500)")).res)
    }

    @Test fun `a failure without message is never silent`() = scope.runTest {
        onboarding.result = Result.failure(Exception())
        val vm = vm()
        vm.onBuildingScanned("COND-1")
        vm.fillAll()
        vm.onRegisterClick()
        advanceUntilIdle()
        assertEquals(1, onboarding.submitCount)
        val error = assertIs<UiText.StringResource>(vm.uiState.value.error)
        assertEquals(Res.string.error_unknown, error.res)
    }

    @Test fun `no network on the building lookup shows the load error, not an invalid code`() = scope.runTest {
        buildings.byCodeResult = Result.failure(NetworkFailureException("Fail to fetch"))
        val vm = vm()
        vm.onBuildingScanned("COND-1")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.loadFailed)
        assertNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoadingBuilding)
    }

    @Test fun `an unknown code (404) on building lookup shows the invalid code text`() = scope.runTest {
        buildings.byCodeResult = Result.failure(ApiFailureException(404))
        val vm = vm()
        vm.onBuildingScanned("COND-1")
        advanceUntilIdle()
        assertFalse(vm.uiState.value.loadFailed)
        val error = assertIs<UiText.StringResource>(vm.uiState.value.error)
        assertEquals(Res.string.qr_invalid_code, error.res)
    }

    @Test fun `a 400 error on building lookup shows the invalid code text`() = scope.runTest {
        buildings.byCodeResult = Result.failure(ApiFailureException(400))
        val vm = vm()
        vm.onBuildingScanned("COND-1")
        advanceUntilIdle()
        assertFalse(vm.uiState.value.loadFailed)
        val error = assertIs<UiText.StringResource>(vm.uiState.value.error)
        assertEquals(Res.string.qr_invalid_code, error.res)
    }

    @Test fun `a 500 error on building lookup shows the load error, not an invalid code`() = scope.runTest {
        buildings.byCodeResult = Result.failure(ApiFailureException(500))
        val vm = vm()
        vm.onBuildingScanned("COND-1")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.loadFailed)
        assertNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoadingBuilding)
    }

    @Test fun `retrying after a network failure loads the building and its units`() = scope.runTest {
        buildings.byCodeResult = Result.failure(NetworkFailureException("Fail to fetch"))
        val vm = vm()
        vm.onBuildingScanned("COND-1")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.loadFailed)

        buildings.byCodeResult = Result.success(Building(id = "b1", name = "Torre"))
        buildings.unitsByCodeResult = Result.success(listOf(UnitDto(id = "u1", buildingId = "b1", name = "1A", floor = null, aliquot = null)))
        vm.retryLoad()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.loadFailed)
        assertEquals("Torre", vm.uiState.value.buildingName)
        assertEquals(1, vm.uiState.value.units.size)
        assertEquals(listOf("COND-1", "COND-1"), buildings.byCodeCalls)
    }

    @Test fun `no network on units lookup shows the load error and retry reloads only units`() = scope.runTest {
        buildings.byCodeResult = Result.success(Building(id = "b1", name = "Torre"))
        buildings.unitsByCodeResult = Result.failure(NetworkFailureException("down"))
        val vm = vm()
        vm.onBuildingScanned("COND-1")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.loadFailed)
        assertEquals("Torre", vm.uiState.value.buildingName)

        buildings.unitsByCodeResult = Result.success(listOf(UnitDto(id = "u1", buildingId = "b1", name = "1A", floor = null, aliquot = null)))
        vm.retryLoad()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.loadFailed)
        assertEquals(1, vm.uiState.value.units.size)
        assertEquals(1, buildings.byCodeCalls.size)
        assertEquals(2, buildings.unitsByCodeCalls.size)
    }

    @Test fun `a 500 error on units lookup shows inline error not load error`() = scope.runTest {
        buildings.byCodeResult = Result.success(Building(id = "b1", name = "Torre"))
        buildings.unitsByCodeResult = Result.failure(ApiFailureException(500))
        val vm = vm()
        vm.onBuildingScanned("COND-1")
        advanceUntilIdle()
        assertFalse(vm.uiState.value.loadFailed)
        assertEquals("Torre", vm.uiState.value.buildingName)
        val error = assertIs<UiText.StringResource>(vm.uiState.value.error)
        assertEquals(Res.string.error_failed_load_units, error.res)
    }

    @Test fun `a 404 error on units lookup shows inline error not load error`() = scope.runTest {
        buildings.byCodeResult = Result.success(Building(id = "b1", name = "Torre"))
        buildings.unitsByCodeResult = Result.failure(ApiFailureException(404))
        val vm = vm()
        vm.onBuildingScanned("COND-1")
        advanceUntilIdle()
        assertFalse(vm.uiState.value.loadFailed)
        val error = assertIs<UiText.StringResource>(vm.uiState.value.error)
        assertEquals(Res.string.error_failed_load_units, error.res)
    }
}
