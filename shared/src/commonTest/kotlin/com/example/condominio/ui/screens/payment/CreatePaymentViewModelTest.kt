package com.example.condominio.ui.screens.payment

import androidx.lifecycle.SavedStateHandle
import com.example.condominio.data.model.ExchangeRateDto
import com.example.condominio.data.remote.NetworkFailureException
import com.example.condominio.testutil.*
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class CreatePaymentViewModelTest {
    private val scope = TestScope()
    private val payments = FakePaymentRepository()

    @BeforeTest fun setUp() = installMain(scope)
    @AfterTest fun tearDown() = resetTestMain()

    private fun create() = CreatePaymentViewModel(payments, FakeAuthRepository(), SavedStateHandle())

    @Test fun `opening loads the BCV rate and its date`() = scope.runTest {
        payments.bcvRateResult = Result.success(ExchangeRateDto("2026-10-27", 216.37))
        val vm = create()
        advanceUntilIdle()
        val s = vm.uiState.value
        assertEquals(216.37, s.bcvRate)
        assertEquals("2026-10-27", s.bcvRateDate)
        assertFalse(s.isLoadingRate)
        assertEquals(1, payments.bcvRateCount)
    }

    @Test fun `a failed BCV rate leaves it null and stops loading`() = scope.runTest {
        payments.bcvRateResult = Result.failure(NetworkFailureException())
        val vm = create()
        advanceUntilIdle()
        val s = vm.uiState.value
        assertNull(s.bcvRate)
        assertNull(s.bcvRateDate)
        assertFalse(s.isLoadingRate)
    }

    @Test fun `retrying after a failed BCV rate loads it`() = scope.runTest {
        payments.bcvRateResult = Result.failure(NetworkFailureException())
        val vm = create()
        advanceUntilIdle()
        payments.bcvRateResult = Result.success(ExchangeRateDto("2026-10-28", 220.0))
        vm.loadBcvRate()
        advanceUntilIdle()
        assertEquals(220.0, vm.uiState.value.bcvRate)
        assertEquals(2, payments.bcvRateCount)
    }
}
