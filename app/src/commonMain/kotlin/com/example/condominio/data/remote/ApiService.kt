package com.example.condominio.data.remote

import com.example.condominio.data.model.*

interface ApiService {
    suspend fun register(request: Map<String, String>): Response<RegisterResponse>
    suspend fun login(credentials: Map<String, String>): Response<LoginResponse>
    
    suspend fun getCurrentUser(): Response<UserProfile>
    suspend fun updateUser(updates: UpdateUserRequest): Response<UserProfile>
    suspend fun getUserUnits(id: String): Response<List<UserUnitDto>>
    
    suspend fun getBuildings(): Response<List<Building>>
    suspend fun getBuilding(id: String): Response<Building>
    suspend fun getBuildingUnits(id: String): Response<List<UnitDto>>
    suspend fun getUnitDetails(id: String): Response<UnitDto>
    
    suspend fun getPaymentSummary(): Response<PaymentSummaryDto>
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
    
    suspend fun getPettyCashBalance(buildingId: String): Response<PettyCashBalanceDto>
    suspend fun getPettyCashHistory(
        buildingId: String,
        type: String? = null,
        category: String? = null,
        page: Int = 1,
        limit: Int = 10
    ): Response<List<PettyCashTransactionDto>>
    
    suspend fun registerPettyCashIncome(request: RegisterIncomeRequest): Response<PettyCashTransactionDto>
    
    suspend fun registerPettyCashExpense(
        buildingId: String,
        amount: String,
        description: String,
        category: String,
        evidenceImage: ByteArray? = null,
        fileName: String? = null
    ): Response<PettyCashTransactionDto>
}
