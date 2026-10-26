package com.example.condominio.ui.screens.login

import com.example.condominio.testutil.*
import com.example.condominio.ui.utils.UiText
import condominio.shared.generated.resources.Res
import condominio.shared.generated.resources.error_passwords_dont_match
import condominio.shared.generated.resources.error_check_connection
import condominio.shared.generated.resources.first_login_password_policy
import com.example.condominio.data.remote.ApiFailureException
import com.example.condominio.data.remote.NetworkFailureException
import condominio.shared.generated.resources.error_unknown
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class FirstLoginChangePasswordViewModelTest {
    private val scope = TestScope()
    private val auth = FakeAuthRepository()

    @BeforeTest fun setUp() = installMain(scope)
    @AfterTest fun tearDown() = resetTestMain()

    private fun vm() = FirstLoginChangePasswordViewModel(auth)

    @Test fun `logging out from the first login screen clears the session and leaves`() = scope.runTest {
        val vm = vm()
        vm.onLogoutClick()
        advanceUntilIdle()
        assertEquals(1, auth.logoutCount)
        assertTrue(vm.uiState.value.loggedOut)
    }

    @Test fun `a weak password flags the new password field`() = scope.runTest {
        val vm = vm()
        vm.onNewPasswordChange("abc")
        vm.onConfirmPasswordChange("abc")
        vm.onSubmitClick()
        assertTrue(vm.uiState.value.newPasswordError)
        assertFalse(vm.uiState.value.confirmPasswordError)
        assertNotNull(vm.uiState.value.error)
    }

    @Test fun `a mismatch flags the confirm field with the mismatch text`() = scope.runTest {
        val vm = vm()
        vm.onNewPasswordChange("Abcdefg1")
        vm.onConfirmPasswordChange("Abcdefg2")
        vm.onSubmitClick()
        assertFalse(vm.uiState.value.newPasswordError)
        assertTrue(vm.uiState.value.confirmPasswordError)
        val error = assertIs<UiText.StringResource>(vm.uiState.value.error)
        assertEquals(Res.string.error_passwords_dont_match, error.res)
    }

    @Test fun `editing a field clears its error flag`() = scope.runTest {
        val vm = vm()
        vm.onNewPasswordChange("abc")
        vm.onSubmitClick()
        vm.onNewPasswordChange("abcd")
        assertFalse(vm.uiState.value.newPasswordError)
        assertNull(vm.uiState.value.error)
    }

    @Test fun `a failure without message is never silent`() = scope.runTest {
        auth.firstLoginResult = Result.failure(Exception())
        val vm = vm()
        vm.onNewPasswordChange("Abcdefg1")
        vm.onConfirmPasswordChange("Abcdefg1")
        vm.onSubmitClick()
        advanceUntilIdle()
        val error = assertIs<UiText.StringResource>(vm.uiState.value.error)
        assertEquals(Res.string.error_unknown, error.res)
        assertFalse(vm.uiState.value.isSuccess)
    }

    private fun TestScope.submitFailing(error: Throwable): UiText.StringResource {
        auth.firstLoginResult = Result.failure(error)
        val vm = vm()
        vm.onNewPasswordChange("Abcdefg1")
        vm.onConfirmPasswordChange("Abcdefg1")
        vm.onSubmitClick()
        advanceUntilIdle()
        return assertIs<UiText.StringResource>(vm.uiState.value.error)
    }

    @Test fun `a server rejection of the password shows the policy`() = scope.runTest {
        assertEquals(Res.string.first_login_password_policy, submitFailing(ApiFailureException(400)).res)
    }

    @Test fun `no network on first login shows the connection text`() = scope.runTest {
        assertEquals(Res.string.error_check_connection, submitFailing(NetworkFailureException("down")).res)
    }

    @Test fun `a technical message on first login is never shown`() = scope.runTest {
        assertEquals(Res.string.error_unknown, submitFailing(Exception("Failed to change password (500)")).res)
    }

    @Test fun `a valid change succeeds`() = scope.runTest {
        val vm = vm()
        vm.onNewPasswordChange("Abcdefg1")
        vm.onConfirmPasswordChange("Abcdefg1")
        vm.onSubmitClick()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isSuccess)
        assertNull(vm.uiState.value.error)
    }
}

class ForgotPasswordViewModelTest {
    private val scope = TestScope()
    private val auth = FakeAuthRepository()

    @BeforeTest fun setUp() = installMain(scope)
    @AfterTest fun tearDown() = resetTestMain()

    @Test fun `a failure without message is never silent`() = scope.runTest {
        auth.resetResult = Result.failure(Exception())
        val vm = ForgotPasswordViewModel(auth)
        vm.onEmailChange("ana@mail.com")
        vm.onSubmitClick()
        advanceUntilIdle()
        val error = assertIs<UiText.StringResource>(vm.uiState.value.error)
        assertEquals(Res.string.error_unknown, error.res)
        assertFalse(vm.uiState.value.isSuccess)
    }

    @Test fun `no network on reset shows the connection text`() = scope.runTest {
        auth.resetResult = Result.failure(NetworkFailureException("Fail to fetch"))
        val vm = ForgotPasswordViewModel(auth)
        vm.onEmailChange("ana@mail.com")
        vm.onSubmitClick()
        advanceUntilIdle()
        assertEquals(Res.string.error_check_connection, assertIs<UiText.StringResource>(vm.uiState.value.error).res)
    }

    @Test fun `a technical message on reset is never shown`() = scope.runTest {
        auth.resetResult = Result.failure(Exception("Failed to send reset email (500)"))
        val vm = ForgotPasswordViewModel(auth)
        vm.onEmailChange("ana@mail.com")
        vm.onSubmitClick()
        advanceUntilIdle()
        assertEquals(Res.string.error_unknown, assertIs<UiText.StringResource>(vm.uiState.value.error).res)
    }

    @Test fun `a successful reset shows success`() = scope.runTest {
        val vm = ForgotPasswordViewModel(auth)
        vm.onEmailChange(" ana@mail.com ")
        vm.onSubmitClick()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isSuccess)
        assertNull(vm.uiState.value.error)
    }

    @Test fun `an email without at sign is rejected without calling the server`() = scope.runTest {
        val vm = ForgotPasswordViewModel(auth)
        vm.onEmailChange("ana")
        vm.onSubmitClick()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }
}
