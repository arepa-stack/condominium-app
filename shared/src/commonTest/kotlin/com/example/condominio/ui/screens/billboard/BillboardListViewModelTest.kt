package com.example.condominio.ui.screens.billboard

import com.example.condominio.data.remote.NetworkFailureException
import com.example.condominio.testutil.*
import com.example.condominio.ui.utils.UiText
import condominio.shared.generated.resources.Res
import condominio.shared.generated.resources.error_loading_announcements
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class BillboardListViewModelTest {
    private val scope = TestScope()
    private val announcements = FakeAnnouncementsRepository()
    private val auth = FakeAuthRepository(testUserWithBuilding)

    @BeforeTest fun setUp() = installMain(scope)
    @AfterTest fun tearDown() = resetTestMain()

    private fun vm() = BillboardListViewModel(announcements, auth)

    @Test fun `opening with a user loads its building's announcements without fetching the user`() = scope.runTest {
        announcements.listResult = Result.success(testAnnouncementsPage(testAnnouncement()))
        val vm = vm()
        advanceUntilIdle()
        assertEquals(listOf("b1"), announcements.listCalls)
        assertEquals(1, vm.uiState.value.announcements.size)
        assertEquals(0, auth.fetchCount)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun `a failed load exposes a translatable error, not the exception text`() = scope.runTest {
        announcements.listResult = Result.failure(Exception("Fail to fetch"))
        val vm = vm()
        advanceUntilIdle()
        val error = vm.uiState.value.error
        assertIs<UiText.StringResource>(error)
        assertEquals(Res.string.error_loading_announcements, error.res)
        assertTrue(vm.uiState.value.announcements.isEmpty())
    }

    @Test fun `a failure without message is never silent`() = scope.runTest {
        announcements.listResult = Result.failure(Exception())
        val vm = vm()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)
    }

    @Test fun `retry after a failure loads the list and clears the error`() = scope.runTest {
        announcements.listResult = Result.failure(Exception("Fail to fetch"))
        val vm = vm()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)

        announcements.listResult = Result.success(testAnnouncementsPage(testAnnouncement()))
        vm.refresh()
        advanceUntilIdle()
        assertNull(vm.uiState.value.error)
        assertEquals(1, vm.uiState.value.announcements.size)
        assertEquals(2, announcements.listCalls.size)
    }

    @Test fun `opening without a loaded user fetches it and then loads the list`() = scope.runTest {
        auth.currentUserFlow.value = null
        auth.publishOnFetch = true
        announcements.listResult = Result.success(testAnnouncementsPage(testAnnouncement()))
        val vm = vm()
        advanceUntilIdle()
        assertEquals(1, auth.fetchCount)
        assertEquals(listOf("b1"), announcements.listCalls)
        assertEquals(1, vm.uiState.value.announcements.size)
        assertNull(vm.uiState.value.error)
    }

    @Test fun `opening without a user and failing to load it shows the error, retry recovers`() = scope.runTest {
        auth.currentUserFlow.value = null
        auth.fetchResult = Result.failure(NetworkFailureException("Fail to fetch"))
        val vm = vm()
        advanceUntilIdle()
        val error = vm.uiState.value.error
        assertIs<UiText.StringResource>(error)
        assertEquals(Res.string.error_loading_announcements, error.res)
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(announcements.listCalls.isEmpty())

        auth.fetchResult = null
        auth.publishOnFetch = true
        announcements.listResult = Result.success(testAnnouncementsPage(testAnnouncement()))
        vm.refresh()
        advanceUntilIdle()
        assertEquals(2, auth.fetchCount)
        assertEquals(listOf("b1"), announcements.listCalls)
        assertNull(vm.uiState.value.error)
        assertEquals(1, vm.uiState.value.announcements.size)
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
        assertEquals(Res.string.error_loading_announcements, error.res)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun `user without buildingId does not fetch the list`() = scope.runTest {
        auth.currentUserFlow.value = testUser
        val vm = vm()
        advanceUntilIdle()
        assertTrue(announcements.listCalls.isEmpty())
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
    }
}
