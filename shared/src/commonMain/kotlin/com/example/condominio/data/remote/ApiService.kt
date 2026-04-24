package com.example.condominio.data.remote

import com.example.condominio.data.model.*
import kotlinx.serialization.json.JsonElement

interface ApiService {
    suspend fun register(request: Map<String, String>): Response<RegisterResponse>
    suspend fun login(credentials: Map<String, String>): Response<LoginResponse>
    
    suspend fun getCurrentUser(): Response<UserProfile>
    suspend fun updateUser(updates: UpdateUserRequest): Response<UserProfile>

    suspend fun getBuildings(): Response<List<Building>>
    suspend fun getBuilding(id: String): Response<Building>
    suspend fun getBuildingUnits(id: String): Response<List<UnitDto>>
    suspend fun getUnitDetails(id: String): Response<UnitDto>
    
    suspend fun getPayments(unitId: String? = null, year: Int? = null): Response<List<PaymentDto>>
    suspend fun getPayment(id: String): Response<PaymentDto>
    
    suspend fun createPaymentMultipart(
        amount: String,
        unitId: String,
        buildingId: String?,
        method: String,
        date: String,
        notes: String?,
        reference: String?,
        bank: String?,
        allocations: String?,
        proofImage: ByteArray? = null,
        fileName: String? = null
    ): Response<PaymentDto>
    
    suspend fun createPayment(request: CreatePaymentRequest): Response<PaymentDto>
    
    suspend fun getBalance(unitId: String): Response<BalanceDto>
    suspend fun getInvoices(unitId: String, status: String? = null): Response<List<InvoiceDto>>
    suspend fun getInvoice(id: String): Response<InvoiceDto>
    suspend fun getInvoicePayments(id: String): Response<List<PaymentDto>>
    
    suspend fun getCredits(unitId: String): Response<JsonElement> // Temporary generic response
    
    suspend fun getPettyCashBalance(buildingId: String): Response<PettyCashBalanceDto>
}
