package com.example.condominio.testutil

import com.example.condominio.data.local.TokenManager
import com.example.condominio.data.model.*
import com.example.condominio.data.remote.ApiService
import com.example.condominio.data.remote.Response
import kotlinx.serialization.json.JsonElement

class FakeTokenManager(var token: String? = null) : TokenManager {
    override suspend fun saveToken(token: String) { this.token = token }
    override suspend fun getToken(): String? = token
    override suspend fun clearToken() { token = null }
}

/** Only [getCurrentUser] is scripted; everything else fails loudly if a test reaches it. */
class FakeApiService : ApiService {
    var currentUserResponse: Response<UserProfile> = Response.success(UserProfile(id = "1", name = "Ana", email = "a@b.c"))
    var currentUserCount = 0

    override suspend fun getCurrentUser(): Response<UserProfile> { currentUserCount++; return currentUserResponse }

    override suspend fun register(request: Map<String, String>): Response<RegisterResponse> = TODO()
    var loginResponse: Response<LoginResponse> = Response.error(401, "x")
    var resetResponse: Response<SuccessDto> = Response.error(500, "x")
    var firstLoginResponse: Response<ChangePasswordFirstLoginResponse> = Response.error(500, "x")
    var registrationResponse: Response<RegistrationRequestResponse> = Response.error(500, "x")
    var buildingByCodeResponse: Response<Building> = Response.error(404, "x")

    override suspend fun login(credentials: Map<String, String>): Response<LoginResponse> = loginResponse
    override suspend fun resetPassword(email: String): Response<SuccessDto> = resetResponse
    var changePasswordResponse: Response<SuccessDto> = Response.success(SuccessDto(true))
    override suspend fun changePassword(newPassword: String): Response<SuccessDto> = changePasswordResponse
    override suspend fun changePasswordFirstLogin(newPassword: String): Response<ChangePasswordFirstLoginResponse> = firstLoginResponse
    var updateUserResponse: Response<UserProfile> = Response.success(UserProfile(id = "1", name = "Ana", email = "a@b.c"))
    val updateRequests = mutableListOf<UpdateUserRequest>()
    override suspend fun updateUser(updates: UpdateUserRequest): Response<UserProfile> { updateRequests += updates; return updateUserResponse }
    var deleteAccountResponse: Response<SuccessDto> = Response.success(SuccessDto(true))
    override suspend fun deleteAccount(reason: String?): Response<SuccessDto> = deleteAccountResponse
    override suspend fun getBuildings(): Response<List<Building>> = TODO()
    override suspend fun getBuilding(id: String): Response<Building> = TODO()
    override suspend fun getBuildingByCode(code: String): Response<Building> = buildingByCodeResponse
    var buildingUnitsByCodeResponse: Response<List<UnitDto>> = Response.success(emptyList())
    override suspend fun getBuildingUnitsByCode(code: String): Response<List<UnitDto>> = buildingUnitsByCodeResponse
    override suspend fun getBuildingUnits(id: String): Response<List<UnitDto>> = TODO()
    override suspend fun getUnitDetails(id: String): Response<UnitDto> = TODO()
    override suspend fun submitRegistrationRequest(body: RegistrationRequestBody): Response<RegistrationRequestResponse> = registrationResponse
    override suspend fun getPayments(unitId: String?, year: Int?): Response<List<PaymentDto>> = TODO()
    override suspend fun getPayment(id: String): Response<PaymentDto> = TODO()
    override suspend fun createPaymentMultipart(amount: String, unitId: String, buildingId: String?, method: String, date: String, notes: String?, reference: String?, bank: String?, allocations: String?, proofImage: ByteArray?, currency: String, fileName: String?): Response<PaymentDto> = TODO()
    override suspend fun createPayment(request: CreatePaymentRequest): Response<PaymentDto> = TODO()
    override suspend fun getBalance(unitId: String): Response<BalanceDto> = TODO()
    override suspend fun getInvoices(unitId: String, status: String?): Response<List<InvoiceDto>> = TODO()
    override suspend fun getInvoice(id: String): Response<InvoiceDto> = TODO()
    override suspend fun getInvoicePayments(id: String): Response<List<PaymentDto>> = TODO()
    override suspend fun getCredits(unitId: String): Response<JsonElement> = TODO()
    override suspend fun getPettyCashBalance(buildingId: String): Response<PettyCashBalanceDto> = TODO()
    override suspend fun getExchangeRates(date: String?): Response<RateSetDto> = TODO()
    override suspend fun getBuildingPaymentInfo(buildingId: String): Response<BuildingPaymentInfoDto> = TODO()
    override suspend fun listDecisions(buildingId: String?, status: String?, search: String?, page: Int?, limit: Int?): Response<DecisionsPageDto> = TODO()
    override suspend fun getDecisionDetail(id: String): Response<DecisionDetailDto> = TODO()
    override suspend fun uploadDecisionQuote(decisionId: String, unitId: String, providerName: String, amount: String, notes: String?, fileBytes: ByteArray, fileName: String, mimeType: String): Response<QuoteDto> = TODO()
    override suspend fun deleteDecisionQuote(decisionId: String, quoteId: String): Response<QuoteDto> = TODO()
    override suspend fun castVote(decisionId: String, apartmentId: String, quoteId: String): Response<VoteDto> = TODO()
    override suspend fun getDecisionResults(id: String, round: Int?): Response<TallyDto> = TODO()
    override suspend fun listAnnouncements(buildingId: String?, category: String?, search: String?, isPinned: Boolean?, readStatus: String?, page: Int?, limit: Int?): Response<AnnouncementsPageDto> = TODO()
    override suspend fun getAnnouncementDetail(id: String): Response<AnnouncementDto> = TODO()
    override suspend fun markAnnouncementRead(id: String): Response<SuccessDto> = TODO()
    override suspend fun toggleAnnouncementReaction(id: String): Response<ToggleReactionResponseDto> = TODO()
}
