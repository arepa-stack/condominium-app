package com.example.condominio.ui.screens.login

import com.example.condominio.data.model.SessionExpiredException
import com.example.condominio.testutil.*
import com.example.condominio.ui.utils.UiText
import condominio.shared.generated.resources.Res
import condominio.shared.generated.resources.error_invalid_credentials
import condominio.shared.generated.resources.error_check_connection
import condominio.shared.generated.resources.error_unknown
import com.example.condominio.data.remote.ApiFailureException
import com.example.condominio.data.remote.NetworkFailureException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class LoginViewModelStartupTest {
    private val scope = TestScope()
    private val auth = FakeAuthRepository()

    @BeforeTest fun setUp() = installMain(scope)
    @AfterTest fun tearDown() = resetTestMain()

    @Test fun `without a saved token the login form shows and users me is never asked`() = scope.runTest {
        auth.hasSession = false
        val vm = LoginViewModel(auth)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isCheckingSession)
        assertFalse(vm.uiState.value.sessionCheckFailed)
        assertEquals(0, auth.fetchCount)
    }

    @Test fun `with a saved token the startup asks users me exactly once`() = scope.runTest {
        val vm = LoginViewModel(auth)
        advanceUntilIdle()
        assertEquals(1, auth.fetchCount)
        assertTrue(vm.uiState.value.isSuccess)
    }

    @Test fun `offline startup with a saved token does not send to login`() = scope.runTest {
        auth.fetchResult = Result.failure(Exception("Network down"))
        val vm = LoginViewModel(auth)
        advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state.sessionCheckFailed)
        assertFalse(state.isCheckingSession)
        assertFalse(state.isSuccess)
        assertEquals(0, auth.logoutCount)
    }

    @Test fun `retry after an offline startup enters when the network is back`() = scope.runTest {
        auth.fetchResult = Result.failure(Exception("Network down"))
        val vm = LoginViewModel(auth)
        advanceUntilIdle()
        auth.fetchResult = null
        vm.onRetrySessionCheck()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.sessionCheckFailed)
        assertTrue(vm.uiState.value.isSuccess)
        assertEquals(2, auth.fetchCount)
    }

    @Test fun `retry while still offline keeps the retry state`() = scope.runTest {
        auth.fetchResult = Result.failure(Exception("Network down"))
        val vm = LoginViewModel(auth)
        advanceUntilIdle()
        vm.onRetrySessionCheck()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.sessionCheckFailed)
        assertFalse(vm.uiState.value.isCheckingSession)
    }

    @Test fun `an expired session 401 goes to the login form`() = scope.runTest {
        auth.fetchResult = Result.failure(SessionExpiredException())
        val vm = LoginViewModel(auth)
        advanceUntilIdle()
        val state = vm.uiState.value
        assertFalse(state.sessionCheckFailed)
        assertFalse(state.isCheckingSession)
        assertFalse(state.isSuccess)
    }

    @Test fun `offline startup lets the user choose to sign in with another account`() = scope.runTest {
        auth.fetchResult = Result.failure(Exception("Network down"))
        val vm = LoginViewModel(auth)
        advanceUntilIdle()
        vm.onUseAnotherAccount()
        advanceUntilIdle()
        assertEquals(1, auth.logoutCount)
        assertFalse(vm.uiState.value.sessionCheckFailed)
        assertFalse(vm.uiState.value.isCheckingSession)
    }
}

class LoginViewModelSubmitTest {
    private val scope = TestScope()
    private val auth = FakeAuthRepository().apply { hasSession = false }

    @BeforeTest fun setUp() = installMain(scope)
    @AfterTest fun tearDown() = resetTestMain()

    private fun vm(): LoginViewModel {
        val vm = LoginViewModel(auth)
        vm.onEmailChange("  ana@mail.com ")
        vm.onPasswordChange("secret")
        return vm
    }

    @Test fun `repeated submits while a login is in flight send one request`() = scope.runTest {
        auth.loginGate = CompletableDeferred()
        val vm = vm()
        advanceUntilIdle()
        vm.onLoginClick()
        vm.onLoginClick()
        advanceUntilIdle()
        auth.loginGate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, auth.logins.size)
    }

    @Test fun `the email is trimmed before logging in`() = scope.runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.onLoginClick()
        advanceUntilIdle()
        assertEquals("ana@mail.com", auth.logins.single().first)
    }

    private suspend fun TestScope.failWith(error: Throwable): UiText.StringResource {
        auth.loginResult = Result.failure(error)
        val vm = vm()
        advanceUntilIdle()
        vm.onLoginClick()
        advanceUntilIdle()
        return assertIs<UiText.StringResource>(vm.uiState.value.error)
    }

    @Test fun `a 401 shows the invalid credentials text`() = scope.runTest {
        assertEquals(Res.string.error_invalid_credentials, failWith(ApiFailureException(401)).res)
    }

    @Test fun `no network shows the connection text`() = scope.runTest {
        assertEquals(Res.string.error_check_connection, failWith(NetworkFailureException("Fail to fetch")).res)
    }

    @Test fun `a server error shows the generic text`() = scope.runTest {
        assertEquals(Res.string.error_unknown, failWith(ApiFailureException(500)).res)
    }

    @Test fun `a technical message from the repository is never shown`() = scope.runTest {
        assertEquals(Res.string.error_unknown, failWith(Exception("Login failed (401): Error from server")).res)
    }
}
