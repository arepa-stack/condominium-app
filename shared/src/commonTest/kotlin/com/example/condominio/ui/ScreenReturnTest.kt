package com.example.condominio.ui

import androidx.lifecycle.Lifecycle.Event
import com.example.condominio.domain.usecase.billboard.GetUnreadAnnouncementsCountUseCase
import com.example.condominio.testutil.*
import com.example.condominio.ui.screens.dashboard.DashboardViewModel
import com.example.condominio.ui.screens.payment.PaymentHistoryViewModel
import com.example.condominio.ui.utils.BackgroundReturnDetector
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class BackgroundReturnDetectorTest {
    @Test fun `resume without previous stop is ignored`() {
        val d = BackgroundReturnDetector()
        assertFalse(d.onEvent(Event.ON_START))
        assertFalse(d.onEvent(Event.ON_RESUME))
    }

    @Test fun `stop then start is a return`() {
        val d = BackgroundReturnDetector()
        assertFalse(d.onEvent(Event.ON_STOP))
        assertTrue(d.onEvent(Event.ON_START))
    }

    @Test fun `a second start after one return is ignored`() {
        val d = BackgroundReturnDetector()
        d.onEvent(Event.ON_STOP)
        assertTrue(d.onEvent(Event.ON_START))
        assertFalse(d.onEvent(Event.ON_START))
    }

    @Test fun `pause and resume alone never count`() {
        val d = BackgroundReturnDetector()
        assertFalse(d.onEvent(Event.ON_PAUSE))
        assertFalse(d.onEvent(Event.ON_RESUME))
    }
}

class PaymentHistoryReloadTest {
    private val scope = TestScope()
    private val auth = FakeAuthRepository()
    private val payments = FakePaymentRepository()

    @BeforeTest fun setUp() = installMain(scope)
    @AfterTest fun tearDown() = resetTestMain()

    private fun vm() = PaymentHistoryViewModel(payments, auth)

    @Test fun `opening loads once`() = scope.runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.onScreenReturn() // first composition
        advanceUntilIdle()
        assertEquals(1, payments.fetchPaymentsCount)
        assertEquals(1, auth.fetchCount)
    }

    @Test fun `return signal after opening reloads once`() = scope.runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.onScreenReturn()
        vm.onOpenChild()
        vm.onScreenReturn()
        advanceUntilIdle()
        assertEquals(2, payments.fetchPaymentsCount)
    }

    @Test fun `repeated return signals while loading do not duplicate`() = scope.runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.onScreenReturn()
        vm.onOpenChild()
        vm.onScreenReturn()
        vm.onOpenChild()
        vm.onScreenReturn()
        advanceUntilIdle()
        assertEquals(2, payments.fetchPaymentsCount)
    }

    @Test fun `return signal after the load finished reloads again`() = scope.runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.onScreenReturn()
        vm.onOpenChild()
        vm.onScreenReturn()
        advanceUntilIdle()
        vm.onOpenChild()
        vm.onScreenReturn()
        advanceUntilIdle()
        assertEquals(3, payments.fetchPaymentsCount)
    }

    @Test fun `explicit retry always loads`() = scope.runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.loadPayments()
        advanceUntilIdle()
        assertEquals(2, payments.fetchPaymentsCount)
    }

    @Test fun `re-entry without onOpenChild does not reload (rotation)`() = scope.runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.onScreenReturn() // first composition
        vm.onScreenReturn() // recreated composition after rotation
        vm.onScreenReturn()
        advanceUntilIdle()
        assertEquals(1, payments.fetchPaymentsCount)
    }

    @Test fun `onOpenChild then re-entry reloads once and clears the pending child`() = scope.runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.onScreenReturn()
        vm.onOpenChild()
        vm.onScreenReturn()
        advanceUntilIdle()
        assertEquals(2, payments.fetchPaymentsCount)
        vm.onScreenReturn() // rotation after coming back
        advanceUntilIdle()
        assertEquals(2, payments.fetchPaymentsCount)
    }

    @Test fun `background return reloads without onOpenChild`() = scope.runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.onScreenReturn()
        vm.onBackgroundReturn()
        advanceUntilIdle()
        assertEquals(2, payments.fetchPaymentsCount)
    }
}

class DashboardReloadTest {
    private val scope = TestScope()
    private val auth = FakeAuthRepository()
    private val payments = FakePaymentRepository()

    @BeforeTest fun setUp() = installMain(scope)
    @AfterTest fun tearDown() = resetTestMain()

    private fun vm() = DashboardViewModel(
        auth, payments, FakeBuildingRepository(), FakePettyCashRepository(),
        GetUnreadAnnouncementsCountUseCase(FakeAnnouncementsRepository()),
    )

    @Test fun `opening loads balance once`() = scope.runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.onScreenReturn()
        advanceUntilIdle()
        assertEquals(1, payments.balanceCount)
        assertEquals(1, payments.fetchPaymentsCount)
    }

    @Test fun `return signal reloads once`() = scope.runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.onScreenReturn()
        vm.onOpenChild()
        vm.onScreenReturn()
        advanceUntilIdle()
        assertEquals(2, payments.balanceCount)
    }

    @Test fun `repeated return signals while loading do not duplicate`() = scope.runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.onScreenReturn()
        vm.onOpenChild()
        vm.onScreenReturn()
        vm.onOpenChild()
        vm.onScreenReturn()
        advanceUntilIdle()
        assertEquals(2, payments.balanceCount)
    }

    @Test fun `refresh always loads`() = scope.runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.refresh()
        advanceUntilIdle()
        assertEquals(2, payments.balanceCount)
    }

    @Test fun `re-entry without onOpenChild does not reload (rotation)`() = scope.runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.onScreenReturn() // first composition
        vm.onScreenReturn() // recreated composition after rotation
        vm.onScreenReturn()
        advanceUntilIdle()
        assertEquals(1, payments.balanceCount)
    }

    @Test fun `onOpenChild then re-entry reloads once and clears the pending child`() = scope.runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.onScreenReturn()
        vm.onOpenChild()
        vm.onScreenReturn()
        advanceUntilIdle()
        assertEquals(2, payments.balanceCount)
        vm.onScreenReturn() // rotation after coming back
        advanceUntilIdle()
        assertEquals(2, payments.balanceCount)
    }

    @Test fun `background return reloads without onOpenChild`() = scope.runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.onScreenReturn()
        vm.onBackgroundReturn()
        advanceUntilIdle()
        assertEquals(2, payments.balanceCount)
    }
}
