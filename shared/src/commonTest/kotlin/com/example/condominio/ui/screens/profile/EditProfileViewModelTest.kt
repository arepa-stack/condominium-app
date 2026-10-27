package com.example.condominio.ui.screens.profile

import com.example.condominio.data.model.User
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

class EditProfileViewModelTest {
    private val scope = TestScope()
    private var auth = FakeAuthRepository()

    @BeforeTest fun setUp() = installMain(scope)
    @AfterTest fun tearDown() = resetTestMain()

    private fun TestScope.loaded(): EditProfileViewModel {
        val vm = EditProfileViewModel(auth)
        advanceUntilIdle()
        return vm
    }

    private fun resError(vm: EditProfileViewModel) = assertIs<UiText.StringResource>(vm.uiState.value.error).res

    // --- caracterizacion ---

    @Test fun `opening shows the current name unit and email`() = scope.runTest {
        val s = loaded().uiState.value
        assertFalse(s.isLoading)
        assertEquals("Ana", s.name)
        assertEquals("1A", s.apartmentUnit)
        assertEquals("a@b.c", s.email)
    }

    @Test fun `saving sends the edited name`() = scope.runTest {
        val vm = loaded()
        vm.onNameChange("Ana Maria")
        vm.onSaveClick()
        advanceUntilIdle()
        assertEquals(listOf("Ana Maria"), auth.updates.map { it.name })
    }

    @Test fun `a blank name is rejected without calling the server`() = scope.runTest {
        val vm = loaded()
        vm.onNameChange("  ")
        vm.onSaveClick()
        advanceUntilIdle()
        assertEquals(0, auth.updates.size)
        assertEquals(Res.string.error_fill_all_fields, resError(vm))
    }

    // --- A-10 ---

    @Test fun `a user without a unit can still save the name`() = scope.runTest {
        auth = FakeAuthRepository(User(id = "1", name = "Ana", email = "a@b.c"))
        val vm = loaded()
        vm.onNameChange("Ana Maria")
        vm.onSaveClick()
        advanceUntilIdle()
        assertEquals(1, auth.updates.size)
        assertTrue(vm.uiState.value.saved)
        assertNull(vm.uiState.value.error)
    }

    // --- E-R4 ---

    @Test fun `no changes at the start`() = scope.runTest {
        assertFalse(loaded().uiState.value.hasChanges)
    }

    @Test fun `editing the name marks unsaved changes and reverting clears them`() = scope.runTest {
        val vm = loaded()
        vm.onNameChange("Ana Maria")
        assertTrue(vm.uiState.value.hasChanges)
        vm.onNameChange("Ana")
        assertFalse(vm.uiState.value.hasChanges)
    }

    @Test fun `surrounding spaces alone are not a change`() = scope.runTest {
        val vm = loaded()
        vm.onNameChange(" Ana ")
        assertFalse(vm.uiState.value.hasChanges)
    }

    @Test fun `saving trims the name and confirms the save`() = scope.runTest {
        val vm = loaded()
        vm.onNameChange("  Ana Maria ")
        vm.onSaveClick()
        advanceUntilIdle()
        assertEquals(listOf("Ana Maria"), auth.updates.map { it.name })
        val s = vm.uiState.value
        assertTrue(s.saved)
        assertFalse(s.hasChanges)
        assertEquals("Ana Maria", s.name)
    }

    @Test fun `the confirmation is consumed once shown`() = scope.runTest {
        val vm = loaded()
        vm.onNameChange("Ana Maria")
        vm.onSaveClick()
        advanceUntilIdle()
        vm.onSavedShown()
        assertFalse(vm.uiState.value.saved)
    }

    @Test fun `saving without changes does not call the server`() = scope.runTest {
        val vm = loaded()
        vm.onSaveClick()
        advanceUntilIdle()
        assertEquals(0, auth.updates.size)
    }

    @Test fun `a second tap while saving is ignored`() = scope.runTest {
        auth.updateGate = CompletableDeferred()
        val vm = loaded()
        vm.onNameChange("Ana Maria")
        vm.onSaveClick()
        advanceUntilIdle()
        vm.onSaveClick()
        advanceUntilIdle()
        assertEquals(1, auth.updates.size)
        assertTrue(vm.uiState.value.isLoading)
        auth.updateGate!!.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
    }

    // --- D (sin texto crudo del servidor) ---

    private fun TestScope.saveFailing(error: Throwable): EditProfileViewModel {
        auth.updateResult = Result.failure(error)
        val vm = loaded()
        vm.onNameChange("Ana Maria")
        vm.onSaveClick()
        advanceUntilIdle()
        return vm
    }

    @Test fun `no network on save shows the connection text and keeps the edit`() = scope.runTest {
        val vm = saveFailing(NetworkFailureException("Fail to fetch"))
        assertEquals(Res.string.error_check_connection, resError(vm))
        assertFalse(vm.uiState.value.saved)
        assertTrue(vm.uiState.value.hasChanges)
    }

    @Test fun `an expired session on save says so`() = scope.runTest {
        assertEquals(Res.string.error_session_expired, resError(saveFailing(ApiFailureException(401))))
    }

    @Test fun `a technical message on save is never shown`() = scope.runTest {
        assertEquals(Res.string.error_unknown, resError(saveFailing(Exception("Failed to update user"))))
    }

    @Test fun `a server error on save is generic`() = scope.runTest {
        assertEquals(Res.string.error_unknown, resError(saveFailing(ApiFailureException(500))))
    }

    @Test fun `editing the name clears the error`() = scope.runTest {
        val vm = saveFailing(ApiFailureException(500))
        vm.onNameChange("Otra")
        assertNull(vm.uiState.value.error)
    }

    @Test fun `a blank name flags the name field`() = scope.runTest {
        val vm = loaded()
        vm.onNameChange("")
        vm.onSaveClick()
        assertTrue(vm.uiState.value.nameError)
        vm.onNameChange("A")
        assertFalse(vm.uiState.value.nameError)
    }
}
