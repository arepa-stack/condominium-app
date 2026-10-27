package com.example.condominio.ui.screens.profile

import com.example.condominio.data.remote.ApiFailureException
import com.example.condominio.data.remote.NetworkFailureException
import com.example.condominio.testutil.*
import com.example.condominio.ui.utils.UiText
import condominio.shared.generated.resources.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ChangePasswordViewModelTest {
    private val scope = TestScope()
    private val auth = FakeAuthRepository()

    @BeforeTest fun setUp() = installMain(scope)
    @AfterTest fun tearDown() = resetTestMain()

    private fun filled(current: String = "Actual1", new: String = "Nueva123", confirm: String = new): ChangePasswordViewModel {
        val vm = ChangePasswordViewModel(auth)
        vm.onCurrentPasswordChange(current)
        vm.onNewPasswordChange(new)
        vm.onConfirmPasswordChange(confirm)
        return vm
    }

    private fun resError(vm: ChangePasswordViewModel) = assertIs<UiText.StringResource>(vm.uiState.value.error).res

    // --- caracterizacion de las validaciones ---

    @Test fun `a blank current password is required`() = scope.runTest {
        val vm = filled(current = "")
        vm.onChangePasswordClick()
        assertEquals(Res.string.error_password_required, resError(vm))
        assertEquals(0, auth.passwordChanges.size)
    }

    @Test fun `a short new password is rejected`() = scope.runTest {
        val vm = filled(new = "abc")
        vm.onChangePasswordClick()
        assertEquals(Res.string.password_min_length, resError(vm))
        assertEquals(0, auth.passwordChanges.size)
    }

    @Test fun `a mismatch is rejected`() = scope.runTest {
        val vm = filled(confirm = "Otra1234")
        vm.onChangePasswordClick()
        assertEquals(Res.string.error_passwords_dont_match, resError(vm))
    }

    @Test fun `the same password is rejected`() = scope.runTest {
        val vm = filled(current = "Igual123", new = "Igual123")
        vm.onChangePasswordClick()
        assertEquals(Res.string.error_new_password_same, resError(vm))
    }

    @Test fun `a valid change calls the server once`() = scope.runTest {
        val vm = filled()
        vm.onChangePasswordClick()
        advanceUntilIdle()
        assertEquals(listOf("Actual1" to "Nueva123"), auth.passwordChanges)
    }

    // --- campos marcados ---

    @Test fun `each validation flags its own field`() = scope.runTest {
        var vm = filled(current = "")
        vm.onChangePasswordClick()
        assertTrue(vm.uiState.value.currentPasswordError)
        vm = filled(new = "abc")
        vm.onChangePasswordClick()
        assertTrue(vm.uiState.value.newPasswordError)
        vm = filled(confirm = "Otra1234")
        vm.onChangePasswordClick()
        assertTrue(vm.uiState.value.confirmPasswordError)
        assertFalse(vm.uiState.value.newPasswordError)
    }

    @Test fun `editing a field clears its flag`() = scope.runTest {
        val vm = filled(current = "")
        vm.onChangePasswordClick()
        vm.onCurrentPasswordChange("x")
        assertFalse(vm.uiState.value.currentPasswordError)
        assertNull(vm.uiState.value.error)
    }

    // --- E-R4 ---

    @Test fun `an empty form has no unsaved changes and typing creates them`() = scope.runTest {
        val vm = ChangePasswordViewModel(auth)
        assertFalse(vm.uiState.value.hasChanges)
        vm.onNewPasswordChange("a")
        assertTrue(vm.uiState.value.hasChanges)
    }

    @Test fun `a successful change confirms and wipes the typed passwords`() = scope.runTest {
        val vm = filled()
        vm.onChangePasswordClick()
        advanceUntilIdle()
        val s = vm.uiState.value
        assertTrue(s.isSuccess)
        assertEquals("", s.currentPassword)
        assertEquals("", s.newPassword)
        assertEquals("", s.confirmPassword)
        assertFalse(s.hasChanges)
        assertNull(s.error)
    }

    @Test fun `the confirmation is consumed once shown`() = scope.runTest {
        val vm = filled()
        vm.onChangePasswordClick()
        advanceUntilIdle()
        vm.onSuccessShown()
        assertFalse(vm.uiState.value.isSuccess)
    }

    @Test fun `a second tap while changing is ignored`() = scope.runTest {
        auth.changePasswordGate = CompletableDeferred()
        val vm = filled()
        vm.onChangePasswordClick()
        advanceUntilIdle()
        vm.onChangePasswordClick()
        advanceUntilIdle()
        assertEquals(1, auth.passwordChanges.size)
        auth.changePasswordGate!!.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
    }

    // --- D (sin texto crudo del servidor) ---

    private fun TestScope.changeFailing(error: Throwable): ChangePasswordViewModel {
        auth.changePasswordResult = Result.failure(error)
        val vm = filled()
        vm.onChangePasswordClick()
        advanceUntilIdle()
        return vm
    }

    @Test fun `no network shows the connection text and keeps what was typed`() = scope.runTest {
        val vm = changeFailing(NetworkFailureException("Fail to fetch"))
        assertEquals(Res.string.error_check_connection, resError(vm))
        assertEquals("Nueva123", vm.uiState.value.newPassword)
        assertFalse(vm.uiState.value.isSuccess)
    }

    @Test fun `a rejected password shows the policy`() = scope.runTest {
        assertEquals(Res.string.first_login_password_policy, resError(changeFailing(ApiFailureException(400))))
    }

    @Test fun `an expired session says so`() = scope.runTest {
        assertEquals(Res.string.error_session_expired, resError(changeFailing(ApiFailureException(401))))
    }

    @Test fun `a technical message is never shown`() = scope.runTest {
        assertEquals(Res.string.error_unknown, resError(changeFailing(Exception("Failed to change password (500)"))))
    }
}
