package com.example.condominio.testutil

import com.example.condominio.data.model.*
import com.example.condominio.data.repository.AnnouncementsRepository
import com.example.condominio.data.repository.AuthRepository
import com.example.condominio.data.repository.BuildingRepository
import com.example.condominio.data.repository.DecisionsRepository
import kotlinx.coroutines.CompletableDeferred
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
    /** null = succeed with [user]. */
    var fetchResult: Result<User>? = null
    var hasSession = true
    var loginResult: Result<User> = Result.success(testUser)
    val logins = mutableListOf<Pair<String, String>>()
    /** When set, [login] suspends until it completes (to test the in-flight state). */
    var loginGate: CompletableDeferred<Unit>? = null
    var logoutCount = 0
    /** When set, [logout] suspends until it completes (to test the in-flight state). */
    var logoutGate: CompletableDeferred<Unit>? = null
    var resetResult: Result<Unit> = Result.success(Unit)
    var firstLoginResult: Result<Unit> = Result.success(Unit)
    override val currentUser: Flow<User?> = MutableStateFlow(user)
    /** When set, [fetchCurrentUser] throws it (an unexpected failure outside Result). */
    var fetchThrows: Throwable? = null
    override suspend fun fetchCurrentUser(): Result<User> { fetchCount++; fetchThrows?.let { throw it }; return fetchResult ?: Result.success(user) }
    override suspend fun login(email: String, password: String): Result<User> {
        logins += email to password
        loginGate?.await()
        return loginResult
    }
    override suspend fun register(name: String, email: String, unitId: String, building: String, password: String) = TODO()
    override suspend fun resetPassword(email: String) = resetResult
    override suspend fun getUnits(buildingId: String) = TODO()
    override suspend fun getUnit(unitId: String) = TODO()
    override suspend fun logout() { logoutCount++; logoutGate?.await() }
    var updateResult: Result<Unit> = Result.success(Unit)
    val updates = mutableListOf<User>()
    /** When set, [updateUser] suspends until it completes (to test the in-flight state). */
    var updateGate: CompletableDeferred<Unit>? = null
    override suspend fun updateUser(user: User): Result<Unit> {
        updates += user
        updateGate?.await()
        return updateResult
    }
    var changePasswordResult: Result<Unit> = Result.success(Unit)
    val passwordChanges = mutableListOf<Pair<String, String>>()
    /** When set, [changePassword] suspends until it completes (to test the in-flight state). */
    var changePasswordGate: CompletableDeferred<Unit>? = null
    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        passwordChanges += currentPassword to newPassword
        changePasswordGate?.await()
        return changePasswordResult
    }
    override suspend fun changePasswordFirstLogin(newPassword: String) = firstLoginResult
    var deleteResult: Result<Unit> = Result.success(Unit)
    override suspend fun deleteAccount(reason: String?): Result<Unit> = deleteResult
    override fun setCurrentUnit(unit: UserUnit) {}
    override suspend fun hasValidSession() = hasSession
}

class FakeOnboardingRepository : com.example.condominio.data.repository.OnboardingRepository {
    var result: Result<RegistrationRequestResponse> = Result.failure(Exception("unset"))
    var submitCount = 0
    override suspend fun submitRegistrationRequest(buildingCode: String, unitId: String, email: String, firstName: String, lastName: String, documentId: String, phone: String?): Result<RegistrationRequestResponse> {
        submitCount++
        return result
    }
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
    var byCodeResult: Result<Building> = Result.failure(Exception("unset"))
    val byCodeCalls = mutableListOf<String>()
    /** When set, [getBuildingByCode] suspends until it completes (to test the in-flight state). */
    var byCodeGate: CompletableDeferred<Unit>? = null
    override suspend fun getBuildingByCode(code: String): Result<Building> {
        byCodeCalls += code
        byCodeGate?.await()
        return byCodeResult
    }
    override suspend fun getBuildingUnitsByCode(code: String) = TODO()
}

class FakePettyCashRepository : PettyCashRepository {
    override suspend fun getBalance(buildingId: String) = TODO()
}

class FakeDecisionsRepository : DecisionsRepository {
    var detailResult: Result<DecisionDetailDto> = Result.failure(Exception("unset"))
    var detailCount = 0
    /** When set, [vote] and [uploadQuote] suspend until it completes (to test the in-flight state). */
    var gate: CompletableDeferred<Unit>? = null
    var voteResult: Result<VoteDto> = Result.failure(Exception("vote failed"))
    var uploadResult: Result<QuoteDto> = Result.failure(Exception("upload failed"))
    val votes = mutableListOf<String>()
    var uploadCount = 0

    override suspend fun detail(id: String): Result<DecisionDetailDto> { detailCount++; return detailResult }
    override suspend fun vote(decisionId: String, unitId: String, quoteId: String): Result<VoteDto> {
        votes += quoteId
        gate?.await()
        return voteResult
    }
    override suspend fun uploadQuote(decisionId: String, unitId: String, providerName: String, amount: Double, notes: String?, fileUri: String, mimeType: String): Result<QuoteDto> {
        uploadCount++
        gate?.await()
        return uploadResult
    }
    override suspend fun list(buildingId: String, status: DecisionStatus?, search: String?, page: Int, limit: Int) = TODO()
    override suspend fun deleteQuote(decisionId: String, quoteId: String) = TODO()
    override suspend fun results(decisionId: String, round: Int?) = TODO()
}

fun testDecisionDetail(status: DecisionStatus = DecisionStatus.VOTING) = DecisionDetailDto(
    decision = DecisionDto(
        id = "d1", buildingId = "b1", title = "Pintura", status = status, currentRound = 1,
        receptionDeadline = "2026-01-01T00:00:00Z", votingDeadline = "2026-01-10T00:00:00Z",
        tiebreakDurationHours = 24, createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
        quoteCount = 0, isDeadlinePassed = false
    ),
    quotes = emptyList(),
    tally = TallyDto(round = 1, status = "OPEN", totalApartments = 10, totalVotes = 0, participationPct = 0.0, tallies = emptyList(), isTied = false)
)

fun testVote() = VoteDto(id = "v1", decisionId = "d1", round = 1, apartmentId = "u1", quoteId = "q1", createdAt = "2026-01-02T00:00:00Z")

fun testAnnouncement(
    id: String = "a1",
    reacted: Boolean = false,
    reactions: Int = 3,
    attachmentUrl: String? = null,
) = AnnouncementDto(
    id = id, buildingId = "b1", title = "Corte de agua", content = "Contenido", contentPreview = "Contenido",
    category = AnnouncementCategory.INFO, attachmentUrl = attachmentUrl, isPinned = false,
    expiresAt = null, createdAt = "2026-03-05T12:00:00Z", updatedAt = "2026-03-05T12:00:00Z",
    readByCurrentUser = false, reactedByCurrentUser = reacted,
    metrics = AnnouncementMetricsDto(readsCount = 5, reactionsCount = reactions),
)

class FakeAnnouncementsRepository : AnnouncementsRepository {
    var detailResult: Result<AnnouncementDto> = Result.failure(Exception("unset"))
    var toggleResult: Result<ToggleReactionResponseDto> = Result.failure(Exception("toggle failed"))
    var detailCount = 0
    val markedRead = mutableListOf<String>()

    override suspend fun list(buildingId: String, category: AnnouncementCategory?, search: String?, isPinned: Boolean?, readStatus: String?, page: Int, limit: Int) = TODO()
    override suspend fun detail(id: String): Result<AnnouncementDto> { detailCount++; return detailResult }
    override suspend fun markRead(id: String): Result<Unit> { markedRead += id; return Result.success(Unit) }
    override suspend fun toggleReaction(id: String): Result<ToggleReactionResponseDto> = toggleResult
}
