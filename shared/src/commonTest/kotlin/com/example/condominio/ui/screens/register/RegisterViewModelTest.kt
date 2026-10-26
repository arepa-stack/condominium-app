package com.example.condominio.ui.screens.register

import com.example.condominio.testutil.*
import com.example.condominio.ui.utils.UiText
import condominio.shared.generated.resources.Res
import condominio.shared.generated.resources.error_fill_all_fields
import condominio.shared.generated.resources.error_unknown
import condominio.shared.generated.resources.error_check_connection
import condominio.shared.generated.resources.error_registration_conflict
import com.example.condominio.data.remote.ApiFailureException
import com.example.condominio.data.remote.NetworkFailureException
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class RegisterViewModelTest {
    private val scope = TestScope()
    private val onboarding = FakeOnboardingRepository()

    @BeforeTest fun setUp() = installMain(scope)
    @AfterTest fun tearDown() = resetTestMain()

    private fun vm() = RegisterViewModel(FakeBuildingRepository(), onboarding)

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
}
