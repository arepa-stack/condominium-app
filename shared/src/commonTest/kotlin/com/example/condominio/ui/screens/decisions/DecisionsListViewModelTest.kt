package com.example.condominio.ui.screens.decisions

import com.example.condominio.data.remote.NetworkFailureException
import com.example.condominio.testutil.*
import com.example.condominio.ui.utils.UiText
import condominio.shared.generated.resources.Res
import condominio.shared.generated.resources.error_loading_decisions
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class DecisionsListViewModelTest {
    private val scope = TestScope()
    private val decisions = FakeDecisionsRepository()
    private val auth = FakeAuthRepository(testUserWithBuilding)

    @BeforeTest fun setUp() = installMain(scope)
    @AfterTest fun tearDown() = resetTestMain()

    private fun vm() = DecisionsListViewModel(decisions, auth)

    @Test fun `opening with a user loads its building's decisions without fetching the user`() = scope.runTest {
        decisions.listResult = Result.success(testDecisionsPage(testDecisionDetail().decision))
        val vm = vm()
        advanceUntilIdle()
        assertEquals(listOf("b1"), decisions.listCalls)
        assertEquals(1, vm.uiState.value.decisions.size)
        assertEquals(0, auth.fetchCount)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun `a failed load exposes a translatable error, not the exception text`() = scope.runTest {
        decisions.listResult = Result.failure(Exception("Fail to fetch"))
        val vm = vm()
        advanceUntilIdle()
        val error = vm.uiState.value.error
        assertIs<UiText.StringResource>(error)
        assertEquals(Res.string.error_loading_decisions, error.res)
        assertTrue(vm.uiState.value.decisions.isEmpty())
    }

    @Test fun `a failure without message is never silent`() = scope.runTest {
        decisions.listResult = Result.failure(Exception())
        val vm = vm()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)
    }

    @Test fun `retry after a failure loads the list and clears the error`() = scope.runTest {
        decisions.listResult = Result.failure(Exception("Fail to fetch"))
        val vm = vm()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)

        decisions.listResult = Result.success(testDecisionsPage(testDecisionDetail().decision))
        vm.refresh()
        advanceUntilIdle()
        assertNull(vm.uiState.value.error)
        assertEquals(1, vm.uiState.value.decisions.size)
        assertEquals(2, decisions.listCalls.size)
    }

    @Test fun `opening without a loaded user fetches it and then loads the list`() = scope.runTest {
        auth.currentUserFlow.value = null
        auth.publishOnFetch = true
        decisions.listResult = Result.success(testDecisionsPage(testDecisionDetail().decision))
        val vm = vm()
        advanceUntilIdle()
        assertEquals(1, auth.fetchCount)
        assertEquals(listOf("b1"), decisions.listCalls)
        assertEquals(1, vm.uiState.value.decisions.size)
        assertNull(vm.uiState.value.error)
    }

    @Test fun `opening without a user and failing to load it shows the error, retry recovers`() = scope.runTest {
        auth.currentUserFlow.value = null
        auth.fetchResult = Result.failure(NetworkFailureException("Fail to fetch"))
        val vm = vm()
        advanceUntilIdle()
        val error = vm.uiState.value.error
        assertIs<UiText.StringResource>(error)
        assertEquals(Res.string.error_loading_decisions, error.res)
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(decisions.listCalls.isEmpty())

        auth.fetchResult = null
        auth.publishOnFetch = true
        decisions.listResult = Result.success(testDecisionsPage(testDecisionDetail().decision))
        vm.refresh()
        advanceUntilIdle()
        assertEquals(2, auth.fetchCount)
        assertEquals(listOf("b1"), decisions.listCalls)
        assertNull(vm.uiState.value.error)
        assertEquals(1, vm.uiState.value.decisions.size)
    }

    @Test fun `a second retry while loading is ignored`() = scope.runTest {
        auth.currentUserFlow.value = null
        auth.fetchGate = kotlinx.coroutines.CompletableDeferred()
        val vm = vm()
        runCurrent()
        vm.refresh()
        assertEquals(1, auth.fetchCount)
        auth.fetchGate!!.complete(Unit)
        advanceUntilIdle()
    }

    @Test fun `a fetch that throws exposes the translatable error`() = scope.runTest {
        auth.currentUserFlow.value = null
        auth.fetchThrows = RuntimeException("Boom")
        val vm = vm()
        advanceUntilIdle()
        val error = vm.uiState.value.error
        assertIs<UiText.StringResource>(error)
        assertEquals(Res.string.error_loading_decisions, error.res)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun `user without buildingId does not fetch the list`() = scope.runTest {
        auth.currentUserFlow.value = testUser
        val vm = vm()
        advanceUntilIdle()
        assertTrue(decisions.listCalls.isEmpty())
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
    }
}
