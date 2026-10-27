package com.example.condominio.ui.screens.register

import com.example.condominio.data.model.Building
import com.example.condominio.data.remote.ApiFailureException
import com.example.condominio.data.remote.NetworkFailureException
import com.example.condominio.testutil.*
import com.example.condominio.ui.utils.UiText
import condominio.shared.generated.resources.Res
import condominio.shared.generated.resources.error_check_connection
import condominio.shared.generated.resources.qr_invalid_code
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class QrScannerViewModelTest {
    private val scope = TestScope()
    private val buildings = FakeBuildingRepository()

    @BeforeTest fun setUp() = installMain(scope)
    @AfterTest fun tearDown() = resetTestMain()

    private fun vm() = QrScannerViewModel(buildings)

    @Test fun `an unknown code stays on the scanner with the invalid code text`() = scope.runTest {
        buildings.byCodeResult = Result.failure(ApiFailureException(404))
        val vm = vm()
        vm.onManualCode("NOPE")
        advanceUntilIdle()
        assertNull(vm.uiState.value.validCode)
        assertFalse(vm.uiState.value.isValidating)
        assertEquals(Res.string.qr_invalid_code, assertIs<UiText.StringResource>(vm.uiState.value.error).res)
    }

    @Test fun `a valid code asks to navigate with the clean code`() = scope.runTest {
        buildings.byCodeResult = Result.success(Building(id = "b1", name = "Torre"))
        val vm = vm()
        vm.onManualCode("  https://x.y/r?code=COND-1&z=2 ")
        advanceUntilIdle()
        assertEquals("COND-1", vm.uiState.value.validCode)
        assertNull(vm.uiState.value.error)
        assertEquals(listOf("COND-1"), buildings.byCodeCalls)
    }

    @Test fun `no network shows the connection text not the invalid code text`() = scope.runTest {
        buildings.byCodeResult = Result.failure(NetworkFailureException("Fail to fetch"))
        val vm = vm()
        vm.onManualCode("COND-1")
        advanceUntilIdle()
        assertNull(vm.uiState.value.validCode)
        assertEquals(Res.string.error_check_connection, assertIs<UiText.StringResource>(vm.uiState.value.error).res)
    }

    @Test fun `a double tap while validating makes one call`() = scope.runTest {
        buildings.byCodeGate = CompletableDeferred()
        buildings.byCodeResult = Result.success(Building(id = "b1", name = "Torre"))
        val vm = vm()
        vm.onManualCode("COND-1")
        vm.onManualCode("COND-1")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isValidating)
        buildings.byCodeGate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, buildings.byCodeCalls.size)
    }

    @Test fun `after a valid code further scans are ignored`() = scope.runTest {
        buildings.byCodeResult = Result.success(Building(id = "b1", name = "Torre"))
        val vm = vm()
        vm.onScanned("COND-1")
        advanceUntilIdle()
        vm.onScanned("COND-1")
        vm.onScanned("COND-2")
        advanceUntilIdle()
        assertEquals(1, buildings.byCodeCalls.size)
    }

    @Test fun `the camera does not hammer the server with a code already rejected`() = scope.runTest {
        buildings.byCodeResult = Result.failure(ApiFailureException(404))
        val vm = vm()
        vm.onScanned("BAD")
        advanceUntilIdle()
        vm.onScanned("BAD")
        vm.onScanned("BAD")
        advanceUntilIdle()
        assertEquals(1, buildings.byCodeCalls.size)
        assertNotNull(vm.uiState.value.error)
    }

    @Test fun `a different scanned code is validated after a rejected one`() = scope.runTest {
        buildings.byCodeResult = Result.failure(ApiFailureException(404))
        val vm = vm()
        vm.onScanned("BAD")
        advanceUntilIdle()
        buildings.byCodeResult = Result.success(Building(id = "b1", name = "Torre"))
        vm.onScanned("GOOD")
        advanceUntilIdle()
        assertEquals("GOOD", vm.uiState.value.validCode)
        assertNull(vm.uiState.value.error)
    }

    @Test fun `typing the same rejected code again retries`() = scope.runTest {
        buildings.byCodeResult = Result.failure(ApiFailureException(404))
        val vm = vm()
        vm.onManualCode("BAD")
        advanceUntilIdle()
        vm.onManualCode("BAD")
        advanceUntilIdle()
        assertEquals(2, buildings.byCodeCalls.size)
    }

    @Test fun `a blank code does nothing`() = scope.runTest {
        val vm = vm()
        vm.onManualCode("   ")
        advanceUntilIdle()
        assertEquals(0, buildings.byCodeCalls.size)
        assertNull(vm.uiState.value.error)
    }

    @Test fun `dismissing the error clears it`() = scope.runTest {
        buildings.byCodeResult = Result.failure(ApiFailureException(404))
        val vm = vm()
        vm.onManualCode("BAD")
        advanceUntilIdle()
        vm.onErrorDismissed()
        assertNull(vm.uiState.value.error)
    }
}
