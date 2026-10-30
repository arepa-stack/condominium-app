package com.example.condominio.telemetry

import com.example.condominio.data.model.User
import com.example.condominio.data.model.UserUnit
import com.example.condominio.testutil.FakeAuthRepository
import com.example.condominio.testutil.FakeTelemetry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class TenantTelemetryTest {
    private val unitA = UserUnit(unitId = "unit-1", buildingId = "b-uuid-a", unitName = "1A", buildingName = "Edificio A")
    private val unitB = UserUnit(unitId = "unit-2", buildingId = "b-uuid-b", unitName = "2B", buildingName = "Edificio B")
    private val userA = User(id = "user-1", name = "Ana", email = "ana@test.com", units = listOf(unitA, unitB), currentUnit = unitA)

    /** Collects [activeBuildingId] into [telemetry] like CondominioNavGraph does. */
    private fun TestScope.collectTenant(authRepository: FakeAuthRepository, telemetry: FakeTelemetry) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            authRepository.activeBuildingId().collect(telemetry::setTenant)
        }
    }

    @Test
    fun `login in building A sets tenant A`() = runTest {
        val authRepository = FakeAuthRepository().apply { currentUserFlow.value = null }
        val telemetry = FakeTelemetry()
        collectTenant(authRepository, telemetry)

        authRepository.currentUserFlow.value = userA

        assertEquals(listOf<String?>(null, "b-uuid-a"), telemetry.tenants)
    }

    @Test
    fun `switching to a unit in building B sets tenant B`() = runTest {
        val authRepository = FakeAuthRepository(user = userA)
        val telemetry = FakeTelemetry()
        collectTenant(authRepository, telemetry)

        authRepository.setCurrentUnit(unitB)

        assertEquals(listOf<String?>("b-uuid-a", "b-uuid-b"), telemetry.tenants)
    }

    @Test
    fun `refreshing the user with the same unit does not emit the tenant again`() = runTest {
        val authRepository = FakeAuthRepository(user = userA).apply { publishOnFetch = true }
        val telemetry = FakeTelemetry()
        collectTenant(authRepository, telemetry)

        // A refresh publishes a different User (e.g. edited name) that keeps the same building.
        authRepository.user = userA.copy(name = "Ana María")
        authRepository.fetchCurrentUser()

        assertEquals(listOf<String?>("b-uuid-a"), telemetry.tenants)
    }

    @Test
    fun `logout clears the tenant`() = runTest {
        val authRepository = FakeAuthRepository(user = userA)
        val telemetry = FakeTelemetry()
        collectTenant(authRepository, telemetry)

        authRepository.logout()

        assertEquals(listOf<String?>("b-uuid-a", null), telemetry.tenants)
    }

    @Test
    fun `account deletion clears the tenant`() = runTest {
        val authRepository = FakeAuthRepository(user = userA)
        val telemetry = FakeTelemetry()
        collectTenant(authRepository, telemetry)

        authRepository.deleteAccount(null)

        assertEquals(listOf<String?>("b-uuid-a", null), telemetry.tenants)
    }

    @Test
    fun `NoTelemetry setTenant is no-op and never throws`() {
        NoTelemetry.setTenant("b-uuid-a")
        NoTelemetry.setTenant(null)
    }
}
