package com.example.condominio.ui.screens.billboard

import com.example.condominio.data.model.ToggleReactionResponseDto
import com.example.condominio.testutil.FakeAnnouncementsRepository
import com.example.condominio.testutil.installMain
import com.example.condominio.testutil.resetTestMain
import com.example.condominio.testutil.testAnnouncement
import com.example.condominio.ui.utils.UiText
import condominio.shared.generated.resources.Res
import condominio.shared.generated.resources.error_loading_announcement
import condominio.shared.generated.resources.error_toggling_reaction
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

class BillboardDetailViewModelTest {
    private val repo = FakeAnnouncementsRepository()

    @AfterTest fun tearDown() = resetTestMain()

    private fun TestScope.vm(): BillboardDetailViewModel {
        installMain(this)
        repo.detailResult = Result.success(testAnnouncement(reacted = false, reactions = 3))
        return BillboardDetailViewModel(repo)
    }

    // --- caracterizacion (comportamiento previo que se conserva) ---

    @Test fun `load stores the announcement reaction state and marks it read once`() = runTest {
        val vm = vm()
        vm.loadAnnouncement("a1"); advanceUntilIdle()
        assertEquals("a1", vm.uiState.value.announcement?.id)
        assertEquals(3, vm.uiState.value.reactionsCount)
        assertFalse(vm.uiState.value.reacted)
        assertFalse(vm.uiState.value.isLoading)
        assertEquals(listOf("a1"), repo.markedRead)
    }

    @Test fun `toggle reaction updates optimistically and keeps the server answer`() = runTest {
        val vm = vm()
        vm.loadAnnouncement("a1"); advanceUntilIdle()
        repo.toggleResult = Result.success(ToggleReactionResponseDto(reacted = true))
        vm.toggleReaction()
        assertTrue(vm.uiState.value.reacted)
        assertEquals(4, vm.uiState.value.reactionsCount)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.reacted)
        assertFalse(vm.uiState.value.isTogglingReaction)
    }

    @Test fun `failed toggle rolls the reaction back`() = runTest {
        val vm = vm()
        vm.loadAnnouncement("a1"); advanceUntilIdle()
        vm.toggleReaction(); advanceUntilIdle()
        assertFalse(vm.uiState.value.reacted)
        assertEquals(3, vm.uiState.value.reactionsCount)
        assertFalse(vm.uiState.value.isTogglingReaction)
    }

    // --- E-C3 / C-01: errores traducibles y reintento ---

    @Test fun `detail failure exposes a translatable error key and no announcement`() = runTest {
        val vm = vm()
        repo.detailResult = Result.failure(Exception("raw server message"))
        vm.loadAnnouncement("a1"); advanceUntilIdle()
        assertNull(vm.uiState.value.announcement)
        val error = assertIs<UiText.StringResource>(vm.uiState.value.error)
        assertEquals(Res.string.error_loading_announcement, error.res)
        assertTrue(repo.markedRead.isEmpty())
    }

    @Test fun `retrying after a failure loads the announcement and clears the error`() = runTest {
        val vm = vm()
        repo.detailResult = Result.failure(Exception("offline"))
        vm.loadAnnouncement("a1"); advanceUntilIdle()
        repo.detailResult = Result.success(testAnnouncement())
        vm.loadAnnouncement("a1"); advanceUntilIdle()
        assertEquals("a1", vm.uiState.value.announcement?.id)
        assertNull(vm.uiState.value.error)
        assertEquals(2, repo.detailCount)
    }

    @Test fun `failed toggle exposes a translatable error key`() = runTest {
        val vm = vm()
        vm.loadAnnouncement("a1"); advanceUntilIdle()
        vm.toggleReaction(); advanceUntilIdle()
        val error = assertIs<UiText.StringResource>(vm.uiState.value.error)
        assertEquals(Res.string.error_toggling_reaction, error.res)
    }
}
