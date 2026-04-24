package com.example.condominio.data.repository

import com.example.condominio.data.model.PettyCashBalanceDto

interface PettyCashRepository {
    suspend fun getBalance(buildingId: String): Result<PettyCashBalanceDto>
}
