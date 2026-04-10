package com.example.condominio.data.repository

import com.example.condominio.data.model.*

interface PettyCashRepository {
    suspend fun getBalance(buildingId: String): Result<PettyCashBalanceDto>
    suspend fun getHistory(
        buildingId: String,
        type: PettyCashTransactionType? = null,
        category: PettyCashCategory? = null,
        page: Int = 1,
        limit: Int = 10
    ): Result<List<PettyCashTransactionDto>>
    suspend fun registerIncome(buildingId: String, amount: Double, description: String): Result<PettyCashTransactionDto>
    suspend fun registerExpense(
        buildingId: String,
        amount: Double,
        description: String,
        category: PettyCashCategory,
        evidencePath: String? = null
    ): Result<PettyCashTransactionDto>
}
