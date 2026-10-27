package com.example.condominio.ui.screens

import com.example.condominio.data.model.BuildingRole
import com.example.condominio.data.model.User
import com.example.condominio.data.model.UserUnit
import com.example.condominio.data.remote.NetworkFailureException
import com.example.condominio.testutil.FakeAuthRepository
import com.example.condominio.testutil.installMain
import com.example.condominio.testutil.resetTestMain
import com.example.condominio.ui.utils.UiText
import condominio.shared.generated.resources.Res
import condominio.shared.generated.resources.error_loading_units
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UnitSelectionViewModelTest {
    private val u1 = UserUnit("u1", "b1", "2-2", "Torre A")
    private val u2 = UserUnit("u2", "b1", "7-2", "Torre A")
    private val u3 = UserUnit("u3", "b2", "1A", "Torre B")
    private val user = User(
        id = "1", name = "Ana", email = "a@b.c", units = listOf(u1, u2, u3),
        buildingRoles = listOf(BuildingRole("b1", "board"))
    )
    private val repo = FakeAuthRepository(user)

    @AfterTest fun tearDown() = resetTestMain()

    private fun TestScope.vm(): UnitSelectionViewModel {
        installMain(this)
        return UnitSelectionViewModel(repo)
    }

    private fun TestScope.loaded(): UnitSelectionViewModel {
        val v = vm(); advanceUntilIdle(); return v
    }

    private fun UnitSelectionViewModel.group(id: String) = uiState.value.buildings.first { it.buildingId == id }

    // --- caracterizacion ---

    @Test fun `opening loads the user once and groups units by building with role`() = runTest {
        val vm = loaded()
        assertEquals(1, repo.fetchCount)
        val s = vm.uiState.value
        assertFalse(s.isLoading)
        assertEquals("Ana", s.userName)
        assertEquals(listOf("b1", "b2"), s.buildings.map { it.buildingId })
        assertEquals("board", s.buildings[0].role)
        assertEquals("resident", s.buildings[1].role)
    }

    @Test fun `choosing a building with one unit selects it`() = runTest {
        val vm = loaded()
        vm.onBuildingSelected(vm.group("b2"))
        assertTrue(vm.uiState.value.unitSelected)
    }

    @Test fun `choosing a building with several units expands it without selecting`() = runTest {
        val vm = loaded()
        vm.onBuildingSelected(vm.group("b1"))
        assertEquals("b1", vm.uiState.value.expandedBuildingId)
        assertFalse(vm.uiState.value.unitSelected)
    }

    @Test fun `choosing a unit selects it`() = runTest {
        val vm = loaded()
        vm.onUnitSelected(u2)
        assertTrue(vm.uiState.value.unitSelected)
    }

    // --- E-U2 ---

    @Test fun `choosing the expanded building again collapses it`() = runTest {
        val vm = loaded()
        vm.onBuildingSelected(vm.group("b1"))
        vm.onBuildingSelected(vm.group("b1"))
        assertNull(vm.uiState.value.expandedBuildingId)
    }

    // --- A-07 ---

    @Test fun `load failure shows an error instead of spinner or empty list`() = runTest {
        repo.fetchResult = Result.failure(NetworkFailureException("down"))
        val s = loaded().uiState.value
        assertFalse(s.isLoading)
        assertEquals(Res.string.error_loading_units, assertIs<UiText.StringResource>(s.error).res)
        assertFalse(s.isEmpty)
    }

    @Test fun `unexpected throw while loading shows the error too`() = runTest {
        repo.fetchThrows = IllegalStateException("boom")
        val s = loaded().uiState.value
        assertFalse(s.isLoading)
        assertIs<UiText.StringResource>(s.error)
    }

    @Test fun `retry after failure reloads and clears the error`() = runTest {
        repo.fetchResult = Result.failure(NetworkFailureException("down"))
        val vm = loaded()
        repo.fetchResult = null
        vm.retry(); advanceUntilIdle()
        val s = vm.uiState.value
        assertNull(s.error)
        assertEquals(2, s.buildings.size)
        assertEquals(2, repo.fetchCount)
    }

    @Test fun `retry shows loading again while it runs`() = runTest {
        repo.fetchResult = Result.failure(NetworkFailureException("down"))
        val vm = loaded()
        vm.retry()
        assertTrue(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
        advanceUntilIdle()
    }

    @Test fun `retry while a load is in flight does not duplicate the request`() = runTest {
        val vm = vm()
        vm.retry(); vm.retry()
        advanceUntilIdle()
        assertEquals(1, repo.fetchCount)
    }

    // --- E-U1 ---

    @Test fun `a user without units is an empty state not an error`() = runTest {
        repo.user = user.copy(units = emptyList())
        val s = loaded().uiState.value
        assertTrue(s.isEmpty)
        assertNull(s.error)
    }

    @Test fun `loading and populated states are not empty`() = runTest {
        val vm = vm()
        assertFalse(vm.uiState.value.isEmpty)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isEmpty)
    }
}
