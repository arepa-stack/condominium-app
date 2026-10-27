package com.example.condominio.ui.screens.profile

import com.example.condominio.data.remote.NetworkFailureException
import com.example.condominio.testutil.*
import com.example.condominio.ui.utils.UiText
import condominio.shared.generated.resources.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ProfileViewModelTest {
    private val scope = TestScope()
    private val auth = FakeAuthRepository()

    @BeforeTest fun setUp() = installMain(scope)
    @AfterTest fun tearDown() = resetTestMain()

    // --- caracterizacion ---

    @Test fun `opening loads the user once`() = scope.runTest {
        val vm = ProfileViewModel(auth)
        advanceUntilIdle()
        assertEquals(1, auth.fetchCount)
        assertEquals("Ana", vm.uiState.value.user?.name)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun `logging out clears the session and leaves`() = scope.runTest {
        val vm = ProfileViewModel(auth)
        advanceUntilIdle()
        vm.onLogoutClick()
        advanceUntilIdle()
        assertEquals(1, auth.logoutCount)
        assertTrue(vm.uiState.value.isLoggedOut)
    }

    @Test fun `deleting the account leaves the screen`() = scope.runTest {
        val vm = ProfileViewModel(auth)
        advanceUntilIdle()
        vm.onDeleteAccountConfirm("")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isLoggedOut)
        assertNull(vm.uiState.value.deleteError)
    }

    // --- E-R1 ---

    @Test fun `a second logout tap while the first is running is ignored`() = scope.runTest {
        auth.logoutGate = CompletableDeferred()
        val vm = ProfileViewModel(auth)
        advanceUntilIdle()
        vm.onLogoutClick()
        vm.onLogoutClick()
        advanceUntilIdle()
        assertEquals(1, auth.logoutCount)
        assertTrue(vm.uiState.value.isLoggingOut)
        auth.logoutGate!!.complete(Unit)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isLoggedOut)
    }

    // --- D (sin texto crudo del servidor) ---

    @Test fun `a failed delete shows a translated text not the server one`() = scope.runTest {
        auth.deleteResult = Result.failure(Exception("Failed to delete account"))
        val vm = ProfileViewModel(auth)
        advanceUntilIdle()
        vm.onDeleteAccountConfirm("reason")
        advanceUntilIdle()
        val error = assertIs<UiText.StringResource>(vm.uiState.value.deleteError)
        assertEquals(Res.string.error_unknown, error.res)
        assertFalse(vm.uiState.value.isLoggedOut)
        assertFalse(vm.uiState.value.isDeleting)
    }

    @Test fun `no network on delete shows the connection text`() = scope.runTest {
        auth.deleteResult = Result.failure(NetworkFailureException("Fail to fetch"))
        val vm = ProfileViewModel(auth)
        advanceUntilIdle()
        vm.onDeleteAccountConfirm("")
        advanceUntilIdle()
        assertEquals(Res.string.error_check_connection, assertIs<UiText.StringResource>(vm.uiState.value.deleteError).res)
    }
}
