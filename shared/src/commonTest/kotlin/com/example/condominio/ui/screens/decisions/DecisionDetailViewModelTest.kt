package com.example.condominio.ui.screens.decisions

import com.example.condominio.testutil.FakeAuthRepository
import com.example.condominio.testutil.FakeDecisionsRepository
import com.example.condominio.testutil.installMain
import com.example.condominio.testutil.resetTestMain
import com.example.condominio.testutil.testDecisionDetail
import com.example.condominio.testutil.testUser
import com.example.condominio.testutil.testVote
import com.example.condominio.ui.utils.UiText
import condominio.shared.generated.resources.Res
import condominio.shared.generated.resources.decisions_no_active_unit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import condominio.shared.generated.resources.error_loading_decision

class DecisionDetailViewModelTest {
    private val repo = FakeDecisionsRepository()

    @AfterTest fun tearDown() = resetTestMain()

    private fun TestScope.vm(auth: FakeAuthRepository = FakeAuthRepository()): DecisionDetailViewModel {
        installMain(this)
        repo.detailResult = Result.success(testDecisionDetail())
        return DecisionDetailViewModel(repo, auth)
    }

    // --- caracterizacion (comportamiento previo que se conserva) ---

    @Test fun `load detail stores the decision`() = runTest {
        val vm = vm()
        vm.loadDetail("d1")
        advanceUntilIdle()
        assertEquals("d1", vm.uiState.value.detail?.decision?.id)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun `detail failure keeps the error and no detail`() = runTest {
        val vm = vm()
        repo.detailResult = Result.failure(Exception("boom"))
        vm.loadDetail("d1")
        advanceUntilIdle()
        assertNull(vm.uiState.value.detail)
        assertNotNull(vm.uiState.value.error)
    }

    @Test fun `successful vote closes the sheet and reloads the detail`() = runTest {
        val vm = vm()
        vm.loadDetail("d1"); advanceUntilIdle()
        repo.voteResult = Result.success(testVote())
        vm.openVoteSheet()
        vm.submitVote("q1"); advanceUntilIdle()
        assertFalse(vm.uiState.value.showVoteSheet)
        assertFalse(vm.uiState.value.isSubmitting)
        assertEquals(2, repo.detailCount)
    }

    @Test fun `failed vote keeps the sheet open with the error`() = runTest {
        val vm = vm()
        vm.loadDetail("d1"); advanceUntilIdle()
        vm.openVoteSheet()
        vm.submitVote("q1"); advanceUntilIdle()
        assertTrue(vm.uiState.value.showVoteSheet)
        assertNotNull(vm.uiState.value.submissionError)
    }

    // --- E-V2 ---

    @Test fun `vote sheet cannot be closed while the vote is being sent`() = runTest {
        val vm = vm()
        vm.loadDetail("d1"); advanceUntilIdle()
        repo.gate = CompletableDeferred()
        repo.voteResult = Result.success(testVote())
        vm.openVoteSheet()
        vm.submitVote("q1"); advanceUntilIdle()
        vm.closeVoteSheet()
        assertTrue(vm.uiState.value.showVoteSheet)
        repo.gate!!.complete(Unit); advanceUntilIdle()
        assertFalse(vm.uiState.value.showVoteSheet)
    }

    @Test fun `upload sheet cannot be closed while the quote is being sent`() = runTest {
        val vm = vm()
        vm.loadDetail("d1"); advanceUntilIdle()
        repo.gate = CompletableDeferred()
        vm.openUploadSheet()
        vm.submitQuote("Proveedor", 10.0, null, "uri", "image/png"); advanceUntilIdle()
        vm.closeUploadSheet()
        assertTrue(vm.uiState.value.showUploadSheet)
        repo.gate!!.complete(Unit); advanceUntilIdle()
    }

    @Test fun `a second vote tap while sending is ignored`() = runTest {
        val vm = vm()
        vm.loadDetail("d1"); advanceUntilIdle()
        repo.gate = CompletableDeferred()
        repo.voteResult = Result.success(testVote())
        vm.openVoteSheet()
        vm.submitVote("q1"); advanceUntilIdle()
        vm.submitVote("q1"); advanceUntilIdle()
        assertEquals(1, repo.votes.size)
        repo.gate!!.complete(Unit); advanceUntilIdle()
    }

    @Test fun `missing unit reports a resource error not hardcoded text`() = runTest {
        val vm = vm(FakeAuthRepository(testUser.copy(units = emptyList(), currentUnit = null)))
        vm.loadDetail("d1"); advanceUntilIdle()
        vm.submitVote("q1"); advanceUntilIdle()
        val err = vm.uiState.value.submissionError
        assertTrue(err is UiText.StringResource && err.res == Res.string.decisions_no_active_unit)
    }

    @Test fun `detail failure exposes a translatable error key`() = runTest {
        val vm = vm()
        repo.detailResult = Result.failure(Exception("Fail to fetch"))
        vm.loadDetail("d1")
        advanceUntilIdle()
        assertEquals(Res.string.error_loading_decision, assertIs<UiText.StringResource>(vm.uiState.value.error).res)
        assertNull(vm.uiState.value.detail)
    }

    @Test fun `a detail failure without message is never silent`() = runTest {
        val vm = vm()
        repo.detailResult = Result.failure(Exception())
        vm.loadDetail("d1")
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)
    }

    @Test fun `retrying after a failure loads the detail and clears the error`() = runTest {
        val vm = vm()
        repo.detailResult = Result.failure(Exception("Fail to fetch"))
        vm.loadDetail("d1")
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)

        repo.detailResult = Result.success(testDecisionDetail())
        vm.refresh()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.detail)
        assertNull(vm.uiState.value.error)
        assertEquals(2, repo.detailCount)
    }
}
