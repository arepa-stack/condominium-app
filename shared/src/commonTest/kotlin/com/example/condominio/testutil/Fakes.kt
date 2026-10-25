package com.example.condominio.testutil

import com.example.condominio.data.model.*
import com.example.condominio.data.repository.AnnouncementsRepository
import com.example.condominio.data.repository.AuthRepository
import com.example.condominio.data.repository.BuildingRepository
import com.example.condominio.data.repository.PaymentRepository
import com.example.condominio.data.repository.PettyCashRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/** Installs a test Main dispatcher bound to [scope]'s scheduler; call [resetTestMain] in @AfterTest. */
@OptIn(ExperimentalCoroutinesApi::class)
fun installMain(scope: TestScope) = Dispatchers.setMain(StandardTestDispatcher(scope.testScheduler))

@OptIn(ExperimentalCoroutinesApi::class)
fun resetTestMain() = Dispatchers.resetMain()

val testUnit = UserUnit(unitId = "u1", buildingId = "", unitName = "1A", buildingName = "B")
val testUser = User(id = "1", name = "Ana", email = "a@b.c", units = listOf(testUnit))

class FakeAuthRepository(var user: User = testUser) : AuthRepository {
    var fetchCount = 0
    override val currentUser: Flow<User?> = MutableStateFlow(user)
    override suspend fun fetchCurrentUser(): Result<User> { fetchCount++; return Result.success(user) }
    override suspend fun login(email: String, password: String) = TODO()
    override suspend fun register(name: String, email: String, unitId: String, building: String, password: String) = TODO()
    override suspend fun resetPassword(email: String) = TODO()
    override suspend fun getUnits(buildingId: String) = TODO()
    override suspend fun getUnit(unitId: String) = TODO()
    override suspend fun logout() = TODO()
    override suspend fun updateUser(user: User) = TODO()
    override suspend fun changePassword(currentPassword: String, newPassword: String) = TODO()
    override suspend fun changePasswordFirstLogin(newPassword: String) = TODO()
    override suspend fun deleteAccount(reason: String?) = TODO()
    override fun setCurrentUnit(unit: UserUnit) = TODO()
    override suspend fun hasValidSession() = TODO()
}

class FakePaymentRepository : PaymentRepository {
    var fetchPaymentsCount = 0
    var balanceCount = 0
    override suspend fun fetchPayments(unitId: String?): Result<List<Payment>> {
        fetchPaymentsCount++
        return Result.success(emptyList())
    }
    override suspend fun getBalance(unitId: String): Result<Balance> {
        balanceCount++
        return Result.success(Balance(unitId, 0.0, pendingInvoices = emptyList()))
    }
    override suspend fun getPayments(unitId: String?) = TODO()
    override suspend fun getPayment(id: String) = TODO()
    override suspend fun fetchPayment(id: String) = TODO()
    override suspend fun createPayment(amount: Double, date: Long, description: String, method: PaymentMethod, unitId: String, allocations: List<PaymentAllocation>, reference: String?, bank: String?, phone: String?, proofUrl: String?, buildingId: String?) = TODO()
    override suspend fun getInvoices(unitId: String, status: String?) = TODO()
    override suspend fun getInvoice(id: String) = TODO()
    override suspend fun getInvoicePayments(invoiceId: String) = TODO()
}

class FakeBuildingRepository : BuildingRepository {
    override suspend fun getBuildings() = TODO()
    override suspend fun getBuilding(id: String) = TODO()
    override suspend fun getBuildingByCode(code: String) = TODO()
    override suspend fun getBuildingUnitsByCode(code: String) = TODO()
}

class FakePettyCashRepository : PettyCashRepository {
    override suspend fun getBalance(buildingId: String) = TODO()
}

class FakeAnnouncementsRepository : AnnouncementsRepository {
    override suspend fun list(buildingId: String, category: AnnouncementCategory?, search: String?, isPinned: Boolean?, readStatus: String?, page: Int, limit: Int) = TODO()
    override suspend fun detail(id: String) = TODO()
    override suspend fun markRead(id: String) = TODO()
    override suspend fun toggleReaction(id: String) = TODO()
}
